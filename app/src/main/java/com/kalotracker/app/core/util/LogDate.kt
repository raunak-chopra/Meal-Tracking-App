package com.kalotracker.app.core.util
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
object LogDate {
    fun timestamp(date: LocalDate, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Long =
        if (date == Instant.ofEpochMilli(now).atZone(zone).toLocalDate()) now
        else date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
}
