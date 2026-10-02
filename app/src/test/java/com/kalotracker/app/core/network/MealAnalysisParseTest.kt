package com.kalotracker.app.core.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MealAnalysisParseTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Wraps a model payload the way Gemini returns it. */
    private fun envelope(payload: String): String {
        val escaped = payload.replace("\\", "\\\\").replace("\"", "\\\"")
        return """{"candidates":[{"content":{"parts":[{"text":"$escaped"}]}}]}"""
    }

    private inline fun <reified T : MealAnalysisException> assertFails(raw: String) {
        try {
            parseGeminiResponse(json, raw)
            fail("expected ${T::class.simpleName}")
        } catch (e: MealAnalysisException) {
            assertTrue("was ${e::class.simpleName}", e is T)
        }
    }

    @Test
    fun validResponseIsParsed() {
        val r = parseGeminiResponse(
            json,
            envelope(
                """{"is_food":true,"meal_title":"Chicken and rice","confidence":0.8,"items":[
                {"name":"Chicken","portion_grams":150,"calories":248,"protein":46,"carbs":0,"fat":5,"confidence":0.9},
                {"name":"Rice","portion_grams":180,"calories":234,"protein":5,"carbs":51,"fat":0.5}]}"""
            )
        )
        assertEquals("Chicken and rice", r.mealTitle)
        assertEquals(2, r.items.size)
        assertEquals(0.7f, r.items[1].confidence, 0.001f) // default when the model omits it
    }

    @Test
    fun notFoodIsRejected() {
        assertFails<MealAnalysisException.NotFood>(envelope("""{"is_food":false,"meal_title":"","items":[],"confidence":0.9}"""))
    }

    @Test
    fun emptyItemsAreRejected() {
        assertFails<MealAnalysisException.NotFood>(envelope("""{"is_food":true,"meal_title":"x","items":[],"confidence":0.5}"""))
    }

    @Test
    fun invalidItemsAreFilteredAndAllInvalidIsRejected() {
        val mixed = parseGeminiResponse(
            json,
            envelope(
                """{"is_food":true,"meal_title":"x","confidence":1.7,"items":[
                {"name":"Good","portion_grams":100,"calories":100,"protein":1,"carbs":1,"fat":1},
                {"name":"Negative","portion_grams":100,"calories":-5,"protein":1,"carbs":1,"fat":1},
                {"name":"","portion_grams":100,"calories":10,"protein":1,"carbs":1,"fat":1},
                {"name":"Zero weight","portion_grams":0,"calories":10,"protein":1,"carbs":1,"fat":1}]}"""
            )
        )
        assertEquals(listOf("Good"), mixed.items.map { it.name })
        assertEquals(1f, mixed.confidence, 0.001f) // clamped

        assertFails<MealAnalysisException.NotFood>(
            envelope("""{"is_food":true,"meal_title":"x","confidence":0.5,"items":[{"name":"Bad","portion_grams":100,"calories":-1,"protein":1,"carbs":1,"fat":1}]}""")
        )
    }

    @Test
    fun garbageIsABadResponseNotAFakeMeal() {
        assertFails<MealAnalysisException.BadResponse>("not json at all")
        assertFails<MealAnalysisException.BadResponse>("""{"candidates":[]}""")
        assertFails<MealAnalysisException.BadResponse>("""{"promptFeedback":{"blockReason":"SAFETY"}}""")
        assertFails<MealAnalysisException.BadResponse>(envelope("this is prose, not the schema"))
    }
}
