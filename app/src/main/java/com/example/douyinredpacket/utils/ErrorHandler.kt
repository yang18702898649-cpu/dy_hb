package com.example.douyinredpacket.utils

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import com.example.douyinredpacket.MainActivity

class ErrorHandler(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private var networkRetryCount = 0
    private var stuckRetryCount = 0
    private val maxRetryCount = 3

    companion object {
        private const val DOUYIN_PACKAGE = "com.ss.android.ugc.aweme"
        private const val STUCK_TIMEOUT = 10000L // 10秒无响应认为卡顿
    }

    /**
     * 检查抖音是否安装
     */
    fun isDouYinInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(DOUYIN_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 检查网络连接
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * 处理抖音未安装的情况
     */
    fun handleDouYinNotInstalled(): Boolean {
        return if (!isDouYinInstalled()) {
            MainActivity.addLog("错误: 抖音APP未安装")
            false
        } else {
            true
        }
    }

    /**
     * 处理网络异常
     */
    fun handleNetworkError(onRetry: () -> Unit): Boolean {
        return if (!isNetworkAvailable()) {
            networkRetryCount++
            MainActivity.addLog("网络异常，尝试重试 ($networkRetryCount/$maxRetryCount)")

            if (networkRetryCount <= maxRetryCount) {
                handler.postDelayed({
                    onRetry()
                }, 3000)
                true
            } else {
                MainActivity.addLog("网络重试次数已达上限，请检查网络连接")
                networkRetryCount = 0
                false
            }
        } else {
            networkRetryCount = 0
            true
        }
    }

    /**
     * 处理卡顿情况
     */
    fun handleStuck(lastActionTime: Long, onRecovery: () -> Unit): Boolean {
        val currentTime = System.currentTimeMillis()
        return if (currentTime - lastActionTime > STUCK_TIMEOUT) {
            stuckRetryCount++
            MainActivity.addLog("检测到卡顿，尝试恢复 ($stuckRetryCount/$maxRetryCount)")

            if (stuckRetryCount <= maxRetryCount) {
                onRecovery()
                true
            } else {
                MainActivity.addLog("卡顿恢复失败，请手动处理")
                stuckRetryCount = 0
                false
            }
        } else {
            stuckRetryCount = 0
            true
        }
    }

    /**
     * 重置错误计数
     */
    fun resetErrorCount() {
        networkRetryCount = 0
        stuckRetryCount = 0
    }

    /**
     * 检查应用是否在前台运行
     */
    fun isAppInForeground(packageName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningProcesses = activityManager.runningAppProcesses

        return runningProcesses?.any { processInfo ->
            processInfo.processName == packageName &&
            processInfo.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        } ?: false
    }

    /**
     * 获取应用版本号
     */
    fun getAppVersion(packageName: String): String? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 记录错误日志
     */
    fun logError(errorType: ErrorType, message: String) {
        val errorMessage = when (errorType) {
            ErrorType.NETWORK -> "网络错误: $message"
            ErrorType.APP_NOT_FOUND -> "应用错误: $message"
            ErrorType.PERMISSION_DENIED -> "权限错误: $message"
            ErrorType.STUCK -> "卡顿错误: $message"
            ErrorType.UNKNOWN -> "未知错误: $message"
        }
        MainActivity.addLog(errorMessage)
    }

    enum class ErrorType {
        NETWORK,
        APP_NOT_FOUND,
        PERMISSION_DENIED,
        STUCK,
        UNKNOWN
    }
}
