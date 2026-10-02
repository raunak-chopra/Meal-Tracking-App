package com.kalotracker.app.feature.meal

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

@Composable fun QuickFoodDialog(onDismiss: () -> Unit, onAdd: (String, Int, Float) -> Unit) {
    var name by rememberSaveable { mutableStateOf("Quick estimate") }
    var calories by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Quick calorie / protein entry") }, text = {
        Column {
            Text("An estimate; carbs and fat are unknown and recorded as zero. Portion is one entry.")
            OutlinedTextField(name, { name = it }, label = { Text("Name") })
            OutlinedTextField(calories, { calories = it }, label = { Text("kcal") })
            OutlinedTextField(protein, { protein = it }, label = { Text("Protein g") })
            error?.let { Text(it) }
        }
    }, confirmButton = { TextButton(onClick = {
        val c = calories.toIntOrNull(); val p = protein.toFloatOrNull()
        if (c == null || c !in 0..100000 || p == null || !p.isFinite() || p !in 0f..10000f || name.isBlank()) error = "Enter a name and nonnegative nutrition values."
        else onAdd(name, c, p)
    }) { Text("Add") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
