package com.kalotracker.app.feature.meal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.KaloTextSecondary

fun EditableFoodItem.correctedNutrition(kcal: Int, p: Float, c: Float, f: Float): EditableFoodItem {
    require(portionGrams.isFinite() && portionGrams > 0f)
    require(kcal >= 0 && listOf(p,c,f).all { it.isFinite() && it >= 0f })
    return copy(baseCaloriesPerGram = kcal / portionGrams, baseProteinPerGram = p / portionGrams,
        baseCarbsPerGram = c / portionGrams, baseFatPerGram = f / portionGrams, confidence = 1f, nutritionBasisName = name)
}

/**
 * Energy the macros account for (4/4/9 kcal per gram), when it differs noticeably from the
 * calories entered. Labels vary a little (fibre, rounding), so small gaps are ignored.
 */
internal fun macroCalorieMismatch(kcal: Int, protein: Float, carbs: Float, fat: Float): Int? {
    val fromMacros = kotlin.math.round(protein * 4 + carbs * 4 + fat * 9).toInt()
    val tolerance = maxOf(20f, kcal * 0.15f)
    return fromMacros.takeIf { kotlin.math.abs(it - kcal) > tolerance }
}

@Composable fun NutritionDialog(item: EditableFoodItem, onDismiss: () -> Unit, onApply: (Int, Float, Float, Float) -> Unit) {
    var kcal by remember(item.id) { mutableStateOf(item.currentCalories.toString()) }
    var p by remember(item.id) { mutableStateOf(oneDecimal(item.currentProtein)) }
    var c by remember(item.id) { mutableStateOf(oneDecimal(item.currentCarbs)) }
    var f by remember(item.id) { mutableStateOf(oneDecimal(item.currentFat)) }
    var error by remember { mutableStateOf<String?>(null) }
    val number = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    val parsed = listOf(p, c, f).map { it.trim().replace(',', '.').toFloatOrNull() }
    val mismatch = kcal.trim().toIntOrNull()?.let { k ->
        if (parsed.all { it != null && it.isFinite() && it >= 0f }) macroCalorieMismatch(k, parsed[0]!!, parsed[1]!!, parsed[2]!!) else null
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Nutrition for ${formatAmount(item.portionGrams, item.unit)}") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(kcal, { kcal = it; error = null }, label = { Text("Calories") }, suffix = { Text("kcal") }, singleLine = true, keyboardOptions = number)
            OutlinedTextField(p, { p = it; error = null }, label = { Text("Protein") }, suffix = { Text("g") }, singleLine = true, keyboardOptions = number)
            OutlinedTextField(c, { c = it; error = null }, label = { Text("Carbs") }, suffix = { Text("g") }, singleLine = true, keyboardOptions = number)
            OutlinedTextField(f, { f = it; error = null }, label = { Text("Fat") }, suffix = { Text("g") }, singleLine = true, keyboardOptions = number)
            if (mismatch != null) {
                Text("These macros add up to about $mismatch kcal.", color = KaloTextSecondary)
                TextButton(onClick = { kcal = mismatch.toString() }) { Text("Use $mismatch kcal") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = {
        val k = kcal.trim().toIntOrNull()
        if (k == null || k !in 0..100000 || parsed.any { it == null || !it.isFinite() || it !in 0f..10000f }) error = "Use numbers of zero or more."
        else { onApply(k, parsed[0]!!, parsed[1]!!, parsed[2]!!); onDismiss() }
    }) { Text("Apply") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

private fun oneDecimal(value: Float): String = formatAmount(value, "").trim()
