package com.kalotracker.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.kalotracker.app.core.database.dao.MealDao
import com.kalotracker.app.core.database.dao.WaterDao
import com.kalotracker.app.core.database.dao.WorkoutDao
import com.kalotracker.app.core.database.entity.ExerciseSetEntity
import com.kalotracker.app.core.database.entity.FoodItemEntity
import com.kalotracker.app.core.database.entity.MealEntity
import com.kalotracker.app.core.database.entity.WaterLogEntity
import com.kalotracker.app.core.database.entity.WorkoutEntity

@Database(
    entities = [
        MealEntity::class,
        FoodItemEntity::class,
        WorkoutEntity::class,
        ExerciseSetEntity::class,
        WaterLogEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class KaloDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun waterDao(): WaterDao

    companion object {
        @Volatile
        private var INSTANCE: KaloDatabase? = null

        fun getInstance(context: Context): KaloDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KaloDatabase::class.java,
                    "kalo_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
