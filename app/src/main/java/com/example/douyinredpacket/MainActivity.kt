package com.example.douyinredpacket

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.douyinredpacket.service.FloatingWindowService
import com.example.douyinredpacket.service.RedPacketAccessibilityService

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvSuccessCount: TextView
    private lateinit var tvFailCount: TextView
    private lateinit var tvLog: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnAccessibility: Button

    companion object {
        var successCount = 0
        var failCount = 0
        var currentStatus = "等待中"
        val logList = mutableListOf<String>()
        const val MAX_LOG_LINES = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        updateUI()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvSuccessCount = findViewById(R.id.tvSuccessCount)
        tvFailCount = findViewById(R.id.tvFailCount)
        tvLog = findViewById(R.id.tvLog)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnAccessibility = findViewById(R.id.btnAccessibility)

        btnStart.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
                return@setOnClickListener
            }
            if (!RedPacketAccessibilityService.isRunning) {
                Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_SHORT).show()
                openAccessibilitySettings()
                return@setOnClickListener
            }
            startService()
        }

        btnStop.setOnClickListener {
            stopService()
        }

        btnAccessibility.setOnClickListener {
            openAccessibilitySettings()
        }

        val btnStatistics = findViewById<Button>(R.id.btnStatistics)
        btnStatistics.setOnClickListener {
            startActivity(Intent(this, StatisticsActivity::class.java))
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun startService() {
        val intent = Intent(this, FloatingWindowService::class.java)
        intent.action = FloatingWindowService.ACTION_START
        startService(intent)

        btnStart.isEnabled = false
        btnStop.isEnabled = true
        addLog("开始抢红包服务")
    }

    private fun stopService() {
        val intent = Intent(this, FloatingWindowService::class.java)
        intent.action = FloatingWindowService.ACTION_STOP
        startService(intent)

        btnStart.isEnabled = true
        btnStop.isEnabled = false
        addLog("停止抢红包服务")
    }

    fun updateUI() {
        runOnUiThread {
            tvStatus.text = currentStatus
            tvSuccessCount.text = successCount.toString()
            tvFailCount.text = failCount.toString()
            tvLog.text = logList.joinToString("\n")
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    companion object {
        fun addLog(message: String) {
            val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date())
            logList.add("[$time] $message")
            if (logList.size > MAX_LOG_LINES) {
                logList.removeAt(0)
            }
        }

        fun updateStatus(status: String) {
            currentStatus = status
        }

        fun incrementSuccess() {
            successCount++
        }

        fun incrementFail() {
            failCount++
        }

        fun resetStats() {
            successCount = 0
            failCount = 0
            logList.clear()
            currentStatus = "等待中"
        }
    }
}
