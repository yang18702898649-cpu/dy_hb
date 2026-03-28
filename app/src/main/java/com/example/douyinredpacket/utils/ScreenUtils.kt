package com.example.douyinredpacket.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.view.WindowManager

object ScreenUtils {

    /**
     * 获取屏幕宽度
     */
    fun getScreenWidth(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size.x
    }

    /**
     * 获取屏幕高度
     */
    fun getScreenHeight(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size.y
    }

    /**
     * 根据比例获取X坐标
     */
    fun getXByRatio(context: Context, ratio: Float): Int {
        return (getScreenWidth(context) * ratio).toInt()
    }

    /**
     * 根据比例获取Y坐标
     */
    fun getYByRatio(context: Context, ratio: Float): Int {
        return (getScreenHeight(context) * ratio).toInt()
    }

    /**
     * 缩放Bitmap
     */
    fun scaleBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        val width = (bitmap.width * scale).toInt()
        val height = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    /**
     * 裁剪Bitmap
     */
    fun cropBitmap(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
        val safeX = x.coerceIn(0, bitmap.width - 1)
        val safeY = y.coerceIn(0, bitmap.height - 1)
        val safeWidth = width.coerceAtMost(bitmap.width - safeX)
        val safeHeight = height.coerceAtMost(bitmap.height - safeY)

        return Bitmap.createBitmap(bitmap, safeX, safeY, safeWidth, safeHeight)
    }

    /**
     * 计算两点之间的距离
     */
    fun calculateDistance(x1: Int, y1: Int, x2: Int, y2: Int): Double {
        return kotlin.math.sqrt(
            ((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1)).toDouble()
        )
    }

    /**
     * 判断坐标是否在屏幕范围内
     */
    fun isInScreenBounds(context: Context, x: Int, y: Int): Boolean {
        return x in 0..getScreenWidth(context) && y in 0..getScreenHeight(context)
    }

    /**
     * 获取状态栏高度
     */
    fun getStatusBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    /**
     * 获取导航栏高度
     */
    fun getNavigationBarHeight(context: Context): Int {
        val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }
}
