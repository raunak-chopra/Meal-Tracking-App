package com.kalotracker.app

import android.app.Instrumentation
import android.content.Context
import android.content.ContextWrapper
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import androidx.room.Room
import androidx.room.withTransaction
import com.kalotracker.app.core.database.KaloDatabase
import com.kalotracker.app.core.database.Migrations
import com.kalotracker.app.core.database.entity.*
import com.kalotracker.app.core.data.backup.*
import com.kalotracker.app.core.data.food.PersonalFood
import com.kalotracker.app.core.data.repository.UserProfileRepository
import com.kalotracker.app.core.network.ScannedFoodProduct
import com.kalotracker.app.core.network.OpenFoodFactsService
import com.kalotracker.app.core.settings.AppSettings
import com.kalotracker.app.feature.workout.WorkoutExercise
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/** Dependency-free instrumented checks, exclusively using disposable databases and preferences. */
class IntegrationRunner : Instrumentation() {
    private var searchFixtureRequested = false
    override fun onCreate(arguments: Bundle?) {
        searchFixtureRequested = arguments?.getString("fixture") == "recent-search-20261027"
        super.onCreate(arguments); start()
    }
    override fun onStart() {
        val result = Bundle()
        try {
            runBlocking { if (searchFixtureRequested) seedSearchFixture() else checks() }
            result.putString("stream", if (searchFixtureRequested) "\nPASS: seeded 19 fictional meal and 3 library search fixtures on an empty installation.\n" else "\nPASS: Room 3→5 migration; all-record/photo restore; merge; cached barcode; workout replacement; goal restart persistence; atomic photo draft round-trip/clear; explicit Progress completion/reopen/restart/error guards.\n")
            finish(-1, result)
        } catch (e: Throwable) {
            result.putString("stream", "\nFAIL: ${e.stackTraceToString()}\n")
            finish(0, result)
        }
    }

    /** Explicit opt-in setup for a disposable emulator; never replaces existing meals. */
    private suspend fun seedSearchFixture() {
        val db = (targetContext.applicationContext as KaloApplication).database
        val now = System.currentTimeMillis()
        fun meal(id: String, title: String, names: List<String>, minutesAgo: Long): com.kalotracker.app.core.database.dao.MealWithItems {
            val items = names.mapIndexed { index, name -> FoodItemEntity(id = "$id-$index", mealId = id,
                name = name, portionGrams = 100f, calories = 150, protein = 5f, carbs = 20f, fat = 5f) }
            return com.kalotracker.app.core.database.dao.MealWithItems(MealEntity(id = id, title = title,
                totalCalories = items.sumOf { it.calories }, totalProteinGrams = items.sumOf { it.protein.toDouble() }.toFloat(),
                totalCarbsGrams = items.sumOf { it.carbs.toDouble() }.toFloat(), totalFatGrams = items.sumOf { it.fat.toDouble() }.toFloat(),
                timestamp = now - minutesAgo * 60_000), items)
        }
        val rows = (0 until 16).map { meal("search-snack-$it", "Fixture snack ${it + 1}", listOf("Fixture snack ${it + 1}"), it.toLong()) } +
            listOf(meal("search-chicken", "Fixture lunch", listOf("Chicken bharta", "Jeera rice"), 16),
                meal("search-paneer-copy", "Fixture lunch", listOf("Paneer curry", "Jeera rice"), 17),
                meal("search-paneer", "Fixture lunch", listOf("Paneer curry", "Jeera rice"), 18))
        db.withTransaction {
            check(db.mealDao().getAllMeals().isEmpty()) { "Refusing to seed an installation with existing meals." }
            check(db.personalDao().foods().isEmpty()) { "Refusing to seed an installation with existing library entries." }
            db.personalDao().putFoods(listOf(
                PersonalFood.recipe("Fixture weekday bowl", 200f, listOf(
                    com.kalotracker.app.core.data.food.Ingredient("Paneer curry", 100f, 150, 5f, 20f, 5f),
                    com.kalotracker.app.core.data.food.Ingredient("Jeera rice", 100f, 150, 5f, 20f, 5f)), "search-recipe").copy(favorite = true),
                SavedFoodEntity(id = "search-food", name = "Fixture yogurt", calories = 100,
                    protein = 5f, carbs = 10f, fat = 4f),
                SavedFoodEntity(id = "search-template", name = "Fixture usual", kind = "TEMPLATE", calories = 150,
                    protein = 5f, carbs = 20f, fat = 5f, ingredientsJson = PersonalFood.json.encodeToString(listOf(
                        com.kalotracker.app.core.data.food.Ingredient("Banana", 100f, 150, 5f, 20f, 5f))))))
            db.mealDao().insertMeals(rows.map { it.meal })
            db.mealDao().insertFoodItems(rows.flatMap { it.items })
        }
    }

    private suspend fun checks() {
        val name = "integration_${System.nanoTime()}.db"
        val prefix = name.removeSuffix(".db")
        val ctx = object : ContextWrapper(targetContext) {
            override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences(prefix + name, mode)
        }
        val photos = File(targetContext.cacheDir, prefix).apply { mkdirs() }
        var migrated: KaloDatabase? = null
        var restored: KaloDatabase? = null
        try {
            // Build an actual v3 database from the exported schema, then let Room validate its migration.
            val schema = JSONObject(context.assets.open("com.kalotracker.app.core.database.KaloDatabase/3.json").bufferedReader().use { it.readText() }).getJSONObject("database")
            targetContext.getDatabasePath(name).parentFile!!.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(targetContext.getDatabasePath(name), null).use { db ->
                val entities = schema.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i)
                    val table = entity.getString("tableName")
                    db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                    val indices = entity.getJSONArray("indices")
                    for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
                val setup = schema.getJSONArray("setupQueries")
                for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
                db.execSQL("INSERT INTO meals VALUES ('old','Rice',200,4,45,1,NULL,'kept',1000)")
                db.execSQL("INSERT INTO food_items VALUES ('item','old','Rice',100,200,4,45,1,1)")
                db.execSQL("INSERT INTO workouts VALUES ('w','Run','CARDIO',20,100,1000)")
                db.execSQL("INSERT INTO water_logs VALUES ('water',250,1000)")
                db.execSQL("INSERT INTO weight_logs VALUES ('weight',80,1000)")
                db.version = 3
            }
            val db = Room.databaseBuilder(targetContext, KaloDatabase::class.java, name).addMigrations(*Migrations.ALL).build()
            migrated = db
            check(db.mealDao().getAllMeals().single().items.single().id == "item")
            check(db.workoutDao().getAllWorkouts().single().workout.exercisesJson == "[]")
            val image = File(photos, "source.jpg").apply { writeBytes(byteArrayOf(1,2,3,4)) }
            val meal = db.mealDao().getAllMeals().single()
            db.mealDao().insertMeals(listOf(meal.meal.copy(imageLocalUri = image.absolutePath)))
            db.mealDao().insertFoodItems(meal.items)
            val payload = PersonalFood.json.encodeToString(listOf(WorkoutExercise("Run",true,durationMinutes=20,calories=100)))
            db.personalDao().putDays(listOf(DayStatusEntity("2026-10-01",true)))
            db.personalDao().putFoods(listOf(SavedFoodEntity(name="Label",calories=120,protein=8f,carbs=10f,fat=5f,favorite=true)))
            db.personalDao().putRoutines(listOf(WorkoutRoutineEntity(name="Run",payload=payload)))
            db.personalDao().putBarcodes(listOf(BarcodeCacheEntity("123",PersonalFood.json.encodeToString(ScannedFoodProduct("123","Label",caloriesPer100g=120)),1)))
            check(OpenFoodFactsService(db.personalDao()).getProductByBarcode("123").getOrThrow().fromCache)
            val profile = UserProfileRepository(ctx)
            profile.updateTargets(2300,160,230,70,10000)
            check(UserProfileRepository(ctx).history.value == profile.history.value)
            val draftPath = File(photos, "meal-draft.json")
            val draftStore = com.kalotracker.app.feature.meal.FileMealDraftStore(draftPath)
            val draft = com.kalotracker.app.feature.meal.MealScanUiState(draftMealId = "fixture-draft", timestamp = 1234,
                mealTitle = "Fixture", items = listOf(com.kalotracker.app.feature.meal.EditableFoodItem(
                    name = "Rice", portionGrams = 100f, baseCaloriesPerGram = 1.3f,
                    baseProteinPerGram = .03f, baseCarbsPerGram = .28f, baseFatPerGram = .01f, confidence = .7f)))
            draftStore.save(draft)
            check(draftStore.load() == draft)
            draftStore.clear()
            check(draftStore.load() == null)
            val settings = AppSettings(ctx)
            settings.saveAppearance(com.kalotracker.app.core.settings.Appearance.DARK)
            val manager = BackupManager(db,profile,photos,settings)
            val output = ByteArrayOutputStream()
            manager.exportArchive(output)
            val archive = ArchiveCodec.read(ByteArrayInputStream(output.toByteArray()))
            val target = Room.inMemoryDatabaseBuilder(targetContext,KaloDatabase::class.java).build()
            restored = target
            val targetManager = BackupManager(target,profile,photos,AppSettings(ctx))
            targetManager.restoreArchive(archive)
            val roundtrip = targetManager.buildBackup()
            check(roundtrip.meals.size == 1 && roundtrip.meals.single().items.size == 1)
            check(roundtrip.workouts.size == 1 && roundtrip.water.size == 1 && roundtrip.weights.size == 1)
            check(roundtrip.savedFoods == archive.backup.savedFoods && roundtrip.routines == archive.backup.routines)
            check(roundtrip.dayStatus == archive.backup.dayStatus && roundtrip.barcodes == archive.backup.barcodes)
            check(roundtrip.goalHistory == archive.backup.goalHistory)
            check(roundtrip.preferences?.appearance == "DARK")
            val restoredPath = target.mealDao().getAllMeals().single().meal.imageLocalUri!!
            check(File(restoredPath).readBytes().contentEquals(image.readBytes()))
            // Restore twice updates matching IDs without multiplying records, while retaining unrelated records.
            target.personalDao().putFoods(listOf(SavedFoodEntity(name="Keep",calories=1,protein=0f,carbs=0f,fat=0f)))
            targetManager.import(archive.backup)
            check(target.mealDao().getAllMeals().size == 1 && target.personalDao().foods().size == 2)
            check(target.mealDao().getAllMeals().single().meal.imageLocalUri == restoredPath)
            target.workoutDao().replaceWorkoutWithSets(WorkoutEntity("w","Edited","STRENGTH",10,50,1000,payload),listOf(ExerciseSetEntity(workoutId="w",exerciseName="Bench",setNumber=1,weightKg=30f,reps=8,isCompleted=false)))
            target.workoutDao().replaceWorkoutWithSets(WorkoutEntity("w","Edited again","CARDIO",20,100,1000,payload),emptyList())
            check(target.workoutDao().getWorkoutById("w")!!.sets.isEmpty())
            checkProgressCompletion(target, ctx, profile, settings)
        } finally {
            migrated?.close(); restored?.close()
            targetContext.deleteDatabase(name)
            photos.listFiles()?.forEach { it.delete() }; photos.delete()
            targetContext.deleteSharedPreferences(prefix + "kalo_user_prefs")
            targetContext.deleteSharedPreferences(prefix + "kalo_app_settings")
        }
    }

    private suspend fun checkProgressCompletion(db: KaloDatabase, ctx: Context,
        profile: UserProfileRepository, settings: AppSettings) {
        val yesterday = java.time.LocalDate.now().minusDays(1)
        val timestamp = yesterday.atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        repeat(2) { index ->
            val meal = MealEntity(id = "completion-$index", title = "Fixture lunch", totalCalories = 1200,
                totalProteinGrams = 30f, totalCarbsGrams = 100f, totalFatGrams = 20f, timestamp = timestamp + index)
            db.mealDao().insertMealWithItems(meal, listOf(FoodItemEntity(mealId = meal.id, name = "Fixture food",
                portionGrams = 300f, calories = 1200, protein = 30f, carbs = 100f, fat = 20f)))
        }
        val store = androidx.lifecycle.ViewModelStore()
        suspend fun create(dao: com.kalotracker.app.core.database.dao.PersonalDao = db.personalDao()) = withContext(Dispatchers.Main) {
            val factory = com.kalotracker.app.feature.trends.TrendsViewModelFactory(dao,
                com.kalotracker.app.core.data.repository.MealRepository(db.mealDao()),
                com.kalotracker.app.core.data.repository.WorkoutRepository(db.workoutDao()),
                com.kalotracker.app.core.data.repository.WaterRepository(db.waterDao()),
                com.kalotracker.app.core.data.repository.WeightRepository(db.weightDao()), profile, settings,
                com.kalotracker.app.core.network.MealAnalysisService { settings.ai.value })
            androidx.lifecycle.ViewModelProvider(store, factory)[com.kalotracker.app.feature.trends.TrendsViewModel::class.java]
        }
        try {
            var vm = create()
            val initial = withTimeout(15_000) { vm.uiState.first { !it.isLoading && it.stats != null } }
            check(initial.stats!!.days.single { it.date == yesterday }.let { it.mealCount == 2 && !it.complete })
            check(initial.stats.loggedDays == 0) // Neither two meals nor a calorie threshold imply completeness.
            withContext(Dispatchers.Main) {
                vm.setDayComplete(java.time.LocalDate.now(), true)
                vm.setDayComplete(yesterday.minusDays(1), true) // No meals: cannot silently declare a full intake.
                vm.setDayComplete(yesterday, true)
                vm.setRange(30)
            }
            val complete = withTimeout(15_000) { vm.uiState.first { it.completionSavingDate == null && it.stats?.loggedDays == 1 } }
            check(complete.stats!!.avgCalories == 2400)
            check(complete.rangeDays == 30 && complete.stats.days.size == 30)
            check(db.personalDao().days().single { it.date == yesterday.toString() }.complete)
            check(db.personalDao().days().none { it.date == java.time.LocalDate.now().toString() || it.date == yesterday.minusDays(1).toString() })
            withContext(Dispatchers.Main) { store.clear() }
            vm = create()
            withTimeout(15_000) { vm.uiState.first { !it.isLoading && it.stats?.loggedDays == 1 } }
            withContext(Dispatchers.Main) { vm.setDayComplete(yesterday, false) }
            withTimeout(15_000) { vm.uiState.first { it.completionSavingDate == null && it.stats?.loggedDays == 0 } }
            check(!db.personalDao().days().single { it.date == yesterday.toString() }.complete)
            withContext(Dispatchers.Main) { store.clear() }
            val failingDao = object : com.kalotracker.app.core.database.dao.PersonalDao by db.personalDao() {
                override suspend fun putDays(rows: List<DayStatusEntity>) { throw java.io.IOException("Fixture storage failure") }
            }
            vm = create(failingDao)
            withTimeout(15_000) { vm.uiState.first { !it.isLoading && it.stats != null } }
            withContext(Dispatchers.Main) { vm.setDayComplete(yesterday, true) }
            val failed = withTimeout(15_000) { vm.uiState.first { it.completionError != null && it.completionSavingDate == null } }
            check(failed.stats!!.loggedDays == 0)
            check(!db.personalDao().days().single { it.date == yesterday.toString() }.complete)
        } finally {
            withContext(Dispatchers.Main) { store.clear() }
        }
    }
}
