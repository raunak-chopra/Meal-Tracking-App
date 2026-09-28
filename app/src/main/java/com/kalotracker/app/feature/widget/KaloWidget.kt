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

        val meals = try {
            db.mealDao().getMealsForDayOnce(startOfDay, endOfDay)
        } catch (_: Exception) {
            emptyList()
        }

        val totalWater = try {
            db.waterDao().getTotalWaterForDayOnce(startOfDay, endOfDay)
        } catch (_: Exception) {
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

        provideContent {
            GlanceWidgetContent(
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
            .background(ColorProvider(Color(0xFF14161F)))
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
                        color = ColorProvider(Color(0xFF38BDF8)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = dateText,
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF94A3B8)),
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
                        text = if (remainingCalories >= 0) "${remainingCalories} kcal" else "+${-remainingCalories} over",
                        style = TextStyle(
                            color = ColorProvider(if (remainingCalories >= 0) Color.White else Color(0xFFF43F5E)),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (remainingCalories >= 0) "remaining of $targetCalories" else "goal: $targetCalories",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF64748B)),
                            fontSize = 11.sp
                        )
                    )
                }

                // Snap Meal Action Button
                Box(
                    modifier = GlanceModifier
                        .background(ColorProvider(Color(0xFF38BDF8)))
                        .cornerRadius(12.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable(actionStartActivity(snapMealIntent))
                ) {
                    Text(
                        text = "Snap Meal",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF090A0F)),
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
                    .background(ColorProvider(Color(0xFF1D212E)))
                    .cornerRadius(10.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MacroPillItem("P", "${protein}g", Color(0xFF38BDF8))
                Spacer(modifier = GlanceModifier.defaultWeight())
                MacroPillItem("C", "${carbs}g", Color(0xFFFBBF24))
                Spacer(modifier = GlanceModifier.defaultWeight())
                MacroPillItem("F", "${fat}g", Color(0xFFF43F5E))
                Spacer(modifier = GlanceModifier.defaultWeight())
                MacroPillItem("H₂O", "${water}ml", Color(0xFF06B6D4))
            }
        }
    }
}

@Composable
private fun MacroPillItem(label: String, value: String, accentColor: Color) {
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
                color = ColorProvider(Color.White),
                fontSize = 11.sp
            )
        )
    }
}
