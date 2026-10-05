package com.hajun.kaloriku.util

import java.time.Duration
import java.time.ZonedDateTime

fun nextReminderDelayMillis(now: ZonedDateTime, hour: Int, minute: Int = 0): Long {
    require(hour in 0..23) { "Hour must be between 0 and 23" }
    require(minute in 0..59) { "Minute must be between 0 and 59" }
    var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
    if (!target.isAfter(now)) target = target.plusDays(1)
    return Duration.between(now, target).toMillis()
}
