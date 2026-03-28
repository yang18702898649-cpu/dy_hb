package com.example.douyinredpacket.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.douyinredpacket.R
import com.example.douyinredpacket.data.StatisticsManager

class RoomRankingAdapter(private val rooms: List<StatisticsManager.RoomStatistics>) :
    RecyclerView.Adapter<RoomRankingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val tvRoomName: TextView = view.findViewById(R.id.tvRoomName)
        val tvSuccessRate: TextView = view.findViewById(R.id.tvSuccessRate)
        val tvStats: TextView = view.findViewById(R.id.tvStats)
        val tvValue: TextView = view.findViewById(R.id.tvValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room_ranking, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val room = rooms[position]

        // 排名
        holder.tvRank.text = "${position + 1}"
        when (position) {
            0 -> holder.tvRank.setTextColor(0xFFFFD700.toInt()) // 金色
            1 -> holder.tvRank.setTextColor(0xFFC0C0C0.toInt()) // 银色
            2 -> holder.tvRank.setTextColor(0xFFCD7F32.toInt()) // 铜色
            else -> holder.tvRank.setTextColor(0xFF666666.toInt())
        }

        holder.tvRoomName.text = room.roomName.ifEmpty { "直播间 ${position + 1}" }

        val successRate = String.format("%.1f", room.getSuccessRate())
        holder.tvSuccessRate.text = "${successRate}%"

        holder.tvStats.text = "${room.successCount}/${room.totalClicks}"
        holder.tvValue.text = "价值: ${String.format("%.2f", room.totalValue)}"
    }

    override fun getItemCount() = rooms.size
}
