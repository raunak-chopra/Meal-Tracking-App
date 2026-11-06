package com.kalotracker.app.core.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WaterIntakeCard(
    currentWaterMl: Int,
    targetWaterMl: Int = 2500,
    onAddWater: (Int) -> Unit,
    onUndoWater: () -> Unit,
    modifier: Modifier = Modifier,
    onEditTarget: (() -> Unit)? = null,
    entryDate: String = ""
) {
    val haptics = LocalHapticFeedback.current
    var custom by rememberSaveable(entryDate) { mutableStateOf(false) }
    var amount by rememberSaveable(entryDate) { mutableStateOf("") }
    val valid = amount.toIntOrNull()?.takeIf { it > 0 && currentWaterMl.toLong() + it <= Int.MAX_VALUE.toLong() }
    val animatedWater by animateIntAsState(
        targetValue = currentWaterMl,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "waterMl"
    )
    val targetFraction = if (targetWaterMl > 0) (currentWaterMl.toFloat() / targetWaterMl).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "waterProgress"
    )
    if (custom) AlertDialog(
        onDismissRequest = { custom = false },
        title = { Text("Add water") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Amount in milliliters for this day")
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = amount.isNotEmpty() && valid == null
                )
                if (amount.isNotEmpty() && valid == null) {
                    Text(
                        "Enter a positive whole amount in ml. This amount may be too large.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid != null,
                onClick = {
                    valid?.let {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAddWater(it)
                    }
                    custom = false
                    amount = ""
                }
            ) { Text("Add water") }
        },
        dismissButton = { TextButton(onClick = { custom = false }) { Text("Cancel") } }
    )
    Column(
        modifier
            .fillMaxWidth()
            .background(KaloSurface, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Water", style = KaloTypography.titleMedium, color = KaloTextPrimary, modifier = Modifier.semantics { heading() })
            if (targetWaterMl > 0) {
                Text(
                    "${(currentWaterMl.toLong() * 100 / targetWaterMl)}%",
                    style = KaloTypography.labelMedium.copy(fontFeatureSettings = TabularFontFeature),
                    color = KaloWater
                )
            }
        }
        Text(
            if (targetWaterMl > 0) "%,d ml / %,d ml target".format(animatedWater, targetWaterMl)
            else "%,d ml · target unavailable".format(animatedWater),
            style = KaloTypography.headlineSmall.copy(fontFeatureSettings = TabularFontFeature),
            color = KaloTextPrimary
        )
        if (targetWaterMl > 0) LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier.fillMaxWidth().height(6.dp).semantics { contentDescription = "Water toward your target" },
            color = KaloWater,
            trackColor = KaloDivider,
            strokeCap = StrokeCap.Round
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(250, 500, 750).forEach { ml ->
                FilledTonalButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAddWater(ml)
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = KaloSurfaceElevated,
                        contentColor = KaloTextPrimary
                    )
                ) { Text("+$ml ml", style = KaloTypography.labelMedium.copy(fontFeatureSettings = TabularFontFeature)) }
            }
            OutlinedButton(
                onClick = { custom = true },
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Custom") }
        }
        if (currentWaterMl > 0 || onEditTarget != null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentWaterMl > 0) TextButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onUndoWater()
                    },
                    modifier = Modifier.heightIn(min = 40.dp)
                ) { Text("Undo last entry") }
                onEditTarget?.let {
                    TextButton(onClick = it, modifier = Modifier.heightIn(min = 40.dp)) { Text("Edit target") }
                }
            }
        }
    }
}
