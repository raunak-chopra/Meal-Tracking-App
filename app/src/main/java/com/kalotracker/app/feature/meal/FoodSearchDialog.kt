package com.kalotracker.app.feature.meal

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.data.food.FoodCatalog
import com.kalotracker.app.core.data.food.FoodCatalogItem
import com.kalotracker.app.core.designsystem.KaloTextMuted
import com.kalotracker.app.core.designsystem.KaloTextPrimary
import com.kalotracker.app.core.designsystem.KaloTypography

/** Search the bundled catalog and pick a food to add to the current meal. */
@Composable
fun FoodSearchDialog(
    onSelect: (FoodCatalogItem) -> Unit,
    onDismiss: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { FoodCatalog.search(query) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Add a food") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search common foods") },
                    trailingIcon = if (query.isNotEmpty()) {
                        { IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear search") } }
                    } else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp).padding(top = 8.dp)) {
                    if (results.isEmpty()) {
                        item { Text("No match in the built-in list.", style = KaloTypography.bodyMedium, color = KaloTextMuted) }
                    }
                    items(results) { food ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable(role = Role.Button, onClickLabel = "Add ${food.name}") { keyboard?.hide(); onSelect(food); onDismiss() }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(food.name, style = KaloTypography.bodyLarge, color = KaloTextPrimary)
                            Text("${food.caloriesPer100g.toInt()} kcal/100g", style = KaloTypography.bodyMedium, color = KaloTextMuted)
                        }
                    }
                }
            }
        }
    )
}
