package com.kalotracker.app.feature.meal

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.SaverScope
import com.kalotracker.app.core.database.entity.FoodItemEntity
import org.junit.Assert.*
import org.junit.Test

class ManualItemsSaverTest {
    @Test fun restoresNamesPortionsNutritionAndIdsForMultipleDraftItems() {
        val items = mutableStateListOf(
            FoodItemEntity("one", "", "Rice", 150f, 195, 4f, 42f, 0.5f, 1f),
            FoodItemEntity("two", "", "Dal", 120f, 140, 9f, 20f, 2f, 0.8f),
            FoodItemEntity("three", "", "Lassi", 250f, 180, 8f, 24f, 6f, 0.7f, unit = "ml")
        )
        val scope = object : SaverScope { override fun canBeSaved(value: Any) = true }
        val saved = with(ManualItemsSaver) { scope.save(items) }!!
        val restored = ManualItemsSaver.restore(saved)!!
        assertEquals(items.toList(), restored.toList())
        assertEquals(3, restored.size)
    }
}
