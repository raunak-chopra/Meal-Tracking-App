package com.kalotracker.app.core.util

/** Technical input bounds, not recommended nutrition targets. */
fun validateGoalInputs(calories: String, protein: String, carbs: String, fat: String,
                       steps: String, water: String): String? {
    val fields = listOf(
        Triple("Calories", calories, 1L..100_000L),
        Triple("Protein", protein, 0L..10_000L),
        Triple("Carbs", carbs, 0L..10_000L),
        Triple("Fat", fat, 0L..10_000L),
        Triple("Steps", steps, 0L..1_000_000L),
        Triple("Water", water, 0L..100_000L)
    )
    return fields.firstOrNull { (_, text, range) -> text.toLongOrNull()?.let { it in range } != true }
        ?.let { (name, _, range) -> "$name must be a whole number between ${range.first} and ${range.last}." }
}

/** A finite positive food portion, bounded to the editor's supported range. */
fun parseFoodPortion(text: String): Float? = text.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f && it <= 5000f }
