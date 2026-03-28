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

class RoomStatsAdapter(private val rooms: List<StatisticsManager.RoomStatistics>) :
    RecyclerView.Adapter<RoomStatsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRoomName: TextView = view.findViewById(R.id.tvRoomName)
        val tvRoomStats: TextView = view.findViewById(R.id.tvRoomStats)
        val tvRoomGifts: TextView = view.findViewById(R.id.tvRoomGifts)
        val tvLastTime: TextView = view.findViewById(R.id.tvLastTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room_stats, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val room = rooms[position]

        holder.tvRoomName.text = room.roomName.ifEmpty { "直播间 ${position + 1}" }

        val successRate = String.format("%.1f", room.getSuccessRate())
        holder.tvRoomStats.text = "点击: ${room.totalClicks} 成功: ${room.successCount} 失败: ${room.failCount} 成功率: ${successRate}%"

        holder.tvRoomGifts.text = "礼物: ${room.totalGifts} 价值: ${String.format("%.2f", room.totalValue)}"

        val lastTime = if (room.lastSuccessTime > 0) {
            formatTime(room.lastSuccessTime)
        } else if (room.lastAttemptTime > 0) {
            formatTime(room.lastAttemptTime)
        } else {
            "无记录"
        }
        holder.tvLastTime.text = "最后操作: $lastTime"
    }

    override fun getItemCount() = rooms.size

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
