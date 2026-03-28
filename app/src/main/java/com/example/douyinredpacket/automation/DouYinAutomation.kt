package com.example.douyinredpacket.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo
import com.example.douyinredpacket.MainActivity
import com.example.douyinredpacket.data.StatisticsManager
import com.example.douyinredpacket.detector.RedPacketDetector
import kotlinx.coroutines.*

class DouYinAutomation(private val service: AccessibilityService) {

    private val automationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var currentJob: Job? = null
    private lateinit var statsManager: StatisticsManager
    private lateinit var redPacketDetector: RedPacketDetector

    // 当前房间信息
    private var currentRoomId: String = ""
    private var currentRoomName: String = ""
    private var roomSwitchCount: Int = 0

    // MediaProjection 用于截图（需要外部传入）
    private var mediaProjection: MediaProjection? = null

    // 配置参数
    private val config = AutomationConfig()

    companion object {
        private const val DOUYIN_PACKAGE = "com.ss.android.ugc.aweme"
        private const val LIVE_TAB_TEXT = "直播"
        private const val LIVE_TAB_TEXT2 = "LIVE"
    }

    data class AutomationConfig(
        var swipeDistance: Int = 800,        // 滑动距离
        var swipeDuration: Long = 300,       // 滑动持续时间
        var swipeInterval: Long = 200,       // 滑动间隔
        var redPacketDelay: Long = 2000,     // 检测延迟
        var grabWaitTime: Long = 5000,       // 抢红包后等待时间
        var maxNoLiveCount: Int = 15,        // 最大无红包次数
        var detectionAreaLeft: Float = 0.0f,  // 检测区域左边界
        var detectionAreaTop: Float = 0.0f,   // 检测区域上边界
        var detectionAreaRight: Float = 0.33f,// 检测区域右边界
        var detectionAreaBottom: Float = 0.25f // 检测区域下边界
    )

    /**
     * 开始自动化任务
     */
    fun startAutomation() {
        if (currentJob?.isActive == true) return

        // 初始化统计管理器和红包检测器
        if (!::statsManager.isInitialized) {
            statsManager = StatisticsManager(service.applicationContext)
        }
        if (!::redPacketDetector.isInitialized) {
            redPacketDetector = RedPacketDetector(service.applicationContext)
        }

        currentJob = automationScope.launch {
            MainActivity.addLog("开始抖音自动化任务")
            var noLiveCount = 0

            while (isActive) {
                try {
                    // 1. 检查并进入抖音
                    if (!ensureInDouYin()) {
                        delay(2000)
                        continue
                    }

                    // 2. 进入直播页面
                    if (!enterLivePage()) {
                        MainActivity.addLog("未找到直播入口，尝试滑动查找")
                        swipeToFindLive()
                        delay(1000)
                        continue
                    }

                    // 更新房间信息
                    updateRoomInfo()

                    // 3. 检测红包（截图 + 图片匹配）
                    MainActivity.updateStatus("检测中")
                    val screenshot = captureScreen()
                    val redPacketLocation = if (screenshot != null) {
                        val location = redPacketDetector.detect(screenshot)
                        screenshot.recycle()
                        location
                    } else {
                        // 如果截图失败，使用无障碍节点查找作为备选
                        detectRedPacketByNode()
                    }

                    if (redPacketLocation != null) {
                        noLiveCount = 0
                        MainActivity.updateStatus("抢红包中")
                        MainActivity.addLog("发现红包，位置: (${redPacketLocation.first}, ${redPacketLocation.second})")

                        // 记录尝试
                        statsManager.recordAttempt(currentRoomId, currentRoomName)

                        // 点击红包
                        val success = clickRedPacket(redPacketLocation.first, redPacketLocation.second)

                        if (success) {
                            // 识别礼物类型（这里简化处理，实际可以通过OCR或图像识别）
                            val giftType = identifyGiftType()
                            val giftCount = 1
                            val giftValue = estimateGiftValue(giftType)

                            statsManager.recordSuccess(
                                currentRoomId,
                                currentRoomName,
                                giftType,
                                giftCount,
                                giftValue
                            )
                            MainActivity.incrementSuccess()
                            MainActivity.addLog("抢到红包！获得: $giftType")

                            // 等待红包结束
                            delay(config.grabWaitTime)
                        } else {
                            statsManager.recordFail(currentRoomId, currentRoomName, "点击失败")
                            MainActivity.incrementFail()
                            MainActivity.addLog("抢红包失败")
                        }
                    } else {
                        noLiveCount++
                        MainActivity.addLog("未检测到红包 ($noLiveCount/${config.maxNoLiveCount})")

                        if (noLiveCount >= config.maxNoLiveCount) {
                            MainActivity.addLog("连续多次无红包，返回重试")
                            goBack()
                            noLiveCount = 0
                            delay(1000)
                            continue
                        }
                    }

                    // 4. 滑动到下一个直播间
                    MainActivity.updateStatus("滑动中")
                    swipeToNextLive()
                    roomSwitchCount++
                    delay(config.swipeInterval)

                } catch (e: Exception) {
                    MainActivity.addLog("自动化异常: ${e.message}")
                    delay(2000)
                }
            }
        }
    }

    /**
     * 停止自动化任务
     */
    fun stopAutomation() {
        currentJob?.cancel()
        MainActivity.addLog("停止自动化任务")
    }

    /**
     * 确保在抖音APP内
     */
    private suspend fun ensureInDouYin(): Boolean {
        val rootNode = service.rootInActiveWindow ?: return false
        val packageName = rootNode.packageName?.toString() ?: ""

        return if (packageName.contains(DOUYIN_PACKAGE)) {
            true
        } else {
            MainActivity.addLog("当前不在抖音，尝试打开")
            openDouYinApp()
            delay(3000)
            false
        }
    }

    /**
     * 打开抖音APP
     */
    private fun openDouYinApp() {
        val context = service.applicationContext
        val intent = context.packageManager.getLaunchIntentForPackage(DOUYIN_PACKAGE)

        if (intent != null) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            MainActivity.addLog("已打开抖音APP")
        } else {
            MainActivity.addLog("错误: 抖音未安装")
        }
    }

    /**
     * 进入直播页面
     */
    private fun enterLivePage(): Boolean {
        val rootNode = service.rootInActiveWindow ?: return false

        // 方法1: 查找直播标签
        val liveNodes = rootNode.findAccessibilityNodeInfosByText(LIVE_TAB_TEXT)
        val liveNodes2 = rootNode.findAccessibilityNodeInfosByText(LIVE_TAB_TEXT2)

        val allLiveNodes = liveNodes + liveNodes2

        for (node in allLiveNodes) {
            if (node.isClickable && node.isEnabled) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                MainActivity.addLog("点击直播标签")
                return true
            }
        }

        // 方法2: 通过ID查找（不同版本可能不同）
        val possibleIds = listOf(
            "com.ss.android.ugc.aweme:id/glp",  // 可能的直播标签ID
            "com.ss.android.ugc.aweme:id/cz7",
            "com.ss.android.ugc.aweme:id/a6a"
        )

        for (id in possibleIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            for (node in nodes) {
                if (node.isClickable && node.isEnabled) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    MainActivity.addLog("通过ID进入直播")
                    return true
                }
            }
        }

        return false
    }

    /**
     * 滑动查找直播入口
     */
    private fun swipeToFindLive() {
        val metrics = service.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        val startX = screenWidth / 2
        val startY = (screenHeight * 0.8).toInt()
        val endX = screenWidth / 2
        val endY = (screenHeight * 0.2).toInt()

        performSwipe(startX, startY, endX, endY, 500)
    }

    /**
     * 截图（需要 MediaProjection 支持）
     * 暂时返回 null，实际使用时需要传入 MediaProjection
     */
    private fun captureScreen(): Bitmap? {
        // TODO: 实现截图功能
        // 需要：
        // 1. 在 MainActivity 中启动 MediaProjection
        // 2. 将 MediaProjection 传入 DouYinAutomation
        // 3. 使用 ImageReader 截图
        return null
    }

    /**
     * 设置 MediaProjection
     */
    fun setMediaProjection(projection: MediaProjection?) {
        mediaProjection = projection
    }

    /**
     * 通过无障碍节点检测红包（备选方案）
     */
    private fun detectRedPacketByNode(): Pair<Int, Int>? {
        val rootNode = service.rootInActiveWindow ?: return null
        val metrics = service.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        // 计算检测区域（左上角）
        val left = (screenWidth * config.detectionAreaLeft).toInt()
        val top = (screenHeight * config.detectionAreaTop).toInt()
        val right = (screenWidth * config.detectionAreaRight).toInt()
        val bottom = (screenHeight * config.detectionAreaBottom).toInt()
        val detectionArea = Rect(left, top, right, bottom)

        // 方法1: 通过文本查找
        val redPacketKeywords = listOf("红包", "抢红包", "福袋", "点击领取", "开", "抢")
        for (keyword in redPacketKeywords) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(keyword)
            for (node in nodes) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                if (detectionArea.contains(rect)) {
                    return Pair(rect.centerX(), rect.centerY())
                }
            }
        }

        // 方法2: 通过描述查找
        val descKeywords = listOf("红包", "red packet", "gift", "福袋")
        for (keyword in descKeywords) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(keyword)
            for (node in nodes) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                if (detectionArea.contains(rect)) {
                    return Pair(rect.centerX(), rect.centerY())
                }
            }
        }

        // 方法3: 在检测区域内查找可点击的图像元素
        val targetNode = findClickableNodeInArea(rootNode, detectionArea)
        if (targetNode != null) {
            val rect = Rect()
            targetNode.getBoundsInScreen(rect)
            return Pair(rect.centerX(), rect.centerY())
        }

        return null
    }

    /**
     * 在区域内查找可点击的节点
     */
    private fun findClickableNodeInArea(node: AccessibilityNodeInfo, area: Rect): AccessibilityNodeInfo? {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        // 检查节点是否在检测区域内且可点击
        if (area.contains(rect) && isPossibleRedPacketNode(node)) {
            return node
        }

        // 递归检查子节点
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findClickableNodeInArea(child, area)
            if (result != null) return result
        }

        return null
    }

    /**
     * 判断是否是可能的红包节点
     */
    private fun isPossibleRedPacketNode(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString() ?: ""

        // 红包通常是图片或按钮
        return (className.contains("Image") ||
                className.contains("Button") ||
                className.contains("FrameLayout")) &&
                node.isEnabled
    }

    /**
     * 点击红包
     */
    private fun clickRedPacket(x: Int, y: Int): Boolean {
        return performClick(x, y)
    }

    /**
     * 滑动到下一个直播间
     */
    private fun swipeToNextLive() {
        val metrics = service.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        val startX = screenWidth / 2
        val startY = (screenHeight * 0.7).toInt()
        val endX = screenWidth / 2
        val endY = (screenHeight * 0.3).toInt()

        performSwipe(startX, startY, endX, endY, config.swipeDuration)
    }

    /**
     * 返回上一级
     */
    private fun goBack() {
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    /**
     * 执行点击
     */
    private fun performClick(x: Int, y: Int): Boolean {
        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        return service.dispatchGesture(gesture, null, null)
    }

    /**
     * 执行滑动
     */
    private fun performSwipe(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long) {
        val path = Path().apply {
            moveTo(startX.toFloat(), startY.toFloat())
            lineTo(endX.toFloat(), endY.toFloat())
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                // 滑动完成
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                MainActivity.addLog("滑动被取消")
            }
        }, null)
    }

    /**
     * 更新配置
     */
    fun updateConfig(newConfig: AutomationConfig) {
        config.swipeDistance = newConfig.swipeDistance
        config.swipeDuration = newConfig.swipeDuration
        config.swipeInterval = newConfig.swipeInterval
        config.redPacketDelay = newConfig.redPacketDelay
        config.grabWaitTime = newConfig.grabWaitTime
        config.maxNoLiveCount = newConfig.maxNoLiveCount
    }

    /**
     * 更新房间信息
     */
    private fun updateRoomInfo() {
        val rootNode = service.rootInActiveWindow ?: return

        // 尝试获取房间ID或主播名称
        // 抖音直播间的房间信息可能在不同的位置，这里尝试多种方式

        // 方法1: 查找主播名称
        val possibleNameIds = listOf(
            "com.ss.android.ugc.aweme:id/title",
            "com.ss.android.ugc.aweme:id/nickname",
            "com.ss.android.ugc.aweme:id/user_name"
        )

        for (id in possibleNameIds) {
            val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty()) {
                val name = nodes[0].text?.toString() ?: ""
                if (name.isNotEmpty()) {
                    currentRoomName = name
                    currentRoomId = "room_${name.hashCode()}"
                    return
                }
            }
        }

        // 方法2: 使用时间戳生成房间ID
        if (currentRoomId.isEmpty()) {
            currentRoomId = "room_${System.currentTimeMillis()}"
            currentRoomName = "直播间${roomSwitchCount + 1}"
        }
    }

    /**
     * 识别礼物类型（简化版）
     * 实际可以通过OCR识别红包弹窗中的文字
     */
    private fun identifyGiftType(): String {
        // 这里可以扩展为实际的图像识别或OCR
        // 暂时返回常见的礼物类型
        val commonGifts = listOf("抖币", "小心心", "玫瑰花", "啤酒", "墨镜", "气球")
        return commonGifts.random()
    }

    /**
     * 估算礼物价值
     */
    private fun estimateGiftValue(giftType: String): Double {
        return when (giftType) {
            "抖币" -> 1.0
            "小心心" -> 0.1
            "玫瑰花" -> 1.0
            "啤酒" -> 2.0
            "墨镜" -> 9.9
            "气球" -> 5.0
            else -> 1.0
        }
    }

    /**
     * 获取统计管理器
     */
    fun getStatisticsManager(): StatisticsManager? {
        return if (::statsManager.isInitialized) statsManager else null
    }
}
