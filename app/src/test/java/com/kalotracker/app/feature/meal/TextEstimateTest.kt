package com.kalotracker.app.feature.meal

import com.kalotracker.app.core.network.DetectedFoodItem
import com.kalotracker.app.core.network.MealAnalysisResponse
import com.kalotracker.app.core.network.MealAnalysisService
import com.kalotracker.app.core.settings.AiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TextEstimateTest {
    private val misread = EditableFoodItem(name = "Chicken bharta", portionGrams = 200f, baseCaloriesPerGram = 1.5f,
        baseProteinPerGram = 0.07f, baseCarbsPerGram = 0.05f, baseFatPerGram = 0.12f, confidence = 0.6f)

    @Test fun reestimateKeepsTypedNameAndAmountButTakesNewNutrition() {
        val response = MealAnalysisResponse(confidence = 0.8f,
            items = listOf(DetectedFoodItem("Chicken bharta", 250f, 400, 35f, 10f, 24f)))
        val fixed = misread.renamed("Corrected chicken").withEstimate(response)
        assertFalse(fixed.needsNutritionReview)
        assertEquals("Corrected chicken", fixed.name)
        assertEquals(200f, fixed.portionGrams)
        assertEquals(320, fixed.currentCalories)
        assertEquals(28f, fixed.currentProtein, 0.01f)
        assertEquals(0.8f, fixed.confidence)
        assertEquals(misread.id, fixed.id)
    }

    @Test fun severalReturnedPartsAreCombinedIntoOneDensity() {
        val response = MealAnalysisResponse(items = listOf(
            DetectedFoodItem("Rice", 100f, 130, 3f, 28f, 0f),
            DetectedFoodItem("Dal", 100f, 110, 7f, 18f, 2f)))
        val fixed = misread.copy(name = "Dal chawal").withEstimate(response)
        assertEquals(240, fixed.currentCalories) // 200 g at the combined 1.2 kcal/g
    }

    @Test fun descriptionCarriesNameAndAmount() {
        assertEquals("Chicken bharta, 200 g", misread.estimateDescription())
        assertEquals("Lassi, 250 ml", misread.copy(name = " Lassi ", portionGrams = 250f, unit = "ml").estimateDescription())
    }

    private class FakeService(val response: Result<MealAnalysisResponse>) : MealAnalysisService({ AiSettings("key") }) {
        val asked = mutableListOf<String>()
        override suspend fun analyzeMealText(description: String): Result<MealAnalysisResponse> {
            asked += description
            return response
        }
    }

    @Test fun manualEstimateIsHeldUntilTheScreenAddsIt() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val service = FakeService(Result.success(MealAnalysisResponse(mealTitle = "Chicken bharta", estimationNotes = "Home-style, 1 tbsp oil",
                items = listOf(DetectedFoodItem("Chicken bharta", 200f, 320, 28f, 8f, 19f, unit = "g")))))
            val vm = ManualMealViewModel(service)
            vm.estimate("chicken bharta")
            vm.estimate("chicken bharta") // ignored while the first is running
            assertTrue(vm.estimating.value)
            runCurrent()
            assertFalse(vm.estimating.value)
            assertEquals(1, service.asked.size)
            val estimate = vm.estimate.value!!
            assertEquals("Chicken bharta", estimate.title)
            assertEquals(320, estimate.items.single().calories)
            assertEquals("", estimate.items.single().mealId)
            vm.consumeEstimate()
            assertNull(vm.estimate.value)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun manualEstimateFailureIsShown() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val vm = ManualMealViewModel(FakeService(Result.failure(com.kalotracker.app.core.network.MealAnalysisException.Offline())))
            vm.estimate("dal")
            runCurrent()
            assertNotNull(vm.estimateError.value)
            assertNull(vm.estimate.value)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun cannotSaveOrRequestAgainUntilEstimateIsConsumed() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val service = FakeService(Result.success(MealAnalysisResponse(items = listOf(
                DetectedFoodItem("Chicken", 200f, 320, 28f, 8f, 19f)))))
            val vm = ManualMealViewModel(service)
            var writes = 0
            vm.estimate("Half a chicken at Nando's")
            vm.save { writes++ }
            runCurrent()
            vm.estimate("Another chicken")
            vm.save { writes++ }
            runCurrent()
            assertEquals(1, service.asked.size)
            assertEquals(0, writes)
            vm.consumeEstimate()
            vm.save { writes++ }
            runCurrent()
            assertEquals(1, writes)
        } finally { Dispatchers.resetMain() }
    }
}
