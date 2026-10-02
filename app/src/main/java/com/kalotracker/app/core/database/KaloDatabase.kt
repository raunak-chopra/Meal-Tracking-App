package com.kalotracker.app.core.database

import android.content.Context
import com.kalotracker.app.core.database.entity.*
import com.kalotracker.app.core.database.dao.PersonalDao
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kalotracker.app.core.database.dao.MealDao
import com.kalotracker.app.core.database.dao.WaterDao
import com.kalotracker.app.core.database.dao.WeightDao
import com.kalotracker.app.core.database.dao.WorkoutDao
import com.kalotracker.app.core.database.entity.ExerciseSetEntity
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.entity.MealEntity
import com.kalotracker.app.core.database.entity.WaterLogEntity
import com.kalotracker.app.core.database.entity.WeightLogEntity
import com.kalotracker.app.core.database.entity.WorkoutEntity

@Database(
    entities = [
        MealEntity::class,
        FoodItemEntity::class,
        WorkoutEntity::class,
        ExerciseSetEntity::class,
        WaterLogEntity::class,
        DayStatusEntity::class, GoalHistoryEntity::class, SavedFoodEntity::class, BarcodeCacheEntity::class, WorkoutRoutineEntity::class,
        WeightLogEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class KaloDatabase : RoomDatabase() {
    abstract fun personalDao(): PersonalDao
    abstract fun mealDao(): MealDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun waterDao(): WaterDao
    abstract fun weightDao(): WeightDao

    companion object {
        @Volatile
        private var INSTANCE: KaloDatabase? = null

        fun getInstance(context: Context): KaloDatabase {
            return INSTANCE ?: synchronized(this) {
                // No destructive fallback: every schema change must ship a tested Migration,
                // otherwise the app fails loudly instead of silently erasing the user's history.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KaloDatabase::class.java,
                    "kalo_database.db"
                ).addMigrations(*Migrations.ALL).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
