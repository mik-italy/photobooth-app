package com.photobooth.app

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.photobooth.app.ui.MainPhotoboothScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Force landscape mode explicitly for tablet
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // 2. Keep screen awake continuously
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 3. Safe modern full-screen immersive setup
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainPhotoboothScreen(
                        onSetFlashBrightness = { maxBrightness ->
                            setFlashBrightness(maxBrightness)
                        }
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        try {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } catch (_: Exception) {}
    }

    /**
     * Controls the screen brightness for the internal white flash feature.
     * When true, pumps the screen brightness to 100% (1.0f) to illuminate faces.
     */
    private fun setFlashBrightness(maxBrightness: Boolean) {
        runOnUiThread {
            try {
                val params = window.attributes
                if (maxBrightness) {
                    params.screenBrightness = 1.0f // 100% full brightness
                } else {
                    params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
                window.attributes = params
            } catch (_: Exception) {}
        }
    }
}
