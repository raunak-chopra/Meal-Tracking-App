package com.kalotracker.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "weight_logs")
data class WeightLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val weightKg: Float,
    val timestamp: Long = System.currentTimeMillis()
)
