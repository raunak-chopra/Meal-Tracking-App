package com.kalotracker.app.core.data.food

data class FoodCatalogItem(
    val id: String,
    val name: String,
    val category: String,
    val defaultServingGrams: Float,
    val caloriesPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float
) {
    fun calculateCalories(grams: Float): Int = ((caloriesPer100g * grams) / 100f).toInt()
    fun calculateProtein(grams: Float): Float = (proteinPer100g * grams) / 100f
    fun calculateCarbs(grams: Float): Float = (carbsPer100g * grams) / 100f
    fun calculateFat(grams: Float): Float = (fatPer100g * grams) / 100f
}

object FoodCatalog {

    val commonFoods: List<FoodCatalogItem> = listOf(
        // Proteins
        FoodCatalogItem("chicken_breast", "Chicken Breast (Boneless, Skinless)", "Protein", 150f, 165f, 31f, 0f, 3.6f),
        FoodCatalogItem("salmon_fillet", "Atlantic Salmon Fillet", "Protein", 150f, 208f, 20f, 0f, 13f),
        FoodCatalogItem("whole_eggs", "Whole Large Egg", "Protein", 50f, 143f, 12.6f, 0.7f, 9.5f),
        FoodCatalogItem("egg_whites", "Liquid Egg Whites", "Protein", 100f, 52f, 11f, 0.7f, 0.2f),
        FoodCatalogItem("greek_yogurt_0", "Greek Yogurt (Nonfat Plain)", "Dairy/Protein", 170f, 59f, 10f, 3.6f, 0.4f),
        FoodCatalogItem("whey_protein", "Whey Protein Powder", "Supplement", 30f, 380f, 80f, 5f, 4f),
        FoodCatalogItem("lean_ground_beef", "Lean Ground Beef (93/7)", "Protein", 120f, 172f, 24f, 0f, 8f),
        FoodCatalogItem("tofu_firm", "Firm Tofu", "Protein", 100f, 83f, 10f, 2.3f, 5.3f),
        FoodCatalogItem("canned_tuna", "Tuna (Chunk Light in Water)", "Protein", 100f, 90f, 19f, 0f, 1f),

        // Carbohydrates
        FoodCatalogItem("jasmine_rice", "Jasmine White Rice (Cooked)", "Carbs", 150f, 130f, 2.7f, 28.2f, 0.3f),
        FoodCatalogItem("brown_rice", "Brown Rice (Cooked)", "Carbs", 150f, 111f, 2.6f, 23f, 0.9f),
        FoodCatalogItem("rolled_oats", "Rolled Oats (Dry)", "Carbs", 50f, 379f, 13.2f, 67.7f, 6.5f),
        FoodCatalogItem("sweet_potato", "Sweet Potato (Baked)", "Carbs", 150f, 90f, 2f, 20.7f, 0.1f),
        FoodCatalogItem("whole_wheat_bread", "Whole Wheat Bread Slice", "Carbs", 40f, 247f, 13f, 41f, 3.4f),
        FoodCatalogItem("pasta_cooked", "Penne / Spaghetti (Cooked)", "Carbs", 140f, 158f, 5.8f, 31f, 0.9f),

        // Fruits & Vegetables
        FoodCatalogItem("banana", "Fresh Banana", "Fruit", 120f, 89f, 1.1f, 22.8f, 0.3f),
        FoodCatalogItem("apple", "Fresh Honeycrisp Apple", "Fruit", 150f, 52f, 0.3f, 14f, 0.2f),
        FoodCatalogItem("blueberries", "Fresh Blueberries", "Fruit", 100f, 57f, 0.7f, 14.5f, 0.3f),
        FoodCatalogItem("broccoli", "Steamed Broccoli Florets", "Vegetable", 100f, 35f, 2.4f, 7.2f, 0.4f),
        FoodCatalogItem("baby_spinach", "Fresh Baby Spinach", "Vegetable", 50f, 23f, 2.9f, 3.6f, 0.4f),

        // Healthy Fats
        FoodCatalogItem("avocado", "Hass Avocado", "Fats", 80f, 160f, 2f, 8.5f, 14.7f),
        FoodCatalogItem("olive_oil", "Extra Virgin Olive Oil", "Fats", 14f, 884f, 0f, 0f, 100f),
        FoodCatalogItem("peanut_butter", "Natural Peanut Butter", "Fats", 32f, 588f, 25f, 20f, 50f),
        FoodCatalogItem("almonds", "Raw Whole Almonds", "Fats", 30f, 579f, 21f, 21.6f, 49.9f)
    )

    fun search(query: String): List<FoodCatalogItem> {
        if (query.isBlank()) return commonFoods
        val lower = query.trim().lowercase()
        return commonFoods.filter {
            it.name.lowercase().contains(lower) || it.category.lowercase().contains(lower)
        }
    }
}
