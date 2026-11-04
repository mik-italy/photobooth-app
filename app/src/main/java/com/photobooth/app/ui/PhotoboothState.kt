package com.photobooth.app.ui

import android.graphics.Bitmap

sealed class PhotoboothScreenState {
    object Idle : PhotoboothScreenState()

    data class Countdown(
        val shotIndex: Int, // 1, 2, or 3
        val totalShots: Int = 3,
        val remainingSeconds: Int,
        val isFlashing: Boolean = false
    ) : PhotoboothScreenState()

    object Composing : PhotoboothScreenState()

    data class Preview(
        val composedBitmap: Bitmap
    ) : PhotoboothScreenState()

    data class Printing(
        val remainingSeconds: Int,
        val statusMessage: String,
        val isCompleted: Boolean = false
    ) : PhotoboothScreenState()
}
