package com.example.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object NotificationHelper {
    private const val CHANNEL_ID = "event_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Nhắc nhở sự kiện", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "Thông báo nhắc nhở trước khi sự kiện bắt đầu"
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, e: Event) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(context)

        // Chạm vào thông báo -> mở MainActivity rồi hiện chi tiết sự kiện
        val main = Intent(context, MainActivity::class.java)
        val detail = EventDetailActivity.intent(context, e.id)
        val pi = PendingIntent.getActivities(
            context, e.id.toInt(), arrayOf(main, detail),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val is24h = Prefs.is24h(context)
        val text = buildString {
            append("Bắt đầu lúc ${TimeUtils.fmtTime(e.startTime, is24h)} ngày ${TimeUtils.fmtDate(e.startDate)}")
            if (e.location.isNotBlank()) append(" • ${e.location}")
        }
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bell)
            .setContentTitle("Nhắc nhở: ${e.title}")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(context).notify(e.id.toInt(), n)
    }
}
