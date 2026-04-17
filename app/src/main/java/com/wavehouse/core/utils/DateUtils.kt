package com.wavehouse.core.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val vietnameseLocale = Locale("vi", "VN")

/** Timestamp (Long ms) → "13/04/2026" */
fun Long.toDateString(): String =
    SimpleDateFormat("dd/MM/yyyy", vietnameseLocale).format(Date(this))

/** Timestamp → "13/04/2026 15:30" */
fun Long.toDateTimeString(): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", vietnameseLocale).format(Date(this))

/** Timestamp → "15:30 13/04" */
fun Long.toShortDateTimeString(): String =
    SimpleDateFormat("HH:mm dd/MM", vietnameseLocale).format(Date(this))

/** Timestamp → relative: "Vừa xong", "5 phút trước", "2 giờ trước", "Hôm qua", "13/04" */
fun Long.toRelativeTimeString(): String {
    val now = System.currentTimeMillis()
    val diff = now - this
    return when {
        diff < 60_000L -> "Vừa xong"
        diff < 3_600_000L -> "${diff / 60_000} phút trước"
        diff < 86_400_000L -> "${diff / 3_600_000} giờ trước"
        diff < 172_800_000L -> "Hôm qua"
        else -> toDateString()
    }
}

/** Lấy đầu ngày hôm nay (00:00:00) */
fun todayStartMillis(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Lấy đầu tuần này (Thứ 2) */
fun weekStartMillis(): Long = Calendar.getInstance().apply {
    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Lấy đầu tháng này */
fun monthStartMillis(): Long = Calendar.getInstance().apply {
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Danh sách 7 ngày gần nhất (label ngắn cho chart) */
fun last7DaysLabels(): List<String> {
    val sdf = SimpleDateFormat("dd/MM", vietnameseLocale)
    return (6 downTo 0).map { daysAgo ->
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        sdf.format(cal.time)
    }
}
