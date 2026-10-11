package com.example.app

/** Bảng DanhMuc(maDanhMuc, tenDanh) */
data class Category(val id: Long, val name: String)

/** Bảng SuKien (đã JOIN thêm tên danh mục để hiển thị). */
data class Event(
    val id: Long = 0,                 // maSuKien
    val title: String,                // tieuDe
    val description: String = "",     // moTa
    val location: String = "",        // diaDiem
    val startDate: String,            // ngayBatDau  (yyyy-MM-dd)
    val startTime: String,            // gioBatDau   (HH:mm)
    val endDate: String,              // ngayKetThuc (yyyy-MM-dd)
    val endTime: String,              // gioKetThuc  (HH:mm)
    val remindMinutes: Int = 30,      // thoiGianNhacNho (phút trước; 0 = không nhắc)
    val categoryId: Long = 6,         // maDanhMuc
    val categoryName: String = ""     // tenDanh
) {
    /** Cả ngày = 00:00 -> 23:59 */
    val isAllDay: Boolean get() = startTime == "00:00" && endTime == "23:59"
    val startMillis: Long get() = TimeUtils.toMillis(startDate, startTime)
    val endMillis: Long get() = TimeUtils.toMillis(endDate, endTime) + 59_999

    fun isOngoing(now: Long = System.currentTimeMillis()) = now in startMillis..endMillis
    fun isEnded(now: Long = System.currentTimeMillis()) = now > endMillis

    fun timeLabel(is24h: Boolean): String {
        val sameDay = startDate == endDate
        return when {
            isAllDay && sameDay -> "Cả ngày"
            isAllDay -> "Cả ngày (${TimeUtils.fmtDayMonth(startDate)} - ${TimeUtils.fmtDayMonth(endDate)})"
            sameDay -> "${TimeUtils.fmtTime(startTime, is24h)} - ${TimeUtils.fmtTime(endTime, is24h)}"
            else -> "${TimeUtils.fmtDayMonth(startDate)} ${TimeUtils.fmtTime(startTime, is24h)} - " +
                "${TimeUtils.fmtDayMonth(endDate)} ${TimeUtils.fmtTime(endTime, is24h)}"
        }
    }
}
