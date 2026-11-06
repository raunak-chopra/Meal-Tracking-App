package com.kalotracker.app.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MacroLinkTest {
    @Test fun caloriesComeFromMacros() {
        assertEquals(2150, MacroSplit.caloriesOf(protein = 160, carbs = 220, fat = 70))
        assertEquals(2190, MacroSplit.caloriesOf(protein = 170, carbs = 220, fat = 70)) // +10 g protein = +40 kcal
        assertEquals(2240, MacroSplit.caloriesOf(protein = 160, carbs = 220, fat = 80)) // +10 g fat = +90 kcal
    }

    @Test fun newCalorieTargetKeepsTheSplit() {
        val split = MacroSplit.of(150, 225, 56)!! // about 30 / 45 / 25
        val (p, c, f) = split.gramsFor(2400)
        assertEquals(2400f, MacroSplit.caloriesOf(p, c, f).toFloat(), 10f)
        assertEquals(split.protein, p * 4f / MacroSplit.caloriesOf(p, c, f), 0.01f)
    }

    @Test fun splitSurvivesTypingATinyCalorieValue() {
        val split = MacroSplit.of(160, 220, 70)!!
        split.gramsFor(2) // first digit typed
        val (p, c, f) = split.gramsFor(2000)
        assertEquals(149, p)
        assertEquals(205, c)
        assertEquals(65, f)
    }

    @Test fun noSplitWithoutAnyMacros() {
        assertNull(MacroSplit.of(0, 0, 0))
    }
}
