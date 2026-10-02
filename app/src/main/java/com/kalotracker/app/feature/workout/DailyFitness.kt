package com.kalotracker.app.feature.workout

import kotlin.math.roundToInt

enum class FitnessEffort(val label: String) { EASY("Easy"), MODERATE("Moderate"), HARD("Hard") }

/** Rough gross energy: MET × 3.5 × kg / 200 × minutes.
 * 2024 Adult Compendium, conditioning activities 02020/02022/02024.
 * Total session time is allocated once; pace and rests make this approximate.
 */
internal fun dailyFitnessExercises(
    minutes: Int, weightKg: Double,
    effort: FitnessEffort = FitnessEffort.MODERATE,
    reps: List<Int> = listOf(20, 20, 20)
): List<WorkoutExercise> {
    require(minutes in 3..60)
    require(weightKg.isFinite() && weightKg in 20.0..300.0)
    require(reps.size == 3 && reps.all { it in 0..10000 } && reps.any { it > 0 })
    val selected = listOf("Push-ups", "Sit-ups", "Crunches").zip(reps).filter { it.second > 0 }
    return selected.mapIndexed { index, (name, count) ->
        val duration = minutes / selected.size + if (index < minutes % selected.size) 1 else 0
        val met = when (effort) {
            FitnessEffort.EASY -> 2.8
            FitnessEffort.MODERATE -> if (name == "Crunches") 2.8 else 3.8
            FitnessEffort.HARD -> 7.5
        }
        WorkoutExercise(name, sets = listOf(EditableSet(1, "0", count.toString())),
            durationMinutes = duration,
            calories = (met * 3.5 * weightKg / 200 * duration).roundToInt())
    }
}
