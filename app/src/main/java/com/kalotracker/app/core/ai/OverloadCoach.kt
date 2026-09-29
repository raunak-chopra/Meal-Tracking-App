package com.kalotracker.app.core.ai

import kotlin.math.abs

/** One completed set from a previous session. */
data class PastSet(val weightKg: Float, val reps: Int)

/**
 * Simple double-progression rule: once every set at your top weight reaches the rep ceiling,
 * add weight; otherwise keep the weight and add a rep to the weakest set.
 */
object OverloadCoach {

    const val REP_CEILING = 10
    const val WEIGHT_STEP_KG = 2.5f

    fun suggest(previous: List<PastSet>): String? {
        val sets = previous.filter { it.reps > 0 }
        if (sets.isEmpty()) return null

        val topWeight = sets.maxOf { it.weightKg }
        val summary = "Last time: ${describe(sets)}"

        if (topWeight <= 0f) {
            val weakest = sets.minOf { it.reps }
            return "$summary. Bodyweight: aim for ${weakest + 1}+ reps on every set."
        }

        val atTop = sets.filter { abs(it.weightKg - topWeight) < 0.01f }
        return if (atTop.all { it.reps >= REP_CEILING }) {
            "$summary. Every set hit $REP_CEILING reps: try ${fmt(topWeight + WEIGHT_STEP_KG)}kg this time."
        } else {
            val weakest = atTop.minOf { it.reps }
            "$summary. Keep ${fmt(topWeight)}kg and aim for ${minOf(weakest + 1, REP_CEILING)}+ reps on your weakest set."
        }
    }

    private fun describe(sets: List<PastSet>): String {
        val topWeight = sets.maxOf { it.weightKg }
        return if (sets.all { abs(it.weightKg - topWeight) < 0.01f }) {
            "${fmt(topWeight)}kg x ${sets.joinToString(", ") { it.reps.toString() }}"
        } else {
            sets.joinToString(", ") { "${fmt(it.weightKg)}kg x ${it.reps}" }
        }
    }

    private fun fmt(kg: Float): String =
        if (kg % 1f == 0f) kg.toInt().toString() else String.format(java.util.Locale.US, "%.1f", kg)
}
