package com.kalotracker.app.feature.meal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ManualMealViewModelTest {
    @Test fun missingKeyIsCaughtBeforeRequestAndConfigurationCanBeRefreshed() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var settings = com.kalotracker.app.core.settings.AiSettings()
            var requests = 0
            val service = object : com.kalotracker.app.core.network.MealAnalysisService({ settings }) {
                override suspend fun analyzeMealText(description: String): Result<com.kalotracker.app.core.network.MealAnalysisResponse> {
                    requests++
                    return Result.success(com.kalotracker.app.core.network.MealAnalysisResponse())
                }
            }
            val vm = ManualMealViewModel(service)
            assertFalse(vm.canEstimate)
            vm.estimate("Fixture lunch"); runCurrent()
            assertEquals(0, requests)
            assertFalse(vm.estimating.value)
            assertNotNull(vm.estimateError.value)
            settings = com.kalotracker.app.core.settings.AiSettings("fixture-key")
            assertTrue(vm.canEstimate)
            vm.estimate("Fixture lunch"); runCurrent()
            assertEquals(1, requests)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun completionIsRetainedForRecreatedScreenAndRepeatedTapsWriteOnce() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val vm = ManualMealViewModel()
            val stored = CompletableDeferred<Unit>()
            var writes = 0
            repeat(3) { vm.save { writes++; stored.await() } }
            runCurrent()
            assertFalse(vm.isSaved.value)
            assertTrue(vm.saveOperation.status.value.busy)
            stored.complete(Unit)
            runCurrent()
            assertTrue(vm.isSaved.value) // a recreated screen can observe the completion
            vm.save { writes++ }
            runCurrent()
            assertEquals(1, writes)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun failedManualSaveDoesNotSignalNavigationAndAllowsRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val vm = ManualMealViewModel()
            vm.save { throw java.io.IOException("disk full") }
            runCurrent()
            assertFalse(vm.isSaved.value)
            assertNotNull(vm.saveOperation.status.value.error)
            vm.save {}
            runCurrent()
            assertTrue(vm.isSaved.value)
            assertNull(vm.saveOperation.status.value.error)
        } finally { Dispatchers.resetMain() }
    }
}
