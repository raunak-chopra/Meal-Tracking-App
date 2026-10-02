package com.kalotracker.app.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val ALL: Array<Migration> get() = arrayOf(MIGRATION_2_3, MIGRATION_3_4)

    /**
     * v2 -> v3: drops the unused cloud-sync columns (meals.imageRemoteUrl, meals.syncStatus,
     * workouts.syncStatus) and adds weight_logs.
     *
     * SQLite on minSdk 26 cannot DROP COLUMN, so the tables are rebuilt. Child rows are copied to a
     * plain backup table BEFORE the parent is dropped, so an ON DELETE CASCADE can never remove
     * history regardless of whether foreign keys are enforced during the migration.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MIGRATION_2_3_SQL.forEach { db.execSQL(it) }
        }
    }

    internal val MIGRATION_2_3_SQL: List<String> = listOf(
        // meals
        "CREATE TABLE `meals_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `totalCalories` INTEGER NOT NULL, `totalProteinGrams` REAL NOT NULL, `totalCarbsGrams` REAL NOT NULL, `totalFatGrams` REAL NOT NULL, `imageLocalUri` TEXT, `notes` TEXT, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        "INSERT INTO `meals_new` (`id`, `title`, `totalCalories`, `totalProteinGrams`, `totalCarbsGrams`, `totalFatGrams`, `imageLocalUri`, `notes`, `timestamp`) SELECT `id`, `title`, `totalCalories`, `totalProteinGrams`, `totalCarbsGrams`, `totalFatGrams`, `imageLocalUri`, `notes`, `timestamp` FROM `meals`",
        "CREATE TABLE `food_items_bak` AS SELECT * FROM `food_items`",
        "DROP TABLE `food_items`",
        "DROP TABLE `meals`",
        "ALTER TABLE `meals_new` RENAME TO `meals`",
        "CREATE TABLE `food_items` (`id` TEXT NOT NULL, `mealId` TEXT NOT NULL, `name` TEXT NOT NULL, `portionGrams` REAL NOT NULL, `calories` INTEGER NOT NULL, `protein` REAL NOT NULL, `carbs` REAL NOT NULL, `fat` REAL NOT NULL, `confidence` REAL NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`mealId`) REFERENCES `meals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_food_items_mealId` ON `food_items` (`mealId`)",
        "INSERT INTO `food_items` (`id`, `mealId`, `name`, `portionGrams`, `calories`, `protein`, `carbs`, `fat`, `confidence`) SELECT `id`, `mealId`, `name`, `portionGrams`, `calories`, `protein`, `carbs`, `fat`, `confidence` FROM `food_items_bak`",
        "DROP TABLE `food_items_bak`",

        // workouts
        "CREATE TABLE `workouts_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `type` TEXT NOT NULL, `durationMinutes` INTEGER NOT NULL, `estimatedCaloriesBurned` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        "INSERT INTO `workouts_new` (`id`, `title`, `type`, `durationMinutes`, `estimatedCaloriesBurned`, `timestamp`) SELECT `id`, `title`, `type`, `durationMinutes`, `estimatedCaloriesBurned`, `timestamp` FROM `workouts`",
        "CREATE TABLE `exercise_sets_bak` AS SELECT * FROM `exercise_sets`",
        "DROP TABLE `exercise_sets`",
        "DROP TABLE `workouts`",
        "ALTER TABLE `workouts_new` RENAME TO `workouts`",
        "CREATE TABLE `exercise_sets` (`id` TEXT NOT NULL, `workoutId` TEXT NOT NULL, `exerciseName` TEXT NOT NULL, `setNumber` INTEGER NOT NULL, `weightKg` REAL NOT NULL, `reps` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`workoutId`) REFERENCES `workouts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_exercise_sets_workoutId` ON `exercise_sets` (`workoutId`)",
        "INSERT INTO `exercise_sets` (`id`, `workoutId`, `exerciseName`, `setNumber`, `weightKg`, `reps`, `isCompleted`) SELECT `id`, `workoutId`, `exerciseName`, `setNumber`, `weightKg`, `reps`, `isCompleted` FROM `exercise_sets_bak`",
        "DROP TABLE `exercise_sets_bak`",

        // new table
        "CREATE TABLE IF NOT EXISTS `weight_logs` (`id` TEXT NOT NULL, `weightKg` REAL NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))"
    )

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) { MIGRATION_3_4_SQL.forEach { db.execSQL(it) } }
    }
    internal val MIGRATION_3_4_SQL = listOf(
        "ALTER TABLE `workouts` ADD COLUMN `exercisesJson` TEXT NOT NULL DEFAULT '[]'",
        "CREATE TABLE IF NOT EXISTS `day_status` (`date` TEXT NOT NULL, `complete` INTEGER NOT NULL, PRIMARY KEY(`date`))",
        "CREATE TABLE IF NOT EXISTS `goal_history` (`date` TEXT NOT NULL, `calories` INTEGER NOT NULL, `protein` INTEGER NOT NULL, `carbs` INTEGER NOT NULL, `fat` INTEGER NOT NULL, `waterMl` INTEGER NOT NULL, `goal` TEXT NOT NULL, PRIMARY KEY(`date`))",
        "CREATE TABLE IF NOT EXISTS `saved_foods` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `kind` TEXT NOT NULL, `yieldGrams` REAL NOT NULL, `calories` INTEGER NOT NULL, `protein` REAL NOT NULL, `carbs` REAL NOT NULL, `fat` REAL NOT NULL, `favorite` INTEGER NOT NULL, `ingredientsJson` TEXT NOT NULL, PRIMARY KEY(`id`))",
        "CREATE TABLE IF NOT EXISTS `barcode_cache` (`barcode` TEXT NOT NULL, `payload` TEXT NOT NULL, `cachedAt` INTEGER NOT NULL, PRIMARY KEY(`barcode`))",
        "CREATE TABLE IF NOT EXISTS `workout_routines` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`id`))"
    )
}
