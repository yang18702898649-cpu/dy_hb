package com.example.douyinredpacket

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.douyinredpacket.data.StatisticsManager
import com.google.android.material.tabs.TabLayout

class StatisticsActivity : AppCompatActivity() {

    private lateinit var statsManager: StatisticsManager
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvSummary: TextView
    private lateinit var btnClear: Button
    private lateinit var btnExport: Button
    private lateinit var btnRefresh: Button

    private var currentTab = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        statsManager = StatisticsManager(this)

        initViews()
        updateData()
    }

    private fun initViews() {
        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.recyclerView)
        tvSummary = findViewById(R.id.tvSummary)
        btnClear = findViewById(R.id.btnClear)
        btnExport = findViewById(R.id.btnExport)
        btnRefresh = findViewById(R.id.btnRefresh)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // 设置Tab
        tabLayout.addTab(tabLayout.newTab().setText("房间统计"))
        tabLayout.addTab(tabLayout.newTab().setText("历史记录"))
        tabLayout.addTab(tabLayout.newTab().setText("排行榜"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                updateData()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        btnClear.setOnClickListener {
            statsManager.clearAll()
            updateData()
        }

        btnExport.setOnClickListener {
            exportReport()
        }

        btnRefresh.setOnClickListener {
            updateData()
        }
    }

    private fun updateData() {
        when (currentTab) {
            0 -> showRoomStats()
            1 -> showHistoryRecords()
            2 -> showRankings()
        }
        updateSummary()
    }

    private fun showRoomStats() {
        val rooms = statsManager.getAllRoomStats()
        val adapter = RoomStatsAdapter(rooms)
        recyclerView.adapter = adapter
    }

    private fun showHistoryRecords() {
        val records = statsManager.getHistoryRecords(100)
        val adapter = HistoryRecordAdapter(records)
        recyclerView.adapter = adapter
    }

    private fun showRankings() {
        val bestRooms = statsManager.getBestRooms(10)
        val adapter = RoomRankingAdapter(bestRooms)
        recyclerView.adapter = adapter
    }

    private fun updateSummary() {
        val total = statsManager.getTotalStats()
        val today = statsManager.getTodayStats()

        val sb = StringBuilder()
        sb.appendLine("【总计】点击: ${total.totalClicks} 成功: ${total.successCount} 失败: ${total.failCount}")
        sb.appendLine("成功率: ${String.format("%.1f", total.getSuccessRate())}% 礼物: ${total.totalGifts} 价值: ${String.format("%.2f", total.totalValue)}")
        sb.appendLine()
        sb.appendLine("【今日】点击: ${today.totalClicks} 成功: ${today.successCount} 失败: ${today.failCount}")
        sb.appendLine("成功率: ${String.format("%.1f", today.getSuccessRate())}% 礼物: ${today.totalGifts} 价值: ${String.format("%.2f", today.totalValue)}")

        tvSummary.text = sb.toString()
    }

    private fun exportReport() {
        val report = statsManager.generateReport()
        // 这里可以实现导出到文件的功能
        tvSummary.text = report
    }
}
