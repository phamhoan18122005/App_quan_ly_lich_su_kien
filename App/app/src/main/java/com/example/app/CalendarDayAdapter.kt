package com.example.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

/** items: ngày dạng "yyyy-MM-dd", null = ô trống đầu tháng. */
class CalendarDayAdapter(
    private val items: List<String?>,
    private val eventDates: Set<String>,
    private var selected: String,
    private val today: String,
    private val onSelect: (String) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvDay: TextView = v.findViewById(R.id.tvDay)
        val dot: View = v.findViewById(R.id.dotEvent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ctx = holder.itemView.context
        val date = items[position]
        if (date == null) {
            holder.tvDay.text = ""
            holder.tvDay.setBackgroundResource(0)
            holder.dot.visibility = View.GONE
            holder.itemView.setOnClickListener(null)
            holder.itemView.isClickable = false
            return
        }
        holder.tvDay.text = TimeUtils.day(date).toString()
        when (date) {
            selected -> {
                holder.tvDay.setBackgroundResource(R.drawable.bg_day_selected)
                holder.tvDay.setTextColor(ContextCompat.getColor(ctx, R.color.on_primary))
            }
            today -> {
                holder.tvDay.setBackgroundResource(R.drawable.bg_day_today)
                holder.tvDay.setTextColor(ContextCompat.getColor(ctx, R.color.brand_primary))
            }
            else -> {
                holder.tvDay.setBackgroundResource(0)
                holder.tvDay.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            }
        }
        // Chấm tròn đánh dấu ngày có sự kiện
        holder.dot.visibility = if (date in eventDates) View.VISIBLE else View.GONE
        holder.itemView.isClickable = true
        holder.itemView.setOnClickListener {
            selected = date
            notifyDataSetChanged()
            onSelect(date)
        }
    }
}
