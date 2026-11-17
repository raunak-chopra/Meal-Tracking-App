package com.kalotracker.app.feature.meal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Null when a quick entry is usable; protein can't supply more energy than the whole food. */
internal fun quickEntryProblem(name: String, kcal: Int?, protein: Float?): String? = when {
    name.isBlank() -> "Give it a name."
    kcal == null || kcal !in 0..100_000 -> "Enter calories as a whole number."
    protein == null || !protein.isFinite() || protein < 0f -> "Enter protein in grams (0 if unknown)."
    protein * 4f > kcal + 4 -> "${formatAmount(protein, "g")} protein is ${(protein * 4).toInt()} kcal on its own, more than $kcal kcal."
    else -> null
}

@Composable fun QuickFoodDialog(onDismiss: () -> Unit, onAdd: (String, Int, Float) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }
    val number = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Quick entry") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("For when you only know the calories. Carbs and fat are left at zero.")
            OutlinedTextField(name, { name = it; error = null }, label = { Text("Name") }, singleLine = true)
            OutlinedTextField(calories, { calories = it; error = null }, label = { Text("Calories") }, suffix = { Text("kcal") },
                singleLine = true, keyboardOptions = number)
            OutlinedTextField(protein, { protein = it; error = null }, label = { Text("Protein") }, suffix = { Text("g") },
                singleLine = true, keyboardOptions = number)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = {
        val c = calories.trim().toIntOrNull(); val p = protein.trim().replace(',', '.').toFloatOrNull()
        error = quickEntryProblem(name, c, p)
        if (error == null) onAdd(name.trim(), c!!, p!!)
    }) { Text("Add") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
