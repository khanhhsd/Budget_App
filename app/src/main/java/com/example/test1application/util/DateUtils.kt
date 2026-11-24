package com.example.test1application.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Hàm thuần Kotlin để chuyển đổi ngày cho DatePicker (Material3).
 *
 * DatePicker làm việc với millis UTC tại 00:00 của ngày được chọn, còn timestamp của
 * giao dịch là thời điểm thực theo múi giờ máy. Không trộn lẫn hai loại này.
 */
object DateUtils {

    private val displayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** Millis UTC (từ DatePicker) -> ngày lịch, đọc theo UTC. */
    fun utcMillisToLocalDate(utcMillis: Long): LocalDate =
        Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

    /** Ngày lịch -> millis UTC lúc 00:00, dùng làm giá trị ban đầu cho DatePicker. */
    fun localDateToUtcMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    /** Ghép ngày đã chọn với giờ (thường là giờ hiện tại) theo múi giờ máy để ra timestamp. */
    fun toTimestamp(date: LocalDate, time: LocalTime, zone: ZoneId): Long =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    /** Timestamp giao dịch -> ngày lịch theo múi giờ máy. */
    fun timestampToLocalDate(timestamp: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

    /** Dùng cho SelectableDates: chỉ cho chọn ngày không nằm sau [today]. */
    fun isSelectableUtcMillis(utcMillis: Long, today: LocalDate): Boolean =
        !utcMillisToLocalDate(utcMillis).isAfter(today)

    fun formatDisplay(date: LocalDate): String = date.format(displayFormatter)
}
