package com.photobooth.app.composer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.photobooth.app.data.SettingsData
import java.io.ByteArrayOutputStream

object PhotoStripComposer {

    // Canon SELPHY CP1500 postcard size (4x6 inches at 300 DPI)
    const val SHEET_WIDTH = 1200
    const val SHEET_HEIGHT = 1800
    const val STRIP_WIDTH = SHEET_WIDTH / 2 // 600 px each

    /**
     * Composes 3 captured bitmaps into a double 2x6 strip on a 4x6" canvas.
     * The strip layout is customized according to [settings] and duplicated identically
     * across both strips (left and right).
     */
    fun createDoubleStrip(
        photos: List<Bitmap>,
        settings: SettingsData
    ): Bitmap {
        val sheetBitmap = Bitmap.createBitmap(SHEET_WIDTH, SHEET_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)

        val bgColor = try {
            Color.parseColor(settings.stripBackgroundColor)
        } catch (_: Exception) {
            Color.WHITE
        }

        // Fill background
        canvas.drawColor(bgColor)

        // Draw Strip 1 (Left column: offset X = 0)
        drawSingleStrip(canvas, photos, settings, offsetX = 0f)

        // Duplicate identical graphics to Strip 2 (Right column: offset X = STRIP_WIDTH)
        drawSingleStrip(canvas, photos, settings, offsetX = STRIP_WIDTH.toFloat())

        // Draw central dotted cut guide line
        val cutLineColor = if (isDarkColor(bgColor)) Color.rgb(80, 80, 80) else Color.rgb(210, 210, 210)
        val cutLinePaint = Paint().apply {
            color = cutLineColor
            strokeWidth = 2f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(15f, 15f), 0f)
        }
        canvas.drawLine(
            STRIP_WIDTH.toFloat(),
            30f,
            STRIP_WIDTH.toFloat(),
            (SHEET_HEIGHT - 30).toFloat(),
            cutLinePaint
        )

        // Guide scissors icon
        val scissorsPaint = Paint().apply {
            color = cutLineColor
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("✂", STRIP_WIDTH.toFloat(), 25f, scissorsPaint)
        canvas.drawText("✂", STRIP_WIDTH.toFloat(), SHEET_HEIGHT - 10f, scissorsPaint)

        return sheetBitmap
    }

    private fun drawSingleStrip(
        canvas: Canvas,
        photos: List<Bitmap>,
        settings: SettingsData,
        offsetX: Float
    ) {
        val photoMarginX = 45f
        val photoWidth = STRIP_WIDTH - (photoMarginX * 2) // 510 px
        val photoHeight = 380f
        val topMargin = 55f
        val spacing = 28f

        val bgColor = try {
            Color.parseColor(settings.stripBackgroundColor)
        } catch (_: Exception) {
            Color.WHITE
        }
        val isDark = isDarkColor(bgColor)

        val primaryTextColor = try {
            Color.parseColor(settings.stripTextColor)
        } catch (_: Exception) {
            if (isDark) Color.WHITE else Color.rgb(25, 25, 25)
        }

        val secondaryTextColor = if (isDark) Color.rgb(200, 200, 200) else Color.rgb(110, 110, 110)
        val borderColor = if (isDark) Color.rgb(60, 60, 60) else Color.rgb(220, 220, 220)

        // Photo slot borders
        val borderPaint = Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        // Draw the 3 photos
        for (i in 0 until 3) {
            val photoTop = topMargin + (i * (photoHeight + spacing))
            val photoRect = RectF(
                offsetX + photoMarginX,
                photoTop,
                offsetX + photoMarginX + photoWidth,
                photoTop + photoHeight
            )

            if (i < photos.size) {
                val srcBitmap = photos[i]
                drawCenterCropBitmap(canvas, srcBitmap, photoRect)
            } else {
                val placeholderPaint = Paint().apply {
                    color = if (isDark) Color.rgb(40, 40, 40) else Color.rgb(245, 245, 245)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(photoRect, placeholderPaint)
            }

            canvas.drawRect(photoRect, borderPaint)
        }

        // Footer Area
        val footerTop = topMargin + (3 * (photoHeight + spacing)) + 20f
        val footerCenterX = offsetX + (STRIP_WIDTH / 2f)

        // Event Title
        val titlePaint = Paint().apply {
            color = primaryTextColor
            textSize = 42f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.08f
        }
        canvas.drawText(settings.eventTitle.uppercase(), footerCenterX, footerTop + 60f, titlePaint)

        // Decorative separator line or hashtag
        val starPaint = Paint().apply {
            color = Color.rgb(212, 175, 55) // Gold
            textSize = 26f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(settings.eventHashtag, footerCenterX, footerTop + 115f, starPaint)

        // Event Subtitle / Date
        val subtitlePaint = Paint().apply {
            color = secondaryTextColor
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.05f
        }
        canvas.drawText(settings.eventSubtitle, footerCenterX, footerTop + 175f, subtitlePaint)

        // Subtle photobooth label
        val watermarkPaint = Paint().apply {
            color = if (isDark) Color.rgb(100, 100, 100) else Color.rgb(180, 180, 180)
            textSize = 17f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("PHOTOBOOTH", footerCenterX, footerTop + 240f, watermarkPaint)
    }

    private fun drawCenterCropBitmap(canvas: Canvas, source: Bitmap, destRect: RectF) {
        val srcW = source.width
        val srcH = source.height
        val destW = destRect.width()
        val destH = destRect.height()

        val srcRatio = srcW.toFloat() / srcH.toFloat()
        val destRatio = destW / destH

        val srcCrop = if (srcRatio > destRatio) {
            val newWidth = (srcH * destRatio).toInt()
            val left = (srcW - newWidth) / 2
            Rect(left, 0, left + newWidth, srcH)
        } else {
            val newHeight = (srcW / destRatio).toInt()
            val top = (srcH - newHeight) / 2
            Rect(0, top, srcW, top + newHeight)
        }

        canvas.drawBitmap(source, srcCrop, destRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    }

    private fun isDarkColor(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }

    fun toJpegByteArray(bitmap: Bitmap, quality: Int = 95): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }
}
