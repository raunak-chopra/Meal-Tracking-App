package com.kalotracker.app.core.data.repository

import com.kalotracker.app.core.database.dao.WeightDao
import com.kalotracker.app.core.database.entity.WeightLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WeightRepository(private val weightDao: WeightDao) {

    /** Newest first. */
    fun observeAll(): Flow<List<WeightLogEntity>> = weightDao.observeAll()

    suspend fun log(weightKg: Float, timestamp: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            weightDao.insert(WeightLogEntity(weightKg = weightKg, timestamp = timestamp))
        }

    suspend fun delete(log: WeightLogEntity) = withContext(Dispatchers.IO) { weightDao.delete(log) }
}
