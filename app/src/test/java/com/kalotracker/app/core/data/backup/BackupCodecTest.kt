package com.kalotracker.app.core.data.backup

import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.entity.MealEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {

    private val meal = MealEntity(
        id = "m1", title = "Oats, with \"milk\"", totalCalories = 350,
        totalProteinGrams = 12.5f, totalCarbsGrams = 60f, totalFatGrams = 7f,
        imageLocalUri = "/data/x.jpg", notes = "n", timestamp = 1_700_000_000_000
    )
    private val items = listOf(FoodItemEntity("f1", "m1", "Oats", 80f, 300, 10f, 50f, 6f, 0.9f))

    private fun sample() = BackupFile(
        profile = BackupProfile(2100, 150, 200, 60, 9000, 2400, "LOSE"),
        meals = listOf(BackupCodec.toBackupMeal(meal, items)),
        water = listOf(BackupWater("w1", 250, 5)),
        weights = listOf(BackupWeight("g1", 80.5f, 6))
    )

    @Test
    fun roundTripPreservesEverythingExceptPhotoPaths() {
        val decoded = BackupCodec.decode(BackupCodec.encode(sample()))
        assertEquals(sample().meals, decoded.meals)
        assertEquals("LOSE", decoded.profile?.goal)

        val entity = BackupCodec.toMealEntity(decoded.meals[0])
        assertEquals(meal.copy(imageLocalUri = null), entity) // photo paths are device-specific
        assertEquals(items, BackupCodec.toFoodItemEntities(decoded.meals[0]))
        assertEquals(80.5f, BackupCodec.toWeightEntity(decoded.weights[0]).weightKg, 0.001f)
    }

    private fun assertRejected(text: String, containing: String) {
        try {
            BackupCodec.decode(text)
            fail("expected rejection")
        } catch (e: BackupFormatException) {
            assertTrue(e.message, e.message!!.contains(containing))
        }
    }

    @Test
    fun garbageAndForeignFilesAreRejectedWithClearMessages() {
        assertRejected("not json", "valid Kalo backup")
        assertRejected("""{"app":"other","version":1}""", "wasn't created by Kalo")
        assertRejected("""{"app":"kalo","version":99}""", "newer version")
        assertRejected("""{"app":"kalo","version":1,"meals":[{"id":"","title":"x","totalCalories":1,"totalProteinGrams":0,"totalCarbsGrams":0,"totalFatGrams":0,"timestamp":1}]}""", "without ids")
    }

    @Test
    fun unknownFieldsFromNewerFilesAreIgnored() {
        val decoded = BackupCodec.decode("""{"app":"kalo","version":1,"someFutureField":42,"meals":[]}""")
        assertTrue(decoded.meals.isEmpty())
    }

    @Test
    fun csvEscapesCommasAndQuotes() {
        val csv = BackupCodec.mealsToCsv(sample().meals)
        val lines = csv.trim().lines()
        assertEquals("date_time,meal,food,grams,calories,protein_g,carbs_g,fat_g", lines[0])
        assertTrue(lines[1], lines[1].contains("\"Oats, with \"\"milk\"\"\""))
        assertEquals(2, lines.size)
    }

    @Test fun duplicateIdsAndInvalidValuesAreRejectedBeforeImport() {
        val valid = sample()
        assertRejected(BackupCodec.encode(valid.copy(meals = valid.meals + valid.meals)), "duplicate")
        assertRejected(BackupCodec.encode(valid.copy(water = listOf(BackupWater("w", -1, 5)))), "invalid log")
        assertRejected(BackupCodec.encode(valid.copy(weights = listOf(BackupWeight("w", 0f, 5)))), "invalid log")
        assertRejected(BackupCodec.encode(valid.copy(profile = valid.profile!!.copy(calories = -10))), "invalid goals")
        assertRejected(BackupCodec.encode(valid.copy(version = 0)), "not supported")
        val bad = valid.meals[0].copy(items = valid.meals[0].items.map { it.copy(portionGrams = -5f) })
        assertRejected(BackupCodec.encode(valid.copy(meals = listOf(bad))), "invalid log")
    }
    @Test fun appearanceIsBackedUpAndOldPreferenceFilesDefaultToSystem() {
        val prefs=BackupPreferences("gemini-test",false,20,0,"DARK")
        val encoded=BackupCodec.encode(sample().copy(preferences=prefs))
        assertEquals("DARK",BackupCodec.decode(encoded).preferences!!.appearance)
        val old=encoded.replace(",\n        \"appearance\": \"DARK\"", "")
        assertEquals("SYSTEM",BackupCodec.decode(old).preferences!!.appearance)
    }

}
