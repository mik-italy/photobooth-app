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

    // FORMATO CANON SELPHY CP1500 (10x15 cm / 4x6" a 300 DPI nativi - Stampa al vivo senza bordi)
    const val SHEET_WIDTH_PORTRAIT = 1200
    const val SHEET_HEIGHT_PORTRAIT = 1800
    const val STRIP_WIDTH = SHEET_WIDTH_PORTRAIT / 2 // 600 px ciascuna (2x6")

    const val SHEET_WIDTH_LANDSCAPE = 1800
    const val SHEET_HEIGHT_LANDSCAPE = 1200

    fun createPrintSheet(
        context: Context,
        photos: List<Bitmap>,
        settings: SettingsData
    ): Bitmap {
        val selectedTypeface = loadTypeface(context, settings.fontChoice)

        return when (settings.layoutMode) {
            LayoutMode.STRIP_3_PHOTOS -> createDoubleStrip3Photos(photos, settings, selectedTypeface)
            LayoutMode.STRIP_2_PHOTOS -> createDoubleStrip2PhotosHorizontal(photos, settings, selectedTypeface)
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
     * 3-Photo Double Strip (10x15 cm / 1200 x 1800 px a 300 DPI):
     * - Due strisce 2x6" (600 x 1800 px) affiancate
     * - Sfondo al vivo da margine a margine (0% bordi bianchi)
     * - 3 foto verticali per striscia (528 x 440 px ciascuna)
     * - Testo strictly su singola riga auto-scalato (solo se showTitleOnPrints è true)
     */
    private fun createDoubleStrip3Photos(
        photos: List<Bitmap>,
        settings: SettingsData,
        titleTypeface: Typeface
    ): Bitmap {
        val sheetBitmap = Bitmap.createBitmap(SHEET_WIDTH_PORTRAIT, SHEET_HEIGHT_PORTRAIT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)

        // Sfondo a filo margine (0 bordi bianchi)
        drawBackground(canvas, SHEET_WIDTH_PORTRAIT, SHEET_HEIGHT_PORTRAIT, settings.stripBgImagePath, settings.stripBackgroundColor)

        // Striscia Sinistra
        drawStrip3Photos(canvas, photos, settings, titleTypeface, offsetX = 0f)

        // Striscia Destra (duplicata)
        drawStrip3Photos(canvas, photos, settings, titleTypeface, offsetX = STRIP_WIDTH.toFloat())

        // Linea tratteggiata di taglio centrale tra le due strisce 2x6"
        val cutLinePaint = Paint().apply {
            color = Color.rgb(180, 180, 180)
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

    private fun drawStrip3Photos(
        canvas: Canvas,
        photos: List<Bitmap>,
        settings: SettingsData,
        titleTypeface: Typeface,
        offsetX: Float
    ) {
        val photoMarginX = 36f
        val photoWidth = STRIP_WIDTH - (photoMarginX * 2) // 528 px
        val photoHeight = 440f
        val topMargin = 36f
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
            strokeWidth = 2.5f
            isAntiAlias = true
        }

        // Disegna le 3 foto
        for (i in 0 until 3) {
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
            val footerTop = topMargin + (3 * (photoHeight + spacing))
            val footerBottom = SHEET_HEIGHT_PORTRAIT.toFloat()
            val footerCenterY = footerTop + ((footerBottom - footerTop) / 2f) + 8f
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
            var currentSize = (settings.titleFontSize * 1.15f).coerceIn(30f, 75f)
            titlePaint.textSize = currentSize
            while (titlePaint.measureText(settings.eventTitle) > maxAllowedTextWidth && currentSize > 16f) {
                currentSize -= 2f
                titlePaint.textSize = currentSize
            }

            canvas.drawText(settings.eventTitle, footerCenterX, footerCenterY, titlePaint)
        }
    }

    /**
     * 2-Photo Horizontal Strip (10x15 cm / 1800 x 1200 px a 300 DPI):
     * - Due strisce 6x2" (1800 x 600 px) una sopra l'altra
     * - Sfondo al vivo da margine a margine (0% bordi bianchi)
     * - In ciascuna striscia: 2 foto affiancate orizzontalmente (640 x 480 px ciascuna)
     * - Area branding / testo sulla destra (larghezza ~400 px)
     * - Linea tratteggiata di taglio orizzontale centrale a Y = 600 px
     */
    private fun createDoubleStrip2PhotosHorizontal(
        photos: List<Bitmap>,
        settings: SettingsData,
        titleTypeface: Typeface
    ): Bitmap {
        val sheetBitmap = Bitmap.createBitmap(SHEET_WIDTH_LANDSCAPE, SHEET_HEIGHT_LANDSCAPE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)

        val stripHeight = SHEET_HEIGHT_LANDSCAPE / 2f // 600 px ciascuna

        // Sfondo a filo margine (0 bordi bianchi) - usa sfondo orizzontale
        drawBackground(canvas, SHEET_WIDTH_LANDSCAPE, SHEET_HEIGHT_LANDSCAPE, settings.singlePhotoBgImagePath, settings.stripBackgroundColor)

        // Striscia Superiore
        drawHorizontalStrip2Photos(canvas, photos, settings, titleTypeface, offsetY = 0f, stripHeight = stripHeight)

        // Striscia Inferiore (duplicata)
        drawHorizontalStrip2Photos(canvas, photos, settings, titleTypeface, offsetY = stripHeight, stripHeight = stripHeight)

        // Linea tratteggiata di taglio centrale orizzontale tra le due strisce 6x2"
        val cutLinePaint = Paint().apply {
            color = Color.rgb(180, 180, 180)
            strokeWidth = 2f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(14f, 14f), 0f)
            isAntiAlias = true
        }
        canvas.drawLine(
            20f,
            stripHeight,
            (SHEET_WIDTH_LANDSCAPE - 20).toFloat(),
            stripHeight,
            cutLinePaint
        )

        return sheetBitmap
    }

    private fun drawHorizontalStrip2Photos(
        canvas: Canvas,
        photos: List<Bitmap>,
        settings: SettingsData,
        titleTypeface: Typeface,
        offsetY: Float,
        stripHeight: Float
    ) {
        val photoHeight = 480f
        val photoWidth = 640f
        val topMargin = (stripHeight - photoHeight) / 2f // 60 px
        val leftMargin = 50f
        val spacing = 30f

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
            strokeWidth = 2.5f
            isAntiAlias = true
        }

        // Disegna le 2 foto affiancate
        for (i in 0 until 2) {
            val photoLeft = leftMargin + (i * (photoWidth + spacing))
            val photoRect = RectF(
                photoLeft,
                offsetY + topMargin,
                photoLeft + photoWidth,
                offsetY + topMargin + photoHeight
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

        // Area Branding / Titolo a destra: SOLO se showTitleOnPrints è true (strict single-line auto-scale)
        if (settings.showTitleOnPrints && settings.eventTitle.isNotBlank()) {
            val textSectionLeft = leftMargin + (2 * photoWidth) + spacing // 50 + 1280 + 30 = 1360 px
            val textSectionRight = SHEET_WIDTH_LANDSCAPE.toFloat() - 40f  // 1760 px
            val textSectionWidth = textSectionRight - textSectionLeft      // 400 px
            val textCenterX = textSectionLeft + (textSectionWidth / 2f)
            val textCenterY = offsetY + (stripHeight / 2f) + 8f

            val maxAllowedTextWidth = textSectionWidth * 0.92f

            val titlePaint = Paint().apply {
                color = primaryTextColor
                typeface = titleTypeface
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                val shadowColor = if (isDarkColor(primaryTextColor)) Color.argb(160, 255, 255, 255) else Color.argb(180, 0, 0, 0)
                setShadowLayer(6f, 1f, 1f, shadowColor)
            }

            // Auto-scale su riga singola
            var currentSize = settings.titleFontSize.toFloat().coerceIn(28f, 65f)
            titlePaint.textSize = currentSize
            while (titlePaint.measureText(settings.eventTitle) > maxAllowedTextWidth && currentSize > 16f) {
                currentSize -= 2f
                titlePaint.textSize = currentSize
            }

            canvas.drawText(settings.eventTitle, textCenterX, textCenterY, titlePaint)
        }
    }

    /**
     * Foto Singola Orizzontale (10x15 cm / 1800 x 1200 px a 300 DPI):
     * - Sfondo al vivo da margine a margine (0% bordi bianchi)
     * - Nessun bordo bianco/passepartout sulla foto
     * - Testo strictly su riga singola auto-scalato in basso
     */
    private fun createSingleHorizontalPhoto(
        photo: Bitmap?,
        settings: SettingsData,
        titleTypeface: Typeface
    ): Bitmap {
        val sheetBitmap = Bitmap.createBitmap(SHEET_WIDTH_LANDSCAPE, SHEET_HEIGHT_LANDSCAPE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)

        // Sfondo a filo margine (0 bordi bianchi)
        drawBackground(canvas, SHEET_WIDTH_LANDSCAPE, SHEET_HEIGHT_LANDSCAPE, settings.singlePhotoBgImagePath, settings.stripBackgroundColor)

        val isDark = isDarkBackground(settings.stripBackgroundColor)

        val outerMarginX = 60f
        val outerMarginTop = 45f
        val photoWidth = SHEET_WIDTH_LANDSCAPE - (outerMarginX * 2) // 1680 px
        val photoHeight = 980f

        val photoRect = RectF(
            outerMarginX,
            outerMarginTop,
            outerMarginX + photoWidth,
            outerMarginTop + photoHeight
        )

        // Foto pulita senza bordo bianco/passepartout
        if (photo != null) {
            drawCenterCropBitmap(canvas, photo, photoRect)
        } else {
            val placeholderPaint = Paint().apply {
                color = if (isDark) Color.rgb(45, 45, 45) else Color.rgb(240, 240, 240)
            }
            canvas.drawRect(photoRect, placeholderPaint)
        }

        // Footer: Titolo Evento SOLO se showTitleOnPrints è true (strict single-line auto-scale)
        if (settings.showTitleOnPrints && settings.eventTitle.isNotBlank()) {
            val footerTop = outerMarginTop + photoHeight
            val footerBottom = SHEET_HEIGHT_LANDSCAPE.toFloat()
            val centerY = footerTop + ((footerBottom - footerTop) / 2f) + 8f
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
            var currentSize = (settings.titleFontSize * 1.35f).coerceIn(36f, 85f)
            titlePaint.textSize = currentSize
            while (titlePaint.measureText(settings.eventTitle) > maxAllowedTextWidth && currentSize > 20f) {
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
                        // Disegna a filo margine (da 0,0 a width,height)
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
