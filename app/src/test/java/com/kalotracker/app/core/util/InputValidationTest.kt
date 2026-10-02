package com.kalotracker.app.core.util

import org.junit.Assert.*
import org.junit.Test

class InputValidationTest {
    private fun validate(calories: String = "2200", protein: String = "160", steps: String = "10000") =
        validateGoalInputs(calories, protein, "220", "70", steps, "2500")

    @Test fun acceptsNormalGoalsAndZeroOptionalTargets() {
        assertNull(validate())
        assertNull(validateGoalInputs("2200", "0", "0", "0", "0", "0"))
    }
    @Test fun rejectsNegativeMalformedAndOverflowValuesWithoutDefaults() {
        assertTrue(validate(calories = "-1")!!.startsWith("Calories"))
        assertTrue(validate(protein = "abc")!!.startsWith("Protein"))
        assertTrue(validate(steps = "99999999999999999999999")!!.startsWith("Steps"))
        assertNotNull(validate(calories = "0"))
    }
    @Test fun foodPortionsRejectInvalidValuesWithoutSubstitutingDefaults() {
        for(value in listOf("", "abc", "0", "-1", "NaN", "Infinity", "5001", "1e40")) assertNull(value,parseFoodPortion(value))
        assertEquals(0.5f,parseFoodPortion("0.5")!!,0f)
        assertEquals(175f,parseFoodPortion("175")!!,0f)
        assertEquals(5000f,parseFoodPortion("5000")!!,0f)
    }

}
