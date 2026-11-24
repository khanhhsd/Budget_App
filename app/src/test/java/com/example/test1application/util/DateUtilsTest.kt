package com.example.test1application.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class DateUtilsTest {

    private val vietnam = ZoneId.of("Asia/Ho_Chi_Minh") // UTC+7
    private val newYork = ZoneId.of("America/New_York") // UTC-5/-4

    @Test
    fun localDateToUtcMillis_isMidnightUtc() {
        // 2026-05-15T00:00:00Z
        assertEquals(1778803200000L, DateUtils.localDateToUtcMillis(LocalDate.of(2026, 5, 15)))
        assertEquals(0L, DateUtils.localDateToUtcMillis(LocalDate.of(1970, 1, 1)))
    }

    @Test
    fun utcMillisToLocalDate_readsAsUtc() {
        assertEquals(LocalDate.of(2026, 5, 15), DateUtils.utcMillisToLocalDate(1778803200000L))
        // 23:59:59.999 UTC vẫn là cùng ngày
        assertEquals(LocalDate.of(2026, 5, 15), DateUtils.utcMillisToLocalDate(1778803200000L + 86_399_999L))
    }

    @Test
    fun roundTrip_preservesDate() {
        val dates = listOf(
            LocalDate.of(2024, 2, 29),
            LocalDate.of(2025, 12, 31),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 8) // ngày đổi giờ mùa hè ở Mỹ
        )
        for (d in dates) {
            assertEquals(d, DateUtils.utcMillisToLocalDate(DateUtils.localDateToUtcMillis(d)))
        }
    }

    @Test
    fun toTimestamp_keepsSelectedDayInLocalZone() {
        val date = LocalDate.of(2026, 5, 15)
        // 06:30 sáng ở VN là 23:30 UTC ngày hôm trước — vẫn phải ra ngày 15 theo giờ máy
        val early = DateUtils.toTimestamp(date, LocalTime.of(6, 30), vietnam)
        assertEquals(date, DateUtils.timestampToLocalDate(early, vietnam))
        assertEquals(LocalDate.of(2026, 5, 14), DateUtils.utcMillisToLocalDate(early))

        // 22:00 ở New York là ngày 16 theo UTC
        val late = DateUtils.toTimestamp(date, LocalTime.of(22, 0), newYork)
        assertEquals(date, DateUtils.timestampToLocalDate(late, newYork))
    }

    @Test
    fun pickerMillis_toTimestamp_givesSameDayForAnyZone() {
        val pickerMillis = DateUtils.localDateToUtcMillis(LocalDate.of(2026, 11, 1))
        for (zone in listOf(vietnam, newYork, ZoneId.of("UTC"), ZoneId.of("Pacific/Kiritimati"))) {
            val picked = DateUtils.utcMillisToLocalDate(pickerMillis)
            val ts = DateUtils.toTimestamp(picked, LocalTime.of(0, 15), zone)
            assertEquals(LocalDate.of(2026, 11, 1), DateUtils.timestampToLocalDate(ts, zone))
        }
    }

    @Test
    fun isSelectableUtcMillis_blocksFutureDates() {
        val today = LocalDate.of(2026, 11, 24)
        assertTrue(DateUtils.isSelectableUtcMillis(DateUtils.localDateToUtcMillis(today), today))
        assertTrue(DateUtils.isSelectableUtcMillis(DateUtils.localDateToUtcMillis(today.minusYears(1)), today))
        assertFalse(DateUtils.isSelectableUtcMillis(DateUtils.localDateToUtcMillis(today.plusDays(1)), today))
    }

    @Test
    fun formatDisplay_usesDayMonthYear() {
        assertEquals("05/03/2026", DateUtils.formatDisplay(LocalDate.of(2026, 3, 5)))
    }
}
