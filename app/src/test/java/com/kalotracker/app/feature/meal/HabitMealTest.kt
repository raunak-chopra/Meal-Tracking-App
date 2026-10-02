package com.kalotracker.app.feature.meal

import androidx.lifecycle.ViewModelStore
import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.database.dao.MealDao
import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.core.database.entity.MealEntity
import com.kalotracker.app.core.network.*
import com.kalotracker.app.core.settings.AiSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class HabitMealTest {
    private fun item() = EditableFoodItem(name = "Rice", portionGrams = 100f,
        baseCaloriesPerGram = 1.3f, baseProteinPerGram = .03f, baseCarbsPerGram = .28f, baseFatPerGram = .01f, confidence = .7f)
    private fun response(name: String) = Result.success(MealAnalysisResponse(mealTitle = name,
        items = listOf(DetectedFoodItem(name, 100f, 130, 3f, 28f, 1f))))

    @Test fun relativePortionsScaleNutritionWithoutChangingFoodIdentity() {
        val original = item()
        val smaller = original.scaledPortion(.75f)
        assertEquals(original.id, smaller.id)
        assertEquals(75f, smaller.portionGrams, .001f)
        assertEquals(97, smaller.currentCalories)
        assertEquals(2.25f, smaller.currentProtein, .001f)
        assertEquals(5000f, original.copy(portionGrams = 4999f).scaledPortion(1.25f).portionGrams, .001f)
    }

    @Test fun oilAndButterHaveDifferentNutritionAndStayInMealTotals() {
        val oil = MealScanUiState(hasAddedOil = true, cookingFatGrams = 14f)
        val butter = oil.copy(cookingFat = CookingFat.BUTTER)
        assertEquals(120, oil.totalCalories)
        assertEquals(100, butter.totalCalories)
        assertEquals(11.34f, butter.totalFat, .001f)
    }

    @Test fun draftRoundTripKeepsCorrectionsDateAndStableIdButNotBusyFlags() {
        val original = MealScanUiState(draftMealId = "stable", mealTitle = "Lunch", timestamp = 123456,
            items = listOf(item().scaledPortion(.75f)), userNote = "half a bowl",
            cookingFat = CookingFat.BUTTER, cookingFatGrams = 5f, hasAddedOil = true,
            isAnalyzing = true, isSaving = true, errorMessage = "temporary")
        val recovered = MealDraftCodec.decode(MealDraftCodec.encode(original))
        assertEquals("stable", recovered.draftMealId)
        assertEquals(123456L, recovered.timestamp)
        assertEquals(original.items, recovered.items)
        assertEquals(original.totalCalories, recovered.totalCalories)
        assertFalse(recovered.isSaving)
        assertFalse(recovered.isAnalyzing)
        assertNull(recovered.errorMessage)
    }

    @Test fun obsoleteAnalysisCannotReplaceANewerPhotoOrDiscardedMeal() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val service = DeferredAnalysis()
        val vm = MealViewModel(MealRepository(fakeDao(), dispatcher), service)
        val store = ViewModelStore().apply { put("meal", vm) }
        try {
            vm.analyzeCapturedImage(byteArrayOf(1)); runCurrent()
            vm.analyzeCapturedImage(byteArrayOf(2)); runCurrent()
            service.requests[1].complete(response("New")); runCurrent()
            service.requests[0].complete(response("Old")); runCurrent()
            assertEquals("New", vm.uiState.value.mealTitle)
            vm.retryAnalysis(); runCurrent()
            vm.resetScan()
            service.requests[2].complete(response("Discarded")); runCurrent()
            assertTrue(vm.uiState.value.items.isEmpty())
            assertFalse(vm.uiState.value.isAnalyzing)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun recoveryRequiresResumeAndSuccessfulSaveKeepsStableMealId() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val disk = MemoryDraft(MealScanUiState(draftMealId = "stable", timestamp = 42, items = listOf(item())))
        val writes = mutableListOf<MealEntity>()
        val vm = MealViewModel(MealRepository(fakeDao(writes), dispatcher), DeferredAnalysis(), draftStore = disk)
        val store = ViewModelStore().apply { put("meal", vm) }
        try {
            runCurrent()
            assertTrue(vm.uiState.value.resumePending)
            assertEquals(42L, vm.uiState.value.timestamp)
            vm.resumeDraft(); runCurrent()
            assertFalse(vm.uiState.value.resumePending)
            var saves = 0
            repeat(3) { vm.saveMeal { saves++ } }; runCurrent()
            assertEquals(1, saves)
            assertEquals(1, writes.size)
            assertEquals("stable", writes.single().id)
            assertNull(disk.value)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun discardCannotResetDraftDuringASuspendedSave() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val gate = CompletableDeferred<Unit>()
        val disk = object : MealDraftStore {
            override suspend fun load() = MealScanUiState(draftMealId = "stable", items = listOf(item()))
            override suspend fun save(state: MealScanUiState) { if (state.isSaving) gate.await() }
            override suspend fun clear() {}
        }
        val writes = mutableListOf<MealEntity>()
        val vm = MealViewModel(MealRepository(fakeDao(writes), dispatcher), DeferredAnalysis(), draftStore = disk)
        val store = ViewModelStore().apply { put("meal", vm) }
        try {
            runCurrent(); vm.resumeDraft(); runCurrent()
            vm.saveMeal {}; runCurrent()
            assertTrue(vm.uiState.value.isSaving)
            vm.resetScan()
            assertEquals("stable", vm.uiState.value.draftMealId)
            assertFalse(vm.uiState.value.items.isEmpty())
            gate.complete(Unit); runCurrent()
            assertEquals(1, writes.size)
            assertTrue(vm.uiState.value.isSaved)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun alreadyCommittedDraftIsClearedInsteadOfOfferedAgain() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val disk = MemoryDraft(MealScanUiState(draftMealId = "committed", items = listOf(item())))
        val existing = MealWithItems(MealEntity(id = "committed", title = "Saved", totalCalories = 130,
            totalProteinGrams = 3f, totalCarbsGrams = 28f, totalFatGrams = 1f), emptyList())
        val vm = MealViewModel(MealRepository(fakeDao(existing = existing), dispatcher), DeferredAnalysis(), draftStore = disk)
        val store = ViewModelStore().apply { put("meal", vm) }
        try {
            runCurrent()
            assertFalse(vm.uiState.value.resumePending)
            assertTrue(vm.uiState.value.items.isEmpty())
            assertNull(disk.value)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun saveIsBlockedWhileAnalysisIsInFlight() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val writes = mutableListOf<MealEntity>()
        val service = DeferredAnalysis()
        val vm = MealViewModel(MealRepository(fakeDao(writes), dispatcher), service)
        val store = ViewModelStore().apply { put("meal", vm) }
        try {
            vm.analyzeCapturedImage(byteArrayOf(1)); runCurrent()
            vm.saveMeal {}; runCurrent()
            assertTrue(writes.isEmpty())
            service.requests.single().complete(response("Rice")); runCurrent()
            vm.retryAnalysis(); runCurrent()
            vm.saveMeal {}; runCurrent()
            assertTrue(writes.isEmpty())
            service.requests[1].complete(response("Rice")); runCurrent()
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    private class DeferredAnalysis : MealAnalysisService({ AiSettings() }) {
        val requests = mutableListOf<CompletableDeferred<Result<MealAnalysisResponse>>>()
        override suspend fun analyzeMealImage(imageBytes: ByteArray, userNote: String?): Result<MealAnalysisResponse> {
            val pending = CompletableDeferred<Result<MealAnalysisResponse>>()
            requests += pending
            // Mimic a transport that finishes even after cancellation.
            return withContext(NonCancellable) { pending.await() }
        }
    }
    private class MemoryDraft(var value: MealScanUiState?) : MealDraftStore {
        override suspend fun load() = value
        override suspend fun save(state: MealScanUiState) { value = MealDraftCodec.decode(MealDraftCodec.encode(state)) }
        override suspend fun clear() { value = null }
    }
    private fun fakeDao(writes: MutableList<MealEntity> = mutableListOf(), existing: MealWithItems? = null): MealDao =
        Proxy.newProxyInstance(MealDao::class.java.classLoader, arrayOf(MealDao::class.java)) { _, method, args ->
            when (method.name) {
                "getMealById" -> existing
                "insertMealWithItems" -> { writes += args[0] as MealEntity; Unit }
                else -> error("Unexpected DAO call: ${method.name}")
            }
        } as MealDao
}
