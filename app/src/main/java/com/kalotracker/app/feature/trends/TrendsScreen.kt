package com.kalotracker.app.feature.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kalotracker.app.core.ai.DayTotals
import com.kalotracker.app.core.database.entity.WeightLogEntity
import com.kalotracker.app.core.designsystem.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TrendsScreen(
    viewModel: TrendsViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = KaloBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Progress", style = KaloTypography.headlineMedium, color = KaloTextPrimary, modifier = Modifier.weight(1f))
                IconButton(onClick=onOpenSettings,modifier=Modifier.size(48.dp)) { Icon(Icons.Default.Settings,"Settings",tint=KaloTextPrimary) }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 30).forEach { days ->
                        FilterChip(
                            selected = state.rangeDays == days,
                            onClick = { viewModel.setRange(days) },
                            label = { Text("Last $days days") }
                        )
                    }
                }
            }

            if (!state.isLoading) item {
                Surface(shape = RoundedCornerShape(16.dp), color = KaloSurfaceElevated) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your habits · last ${state.rangeDays} days", style = KaloTypography.titleLarge)
                        Text("Meals recorded on ${state.habits.mealDays} days")
                        Text("${state.habits.sessions} fitness sessions · ${state.habits.minutes} minutes")
                        state.habits.reps.forEach { (exercise, reps) -> Text("$exercise · $reps completed reps") }
                        Text("Recording a meal builds the habit. Mark a day complete only when all its food is logged; nutrition averages use complete past days.", style = KaloTypography.bodySmall)
                    }
                }
            }

            val stats = state.stats
            if (stats != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        StatTile("Streak", "${stats.streak}d", Modifier.weight(1f))
                        StatTile("Complete", "${stats.loggedDays}/${state.rangeDays}", Modifier.weight(1f))
                        StatTile("Avg kcal", if (stats.loggedDays == 0) "-" else "${stats.avgCalories}", Modifier.weight(1f))
                        StatTile(
                            "Protein hit",
                            if (stats.loggedDays == 0) "-" else "${stats.proteinHitDays}/${stats.loggedDays}",
                            Modifier.weight(1f)
                        )
                    }
                }

                item {
                    ChartCard(
                        title = "CALORIES PER DAY",
                        subtitle = "Historical daily targets; faint bars are partial",
                        days = stats.days,
                        value = { it.calories.toFloat() },
                        dailyTarget = { it.historicalTargets?.calories?.toFloat() },
                        target = state.targets.calories.toFloat(),
                        color = KaloCalories
                    )
                }
                item {
                    ChartCard(
                        title = "PROTEIN PER DAY (G)",
                        subtitle = "Historical daily targets; faint bars are partial",
                        days = stats.days,
                        value = { it.protein },
                        dailyTarget = { it.historicalTargets?.protein?.toFloat() },
                        target = state.targets.protein.toFloat(),
                        color = KaloProtein
                    )
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().background(KaloSurface, RoundedCornerShape(16.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("WHAT STANDS OUT", style = KaloTypography.labelSmall, color = KaloTextSecondary)
                        state.insights.forEach { line ->
                            Text("·  $line", style = KaloTypography.bodyMedium, color = KaloTextPrimary)
                        }
                    }
                }

                item { AiSummaryCard(state, viewModel, onOpenSettings) }
            }

            item { WeightCard(state, viewModel) }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(KaloSurface, RoundedCornerShape(14.dp)).padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = KaloTypography.titleMedium, color = KaloTextPrimary)
        Text(label, style = KaloTypography.labelSmall, color = KaloTextMuted)
    }
}

@Composable
private fun ChartCard(
    title: String,
    subtitle: String,
    days: List<DayTotals>,
    value: (DayTotals) -> Float,
    dailyTarget: (DayTotals) -> Float?,
    target: Float,
    color: Color
) {
    val dateFmt = DateTimeFormatter.ofPattern("MMM d")
    val emptyColor = KaloBorder
    Column(
        modifier = Modifier.fillMaxWidth().background(KaloSurface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = KaloTypography.labelSmall, color = KaloTextSecondary)
        Text(subtitle, style = KaloTypography.bodyMedium, color = KaloTextMuted)
        val targetLineColor = KaloTextSecondary
        Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
            val maxValue = maxOf(target * 1.25f, days.maxOfOrNull { value(it) } ?: 0f, 1f)
            val n = days.size.coerceAtLeast(1)
            val slot = size.width / n
            val barWidth = slot * 0.62f
            days.forEachIndexed { i, day ->
                val v = value(day)
                val h = (v / maxValue) * size.height
                val left = i * slot + (slot - barWidth) / 2f
                if (day.isLogged) {
                    drawRect(if (day.complete) color else color.copy(alpha = 0.35f), topLeft = Offset(left, size.height - h), size = Size(barWidth, h))
                } else {
                    // Unlogged day: thin stub so gaps are visible rather than looking like 0 intake.
                    drawRect(emptyColor, topLeft = Offset(left, size.height - 3.dp.toPx()), size = Size(barWidth, 3.dp.toPx()))
                }
            }
            days.forEachIndexed { i, day ->
                dailyTarget(day)?.let { t ->
                    val y = size.height - (t / maxValue) * size.height
                    drawLine(targetLineColor, Offset(i * slot, y), Offset((i + 1) * slot, y),
                        strokeWidth = 1.5.dp.toPx())
                }
            }
        }
        if (days.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(days.first().date.format(dateFmt), style = KaloTypography.labelSmall, color = KaloTextMuted)
                Text(days.last().date.format(dateFmt), style = KaloTypography.labelSmall, color = KaloTextMuted)
            }
        }
    }
}

@Composable
private fun AiSummaryCard(state: TrendsUiState, viewModel: TrendsViewModel, onOpenSettings: () -> Unit) {
    val enoughData = (state.stats?.loggedDays ?: 0) >= 3
    Column(
        modifier = Modifier.fillMaxWidth().background(KaloSurface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("AI WEEKLY SUMMARY", style = KaloTypography.labelSmall, color = KaloTextSecondary)
        when {
            !state.aiConfigured -> {
                Text(
                    "Add your Gemini key in Settings to get a written summary of your patterns.",
                    style = KaloTypography.bodyMedium,
                    color = KaloTextMuted
                )
                OutlinedButton(onClick = onOpenSettings) { Text("Open settings") }
            }
            !enoughData -> Text(
                "Log at least 3 days first.",
                style = KaloTypography.bodyMedium,
                color = KaloTextMuted
            )
            else -> {
                if (!state.aiSummary.isNullOrBlank()) {
                    Text(state.aiSummary ?: "", style = KaloTypography.bodyMedium, color = KaloTextPrimary)
                    Text(
                        "AI-generated from your totals. Not medical advice.",
                        style = KaloTypography.labelSmall,
                        color = KaloTextMuted
                    )
                }
                if (!state.aiError.isNullOrBlank()) {
                    Text(state.aiError ?: "", style = KaloTypography.bodyMedium, color = KaloFat)
                }
                Text("Generating sends daily totals and goals to Google Gemini with your key. Saved records stay on this device.", style = KaloTypography.bodySmall, color = KaloTextSecondary)
                OutlinedButton(onClick = { viewModel.generateAiSummary() }, enabled = !state.isSummarizing) {
                    Text(
                        when {
                            state.isSummarizing -> "Writing..."
                            state.aiSummary != null -> "Regenerate"
                            else -> "Generate summary"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightCard(state: TrendsUiState, viewModel: TrendsViewModel) {
    val dateFmt = DateTimeFormatter.ofPattern("MMM d")
    val zone = ZoneId.systemDefault()
    Column(
        modifier = Modifier.fillMaxWidth().background(KaloSurface, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("BODY WEIGHT", style = KaloTypography.labelSmall, color = KaloTextSecondary)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.weightInput,
                onValueChange = viewModel::setWeightInput,
                label = { Text("Weight today (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { viewModel.logWeight() }, enabled = state.weightInput.isNotBlank()) { Text("Log") }
        }
        if (!state.weightError.isNullOrBlank()) {
            Text(state.weightError ?: "", style = KaloTypography.bodyMedium, color = KaloFat)
        }

        if (state.weights.size >= 2) {
            WeightLineChart(state.weights.take(30).reversed())
        }

        state.weights.take(5).forEach { w ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${Instant.ofEpochMilli(w.timestamp).atZone(zone).format(dateFmt)}   ${String.format(java.util.Locale.US, "%.1f", w.weightKg)} kg",
                    style = KaloTypography.bodyMedium,
                    color = KaloTextPrimary
                )
                IconButton(onClick = { viewModel.deleteWeight(w) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Delete weight entry", tint = KaloTextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }

        state.goalSuggestion?.let { suggestion ->
            HorizontalDivider(color = KaloBorder)
            Text("CALORIE TARGET CHECK  (${state.profile.goal.label})", style = KaloTypography.labelSmall, color = KaloProtein)
            Text(suggestion.message, style = KaloTypography.bodyMedium, color = KaloTextPrimary)
            if (suggestion.newCalorieTarget != null) {
                OutlinedButton(onClick = { viewModel.applySuggestedTarget() }) {
                    Text("Apply ${suggestion.newCalorieTarget} kcal")
                }
            }
            Text(
                "Weight trends are noisy day to day; this uses the trend over your last 6 weeks. Change your goal type in Settings.",
                style = KaloTypography.labelSmall,
                color = KaloTextMuted
            )
        }
    }
}

@Composable
private fun WeightLineChart(oldestFirst: List<WeightLogEntity>) {
    val lineColor = KaloSteps
    Canvas(modifier = Modifier.fillMaxWidth().height(110.dp)) {
        val values = oldestFirst.map { it.weightKg }
        val lo = values.min()
        val hi = values.max()
        val range = (hi - lo).coerceAtLeast(0.5f)
        val pad = 8.dp.toPx()
        val firstTime = oldestFirst.first().timestamp
        val span = (oldestFirst.last().timestamp - firstTime).coerceAtLeast(1L)
        fun point(i: Int) = Offset(
            pad + ((oldestFirst[i].timestamp - firstTime).toDouble() / span).toFloat() * (size.width - 2 * pad),
            pad + (1f - (values[i] - lo) / range) * (size.height - 2 * pad)
        )
        for (i in 0 until values.size - 1) {
            drawLine(lineColor, point(i), point(i + 1), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        }
        values.indices.forEach { drawCircle(lineColor, radius = 3.5.dp.toPx(), center = point(it)) }
    }
}
