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

    suspend fun logWater(milliliters: Int, timestamp: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            val entity = WaterLogEntity(
                milliliters = milliliters,
                timestamp = timestamp
            )
            waterDao.insertWaterLog(entity)
        }

    suspend fun undoLastWaterLog(startOfDay: Long, endOfDay: Long) =
        withContext(Dispatchers.IO) {
            waterDao.deleteLatestWaterLogForDay(startOfDay, endOfDay)
        }
}
