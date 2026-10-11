package com.example.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/** Chi tiết sự kiện. Cũng là nơi mở ra khi chạm vào thông báo nhắc nhở. */
class EventDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ID = "event_id"
        fun intent(context: Context, id: Long) =
            Intent(context, EventDetailActivity::class.java).putExtra(EXTRA_ID, id)
        fun start(context: Context, id: Long) = context.startActivity(intent(context, id))
    }

    private var eventId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        Prefs.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)
        eventId = intent.getLongExtra(EXTRA_ID, -1L)
        findViewById<View>(R.id.btnDetailBack).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        bind()
    }

    private fun bind() {
        val e = DatabaseHelper.get(this).getEvent(eventId)
        if (e == null) {          // đã bị xóa
            finish()
            return
        }
        findViewById<TextView>(R.id.tvDTag).apply {
            text = e.categoryName
            setBackgroundResource(CategoryStyle.tagBg(e.categoryId))
            setTextColor(ContextCompat.getColor(context, CategoryStyle.color(e.categoryId)))
        }
        findViewById<TextView>(R.id.tvDTitle).text = e.title
        findViewById<TextView>(R.id.tvDDesc).apply {
            text = e.description
            visibility = if (e.description.isBlank()) View.GONE else View.VISIBLE
        }
        findViewById<TextView>(R.id.tvDTime).text = e.timeLabel(Prefs.is24h(this))
        findViewById<TextView>(R.id.tvDDate).text =
            if (e.startDate == e.endDate) "${TimeUtils.weekdayName(e.startDate)}, ${TimeUtils.fmtDate(e.startDate)}"
            else "${TimeUtils.fmtDate(e.startDate)} → ${TimeUtils.fmtDate(e.endDate)}"
        findViewById<TextView>(R.id.tvDPlace).text = e.location.ifBlank { "Chưa có địa điểm" }
        findViewById<TextView>(R.id.tvDReminder).text = TimeUtils.reminderLabel(e.remindMinutes)

        findViewById<View>(R.id.btnDEdit).setOnClickListener { EventEditActivity.start(this, e.id) }
        findViewById<View>(R.id.btnDDelete).setOnClickListener {
            DialogHelper.confirmDeleteEvent(this, e) {
                AlarmScheduler.cancel(this, e.id)
                DatabaseHelper.get(this).delete(e.id)
                finish()
            }
        }
    }
}
