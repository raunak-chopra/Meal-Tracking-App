package com.kalotracker.app.core.network

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenFoodFactsParseTest {

    private fun parse(json: String) = OpenFoodFactsService.parseProduct("123456789012", JSONObject(json))

    @Test
    fun knownProductIsParsed() {
        val p = parse(
            """{"status":1,"product":{"product_name":"Greek Yogurt","brands":"Acme","serving_quantity":170,
            "nutriments":{"energy-kcal_100g":57,"proteins_100g":10.3,"carbohydrates_100g":3,"fat_100g":0.2}}}"""
        ).getOrThrow()
        assertEquals("Greek Yogurt", p.name)
        assertEquals(57, p.caloriesPer100g)
        assertEquals(170f, p.servingSizeGrams, 0.001f)
    }

    @Test
    fun unknownBarcodeIsNotFoundNeverInvented() {
        val e = parse("""{"status":0,"status_verbose":"product not found"}""").exceptionOrNull()
        assertTrue(e is BarcodeLookupException.NotFound)
    }

    @Test
    fun productWithoutCaloriesIsAnErrorNotZeroCalories() {
        val e = parse("""{"status":1,"product":{"product_name":"Mystery","nutriments":{"proteins_100g":5}}}""").exceptionOrNull()
        assertTrue(e is BarcodeLookupException.NoNutritionData)
        val e2 = parse("""{"status":1,"product":{"product_name":"Mystery"}}""").exceptionOrNull()
        assertTrue(e2 is BarcodeLookupException.NoNutritionData)
    }

    @Test
    fun kilojouleOnlyProductsAreConverted() {
        val p = parse("""{"status":1,"product":{"product_name":"Bar","nutriments":{"energy_100g":1046}}}""").getOrThrow()
        assertEquals(250, p.caloriesPer100g) // 1046 kJ / 4.184
    }

    @Test
    fun missingNameGetsNeutralPlaceholder() {
        val p = parse("""{"status":1,"product":{"nutriments":{"energy-kcal_100g":100}}}""").getOrThrow()
        assertEquals("Product 123456789012", p.name)
        assertEquals(100f, p.servingSizeGrams, 0.001f)
    }
}
