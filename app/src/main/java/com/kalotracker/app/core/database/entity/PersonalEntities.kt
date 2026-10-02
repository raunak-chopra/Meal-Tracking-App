package com.kalotracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable @Entity(tableName = "day_status")
data class DayStatusEntity(@PrimaryKey val date: String, val complete: Boolean)

@Serializable @Entity(tableName = "goal_history")
data class GoalHistoryEntity(@PrimaryKey val date: String, val calories: Int, val protein: Int,
    val carbs: Int, val fat: Int, val waterMl: Int, val goal: String)

/** Nutrition for the recorded yield: one serving for a food/template, cooked batch for a recipe. */
@Serializable @Entity(tableName = "saved_foods")
data class SavedFoodEntity(@PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String, val kind: String = "FOOD", val yieldGrams: Float = 100f,
    val calories: Int, val protein: Float, val carbs: Float, val fat: Float,
    val favorite: Boolean = false, val ingredientsJson: String = "[]")

@Serializable @Entity(tableName = "barcode_cache")
data class BarcodeCacheEntity(@PrimaryKey val barcode: String, val payload: String, val cachedAt: Long)

@Serializable @Entity(tableName = "workout_routines")
data class WorkoutRoutineEntity(@PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String, val payload: String)
