package com.kalotracker.app.core.data.food

import com.kalotracker.app.core.database.entity.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.roundToInt

@Serializable
data class Ingredient(val name: String, val grams: Float, val calories: Int,
    val protein: Float, val carbs: Float, val fat: Float)

object PersonalFood {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun ingredients(food: SavedFoodEntity): List<Ingredient> =
        json.decodeFromString(food.ingredientsJson)
    fun validate(food: SavedFoodEntity) {
        require(food.id.isNotBlank() && food.name.isNotBlank()) { "Enter a name." }
        require(food.kind in setOf("FOOD", "RECIPE", "TEMPLATE")) { "Unknown food type." }
        require(food.yieldGrams.isFinite() && food.yieldGrams > 0f) { "Enter a positive serving/cooked yield." }
        require(food.calories >= 0 && listOf(food.protein, food.carbs, food.fat).all { it.isFinite() && it >= 0 }) { "Nutrition must be nonnegative." }
        require(food.kind == "FOOD" || ingredients(food).isNotEmpty()) { "Add ingredients first." }
        ingredients(food).forEach {
            require(it.name.isNotBlank() && it.grams.isFinite() && it.grams > 0f && it.calories >= 0 &&
                listOf(it.protein,it.carbs,it.fat).all { v -> v.isFinite() && v >= 0f }) { "Invalid ingredient." }
        }
    }
    fun portion(food: SavedFoodEntity, grams: Float, mealId: String): List<FoodItemEntity> {
        validate(food)
        require(grams.isFinite() && grams > 0f) { "Enter a positive portion." }
        val scale = grams / food.yieldGrams
        require(scale.isFinite() && food.calories.toDouble() * scale <= Int.MAX_VALUE &&
            listOf(food.protein, food.carbs, food.fat).all { (it * scale).isFinite() }) { "Portion is too large." }
        val rows = if (food.kind == "TEMPLATE" && ingredients(food).isNotEmpty()) ingredients(food)
            else listOf(Ingredient(food.name, food.yieldGrams, food.calories, food.protein, food.carbs, food.fat))
        return rows.map { FoodItemEntity(UUID.randomUUID().toString(), mealId, it.name,
            it.grams * scale, (it.calories * scale).roundToInt(), it.protein * scale, it.carbs * scale, it.fat * scale) }
    }
    fun recipe(name: String, cookedYield: Float, rows: List<Ingredient>, id: String = UUID.randomUUID().toString()) =
        SavedFoodEntity(id, name, "RECIPE", cookedYield, rows.sumOf { it.calories },
            rows.sumOf { it.protein.toDouble() }.toFloat(), rows.sumOf { it.carbs.toDouble() }.toFloat(),
            rows.sumOf { it.fat.toDouble() }.toFloat(), ingredientsJson = json.encodeToString(rows)).also {
                require(rows.isNotEmpty()) { "Add ingredients first." }; validate(it)
            }
}
