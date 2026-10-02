package com.kalotracker.app.feature.meal

import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

interface MealDraftStore {
    suspend fun load(): MealScanUiState?
    suspend fun save(state: MealScanUiState)
    suspend fun clear()
}

internal object MealDraftCodec {
    private val json = Json { ignoreUnknownKeys = true }
    fun encode(state: MealScanUiState): String = json.encodeToString(MealScanUiState.serializer(),
        state.copy(isAnalyzing = false, isSaving = false, isSaved = false, draftLoading = false,
            resumePending = false, draftMessage = null, errorMessage = null))
    fun decode(value: String): MealScanUiState = json.decodeFromString(MealScanUiState.serializer(), value).also { s ->
        require(s.items.size <= 100 && s.cookingFatGrams.isFinite() && s.cookingFatGrams in 1f..100f)
        require(s.items.all { it.portionGrams.isFinite() && it.portionGrams in 1f..5000f &&
            listOf(it.baseCaloriesPerGram, it.baseProteinPerGram, it.baseCarbsPerGram, it.baseFatPerGram).all { n -> n.isFinite() && n >= 0 } })
    }
}

/** One device-local draft, excluded from exports and Android backup. */
class FileMealDraftStore(file: File) : MealDraftStore {
    private val atomic = AtomicFile(file)
    override suspend fun load(): MealScanUiState? = withContext(Dispatchers.IO) {
        try { atomic.openRead().use { MealDraftCodec.decode(it.bufferedReader().readText()) } }
        catch (_: java.io.FileNotFoundException) { null }
    }
    override suspend fun save(state: MealScanUiState) = withContext(Dispatchers.IO) {
        val stream = atomic.startWrite()
        try {
            stream.write(MealDraftCodec.encode(state).toByteArray(Charsets.UTF_8))
            atomic.finishWrite(stream)
        } catch (e: Exception) { atomic.failWrite(stream); throw e }
    }
    override suspend fun clear() = withContext(Dispatchers.IO) { atomic.delete() }
}
