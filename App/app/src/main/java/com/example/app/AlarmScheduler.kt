package com.example.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/** Đặt / hủy báo thức nhắc nhở bằng AlarmManager. */
object AlarmScheduler {
    const val EXTRA_EVENT_ID = "event_id"

    private fun pending(context: Context, id: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_EVENT_ID, id)
        return PendingIntent.getBroadcast(
            context, id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(context: Context, e: Event) {
        cancel(context, e.id)
        if (!Prefs.alarmEnabled(context) || e.remindMinutes <= 0) return
        val trigger = e.startMillis - e.remindMinutes * 60_000L
        if (trigger <= System.currentTimeMillis()) return

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(context, e.id)
        try {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        } catch (se: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    fun cancel(context: Context, id: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context, id))
    }

    fun cancelAll(context: Context) {
        DatabaseHelper.get(context).getAll().forEach { cancel(context, it.id) }
    }

    /** Đặt lại tất cả báo thức (sau khi khởi động lại máy, khôi phục dữ liệu, bật lại nhắc nhở). */
    fun rescheduleAll(context: Context) {
        DatabaseHelper.get(context).getUpcoming().forEach { schedule(context, it) }
    }
}
