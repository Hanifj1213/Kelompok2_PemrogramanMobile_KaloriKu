package com.hajun.kaloriku.util

import java.time.Duration
import java.time.ZonedDateTime

fun nextReminderDelayMillis(now: ZonedDateTime, hour: Int): Long {
    require(hour in 0..23)
    var target = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
    if (!target.isAfter(now)) target = target.plusDays(1)
    return Duration.between(now, target).toMillis()
}
