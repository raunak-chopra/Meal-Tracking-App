package com.kalotracker.app.core.network

import org.json.JSONObject

/** Bundled community records. Packet confirmation is required before logging. */
class IndianSnackCatalog private constructor(private val entries: Map<String, JSONObject>) {
    fun lookup(barcode: String): Result<ScannedFoodProduct>? {
        val entry = entries[barcode] ?: return null
        val label = entry.getString("label_url")
        val name = entry.getString("name")
        val n = entry.getJSONObject("nutrition_per_100g_as_recorded")
        val keys = listOf("kcal", "protein_g", "carbohydrate_g", "fat_g")
        val values = keys.map { n.optDouble(it, Double.NaN) }
        if (!entry.getString("nutrition_basis").startsWith("as sold") ||
            values.any { !it.isFinite() || it < 0 } || values[0] > 1000 || values.drop(1).any { it > 100 }) {
            return Result.failure(BarcodeLookupException.LabelReviewRequired(name, label))
        }
        // Only explicitly unit-bearing gram pack sizes may provide a default portion.
        val pack = entry.optString("pack_size_as_recorded")
        val grams = Regex("(?i)^\\s*(\\d+(?:\\.\\d+)?)\\s*(g|gm|gram|grams)\\s*$").matchEntire(pack)
            ?.groupValues?.get(1)?.toFloatOrNull()?.takeIf { it in 5f..2500f } ?: 100f
        return Result.success(ScannedFoodProduct(barcode, name,
            brand = entry.optString("brand").takeUnless { it.isBlank() || it == "null" },
            servingSizeGrams = grams, caloriesPer100g = values[0].toInt(),
            proteinPer100g = values[1].toFloat(), carbsPer100g = values[2].toFloat(), fatPer100g = values[3].toFloat(),
            labelUrl = label, requiresLabelConfirmation = true, fromCatalog = true))
    }

    companion object {
        fun parse(text: String): IndianSnackCatalog {
            val rows = JSONObject(text).getJSONArray("products")
            return IndianSnackCatalog((0 until rows.length()).associate { i ->
                val row = rows.getJSONObject(i)
                row.getString("barcode") to row
            })
        }
    }
}
