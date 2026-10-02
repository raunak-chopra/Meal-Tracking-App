package com.kalotracker.app.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@kotlinx.serialization.Serializable
data class ScannedFoodProduct(
    val barcode: String,
    val name: String,
    val brand: String? = null,
    val servingSizeGrams: Float = 100f,
    val caloriesPer100g: Int = 0,
    val proteinPer100g: Float = 0f,
    val carbsPer100g: Float = 0f,
    val fatPer100g: Float = 0f,
    val imageUrl: String? = null,
    val fromCache: Boolean = false,
    val labelUrl: String? = null,
    val requiresLabelConfirmation: Boolean = false,
    val fromCatalog: Boolean = false
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

    class LabelReviewRequired(val productName: String, val labelUrl: String) :
        BarcodeLookupException("Found $productName. Nutrition is incomplete or its preparation basis needs checking. Open the label, then enter nutrition manually.")

    class Offline : BarcodeLookupException("No internet connection, so the barcode couldn't be looked up.")
    class Other(detail: String) : BarcodeLookupException(detail)
}

class OpenFoodFactsService(private val cache: com.kalotracker.app.core.database.dao.PersonalDao? = null,
    private val catalogLoader: (() -> IndianSnackCatalog)? = null) {

    private val catalog by lazy { catalogLoader?.invoke() }

    suspend fun getProductByBarcode(barcode: String, refresh: Boolean = false): Result<ScannedFoodProduct> =
        withContext(Dispatchers.IO) {
            val cleaned = barcode.trim()
            if (cleaned.isBlank() || !cleaned.all { it.isDigit() }) {
                return@withContext Result.failure(BarcodeLookupException.Other("That doesn't look like a valid barcode."))
            }

            // Known starter records keep their preparation/confirmation rules on every path.
            val local = catalog?.lookup(cleaned)
            if (local != null) return@withContext local
            val cached = runCatching { cache?.cachedBarcode(cleaned) }.getOrNull()?.let { runCatching {
                com.kalotracker.app.core.data.food.PersonalFood.json.decodeFromString<ScannedFoodProduct>(it.payload).copy(fromCache = true)
            }.getOrNull() }
            if (cached != null && !refresh) return@withContext Result.success(cached)
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
                        val product = parseProduct(cleaned, JSONObject(text)).getOrElse {
                            return@withContext Result.failure(it)
                        }
                        runCatching { cache?.putBarcodes(listOf(com.kalotracker.app.core.database.entity.BarcodeCacheEntity(cleaned,
                            com.kalotracker.app.core.data.food.PersonalFood.json.encodeToString(ScannedFoodProduct.serializer(), product), System.currentTimeMillis()))) }
                        Result.success(product)
                    }
                    404 -> Result.failure(BarcodeLookupException.NotFound(cleaned))
                    else -> Result.failure(BarcodeLookupException.Other("Lookup failed (HTTP $code). Try again."))
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: IOException) {
                if (cached != null) Result.success(cached) else Result.failure(BarcodeLookupException.Offline())
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
            if (!kcal.isFinite() || kcal < 0 || kcal > Int.MAX_VALUE) {
                return Result.failure(BarcodeLookupException.NoNutritionData(barcode))
            }

            val name = product.optString("product_name").ifBlank { product.optString("product_name_en") }
                .ifBlank { "Product $barcode" }
            val serving = product.optDouble("serving_quantity", 0.0).toFloat().takeIf { it.isFinite() && it > 0f } ?: 100f

            val macros = listOf("proteins_100g", "carbohydrates_100g", "fat_100g").map { nutriments.optDouble(it, 0.0).toFloat() }
            if (macros.any { !it.isFinite() || it < 0f }) return Result.failure(BarcodeLookupException.Other("Invalid label nutrition."))
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
