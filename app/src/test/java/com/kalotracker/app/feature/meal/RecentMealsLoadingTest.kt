package com.kalotracker.app.feature.meal

import androidx.lifecycle.ViewModelStore
import com.kalotracker.app.core.data.repository.MealRepository
import com.kalotracker.app.core.database.dao.MealDao
import com.kalotracker.app.core.database.dao.MealWithItems
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.entity.MealEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class RecentMealsLoadingTest {
    private fun fixture(id: Int) = MealWithItems(MealEntity(id = "$id", title = "Fixture $id", totalCalories = 100,
        totalProteinGrams = 1f, totalCarbsGrams = 20f, totalFatGrams = 1f), listOf(FoodItemEntity(mealId = "$id",
        name = "Fixture food $id", portionGrams = 100f, calories = 100, protein = 1f, carbs = 20f, fat = 1f)))

    private fun repository(dispatcher: CoroutineDispatcher, load: suspend (Int) -> List<MealWithItems>): MealRepository {
        val unexpected = Proxy.newProxyInstance(MealDao::class.java.classLoader, arrayOf(MealDao::class.java)) { _, method, _ ->
            error("Unexpected operation: ${method.name}")
        } as MealDao
        return MealRepository(object : MealDao by unexpected {
            override suspend fun getRecentMeals(limit: Int) = load(limit)
        }, dispatcher)
    }

    @Test fun loadsTheBoundedCandidateWindowOnceWithoutTrimmingItToTwelveTitles() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            var calls = 0
            val candidates = (0 until 25).map(::fixture)
            val vm = ManualMealViewModel(mealRepository = repository(StandardTestDispatcher(testScheduler)) { limit ->
                assertEquals(200, limit); calls++; candidates
            })
            store.put("manual", vm)
            assertTrue(vm.recentMeals.value.isLoading)
            repeat(3) { vm.loadRecentMeals() }
            runCurrent()
            assertEquals(1, calls)
            assertEquals(candidates, vm.recentMeals.value.meals)
            assertFalse(vm.recentMeals.value.isLoading)
            assertNull(vm.recentMeals.value.error)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun failedReadIsVisibleAndCanBeRetriedWithoutLosingCommonFoodSearch() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            var calls = 0
            val vm = ManualMealViewModel(mealRepository = repository(StandardTestDispatcher(testScheduler)) {
                if (++calls == 1) throw java.io.IOException("Private fixture detail")
                listOf(fixture(1))
            })
            store.put("manual", vm); runCurrent()
            assertNotNull(vm.recentMeals.value.error)
            assertFalse(vm.recentMeals.value.error!!.contains("Private fixture detail"))
            assertTrue(mealSearchResults(vm.recentMeals.value.meals, "rice").commonFoods.isNotEmpty())
            vm.loadRecentMeals(); runCurrent()
            assertEquals(2, calls)
            assertEquals(1, vm.recentMeals.value.meals.size)
            assertNull(vm.recentMeals.value.error)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun destroyingTheScreenCancelsItsReadWithoutPublishingAFakeStorageError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            var cancelled = false
            val vm = ManualMealViewModel(mealRepository = repository(StandardTestDispatcher(testScheduler)) {
                try { awaitCancellation() } finally { cancelled = true }
            })
            store.put("manual", vm); runCurrent()
            store.clear(); runCurrent()
            assertTrue(cancelled)
            assertNull(vm.recentMeals.value.error)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }
}
