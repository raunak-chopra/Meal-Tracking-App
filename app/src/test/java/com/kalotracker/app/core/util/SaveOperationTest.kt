package com.kalotracker.app.core.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SaveOperationTest {
    @Test fun waitsForStorageAndIgnoresRepeatedTaps() = runTest {
        val operation = SaveOperation()
        val stored = CompletableDeferred<Unit>()
        var writes = 0
        var navigations = 0
        repeat(3) {
            operation.launch(this, { navigations++ }) { writes++; stored.await() }
        }
        assertTrue(operation.status.value.busy)
        runCurrent()
        assertEquals(1, writes)
        assertEquals(0, navigations)
        stored.complete(Unit)
        runCurrent()
        assertEquals(1, navigations)
        assertFalse(operation.status.value.busy)
    }

    @Test fun failureStaysOnScreenAndAllowsRetry() = runTest {
        val operation = SaveOperation()
        var navigations = 0
        operation.launch(this, { navigations++ }) { throw java.io.IOException("disk full") }
        runCurrent()
        assertEquals(0, navigations)
        assertNotNull(operation.status.value.error)
        assertFalse(operation.status.value.busy)
        operation.launch(this, { navigations++ }) {}
        runCurrent()
        assertEquals(1, navigations)
        assertNull(operation.status.value.error)
    }

    @Test fun cancellationDoesNotReportSuccessOrStorageFailure() = runTest {
        val operation = SaveOperation()
        var navigations = 0
        operation.launch(this, { navigations++ }) { throw CancellationException() }
        runCurrent()
        assertEquals(0, navigations)
        assertEquals(SaveStatus(), operation.status.value)
    }
}
