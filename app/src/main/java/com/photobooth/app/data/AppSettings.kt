package com.photobooth.app.data

import android.content.Context
import android.content.SharedPreferences

enum class LayoutMode {
    STRIP_3_PHOTOS,   // 3 FOTO (Doppia striscia verticale 2x6)
    STRIP_2_PHOTOS,   // 2 FOTO (Doppia striscia orizzontale 6x2)
    SINGLE_PHOTO      // 1 FOTO (Foto unica orizzontale 10x15)
}

enum class PrinterType {
    CANON_SELPHY_WIFI,  // Canon SELPHY CP1500 (Wi-Fi IPP)
    DNP_DS40_USB        // DNP DS40 / Citizen CX (Cavo USB OTG)
}

enum class FontChoice {
    PLAYFAIR,        // Playfair Display Bold Italic (Elegante)
    MONTSERRAT,      // Montserrat Bold (Moderno Sans)
    DANCING_SCRIPT,  // Dancing Script (Romantico Corsivo)
    CINZEL           // Cinzel Bold (Classico / Lusso)
}

data class SettingsData(
    val printerType: PrinterType = PrinterType.CANON_SELPHY_WIFI,
    val printerIp: String = "192.168.0.1",      // Default IP Canon: 192.168.0.1
    val printerPort: Int = 631,
    val dnpCut2x6: Boolean = true,              // Taglio automatico 2x6" per strisce su DNP DS40
    val dnpMatte: Boolean = false,              // Finitura DNP: false = Lucida (Glossy), true = Opaca (Matte)
    val eventTitle: String = "Photobooth",      // Default: Photobooth
    val showTitleOnPrints: Boolean = false,     // Default: disattivato sotto le foto (sempre visibile in Home)
    val fontChoice: FontChoice = FontChoice.PLAYFAIR,
    val layoutMode: LayoutMode = LayoutMode.STRIP_3_PHOTOS,
    val stripBackgroundColor: String = "#FFFFFF",
    val stripTextColor: String = "#000000",
    val titleFontSize: Int = 48,
    val stripBgImagePath: String? = null,
    val singlePhotoBgImagePath: String? = null,
    val isPaused: Boolean = false,              // Modalità "In Pausa"
    val adminPin: String = "3552",              // Password di sicurezza: 3552
    val countdownSeconds: Int = 5,
    val waitForPrinterFinish: Boolean = true,
    val printTimeoutSeconds: Int = 40,
    val useDirectIpp: Boolean = true
) {
    val totalShots: Int
        get() = when (layoutMode) {
            LayoutMode.STRIP_3_PHOTOS -> 3
            LayoutMode.STRIP_2_PHOTOS -> 2
            LayoutMode.SINGLE_PHOTO -> 1
        }
}

class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("photobooth_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): SettingsData {
        val modeStr = prefs.getString("layout_mode", LayoutMode.STRIP_3_PHOTOS.name)
        val mode = try {
            if (modeStr == "STRIP_4_PHOTOS") LayoutMode.STRIP_3_PHOTOS
            else LayoutMode.valueOf(modeStr ?: LayoutMode.STRIP_3_PHOTOS.name)
        } catch (_: Exception) {
            LayoutMode.STRIP_3_PHOTOS
        }

        val fontStr = prefs.getString("font_choice", FontChoice.PLAYFAIR.name)
        val font = try {
            FontChoice.valueOf(fontStr ?: FontChoice.PLAYFAIR.name)
        } catch (_: Exception) {
            FontChoice.PLAYFAIR
        }

        val printerTypeStr = prefs.getString("printer_type", PrinterType.CANON_SELPHY_WIFI.name)
        val pType = try {
            PrinterType.valueOf(printerTypeStr ?: PrinterType.CANON_SELPHY_WIFI.name)
        } catch (_: Exception) {
            PrinterType.CANON_SELPHY_WIFI
        }

        return SettingsData(
            printerType = pType,
            printerIp = prefs.getString("printer_ip", "192.168.0.1") ?: "192.168.0.1",
            printerPort = prefs.getInt("printer_port", 631),
            dnpCut2x6 = prefs.getBoolean("dnp_cut_2x6", true),
            dnpMatte = prefs.getBoolean("dnp_matte", false),
            eventTitle = prefs.getString("event_title", "Photobooth") ?: "Photobooth",
            showTitleOnPrints = prefs.getBoolean("show_title_on_prints", false),
            fontChoice = font,
            layoutMode = mode,
            stripBackgroundColor = prefs.getString("strip_bg_color", "#FFFFFF") ?: "#FFFFFF",
            stripTextColor = prefs.getString("strip_text_color", "#000000") ?: "#000000",
            titleFontSize = prefs.getInt("title_font_size", 48),
            stripBgImagePath = prefs.getString("strip_bg_path", null),
            singlePhotoBgImagePath = prefs.getString("single_bg_path", null),
            isPaused = prefs.getBoolean("is_paused", false),
            adminPin = prefs.getString("admin_pin", "3552") ?: "3552",
            countdownSeconds = prefs.getInt("countdown_seconds", 5),
            waitForPrinterFinish = prefs.getBoolean("wait_for_printer", true),
            printTimeoutSeconds = prefs.getInt("print_timeout", 40),
            useDirectIpp = prefs.getBoolean("use_direct_ipp", true)
        )
    }

    fun saveSettings(settings: SettingsData) {
        prefs.edit().apply {
            putString("printer_type", settings.printerType.name)
            putString("printer_ip", settings.printerIp)
            putInt("printer_port", settings.printerPort)
            putBoolean("dnp_cut_2x6", settings.dnpCut2x6)
            putBoolean("dnp_matte", settings.dnpMatte)
            putString("event_title", settings.eventTitle)
            putBoolean("show_title_on_prints", settings.showTitleOnPrints)
            putString("font_choice", settings.fontChoice.name)
            putString("layout_mode", settings.layoutMode.name)
            putString("strip_bg_color", settings.stripBackgroundColor)
            putString("strip_text_color", settings.stripTextColor)
            putInt("title_font_size", settings.titleFontSize)
            putString("strip_bg_path", settings.stripBgImagePath)
            putString("single_bg_path", settings.singlePhotoBgImagePath)
            putBoolean("is_paused", settings.isPaused)
            putString("admin_pin", settings.adminPin)
            putInt("countdown_seconds", settings.countdownSeconds)
            putBoolean("wait_for_printer", settings.waitForPrinterFinish)
            putInt("print_timeout", settings.printTimeoutSeconds)
            putBoolean("use_direct_ipp", settings.useDirectIpp)
            apply()
        }
    }
}
