package com.kalotracker.app.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class MacroPresetTest {

    @Test
    fun testAllNonCustomPresetsSumTo100Percent() {
        val nonCustomPresets = MacroPreset.values().filter { it != MacroPreset.CUSTOM }
        for (preset in nonCustomPresets) {
            val totalPct = preset.proteinPct + preset.carbsPct + preset.fatPct
            assertEquals("Macro percentages for ${preset.name} must total exactly 100%", 100, totalPct)
        }
    }

    @Test
    fun testBalancedPresetMath() {
        val totalCalories = 2000
        val preset = MacroPreset.BALANCED // 30% P, 45% C, 25% F

        val proteinGrams = ((totalCalories * (preset.proteinPct / 100f)) / 4f).toInt()
        val carbsGrams = ((totalCalories * (preset.carbsPct / 100f)) / 4f).toInt()
        val fatGrams = ((totalCalories * (preset.fatPct / 100f)) / 9f).toInt()

        assertEquals(150, proteinGrams)
        assertEquals(225, carbsGrams)
        assertEquals(55, fatGrams)
    }

    @Test
    fun testHighProteinPresetMath() {
        val totalCalories = 2000
        val preset = MacroPreset.HIGH_PROTEIN // 40% P, 35% C, 25% F

        val proteinGrams = ((totalCalories * (preset.proteinPct / 100f)) / 4f).toInt()
        val carbsGrams = ((totalCalories * (preset.carbsPct / 100f)) / 4f).toInt()
        val fatGrams = ((totalCalories * (preset.fatPct / 100f)) / 9f).toInt()

        assertEquals(200, proteinGrams)
        assertEquals(175, carbsGrams)
        assertEquals(55, fatGrams)
    }

    @Test
    fun testKetoPresetMath() {
        val totalCalories = 2000
        val preset = MacroPreset.KETO // 25% P, 5% C, 70% F

        val proteinGrams = ((totalCalories * (preset.proteinPct / 100f)) / 4f).toInt()
        val carbsGrams = ((totalCalories * (preset.carbsPct / 100f)) / 4f).toInt()
        val fatGrams = ((totalCalories * (preset.fatPct / 100f)) / 9f).toInt()

        assertEquals(125, proteinGrams)
        assertEquals(25, carbsGrams)
        assertEquals(155, fatGrams)
    }
}
