package com.photobooth.app.composer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.photobooth.app.data.LayoutMode
import com.photobooth.app.data.SettingsData
import java.io.ByteArrayOutputStream

object PhotoStripComposer {

    // Canon SELPHY CP1500 postcard size (4x6 inches at 300 DPI)
    const val SHEET_WIDTH = 1200
    const val SHEET_HEIGHT = 1800
    const val STRIP_WIDTH = SHEET_WIDTH / 2 // 600 px each

    /**
     * Composes captured bitmaps according to the selected LayoutMode:
     * - STRIP_4_PHOTOS: 4 photos duplicated onto two 2x6 strips.
     * - SINGLE_PHOTO: 1 full photo on 10x15 cm sheet.
     */
    fun createPrintSheet(
        photos: List<Bitmap>,
        settings: SettingsData
    ): Bitmap {
        return when (settings.layoutMode) {
            LayoutMode.STRIP_4_PHOTOS -> createDoubleStripWith4Photos(photos, settings)
            LayoutMode.SINGLE_PHOTO -> createSingleFullPhoto(photos.firstOrNull(), settings)
        }
    }

    /**
     * 4-Photo Double Strip (2x6") on 4x6" canvas.
     */
    private fun createDoubleStripWith4Photos(
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

        canvas.drawColor(bgColor)

        // Draw Strip 1 (Left column)
        drawStrip4Photos(canvas, photos, settings, offsetX = 0f)

        // Duplicate identically to Strip 2 (Right column)
        drawStrip4Photos(canvas, photos, settings, offsetX = STRIP_WIDTH.toFloat())

        // Subtle center dotted line only (NO scissors symbol)
        val cutLineColor = if (isDarkColor(bgColor)) Color.rgb(80, 80, 80) else Color.rgb(215, 215, 215)
        val cutLinePaint = Paint().apply {
            color = cutLineColor
            strokeWidth = 2f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(14f, 14f), 0f)
            isAntiAlias = true
        }
        canvas.drawLine(
            STRIP_WIDTH.toFloat(),
            20f,
            STRIP_WIDTH.toFloat(),
            (SHEET_HEIGHT - 20).toFloat(),
            cutLinePaint
        )

        return sheetBitmap
    }

    private fun drawStrip4Photos(
        canvas: Canvas,
        photos: List<Bitmap>,
        settings: SettingsData,
        offsetX: Float
    ) {
        val photoMarginX = 42f
        val photoWidth = STRIP_WIDTH - (photoMarginX * 2) // 516 px
        val photoHeight = 300f // 4 photos
        val topMargin = 40f
        val spacing = 18f

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

        val secondaryTextColor = if (isDark) Color.rgb(200, 200, 200) else Color.rgb(115, 115, 115)
        val borderColor = if (isDark) Color.rgb(65, 65, 65) else Color.rgb(225, 225, 225)

        val borderPaint = Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        // Draw the 4 photos
        for (i in 0 until 4) {
            val photoTop = topMargin + (i * (photoHeight + spacing))
            val photoRect = RectF(
                offsetX + photoMarginX,
                photoTop,
                offsetX + photoMarginX + photoWidth,
                photoTop + photoHeight
            )

            if (i < photos.size) {
                drawCenterCropBitmap(canvas, photos[i], photoRect)
            } else {
                val placeholderPaint = Paint().apply {
                    color = if (isDark) Color.rgb(40, 40, 40) else Color.rgb(245, 245, 245)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(photoRect, placeholderPaint)
            }

            canvas.drawRect(photoRect, borderPaint)
        }

        // Footer Area (Begins after the 4th photo)
        val footerTop = topMargin + (4 * (photoHeight + spacing)) + 15f
        val footerCenterX = offsetX + (STRIP_WIDTH / 2f)

        // Event Title (Bold, Serif)
        val titlePaint = Paint().apply {
            color = primaryTextColor
            textSize = 38f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.08f
        }
        canvas.drawText(settings.eventTitle.uppercase(), footerCenterX, footerTop + 55f, titlePaint)

        // Event Subtitle / Date (No separator/hashtag as requested)
        val subtitlePaint = Paint().apply {
            color = secondaryTextColor
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.05f
        }
        canvas.drawText(settings.eventSubtitle, footerCenterX, footerTop + 115f, subtitlePaint)

        // Subtle watermark
        val watermarkPaint = Paint().apply {
            color = if (isDark) Color.rgb(90, 90, 90) else Color.rgb(180, 180, 180)
            textSize = 16f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("PHOTOBOOTH", footerCenterX, footerTop + 175f, watermarkPaint)
    }

    /**
     * Single Full Photo (10x15 cm / Postcard) layout with custom footer.
     */
    private fun createSingleFullPhoto(
        photo: Bitmap?,
        settings: SettingsData
    ): Bitmap {
        val sheetBitmap = Bitmap.createBitmap(SHEET_WIDTH, SHEET_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)

        val bgColor = try {
            Color.parseColor(settings.stripBackgroundColor)
        } catch (_: Exception) {
            Color.WHITE
        }
        val isDark = isDarkColor(bgColor)

        canvas.drawColor(bgColor)

        val margin = 50f
        val photoWidth = SHEET_WIDTH - (margin * 2) // 1100 px
        val photoHeight = 1420f

        val photoRect = RectF(margin, margin, margin + photoWidth, margin + photoHeight)

        if (photo != null) {
            drawCenterCropBitmap(canvas, photo, photoRect)
        } else {
            val placeholderPaint = Paint().apply {
                color = if (isDark) Color.rgb(40, 40, 40) else Color.rgb(240, 240, 240)
            }
            canvas.drawRect(photoRect, placeholderPaint)
        }

        // Photo border
        val borderPaint = Paint().apply {
            color = if (isDark) Color.rgb(65, 65, 65) else Color.rgb(220, 220, 220)
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRect(photoRect, borderPaint)

        // Footer Area
        val footerTop = margin + photoHeight + 30f
        val centerX = SHEET_WIDTH / 2f

        val primaryTextColor = try {
            Color.parseColor(settings.stripTextColor)
        } catch (_: Exception) {
            if (isDark) Color.WHITE else Color.rgb(25, 25, 25)
        }
        val secondaryTextColor = if (isDark) Color.rgb(200, 200, 200) else Color.rgb(115, 115, 115)

        val titlePaint = Paint().apply {
            color = primaryTextColor
            textSize = 52f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.08f
        }
        canvas.drawText(settings.eventTitle.uppercase(), centerX, footerTop + 65f, titlePaint)

        val subtitlePaint = Paint().apply {
            color = secondaryTextColor
            textSize = 32f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.05f
        }
        canvas.drawText(settings.eventSubtitle, centerX, footerTop + 130f, subtitlePaint)

        return sheetBitmap
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
