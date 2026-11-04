package com.photobooth.app.data

import android.content.Context
import android.content.SharedPreferences

data class StripStyle(
    val backgroundColorHex: String = "#FFFFFF",
    val titleColorHex: String = "#1E1E1E",
    val subtitleColorHex: String = "#666666",
    val accentColorHex: String = "#D4AF37", // Elegant gold
    val photoBorderColorHex: String = "#E0E0E0",
    val showCutLine: Boolean = true
)

data class SettingsData(
    val printerIp: String = "192.168.4.1",
    val printerPort: Int = 631,
    val eventTitle: String = "PHOTO BOOTH",
    val eventSubtitle: String = "SPECIALE EVENTO",
    val eventHashtag: String = "✦   ✦   ✦",
    val stripBackgroundColor: String = "#FFFFFF", // #FFFFFF, #181818, #FDFBF7, #FCE4EC
    val stripTextColor: String = "#181818",
    val countdownSeconds: Int = 3,
    val adminPin: String = "1234",
    val waitForPrinterFinish: Boolean = true,
    val printTimeoutSeconds: Int = 45,
    val useDirectIpp: Boolean = true,
    val shotsPerSession: Int = 3
)

class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("photobooth_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): SettingsData {
        return SettingsData(
            printerIp = prefs.getString("printer_ip", "192.168.4.1") ?: "192.168.4.1",
            printerPort = prefs.getInt("printer_port", 631),
            eventTitle = prefs.getString("event_title", "PHOTO BOOTH") ?: "PHOTO BOOTH",
            eventSubtitle = prefs.getString("event_subtitle", "SPECIALE EVENTO") ?: "SPECIALE EVENTO",
            eventHashtag = prefs.getString("event_hashtag", "✦   ✦   ✦") ?: "✦   ✦   ✦",
            stripBackgroundColor = prefs.getString("strip_bg_color", "#FFFFFF") ?: "#FFFFFF",
            stripTextColor = prefs.getString("strip_text_color", "#181818") ?: "#181818",
            countdownSeconds = prefs.getInt("countdown_seconds", 3),
            adminPin = prefs.getString("admin_pin", "1234") ?: "1234",
            waitForPrinterFinish = prefs.getBoolean("wait_for_printer", true),
            printTimeoutSeconds = prefs.getInt("print_timeout", 45),
            useDirectIpp = prefs.getBoolean("use_direct_ipp", true),
            shotsPerSession = prefs.getInt("shots_per_session", 3)
        )
    }

    fun saveSettings(settings: SettingsData) {
        prefs.edit().apply {
            putString("printer_ip", settings.printerIp)
            putInt("printer_port", settings.printerPort)
            putString("event_title", settings.eventTitle)
            putString("event_subtitle", settings.eventSubtitle)
            putString("event_hashtag", settings.eventHashtag)
            putString("strip_bg_color", settings.stripBackgroundColor)
            putString("strip_text_color", settings.stripTextColor)
            putInt("countdown_seconds", settings.countdownSeconds)
            putString("admin_pin", settings.adminPin)
            putBoolean("wait_for_printer", settings.waitForPrinterFinish)
            putInt("print_timeout", settings.printTimeoutSeconds)
            putBoolean("use_direct_ipp", settings.useDirectIpp)
            putInt("shots_per_session", settings.shotsPerSession)
            apply()
        }
    }
}
