package com.kalotracker.app.feature.meal

/** Relative portions keep estimated nutrition proportional; no scale is required. */
internal fun EditableFoodItem.scaledPortion(multiplier: Float): EditableFoodItem {
    require(multiplier.isFinite() && multiplier > 0f)
    return copy(portionGrams = (portionGrams * multiplier).coerceIn(1f, 5000f), amountInput = null)
}

/** Same range as photo/edit controls and durable photo drafts; never clamps typed input. */
internal fun parseEditablePortion(text: String): Float? =
    text.toFloatOrNull()?.takeIf { it.isFinite() && it in 1f..5000f }

internal fun EditableFoodItem.withPortionInput(text: String): EditableFoodItem {
    val raw = text.take(32)
    return copy(amountInput = raw, portionGrams = parseEditablePortion(raw) ?: portionGrams)
}

enum class CookingFat(val label: String, val kcalPerGram: Float, val fatPerGram: Float) {
    OIL("Cooking oil", 8.57f, 1f),
    BUTTER("Butter", 7.17f, 0.81f)
}

/** "180 g" or "250 ml", without a trailing ".0". */
internal fun formatAmount(amount: Float, unit: String): String {
    val rounded = kotlin.math.round(amount * 10f) / 10f
    val text = if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
    return "$text $unit"
}
