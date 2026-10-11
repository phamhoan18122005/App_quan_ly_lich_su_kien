package com.example.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Sau khi khởi động lại máy, báo thức bị xóa -> đặt lại. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) AlarmScheduler.rescheduleAll(context)
    }
}
