package com.example.app

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import java.util.Calendar

/** SharedPreferences: tuần bắt đầu từ T2/CN, sáng/tối, định dạng giờ, nhắc nhở... */
object Prefs {
    private const val FILE = "settings"
    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // Tuần bắt đầu: Calendar.MONDAY (2) hoặc Calendar.SUNDAY (1)
    fun weekStart(c: Context) = sp(c).getInt("week_start", Calendar.MONDAY)
    fun setWeekStart(c: Context, v: Int) = sp(c).edit().putInt("week_start", v).apply()

    // Giao diện sáng/tối
    fun nightMode(c: Context) = sp(c).getInt("night_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    fun applyTheme(c: Context) = AppCompatDelegate.setDefaultNightMode(nightMode(c))
    fun saveTheme(c: Context, mode: Int) {
        sp(c).edit().putInt("night_mode", mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    // Định dạng giờ 24h / 12h
    fun is24h(c: Context) = sp(c).getBoolean("is_24h", true)
    fun setIs24h(c: Context, v: Boolean) = sp(c).edit().putBoolean("is_24h", v).apply()

    // Bật/tắt nhắc nhở
    fun alarmEnabled(c: Context) = sp(c).getBoolean("alarm_enabled", true)
    fun setAlarmEnabled(c: Context, v: Boolean) = sp(c).edit().putBoolean("alarm_enabled", v).apply()

    // Nhắc nhở mặc định (phút)
    fun defaultReminder(c: Context) = sp(c).getInt("default_reminder", 30)
    fun setDefaultReminder(c: Context, v: Int) = sp(c).edit().putInt("default_reminder", v).apply()

    // Chế độ xem mặc định: 0 tháng, 1 tuần, 2 danh sách
    fun defaultView(c: Context) = sp(c).getInt("default_view", 0)
    fun setDefaultView(c: Context, v: Int) = sp(c).edit().putInt("default_view", v).apply()
}
