package com.kalotracker.app.core.designsystem.components

data class EnergyDisplay(val logged: Int,val target: Int) {
    val progress: Float get() = if(target > 0) (logged.toFloat()/target).coerceIn(0f,1f) else 0f
    val deltaText: String get() = when {
        target <= 0 -> "Target unavailable"
        logged < target -> "${target.toLong()-logged} kcal to target"
        logged > target -> "${logged.toLong()-target} kcal above target"
        else -> "At your target"
    }
}
