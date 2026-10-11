package com.example.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

/** Một dòng trong danh sách: tiêu đề nhóm hoặc một sự kiện. */
sealed class Row {
    data class Header(val title: String, val count: Int) : Row()
    data class Item(val event: Event) : Row()
}

class EventAdapter(
    private var rows: List<Row> = emptyList(),
    private val onClick: (Event) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
        fun items(events: List<Event>): List<Row> = events.map { Row.Item(it) }

        /** Gom theo ngày: mỗi ngày một tiêu đề. */
        fun grouped(events: List<Event>, keyOf: (Event) -> String, titleOf: (String) -> String): List<Row> {
            val rows = ArrayList<Row>()
            events.groupBy(keyOf).forEach { (key, list) ->
                rows.add(Row.Header(titleOf(key), list.size))
                list.forEach { rows.add(Row.Item(it)) }
            }
            return rows
        }
    }

    fun submit(newRows: List<Row>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (rows[position] is Row.Header) TYPE_HEADER else TYPE_ITEM

    override fun getItemCount() = rows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER)
            HeaderVH(inflater.inflate(R.layout.item_section_header, parent, false))
        else
            ItemVH(inflater.inflate(R.layout.item_event, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as HeaderVH).bind(row)
            is Row.Item -> (holder as ItemVH).bind(row.event, onClick)
        }
    }

    class HeaderVH(v: View) : RecyclerView.ViewHolder(v) {
        private val title = v.findViewById<TextView>(R.id.tvSectionTitle)
        private val count = v.findViewById<TextView>(R.id.tvSectionCount)
        fun bind(h: Row.Header) {
            title.text = h.title
            count.text = "${h.count} sự kiện"
        }
    }

    class ItemVH(v: View) : RecyclerView.ViewHolder(v) {
        private val card = v.findViewById<MaterialCardView>(R.id.cardEvent)
        private val stripe = v.findViewById<View>(R.id.viewStripe)
        private val time = v.findViewById<TextView>(R.id.tvTime)
        private val tag = v.findViewById<TextView>(R.id.tvTag)
        private val title = v.findViewById<TextView>(R.id.tvTitle)
        private val location = v.findViewById<TextView>(R.id.tvLocation)
        private val reminder = v.findViewById<TextView>(R.id.tvReminder)
        private val done = v.findViewById<View>(R.id.ivDone)

        fun bind(e: Event, onClick: (Event) -> Unit) {
            val ctx = itemView.context
            val color = ContextCompat.getColor(ctx, CategoryStyle.color(e.categoryId))
            stripe.setBackgroundColor(color)
            tag.text = e.categoryName
            tag.setBackgroundResource(CategoryStyle.tagBg(e.categoryId))
            tag.setTextColor(color)
            time.text = e.timeLabel(Prefs.is24h(ctx))
            title.text = e.title
            location.text = e.location.ifBlank { "Chưa có địa điểm" }
            reminder.text = TimeUtils.reminderLabel(e.remindMinutes)

            val ended = e.isEnded()
            done.visibility = if (ended) View.VISIBLE else View.GONE

            // Sự kiện đang diễn ra: viền đậm màu chủ đạo
            val density = ctx.resources.displayMetrics.density
            if (e.isOngoing()) {
                card.strokeColor = ContextCompat.getColor(ctx, R.color.brand_primary)
                card.strokeWidth = (2 * density).toInt()
            } else {
                card.strokeColor = ContextCompat.getColor(ctx, R.color.stroke)
                card.strokeWidth = (1 * density).toInt()
            }
            itemView.setOnClickListener { onClick(e) }
        }
    }
}
