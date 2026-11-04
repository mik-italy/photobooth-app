package com.photobooth.app.data

import android.content.Context
import android.content.SharedPreferences

enum class LayoutMode {
    STRIP_4_PHOTOS,   // 4 FOTO (Doppia striscia 2x6)
    SINGLE_PHOTO      // 1 FOTO (Foto unica orizzontale 10x15)
}

enum class FontChoice {
    PLAYFAIR,        // Playfair Display Bold Italic (Elegante)
    MONTSERRAT,      // Montserrat Bold (Moderno Sans)
    DANCING_SCRIPT,  // Dancing Script (Romantico Corsivo)
    CINZEL           // Cinzel Bold (Classico / Lusso)
}

data class SettingsData(
    val printerIp: String = "192.168.4.1",
    val printerPort: Int = 631,
    val eventTitle: String = "Marco & Sara",
    val showTitleText: Boolean = true, // Opzione per nascondere il testo
    val fontChoice: FontChoice = FontChoice.PLAYFAIR,
    val layoutMode: LayoutMode = LayoutMode.STRIP_4_PHOTOS,
    val stripBackgroundColor: String = "#FFFFFF",
    val stripTextColor: String = "#181818",
    val titleFontSize: Int = 48,
    val stripBgImagePath: String? = null,        // Sfondo per 4 FOTO
    val singlePhotoBgImagePath: String? = null,  // Sfondo per 1 FOTO
    val adminPin: String = "3552",               // Password richiesta: 3552
    val countdownSeconds: Int = 5,
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

        val fontStr = prefs.getString("font_choice", FontChoice.PLAYFAIR.name)
        val font = try {
            FontChoice.valueOf(fontStr ?: FontChoice.PLAYFAIR.name)
        } catch (_: Exception) {
            FontChoice.PLAYFAIR
        }

        return SettingsData(
            printerIp = prefs.getString("printer_ip", "192.168.4.1") ?: "192.168.4.1",
            printerPort = prefs.getInt("printer_port", 631),
            eventTitle = prefs.getString("event_title", "Marco & Sara") ?: "Marco & Sara",
            showTitleText = prefs.getBoolean("show_title_text", true),
            fontChoice = font,
            layoutMode = mode,
            stripBackgroundColor = prefs.getString("strip_bg_color", "#FFFFFF") ?: "#FFFFFF",
            stripTextColor = prefs.getString("strip_text_color", "#181818") ?: "#181818",
            titleFontSize = prefs.getInt("title_font_size", 48),
            stripBgImagePath = prefs.getString("strip_bg_path", null),
            singlePhotoBgImagePath = prefs.getString("single_bg_path", null),
            adminPin = prefs.getString("admin_pin", "3552") ?: "3552",
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
            putBoolean("show_title_text", settings.showTitleText)
            putString("font_choice", settings.fontChoice.name)
            putString("layout_mode", settings.layoutMode.name)
            putString("strip_bg_color", settings.stripBackgroundColor)
            putString("strip_text_color", settings.stripTextColor)
            putInt("title_font_size", settings.titleFontSize)
            putString("strip_bg_path", settings.stripBgImagePath)
            putString("single_bg_path", settings.singlePhotoBgImagePath)
            putString("admin_pin", settings.adminPin)
            putInt("countdown_seconds", settings.countdownSeconds)
            putBoolean("wait_for_printer", settings.waitForPrinterFinish)
            putInt("print_timeout", settings.printTimeoutSeconds)
            putBoolean("use_direct_ipp", settings.useDirectIpp)
            apply()
        }
    }
}
