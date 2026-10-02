package com.kalotracker.app.navigation

object KaloDestinations {
    const val DASHBOARD = "dashboard"
    const val MEALS = "meals"
    const val CAMERA = "camera"
    const val MANUAL_MEAL = "manual_meal"
    const val EDIT_WORKOUT = "edit_workout/{workoutId}"
    fun editWorkout(id: String) = "edit_workout/$id"
    const val WORKOUT = "workout"
    const val HEALTH_PERMISSIONS = "health_permissions"
    const val SETTINGS = "settings"
    const val BARCODE_SCANNER = "barcode_scanner"
    const val PERSONAL_FOODS = "personal_foods"
    const val TRENDS = "trends"
    const val EDIT_MEAL = "edit_meal/{mealId}"

    fun editMeal(mealId: String) = "edit_meal/$mealId"
}
