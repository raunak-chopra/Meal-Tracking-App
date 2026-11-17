package com.kalotracker.app.core.database

import com.kalotracker.app.core.database.dao.WaterDao
import com.kalotracker.app.core.database.entity.WaterLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class WaterCapacityTest {
    @Test fun dailyCapacityRejectsOverflowWithoutChangingEntries() = runBlocking {
        val dao = MemoryWaterDao()
        dao.insertWaterLogChecked(WaterLogEntity("first", Int.MAX_VALUE, 1), 0, 10)
        try {
            dao.insertWaterLogChecked(WaterLogEntity("second", 250, 2), 0, 10)
            fail("Overflow must be rejected")
        } catch (_: IllegalArgumentException) { }
        assertEquals(1, dao.logs.size)
        assertEquals(Int.MAX_VALUE, dao.logs.single().milliliters)
    }

    @Test fun anotherDayHasIndependentCapacityAndMeasuredZeroIsNotAnEntry() = runBlocking {
        val dao = MemoryWaterDao()
        dao.insertWaterLogChecked(WaterLogEntity("yesterday", Int.MAX_VALUE, 1), 0, 10)
        dao.insertWaterLogChecked(WaterLogEntity("today", 750, 20), 11, 30)
        for(amount in listOf(0, -250)) {
            try {
                dao.insertWaterLogChecked(WaterLogEntity("invalid", amount, 21), 11, 30)
                fail("Non-positive amount must be rejected")
            } catch (_: IllegalArgumentException) { }
        }
        assertEquals(2, dao.logs.size)
        assertEquals(750, dao.getTotalWaterForDayOnce(11, 30))
    }

    private class MemoryWaterDao : WaterDao {
        val logs = mutableListOf<WaterLogEntity>()
        override fun getWaterLogsForDay(startOfDay: Long, endOfDay: Long): Flow<List<WaterLogEntity>> = flowOf(logs.filter { it.timestamp in startOfDay..endOfDay })
        override fun getTotalWaterForDay(startOfDay: Long, endOfDay: Long): Flow<Int> = flowOf(logs.filter { it.timestamp in startOfDay..endOfDay }.sumOf { it.milliliters })
        override suspend fun getTotalWaterForDayOnce(startOfDay: Long, endOfDay: Long): Int = logs.filter { it.timestamp in startOfDay..endOfDay }.sumOf { it.milliliters }
        override suspend fun getWaterLogsBetween(start: Long, end: Long): List<WaterLogEntity> = logs.filter { it.timestamp in start..end }
        override suspend fun getAll(): List<WaterLogEntity> = logs.toList()
        override suspend fun insertAll(logs: List<WaterLogEntity>) { this.logs.addAll(logs) }
        override suspend fun deleteAll() { logs.clear() }
        override suspend fun insertWaterLog(waterLog: WaterLogEntity) { logs.add(waterLog) }
        override suspend fun deleteWaterLog(waterLog: WaterLogEntity) { logs.remove(waterLog) }
        override suspend fun deleteLatestWaterLogForDay(startOfDay: Long, endOfDay: Long) { logs.filter { it.timestamp in startOfDay..endOfDay }.maxByOrNull { it.timestamp }?.let { logs.remove(it) } }
    }
}
