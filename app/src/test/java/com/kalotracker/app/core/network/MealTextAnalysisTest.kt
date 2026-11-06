package com.kalotracker.app.core.network

import com.kalotracker.app.core.settings.AiSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MealTextAnalysisTest {
    private class Recording(val reply: String, settings: AiSettings = AiSettings("key")) : MealAnalysisService({ settings }) {
        val bodies = mutableListOf<String>()
        override fun postWithKey(model: String, apiKey: String, body: String): String {
            bodies += body
            return reply
        }
    }

    private fun envelope(meal: String) =
        """{"candidates":[{"content":{"parts":[{"text":${kotlinx.serialization.json.JsonPrimitive(meal)}}]}}]}"""

    @Test fun describedMealIsSentAsTextAndParsedLikeAPhoto() = runTest {
        val service = Recording(envelope(
            """{"is_food":true,"meal_title":"Chicken bharta with roti","confidence":0.8,
               "items":[{"name":"Chicken bharta","portion_grams":200,"calories":320,"protein":28,"carbs":8,"fat":19},
                        {"name":"Roti","portion_grams":80,"calories":240,"protein":8,"carbs":44,"fat":4}]}"""))
        val result = service.analyzeMealText("  chicken bharta and 2 rotis ").getOrThrow()
        assertEquals(listOf("Chicken bharta", "Roti"), result.items.map { it.name })
        val body = service.bodies.single()
        assertTrue(body.contains("chicken bharta and 2 rotis"))
        assertTrue(body.contains("not paneer"))
        assertTrue(!body.contains("inline_data"))
    }

    @Test fun blankDescriptionIsRejectedWithoutARequest() = runTest {
        val service = Recording(envelope("{}"))
        val error = service.analyzeMealText("   ").exceptionOrNull()
        assertTrue(error is MealAnalysisException.NotFood)
        assertTrue(error!!.message!!.contains("description"))
        assertTrue(service.bodies.isEmpty())
    }

    @Test fun nonFoodTextGetsADescriptionMessageNotAPhotoOne() = runTest {
        val service = Recording(envelope("""{"is_food":false,"meal_title":"","items":[],"confidence":0.9}"""))
        val error = service.analyzeMealText("went for a walk").exceptionOrNull()
        assertTrue(error is MealAnalysisException.NotFood)
        assertTrue(!error!!.message!!.contains("photo"))
    }

    @Test fun missingKeyIsReported() = runTest {
        val service = Recording(envelope("{}"), AiSettings(""))
        assertTrue(service.analyzeMealText("dal").exceptionOrNull() is MealAnalysisException.NotConfigured)
    }
}
