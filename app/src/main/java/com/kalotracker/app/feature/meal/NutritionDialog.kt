package com.kalotracker.app.feature.meal

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*

fun EditableFoodItem.correctedNutrition(kcal: Int, p: Float, c: Float, f: Float): EditableFoodItem {
    require(portionGrams.isFinite() && portionGrams > 0f)
    require(kcal >= 0 && listOf(p,c,f).all { it.isFinite() && it >= 0f })
    return copy(baseCaloriesPerGram = kcal / portionGrams, baseProteinPerGram = p / portionGrams,
        baseCarbsPerGram = c / portionGrams, baseFatPerGram = f / portionGrams, confidence = 1f)
}

@Composable fun NutritionDialog(item: EditableFoodItem, onDismiss: () -> Unit, onApply: (Int, Float, Float, Float) -> Unit) {
    var kcal by remember(item.id) { mutableStateOf(item.currentCalories.toString()) }
    var p by remember(item.id) { mutableStateOf(item.currentProtein.toString()) }
    var c by remember(item.id) { mutableStateOf(item.currentCarbs.toString()) }
    var f by remember(item.id) { mutableStateOf(item.currentFat.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Correct nutrition for ${item.portionGrams}g") }, text = {
        Column {
            OutlinedTextField(kcal, { kcal = it }, label = { Text("kcal") })
            OutlinedTextField(p, { p = it }, label = { Text("Protein g") })
            OutlinedTextField(c, { c = it }, label = { Text("Carbs g") })
            OutlinedTextField(f, { f = it }, label = { Text("Fat g") })
            error?.let { Text(it) }
        }
    }, confirmButton = { TextButton(onClick = {
        val k = kcal.toIntOrNull(); val values = listOf(p,c,f).map { it.toFloatOrNull() }
        if (k == null || k !in 0..100000 || values.any { it == null || !it.isFinite() || it !in 0f..10000f }) error = "Enter nonnegative nutrition values."
        else { onApply(k, values[0]!!, values[1]!!, values[2]!!); onDismiss() }
    }) { Text("Apply") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
