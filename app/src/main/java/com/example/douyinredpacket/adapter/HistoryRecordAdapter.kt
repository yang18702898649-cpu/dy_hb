package com.example.douyinredpacket.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.douyinredpacket.R
import com.example.douyinredpacket.data.StatisticsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryRecordAdapter(private val records: List<StatisticsManager.GrabRecord>) :
    RecyclerView.Adapter<HistoryRecordAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvRoom: TextView = view.findViewById(R.id.tvRoom)
        val tvResult: TextView = view.findViewById(R.id.tvResult)
        val tvGift: TextView = view.findViewById(R.id.tvGift)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]

        holder.tvTime.text = formatTime(record.timestamp)
        holder.tvRoom.text = record.roomName.ifEmpty { "直播间" }

        if (record.isSuccess) {
            holder.tvResult.text = "✅ 成功"
            holder.tvResult.setTextColor(0xFF4CAF50.toInt())
            holder.tvGift.text = "${record.giftType} x${record.giftCount} (价值: ${String.format("%.2f", record.giftValue)})"
            holder.tvGift.visibility = View.VISIBLE
        } else {
            holder.tvResult.text = "❌ 失败"
            holder.tvResult.setTextColor(0xFFF44336.toInt())
            holder.tvGift.text = "原因: ${record.failReason}"
            holder.tvGift.visibility = View.VISIBLE
        }
    }

    override fun getItemCount() = records.size

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }
}
