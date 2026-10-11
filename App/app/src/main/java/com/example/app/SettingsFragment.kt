package com.example.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.Calendar

/** Cài đặt: SharedPreferences + quản lý dữ liệu SQLite. */
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var db: DatabaseHelper

    // Chọn nơi lưu file sao lưu / chọn file khôi phục (Storage Access Framework, không cần xin quyền)
    private val createBackup = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) toast(if (db.exportTo(uri)) "Đã sao lưu dữ liệu" else "Sao lưu thất bại")
    }

    private val openBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        val ctx = requireContext()
        AlarmScheduler.cancelAll(ctx)                 // hủy báo thức của dữ liệu cũ
        val ok = db.importFrom(uri)
        AlarmScheduler.rescheduleAll(ctx)             // đặt báo thức theo dữ liệu mới
        toast(if (ok) "Đã khôi phục dữ liệu" else "File không hợp lệ")
        refreshStats()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        db = DatabaseHelper.get(ctx)

        // --- Tuần bắt đầu từ ---
        view.findViewById<MaterialButtonToggleGroup>(R.id.toggleWeekStart).apply {
            check(if (Prefs.weekStart(ctx) == Calendar.SUNDAY) R.id.btnSunday else R.id.btnMonday)
            addOnButtonCheckedListener { _, id, checked ->
                if (checked) Prefs.setWeekStart(ctx, if (id == R.id.btnSunday) Calendar.SUNDAY else Calendar.MONDAY)
            }
        }

        // --- Chế độ xem mặc định ---
        view.findViewById<MaterialButtonToggleGroup>(R.id.toggleDefaultView).apply {
            check(
                when (Prefs.defaultView(ctx)) {
                    CalendarFragment.MODE_WEEK -> R.id.btnViewWeek
                    CalendarFragment.MODE_LIST -> R.id.btnViewList
                    else -> R.id.btnViewMonth
                }
            )
            addOnButtonCheckedListener { _, id, checked ->
                if (checked) Prefs.setDefaultView(
                    ctx, when (id) {
                        R.id.btnViewWeek -> CalendarFragment.MODE_WEEK
                        R.id.btnViewList -> CalendarFragment.MODE_LIST
                        else -> CalendarFragment.MODE_MONTH
                    }
                )
            }
        }

        // --- Định dạng giờ ---
        view.findViewById<MaterialButtonToggleGroup>(R.id.toggleTimeFormat).apply {
            check(if (Prefs.is24h(ctx)) R.id.btn24h else R.id.btn12h)
            addOnButtonCheckedListener { _, id, checked ->
                if (checked) Prefs.setIs24h(ctx, id == R.id.btn24h)
            }
        }

        // --- Giao diện Sáng / Tối / Tự động ---
        view.findViewById<MaterialButtonToggleGroup>(R.id.toggleTheme).apply {
            check(
                when (Prefs.nightMode(ctx)) {
                    AppCompatDelegate.MODE_NIGHT_NO -> R.id.btnThemeLight
                    AppCompatDelegate.MODE_NIGHT_YES -> R.id.btnThemeDark
                    else -> R.id.btnThemeAuto
                }
            )
            addOnButtonCheckedListener { _, id, checked ->
                if (!checked) return@addOnButtonCheckedListener
                val mode = when (id) {
                    R.id.btnThemeLight -> AppCompatDelegate.MODE_NIGHT_NO
                    R.id.btnThemeDark -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                if (mode != Prefs.nightMode(ctx)) Prefs.saveTheme(ctx, mode)
            }
        }

        // --- Bật / tắt nhắc nhở ---
        view.findViewById<MaterialSwitch>(R.id.switchAlarm).apply {
            isChecked = Prefs.alarmEnabled(ctx)
            setOnCheckedChangeListener { _, on ->
                Prefs.setAlarmEnabled(ctx, on)
                if (on) AlarmScheduler.rescheduleAll(ctx) else AlarmScheduler.cancelAll(ctx)
            }
        }

        // --- Nhắc nhở mặc định ---
        val tvDefault = view.findViewById<TextView>(R.id.tvDefaultReminder)
        tvDefault.text = TimeUtils.reminderLabel(Prefs.defaultReminder(ctx))
        tvDefault.setOnClickListener {
            val values = intArrayOf(0, 1, 5, 10, 30, 60, 1440)
            val labels = values.map { TimeUtils.reminderLabel(it) }.toTypedArray()
            MaterialAlertDialogBuilder(ctx)
                .setTitle("Thời gian nhắc mặc định")
                .setSingleChoiceItems(labels, values.indexOf(Prefs.defaultReminder(ctx))) { d, which ->
                    Prefs.setDefaultReminder(ctx, values[which])
                    tvDefault.text = labels[which]
                    d.dismiss()
                }.show()
        }
        view.findViewById<View>(R.id.btnPickSound).setOnClickListener {
            toast("Đang dùng âm thanh thông báo mặc định của hệ thống")
        }

        // --- Quản lý dữ liệu ---
        view.findViewById<View>(R.id.rowBackup).setOnClickListener { createBackup.launch("eventcal_backup.sqlite") }
        view.findViewById<View>(R.id.rowRestore).setOnClickListener { openBackup.launch(arrayOf("*/*")) }
        view.findViewById<View>(R.id.rowDeleteAll).setOnClickListener {
            DialogHelper.confirmDeleteAll(ctx, db.count()) {
                AlarmScheduler.cancelAll(ctx)
                db.deleteAll()
                refreshStats()
                toast("Đã xóa toàn bộ sự kiện")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStats()
    }

    private fun refreshStats() {
        val v = view ?: return
        v.findViewById<TextView>(R.id.tvStorageInfo).text =
            "Đã lưu ${db.count()} sự kiện trên bộ nhớ thiết bị. Mọi dữ liệu thuộc về bạn, không đồng bộ máy chủ bên thứ ba."
        v.findViewById<TextView>(R.id.tvDbSize).text = "Kích thước file db: ${db.dbSizeKb()} KB"
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
