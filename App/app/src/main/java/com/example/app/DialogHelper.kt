package com.example.app

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object DialogHelper {

    private fun show(context: Context, view: View) =
        MaterialAlertDialogBuilder(context, R.style.Theme_EventCal_Dialog)
            .setView(view)
            .create()
            .also { d ->
                d.window?.setBackgroundDrawableResource(android.R.color.transparent)
                d.show()
                d.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

    /** Dialog "Xóa sự kiện này?" */
    fun confirmDeleteEvent(context: Context, event: Event, onConfirm: () -> Unit) {
        val v = LayoutInflater.from(context).inflate(R.layout.dialog_delete_event, null)
        v.findViewById<TextView>(R.id.tvDlgEventTitle).text = event.title
        v.findViewById<TextView>(R.id.tvDlgEventTime).text =
            "${TimeUtils.fmtDate(event.startDate)} • ${event.timeLabel(Prefs.is24h(context))}"
        val dialog = show(context, v)
        v.findViewById<View>(R.id.btnDlgCancel).setOnClickListener { dialog.dismiss() }
        v.findViewById<View>(R.id.btnDlgConfirm).setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }
    }

    /** Dialog "Xóa toàn bộ sự kiện?" */
    fun confirmDeleteAll(context: Context, total: Long, onConfirm: () -> Unit) {
        val v = LayoutInflater.from(context).inflate(R.layout.dialog_delete_all, null)
        v.findViewById<TextView>(R.id.tvImpactTotal).text = "Tổng cộng: $total sự kiện"
        val dialog = show(context, v)
        v.findViewById<View>(R.id.btnDlgKeep).setOnClickListener { dialog.dismiss() }
        v.findViewById<View>(R.id.btnDlgDeleteAll).setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }
    }
}
