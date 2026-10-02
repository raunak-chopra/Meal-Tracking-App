package com.kalotracker.app.core.data.food

import com.kalotracker.app.core.database.entity.SavedFoodEntity
import org.junit.Assert.*
import org.junit.Test

class PersonalFoodTest {
    @Test fun cookedYieldControlsRecipePortionsRatherThanRawWeight() {
        val recipe = PersonalFood.recipe("Dal", 600f, listOf(Ingredient("Dry lentils",200f,700,50f,100f,5f), Ingredient("Oil",10f,90,0f,0f,10f)))
        val portion = PersonalFood.portion(recipe,150f,"meal").single()
        assertEquals(198,portion.calories)
        assertEquals(12.5f,portion.protein,0.001f)
        assertEquals(150f,portion.portionGrams,0.001f)
    }
    @Test fun templateRetainsIndividualFoodsAndFreshIds() {
        val template = PersonalFood.recipe("Breakfast",300f,listOf(Ingredient("Oats",100f,300,10f,50f,5f),Ingredient("Milk",200f,100,7f,10f,4f))).copy(kind="TEMPLATE")
        val a=PersonalFood.portion(template,150f,"m1"); val b=PersonalFood.portion(template,150f,"m2")
        assertEquals(listOf("Oats","Milk"),a.map{it.name});assertEquals(200,a.sumOf{it.calories})
        assertNotEquals(a[0].id,b[0].id);assertEquals("m2",b[0].mealId)
    }
    @Test fun labelServingsScaleCorrectly() {
        val food=SavedFoodEntity(name="Yogurt",yieldGrams=170f,calories=100,protein=17f,carbs=5f,fat=0f)
        assertEquals(200,PersonalFood.portion(food,340f,"m").single().calories)
    }
    @Test(expected=IllegalArgumentException::class) fun invalidYieldIsRejected() {
        PersonalFood.validate(SavedFoodEntity(name="Bad",yieldGrams=0f,calories=10,protein=1f,carbs=0f,fat=0f))
    }
}
