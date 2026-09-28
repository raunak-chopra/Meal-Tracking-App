package com.kalotracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "water_logs")
data class WaterLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val milliliters: Int,
    val timestamp: Long = System.currentTimeMillis()
)
