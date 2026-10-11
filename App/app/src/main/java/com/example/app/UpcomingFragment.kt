package com.example.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.progressindicator.LinearProgressIndicator

/** Sự kiện sắp tới (từ hôm nay), sự kiện đang diễn ra được làm nổi bật. */
class UpcomingFragment : Fragment(R.layout.fragment_upcoming) {

    private lateinit var db: DatabaseHelper
    private lateinit var adapter: EventAdapter
    private var onlyImportant = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        db = DatabaseHelper.get(requireContext())
        adapter = EventAdapter { EventDetailActivity.start(requireContext(), it.id) }
        view.findViewById<RecyclerView>(R.id.rvUpcoming).adapter = adapter

        // "Quan trọng" = danh mục Công việc (mã 1)
        view.findViewById<MaterialButtonToggleGroup>(R.id.toggleImportance)
            .addOnButtonCheckedListener { _, id, checked ->
                if (checked) {
                    onlyImportant = id == R.id.btnImportant
                    render()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun groupTitle(date: String): String {
        val dm = "${TimeUtils.day(date)} THÁNG ${TimeUtils.month(date)}"
        val today = TimeUtils.today()
        return when (date) {
            today -> "HÔM NAY - $dm"
            TimeUtils.addDays(today, 1) -> "NGÀY MAI - $dm"
            else -> "${TimeUtils.weekdayName(date).uppercase()} - $dm"
        }
    }

    private fun render() {
        val v = view ?: return
        val today = TimeUtils.today()
        val events = db.getUpcoming().filter { !onlyImportant || it.categoryId == 1L }

        adapter.submit(EventAdapter.grouped(events, { maxOf(it.startDate, today) }) { groupTitle(it) })
        v.findViewById<TextView>(R.id.tvUpcomingCount).text = "${events.size} sự kiện từ hôm nay"
        bindHero(v, events.firstOrNull { it.isOngoing() })
    }

    /** Thẻ nổi bật cho sự kiện đang diễn ra. */
    private fun bindHero(v: View, e: Event?) {
        val card = v.findViewById<View>(R.id.cardHero)
        val label = v.findViewById<View>(R.id.tvHappeningLabel)
        if (e == null) {
            card.visibility = View.GONE
            label.visibility = View.GONE
            return
        }
        card.visibility = View.VISIBLE
        label.visibility = View.VISIBLE
        v.findViewById<TextView>(R.id.tvHeroTitle).text = e.title
        v.findViewById<TextView>(R.id.tvHeroDesc).text = e.description.ifBlank { e.categoryName }
        v.findViewById<TextView>(R.id.tvHeroTime).text = e.timeLabel(Prefs.is24h(requireContext()))
        v.findViewById<TextView>(R.id.tvHeroPlace).text = e.location.ifBlank { "—" }

        val total = (e.endMillis - e.startMillis).coerceAtLeast(1)
        val percent = ((System.currentTimeMillis() - e.startMillis) * 100 / total).toInt().coerceIn(0, 100)
        v.findViewById<TextView>(R.id.tvHeroPercent).text = "$percent% hoàn thành"
        v.findViewById<LinearProgressIndicator>(R.id.progressHero).progress = percent

        v.findViewById<View>(R.id.btnHeroMute).visibility = View.GONE
        v.findViewById<View>(R.id.btnHeroDetail).setOnClickListener {
            EventDetailActivity.start(requireContext(), e.id)
        }
    }
}
