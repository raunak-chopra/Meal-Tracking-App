package com.kalotracker.app.core.ai

import kotlin.math.roundToInt

enum class Sex(val label: String) { FEMALE("Female"), MALE("Male") }

enum class ActivityLevel(val label: String, val detail: String, val multiplier: Float) {
    SEDENTARY("Mostly sitting", "Desk work, little exercise", 1.2f),
    LIGHT("Lightly active", "Exercise 1–3 days a week", 1.375f),
    MODERATE("Active", "Exercise 3–5 days a week", 1.55f),
    VERY("Very active", "Hard exercise 6–7 days a week", 1.725f),
    EXTRA("Physical job + training", "Manual work or twice-a-day training", 1.9f)
}

/** What the user tells us about themselves; used only to suggest targets. */
data class BodyProfile(
    val sex: Sex,
    val age: Int,
    val heightCm: Float,
    val weightKg: Float,
    val activity: ActivityLevel
) {
    /** Null when usable, otherwise a short message for the form. */
    fun problem(): String? = when {
        age !in 15..100 -> "Age must be between 15 and 100."
        !heightCm.isFinite() || heightCm !in 120f..230f -> "Height must be between 120 and 230 cm."
        !weightKg.isFinite() || weightKg !in 30f..300f -> "Weight must be between 30 and 300 kg."
        else -> null
    }

    /** Keep older stored profiles readable; automatic suggestions have a narrower adult scope. */
    fun automaticTargetProblem(): String? = problem()
        ?: if (age < 18) "Automatic calorie targets are for adults 18 and over. You can still use the journal and manual targets." else null
}

data class SuggestedTargets(
    val restingCalories: Int,
    val maintenanceCalories: Int,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val waterMl: Int
)

/**
 * Starting targets from height, weight, age, sex and activity.
 *
 * Resting energy uses the Mifflin-St Jeor equation; maintenance multiplies it by the activity level.
 * Lose fat is a 20% deficit, never below resting energy or 1200/1500 kcal; build muscle is a 10% surplus.
 * Protein is per kg (capped at the weight for a BMI of 25), fat is 27% of energy, carbs fill the rest.
 */
object TargetCalculator {

    fun restingCalories(body: BodyProfile): Float =
        10f * body.weightKg + 6.25f * body.heightCm - 5f * body.age + if (body.sex == Sex.MALE) 5f else -161f

    fun suggest(body: BodyProfile, goal: GoalType): SuggestedTargets {
        require(body.automaticTargetProblem() == null) { body.automaticTargetProblem()!! }
        val resting = restingCalories(body)
        val maintenance = resting * body.activity.multiplier
        val floor = minimumCalories(body)
        val calories = when (goal) {
            // Round the floor up so rounding can never take the target below it.
            GoalType.LOSE -> maxOf(roundTo(maintenance * 0.8f, 10), floor)
            GoalType.MAINTAIN -> roundTo(maintenance, 10)
            GoalType.GAIN -> roundTo(maintenance * 1.1f, 10)
        }

        val heightM = body.heightCm / 100f
        val proteinWeight = minOf(body.weightKg, 25f * heightM * heightM)
        val proteinPerKg = when (goal) { GoalType.LOSE -> 2.0f; GoalType.MAINTAIN -> 1.6f; GoalType.GAIN -> 1.8f }
        val protein = (proteinWeight * proteinPerKg).roundToInt()
        val fat = (calories * 0.27f / 9f).roundToInt()
        val carbs = ((calories - protein * 4 - fat * 9) / 4f).roundToInt().coerceAtLeast(0)
        val water = roundTo(body.weightKg * 35f, 100).coerceIn(1500, 5000)

        return SuggestedTargets(resting.roundToInt(), roundTo(maintenance, 10), calories, protein, carbs, fat, water)
    }

    /** Shared existing formula guardrail for initial loss targets and later target checks. */
    fun minimumCalories(body: BodyProfile): Int {
        require(body.automaticTargetProblem() == null) { body.automaticTargetProblem()!! }
        val floor = maxOf(restingCalories(body), if (body.sex == Sex.MALE) 1500f else 1200f)
        return kotlin.math.ceil(floor / 10f).toInt() * 10
    }

    private fun roundTo(value: Float, step: Int): Int = (value / step).roundToInt() * step
}
