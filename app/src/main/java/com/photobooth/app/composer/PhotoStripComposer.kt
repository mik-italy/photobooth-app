package com.photobooth.app.composer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.photobooth.app.R
import com.photobooth.app.data.BgScaleMode
import com.photobooth.app.data.LayoutMode
import com.photobooth.app.data.SettingsData
import java.io.ByteArrayOutputStream
import java.io.File

object PhotoStripComposer {

    // Canon SELPHY CP1500 postcard size (4x6 inches at 300 DPI)
    const val SHEET_WIDTH_PORTRAIT = 1200
    const val SHEET_HEIGHT_PORTRAIT = 1800
    const val STRIP_WIDTH = SHEET_WIDTH_PORTRAIT / 2 // 600 px each

    const val SHEET_WIDTH_LANDSCAPE = 1800
    const val SHEET_HEIGHT_LANDSCAPE = 1200

    fun createPrintSheet(
        context: Context,
        photos: List<Bitmap>,
        settings: SettingsData
    ): Bitmap {
        // Playfair Display Bold Italic
        val playfairBoldItalicTypeface = try {
            ResourcesCompat.getFont(context, R.font.playfair_display_bold_italic)
                ?: Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        } catch (_: Exception) {
            Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        }

        return when (settings.layoutMode) {
            LayoutMode.STRIP_4_PHOTOS -> createDoubleStrip4Photos(photos, settings, playfairBoldItalicTypeface)
            LayoutMode.SINGLE_PHOTO -> createSingleHorizontalPhoto(photos.firstOrNull(), settings, playfairBoldItalicTypeface)
        }
    }

    /**
     * 4-Photo Double Strip:
     * - Maximizes vertical space for photos (4 photos of 370px each = 1480px)
     * - Compact footer for title and date
     * - Title in Playfair Display Bold Italic (NOT uppercase) with contrasting shadow
     * - Configurable scaling for background image
     * - NO watermark / NO "PHOTOBOOTH" text
     */
    private fun createDoubleStrip4Photos(
        photos: List<Bitmap>,
        settings: SettingsData,
        titleTypeface: Typeface
    ): Bitmap {
        val sheetBitmap = Bitmap.createBitmap(SHEET_WIDTH_PORTRAIT, SHEET_HEIGHT_PORTRAIT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)

        drawBackground(canvas, SHEET_WIDTH_PORTRAIT, SHEET_HEIGHT_PORTRAIT, settings)

        // Draw Left Strip
        drawStrip4Photos(canvas, photos, settings, titleTypeface, offsetX = 0f)

        // Duplicate Right Strip
        drawStrip4Photos(canvas, photos, settings, titleTypeface, offsetX = STRIP_WIDTH.toFloat())

        // Subtle center dotted line
        val cutLinePaint = Paint().apply {
            color = Color.rgb(190, 190, 190)
            strokeWidth = 2f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(14f, 14f), 0f)
            isAntiAlias = true
        }
        canvas.drawLine(
            STRIP_WIDTH.toFloat(),
            20f,
            STRIP_WIDTH.toFloat(),
            (SHEET_HEIGHT_PORTRAIT - 20).toFloat(),
            cutLinePaint
        )

        return sheetBitmap
    }

    private fun drawStrip4Photos(
        canvas: Canvas,
        photos: List<Bitmap>,
        settings: SettingsData,
        titleTypeface: Typeface,
        offsetX: Float
    ) {
        val photoMarginX = 38f
        val photoWidth = STRIP_WIDTH - (photoMarginX * 2) // 524 px
        val photoHeight = 368f // Maximized vertical space
        val topMargin = 26f
        val spacing = 12f

        val isDark = isDarkBackground(settings)

        val primaryTextColor = try {
            Color.parseColor(settings.stripTextColor)
        } catch (_: Exception) {
            if (isDark) Color.WHITE else Color.rgb(25, 25, 25)
        }

        val secondaryTextColor = if (isDark) Color.rgb(215, 215, 215) else Color.rgb(110, 110, 110)
        val borderColor = if (isDark) Color.rgb(70, 70, 70) else Color.rgb(225, 225, 225)

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

        // Compact Footer Area
        val footerTop = topMargin + (4 * (photoHeight + spacing)) + 12f
        val footerCenterX = offsetX + (STRIP_WIDTH / 2f)

        // Event Title (Playfair Display Bold Italic - NOT UPPERCASE, with contrast shadow)
        val titlePaint = Paint().apply {
            color = primaryTextColor
            textSize = settings.titleFontSize.toFloat()
            typeface = titleTypeface
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            // High-contrast shadow layer so text is readable over any photo background
            val shadowColor = if (isDarkColor(primaryTextColor)) Color.argb(180, 255, 255, 255) else Color.argb(200, 0, 0, 0)
            setShadowLayer(6f, 1f, 1f, shadowColor)
        }
        // User requested: NOT uppercase!
        canvas.drawText(settings.eventTitle, footerCenterX, footerTop + 54f, titlePaint)

        // Event Subtitle / Date (NOT uppercase, with contrast shadow)
        val subtitlePaint = Paint().apply {
            color = secondaryTextColor
            textSize = (settings.titleFontSize * 0.58f).coerceAtLeast(22f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.04f
            val shadowColor = if (isDarkColor(secondaryTextColor)) Color.argb(140, 255, 255, 255) else Color.argb(180, 0, 0, 0)
            setShadowLayer(4f, 1f, 1f, shadowColor)
        }
        canvas.drawText(settings.eventSubtitle, footerCenterX, footerTop + 112f, subtitlePaint)

        // NOTE: "PHOTOBOOTH" text has been removed as requested!
    }

    /**
     * Single Horizontal Photo (10x15 cm Landscape / 1800 x 1200 px)
     * - Horizontal photo orientation
     * - Cornice intorno (framed border)
     * - Title in Playfair Display Bold Italic (NOT uppercase)
     * - NO watermark
     */
    private fun createSingleHorizontalPhoto(
        photo: Bitmap?,
        settings: SettingsData,
        titleTypeface: Typeface
    ): Bitmap {
        val sheetBitmap = Bitmap.createBitmap(SHEET_WIDTH_LANDSCAPE, SHEET_HEIGHT_LANDSCAPE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)

        drawBackground(canvas, SHEET_WIDTH_LANDSCAPE, SHEET_HEIGHT_LANDSCAPE, settings)

        val isDark = isDarkBackground(settings)

        // Frame dimensions for horizontal 10x15
        val outerMarginX = 70f
        val outerMarginTop = 50f
        val photoWidth = SHEET_WIDTH_LANDSCAPE - (outerMarginX * 2) // 1660 px
        val photoHeight = 920f // Horizontal photo

        val photoRect = RectF(
            outerMarginX,
            outerMarginTop,
            outerMarginX + photoWidth,
            outerMarginTop + photoHeight
        )

        // 1. Draw outer mat border / frame (Cornice intorno)
        val matPaint = Paint().apply {
            color = if (isDark) Color.rgb(35, 35, 35) else Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val matRect = RectF(
            photoRect.left - 12f,
            photoRect.top - 12f,
            photoRect.right + 12f,
            photoRect.bottom + 12f
        )
        canvas.drawRect(matRect, matPaint)

        // 2. Draw outer frame outline
        val outerFramePaint = Paint().apply {
            color = if (isDark) Color.rgb(80, 80, 80) else Color.rgb(200, 200, 200)
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRect(matRect, outerFramePaint)

        // 3. Draw inner photo
        if (photo != null) {
            drawCenterCropBitmap(canvas, photo, photoRect)
        } else {
            val placeholderPaint = Paint().apply {
                color = if (isDark) Color.rgb(50, 50, 50) else Color.rgb(240, 240, 240)
            }
            canvas.drawRect(photoRect, placeholderPaint)
        }

        // 4. Inner photo border
        val innerBorderPaint = Paint().apply {
            color = if (isDark) Color.rgb(70, 70, 70) else Color.rgb(220, 220, 220)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRect(photoRect, innerBorderPaint)

        // Footer Area in bottom of horizontal sheet
        val footerTop = outerMarginTop + photoHeight + 25f
        val centerX = SHEET_WIDTH_LANDSCAPE / 2f

        val primaryTextColor = try {
            Color.parseColor(settings.stripTextColor)
        } catch (_: Exception) {
            if (isDark) Color.WHITE else Color.rgb(25, 25, 25)
        }
        val secondaryTextColor = if (isDark) Color.rgb(215, 215, 215) else Color.rgb(115, 115, 115)

        // Event Title (Playfair Display Bold Italic - NOT UPPERCASE, with contrast shadow)
        val titlePaint = Paint().apply {
            color = primaryTextColor
            textSize = (settings.titleFontSize * 1.3f).coerceAtLeast(50f)
            typeface = titleTypeface
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            val shadowColor = if (isDarkColor(primaryTextColor)) Color.argb(180, 255, 255, 255) else Color.argb(200, 0, 0, 0)
            setShadowLayer(8f, 2f, 2f, shadowColor)
        }
        canvas.drawText(settings.eventTitle, centerX, footerTop + 62f, titlePaint)

        // Event Subtitle / Date (with contrast shadow)
        val subtitlePaint = Paint().apply {
            color = secondaryTextColor
            textSize = (settings.titleFontSize * 0.68f).coerceAtLeast(28f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.04f
            val shadowColor = if (isDarkColor(secondaryTextColor)) Color.argb(140, 255, 255, 255) else Color.argb(180, 0, 0, 0)
            setShadowLayer(5f, 1f, 1f, shadowColor)
        }
        canvas.drawText(settings.eventSubtitle, centerX, footerTop + 120f, subtitlePaint)

        return sheetBitmap
    }

    /**
     * Draws background with chosen scaling mode:
     * - CENTER_CROP: Fills whole sheet without distortion (crops overflow)
     * - FIT_XY: Stretches to exact width and height
     * - FIT_CENTER: Scales to fit while preserving proportions
     */
    private fun drawBackground(canvas: Canvas, width: Int, height: Int, settings: SettingsData) {
        var drewCustomImage = false
        if (!settings.customBgImagePath.isNullOrEmpty()) {
            val bgFile = File(settings.customBgImagePath)
            if (bgFile.exists()) {
                try {
                    val customBg = BitmapFactory.decodeFile(bgFile.absolutePath)
                    if (customBg != null) {
                        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                        when (settings.bgScaleMode) {
                            BgScaleMode.FIT_XY -> {
                                val destRect = Rect(0, 0, width, height)
                                canvas.drawBitmap(customBg, null, destRect, paint)
                            }
                            BgScaleMode.CENTER_CROP -> {
                                drawCenterCropBitmap(canvas, customBg, RectF(0f, 0f, width.toFloat(), height.toFloat()))
                            }
                            BgScaleMode.FIT_CENTER -> {
                                val srcW = customBg.width
                                val srcH = customBg.height
                                val scale = Math.min(width.toFloat() / srcW, height.toFloat() / srcH)
                                val destW = srcW * scale
                                val destH = srcH * scale
                                val left = (width - destW) / 2f
                                val top = (height - destH) / 2f
                                val destRect = RectF(left, top, left + destW, top + destH)
                                // Fill base color first
                                val baseColor = try { Color.parseColor(settings.stripBackgroundColor) } catch (_: Exception) { Color.WHITE }
                                canvas.drawColor(baseColor)
                                canvas.drawBitmap(customBg, null, destRect, paint)
                            }
                        }
                        drewCustomImage = true
                    }
                } catch (_: Exception) {}
            }
        }

        if (!drewCustomImage) {
            val bgColor = try {
                Color.parseColor(settings.stripBackgroundColor)
            } catch (_: Exception) {
                Color.WHITE
            }
            canvas.drawColor(bgColor)
        }
    }

    private fun isDarkBackground(settings: SettingsData): Boolean {
        return try {
            val color = Color.parseColor(settings.stripBackgroundColor)
            isDarkColor(color)
        } catch (_: Exception) {
            false
        }
    }

    private fun isDarkColor(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
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

    fun toJpegByteArray(bitmap: Bitmap, quality: Int = 95): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }
}
