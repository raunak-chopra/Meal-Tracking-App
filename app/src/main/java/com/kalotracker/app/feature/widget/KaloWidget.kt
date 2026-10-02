package com.kalotracker.app.feature.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.kalotracker.app.core.designsystem.BrandPalette
import com.kalotracker.app.core.designsystem.WarmDark
import com.kalotracker.app.core.designsystem.WarmLight
import com.kalotracker.app.core.designsystem.components.EnergyDisplay
import com.kalotracker.app.MainActivity
import com.kalotracker.app.core.database.KaloDatabase
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class KaloWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = KaloDatabase.getInstance(context)
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

        var readFailed = false
        val meals = try {
            db.mealDao().getMealsForDayOnce(startOfDay, endOfDay)
        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel } catch (_: Exception) {
            readFailed = true
            emptyList()
        }

        val totalWater = try {
            db.waterDao().getTotalWaterForDayOnce(startOfDay, endOfDay)
        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel } catch (_: Exception) {
            readFailed = true
            0
        }

        val prefs = context.getSharedPreferences("kalo_user_prefs", Context.MODE_PRIVATE)
        val targetCalories = prefs.getInt("target_calories", 2200)
        val targetProtein = prefs.getInt("target_protein", 160)
        val targetCarbs = prefs.getInt("target_carbs", 220)
        val targetFat = prefs.getInt("target_fat", 70)
        val targetWater = prefs.getInt("target_water_ml", 2500)

        val consumedCalories = meals.sumOf { it.meal.totalCalories }
        val consumedProtein = meals.sumOf { it.meal.totalProteinGrams.toDouble() }.toInt()
        val consumedCarbs = meals.sumOf { it.meal.totalCarbsGrams.toDouble() }.toInt()
        val consumedFat = meals.sumOf { it.meal.totalFatGrams.toDouble() }.toInt()
        val remainingCalories = targetCalories - consumedCalories

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val snapMealIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_START_DESTINATION", "camera")
        }

        val mode = context.getSharedPreferences("kalo_app_settings", Context.MODE_PRIVATE).getString("appearance", "SYSTEM")
        val dark = mode == "DARK" || (mode == "SYSTEM" && context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES)
        val palette = if(dark) WarmDark else WarmLight
        provideContent {
            GlanceWidgetContent(
                palette = palette, readFailed = readFailed,
                remainingCalories = remainingCalories,
                consumedCalories = consumedCalories,
                targetCalories = targetCalories,
                protein = consumedProtein,
                targetProtein = targetProtein,
                carbs = consumedCarbs,
                targetCarbs = targetCarbs,
                fat = consumedFat,
                targetFat = targetFat,
                water = totalWater,
                targetWater = targetWater,
                openAppIntent = openAppIntent,
                snapMealIntent = snapMealIntent
            )
        }
    }
}

@Composable
private fun GlanceWidgetContent(
    palette: BrandPalette, readFailed: Boolean,
    remainingCalories: Int,
    consumedCalories: Int,
    targetCalories: Int,
    protein: Int,
    targetProtein: Int,
    carbs: Int,
    targetCarbs: Int,
    fat: Int,
    targetFat: Int,
    water: Int,
    targetWater: Int,
    openAppIntent: Intent,
    snapMealIntent: Intent
) {
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d"))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(palette.background))
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(openAppIntent))
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            // Header Row: Brand Tag + Date
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "KALO",
                    style = TextStyle(
                        color = ColorProvider(palette.accent),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = dateText,
                    style = TextStyle(
                        color = ColorProvider(palette.secondaryText),
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Main Calories Remaining Highlight
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = if(readFailed) "Logs unavailable" else "$consumedCalories kcal logged",
                        style = TextStyle(
                            color = ColorProvider(palette.text),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if(readFailed) "Open app to check" else EnergyDisplay(consumedCalories, targetCalories).deltaText,
                        style = TextStyle(
                            color = ColorProvider(palette.secondaryText),
                            fontSize = 11.sp
                        )
                    )
                }

                // Snap Meal Action Button
                Box(
                    modifier = GlanceModifier
                        .background(ColorProvider(palette.accent))
                        .cornerRadius(12.dp)
                        .height(48.dp)
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                        .clickable(actionStartActivity(snapMealIntent))
                ) {
                    Text(
                        text = "Add meal",
                        style = TextStyle(
                            color = ColorProvider(palette.onAccent),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(10.dp))

            // Macro & Hydration Row
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(palette.raised))
                    .cornerRadius(10.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MacroPillItem("P", "${protein}g", palette.protein, palette.text)
                Spacer(modifier = GlanceModifier.defaultWeight())
                MacroPillItem("C", "${carbs}g", palette.carbs, palette.text)
                Spacer(modifier = GlanceModifier.defaultWeight())
                MacroPillItem("F", "${fat}g", palette.fat, palette.text)
                Spacer(modifier = GlanceModifier.defaultWeight())
                MacroPillItem("Water", "${water}ml", palette.protein, palette.text)
            }
        }
    }
}

@Composable
private fun MacroPillItem(label: String, value: String, accentColor: Color, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = TextStyle(
                color = ColorProvider(accentColor),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = value,
            style = TextStyle(
                color = ColorProvider(textColor),
                fontSize = 11.sp
            )
        )
    }
}
