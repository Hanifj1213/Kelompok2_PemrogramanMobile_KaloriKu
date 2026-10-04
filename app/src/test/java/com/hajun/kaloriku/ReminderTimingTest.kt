package com.hajun.kaloriku

import com.hajun.kaloriku.util.nextReminderDelayMillis
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTimingTest {
    @Test
    fun schedulesNextUpcomingTime() {
        val now = ZonedDateTime.parse("2026-10-02T07:30:00+07:00[Asia/Jakarta]")
        assertEquals(30 * 60_000L, nextReminderDelayMillis(now, 8))
    }

    @Test
    fun preservesSecondsInsteadOfRoundingNotificationEarly() {
        val now = ZonedDateTime.parse("2026-10-02T07:59:59+07:00[Asia/Jakarta]")
        assertEquals(1000L, nextReminderDelayMillis(now, 8))
    }

    @Test
    fun passedTimeSchedulesTomorrow() {
        val now = ZonedDateTime.parse("2026-10-02T09:00:00+07:00[Asia/Jakarta]")
        assertEquals(23 * 60 * 60_000L, nextReminderDelayMillis(now, 8))
    }

    @Test
    fun exactTimeSchedulesNextDay() {
        val now = ZonedDateTime.parse("2026-10-02T08:00:00+07:00[Asia/Jakarta]")
        assertEquals(24 * 60 * 60_000L, nextReminderDelayMillis(now, 8))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidHour() {
        nextReminderDelayMillis(ZonedDateTime.now(), 24)
    }
}
