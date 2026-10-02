package com.kalotracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: String, // "STRENGTH", "CARDIO"
    val durationMinutes: Int = 0,
    val estimatedCaloriesBurned: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    @androidx.room.ColumnInfo(defaultValue = "'[]'") val exercisesJson: String = "[]"
)

@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["workoutId"])]
)
data class ExerciseSetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val exerciseName: String,
    val setNumber: Int,
    val weightKg: Float,
    val reps: Int,
    val isCompleted: Boolean = true
)
