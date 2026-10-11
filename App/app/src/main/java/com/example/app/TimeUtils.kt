package com.example.app

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Ngày lưu dạng "yyyy-MM-dd", giờ lưu dạng "HH:mm" để so sánh chuỗi đúng thứ tự thời gian. */
object TimeUtils {
    private const val ISO = "yyyy-MM-dd"
    private fun fmt(p: String) = SimpleDateFormat(p, Locale.US)

    private val WEEKDAYS = arrayOf("Chủ nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy")

    fun today(): String = fmt(ISO).format(Date())

    fun toMillis(date: String, time: String): Long =
        fmt("yyyy-MM-dd HH:mm").parse("$date $time")?.time ?: 0L

    fun calendar(date: String): Calendar =
        Calendar.getInstance().apply { time = fmt(ISO).parse(date) ?: Date() }

    fun format(cal: Calendar): String = fmt(ISO).format(cal.time)

    fun addDays(date: String, n: Int): String =
        format(calendar(date).apply { add(Calendar.DAY_OF_MONTH, n) })

    fun datesBetween(start: String, end: String): List<String> {
        val out = ArrayList<String>()
        var d = start
        var guard = 0
        while (d <= end && guard++ < 400) {
            out.add(d)
            d = addDays(d, 1)
        }
        return out
    }

    fun day(date: String) = date.substring(8, 10).toInt()
    fun month(date: String) = date.substring(5, 7).toInt()
    fun year(date: String) = date.substring(0, 4).toInt()

    fun fmtDate(date: String) = fmt("dd/MM/yyyy").format(calendar(date).time)
    fun fmtDayMonth(date: String) = fmt("dd/MM").format(calendar(date).time)

    fun weekdayName(date: String) = WEEKDAYS[calendar(date).get(Calendar.DAY_OF_WEEK) - 1]

    fun fmtTime(hhmm: String, is24h: Boolean): String {
        if (is24h) return hhmm
        val d = fmt("HH:mm").parse(hhmm) ?: return hhmm
        return fmt("hh:mm a").format(d)
    }

    /** Giờ tròn kế tiếp (dùng làm mặc định khi tạo sự kiện mới). */
    fun nextHour(): Pair<String, String> {
        val c = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
        }
        return format(c) to fmt("HH:mm").format(c.time)
    }

    fun reminderLabel(min: Int): String = when (min) {
        0 -> "Không nhắc"
        1 -> "1 phút trước"
        5 -> "5 phút trước"
        10 -> "10 phút trước"
        30 -> "30 phút trước"
        60 -> "1 giờ trước"
        1440 -> "1 ngày trước"
        else -> "$min phút trước"
    }
}
