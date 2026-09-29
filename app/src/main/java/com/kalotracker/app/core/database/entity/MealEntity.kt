package com.kalotracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val totalCalories: Int,
    val totalProteinGrams: Float,
    val totalCarbsGrams: Float,
    val totalFatGrams: Float,
    val imageLocalUri: String? = null,
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "food_items",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mealId"])]
)
data class FoodItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val mealId: String,
    val name: String,
    val portionGrams: Float,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val confidence: Float = 1.0f
)
