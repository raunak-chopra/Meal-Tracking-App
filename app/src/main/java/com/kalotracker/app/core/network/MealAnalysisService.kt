package com.kalotracker.app.core.network

import android.util.Base64
import com.kalotracker.app.core.settings.AiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Talks to the Gemini API directly using the user's own key. No server involved. */
class MealAnalysisService(private val settingsProvider: () -> AiSettings) {

    suspend fun analyzeMealImage(
        imageBytes: ByteArray,
        userNote: String? = null
    ): Result<MealAnalysisResponse> = withContext(Dispatchers.IO) {
        val settings = settingsProvider()
        if (!settings.isConfigured) return@withContext Result.failure(MealAnalysisException.NotConfigured())

        try {
            val body = buildRequest(Base64.encodeToString(imageBytes, Base64.NO_WRAP), userNote)
            val text = post(settings, body.toString())
            Result.success(parseGeminiResponse(JSON, text))
        } catch (e: MealAnalysisException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(MealAnalysisException.Offline())
        } catch (e: Exception) {
            Result.failure(MealAnalysisException.Other(e.localizedMessage ?: "Analysis failed"))
        }
    }

    /** Lightweight key/model check used by the Settings "Test" button. */
    suspend fun testConnection(settings: AiSettings): Result<Unit> = withContext(Dispatchers.IO) {
        if (!settings.isConfigured) return@withContext Result.failure(MealAnalysisException.NotConfigured())
        try {
            val body = buildJsonObject {
                put("contents", buildJsonArray {
                    add(buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject { put("text", "Reply with the single word: ok") })
                        })
                    })
                })
            }
            post(settings, body.toString())
            Result.success(Unit)
        } catch (e: MealAnalysisException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(MealAnalysisException.Offline())
        } catch (e: Exception) {
            Result.failure(MealAnalysisException.Other(e.localizedMessage ?: "Test failed"))
        }
    }

    /** One-shot text generation used for the optional weekly AI summary. */
    suspend fun generateText(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val settings = settingsProvider()
        if (!settings.isConfigured) return@withContext Result.failure(MealAnalysisException.NotConfigured())
        try {
            val body = buildJsonObject {
                put("contents", buildJsonArray {
                    add(buildJsonObject {
                        put("parts", buildJsonArray { add(buildJsonObject { put("text", prompt) }) })
                    })
                })
                put("generationConfig", buildJsonObject { put("temperature", 0.4) })
            }
            val raw = post(settings, body.toString())
            val text = JSON.parseToJsonElement(raw).jsonObject["candidates"]?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")?.jsonObject?.get("parts")?.jsonArray
                ?.firstNotNullOfOrNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
                ?: throw MealAnalysisException.BadResponse("empty response")
            Result.success(text.trim())
        } catch (e: MealAnalysisException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(MealAnalysisException.Offline())
        } catch (e: Exception) {
            Result.failure(MealAnalysisException.Other(e.localizedMessage ?: "Request failed"))
        }
    }

    private fun buildRequest(base64Image: String, userNote: String?): JsonObject = buildJsonObject {
        put("system_instruction", buildJsonObject {
            put("parts", buildJsonArray { add(buildJsonObject { put("text", SYSTEM_PROMPT) }) })
        })
        put("contents", buildJsonArray {
            add(buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject {
                        put("text", buildString {
                            append("Analyze this meal photo.")
                            if (!userNote.isNullOrBlank()) {
                                append(" The user says: \"").append(userNote.trim().take(300)).append("\".")
                            }
                        })
                    })
                    add(buildJsonObject {
                        put("inline_data", buildJsonObject {
                            put("mime_type", "image/jpeg")
                            put("data", base64Image)
                        })
                    })
                })
            })
        })
        put("generationConfig", buildJsonObject {
            put("response_mime_type", "application/json")
            put("response_schema", RESPONSE_SCHEMA)
            put("temperature", 0.2)
        })
    }

    private fun post(settings: AiSettings, body: String): String {
        val url = "$BASE/models/${settings.model}:generateContent"
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 60_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-goog-api-key", settings.apiKey)
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            if (code in 200..299) {
                return conn.inputStream.bufferedReader().use { it.readText() }
            }
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val message = runCatching {
                JSON.parseToJsonElement(err).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
            }.getOrNull() ?: "HTTP $code"
            throw when (code) {
                400, 401, 403, 404 -> MealAnalysisException.BadKey(message.take(160))
                429 -> MealAnalysisException.RateLimited()
                else -> MealAnalysisException.Other("Gemini error: ${message.take(160)}")
            }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val BASE = "https://generativelanguage.googleapis.com/v1beta"
        private val JSON = Json { ignoreUnknownKeys = true; isLenient = true }

        private const val SYSTEM_PROMPT = """You are a careful nutrition analyst. Look at the meal photo and list each distinguishable food.
Rules:
1. Set is_food=false (and items=[]) if the photo does not show food or drink.
2. Estimate each item's edible weight in grams from visual cues (plate size, cutlery, typical servings). Prefer realistic servings; do not inflate.
3. Give calories and macros for THAT weight using standard USDA-style values.
4. Do NOT add cooking oil or butter unless it is clearly visible as its own item; the app adds oil separately when the user asks.
5. confidence is 0.0-1.0 for the whole meal; lower it for blurry photos, mixed dishes or hidden ingredients.
6. estimation_notes: one short sentence naming the main assumption, for example the plate size you assumed."""

        private fun type(name: String) = buildJsonObject { put("type", name) }

        private fun required(vararg names: String) = buildJsonArray { names.forEach { add(JsonPrimitive(it)) } }

        private val RESPONSE_SCHEMA: JsonObject = buildJsonObject {
            put("type", "OBJECT")
            put("properties", buildJsonObject {
                put("is_food", type("BOOLEAN"))
                put("meal_title", type("STRING"))
                put("items", buildJsonObject {
                    put("type", "ARRAY")
                    put("items", buildJsonObject {
                        put("type", "OBJECT")
                        put("properties", buildJsonObject {
                            put("name", type("STRING"))
                            put("portion_grams", type("NUMBER"))
                            put("calories", type("INTEGER"))
                            put("protein", type("NUMBER"))
                            put("carbs", type("NUMBER"))
                            put("fat", type("NUMBER"))
                            put("confidence", type("NUMBER"))
                        })
                        put("required", required("name", "portion_grams", "calories", "protein", "carbs", "fat"))
                    })
                })
                put("confidence", type("NUMBER"))
                put("estimation_notes", type("STRING"))
            })
            put("required", required("is_food", "meal_title", "items", "confidence"))
        }
    }
}

/**
 * Extracts and validates the meal JSON from a Gemini generateContent response.
 * Pure function so it can be unit tested without the network.
 */
internal fun parseGeminiResponse(json: Json, raw: String): MealAnalysisResponse {
    val root = try {
        json.parseToJsonElement(raw).jsonObject
    } catch (e: Exception) {
        throw MealAnalysisException.BadResponse("invalid JSON")
    }
    val candidate = (root["candidates"] as? JsonArray)?.firstOrNull()?.jsonObject
    val text = candidate?.get("content")?.jsonObject?.get("parts")?.jsonArray
        ?.firstNotNullOfOrNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
        ?: throw MealAnalysisException.BadResponse(
            root["promptFeedback"]?.jsonObject?.get("blockReason")?.jsonPrimitive?.contentOrNull ?: "empty response"
        )

    val parsed = try {
        json.decodeFromString(MealAnalysisResponse.serializer(), text)
    } catch (e: Exception) {
        throw MealAnalysisException.BadResponse("unexpected format")
    }

    if (!parsed.isFood) throw MealAnalysisException.NotFood()

    val valid = parsed.items.filter {
        it.name.isNotBlank() &&
            it.portionGrams.isFinite() && it.portionGrams > 0f &&
            it.calories >= 0 &&
            it.protein.isFinite() && it.protein >= 0f &&
            it.carbs.isFinite() && it.carbs >= 0f &&
            it.fat.isFinite() && it.fat >= 0f
    }.map { it.copy(confidence = it.confidence.coerceIn(0f, 1f)) }

    if (valid.isEmpty()) throw MealAnalysisException.NotFood()

    return parsed.copy(items = valid, confidence = parsed.confidence.coerceIn(0f, 1f))
}
