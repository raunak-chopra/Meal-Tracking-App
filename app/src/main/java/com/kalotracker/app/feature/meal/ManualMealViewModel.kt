package com.kalotracker.app.feature.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kalotracker.app.core.util.SaveOperation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Keeps an in-flight save and its completion alive across screen recreation. */
class ManualMealViewModel : ViewModel() {
    private val saved = MutableStateFlow(false)
    val isSaved = saved.asStateFlow()
    val saveOperation = SaveOperation()

    fun save(persist: suspend () -> Unit) {
        if (saved.value) return
        saveOperation.launch(viewModelScope, { saved.value = true }, persist)
    }
}
