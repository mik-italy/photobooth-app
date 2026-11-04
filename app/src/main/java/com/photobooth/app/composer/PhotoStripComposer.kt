package com.photobooth.app.composer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import java.io.ByteArrayOutputStream

object PhotoStripComposer {

    // Canon SELPHY CP1500 postcard size (4x6 inches at 300 DPI)
    const val SHEET_WIDTH = 1200
    const val SHEET_HEIGHT = 1800
    const val STRIP_WIDTH = SHEET_WIDTH / 2 // 600 px each

    /**
     * Composes 3 captured bitmaps into a double 2x6 strip on a 4x6" canvas.
     */
    fun createDoubleStrip(
        photos: List<Bitmap>,
        title: String,
        subtitle: String
    ): Bitmap {
        val sheetBitmap = Bitmap.createBitmap(SHEET_WIDTH, SHEET_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetBitmap)

        // Clean white background
        canvas.drawColor(Color.WHITE)

        // Draw left strip (offset X = 0)
        drawSingleStrip(canvas, photos, title, subtitle, offsetX = 0f)

        // Draw right strip (offset X = STRIP_WIDTH)
        drawSingleStrip(canvas, photos, title, subtitle, offsetX = STRIP_WIDTH.toFloat())

        // Draw center dotted cut guide line
        val cutLinePaint = Paint().apply {
            color = Color.rgb(200, 200, 200)
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

        // Draw small scissors icon hint or "✂" at the top and bottom of cut line
        val scissorsPaint = Paint().apply {
            color = Color.rgb(180, 180, 180)
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
        title: String,
        subtitle: String,
        offsetX: Float
    ) {
        val photoMarginX = 45f
        val photoWidth = STRIP_WIDTH - (photoMarginX * 2) // 510 px
        val photoHeight = 380f // 4:3 aspect ratio approx
        val topMargin = 55f
        val spacing = 28f

        // Photo slot borders
        val borderPaint = Paint().apply {
            color = Color.rgb(230, 230, 230)
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        // Draw the photos
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
                // Placeholder if less than 3 photos
                val placeholderPaint = Paint().apply {
                    color = Color.rgb(245, 245, 245)
                    style = Paint.Style.FILL
                }
                canvas.drawRect(photoRect, placeholderPaint)
            }

            // Crisp border around photo
            canvas.drawRect(photoRect, borderPaint)
        }

        // Footer Area
        val footerTop = topMargin + (3 * (photoHeight + spacing)) + 20f
        val footerCenterX = offsetX + (STRIP_WIDTH / 2f)

        // Event Title
        val titlePaint = Paint().apply {
            color = Color.rgb(25, 25, 25)
            textSize = 42f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.08f
        }
        canvas.drawText(title.uppercase(), footerCenterX, footerTop + 60f, titlePaint)

        // Decorative separator line or stars
        val starPaint = Paint().apply {
            color = Color.rgb(212, 175, 55) // Gold
            textSize = 26f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("✦   ✦   ✦", footerCenterX, footerTop + 115f, starPaint)

        // Event Subtitle / Date
        val subtitlePaint = Paint().apply {
            color = Color.rgb(100, 100, 100)
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            letterSpacing = 0.05f
        }
        canvas.drawText(subtitle, footerCenterX, footerTop + 175f, subtitlePaint)

        // Tiny photobooth watermark
        val watermarkPaint = Paint().apply {
            color = Color.rgb(190, 190, 190)
            textSize = 18f
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
            // Source is wider than dest: crop width
            val newWidth = (srcH * destRatio).toInt()
            val left = (srcW - newWidth) / 2
            Rect(left, 0, left + newWidth, srcH)
        } else {
            // Source is taller than dest: crop height
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
