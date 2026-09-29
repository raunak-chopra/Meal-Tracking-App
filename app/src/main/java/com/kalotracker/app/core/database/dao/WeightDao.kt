package com.kalotracker.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kalotracker.app.core.database.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs ORDER BY timestamp ASC")
    suspend fun getAll(): List<WeightLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: WeightLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<WeightLogEntity>)

    @Delete
    suspend fun delete(log: WeightLogEntity)

    @Query("DELETE FROM weight_logs")
    suspend fun deleteAll()
}
