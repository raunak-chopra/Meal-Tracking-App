package com.kalotracker.app.feature.settings

import kotlin.math.roundToInt

/**
 * Keeps the calorie target and the macro targets in step.
 * Protein and carbs are 4 kcal/g, fat 9 kcal/g.
 */
internal data class MacroSplit(val protein: Float, val carbs: Float, val fat: Float) {
    /** Grams of each macro for [calories], keeping this share of energy. */
    fun gramsFor(calories: Int) = Triple(
        (calories * protein / 4f).roundToInt(),
        (calories * carbs / 4f).roundToInt(),
        (calories * fat / 9f).roundToInt()
    )

    fun percentText() = "Protein ${(protein * 100).roundToInt()}%  ·  Carbs ${(carbs * 100).roundToInt()}%  ·  Fat ${(fat * 100).roundToInt()}%"

    companion object {
        fun caloriesOf(protein: Int, carbs: Int, fat: Int) = protein * 4 + carbs * 4 + fat * 9

        /** Share of energy from each macro, or null when the macros add up to nothing. */
        fun of(protein: Int, carbs: Int, fat: Int): MacroSplit? {
            val total = caloriesOf(protein, carbs, fat).toFloat()
            if (total <= 0f) return null
            return MacroSplit(protein * 4 / total, carbs * 4 / total, fat * 9 / total)
        }
    }
}
