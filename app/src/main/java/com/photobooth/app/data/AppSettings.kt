package com.photobooth.app.data

import android.content.Context
import android.content.SharedPreferences

enum class LayoutMode {
    STRIP_4_PHOTOS,   // Doppia striscia 2x6 (4 foto)
    SINGLE_PHOTO      // Foto unica orizzontale 10x15 cm (1 foto)
}

data class SettingsData(
    val printerIp: String = "192.168.4.1",
    val printerPort: Int = 631,
    val eventTitle: String = "PHOTO BOOTH",
    val eventSubtitle: String = "SPECIALE EVENTO",
    val layoutMode: LayoutMode = LayoutMode.STRIP_4_PHOTOS,
    val stripBackgroundColor: String = "#FFFFFF",
    val stripTextColor: String = "#181818",
    val customBgImagePath: String? = null,
    val countdownSeconds: Int = 5, // Default 5 seconds as requested
    val waitForPrinterFinish: Boolean = true,
    val printTimeoutSeconds: Int = 45,
    val useDirectIpp: Boolean = true
) {
    val totalShots: Int
        get() = when (layoutMode) {
            LayoutMode.STRIP_4_PHOTOS -> 4
            LayoutMode.SINGLE_PHOTO -> 1
        }
}

class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("photobooth_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): SettingsData {
        val modeStr = prefs.getString("layout_mode", LayoutMode.STRIP_4_PHOTOS.name)
        val mode = try {
            LayoutMode.valueOf(modeStr ?: LayoutMode.STRIP_4_PHOTOS.name)
        } catch (_: Exception) {
            LayoutMode.STRIP_4_PHOTOS
        }

        return SettingsData(
            printerIp = prefs.getString("printer_ip", "192.168.4.1") ?: "192.168.4.1",
            printerPort = prefs.getInt("printer_port", 631),
            eventTitle = prefs.getString("event_title", "PHOTO BOOTH") ?: "PHOTO BOOTH",
            eventSubtitle = prefs.getString("event_subtitle", "SPECIALE EVENTO") ?: "SPECIALE EVENTO",
            layoutMode = mode,
            stripBackgroundColor = prefs.getString("strip_bg_color", "#FFFFFF") ?: "#FFFFFF",
            stripTextColor = prefs.getString("strip_text_color", "#181818") ?: "#181818",
            customBgImagePath = prefs.getString("custom_bg_path", null),
            countdownSeconds = prefs.getInt("countdown_seconds", 5),
            waitForPrinterFinish = prefs.getBoolean("wait_for_printer", true),
            printTimeoutSeconds = prefs.getInt("print_timeout", 45),
            useDirectIpp = prefs.getBoolean("use_direct_ipp", true)
        )
    }

    fun saveSettings(settings: SettingsData) {
        prefs.edit().apply {
            putString("printer_ip", settings.printerIp)
            putInt("printer_port", settings.printerPort)
            putString("event_title", settings.eventTitle)
            putString("event_subtitle", settings.eventSubtitle)
            putString("layout_mode", settings.layoutMode.name)
            putString("strip_bg_color", settings.stripBackgroundColor)
            putString("strip_text_color", settings.stripTextColor)
            putString("custom_bg_path", settings.customBgImagePath)
            putInt("countdown_seconds", settings.countdownSeconds)
            putBoolean("wait_for_printer", settings.waitForPrinterFinish)
            putInt("print_timeout", settings.printTimeoutSeconds)
            putBoolean("use_direct_ipp", settings.useDirectIpp)
            apply()
        }
    }
}
