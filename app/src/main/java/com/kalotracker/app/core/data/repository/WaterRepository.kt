package com.kalotracker.app.core.data.repository

import com.kalotracker.app.core.database.dao.WaterDao
import com.kalotracker.app.core.database.entity.WaterLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WaterRepository(
    private val waterDao: WaterDao
) {

    fun getWaterForDay(startOfDay: Long, endOfDay: Long): Flow<Int> {
        return waterDao.getTotalWaterForDay(startOfDay, endOfDay)
    }

    fun getWaterLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<WaterLogEntity>> {
        return waterDao.getWaterLogsForDay(startOfDay, endOfDay)
    }

    suspend fun getLogsBetween(start: Long, end: Long): List<WaterLogEntity> =
        withContext(Dispatchers.IO) { waterDao.getWaterLogsBetween(start, end) }

    suspend fun logWater(milliliters: Int, timestamp: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            val entity = WaterLogEntity(
                milliliters = milliliters,
                timestamp = timestamp
            )
            val date = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            val start = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
            waterDao.insertWaterLogChecked(entity, start, end)
        }

    suspend fun undoLastWaterLog(startOfDay: Long, endOfDay: Long) =
        withContext(Dispatchers.IO) {
            waterDao.deleteLatestWaterLogForDay(startOfDay, endOfDay)
        }
}
