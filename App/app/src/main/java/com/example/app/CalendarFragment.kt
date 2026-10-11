package com.example.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import java.util.Calendar

/** Lịch: chế độ Tháng / Tuần / Danh sách. */
class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    companion object {
        const val MODE_MONTH = 0
        const val MODE_WEEK = 1
        const val MODE_LIST = 2
    }

    private lateinit var db: DatabaseHelper
    private lateinit var rvDays: RecyclerView
    private lateinit var rvEvents: RecyclerView
    private lateinit var eventsAdapter: EventAdapter
    private lateinit var cardCalendar: View
    private lateinit var rowDayHeader: View
    private lateinit var tvEmpty: TextView
    private lateinit var tvMonthYear: TextView
    private lateinit var tvSelectedDay: TextView
    private lateinit var tvSelectedCount: TextView
    private lateinit var dows: List<TextView>

    private var mode = MODE_MONTH
    private var selected = TimeUtils.today()   // ngày đang chọn
    private var anchor = TimeUtils.today()     // tháng đang hiển thị (chế độ tháng)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        db = DatabaseHelper.get(ctx)

        rvDays = view.findViewById(R.id.rvCalendarDays)
        rvEvents = view.findViewById(R.id.rvDayEvents)
        cardCalendar = view.findViewById(R.id.cardCalendar)
        rowDayHeader = view.findViewById(R.id.rowDayHeader)
        tvEmpty = view.findViewById(R.id.tvEmptyDay)
        tvMonthYear = view.findViewById(R.id.tvMonthYear)
        tvSelectedDay = view.findViewById(R.id.tvSelectedDay)
        tvSelectedCount = view.findViewById(R.id.tvSelectedCount)
        dows = listOf(R.id.dow0, R.id.dow1, R.id.dow2, R.id.dow3, R.id.dow4, R.id.dow5, R.id.dow6)
            .map { view.findViewById(it) }

        eventsAdapter = EventAdapter { EventDetailActivity.start(ctx, it.id) }
        rvEvents.adapter = eventsAdapter

        // Chế độ xem
        mode = Prefs.defaultView(ctx)
        val toggle = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleViewMode)
        toggle.check(
            when (mode) {
                MODE_WEEK -> R.id.btnModeWeek
                MODE_LIST -> R.id.btnModeList
                else -> R.id.btnModeMonth
            }
        )
        toggle.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            mode = when (id) {
                R.id.btnModeWeek -> MODE_WEEK
                R.id.btnModeList -> MODE_LIST
                else -> MODE_MONTH
            }
            if (mode == MODE_MONTH) anchor = selected
            refresh()
        }

        view.findViewById<View>(R.id.btnPrevMonth).setOnClickListener { shift(-1) }
        view.findViewById<View>(R.id.btnNextMonth).setOnClickListener { shift(1) }
        view.findViewById<View>(R.id.btnToday).setOnClickListener {
            selected = TimeUtils.today()
            anchor = selected
            refresh()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()   // cập nhật khi quay lại từ màn hình thêm/sửa/xóa
    }

    private fun shift(delta: Int) {
        if (mode == MODE_WEEK) {
            selected = TimeUtils.addDays(selected, 7 * delta)
            anchor = selected
        } else {
            val c = TimeUtils.calendar(anchor).apply {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, delta)
            }
            anchor = TimeUtils.format(c)
            val today = TimeUtils.today()
            selected = if (today.substring(0, 7) == anchor.substring(0, 7)) today else anchor
        }
        refresh()
    }

    private fun refresh() {
        val ctx = requireContext()
        updateWeekdayHeader()

        val listMode = mode == MODE_LIST
        cardCalendar.visibility = if (listMode) View.GONE else View.VISIBLE
        rowDayHeader.visibility = if (listMode) View.GONE else View.VISIBLE

        if (!listMode) {
            val cells = if (mode == MODE_WEEK) weekCells() else monthCells()
            val real = cells.filterNotNull()
            val eventDates = db.getEventDates(real.first(), real.last())
            rvDays.adapter = CalendarDayAdapter(cells, eventDates, selected, TimeUtils.today()) {
                selected = it
                loadEvents()
            }
            tvMonthYear.text = if (mode == MODE_WEEK)
                "${TimeUtils.fmtDayMonth(real.first())} - ${TimeUtils.fmtDate(real.last())}"
            else
                "Tháng ${TimeUtils.month(anchor)}, ${TimeUtils.year(anchor)}"
        }
        loadEvents()
    }

    private fun loadEvents() {
        val is24 = Prefs.is24h(requireContext())
        if (mode == MODE_LIST) {
            val all = db.getAll()
            eventsAdapter.submit(
                EventAdapter.grouped(all, { it.startDate }) {
                    "${TimeUtils.weekdayName(it)}, ${TimeUtils.fmtDate(it)}"
                }
            )
            tvEmpty.visibility = if (all.isEmpty()) View.VISIBLE else View.GONE
        } else {
            val list = db.getByDate(selected)
            tvSelectedDay.text = "Sự kiện ngày ${TimeUtils.day(selected)} Th${TimeUtils.month(selected)}"
            tvSelectedCount.text = "${list.size} sự kiện"
            eventsAdapter.submit(EventAdapter.items(list))
            tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    /** Tên thứ theo tuần bắt đầu từ T2 hoặc CN (cài đặt trong SharedPreferences). */
    private fun updateWeekdayHeader() {
        val ctx = requireContext()
        val base = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        val labels = if (Prefs.weekStart(ctx) == Calendar.SUNDAY) listOf("CN") + base.dropLast(1) else base
        dows.forEachIndexed { i, tv ->
            tv.text = labels[i]
            tv.setTextColor(
                ContextCompat.getColor(ctx, if (labels[i] == "CN") R.color.danger else R.color.text_secondary)
            )
        }
    }

    private fun monthCells(): List<String?> {
        val ws = Prefs.weekStart(requireContext())
        val first = TimeUtils.calendar(anchor).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val offset = (first.get(Calendar.DAY_OF_WEEK) - ws + 7) % 7
        val total = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        val ym = TimeUtils.format(first).substring(0, 8)   // "yyyy-MM-"
        val cells = ArrayList<String?>()
        repeat(offset) { cells.add(null) }
        for (d in 1..total) cells.add(ym + String.format("%02d", d))
        return cells
    }

    private fun weekCells(): List<String?> {
        val ws = Prefs.weekStart(requireContext())
        val cal = TimeUtils.calendar(selected)
        val diff = (cal.get(Calendar.DAY_OF_WEEK) - ws + 7) % 7
        val start = TimeUtils.addDays(selected, -diff)
        return (0..6).map { TimeUtils.addDays(start, it) }
    }
}
