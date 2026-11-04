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
import com.photobooth.app.data.FontChoice
import com.photobooth.app.data.LayoutMode
import com.photobooth.app.data.SettingsData
import java.io.ByteArrayOutputStream
import java.io.File

object PhotoStripComposer {

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
        val selectedTypeface = loadTypeface(context, settings.fontChoice)

        return when (settings.layoutMode) {
            LayoutMode.STRIP_4_PHOTOS -> createDoubleStrip4Photos(photos, settings, selectedTypeface)
            LayoutMode.SINGLE_PHOTO -> createSingleHorizontalPhoto(photos.firstOrNull(), settings, selectedTypeface)
        }
    }

    private fun loadTypeface(context: Context, choice: FontChoice): Typeface {
        return try {
            val fontRes = when (choice) {
                FontChoice.PLAYFAIR -> R.font.playfair_display_bold_italic
                FontChoice.MONTSERRAT -> R.font.montserrat_bold
                FontChoice.DANCING_SCRIPT -> R.font.dancingscript_bold
                FontChoice.CINZEL -> R.font.cinzel_bold
            }
            ResourcesCompat.getFont(context, fontRes) ?: Typeface.create(Typeface.SERIF, Typeface.BOLD)
        } catch (_: Exception) {
            Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
    }

    /**
     * 4-Photo Double Strip:
     * - Uses stripBgImagePath if present
     * - Maximizes vertical space for photos (370px each = 1480px total)
     * - Draws eventTitle only if showTitleText is true (NO subtitle)
     */
    private fun createDoubleStrip4Photos(
        photos: List<Bitmap>,
        settings: SettingsData,
        titleTypeface: Typeface
    ): Bitmap {
        val sheetBitmap = Bitmap.createBitmap(SHEET_WIDTH_PORTRAIT, SHEET_HEIGHT_PORTRAIT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)

        drawBackground(canvas, SHEET_WIDTH_PORTRAIT, SHEET_HEIGHT_PORTRAIT, settings.stripBgImagePath, settings.stripBackgroundColor)

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
        val photoHeight = 368f
        val topMargin = 26f
        val spacing = 12f

        val isDark = isDarkBackground(settings.stripBackgroundColor)

        val primaryTextColor = try {
            Color.parseColor(settings.stripTextColor)
        } catch (_: Exception) {
            if (isDark) Color.WHITE else Color.rgb(25, 25, 25)
        }

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

        // Footer Area: Event Title only (if enabled)
        if (settings.showTitleText && settings.eventTitle.isNotBlank()) {
            val footerTop = topMargin + (4 * (photoHeight + spacing)) + 12f
            val footerCenterX = offsetX + (STRIP_WIDTH / 2f)

            val titlePaint = Paint().apply {
                color = primaryTextColor
                textSize = settings.titleFontSize.toFloat()
                typeface = titleTypeface
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                val shadowColor = if (isDarkColor(primaryTextColor)) Color.argb(180, 255, 255, 255) else Color.argb(200, 0, 0, 0)
                setShadowLayer(6f, 1f, 1f, shadowColor)
            }
            canvas.drawText(settings.eventTitle, footerCenterX, footerTop + 65f, titlePaint)
        }
    }

    /**
     * Single Horizontal Photo (10x15 cm Landscape / 1800 x 1200 px)
     * - Uses singlePhotoBgImagePath if present
     * - Horizontal photo with elegant framed border
     * - Draws eventTitle only if showTitleText is true (NO subtitle)
     */
    private fun createSingleHorizontalPhoto(
        photo: Bitmap?,
        settings: SettingsData,
        titleTypeface: Typeface
    ): Bitmap {
        val sheetBitmap = Bitmap.createBitmap(SHEET_WIDTH_LANDSCAPE, SHEET_HEIGHT_LANDSCAPE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)

        drawBackground(canvas, SHEET_WIDTH_LANDSCAPE, SHEET_HEIGHT_LANDSCAPE, settings.singlePhotoBgImagePath, settings.stripBackgroundColor)

        val isDark = isDarkBackground(settings.stripBackgroundColor)

        val outerMarginX = 70f
        val outerMarginTop = 50f
        val photoWidth = SHEET_WIDTH_LANDSCAPE - (outerMarginX * 2) // 1660 px
        val photoHeight = 920f

        val photoRect = RectF(
            outerMarginX,
            outerMarginTop,
            outerMarginX + photoWidth,
            outerMarginTop + photoHeight
        )

        // Mat border / frame around photo
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

        // Frame outline
        val outerFramePaint = Paint().apply {
            color = if (isDark) Color.rgb(80, 80, 80) else Color.rgb(200, 200, 200)
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRect(matRect, outerFramePaint)

        if (photo != null) {
            drawCenterCropBitmap(canvas, photo, photoRect)
        } else {
            val placeholderPaint = Paint().apply {
                color = if (isDark) Color.rgb(50, 50, 50) else Color.rgb(240, 240, 240)
            }
            canvas.drawRect(photoRect, placeholderPaint)
        }

        val innerBorderPaint = Paint().apply {
            color = if (isDark) Color.rgb(70, 70, 70) else Color.rgb(220, 220, 220)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRect(photoRect, innerBorderPaint)

        // Footer Area
        if (settings.showTitleText && settings.eventTitle.isNotBlank()) {
            val footerTop = outerMarginTop + photoHeight + 30f
            val centerX = SHEET_WIDTH_LANDSCAPE / 2f

            val primaryTextColor = try {
                Color.parseColor(settings.stripTextColor)
            } catch (_: Exception) {
                if (isDark) Color.WHITE else Color.rgb(25, 25, 25)
            }

            val titlePaint = Paint().apply {
                color = primaryTextColor
                textSize = (settings.titleFontSize * 1.35f).coerceAtLeast(52f)
                typeface = titleTypeface
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                val shadowColor = if (isDarkColor(primaryTextColor)) Color.argb(180, 255, 255, 255) else Color.argb(200, 0, 0, 0)
                setShadowLayer(8f, 2f, 2f, shadowColor)
            }
            canvas.drawText(settings.eventTitle, centerX, footerTop + 70f, titlePaint)
        }

        return sheetBitmap
    }

    private fun drawBackground(canvas: Canvas, width: Int, height: Int, imagePath: String?, defaultColorHex: String) {
        var drewCustomImage = false
        if (!imagePath.isNullOrEmpty()) {
            val bgFile = File(imagePath)
            if (bgFile.exists()) {
                try {
                    val customBg = BitmapFactory.decodeFile(bgFile.absolutePath)
                    if (customBg != null) {
                        drawCenterCropBitmap(canvas, customBg, RectF(0f, 0f, width.toFloat(), height.toFloat()))
                        drewCustomImage = true
                    }
                } catch (_: Exception) {}
            }
        }

        if (!drewCustomImage) {
            val bgColor = try {
                Color.parseColor(defaultColorHex)
            } catch (_: Exception) {
                Color.WHITE
            }
            canvas.drawColor(bgColor)
        }
    }

    private fun isDarkBackground(colorHex: String): Boolean {
        return try {
            val color = Color.parseColor(colorHex)
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
