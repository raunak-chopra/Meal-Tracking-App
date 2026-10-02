package com.kalotracker.app.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kalotracker.app.core.database.entity.WaterLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterDao {

    @Query("SELECT * FROM water_logs WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay ORDER BY timestamp DESC")
    fun getWaterLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<WaterLogEntity>>

    @Query("SELECT COALESCE(SUM(milliliters), 0) FROM water_logs WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    fun getTotalWaterForDay(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(milliliters), 0) FROM water_logs WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    suspend fun getTotalWaterForDayOnce(startOfDay: Long, endOfDay: Long): Int

    @Query("SELECT * FROM water_logs WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp ASC")
    suspend fun getWaterLogsBetween(start: Long, end: Long): List<WaterLogEntity>

    @Query("SELECT * FROM water_logs ORDER BY timestamp ASC")
    suspend fun getAll(): List<WaterLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<WaterLogEntity>)

    @Query("DELETE FROM water_logs")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLog(waterLog: WaterLogEntity)

    @Delete
    suspend fun deleteWaterLog(waterLog: WaterLogEntity)

    @Query("DELETE FROM water_logs WHERE id = (SELECT id FROM water_logs WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay ORDER BY timestamp DESC LIMIT 1)")
    suspend fun deleteLatestWaterLogForDay(startOfDay: Long, endOfDay: Long)
}
