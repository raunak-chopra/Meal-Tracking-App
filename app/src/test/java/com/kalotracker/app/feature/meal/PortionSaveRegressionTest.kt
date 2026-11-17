package com.kalotracker.app.feature.meal

import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.database.dao.MealDao
import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.entity.MealEntity
import com.kalotracker.app.core.network.*
import com.kalotracker.app.core.settings.AiSettings
import com.kalotracker.app.feature.barcode.BarcodeUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PortionSaveRegressionTest {
    private fun food() = EditableFoodItem(name = "Fixture rice", portionGrams = 150f,
        baseCaloriesPerGram = 1.3f, baseProteinPerGram = .03f,
        baseCarbsPerGram = .28f, baseFatPerGram = .01f, confidence = .7f)

    @Test fun invalidVisibleAmountsRemainInvalidAcrossDraftRecovery() {
        for (raw in listOf("", "0", "-2", ".", "1..2", "NaN", "Infinity", "5001", "0.5")) {
            val item = food().withPortionInput(raw)
            assertEquals(raw, item.amountText)
            assertFalse(raw, item.hasValidPortion)
            val recovered = MealDraftCodec.decode(MealDraftCodec.encode(MealScanUiState(items = listOf(item))))
            assertEquals(raw, recovered.items.single().amountText)
            assertFalse(raw, recovered.items.single().hasValidPortion)
        }
        val corrected = food().withPortionInput("200.25")
        assertTrue(corrected.hasValidPortion)
        assertEquals(200.25f, corrected.portionGrams, 0f)
        assertEquals("200.25", corrected.amountText)
        assertTrue(food().hasValidPortion) // old drafts have no raw amount field
        assertFalse(food().copy(amountInput = "200").hasValidPortion) // malformed draft cannot show one amount and save another
        assertTrue(food().withPortionInput("0").scaledPortion(1.25f).hasValidPortion)
    }

    @Test fun photoSaveRejectsInvalidInputThenPersistsCorrectedDisplayedAmountOnce() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val writes = mutableListOf<Pair<MealEntity, List<FoodItemEntity>>>()
            val service = object : MealAnalysisService({ AiSettings("fixture") }) {
                override suspend fun analyzeMealImage(imageBytes: ByteArray, userNote: String?) =
                    Result.success(MealAnalysisResponse(mealTitle = "Fixture rice",
                        items = listOf(DetectedFoodItem("Fixture rice", 150f, 195, 4.5f, 42f, 1.5f))))
            }
            val vm = MealViewModel(MealRepository(dao(null, writes), dispatcher), service)
            vm.analyzeCapturedImage(byteArrayOf(1)); runCurrent()
            val id = vm.uiState.value.items.single().id
            vm.setItemAmountInput(id, "0")
            var callbacks = 0
            vm.saveMeal { callbacks++ }; runCurrent()
            assertTrue(writes.isEmpty())
            assertEquals(0, callbacks)
            assertNotNull(vm.uiState.value.errorMessage)
            assertEquals("0", vm.uiState.value.items.single().amountText)
            vm.setItemAmountInput(id, "200")
            repeat(3) { vm.saveMeal { callbacks++ } }; runCurrent()
            assertEquals(1, writes.size)
            assertEquals(1, callbacks)
            assertEquals(200f, writes.single().second.single().portionGrams, 0f)
            assertEquals(260, writes.single().first.totalCalories)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun savedMealEditorCannotCommitLastValidAmountWhileFieldIsEmpty() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val meal = MealEntity(id = "fixture", title = "Fixture rice", totalCalories = 195,
                totalProteinGrams = 4.5f, totalCarbsGrams = 42f, totalFatGrams = 1.5f)
            val row = FoodItemEntity(id = "food", mealId = meal.id, name = meal.title,
                portionGrams = 150f, calories = 195, protein = 4.5f, carbs = 42f, fat = 1.5f)
            val writes = mutableListOf<Pair<MealEntity, List<FoodItemEntity>>>()
            val vm = EditMealViewModel(meal.id, MealRepository(dao(MealWithItems(meal, listOf(row)), writes), dispatcher))
            runCurrent()
            vm.setItemAmountInput(row.id, "")
            vm.save { fail("Invalid editor input must not navigate") }; runCurrent()
            assertTrue(writes.isEmpty())
            assertNotNull(vm.uiState.value.errorMessage)
            vm.setItemAmountInput(row.id, "250")
            vm.save {}; runCurrent()
            assertEquals(250f, writes.single().second.single().portionGrams, 0f)
            assertEquals(325, writes.single().first.totalCalories)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun barcodeCannotLogAnInvalidVisibleAmountEvenWhenLabelIsConfirmed() {
        val product = ScannedFoodProduct("fixture", "Fixture packet", requiresLabelConfirmation = true)
        val valid = BarcodeUiState(product = product, labelConfirmed = true)
        assertTrue(valid.canLog)
        for (raw in listOf("", "0", "4", "2501", "NaN", "-100", "1..2")) {
            assertFalse(raw, valid.copy(amountInput = raw).canLog)
        }
        assertTrue(valid.copy(amountInput = "100.5", portionGrams = 100.5f).canLog)
        assertFalse(valid.copy(amountInput = "100.5").canLog)
        assertFalse(valid.copy(labelConfirmed = false).canLog)
        assertFalse(valid.copy(isSaving = true).canLog)
        assertFalse(valid.copy(portionGrams = 3000f).canLog)
    }

    @Test fun renamedFoodNeedsExplicitNutritionReviewAndRetainsItAcrossDraftRecovery() {
        val renamed = food().renamed("Fixture dal")
        assertTrue(renamed.needsNutritionReview)
        assertFalse(renamed.isReadyToSave)
        val recovered = MealDraftCodec.decode(MealDraftCodec.encode(MealScanUiState(items = listOf(renamed)))).items.single()
        assertTrue(recovered.needsNutritionReview)
        assertTrue(renamed.renamed("Fixture rice").isReadyToSave)
        assertTrue(renamed.keepNutrition().isReadyToSave)
        assertFalse(renamed.keepNutrition().renamed("Another food").isReadyToSave)
        assertTrue(renamed.correctedNutrition(200, 5f, 30f, 4f).isReadyToSave)
        assertFalse(food().renamed(" ").keepNutrition().isReadyToSave)
    }

    @Test fun photoRenameCannotSaveUntilExistingValuesAreExplicitlyKept() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val writes = mutableListOf<Pair<MealEntity, List<FoodItemEntity>>>()
            val service = object : MealAnalysisService({ AiSettings("fixture") }) {
                override suspend fun analyzeMealImage(imageBytes: ByteArray, userNote: String?) =
                    Result.success(MealAnalysisResponse(items = listOf(DetectedFoodItem("Fixture rice", 150f, 195, 4.5f, 42f, 1.5f))))
            }
            val vm = MealViewModel(MealRepository(dao(null, writes), StandardTestDispatcher(testScheduler)), service)
            vm.analyzeCapturedImage(byteArrayOf(1)); runCurrent()
            val id = vm.uiState.value.items.single().id
            vm.setItemName(id, "Fixture dal")
            vm.saveMeal { fail("Unreviewed renamed food must not save") }; runCurrent()
            assertTrue(writes.isEmpty())
            vm.keepItemNutrition(id)
            vm.saveMeal {}; runCurrent()
            assertEquals("Fixture dal", writes.single().second.single().name)
            assertEquals(195, writes.single().first.totalCalories)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun savedFoodRenameCanBeResolvedByManualCorrectionAndRejectsIncompatibleAiUnits() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val meal = MealEntity(id = "fixture", title = "Fixture rice", totalCalories = 195,
                totalProteinGrams = 4.5f, totalCarbsGrams = 42f, totalFatGrams = 1.5f)
            val row = FoodItemEntity(id = "food", mealId = meal.id, name = meal.title,
                portionGrams = 150f, calories = 195, protein = 4.5f, carbs = 42f, fat = 1.5f)
            val writes = mutableListOf<Pair<MealEntity, List<FoodItemEntity>>>()
            val service = object : MealAnalysisService({ AiSettings("fixture") }) {
                override suspend fun analyzeMealText(description: String) = Result.success(MealAnalysisResponse(
                    items = listOf(DetectedFoodItem("Fixture drink", 200f, 100, 2f, 20f, 1f, unit = "ml"))))
            }
            val vm = EditMealViewModel(meal.id, MealRepository(dao(MealWithItems(meal, listOf(row)), writes), StandardTestDispatcher(testScheduler)), service)
            runCurrent()
            vm.setItemName(row.id, "Fixture dal")
            vm.reestimateItem(row.id); runCurrent()
            assertNotNull(vm.uiState.value.errorMessage)
            assertTrue(vm.uiState.value.items.single().needsNutritionReview)
            vm.save { fail("Unreviewed renamed food must not save") }; runCurrent()
            assertTrue(writes.isEmpty())
            vm.correctNutrition(row.id, 220, 9f, 30f, 7f)
            vm.save {}; runCurrent()
            assertEquals(220, writes.single().first.totalCalories)
            assertEquals("Fixture dal", writes.single().second.single().name)
        } finally { Dispatchers.resetMain() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun dao(loaded: MealWithItems?, writes: MutableList<Pair<MealEntity, List<FoodItemEntity>>>): MealDao =
        Proxy.newProxyInstance(MealDao::class.java.classLoader, arrayOf(MealDao::class.java)) { _, method, args ->
            when (method.name) {
                "getMealById" -> loaded
                "insertMealWithItems", "replaceMealWithItems" -> {
                    writes += (args[0] as MealEntity) to (args[1] as List<FoodItemEntity>); Unit
                }
                else -> error("Unexpected DAO operation: ${method.name}")
            }
        } as MealDao
}
