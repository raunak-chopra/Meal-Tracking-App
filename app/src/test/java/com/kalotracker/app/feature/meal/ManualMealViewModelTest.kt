package com.kalotracker.app.feature.meal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ManualMealViewModelTest {
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
