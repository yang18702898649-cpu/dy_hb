package com.example.douyinredpacket.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.douyinredpacket.MainActivity
import com.example.douyinredpacket.R

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var isRunning = false

    companion object {
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val CHANNEL_ID = "red_packet_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startFloatingWindow()
            ACTION_STOP -> stopFloatingWindow()
        }
        return START_STICKY
    }

    private fun startFloatingWindow() {
        if (floatingView != null) return

        isRunning = true
        startForeground(NOTIFICATION_ID, createNotification())

        val inflater = LayoutInflater.from(this)
        floatingView = inflater.inflate(R.layout.floating_window, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        windowManager.addView(floatingView, params)

        setupFloatingView(floatingView!!, params)
        startRedPacketTask()
    }

    private fun setupFloatingView(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }

        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)
        val btnStart = view.findViewById<Button>(R.id.btnFloatStart)
        val btnStop = view.findViewById<Button>(R.id.btnFloatStop)

        btnClose.setOnClickListener {
            stopFloatingWindow()
        }

        btnStart.setOnClickListener {
            if (!RedPacketAccessibilityService.isRunning) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                return@setOnClickListener
            }
            startRedPacketTask()
            btnStart.isEnabled = false
            btnStop.isEnabled = true
        }

        btnStop.setOnClickListener {
            stopRedPacketTask()
            btnStart.isEnabled = true
            btnStop.isEnabled = false
        }
    }

    private fun startRedPacketTask() {
        RedPacketAccessibilityService.startTask()
        updateFloatingStatus("运行中", true)
    }

    private fun stopRedPacketTask() {
        RedPacketAccessibilityService.stopTask()
        updateFloatingStatus("已停止", false)
    }

    private fun updateFloatingStatus(status: String, isRunning: Boolean) {
        floatingView?.let { view ->
            val tvStatus = view.findViewById<TextView>(R.id.tvFloatStatus)
            val tvSuccess = view.findViewById<TextView>(R.id.tvFloatSuccess)
            val tvFail = view.findViewById<TextView>(R.id.tvFloatFail)

            tvStatus.text = "状态: $status"
            tvSuccess.text = "成功: ${MainActivity.successCount}"
            tvFail.text = "失败: ${MainActivity.failCount}"
        }
    }

    private fun stopFloatingWindow() {
        stopRedPacketTask()
        floatingView?.let {
            windowManager.removeView(it)
            floatingView = null
        }
        isRunning = false
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "红包助手服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持红包助手在后台运行"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("抖音红包助手")
            .setContentText("服务正在运行中...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopFloatingWindow()
    }
}
