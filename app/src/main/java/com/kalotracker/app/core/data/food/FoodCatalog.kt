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
        FoodCatalogItem("almonds", "Raw Whole Almonds", "Fats", 30f, 579f, 21f, 21.6f, 49.9f),

        // More proteins
        FoodCatalogItem("turkey_breast", "Turkey Breast (Roasted)", "Protein", 120f, 135f, 30f, 0f, 0.7f),
        FoodCatalogItem("chicken_thigh", "Chicken Thigh (Cooked, Skinless)", "Protein", 120f, 209f, 26f, 0f, 10.9f),
        FoodCatalogItem("pork_tenderloin", "Pork Tenderloin (Cooked)", "Protein", 120f, 143f, 26f, 0f, 3.5f),
        FoodCatalogItem("shrimp", "Shrimp (Cooked)", "Protein", 100f, 99f, 24f, 0.2f, 0.3f),
        FoodCatalogItem("cod", "Cod Fillet (Cooked)", "Protein", 150f, 105f, 23f, 0f, 0.9f),
        FoodCatalogItem("tempeh", "Tempeh", "Protein", 100f, 192f, 20f, 7.6f, 10.8f),
        FoodCatalogItem("bacon", "Bacon (Cooked)", "Protein", 16f, 541f, 37f, 1.4f, 42f),
        FoodCatalogItem("ham_sliced", "Ham (Sliced)", "Protein", 60f, 145f, 21f, 1.5f, 5.5f),
        FoodCatalogItem("paneer", "Paneer", "Dairy/Protein", 100f, 265f, 18f, 1.2f, 20f),
        FoodCatalogItem("lentils", "Lentils / Dal (Cooked)", "Legumes", 150f, 116f, 9f, 20f, 0.4f),
        FoodCatalogItem("chickpeas", "Chickpeas (Cooked)", "Legumes", 120f, 164f, 8.9f, 27.4f, 2.6f),
        FoodCatalogItem("black_beans", "Black Beans (Cooked)", "Legumes", 120f, 132f, 8.9f, 23.7f, 0.5f),
        FoodCatalogItem("kidney_beans", "Kidney Beans / Rajma (Cooked)", "Legumes", 120f, 127f, 8.7f, 22.8f, 0.5f),

        // Dairy
        FoodCatalogItem("whole_milk", "Whole Milk", "Dairy", 240f, 61f, 3.2f, 4.8f, 3.3f),
        FoodCatalogItem("skim_milk", "Skim Milk", "Dairy", 240f, 34f, 3.4f, 5f, 0.1f),
        FoodCatalogItem("almond_milk", "Almond Milk (Unsweetened)", "Dairy", 240f, 15f, 0.6f, 0.3f, 1.2f),
        FoodCatalogItem("cottage_cheese", "Cottage Cheese (Low-fat)", "Dairy/Protein", 120f, 81f, 10.4f, 4.8f, 2.3f),
        FoodCatalogItem("plain_yogurt", "Plain Yogurt / Curd (Whole Milk)", "Dairy", 170f, 61f, 3.5f, 4.7f, 3.3f),
        FoodCatalogItem("cheddar", "Cheddar Cheese", "Dairy", 30f, 403f, 25f, 1.3f, 33f),
        FoodCatalogItem("mozzarella", "Mozzarella Cheese", "Dairy", 30f, 280f, 28f, 3.1f, 17f),
        FoodCatalogItem("butter", "Butter", "Fats", 10f, 717f, 0.9f, 0.1f, 81f),

        // More carbs and grains
        FoodCatalogItem("quinoa", "Quinoa (Cooked)", "Carbs", 150f, 120f, 4.4f, 21.3f, 1.9f),
        FoodCatalogItem("couscous", "Couscous (Cooked)", "Carbs", 150f, 112f, 3.8f, 23.2f, 0.2f),
        FoodCatalogItem("white_potato", "Potato (Boiled)", "Carbs", 150f, 87f, 1.9f, 20.1f, 0.1f),
        FoodCatalogItem("white_bread", "White Bread Slice", "Carbs", 30f, 265f, 9f, 49f, 3.2f),
        FoodCatalogItem("bagel", "Plain Bagel", "Carbs", 100f, 257f, 10f, 50f, 1.6f),
        FoodCatalogItem("chapati", "Roti / Chapati", "Carbs", 40f, 297f, 9.8f, 50f, 7.5f),
        FoodCatalogItem("naan", "Naan", "Carbs", 90f, 310f, 9f, 50f, 8f),
        FoodCatalogItem("flour_tortilla", "Flour Tortilla", "Carbs", 45f, 304f, 8.2f, 49f, 7.7f),
        FoodCatalogItem("corn_tortilla", "Corn Tortilla", "Carbs", 30f, 218f, 5.7f, 44.6f, 2.9f),
        FoodCatalogItem("cornflakes", "Corn Flakes Cereal", "Carbs", 30f, 357f, 7.5f, 84f, 0.4f),
        FoodCatalogItem("granola", "Granola", "Carbs", 45f, 471f, 10f, 64f, 20f),

        // More fruit and vegetables
        FoodCatalogItem("orange", "Orange", "Fruit", 130f, 47f, 0.9f, 11.8f, 0.1f),
        FoodCatalogItem("strawberries", "Strawberries", "Fruit", 150f, 32f, 0.7f, 7.7f, 0.3f),
        FoodCatalogItem("grapes", "Grapes", "Fruit", 100f, 69f, 0.7f, 18.1f, 0.2f),
        FoodCatalogItem("mango", "Mango", "Fruit", 165f, 60f, 0.8f, 15f, 0.4f),
        FoodCatalogItem("watermelon", "Watermelon", "Fruit", 200f, 30f, 0.6f, 7.6f, 0.2f),
        FoodCatalogItem("carrot", "Carrot (Raw)", "Vegetable", 80f, 41f, 0.9f, 9.6f, 0.2f),
        FoodCatalogItem("tomato", "Tomato", "Vegetable", 120f, 18f, 0.9f, 3.9f, 0.2f),
        FoodCatalogItem("cucumber", "Cucumber", "Vegetable", 100f, 15f, 0.7f, 3.6f, 0.1f),
        FoodCatalogItem("bell_pepper", "Bell Pepper", "Vegetable", 100f, 26f, 1f, 6f, 0.3f),
        FoodCatalogItem("onion", "Onion", "Vegetable", 80f, 40f, 1.1f, 9.3f, 0.1f),
        FoodCatalogItem("cauliflower", "Cauliflower", "Vegetable", 100f, 25f, 1.9f, 5f, 0.3f),
        FoodCatalogItem("mushrooms", "Mushrooms", "Vegetable", 80f, 22f, 3.1f, 3.3f, 0.3f),
        FoodCatalogItem("mixed_greens", "Mixed Salad Greens", "Vegetable", 60f, 17f, 1.5f, 3.1f, 0.3f),

        // Nuts, spreads and treats
        FoodCatalogItem("walnuts", "Walnuts", "Fats", 30f, 654f, 15.2f, 13.7f, 65.2f),
        FoodCatalogItem("cashews", "Cashews", "Fats", 30f, 553f, 18.2f, 30.2f, 43.8f),
        FoodCatalogItem("chia_seeds", "Chia Seeds", "Fats", 15f, 486f, 16.5f, 42.1f, 30.7f),
        FoodCatalogItem("hummus", "Hummus", "Fats", 60f, 166f, 7.9f, 14.3f, 9.6f),
        FoodCatalogItem("coconut_oil", "Coconut Oil", "Fats", 14f, 862f, 0f, 0f, 100f),
        FoodCatalogItem("mayonnaise", "Mayonnaise", "Fats", 14f, 680f, 1f, 0.6f, 75f),
        FoodCatalogItem("dark_chocolate", "Dark Chocolate (70-85%)", "Treats", 25f, 598f, 7.8f, 45.9f, 42.6f),
        FoodCatalogItem("honey", "Honey", "Treats", 21f, 304f, 0.3f, 82.4f, 0f),
        FoodCatalogItem("sugar", "Sugar", "Treats", 4f, 387f, 0f, 100f, 0f),

        // Common prepared foods and drinks
        FoodCatalogItem("pizza_cheese", "Cheese Pizza Slice", "Prepared", 107f, 266f, 11f, 33f, 10f),
        FoodCatalogItem("french_fries", "French Fries", "Prepared", 120f, 312f, 3.4f, 41f, 15f),
        FoodCatalogItem("cola", "Cola (Regular)", "Drinks", 330f, 42f, 0f, 10.6f, 0f),
        FoodCatalogItem("orange_juice", "Orange Juice", "Drinks", 250f, 45f, 0.7f, 10.4f, 0.2f)
    )

    fun search(query: String): List<FoodCatalogItem> {
        if (query.isBlank()) return commonFoods
        val lower = query.trim().lowercase()
        return commonFoods.filter {
            it.name.lowercase().contains(lower) || it.category.lowercase().contains(lower)
        }
    }
}
