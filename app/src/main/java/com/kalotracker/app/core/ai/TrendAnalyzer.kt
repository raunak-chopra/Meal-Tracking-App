package com.kalotracker.app.core.ai

import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.core.database.dao.WorkoutWithSets
import com.kalotracker.app.core.database.entity.WaterLogEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

data class DayTotals(
    val date: LocalDate,
    val calories: Int = 0,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val waterMl: Int = 0,
    val workoutCount: Int = 0,
    val mealCount: Int = 0,
    /** Calories from meals eaten at 21:00 or later. */
    val lateCalories: Int = 0,
    val complete: Boolean = true,
    val historicalTargets: NutritionTargets? = null
) {
    val isLogged: Boolean get() = mealCount > 0
}

data class NutritionTargets(val calories: Int, val protein: Int, val waterMl: Int)

data class TrendStats(
    val days: List<DayTotals>,
    val loggedDays: Int,
    val avgCalories: Int,
    val avgProtein: Int,
    val avgWaterMl: Int,
    val proteinHitDays: Int,
    val calorieOnTargetDays: Int,
    val workoutDays: Int,
    val streak: Int
)

object TrendAnalyzer {

    private const val LATE_HOUR = 21

    fun buildDays(
        start: LocalDate,
        end: LocalDate,
        meals: List<MealWithItems>,
        water: List<WaterLogEntity>,
        workouts: List<WorkoutWithSets>,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<DayTotals> {
        fun dateOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone)

        val mealsByDay = meals.groupBy { dateOf(it.meal.timestamp).toLocalDate() }
        val waterByDay = water.groupBy { dateOf(it.timestamp).toLocalDate() }
        val workoutsByDay = workouts.groupBy { dateOf(it.workout.timestamp).toLocalDate() }

        return generateSequence(start) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .map { day ->
                val dayMeals = mealsByDay[day].orEmpty()
                DayTotals(
                    date = day,
                    calories = dayMeals.sumOf { it.meal.totalCalories },
                    protein = dayMeals.map { it.meal.totalProteinGrams }.sum(),
                    carbs = dayMeals.map { it.meal.totalCarbsGrams }.sum(),
                    fat = dayMeals.map { it.meal.totalFatGrams }.sum(),
                    waterMl = waterByDay[day].orEmpty().sumOf { it.milliliters },
                    workoutCount = workoutsByDay[day].orEmpty().size,
                    mealCount = dayMeals.size,
                    lateCalories = dayMeals
                        .filter { dateOf(it.meal.timestamp).hour >= LATE_HOUR }
                        .sumOf { it.meal.totalCalories }
                )
            }.toList()
    }

    fun stats(days: List<DayTotals>, targets: NutritionTargets, today: LocalDate, requireComplete: Boolean = false): TrendStats {
        val logged = days.filter { it.isLogged && (!requireComplete || (it.complete && it.date < today)) }
        val n = logged.size
        return TrendStats(
            days = days,
            loggedDays = n,
            avgCalories = if (n == 0) 0 else logged.sumOf { it.calories } / n,
            avgProtein = if (n == 0) 0 else (logged.map { it.protein }.sum() / n).roundToInt(),
            avgWaterMl = if (requireComplete) { if (n == 0) 0 else logged.sumOf { it.waterMl } / n } else if (days.isEmpty()) 0 else days.sumOf { it.waterMl } / days.size,
            proteinHitDays = logged.count { val t = if (requireComplete) it.historicalTargets else targets; t != null && t.protein > 0 && it.protein >= t.protein * 0.9f },
            calorieOnTargetDays = logged.count {
                val t = if (requireComplete) it.historicalTargets else targets
                t != null && t.calories > 0 && abs(it.calories - t.calories) <= t.calories * 0.10f
            },
            workoutDays = days.count { it.workoutCount > 0 },
            streak = streak(if (requireComplete) days.filter { it.complete && it.date < today } else days, today)
        )
    }

    /** Consecutive logged days ending today (or yesterday, if today has nothing logged yet). */
    fun streak(days: List<DayTotals>, today: LocalDate): Int {
        val loggedDates = days.filter { it.isLogged }.map { it.date }.toSet()
        var cursor = if (today in loggedDates) today else today.minusDays(1)
        var count = 0
        while (cursor in loggedDates) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }

    /** Plain-language observations, most useful first (max 4). Returns nothing when data is too thin. */
    fun insights(stats: TrendStats, targets: NutritionTargets): List<String> {
        if (stats.loggedDays < 3) {
            return listOf("Log meals on at least 3 days to unlock trends. Right now there is not enough data to say anything reliable.")
        }
        val out = mutableListOf<String>()
        val logged = stats.days.filter { it.isLogged }

        if (targets.calories > 0) {
            val diff = stats.avgCalories - targets.calories
            when {
                diff > targets.calories * 0.10f ->
                    out += "You average ${stats.avgCalories} kcal on logged days, ${diff} over your ${targets.calories} target."
                stats.avgCalories < targets.calories * 0.75f ->
                    out += "You average only ${stats.avgCalories} kcal against a ${targets.calories} target. Either you are eating very little or some meals are not being logged."
            }
        }

        val proteinRate = stats.proteinHitDays.toFloat() / stats.loggedDays
        if (targets.protein > 0) {
            if (proteinRate < 0.5f) {
                out += "Protein was on target on ${stats.proteinHitDays} of ${stats.loggedDays} logged days (average ${stats.avgProtein}g vs ${targets.protein}g). Adding a protein source to your lightest meal is the easiest fix."
            } else if (proteinRate >= 0.8f) {
                out += "Strong protein consistency: on target ${stats.proteinHitDays} of ${stats.loggedDays} logged days."
            }
        }

        val weekend = logged.filter { it.date.dayOfWeek.value >= 6 }
        val weekday = logged.filter { it.date.dayOfWeek.value < 6 }
        if (weekend.size >= 2 && weekday.size >= 3) {
            val we = weekend.sumOf { it.calories } / weekend.size
            val wd = weekday.sumOf { it.calories } / weekday.size
            if (wd > 0 && we > wd * 1.15f) {
                out += "Weekends run about ${we - wd} kcal higher than weekdays ($we vs $wd)."
            }
        }

        val totalCals = logged.sumOf { it.calories }
        val lateCals = logged.sumOf { it.lateCalories }
        if (totalCals > 0 && lateCals.toFloat() / totalCals > 0.30f) {
            out += "${(lateCals * 100f / totalCals).roundToInt()}% of your calories are eaten after 9pm."
        }

        if (targets.waterMl > 0 && stats.avgWaterMl < targets.waterMl * 0.6f) {
            out += "Water averages ${stats.avgWaterMl} ml a day against a ${targets.waterMl} ml goal."
        }

        if (out.isEmpty()) out += "Nothing stands out. Calories, protein and timing are all close to your targets."
        return out.take(4)
    }
}

enum class GoalType(val label: String) {
    LOSE("Lose fat"),
    MAINTAIN("Maintain"),
    GAIN("Build muscle")
}

data class WeightPoint(val epochMillis: Long, val kg: Float)

data class GoalSuggestion(val message: String, val newCalorieTarget: Int? = null)

object GoalAdvisor {

    fun withEvidence(goal: GoalType, points: List<WeightPoint>, days: List<DayTotals>, currentTarget: Int): GoalSuggestion {
        val complete = days.filter { it.complete && it.isLogged }
        if (complete.size < 10) return GoalSuggestion("Mark at least 10 fully logged past days complete before adjusting your target.")
        if (complete.any { it.historicalTargets == null } || complete.map { it.historicalTargets }.distinct().size > 1)
            return GoalSuggestion("Keep a consistent goal and collect complete days before adjusting calories.")
        val onTarget = complete.count { abs(it.calories - currentTarget) <= currentTarget * 0.15f }
        if (onTarget < complete.size * 0.7f) return GoalSuggestion("Your logged intake varies from your target. Review consistency before changing the target.")
        val start = complete.minOf { it.date }.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = complete.maxOf { it.date }.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return suggest(goal, weeklyChangeKg(points.filter { it.epochMillis in start until end }), currentTarget)
    }

    private const val MIN_SPAN_DAYS = 10
    private const val MS_PER_WEEK = 7 * 24 * 60 * 60 * 1000.0

    /** Least-squares trend in kg per week over the given points, or null if the data is too thin. */
    fun weeklyChangeKg(points: List<WeightPoint>): Float? {
        if (points.size < 3) return null
        val sorted = points.sortedBy { it.epochMillis }
        val spanDays = (sorted.last().epochMillis - sorted.first().epochMillis) / (24 * 60 * 60 * 1000.0)
        if (spanDays < MIN_SPAN_DAYS) return null

        val t0 = sorted.first().epochMillis
        val xs = sorted.map { (it.epochMillis - t0) / MS_PER_WEEK }
        val ys = sorted.map { it.kg.toDouble() }
        val xMean = xs.average()
        val yMean = ys.average()
        val denom = xs.sumOf { (it - xMean) * (it - xMean) }
        if (denom == 0.0) return null
        val slope = xs.indices.sumOf { (xs[it] - xMean) * (ys[it] - yMean) } / denom
        return slope.toFloat()
    }

    fun suggest(goal: GoalType, weeklyChangeKg: Float?, currentTarget: Int): GoalSuggestion {
        if (weeklyChangeKg == null) {
            return GoalSuggestion("Log your weight a few times over at least ${MIN_SPAN_DAYS} days to get a calorie-target check.")
        }
        val rate = String.format(java.util.Locale.US, "%+.2f", weeklyChangeKg)
        fun adjust(delta: Int, why: String): GoalSuggestion {
            val target = (currentTarget + delta).coerceIn(1200, 5000)
            return GoalSuggestion("Your weight is trending $rate kg/week. $why Try $target kcal/day (${if (delta > 0) "+" else ""}$delta).", target)
        }
        return when (goal) {
            GoalType.LOSE -> when {
                weeklyChangeKg > -0.1f -> adjust(-150, "That is not enough loss for your goal.")
                weeklyChangeKg < -1.0f -> adjust(150, "That is faster than is sustainable and risks muscle loss.")
                else -> GoalSuggestion("Trending $rate kg/week: a steady pace for fat loss. Keep your current target.")
            }
            GoalType.MAINTAIN -> when {
                weeklyChangeKg > 0.3f -> adjust(-100, "You are gaining while aiming to maintain.")
                weeklyChangeKg < -0.3f -> adjust(100, "You are losing while aiming to maintain.")
                else -> GoalSuggestion("Trending $rate kg/week: weight is stable. Keep your current target.")
            }
            GoalType.GAIN -> when {
                weeklyChangeKg < 0.1f -> adjust(150, "That is not enough gain for your goal.")
                weeklyChangeKg > 0.6f -> adjust(-100, "That is faster than lean gain usually allows, so more of it may be fat.")
                else -> GoalSuggestion("Trending $rate kg/week: a lean pace for muscle gain. Keep your current target.")
            }
        }
    }
}
