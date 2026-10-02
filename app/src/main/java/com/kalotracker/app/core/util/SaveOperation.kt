package com.kalotracker.app.core.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SaveStatus(val busy: Boolean = false, val error: String? = null)

/** Owns the synchronous tap guard, persistence result and retry state for a screen. */
class SaveOperation(private val onStatus: (SaveStatus) -> Unit = {}) {
    private val mutableStatus = MutableStateFlow(SaveStatus())
    val status = mutableStatus.asStateFlow()

    private fun publish(status: SaveStatus) {
        mutableStatus.value = status
        onStatus(status)
    }

    fun launch(scope: CoroutineScope, onSuccess: () -> Unit, persist: suspend () -> Unit) {
        if (mutableStatus.value.busy) return
        publish(SaveStatus(busy = true))
        scope.launch {
            try {
                persist()
            } catch (cancelled: CancellationException) {
                publish(SaveStatus())
                throw cancelled
            } catch (_: Exception) {
                publish(SaveStatus(error = "Could not save. Your entry is still here. Please try again."))
                return@launch
            }
            publish(SaveStatus())
            onSuccess()
        }
    }
}
