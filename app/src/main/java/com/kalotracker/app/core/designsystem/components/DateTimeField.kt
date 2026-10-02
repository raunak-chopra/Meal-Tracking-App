package com.kalotracker.app.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.designsystem.KaloBorder
import com.kalotracker.app.core.designsystem.KaloSurfaceElevated
import com.kalotracker.app.core.designsystem.KaloTextPrimary
import com.kalotracker.app.core.designsystem.KaloTypography
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Tappable chip showing a date-time; tapping opens a date picker, then a time picker. */
@Composable
fun DateTimeChip(
    millis: Long,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val zone = ZoneId.systemDefault()
    var step by remember { mutableStateOf(0) } // 0 closed, 1 date, 2 time
    var pickedDate by remember { mutableStateOf(LocalDate.now()) }

    val current = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)
    val today = LocalDate.now()
    val dayLabel = when (current.toLocalDate()) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> current.format(DateTimeFormatter.ofPattern("MMM d"))
    }

    Surface(
        onClick = {
            pickedDate = current.toLocalDate()
            step = 1
        },
        shape = RoundedCornerShape(12.dp),
        color = KaloSurfaceElevated,
        border = BorderStroke(1.dp, KaloBorder),
        modifier = modifier
    ) {
        Text(
            text = "$dayLabel, ${current.format(DateTimeFormatter.ofPattern("HH:mm"))}  ✎",
            style = KaloTypography.bodyMedium,
            color = KaloTextPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }

    if (step == 1) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = current.toLocalDate().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let {
                        pickedDate = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    step = 2
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("Cancel") } }
        ) { DatePicker(state = dateState) }
    }

    if (step == 2) {
        val timeState = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(onClick = {
                    val chosen = LocalDateTime.of(pickedDate, LocalTime.of(timeState.hour, timeState.minute))
                    onChange(chosen.atZone(zone).toInstant().toEpochMilli())
                    step = 0
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("Cancel") } },
            text = { TimePicker(state = timeState) }
        )
    }
}
