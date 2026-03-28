package com.example.douyinredpacket.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.example.douyinredpacket.MainActivity
import com.example.douyinredpacket.automation.DouYinAutomation
import com.example.douyinredpacket.utils.ErrorHandler

class RedPacketAccessibilityService : AccessibilityService() {

    private lateinit var automation: DouYinAutomation
    private lateinit var errorHandler: ErrorHandler
    private val handler = Handler(Looper.getMainLooper())
    private var lastActionTime = System.currentTimeMillis()

    companion object {
        var isRunning = false
        var isTaskRunning = false

        fun startTask() {
            isTaskRunning = true
        }

        fun stopTask() {
            isTaskRunning = false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true

        automation = DouYinAutomation(this)
        errorHandler = ErrorHandler(this)

        MainActivity.addLog("无障碍服务已连接")

        // 检查抖音是否安装
        if (!errorHandler.isDouYinInstalled()) {
            MainActivity.addLog("错误: 抖音未安装，请先安装抖音APP")
            return
        }

        // 开始监控任务状态
        startTaskMonitor()
    }

    /**
     * 获取统计管理器
     */
    fun getStatisticsManager(): StatisticsManager? {
        return automation.getStatisticsManager()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 更新最后操作时间
        lastActionTime = System.currentTimeMillis()

        event?.let {
            when (it.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    // 窗口变化时检查当前应用
                    val packageName = it.packageName?.toString() ?: ""
                    if (packageName.contains("com.ss.android.ugc.aweme")) {
                        MainActivity.addLog("进入抖音")
                    }
                }
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    // 点击事件
                }
                else -> {}
            }
        }
    }

    override fun onInterrupt() {
        MainActivity.addLog("无障碍服务被中断")
        stopAutomation()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        isTaskRunning = false
        stopAutomation()
        MainActivity.addLog("无障碍服务已停止")
    }

    /**
     * 开始任务监控
     */
    private fun startTaskMonitor() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isTaskRunning) {
                    // 检查网络
                    if (!errorHandler.isNetworkAvailable()) {
                        errorHandler.handleNetworkError {
                            startAutomation()
                        }
                    }

                    // 检查卡顿
                    errorHandler.handleStuck(lastActionTime) {
                        MainActivity.addLog("尝试从卡顿中恢复")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }

                    // 确保自动化在运行
                    startAutomation()
                } else {
                    stopAutomation()
                }

                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    /**
     * 开始自动化
     */
    private fun startAutomation() {
        if (isTaskRunning) {
            automation.startAutomation()
        }
    }

    /**
     * 停止自动化
     */
    private fun stopAutomation() {
        automation.stopAutomation()
        errorHandler.resetErrorCount()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }
}
