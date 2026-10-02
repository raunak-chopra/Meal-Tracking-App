package com.kalotracker.app.core.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HealthDataSummary(
    val steps: Long = 0L,
    val activeCaloriesBurned: Double = 0.0,
    val isConnected: Boolean = false,
    val syncSource: String = "Health Connect"
)

enum class HealthConnectAvailability {
    AVAILABLE,
    UPDATE_REQUIRED,
    NOT_SUPPORTED
}

class HealthConnectManager(private val context: Context) {

    val healthConnectClient by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    fun checkAvailability(): HealthConnectAvailability {
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    /**
     * Reads aggregated steps and active calories burned from today's start of day (midnight) to now.
     */
    suspend fun readTodayHealthData(): HealthDataSummary {
        return readHealthDataForDate(LocalDate.now())
    }

    /**
     * Reads aggregated steps and ACTIVE calories burned (excludes resting metabolism) for a date.
     * If date is today, queries up to Instant.now(); otherwise queries the full 24-hour window.
     */
    suspend fun readHealthDataForDate(date: LocalDate): HealthDataSummary {
        val client = healthConnectClient
            ?: return HealthDataSummary(isConnected = false, syncSource = "Health Connect unavailable")

        val zoneId = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zoneId).toInstant()
        val endOfDay = if (date.isEqual(LocalDate.now())) {
            Instant.now()
        } else {
            date.plusDays(1).atStartOfDay(zoneId).toInstant()
        }

        return try {
            if (!hasAllPermissions()) {
                return HealthDataSummary(isConnected = false, syncSource = "Permission needed")
            }
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
                    ),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
            )

            val steps = response[StepsRecord.COUNT_TOTAL] ?: 0L
            val energy = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0

            HealthDataSummary(
                steps = steps,
                activeCaloriesBurned = energy,
                isConnected = true,
                syncSource = "Health Connect"
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            HealthDataSummary(
                steps = 0L,
                activeCaloriesBurned = 0.0,
                isConnected = false,
                syncSource = "Couldn't read Health Connect"
            )
        }
    }
}
