package com.example.douyinredpacket.data

import android.content.Context
import android.content.SharedPreferences
import com.example.douyinredpacket.MainActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 统计管理器 - 管理抢红包的详细统计数据
 */
class StatisticsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    // 内存中的统计数据
    private var roomStatsMap: MutableMap<String, RoomStatistics> = mutableMapOf()
    private var totalStats: TotalStatistics = TotalStatistics()
    private var todayStats: DayStatistics = DayStatistics()
    private var historyRecords: MutableList<GrabRecord> = mutableListOf()

    companion object {
        private const val PREFS_NAME = "red_packet_stats"
        private const val KEY_ROOM_STATS = "room_stats"
        private const val KEY_TOTAL_STATS = "total_stats"
        private const val KEY_TODAY_STATS = "today_stats"
        private const val KEY_HISTORY = "history_records"
        private const val MAX_HISTORY_SIZE = 500 // 最大历史记录数
    }

    init {
        loadData()
    }

    /**
     * 加载保存的数据
     */
    private fun loadData() {
        // 加载房间统计
        val roomStatsJson = prefs.getString(KEY_ROOM_STATS, "{}")
        val roomType = object : TypeToken<MutableMap<String, RoomStatistics>>() {}.type
        roomStatsMap = gson.fromJson(roomStatsJson, roomType) ?: mutableMapOf()

        // 加载总计统计
        val totalJson = prefs.getString(KEY_TOTAL_STATS, "{}")
        totalStats = gson.fromJson(totalJson, TotalStatistics::class.java) ?: TotalStatistics()

        // 加载今日统计
        val todayJson = prefs.getString(KEY_TODAY_STATS, "{}")
        todayStats = gson.fromJson(todayJson, DayStatistics::class.java) ?: DayStatistics()

        // 检查是否需要重置今日统计
        checkAndResetTodayStats()

        // 加载历史记录
        val historyJson = prefs.getString(KEY_HISTORY, "[]")
        val historyType = object : TypeToken<MutableList<GrabRecord>>() {}.type
        historyRecords = gson.fromJson(historyJson, historyType) ?: mutableListOf()
    }

    /**
     * 保存数据
     */
    private fun saveData() {
        prefs.edit().apply {
            putString(KEY_ROOM_STATS, gson.toJson(roomStatsMap))
            putString(KEY_TOTAL_STATS, gson.toJson(totalStats))
            putString(KEY_TODAY_STATS, gson.toJson(todayStats))
            putString(KEY_HISTORY, gson.toJson(historyRecords))
            apply()
        }
    }

    /**
     * 检查并重置今日统计
     */
    private fun checkAndResetTodayStats() {
        val today = getTodayString()
        if (todayStats.date != today) {
            todayStats = DayStatistics(date = today)
        }
    }

    /**
     * 记录一次抢红包尝试
     */
    fun recordAttempt(roomId: String, roomName: String = "") {
        checkAndResetTodayStats()

        // 更新房间统计
        val roomStats = roomStatsMap.getOrPut(roomId) {
            RoomStatistics(roomId = roomId, roomName = roomName)
        }
        roomStats.totalClicks++
        roomStats.lastAttemptTime = System.currentTimeMillis()

        // 更新总计
        totalStats.totalClicks++

        // 更新今日
        todayStats.totalClicks++

        saveData()

        MainActivity.addLog("房间[$roomId] 第${roomStats.totalClicks}次尝试抢红包")
    }

    /**
     * 记录抢红包成功
     */
    fun recordSuccess(
        roomId: String,
        roomName: String = "",
        giftType: String = "未知",
        giftCount: Int = 1,
        giftValue: Double = 0.0
    ) {
        checkAndResetTodayStats()

        val timestamp = System.currentTimeMillis()

        // 更新房间统计
        val roomStats = roomStatsMap.getOrPut(roomId) {
            RoomStatistics(roomId = roomId, roomName = roomName)
        }
        roomStats.successCount++
        roomStats.totalGifts += giftCount
        roomStats.totalValue += giftValue
        roomStats.lastSuccessTime = timestamp

        // 添加获得的礼物
        val gift = GiftInfo(
            type = giftType,
            count = giftCount,
            value = giftValue,
            timestamp = timestamp
        )
        roomStats.gifts.add(gift)

        // 更新总计
        totalStats.successCount++
        totalStats.totalGifts += giftCount
        totalStats.totalValue += giftValue

        // 更新今日
        todayStats.successCount++
        todayStats.totalGifts += giftCount
        todayStats.totalValue += giftValue

        // 添加历史记录
        val record = GrabRecord(
            roomId = roomId,
            roomName = roomName,
            giftType = giftType,
            giftCount = giftCount,
            giftValue = giftValue,
            timestamp = timestamp,
            isSuccess = true
        )
        addHistoryRecord(record)

        saveData()

        MainActivity.addLog("✅ 房间[$roomId] 抢到红包: $giftType x$giftCount")
    }

    /**
     * 记录抢红包失败
     */
    fun recordFail(roomId: String, roomName: String = "", reason: String = "") {
        checkAndResetTodayStats()

        val timestamp = System.currentTimeMillis()

        // 更新房间统计
        val roomStats = roomStatsMap.getOrPut(roomId) {
            RoomStatistics(roomId = roomId, roomName = roomName)
        }
        roomStats.failCount++
        roomStats.lastFailTime = timestamp
        if (reason.isNotEmpty()) {
            roomStats.failReasons[reason] = roomStats.failReasons.getOrDefault(reason, 0) + 1
        }

        // 更新总计
        totalStats.failCount++

        // 更新今日
        todayStats.failCount++

        // 添加历史记录
        val record = GrabRecord(
            roomId = roomId,
            roomName = roomName,
            timestamp = timestamp,
            isSuccess = false,
            failReason = reason
        )
        addHistoryRecord(record)

        saveData()

        MainActivity.addLog("❌ 房间[$roomId] 抢红包失败: $reason")
    }

    /**
     * 添加历史记录
     */
    private fun addHistoryRecord(record: GrabRecord) {
        historyRecords.add(0, record) // 新记录放前面
        if (historyRecords.size > MAX_HISTORY_SIZE) {
            historyRecords.removeAt(historyRecords.size - 1)
        }
    }

    /**
     * 获取房间统计
     */
    fun getRoomStats(roomId: String): RoomStatistics? {
        return roomStatsMap[roomId]
    }

    /**
     * 获取所有房间统计
     */
    fun getAllRoomStats(): List<RoomStatistics> {
        return roomStatsMap.values.toList().sortedByDescending { it.lastAttemptTime }
    }

    /**
     * 获取总计统计
     */
    fun getTotalStats(): TotalStatistics {
        return totalStats
    }

    /**
     * 获取今日统计
     */
    fun getTodayStats(): DayStatistics {
        checkAndResetTodayStats()
        return todayStats
    }

    /**
     * 获取历史记录
     */
    fun getHistoryRecords(limit: Int = 100): List<GrabRecord> {
        return historyRecords.take(limit)
    }

    /**
     * 获取成功率最高的房间
     */
    fun getBestRooms(limit: Int = 5): List<RoomStatistics> {
        return roomStatsMap.values
            .filter { it.totalClicks >= 5 } // 至少尝试5次
            .sortedByDescending { it.getSuccessRate() }
            .take(limit)
    }

    /**
     * 获取最活跃的房间
     */
    fun getMostActiveRooms(limit: Int = 5): List<RoomStatistics> {
        return roomStatsMap.values
            .sortedByDescending { it.totalClicks }
            .take(limit)
    }

    /**
     * 获取收益最高的房间
     */
    fun getMostProfitableRooms(limit: Int = 5): List<RoomStatistics> {
        return roomStatsMap.values
            .sortedByDescending { it.totalValue }
            .take(limit)
    }

    /**
     * 生成统计报告
     */
    fun generateReport(): String {
        val sb = StringBuilder()
        sb.appendLine("========== 抢红包统计报告 ==========")
        sb.appendLine()

        // 总计
        sb.appendLine("【总计统计】")
        sb.appendLine("  总点击次数: ${totalStats.totalClicks}")
        sb.appendLine("  成功次数: ${totalStats.successCount}")
        sb.appendLine("  失败次数: ${totalStats.failCount}")
        sb.appendLine("  成功率: ${String.format("%.2f", totalStats.getSuccessRate())}%")
        sb.appendLine("  总礼物数: ${totalStats.totalGifts}")
        sb.appendLine("  总价值: ${String.format("%.2f", totalStats.totalValue)}")
        sb.appendLine()

        // 今日
        sb.appendLine("【今日统计】")
        sb.appendLine("  点击次数: ${todayStats.totalClicks}")
        sb.appendLine("  成功次数: ${todayStats.successCount}")
        sb.appendLine("  失败次数: ${todayStats.failCount}")
        sb.appendLine("  成功率: ${String.format("%.2f", todayStats.getSuccessRate())}%")
        sb.appendLine()

        // 房间排名
        sb.appendLine("【房间排名 TOP 5】")
        getBestRooms(5).forEachIndexed { index, room ->
            sb.appendLine("  ${index + 1}. ${room.roomName}(${room.roomId})")
            sb.appendLine("     成功率: ${String.format("%.2f", room.getSuccessRate())}% " +
                    "(${room.successCount}/${room.totalClicks})")
            sb.appendLine("     获得礼物: ${room.totalGifts} 价值: ${String.format("%.2f", room.totalValue)}")
        }
        sb.appendLine()

        // 最近记录
        sb.appendLine("【最近10条记录】")
        getHistoryRecords(10).forEach { record ->
            val time = formatTime(record.timestamp)
            if (record.isSuccess) {
                sb.appendLine("  [$time] ✅ ${record.roomName} 获得 ${record.giftType} x${record.giftCount}")
            } else {
                sb.appendLine("  [$time] ❌ ${record.roomName} 失败: ${record.failReason}")
            }
        }

        return sb.toString()
    }

    /**
     * 清空所有统计
     */
    fun clearAll() {
        roomStatsMap.clear()
        totalStats = TotalStatistics()
        todayStats = DayStatistics()
        historyRecords.clear()
        saveData()
        MainActivity.addLog("已清空所有统计数据")
    }

    /**
     * 获取今日日期字符串
     */
    private fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    /**
     * 格式化时间
     */
    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    // ==================== 数据类 ====================

    /**
     * 房间统计
     */
    data class RoomStatistics(
        val roomId: String,
        var roomName: String = "",
        var totalClicks: Int = 0,
        var successCount: Int = 0,
        var failCount: Int = 0,
        var totalGifts: Int = 0,
        var totalValue: Double = 0.0,
        var lastAttemptTime: Long = 0,
        var lastSuccessTime: Long = 0,
        var lastFailTime: Long = 0,
        var gifts: MutableList<GiftInfo> = mutableListOf(),
        var failReasons: MutableMap<String, Int> = mutableMapOf()
    ) {
        fun getSuccessRate(): Double {
            return if (totalClicks > 0) {
                (successCount.toDouble() / totalClicks) * 100
            } else 0.0
        }
    }

    /**
     * 礼物信息
     */
    data class GiftInfo(
        val type: String,
        val count: Int,
        val value: Double,
        val timestamp: Long
    )

    /**
     * 总计统计
     */
    data class TotalStatistics(
        var totalClicks: Int = 0,
        var successCount: Int = 0,
        var failCount: Int = 0,
        var totalGifts: Int = 0,
        var totalValue: Double = 0.0
    ) {
        fun getSuccessRate(): Double {
            val total = successCount + failCount
            return if (total > 0) {
                (successCount.toDouble() / total) * 100
            } else 0.0
        }
    }

    /**
     * 每日统计
     */
    data class DayStatistics(
        var date: String = "",
        var totalClicks: Int = 0,
        var successCount: Int = 0,
        var failCount: Int = 0,
        var totalGifts: Int = 0,
        var totalValue: Double = 0.0
    ) {
        fun getSuccessRate(): Double {
            val total = successCount + failCount
            return if (total > 0) {
                (successCount.toDouble() / total) * 100
            } else 0.0
        }
    }

    /**
     * 抢红包记录
     */
    data class GrabRecord(
        val roomId: String,
        val roomName: String = "",
        val giftType: String = "",
        val giftCount: Int = 0,
        val giftValue: Double = 0.0,
        val timestamp: Long = 0,
        val isSuccess: Boolean = false,
        val failReason: String = ""
    )
}
