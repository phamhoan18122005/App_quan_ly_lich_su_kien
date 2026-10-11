package com.example.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Nhận báo thức từ AlarmManager và hiện thông báo. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_EVENT_ID, -1L)
        if (id < 0) return
        val event = DatabaseHelper.get(context).getEvent(id) ?: return
        NotificationHelper.show(context, event)
    }
}
