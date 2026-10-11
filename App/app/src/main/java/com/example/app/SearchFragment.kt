package com.example.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

/** Tìm theo tiêu đề / mô tả + lọc theo danh mục. */
class SearchFragment : Fragment(R.layout.fragment_search) {

    private lateinit var db: DatabaseHelper
    private lateinit var adapter: EventAdapter
    private var query = ""
    private var categoryId: Long? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ctx = requireContext()
        db = DatabaseHelper.get(ctx)

        adapter = EventAdapter { showDetail(it) }
        view.findViewById<RecyclerView>(R.id.rvResults).adapter = adapter

        view.findViewById<TextInputEditText>(R.id.etSearch).doAfterTextChanged {
            query = it?.toString().orEmpty()
            refresh()
        }

        view.findViewById<ChipGroup>(R.id.chipGroupFilter).setOnCheckedStateChangeListener { _, ids ->
            categoryId = when (ids.firstOrNull()) {
                R.id.fWork -> 1L
                R.id.fPersonal -> 2L
                R.id.fFamily -> 3L
                R.id.fFriends -> 4L
                R.id.fHealth -> 5L
                R.id.fOther -> 6L
                else -> null
            }
            refresh()
        }

        view.findViewById<View>(R.id.btnBannerClose).setOnClickListener {
            view.findViewById<View>(R.id.bannerReminder).visibility = View.GONE
        }
        view.findViewById<View>(R.id.bannerReminder).visibility = View.GONE   // banner chỉ là minh họa
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val v = view ?: return
        val result = db.search(query, categoryId)
        adapter.submit(EventAdapter.items(result))
        v.findViewById<TextView>(R.id.tvResultTitle).text = "Kết quả tìm kiếm (${result.size})"
        v.findViewById<TextView>(R.id.tvSearchHint).text = "Truy vấn SQLite: ${result.size} kết quả phù hợp"
        val card = v.findViewById<View>(R.id.cardDetail)
        if (result.isEmpty()) card.visibility = View.GONE else showDetail(result.first())
    }

    private fun showDetail(e: Event) {
        val v = view ?: return
        val ctx = requireContext()
        v.findViewById<View>(R.id.cardDetail).visibility = View.VISIBLE

        v.findViewById<TextView>(R.id.tvDetailTag).apply {
            text = e.categoryName
            setBackgroundResource(CategoryStyle.tagBg(e.categoryId))
            setTextColor(ContextCompat.getColor(ctx, CategoryStyle.color(e.categoryId)))
        }
        v.findViewById<TextView>(R.id.tvDetailTitle).text = e.title
        v.findViewById<TextView>(R.id.tvDetailDesc).text = e.description.ifBlank { "Không có mô tả" }
        v.findViewById<TextView>(R.id.tvDetailTime).text = e.timeLabel(Prefs.is24h(ctx))
        v.findViewById<TextView>(R.id.tvDetailDate).text =
            if (e.startDate == e.endDate) "${TimeUtils.weekdayName(e.startDate)}, ${TimeUtils.fmtDate(e.startDate)}"
            else "${TimeUtils.fmtDate(e.startDate)} → ${TimeUtils.fmtDate(e.endDate)}"
        v.findViewById<TextView>(R.id.tvDetailPlace).text = e.location.ifBlank { "Chưa có địa điểm" }
        v.findViewById<TextView>(R.id.tvDetailReminder).text = TimeUtils.reminderLabel(e.remindMinutes)
        v.findViewById<View>(R.id.tvDetailPeople).visibility = View.GONE

        v.findViewById<View>(R.id.btnDetailEdit).setOnClickListener {
            EventEditActivity.start(ctx, e.id)
        }
        v.findViewById<View>(R.id.btnDetailDelete).setOnClickListener {
            DialogHelper.confirmDeleteEvent(ctx, e) {
                AlarmScheduler.cancel(ctx, e.id)
                db.delete(e.id)
                refresh()
            }
        }
    }
}
