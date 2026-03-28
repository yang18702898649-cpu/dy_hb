package com.example.douyinredpacket.detector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.douyinredpacket.MainActivity
import kotlinx.coroutines.*
import java.io.File
import java.nio.ByteBuffer

/**
 * 红包检测器 - 基于截图图片相似度匹配
 * 使用方法：提供红包截图模板，通过图片相似度检测屏幕上的红包
 */
class RedPacketDetector(private val context: Context) {

    private val detectorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())

    // 红包模板图片列表
    private var templateBitmaps: MutableList<Bitmap> = mutableListOf()

    companion object {
        private const val TAG = "RedPacketDetector"
        private const val DETECTION_INTERVAL = 500L // 检测间隔 500ms
        private const val SIMILARITY_THRESHOLD = 0.75f // 相似度阈值 75%
        private const val SEARCH_STEP = 15 // 滑动窗口步长（越大越快但可能漏检）
    }

    init {
        loadTemplateImages()
    }

    /**
     * 加载红包模板图片
     * 从以下位置加载：
     * 1. /sdcard/Pictures/红包.png
     * 2. /sdcard/Pictures/红包2.png
     * 3. /sdcard/DCIM/红包.png
     * 4. 项目 assets 目录
     * 5. 项目 res/drawable 目录
     */
    private fun loadTemplateImages() {
        try {
            val possiblePaths = mutableListOf(
                File("/sdcard/Pictures/红包.png"),
                File("/sdcard/Pictures/红包2.png"),
                File("/sdcard/DCIM/红包.png"),
                File("/sdcard/DCIM/红包2.png"),
                File(context.getExternalFilesDir(null), "红包.png"),
                File(context.getExternalFilesDir(null), "红包2.png")
            )

            // 添加项目目录下的截图（如果存在）
            val projectDir = context.getExternalFilesDir(null)?.parentFile?.parentFile?.parentFile
            if (projectDir != null) {
                possiblePaths.add(File(projectDir, "红包.png"))
                possiblePaths.add(File(projectDir, "红包2.png"))
            }

            templateBitmaps.clear()
            for (file in possiblePaths) {
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.let { bitmap ->
                        templateBitmaps.add(bitmap)
                        MainActivity.addLog("✅ 加载红包模板: ${file.absolutePath}")
                    }
                }
            }

            // 尝试从 assets 加载
            loadFromAssets()

            // 尝试从 drawable 加载
            loadFromDrawable()

            if (templateBitmaps.isEmpty()) {
                MainActivity.addLog("⚠️ 警告: 未找到红包模板图片")
                MainActivity.addLog("请将红包截图保存到以下任一位置:")
                MainActivity.addLog("  1. /sdcard/Pictures/红包.png")
                MainActivity.addLog("  2. /sdcard/DCIM/红包.png")
                MainActivity.addLog("  3. 项目目录下: 红包.png")
                MainActivity.addLog("  4. app/src/main/assets/红包.png")
                MainActivity.addLog("  5. app/src/main/res/drawable/红包.png")
            } else {
                MainActivity.addLog("📸 已加载 ${templateBitmaps.size} 个红包模板")
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载模板图片失败", e)
            MainActivity.addLog("❌ 加载模板失败: ${e.message}")
        }
    }

    /**
     * 从 assets 目录加载红包模板
     */
    private fun loadFromAssets() {
        try {
            val assetFiles = listOf("红包.png", "红包2.png")
            for (fileName in assetFiles) {
                context.assets.open(fileName).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)?.let { bitmap ->
                        templateBitmaps.add(bitmap)
                        MainActivity.addLog("✅ 从 assets 加载: $fileName")
                    }
                }
            }
        } catch (e: Exception) {
            // assets 中可能没有文件，忽略错误
        }
    }

    /**
     * 从 drawable 资源加载红包模板
     */
    private fun loadFromDrawable() {
        try {
            val drawableIds = listOf(
                context.resources.getIdentifier("red_packet", "drawable", context.packageName),
                context.resources.getIdentifier("red_packet2", "drawable", context.packageName),
                context.resources.getIdentifier("红包", "drawable", context.packageName),
                context.resources.getIdentifier("红包2", "drawable", context.packageName)
            )

            for (id in drawableIds) {
                if (id != 0) {
                    BitmapFactory.decodeResource(context.resources, id)?.let { bitmap ->
                        templateBitmaps.add(bitmap)
                        MainActivity.addLog("✅ 从 drawable 加载: id=$id")
                    }
                }
            }
        } catch (e: Exception) {
            // drawable 中可能没有文件，忽略错误
        }
    }

    /**
     * 开始检测红包
     * @param screenshot 屏幕截图
     * @return 红包位置 (x, y) 或 null
     */
    fun detect(screenshot: Bitmap): Pair<Int, Int>? {
        if (templateBitmaps.isEmpty()) {
            // 尝试重新加载
            loadTemplateImages()
            if (templateBitmaps.isEmpty()) return null
        }

        // 只在左上角区域检测（抖音红包通常在左上角）
        val searchWidth = screenshot.width / 3
        val searchHeight = screenshot.height / 3

        val searchArea = Bitmap.createBitmap(
            screenshot,
            0, 0,
            searchWidth,
            searchHeight
        )

        // 遍历所有模板进行匹配
        for ((index, template) in templateBitmaps.withIndex()) {
            val result = matchTemplate(searchArea, template)
            if (result != null) {
                MainActivity.addLog("🎯 模板${index + 1}匹配成功: 相似度${String.format("%.1f", result.third * 100)}%")
                searchArea.recycle()
                return Pair(result.first, result.second)
            }
        }

        searchArea.recycle()
        return null
    }

    /**
     * 模板匹配 - 使用滑动窗口 + 多尺度检测
     * @return (x, y, similarity) 或 null
     */
    private fun matchTemplate(source: Bitmap, template: Bitmap): Triple<Int, Int, Float>? {
        val sourceWidth = source.width
        val sourceHeight = source.height
        val templateWidth = template.width
        val templateHeight = template.height

        // 多尺度检测（适应不同分辨率）
        val scales = listOf(0.6f, 0.8f, 1.0f, 1.2f, 1.4f)

        var bestMatch: Triple<Int, Int, Float>? = null
        var bestSimilarity = 0f

        for (scale in scales) {
            val scaledTemplateWidth = (templateWidth * scale).toInt()
            val scaledTemplateHeight = (templateHeight * scale).toInt()

            // 跳过不合适的尺寸
            if (scaledTemplateWidth > sourceWidth || scaledTemplateHeight > sourceHeight) continue
            if (scaledTemplateWidth < 30 || scaledTemplateHeight < 30) continue

            // 创建缩放后的模板
            val scaledTemplate = Bitmap.createScaledBitmap(
                template, scaledTemplateWidth, scaledTemplateHeight, true
            )

            // 滑动窗口搜索
            for (y in 0..(sourceHeight - scaledTemplateHeight) step SEARCH_STEP) {
                for (x in 0..(sourceWidth - scaledTemplateWidth) step SEARCH_STEP) {
                    val similarity = calculateSimilarity(source, scaledTemplate, x, y)

                    if (similarity > bestSimilarity) {
                        bestSimilarity = similarity
                        bestMatch = Triple(
                            x + scaledTemplateWidth / 2,
                            y + scaledTemplateHeight / 2,
                            similarity
                        )
                    }
                }
            }

            scaledTemplate.recycle()
        }

        // 返回最佳匹配（如果超过阈值）
        return if (bestSimilarity >= SIMILARITY_THRESHOLD) {
            bestMatch
        } else {
            null
        }
    }

    /**
     * 计算两个图像区域的相似度
     * 使用简化的像素比较算法
     */
    private fun calculateSimilarity(
        source: Bitmap,
        template: Bitmap,
        offsetX: Int,
        offsetY: Int
    ): Float {
        val templateWidth = template.width
        val templateHeight = template.height

        var totalDiff = 0.0
        var sampleCount = 0

        // 采样步长（减少计算量）
        val stepX = maxOf(1, templateWidth / 8)
        val stepY = maxOf(1, templateHeight / 8)

        for (y in 0 until templateHeight step stepY) {
            for (x in 0 until templateWidth step stepX) {
                val sourceX = offsetX + x
                val sourceY = offsetY + y

                if (sourceX < source.width && sourceY < source.height) {
                    val sourcePixel = source.getPixel(sourceX, sourceY)
                    val templatePixel = template.getPixel(x, y)

                    val diff = pixelDiff(sourcePixel, templatePixel)
                    totalDiff += diff
                    sampleCount++
                }
            }
        }

        return if (sampleCount > 0) {
            val maxDiff = 441.67 // sqrt(255^2 * 3)
            1.0f - (totalDiff / (sampleCount * maxDiff)).toFloat()
        } else {
            0f
        }
    }

    /**
     * 计算两个像素的颜色差异
     */
    private fun pixelDiff(pixel1: Int, pixel2: Int): Double {
        val r1 = (pixel1 shr 16) and 0xFF
        val g1 = (pixel1 shr 8) and 0xFF
        val b1 = pixel1 and 0xFF

        val r2 = (pixel2 shr 16) and 0xFF
        val g2 = (pixel2 shr 8) and 0xFF
        val b2 = pixel2 and 0xFF

        return kotlin.math.sqrt(
            ((r1 - r2) * (r1 - r2) +
             (g1 - g2) * (g1 - g2) +
             (b1 - b2) * (b1 - b2)).toDouble()
        )
    }

    /**
     * 添加新的红包模板
     */
    fun addTemplate(bitmap: Bitmap) {
        templateBitmaps.add(bitmap)
        MainActivity.addLog("📸 添加新模板，当前共 ${templateBitmaps.size} 个")
    }

    /**
     * 清除所有模板
     */
    fun clearTemplates() {
        templateBitmaps.clear()
        MainActivity.addLog("🗑️ 已清除所有模板")
    }

    /**
     * 获取模板数量
     */
    fun getTemplateCount(): Int = templateBitmaps.size

    /**
     * 重新加载模板
     */
    fun reloadTemplates() {
        loadTemplateImages()
    }
}
