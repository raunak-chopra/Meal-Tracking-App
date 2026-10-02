package com.kalotracker.app.feature.meal

/** Relative portions keep estimated nutrition proportional; no scale is required. */
internal fun EditableFoodItem.scaledPortion(multiplier: Float): EditableFoodItem {
    require(multiplier.isFinite() && multiplier > 0f)
    return copy(portionGrams = (portionGrams * multiplier).coerceIn(1f, 5000f))
}

enum class CookingFat(val label: String, val kcalPerGram: Float, val fatPerGram: Float) {
    OIL("Cooking oil", 8.57f, 1f),
    BUTTER("Butter", 7.17f, 0.81f)
}
