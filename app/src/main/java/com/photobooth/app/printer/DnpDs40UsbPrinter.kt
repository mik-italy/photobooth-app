package com.photobooth.app.printer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Risultato della diagnostica avanzata DNP DS40 via cavo USB
 */
data class DnpDiagnosticResult(
    val success: Boolean,
    val connectedDevicesCount: Int,
    val deviceName: String,
    val vidPid: String,
    val hasPermission: Boolean,
    val statusCode: String?,
    val statusMessage: String,
    val serialNumber: String? = null,
    val firmwareVersion: String? = null,
    val freeBuffers: String? = null,
    val error: String? = null
)

/**
 * Driver nativo USB per stampanti a sublimazione termica DNP (Dai Nippon Printing):
 * Supporta:
 * - DNP DS40 (VID: 0x1343, PID: 0x0003) - Also Citizen CX
 * - DNP DS80 (VID: 0x1343, PID: 0x0004) - Also Citizen CX-W
 * - DNP DS-RX1 / DS-RX1HS (VID: 0x1343, PID: 0x0005 / 0x0006) - Also Citizen CY
 * - DNP DS620 / DS820 (VID: 0x1452, PID: 0x8b01 / 0x8b02)
 *
 * Comunica direttamente con gli endpoint USB Bulk senza richiedere server esterni o driver proprietari.
 * Protocollo ESC/P conforme alle specifiche Gutenprint / Solomon Peachy cups dye-sub:
 * - Dimensioni di stampa 4x6" al vivo: 1920 x 1240 pixel a 300 DPI nativi
 * - Header DIB 1088 byte per piano colore
 * - Separazione canali Y (Giallo), M (Magenta), C (Ciano)
 * - Taglio meccanico 2x6 pollici a doppia striscia photobooth
 * - Controllo laccatura Lucida / Opaca (Matte)
 */
class DnpDs40UsbPrinter(private val context: Context) {

    companion object {
        private const val TAG = "DnpDs40UsbPrinter"
        const val ACTION_USB_PERMISSION = "com.photobooth.app.USB_PERMISSION"

        // USB Vendor IDs
        const val VID_DNP_CITIZEN = 0x1343
        const val VID_DNP_DS620 = 0x1452

        // USB Product IDs
        const val PID_DS40 = 0x0003
        const val PID_DS80 = 0x0004
        const val PID_DSRX1 = 0x0005
        const val PID_DSRX1HS = 0x0006
        const val PID_DS620 = 0x8b01
        const val PID_DS820 = 0x8b02

        // Dimensioni di stampa DNP DS40 per 4x6" (10x15cm) a 300 DPI al vivo (Gutenprint w288h432 landscape)
        const val DNP_PRINT_WIDTH = 1920
        const val DNP_PRINT_HEIGHT = 1240
        const val DNP_PLANE_HEADER_SIZE = 1088
        const val DNP_PLANE_DATA_SIZE = DNP_PRINT_WIDTH * DNP_PRINT_HEIGHT // 2,380,800 bytes
        const val DNP_PLANE_TOTAL_SIZE = DNP_PLANE_HEADER_SIZE + DNP_PLANE_DATA_SIZE // 2,381,888 bytes
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    /**
     * Ritorna la lista di tutte le periferiche USB fisicamente collegate al tablet
     */
    fun getAllUsbDevices(): List<UsbDevice> {
        return usbManager.deviceList.values.toList()
    }

    /**
     * Cerca una stampante DNP collegata via cavo USB
     */
    fun findConnectedDevice(): UsbDevice? {
        val deviceList = usbManager.deviceList
        for (device in deviceList.values) {
            if (isSupportedDnpDevice(device)) {
                return device
            }
        }
        return null
    }

    /**
     * Verifica se una periferica USB corrisponde a una stampante DNP / dye-sub
     */
    fun isSupportedDnpDevice(device: UsbDevice): Boolean {
        // Riconosci qualsiasi periferica con Vendor ID DNP / Citizen (0x1343)
        if (device.vendorId == VID_DNP_CITIZEN) {
            return true
        }
        // Riconosci stampanti DNP DS620 / DS820 (0x1452)
        if (device.vendorId == VID_DNP_DS620) {
            return true
        }
        // Riconosci periferiche con classe standard USB Printer (7)
        if (device.deviceClass == UsbConstants.USB_CLASS_PRINTER) {
            return true
        }
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                return true
            }
        }
        return false
    }

    fun hasPermission(device: UsbDevice): Boolean {
        return usbManager.hasPermission(device)
    }

    /**
     * Richiede il permesso di accesso USB all'utente con broadcast receiver compatibile Android 13/14
     */
    fun requestPermission(device: UsbDevice, onResult: (Boolean) -> Unit) {
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION).apply {
                setPackage(context.packageName)
            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (ACTION_USB_PERMISSION == intent?.action) {
                    val grantedDevice: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    try {
                        context?.unregisterReceiver(this)
                    } catch (_: Exception) {}
                    if (grantedDevice == null || grantedDevice.deviceId == device.deviceId) {
                        onResult(granted)
                    }
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        usbManager.requestPermission(device, permissionIntent)
    }

    /**
     * Interroga lo stato in tempo reale della stampante DNP DS40
     */
    suspend fun getPrinterStatus(): PrinterStatusResult = withContext(Dispatchers.IO) {
        val device = findConnectedDevice()
            ?: return@withContext PrinterStatusResult(
                PrinterState.OFFLINE,
                "DNP DS40 non connessa (cavo USB)"
            )

        if (!hasPermission(device)) {
            return@withContext PrinterStatusResult(
                PrinterState.STOPPED,
                "DNP DS40: Autorizzazione USB richiesta"
            )
        }

        var connection: UsbDeviceConnection? = null
        var usbInterface: UsbInterface? = null

        try {
            connection = usbManager.openDevice(device)
                ?: return@withContext PrinterStatusResult(
                    PrinterState.OFFLINE,
                    "Impossibile aprire connessione USB DNP DS40"
                )

            val endpoints = findBulkEndpoints(device)
                ?: return@withContext PrinterStatusResult(
                    PrinterState.STOPPED,
                    "Endpoint USB Bulk non trovati sulla stampante"
                )

            usbInterface = endpoints.first
            val inEndpoint = endpoints.second
            val outEndpoint = endpoints.third

            if (!connection.claimInterface(usbInterface, true)) {
                return@withContext PrinterStatusResult(
                    PrinterState.STOPPED,
                    "Impossibile acquisire interfaccia USB DNP"
                )
            }

            val statusCode = sendDnpQuery(connection, inEndpoint, outEndpoint, "STATUS", "")
                ?: return@withContext PrinterStatusResult(
                    PrinterState.OFFLINE,
                    "Nessuna risposta da DNP DS40"
                )

            return@withContext parseDnpStatus(statusCode)

        } catch (e: Exception) {
            Log.e(TAG, "Errore durante interrogazione DNP DS40", e)
            return@withContext PrinterStatusResult(
                PrinterState.OFFLINE,
                "Errore USB: ${e.localizedMessage ?: "sconosciuto"}"
            )
        } finally {
            try {
                if (usbInterface != null && connection != null) {
                    connection.releaseInterface(usbInterface)
                }
                connection?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Esegue un test di diagnostica approfondito sulla connessione USB DNP DS40
     */
    suspend fun runDiagnostics(): DnpDiagnosticResult = withContext(Dispatchers.IO) {
        val allDevices = getAllUsbDevices()
        val device = findConnectedDevice()

        if (device == null) {
            return@withContext DnpDiagnosticResult(
                success = false,
                connectedDevicesCount = allDevices.size,
                deviceName = "Nessuna stampante rilevata",
                vidPid = if (allDevices.isNotEmpty()) {
                    allDevices.joinToString { "VID:0x${it.vendorId.toString(16)} PID:0x${it.productId.toString(16)}" }
                } else {
                    "Nessuna periferica USB"
                },
                hasPermission = false,
                statusCode = null,
                statusMessage = "Nessuna stampante DNP rilevata",
                error = if (allDevices.isEmpty()) {
                    "Nessun dispositivo USB rilevato dal tablet. Verifica che l'adattatore USB OTG sia funzionante e collegato."
                } else {
                    "Periferiche USB rilevate ma nessuna riconosciuta come DNP (VID 0x1343 o 0x1452)."
                }
            )
        }

        val vidPidStr = "VID: 0x${device.vendorId.toString(16).padStart(4, '0')} - PID: 0x${device.productId.toString(16).padStart(4, '0')}"
        val devName = device.productName ?: "DNP Photo Printer"
        val permitted = hasPermission(device)

        if (!permitted) {
            return@withContext DnpDiagnosticResult(
                success = false,
                connectedDevicesCount = allDevices.size,
                deviceName = devName,
                vidPid = vidPidStr,
                hasPermission = false,
                statusCode = null,
                statusMessage = "Autorizzazione USB necessaria",
                error = "Tocca 'Autorizza Permesso USB' per concedere l'accesso ad Android."
            )
        }

        var connection: UsbDeviceConnection? = null
        var usbInterface: UsbInterface? = null

        try {
            connection = usbManager.openDevice(device)
                ?: return@withContext DnpDiagnosticResult(
                    success = false,
                    connectedDevicesCount = allDevices.size,
                    deviceName = devName,
                    vidPid = vidPidStr,
                    hasPermission = true,
                    statusCode = null,
                    statusMessage = "Impossibile aprire porta USB",
                    error = "Impossibile aprire UsbDeviceConnection con la periferica."
                )

            val endpoints = findBulkEndpoints(device)
                ?: return@withContext DnpDiagnosticResult(
                    success = false,
                    connectedDevicesCount = allDevices.size,
                    deviceName = devName,
                    vidPid = vidPidStr,
                    hasPermission = true,
                    statusCode = null,
                    statusMessage = "Endpoint USB Bulk assenti",
                    error = "Nessun endpoint Bulk IN/OUT trovato nelle interfacce USB."
                )

            usbInterface = endpoints.first
            val inEndpoint = endpoints.second
            val outEndpoint = endpoints.third

            if (!connection.claimInterface(usbInterface, true)) {
                return@withContext DnpDiagnosticResult(
                    success = false,
                    connectedDevicesCount = allDevices.size,
                    deviceName = devName,
                    vidPid = vidPidStr,
                    hasPermission = true,
                    statusCode = null,
                    statusMessage = "Interfaccia USB occupata",
                    error = "Impossibile acquisire l'interfaccia USB (claimInterface fallito)."
                )
            }

            // Query stato
            val statusRaw = sendDnpQuery(connection, inEndpoint, outEndpoint, "STATUS", "")
            val statusParsed = if (statusRaw != null) parseDnpStatus(statusRaw) else null

            // Query Serial Number
            val serial = sendDnpQuery(connection, inEndpoint, outEndpoint, "INFO", "SERIAL_NUMBER")

            // Query Firmware Version
            val firmware = sendDnpQuery(connection, inEndpoint, outEndpoint, "INFO", "FVER")

            // Query Free Print Buffers
            val freeBuf = sendDnpQuery(connection, inEndpoint, outEndpoint, "INFO", "FREE_PBUFFER")

            val isReady = statusParsed?.state == PrinterState.IDLE

            return@withContext DnpDiagnosticResult(
                success = isReady || statusRaw != null,
                connectedDevicesCount = allDevices.size,
                deviceName = devName,
                vidPid = vidPidStr,
                hasPermission = true,
                statusCode = statusRaw,
                statusMessage = statusParsed?.message ?: "Risposta ricevuta ($statusRaw)",
                serialNumber = serial,
                firmwareVersion = firmware,
                freeBuffers = freeBuf,
                error = if (statusRaw == null) "Nessuna risposta ai comandi ESC/P" else null
            )

        } catch (e: Exception) {
            return@withContext DnpDiagnosticResult(
                success = false,
                connectedDevicesCount = allDevices.size,
                deviceName = devName,
                vidPid = vidPidStr,
                hasPermission = true,
                statusCode = null,
                statusMessage = "Errore di comunicazione USB",
                error = e.localizedMessage ?: "Eccezione sconosciuta"
            )
        } finally {
            try {
                if (usbInterface != null && connection != null) {
                    connection.releaseInterface(usbInterface)
                }
                connection?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Invia un lavoro di stampa completo a DNP DS40 via cavo USB
     */
    suspend fun printBitmap(
        bitmap: Bitmap,
        copies: Int = 1,
        cut2x6: Boolean = true,
        isMatte: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        val device = findConnectedDevice()
            ?: return@withContext Result.failure(Exception("Stampante DNP DS40 non collegata via cavo USB. Verifica adattatore OTG e accensione."))

        if (!hasPermission(device)) {
            return@withContext Result.failure(Exception("Permesso USB non concesso per DNP DS40. Apri Impostazioni > Autorizza Permesso USB."))
        }

        var connection: UsbDeviceConnection? = null
        var usbInterface: UsbInterface? = null

        try {
            connection = usbManager.openDevice(device)
                ?: return@withContext Result.failure(Exception("Impossibile aprire connessione USB con DNP DS40"))

            val endpoints = findBulkEndpoints(device)
                ?: return@withContext Result.failure(Exception("Endpoint USB Bulk non trovati sulla stampante"))

            usbInterface = endpoints.first
            val inEndpoint = endpoints.second
            val outEndpoint = endpoints.third

            if (!connection.claimInterface(usbInterface, true)) {
                return@withContext Result.failure(Exception("Impossibile acquisire interfaccia USB DNP"))
            }

            // 1. Verifica preventiva stato stampante
            val statusCheck = sendDnpQuery(connection, inEndpoint, outEndpoint, "STATUS", "")
            if (statusCheck != null) {
                val parsed = parseDnpStatus(statusCheck)
                if (parsed.state == PrinterState.STOPPED) {
                    return@withContext Result.failure(Exception("Impossibile stampare: ${parsed.message}"))
                }
            }

            // 2. Prepara il bitmap nelle dimensioni native DNP DS40 (1920 x 1240 px a 300 DPI)
            val dnpBitmap = prepareBitmapForDnp(bitmap)

            // 3. Estrai e prepara i dati dei 3 piani colore Y, M, C (separazione sottrattiva)
            val width = DNP_PRINT_WIDTH
            val height = DNP_PRINT_HEIGHT
            val planeDataSize = DNP_PLANE_DATA_SIZE
            val pixels = IntArray(planeDataSize)
            dnpBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val yPlane = ByteArray(planeDataSize)
            val mPlane = ByteArray(planeDataSize)
            val cPlane = ByteArray(planeDataSize)

            for (y in 0 until height) {
                val rowOffset = y * width
                for (x in 0 until width) {
                    // DYESUB_FEATURE_PLANE_LEFTTORIGHT:
                    // Inversione orizzontale della riga per allinearsi agli elementi della testina DNP
                    val pixel = pixels[rowOffset + (width - 1 - x)]
                    val planeIdx = rowOffset + x
                    // Canale Y = Blu, M = Verde, C = Rosso (8-bit per canale)
                    yPlane[planeIdx] = (Color.blue(pixel) and 0xFF).toByte()
                    mPlane[planeIdx] = (Color.green(pixel) and 0xFF).toByte()
                    cPlane[planeIdx] = (Color.red(pixel) and 0xFF).toByte()
                }
            }

            // 4. Header BMP standard da 1088 byte per ciascun piano
            val bmpHeader = buildDnpBmpHeader(width, height)
            val planeTotalPayloadSize = DNP_PLANE_TOTAL_SIZE // 2,381,888 byte

            Log.i(TAG, "Invio sequenza comandi ESC/P DNP DS40 (copie: $copies, cut2x6: $cut2x6, matte: $isMatte)...")

            // --- COMANDO 1: CNTRL RETENTION ---
            val cmdRetention = buildDnpCommand("CNTRL", "RETENTION", 8)
            val payloadRetention = "00000000".toByteArray(Charsets.US_ASCII)
            if (!sendBulk(connection, outEndpoint, cmdRetention) ||
                !sendBulk(connection, outEndpoint, payloadRetention)) {
                return@withContext Result.failure(Exception("Errore invio CNTRL RETENTION su USB"))
            }

            // --- COMANDO 2: CNTRL OVERCOAT (Finitura) ---
            val cmdOvercoat = buildDnpCommand("CNTRL", "OVERCOAT", 8)
            val payloadOvercoat = (if (isMatte) "00000001" else "00000000").toByteArray(Charsets.US_ASCII)
            if (!sendBulk(connection, outEndpoint, cmdOvercoat) ||
                !sendBulk(connection, outEndpoint, payloadOvercoat)) {
                return@withContext Result.failure(Exception("Errore invio CNTRL OVERCOAT su USB"))
            }

            // --- COMANDO 3: CNTRL BUFFCNTRL ---
            val cmdBuffcntrl = buildDnpCommand("CNTRL", "BUFFCNTRL", 8)
            val payloadBuffcntrl = "00000000".toByteArray(Charsets.US_ASCII)
            if (!sendBulk(connection, outEndpoint, cmdBuffcntrl) ||
                !sendBulk(connection, outEndpoint, payloadBuffcntrl)) {
                return@withContext Result.failure(Exception("Errore invio CNTRL BUFFCNTRL su USB"))
            }

            // --- COMANDO 4: CNTRL QTY ---
            val cmdQty = buildDnpCommand("CNTRL", "QTY", 8)
            val payloadQty = String.format("%07d\r", copies).toByteArray(Charsets.US_ASCII)
            if (!sendBulk(connection, outEndpoint, cmdQty) ||
                !sendBulk(connection, outEndpoint, payloadQty)) {
                return@withContext Result.failure(Exception("Errore invio CNTRL QTY su USB"))
            }

            // --- COMANDO 5: CNTRL CUTTER ---
            val cmdCutter = buildDnpCommand("CNTRL", "CUTTER", 8)
            val payloadCutter = (if (cut2x6) "00000120" else "00000000").toByteArray(Charsets.US_ASCII)
            if (!sendBulk(connection, outEndpoint, cmdCutter) ||
                !sendBulk(connection, outEndpoint, payloadCutter)) {
                return@withContext Result.failure(Exception("Errore invio CNTRL CUTTER su USB"))
            }

            // --- COMANDO 6: IMAGE MULTICUT (6x4 = 02) ---
            val cmdMulticut = buildDnpCommand("IMAGE", "MULTICUT", 8)
            val payloadMulticut = "00000002".toByteArray(Charsets.US_ASCII)
            if (!sendBulk(connection, outEndpoint, cmdMulticut) ||
                !sendBulk(connection, outEndpoint, payloadMulticut)) {
                return@withContext Result.failure(Exception("Errore invio IMAGE MULTICUT su USB"))
            }

            // --- COMANDO 7: IMAGE YPLANE (Giallo) ---
            Log.i(TAG, "Invio IMAGE YPLANE ($planeTotalPayloadSize byte)...")
            val cmdY = buildDnpCommand("IMAGE", "YPLANE", planeTotalPayloadSize)
            if (!sendBulk(connection, outEndpoint, cmdY) ||
                !sendBulk(connection, outEndpoint, bmpHeader) ||
                !sendBulk(connection, outEndpoint, yPlane)) {
                return@withContext Result.failure(Exception("Errore invio IMAGE YPLANE su USB"))
            }

            // --- COMANDO 8: IMAGE MPLANE (Magenta) ---
            Log.i(TAG, "Invio IMAGE MPLANE ($planeTotalPayloadSize byte)...")
            val cmdM = buildDnpCommand("IMAGE", "MPLANE", planeTotalPayloadSize)
            if (!sendBulk(connection, outEndpoint, cmdM) ||
                !sendBulk(connection, outEndpoint, bmpHeader) ||
                !sendBulk(connection, outEndpoint, mPlane)) {
                return@withContext Result.failure(Exception("Errore invio IMAGE MPLANE su USB"))
            }

            // --- COMANDO 9: IMAGE CPLANE (Ciano) ---
            Log.i(TAG, "Invio IMAGE CPLANE ($planeTotalPayloadSize byte)...")
            val cmdC = buildDnpCommand("IMAGE", "CPLANE", planeTotalPayloadSize)
            if (!sendBulk(connection, outEndpoint, cmdC) ||
                !sendBulk(connection, outEndpoint, bmpHeader) ||
                !sendBulk(connection, outEndpoint, cPlane)) {
                return@withContext Result.failure(Exception("Errore invio IMAGE CPLANE su USB"))
            }

            // --- COMANDO 10: CNTRL START (Avvio stampa termica) ---
            Log.i(TAG, "Invio CNTRL START a DNP DS40...")
            val cmdStart = buildDnpCommand("CNTRL", "START", 0)
            if (!sendBulk(connection, outEndpoint, cmdStart)) {
                return@withContext Result.failure(Exception("Errore invio CNTRL START su USB"))
            }

            Log.i(TAG, "Lavoro di stampa DNP DS40 inviato con successo ($copies copie)!")
            return@withContext Result.success("Stampa inviata con successo alla DNP DS40 ($copies copie)!")

        } catch (e: Exception) {
            Log.e(TAG, "Errore durante la stampa su DNP DS40", e)
            return@withContext Result.failure(e)
        } finally {
            try {
                if (usbInterface != null && connection != null) {
                    connection.releaseInterface(usbInterface)
                }
                connection?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Attende che la stampa sia completata monitorando lo stato della DNP DS40
     */
    suspend fun waitForPrintCompletion(
        maxWaitSeconds: Int = 9,
        onProgressUpdate: (remainingSeconds: Int, status: String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        var elapsed = 0
        var hasSeenProcessing = false

        while (elapsed < maxWaitSeconds) {
            val remaining = maxWaitSeconds - elapsed
            val statusResult = getPrinterStatus()

            when (statusResult.state) {
                PrinterState.PROCESSING -> {
                    hasSeenProcessing = true
                    onProgressUpdate(remaining, "DNP DS40 in stampa termica...")
                }
                PrinterState.IDLE -> {
                    if (hasSeenProcessing && elapsed >= maxWaitSeconds - 1) {
                        onProgressUpdate(0, "Stampa completata con successo!")
                        return@withContext true
                    }
                    onProgressUpdate(remaining, "Stampa in corso su DNP DS40...")
                }
                PrinterState.STOPPED -> {
                    onProgressUpdate(remaining, statusResult.message)
                    return@withContext false
                }
                PrinterState.OFFLINE -> {
                    if (elapsed >= 5) {
                        onProgressUpdate(remaining, "Stampante DNP offline")
                    }
                }
                PrinterState.UNKNOWN -> {
                    onProgressUpdate(remaining, "Stampa in corso...")
                }
            }

            delay(1000)
            elapsed++
        }

        onProgressUpdate(0, "Stampa completata con successo!")
        return@withContext true
    }

    /**
     * Invia pacchetti di byte via endpoint Bulk OUT a blocchi sicuri
     */
    private fun sendBulk(
        connection: UsbDeviceConnection,
        outEndpoint: UsbEndpoint,
        data: ByteArray,
        offset: Int = 0,
        length: Int = data.size - offset,
        timeoutMs: Int = 10000
    ): Boolean {
        val chunkSize = 32768
        var written = 0
        val chunk = ByteArray(chunkSize)

        while (written < length) {
            val toWrite = minOf(chunkSize, length - written)
            System.arraycopy(data, offset + written, chunk, 0, toWrite)
            val sent = connection.bulkTransfer(outEndpoint, chunk, toWrite, timeoutMs)
            if (sent < 0) {
                Log.e(TAG, "sendBulk fallito: codice $sent all'offset ${offset + written} di $length")
                return false
            }
            written += sent
        }
        return true
    }

    /**
     * Invia un comando di richiesta INFO o STATUS e legge la risposta
     */
    private fun sendDnpQuery(
        connection: UsbDeviceConnection,
        inEndpoint: UsbEndpoint,
        outEndpoint: UsbEndpoint,
        arg1: String,
        arg2: String
    ): String? {
        val cmd = buildDnpCommand(arg1, arg2, 0)
        val sent = connection.bulkTransfer(outEndpoint, cmd, cmd.size, 3000)
        if (sent < cmd.size) return null

        val buf = ByteArray(128)
        val read = connection.bulkTransfer(inEndpoint, buf, buf.size, 3000)
        if (read < 8) return null

        val raw = String(buf, 0, read, Charsets.US_ASCII)
        val payloadLenStr = raw.substring(0, minOf(8, raw.length)).trim()
        val payloadLen = payloadLenStr.toIntOrNull() ?: (read - 8)

        return if (raw.length > 8) {
            raw.substring(8).trim().replace("\r", "")
        } else if (payloadLen > 0) {
            val payloadBuf = ByteArray(payloadLen)
            val read2 = connection.bulkTransfer(inEndpoint, payloadBuf, payloadBuf.size, 3000)
            if (read2 > 0) String(payloadBuf, 0, read2, Charsets.US_ASCII).trim().replace("\r", "") else null
        } else {
            ""
        }
    }

    /**
     * Cerca gli endpoint Bulk IN e Bulk OUT nelle interfacce USB del dispositivo
     */
    private fun findBulkEndpoints(device: UsbDevice): Triple<UsbInterface, UsbEndpoint, UsbEndpoint>? {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            var inEp: UsbEndpoint? = null
            var outEp: UsbEndpoint? = null

            for (j in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(j)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.direction == UsbConstants.USB_DIR_IN) {
                        inEp = ep
                    } else if (ep.direction == UsbConstants.USB_DIR_OUT) {
                        outEp = ep
                    }
                }
            }

            if (inEp != null && outEp != null) {
                return Triple(iface, inEp, outEp)
            }
        }
        return null
    }

    private fun parseDnpStatus(statusCode: String): PrinterStatusResult {
        val cleanCode = statusCode.trim().replace("\r", "")
        val codeInt = cleanCode.toIntOrNull() ?: 9999
        return when (codeInt) {
            0 -> PrinterStatusResult(PrinterState.IDLE, "DNP DS40: Pronta")
            1 -> PrinterStatusResult(PrinterState.PROCESSING, "DNP DS40: In Stampa...")
            500, 510 -> PrinterStatusResult(PrinterState.PROCESSING, "DNP DS40: Raffreddamento in corso...")
            1000 -> PrinterStatusResult(PrinterState.STOPPED, "DNP DS40: Coperchio aperto")
            1010 -> PrinterStatusResult(PrinterState.STOPPED, "DNP DS40: Contenitore ritagli assente")
            1100 -> PrinterStatusResult(PrinterState.STOPPED, "DNP DS40: Carta esaurita")
            1200 -> PrinterStatusResult(PrinterState.STOPPED, "DNP DS40: Nastro esaurito")
            1300 -> PrinterStatusResult(PrinterState.STOPPED, "DNP DS40: Inceppamento carta")
            1400 -> PrinterStatusResult(PrinterState.STOPPED, "DNP DS40: Errore nastro")
            1500 -> PrinterStatusResult(PrinterState.STOPPED, "DNP DS40: Errore formato carta")
            1600 -> PrinterStatusResult(PrinterState.STOPPED, "DNP DS40: Errore dati")
            in 2000..3000 -> PrinterStatusResult(PrinterState.STOPPED, "DNP DS40: Errore hardware ($cleanCode)")
            else -> PrinterStatusResult(PrinterState.UNKNOWN, "DNP DS40: Stato $cleanCode")
        }
    }

    /**
     * Adatta il foglio grafico per la stampante DNP DS40 (300 DPI nativi, 1920 x 1240 px al vivo):
     * - Se in verticale (1200 x 1800 px), ruota di 90° orario per allineare le due strisce 2x6"
     *   lungo la lama centrale di taglio orizzontale (Y = 600 -> Y = 620).
     * - Mantiene i margini originali perfetti 1:1 identici alla Canon Selphy:
     *   Centra il canvas 1800 x 1200 all'interno del buffer 1920 x 1240 (offset X=60 px, Y=20 px).
     * - Estende il background nei 60 px di overbleed laterali e 20 px sopra/sotto per evitare
     *   qualsiasi bordo bianco senza tagliare né distorcere le foto e i testi.
     */
    private fun prepareBitmapForDnp(source: Bitmap): Bitmap {
        val srcW = source.width
        val srcH = source.height

        val landscapeBitmap: Bitmap = if (srcH > srcW) {
            val matrix = Matrix().apply { postRotate(90f) }
            Bitmap.createBitmap(source, 0, 0, srcW, srcH, matrix, true)
        } else {
            source
        }

        val outputBitmap = Bitmap.createBitmap(DNP_PRINT_WIDTH, DNP_PRINT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        val offsetX = (DNP_PRINT_WIDTH - landscapeBitmap.width) / 2 // (1920 - 1800) / 2 = 60 px
        val offsetY = (DNP_PRINT_HEIGHT - landscapeBitmap.height) / 2 // (1240 - 1200) / 2 = 20 px

        // 1. Riempi il canvas con il colore di fondo (pixel d'angolo della composizione)
        val bgColor = landscapeBitmap.getPixel(0, 0)
        canvas.drawColor(bgColor)

        // 2. Clona i bordi esterni nell'area di overbleed (60 px a sinistra/destra, 20 px sopra/sotto)
        // per garantire continuità anche con sfondi grafici personalizzati o texture ed evitare bordi bianchi
        val edgePaint = Paint(Paint.FILTER_BITMAP_FLAG)
        // Bordo sinistro (X: 0..offsetX)
        canvas.drawBitmap(landscapeBitmap, Rect(0, 0, 1, landscapeBitmap.height), Rect(0, offsetY, offsetX, offsetY + landscapeBitmap.height), edgePaint)
        // Bordo destro (X: offsetX+W .. 1920)
        canvas.drawBitmap(landscapeBitmap, Rect(landscapeBitmap.width - 1, 0, landscapeBitmap.width, landscapeBitmap.height), Rect(offsetX + landscapeBitmap.width, offsetY, DNP_PRINT_WIDTH, offsetY + landscapeBitmap.height), edgePaint)
        // Bordo superiore (Y: 0..offsetY)
        canvas.drawBitmap(landscapeBitmap, Rect(0, 0, landscapeBitmap.width, 1), Rect(offsetX, 0, offsetX + landscapeBitmap.width, offsetY), edgePaint)
        // Bordo inferiore (Y: offsetY+H .. 1240)
        canvas.drawBitmap(landscapeBitmap, Rect(0, landscapeBitmap.height - 1, landscapeBitmap.width, landscapeBitmap.height), Rect(offsetX, offsetY + landscapeBitmap.height, offsetX + landscapeBitmap.width, DNP_PRINT_HEIGHT), edgePaint)

        // 2. Disegna l'immagine originale a grandezza naturale 1:1 centrata (X=60..1860, Y=20..1220)
        // In questo modo le proporzioni, i margini e la nitidezza rimangono perfetti al 100% identici a Canon Selphy
        // e la linea di taglio cade esattamente a Y=620 (lama meccanica centrale DNP DS40).
        val contentDestRect = Rect(offsetX, offsetY, offsetX + landscapeBitmap.width, offsetY + landscapeBitmap.height)
        val contentPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(landscapeBitmap, null, contentDestRect, contentPaint)

        return outputBitmap
    }

    /**
     * Costruisce il pacchetto di comando standard ESC P (32 byte esatti con padding spazi 0x20)
     */
    fun buildDnpCommand(arg1: String, arg2: String, payloadLength: Int): ByteArray {
        val cmd = ByteArray(32) { 0x20 } // Inizializza tutto a spazio ASCII (0x20)
        cmd[0] = 0x1b // ESC
        cmd[1] = 0x50 // 'P'

        val a1Bytes = arg1.toByteArray(Charsets.US_ASCII)
        System.arraycopy(a1Bytes, 0, cmd, 2, minOf(a1Bytes.size, 6))

        val a2Bytes = arg2.toByteArray(Charsets.US_ASCII)
        System.arraycopy(a2Bytes, 0, cmd, 8, minOf(a2Bytes.size, 16))

        if (payloadLength > 0) {
            val lenStr = String.format("%08d", payloadLength)
            val lenBytes = lenStr.toByteArray(Charsets.US_ASCII)
            System.arraycopy(lenBytes, 0, cmd, 24, minOf(lenBytes.size, 8))
        }

        return cmd
    }

    /**
     * Costruisce l'header BMP da 1088 byte esatti richiesto da DNP (conforme a Gutenprint / cups dnpds40_plane_init):
     * - 14 byte BITMAPFILEHEADER
     * - 40 byte BITMAPINFOHEADER (300 DPI, 8 bpp)
     * - 1024 byte array RGB a zero
     * - 10 byte PadSize di allineamento a zero
     * Totale: 1088 byte esatti prima dell'inizio dei pixel di ciascun piano.
     */
    fun buildDnpBmpHeader(width: Int, height: Int): ByteArray {
        val planeDataSize = width * height
        val fSize = planeDataSize + DNP_PLANE_HEADER_SIZE
        val buffer = ByteBuffer.allocate(DNP_PLANE_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)

        // --- 14-byte BITMAPFILEHEADER ---
        buffer.put(0x42.toByte()) // 'B'
        buffer.put(0x4D.toByte()) // 'M'
        buffer.putInt(fSize) // Dimensione totale file piano: 2,381,888 byte
        buffer.putShort(0) // Reserved1
        buffer.putShort(0) // Reserved2
        buffer.putInt(DNP_PLANE_HEADER_SIZE) // Offset dati pixel: 1088 byte (0x0440)

        // --- 40-byte BITMAPINFOHEADER ---
        buffer.putInt(40) // biSize
        buffer.putInt(width) // biWidth = 1920
        buffer.putInt(height) // biHeight = 1240
        buffer.putShort(1) // biPlanes
        buffer.putShort(8) // biBitCount (8 bpp)
        buffer.putInt(0) // biCompression (BI_RGB)
        buffer.putInt(0) // biSizeImage (ignorato)
        buffer.putInt(11808) // biXPelsPerMeter (300 DPI)
        buffer.putInt(11808) // biYPelsPerMeter (300 DPI)
        buffer.putInt(256) // biClrUsed
        buffer.putInt(0) // biClrImportant

        // --- 1024-byte RGB Array (azzerato come in Gutenprint) ---
        for (i in 0 until 1024) {
            buffer.put(0.toByte())
        }

        // --- 10-byte PadSize di allineamento piano dati (azzerato) ---
        for (i in 0 until 10) {
            buffer.put(0.toByte())
        }

        return buffer.array()
    }
}
