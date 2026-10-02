package com.kalotracker.app.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class ScannedFoodProduct(
    val barcode: String,
    val name: String,
    val brand: String? = null,
    val servingSizeGrams: Float = 100f,
    val caloriesPer100g: Int = 0,
    val proteinPer100g: Float = 0f,
    val carbsPer100g: Float = 0f,
    val fatPer100g: Float = 0f,
    val imageUrl: String? = null
) {
    fun calculateCalories(grams: Float): Int = ((caloriesPer100g * grams) / 100f).toInt()
    fun calculateProtein(grams: Float): Float = (proteinPer100g * grams) / 100f
    fun calculateCarbs(grams: Float): Float = (carbsPer100g * grams) / 100f
    fun calculateFat(grams: Float): Float = (fatPer100g * grams) / 100f
}

sealed class BarcodeLookupException(message: String) : Exception(message) {
    class NotFound(val barcode: String) :
        BarcodeLookupException("Barcode $barcode isn't in the Open Food Facts database.")

    class NoNutritionData(val barcode: String) :
        BarcodeLookupException("This product is listed but has no calorie data.")

    class Offline : BarcodeLookupException("No internet connection, so the barcode couldn't be looked up.")
    class Other(detail: String) : BarcodeLookupException(detail)
}

class OpenFoodFactsService {

    suspend fun getProductByBarcode(barcode: String): Result<ScannedFoodProduct> =
        withContext(Dispatchers.IO) {
            val cleaned = barcode.trim()
            if (cleaned.isBlank() || !cleaned.all { it.isDigit() }) {
                return@withContext Result.failure(BarcodeLookupException.Other("That doesn't look like a valid barcode."))
            }

            val conn = try {
                URL("https://world.openfoodfacts.org/api/v2/product/$cleaned.json").openConnection() as HttpURLConnection
            } catch (e: Exception) {
                return@withContext Result.failure(BarcodeLookupException.Other("Invalid barcode."))
            }
            try {
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "KaloTracker-Android/1.0 (personal use)")
                conn.connectTimeout = 8000
                conn.readTimeout = 8000

                when (val code = conn.responseCode) {
                    200 -> {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        Result.success(parseProduct(cleaned, JSONObject(text)).getOrElse {
                            return@withContext Result.failure(it)
                        })
                    }
                    404 -> Result.failure(BarcodeLookupException.NotFound(cleaned))
                    else -> Result.failure(BarcodeLookupException.Other("Lookup failed (HTTP $code). Try again."))
                }
            } catch (e: IOException) {
                Result.failure(BarcodeLookupException.Offline())
            } catch (e: Exception) {
                Result.failure(BarcodeLookupException.Other(e.localizedMessage ?: "Lookup failed."))
            } finally {
                conn.disconnect()
            }
        }

    companion object {
        /** Pure parser (unit tested). Never fabricates data: missing calories is an error. */
        internal fun parseProduct(barcode: String, json: JSONObject): Result<ScannedFoodProduct> {
            if (json.optInt("status", 0) != 1 || !json.has("product")) {
                return Result.failure(BarcodeLookupException.NotFound(barcode))
            }
            val product = json.getJSONObject("product")
            val nutriments = product.optJSONObject("nutriments")
                ?: return Result.failure(BarcodeLookupException.NoNutritionData(barcode))

            val kcal = when {
                nutriments.has("energy-kcal_100g") -> nutriments.optDouble("energy-kcal_100g")
                nutriments.has("energy_100g") -> nutriments.optDouble("energy_100g") / 4.184 // kJ -> kcal
                else -> Double.NaN
            }
            if (kcal.isNaN() || kcal < 0) {
                return Result.failure(BarcodeLookupException.NoNutritionData(barcode))
            }

            val name = product.optString("product_name").ifBlank { product.optString("product_name_en") }
                .ifBlank { "Product $barcode" }
            val serving = product.optDouble("serving_quantity", 0.0).toFloat().takeIf { it > 0f } ?: 100f

            return Result.success(
                ScannedFoodProduct(
                    barcode = barcode,
                    name = name,
                    brand = product.optString("brands", "").ifBlank { null },
                    servingSizeGrams = serving,
                    caloriesPer100g = kcal.toInt(),
                    proteinPer100g = nutriments.optDouble("proteins_100g", 0.0).toFloat(),
                    carbsPer100g = nutriments.optDouble("carbohydrates_100g", 0.0).toFloat(),
                    fatPer100g = nutriments.optDouble("fat_100g", 0.0).toFloat(),
                    imageUrl = product.optString("image_url", "").ifBlank { null }
                )
            )
        }
    }
}
