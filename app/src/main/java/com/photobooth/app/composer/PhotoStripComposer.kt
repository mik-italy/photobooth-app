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

    // FORMATO A5 (148 x 210 mm a 300 DPI)
    const val SHEET_WIDTH_PORTRAIT = 1748
    const val SHEET_HEIGHT_PORTRAIT = 2480
    const val STRIP_WIDTH = SHEET_WIDTH_PORTRAIT / 2 // 874 px each strip

    // FORMATO A5 ORIZZONTALE (210 x 148 mm a 300 DPI)
    const val SHEET_WIDTH_LANDSCAPE = 2480
    const val SHEET_HEIGHT_LANDSCAPE = 1748

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
     * 4-Photo Double Strip su foglio A5 (1748 x 2480 px a 300 DPI):
     * - Due strisce da 874 x 2480 px ciascuna
     * - 4 foto verticali con spazio massimizzato
     * - Testo strictly su singola riga auto-scalato (solo se showTitleOnPrints è true)
     */
    private fun createDoubleStrip4Photos(
        photos: List<Bitmap>,
        settings: SettingsData,
        titleTypeface: Typeface
    ): Bitmap {
        val sheetBitmap = Bitmap.createBitmap(SHEET_WIDTH_PORTRAIT, SHEET_HEIGHT_PORTRAIT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)

        drawBackground(canvas, SHEET_WIDTH_PORTRAIT, SHEET_HEIGHT_PORTRAIT, settings.stripBgImagePath, settings.stripBackgroundColor)

        // Striscia Sinistra
        drawStrip4Photos(canvas, photos, settings, titleTypeface, offsetX = 0f)

        // Striscia Destra (duplicata)
        drawStrip4Photos(canvas, photos, settings, titleTypeface, offsetX = STRIP_WIDTH.toFloat())

        // Linea tratteggiata di taglio centrale tra le due strisce A5
        val cutLinePaint = Paint().apply {
            color = Color.rgb(180, 180, 180)
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(18f, 18f), 0f)
            isAntiAlias = true
        }
        canvas.drawLine(
            STRIP_WIDTH.toFloat(),
            30f,
            STRIP_WIDTH.toFloat(),
            (SHEET_HEIGHT_PORTRAIT - 30).toFloat(),
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
        val photoMarginX = 54f
        val photoWidth = STRIP_WIDTH - (photoMarginX * 2) // 766 px
        val photoHeight = 506f
        val topMargin = 38f
        val spacing = 16f

        val isDark = isDarkBackground(settings.stripBackgroundColor)

        val primaryTextColor = try {
            Color.parseColor(settings.stripTextColor)
        } catch (_: Exception) {
            if (isDark) Color.WHITE else Color.rgb(25, 25, 25)
        }

        val borderColor = if (isDark) Color.rgb(75, 75, 75) else Color.rgb(220, 220, 220)
        val borderPaint = Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        // Disegna le 4 foto
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

        // Footer: Titolo Evento SOLO se showTitleOnPrints è true (strict single-line auto-scale)
        if (settings.showTitleOnPrints && settings.eventTitle.isNotBlank()) {
            val footerTop = topMargin + (4 * (photoHeight + spacing))
            val footerBottom = SHEET_HEIGHT_PORTRAIT.toFloat()
            val footerCenterY = footerTop + ((footerBottom - footerTop) / 2f) + 10f
            val footerCenterX = offsetX + (STRIP_WIDTH / 2f)

            val maxAllowedTextWidth = photoWidth * 0.94f

            val titlePaint = Paint().apply {
                color = primaryTextColor
                typeface = titleTypeface
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                val shadowColor = if (isDarkColor(primaryTextColor)) Color.argb(160, 255, 255, 255) else Color.argb(180, 0, 0, 0)
                setShadowLayer(6f, 1f, 1f, shadowColor)
            }

            // Auto-scale su riga singola
            var currentSize = (settings.titleFontSize * 1.45f).coerceIn(36f, 86f)
            titlePaint.textSize = currentSize
            while (titlePaint.measureText(settings.eventTitle) > maxAllowedTextWidth && currentSize > 20f) {
                currentSize -= 2f
                titlePaint.textSize = currentSize
            }

            canvas.drawText(settings.eventTitle, footerCenterX, footerCenterY, titlePaint)
        }
    }

    /**
     * Foto Singola Orizzontale su foglio A5 (2480 x 1748 px a 300 DPI):
     * - Foto orizzontale incorniciata ad alta risoluzione
     * - Testo strictly su riga singola auto-scalato in basso
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

        val outerMarginX = 100f
        val outerMarginTop = 68f
        val photoWidth = SHEET_WIDTH_LANDSCAPE - (outerMarginX * 2) // 2280 px
        val photoHeight = 1350f

        val photoRect = RectF(
            outerMarginX,
            outerMarginTop,
            outerMarginX + photoWidth,
            outerMarginTop + photoHeight
        )

        // Cornice / Passepartout
        val matPaint = Paint().apply {
            color = if (isDark) Color.rgb(32, 32, 32) else Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val matRect = RectF(
            photoRect.left - 16f,
            photoRect.top - 16f,
            photoRect.right + 16f,
            photoRect.bottom + 16f
        )
        canvas.drawRect(matRect, matPaint)

        // Bordo esterno passepartout
        val outerFramePaint = Paint().apply {
            color = if (isDark) Color.rgb(75, 75, 75) else Color.rgb(205, 205, 205)
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRect(matRect, outerFramePaint)

        if (photo != null) {
            drawCenterCropBitmap(canvas, photo, photoRect)
        } else {
            val placeholderPaint = Paint().apply {
                color = if (isDark) Color.rgb(45, 45, 45) else Color.rgb(240, 240, 240)
            }
            canvas.drawRect(photoRect, placeholderPaint)
        }

        val innerBorderPaint = Paint().apply {
            color = if (isDark) Color.rgb(65, 65, 65) else Color.rgb(220, 220, 220)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRect(photoRect, innerBorderPaint)

        // Footer: Titolo Evento SOLO se showTitleOnPrints è true (strict single-line auto-scale)
        if (settings.showTitleOnPrints && settings.eventTitle.isNotBlank()) {
            val footerTop = outerMarginTop + photoHeight
            val footerBottom = SHEET_HEIGHT_LANDSCAPE.toFloat()
            val centerY = footerTop + ((footerBottom - footerTop) / 2f) + 14f
            val centerX = SHEET_WIDTH_LANDSCAPE / 2f

            val primaryTextColor = try {
                Color.parseColor(settings.stripTextColor)
            } catch (_: Exception) {
                if (isDark) Color.WHITE else Color.rgb(25, 25, 25)
            }

            val maxAllowedTextWidth = photoWidth * 0.94f

            val titlePaint = Paint().apply {
                color = primaryTextColor
                typeface = titleTypeface
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                val shadowColor = if (isDarkColor(primaryTextColor)) Color.argb(160, 255, 255, 255) else Color.argb(180, 0, 0, 0)
                setShadowLayer(8f, 2f, 2f, shadowColor)
            }

            // Auto-scale su riga singola
            var currentSize = (settings.titleFontSize * 1.9f).coerceIn(50f, 120f)
            titlePaint.textSize = currentSize
            while (titlePaint.measureText(settings.eventTitle) > maxAllowedTextWidth && currentSize > 26f) {
                currentSize -= 2f
                titlePaint.textSize = currentSize
            }

            canvas.drawText(settings.eventTitle, centerX, centerY, titlePaint)
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
