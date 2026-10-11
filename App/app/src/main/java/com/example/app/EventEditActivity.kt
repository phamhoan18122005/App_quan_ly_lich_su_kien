package com.example.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Thêm mới / sửa / xóa sự kiện. */
class EventEditActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ID = "event_id"
        private const val EXTRA_DATE = "date"

        /** id = null -> tạo mới (date: ngày mặc định), id != null -> chỉnh sửa. */
        fun start(context: Context, id: Long? = null, date: String? = null) {
            val i = Intent(context, EventEditActivity::class.java)
            if (id != null) i.putExtra(EXTRA_ID, id)
            if (date != null) i.putExtra(EXTRA_DATE, date)
            context.startActivity(i)
        }
    }

    private val categoryChips = mapOf(
        R.id.chipWork to 1L, R.id.chipPersonal to 2L, R.id.chipFamily to 3L,
        R.id.chipFriends to 4L, R.id.chipHealth to 5L, R.id.chipOther to 6L
    )
    private val reminderChips = mapOf(
        R.id.rem0 to 0, R.id.rem1 to 1, R.id.rem5 to 5, R.id.rem10 to 10,
        R.id.rem30 to 30, R.id.rem60 to 60, R.id.rem1440 to 1440
    )

    private lateinit var db: DatabaseHelper
    private var editing: Event? = null

    private var startDate = ""
    private var startTime = ""
    private var endDate = ""
    private var endTime = ""

    private lateinit var tvStartDate: TextView
    private lateinit var tvStartTime: TextView
    private lateinit var tvEndDate: TextView
    private lateinit var tvEndTime: TextView
    private lateinit var rowEnd: View
    private lateinit var switchAllDay: MaterialSwitch
    private lateinit var etTitle: TextInputEditText
    private lateinit var tilTitle: TextInputLayout
    private lateinit var etLocation: TextInputEditText
    private lateinit var etNote: TextInputEditText
    private lateinit var chipsCategory: ChipGroup
    private lateinit var chipsReminder: ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        Prefs.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_edit)
        db = DatabaseHelper.get(this)

        tvStartDate = findViewById(R.id.tvStartDate)
        tvStartTime = findViewById(R.id.tvStartTime)
        tvEndDate = findViewById(R.id.tvEndDate)
        tvEndTime = findViewById(R.id.tvEndTime)
        rowEnd = findViewById(R.id.rowEnd)
        switchAllDay = findViewById(R.id.switchAllDay)
        etTitle = findViewById(R.id.etTitle)
        tilTitle = findViewById(R.id.tilTitle)
        etLocation = findViewById(R.id.etLocation)
        etNote = findViewById(R.id.etNote)
        chipsCategory = findViewById(R.id.chipGroupCategory)
        chipsReminder = findViewById(R.id.chipGroupReminder)

        findViewById<Chip>(R.id.chipAttachment).visibility = View.GONE   // lược đồ không có tệp đính kèm

        val id = intent.getLongExtra(EXTRA_ID, -1L)
        editing = if (id >= 0) db.getEvent(id) else null
        populate()

        findViewById<View>(R.id.btnClose).setOnClickListener { finish() }
        findViewById<View>(R.id.btnSaveTop).setOnClickListener { save() }
        findViewById<View>(R.id.btnSave).setOnClickListener { save() }
        findViewById<View>(R.id.btnDelete).setOnClickListener {
            val e = editing ?: return@setOnClickListener
            DialogHelper.confirmDeleteEvent(this, e) {
                AlarmScheduler.cancel(this, e.id)
                db.delete(e.id)
                Toast.makeText(this, "Đã xóa sự kiện", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        tvStartDate.setOnClickListener {
            pickDate(startDate) {
                startDate = it
                if (endDate < startDate) endDate = startDate
                updateUi()
            }
        }
        tvEndDate.setOnClickListener { pickDate(endDate) { endDate = it; updateUi() } }
        tvStartTime.setOnClickListener { pickTime(startTime) { startTime = it; updateUi() } }
        tvEndTime.setOnClickListener { pickTime(endTime) { endTime = it; updateUi() } }
        switchAllDay.setOnCheckedChangeListener { _, _ -> updateUi() }

        // Mở Google Maps (hoặc app bản đồ bất kỳ) theo địa điểm đã nhập
        findViewById<View>(R.id.mapPreview).setOnClickListener {
            val q = etLocation.text?.toString()?.trim().orEmpty()
            if (q.isEmpty()) {
                Toast.makeText(this, "Hãy nhập địa điểm trước", Toast.LENGTH_SHORT).show()
            } else try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(q))))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Không có ứng dụng bản đồ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populate() {
        val e = editing
        if (e != null) {
            findViewById<TextView>(R.id.tvEditTitle).text = getString(R.string.edit_event)
            etTitle.setText(e.title)
            etLocation.setText(e.location)
            etNote.setText(e.description)
            startDate = e.startDate; startTime = e.startTime
            endDate = e.endDate; endTime = e.endTime
            switchAllDay.isChecked = e.isAllDay
            chipsCategory.check(categoryChips.entries.firstOrNull { it.value == e.categoryId }?.key ?: R.id.chipOther)
            chipsReminder.check(reminderChips.entries.firstOrNull { it.value == e.remindMinutes }?.key ?: R.id.rem30)
            findViewById<View>(R.id.btnDelete).visibility = View.VISIBLE
        } else {
            findViewById<TextView>(R.id.tvEditTitle).text = getString(R.string.nav_create)
            val (nextDate, nextStart) = TimeUtils.nextHour()
            startDate = intent.getStringExtra(EXTRA_DATE) ?: nextDate
            startTime = nextStart
            // Kết thúc = bắt đầu + 1 giờ
            val endCal = java.util.Calendar.getInstance().apply {
                timeInMillis = TimeUtils.toMillis(startDate, startTime) + 3_600_000L
            }
            endDate = TimeUtils.format(endCal)
            endTime = SimpleDateFormat("HH:mm", Locale.US).format(endCal.time)
            chipsCategory.check(R.id.chipWork)
            val def = Prefs.defaultReminder(this)
            chipsReminder.check(reminderChips.entries.firstOrNull { it.value == def }?.key ?: R.id.rem30)
            findViewById<View>(R.id.btnDelete).visibility = View.GONE
        }
        updateUi()
    }

    private fun updateUi() {
        val is24 = Prefs.is24h(this)
        val allDay = switchAllDay.isChecked
        tvStartDate.text = TimeUtils.fmtDate(startDate)
        tvEndDate.text = TimeUtils.fmtDate(endDate)
        tvStartTime.text = TimeUtils.fmtTime(startTime, is24)
        tvEndTime.text = TimeUtils.fmtTime(endTime, is24)
        // Cả ngày: ẩn giờ và dòng kết thúc (tự đặt đến 23:59 cùng ngày)
        tvStartTime.visibility = if (allDay) View.GONE else View.VISIBLE
        rowEnd.visibility = if (allDay) View.GONE else View.VISIBLE
    }

    private fun save() {
        val title = etTitle.text?.toString()?.trim().orEmpty()
        if (title.isEmpty()) {
            tilTitle.error = "Vui lòng nhập tiêu đề"
            return
        }
        tilTitle.error = null

        var sd = startDate; var st = startTime; var ed = endDate; var et = endTime
        if (switchAllDay.isChecked) {
            st = "00:00"; et = "23:59"; ed = sd
        }
        if (TimeUtils.toMillis(ed, et) < TimeUtils.toMillis(sd, st)) {
            Toast.makeText(this, "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show()
            return
        }

        val categoryId = categoryChips[chipsCategory.checkedChipId] ?: 6L
        val categoryName = db.getCategories().firstOrNull { it.id == categoryId }?.name ?: "Khác"
        val remind = reminderChips[chipsReminder.checkedChipId] ?: 0

        var event = Event(
            id = editing?.id ?: 0,
            title = title,
            description = etNote.text?.toString()?.trim().orEmpty(),
            location = etLocation.text?.toString()?.trim().orEmpty(),
            startDate = sd, startTime = st, endDate = ed, endTime = et,
            remindMinutes = remind, categoryId = categoryId, categoryName = categoryName
        )
        if (editing == null) {
            event = event.copy(id = db.insert(event))
        } else {
            db.update(event)
        }
        AlarmScheduler.schedule(this, event)   // đặt (hoặc hủy nếu "Không nhắc") báo thức
        Toast.makeText(this, "Đã lưu sự kiện", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun pickDate(current: String, onPicked: (String) -> Unit) {
        val utc = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(utc.parse(current)?.time ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { onPicked(utc.format(java.util.Date(it))) }
        picker.show(supportFragmentManager, "date")
    }

    private fun pickTime(current: String, onPicked: (String) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(if (Prefs.is24h(this)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(current.substring(0, 2).toInt())
            .setMinute(current.substring(3, 5).toInt())
            .build()
        picker.addOnPositiveButtonClickListener {
            onPicked(String.format(Locale.US, "%02d:%02d", picker.hour, picker.minute))
        }
        picker.show(supportFragmentManager, "time")
    }
}
