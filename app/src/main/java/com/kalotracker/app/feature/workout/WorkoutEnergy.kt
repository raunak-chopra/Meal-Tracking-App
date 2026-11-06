package com.kalotracker.app.feature.workout

import kotlin.math.roundToInt

/**
 * Typical MET values (2024 Adult Compendium of Physical Activities), matched loosely by name.
 * Energy is time-based: more sets only matter through the minutes they take.
 */
internal fun workoutMet(name: String, cardio: Boolean): Double {
    val n = name.lowercase()
    return when {
        "run" in n || "jog" in n -> 8.3
        "bike" in n || "cycl" in n -> 6.8
        "walk" in n -> 3.5
        "swim" in n -> 6.0
        "row" in n && "barbell" !in n && "dumbbell" !in n -> 7.0
        cardio -> 6.0
        "crunch" in n -> 2.8
        "push-up" in n || "pushup" in n || "sit-up" in n || "squat" in n && "barbell" !in n -> 3.8
        else -> 3.5 // general resistance training
    }
}

/** Rough energy used: MET × 3.5 × kg ÷ 200 per minute. */
internal fun workoutEnergy(name: String, cardio: Boolean, minutes: Int, bodyWeightKg: Double): Int {
    if (minutes <= 0 || !bodyWeightKg.isFinite() || bodyWeightKg <= 0) return 0
    return (workoutMet(name, cardio) * 3.5 * bodyWeightKg / 200 * minutes).roundToInt()
}
