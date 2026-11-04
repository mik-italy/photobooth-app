package com.photobooth.app.data

import android.content.Context
import android.content.SharedPreferences

data class SettingsData(
    val printerIp: String = "192.168.4.1",
    val printerPort: Int = 631,
    val eventTitle: String = "PHOTO BOOTH",
    val eventSubtitle: String = "MEMORIES",
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
            eventSubtitle = prefs.getString("event_subtitle", "MEMORIES") ?: "MEMORIES",
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
