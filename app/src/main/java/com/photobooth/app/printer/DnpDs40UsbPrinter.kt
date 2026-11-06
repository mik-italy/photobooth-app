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
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Driver nativo USB per stampanti a sublimazione termica DNP (Dai Nippon Printing):
 * Supporta:
 * - DNP DS40 (VID: 0x1343, PID: 0x0003) - Also Citizen CX
 * - DNP DS-RX1 (VID: 0x1343, PID: 0x0005) - Also Citizen CY
 * - DNP DS620 (VID: 0x1452, PID: 0x8b01)
 *
 * Comunica via USB Host (OTG) tramite endpoint Bulk standard.
 * Supporta risoluzione nativa 300 DPI (1920 x 1240 px con abbondanza al vivo),
 * separazione canali Y (Giallo), M (Magenta), C (Ciano), finitura Lucida/Opaca,
 * e taglio automatico 2x6 pollici per strisce photobooth.
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
        const val PID_DSRX1 = 0x0005
        const val PID_DS620 = 0x8b01

        // Dimensioni di stampa DNP DS40 per 4x6" (10x15cm) a 300 DPI al vivo
        const val DNP_PRINT_WIDTH = 1920
        const val DNP_PRINT_HEIGHT = 1240
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

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

    fun isSupportedDnpDevice(device: UsbDevice): Boolean {
        if (device.vendorId == VID_DNP_CITIZEN) {
            if (device.productId == PID_DS40 || device.productId == PID_DSRX1) {
                return true
            }
        }
        if (device.vendorId == VID_DNP_DS620 && device.productId == PID_DS620) {
            return true
        }
        return false
    }

    fun hasPermission(device: UsbDevice): Boolean {
        return usbManager.hasPermission(device)
    }

    fun requestPermission(device: UsbDevice, onResult: (Boolean) -> Unit) {
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
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
                    val grantedDevice: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    try {
                        context?.unregisterReceiver(this)
                    } catch (_: Exception) {}
                    if (grantedDevice?.deviceId == device.deviceId) {
                        onResult(granted)
                    }
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
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

            usbInterface = device.getInterface(0)
            if (!connection.claimInterface(usbInterface, true)) {
                return@withContext PrinterStatusResult(
                    PrinterState.STOPPED,
                    "Impossibile acquisire interfaccia USB DNP"
                )
            }

            var inEndpoint: UsbEndpoint? = null
            var outEndpoint: UsbEndpoint? = null

            for (i in 0 until usbInterface.endpointCount) {
                val ep = usbInterface.getEndpoint(i)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.direction == UsbConstants.USB_DIR_IN) {
                        inEndpoint = ep
                    } else if (ep.direction == UsbConstants.USB_DIR_OUT) {
                        outEndpoint = ep
                    }
                }
            }

            if (inEndpoint == null || outEndpoint == null) {
                return@withContext PrinterStatusResult(
                    PrinterState.STOPPED,
                    "Endpoint USB DNP non validi"
                )
            }

            // Invia comando STATUS
            val cmd = buildDnpCommand("STATUS", "", 0)
            val sent = connection.bulkTransfer(outEndpoint, cmd, cmd.size, 3000)
            if (sent < cmd.size) {
                return@withContext PrinterStatusResult(
                    PrinterState.OFFLINE,
                    "Timeout invio comando STATUS"
                )
            }

            // Legge header risposta (8 byte con lunghezza decimale payload)
            val headerBuf = ByteArray(8)
            val readHdr = connection.bulkTransfer(inEndpoint, headerBuf, headerBuf.size, 3000)
            if (readHdr < 8) {
                return@withContext PrinterStatusResult(
                    PrinterState.OFFLINE,
                    "Nessuna risposta da DNP DS40"
                )
            }

            val payloadLenStr = String(headerBuf).trim()
            val payloadLen = payloadLenStr.toIntOrNull() ?: 5
            val payloadBuf = ByteArray(payloadLen)
            val readPayload = connection.bulkTransfer(inEndpoint, payloadBuf, payloadBuf.size, 3000)
            if (readPayload < 0) {
                return@withContext PrinterStatusResult(
                    PrinterState.OFFLINE,
                    "Errore lettura payload stato DNP"
                )
            }

            val statusCode = String(payloadBuf, 0, minOf(5, readPayload)).trim()
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

    private fun parseDnpStatus(statusCode: String): PrinterStatusResult {
        val codeInt = statusCode.toIntOrNull() ?: 9999
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
            in 2000..3000 -> PrinterStatusResult(PrinterState.STOPPED, "DNP DS40: Errore hardware ($statusCode)")
            else -> PrinterStatusResult(PrinterState.UNKNOWN, "DNP DS40: Stato $statusCode")
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
            ?: return@withContext Result.failure(Exception("Stampante DNP DS40 non collegata via cavo USB"))

        if (!hasPermission(device)) {
            return@withContext Result.failure(Exception("Permesso USB non concesso per DNP DS40"))
        }

        var connection: UsbDeviceConnection? = null
        var usbInterface: UsbInterface? = null

        try {
            connection = usbManager.openDevice(device)
                ?: return@withContext Result.failure(Exception("Impossibile aprire connessione USB con DNP DS40"))

            usbInterface = device.getInterface(0)
            if (!connection.claimInterface(usbInterface, true)) {
                return@withContext Result.failure(Exception("Impossibile acquisire interfaccia USB DNP"))
            }

            var inEndpoint: UsbEndpoint? = null
            var outEndpoint: UsbEndpoint? = null

            for (i in 0 until usbInterface.endpointCount) {
                val ep = usbInterface.getEndpoint(i)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.direction == UsbConstants.USB_DIR_IN) {
                        inEndpoint = ep
                    } else if (ep.direction == UsbConstants.USB_DIR_OUT) {
                        outEndpoint = ep
                    }
                }
            }

            if (inEndpoint == null || outEndpoint == null) {
                return@withContext Result.failure(Exception("Endpoint USB non validi sulla stampante"))
            }

            // 1. Verifica disponibilità buffer di stampa
            waitForFreePrintBuffer(connection, inEndpoint, outEndpoint, maxWaitSeconds = 15)

            // 2. Prepara il bitmap nelle dimensioni native DNP DS40 (1920 x 1240 px)
            val dnpBitmap = prepareBitmapForDnp(bitmap)

            // 3. Costruisci il flusso completo di comandi e dati immagine YMC
            val jobStream = buildDnpPrintJobStream(dnpBitmap, copies, cut2x6, isMatte)

            // 4. Trasmetti il flusso dati a blocchi via bulk transfer
            val chunkSize = 32768
            var offset = 0
            val totalBytes = jobStream.size
            val chunkBuffer = ByteArray(chunkSize)

            while (offset < totalBytes) {
                val bytesToSend = minOf(chunkSize, totalBytes - offset)
                System.arraycopy(jobStream, offset, chunkBuffer, 0, bytesToSend)

                val sent = connection.bulkTransfer(outEndpoint, chunkBuffer, bytesToSend, 8000)
                if (sent < 0) {
                    return@withContext Result.failure(Exception("Errore di trasmissione USB a offset $offset / $totalBytes"))
                }
                offset += sent
            }

            Log.i(TAG, "Lavoro di stampa DNP DS40 inviato con successo ($totalBytes byte, $copies copie)")
            return@withContext Result.success("Stampa inviata a DNP DS40 ($copies copie)!")

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
        maxWaitSeconds: Int = 25,
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
                    onProgressUpdate(remaining, "DNP DS40 in stampa...")
                }
                PrinterState.IDLE -> {
                    if (hasSeenProcessing && elapsed >= 4) {
                        onProgressUpdate(0, "Stampa completata con successo!")
                        return@withContext true
                    }
                    onProgressUpdate(remaining, "Preparazione foglio DNP DS40...")
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
                    onProgressUpdate(remaining, "Comunicazione con DNP...")
                }
            }

            delay(1000)
            elapsed++
        }

        return@withContext true
    }

    private suspend fun waitForFreePrintBuffer(
        connection: UsbDeviceConnection,
        inEp: UsbEndpoint,
        outEp: UsbEndpoint,
        maxWaitSeconds: Int
    ): Boolean {
        var waited = 0
        while (waited < maxWaitSeconds) {
            val cmd = buildDnpCommand("INFO", "FREE_PBUFFER", 0)
            val sent = connection.bulkTransfer(outEp, cmd, cmd.size, 3000)
            if (sent == cmd.size) {
                val hdr = ByteArray(8)
                val readHdr = connection.bulkTransfer(inEp, hdr, hdr.size, 3000)
                if (readHdr == 8) {
                    val len = String(hdr).trim().toIntOrNull() ?: 5
                    val respBuf = ByteArray(len)
                    val readResp = connection.bulkTransfer(inEp, respBuf, respBuf.size, 3000)
                    if (readResp > 0) {
                        val resp = String(respBuf, 0, readResp).trim()
                        if (resp.startsWith("FBP")) {
                            val freeBuffers = resp.substring(3).toIntOrNull() ?: 0
                            if (freeBuffers > 0) {
                                return true
                            }
                        }
                    }
                }
            }
            delay(500)
            waited++
        }
        return true
    }

    /**
     * Ridimensiona e orienta il bitmap per il rullo da 6 pollici di DNP DS40:
     * - Se verticale (1200 x 1800), ruota di 90° in orizzontale (1800 x 1200) e adatta a 1920 x 1240.
     * - Se orizzontale (1800 x 1200), adatta direttamente a 1920 x 1240.
     */
    private fun prepareBitmapForDnp(source: Bitmap): Bitmap {
        val srcW = source.width
        val srcH = source.height

        val rotatedBitmap: Bitmap = if (srcH > srcW) {
            // Ruota 90 gradi orario in landscape per orientare le strisce lungo il foglio
            val matrix = Matrix().apply { postRotate(90f) }
            Bitmap.createBitmap(source, 0, 0, srcW, srcH, matrix, true)
        } else {
            source
        }

        // Scala a 1920 x 1240 con ritaglio centrale
        val outputBitmap = Bitmap.createBitmap(DNP_PRINT_WIDTH, DNP_PRINT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        val rw = rotatedBitmap.width.toFloat()
        val rh = rotatedBitmap.height.toFloat()
        val destW = DNP_PRINT_WIDTH.toFloat()
        val destH = DNP_PRINT_HEIGHT.toFloat()

        val rRatio = rw / rh
        val destRatio = destW / destH

        val srcRect = if (rRatio > destRatio) {
            val newWidth = (rh * destRatio).toInt()
            val left = ((rw - newWidth) / 2f).toInt()
            Rect(left, 0, left + newWidth, rh.toInt())
        } else {
            val newHeight = (rw / destRatio).toInt()
            val top = ((rh - newHeight) / 2f).toInt()
            Rect(0, top, rw.toInt(), top + newHeight)
        }

        val destRect = Rect(0, 0, DNP_PRINT_WIDTH, DNP_PRINT_HEIGHT)
        canvas.drawBitmap(rotatedBitmap, srcRect, destRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))

        return outputBitmap
    }

    /**
     * Costruisce il pacchetto di comando standard ESC P (32 byte)
     */
    private fun buildDnpCommand(arg1: String, arg2: String, payloadLength: Int): ByteArray {
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
     * Genera l'intero flusso di comandi e dati grafici da inviare a DNP DS40
     */
    private fun buildDnpPrintJobStream(
        bitmap: Bitmap,
        copies: Int,
        cut2x6: Boolean,
        isMatte: Boolean
    ): ByteArray {
        val stream = ByteArrayOutputStream()

        // 1. CNTRL QTY
        val qtyPayload = String.format("%07d\r", copies).toByteArray(Charsets.US_ASCII)
        stream.write(buildDnpCommand("CNTRL", "QTY", 8))
        stream.write(qtyPayload)

        // 2. CNTRL BUFFCNTRL
        val buffPayload = "00000001".toByteArray(Charsets.US_ASCII)
        stream.write(buildDnpCommand("CNTRL", "BUFFCNTRL", 8))
        stream.write(buffPayload)

        // 3. CNTRL OVERCOAT (Finitura Opaca o Lucida)
        val overcoatVal = if (isMatte) "00000001" else "00000000"
        val overcoatPayload = overcoatVal.toByteArray(Charsets.US_ASCII)
        stream.write(buildDnpCommand("CNTRL", "OVERCOAT", 8))
        stream.write(overcoatPayload)

        // 4. CNTRL CUTTER (Taglio 2x6 pollici)
        if (cut2x6) {
            val cutterPayload = "00000120".toByteArray(Charsets.US_ASCII)
            stream.write(buildDnpCommand("CNTRL", "CUTTER", 8))
            stream.write(cutterPayload)
        }

        // 5. IMAGE MULTICUT (MULTICUT_6x4 = 2)
        val multicutPayload = "00000002".toByteArray(Charsets.US_ASCII)
        stream.write(buildDnpCommand("IMAGE", "MULTICUT", 8))
        stream.write(multicutPayload)

        // 6. Header BMP standard per i 3 piani (1088 byte = 14 byte file header + 40 byte DIB + 1024 palette)
        val planeDataSize = DNP_PRINT_WIDTH * DNP_PRINT_HEIGHT
        val bmpFileAndDataSize = 1088 + planeDataSize
        val bmpHeader = buildDnpBmpHeader(DNP_PRINT_WIDTH, DNP_PRINT_HEIGHT, planeDataSize)

        // Estrai canali pixel per i tre piani Y, M, C
        val width = DNP_PRINT_WIDTH
        val height = DNP_PRINT_HEIGHT
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val yPlane = ByteArray(planeDataSize)
        val mPlane = ByteArray(planeDataSize)
        val cPlane = ByteArray(planeDataSize)

        for (i in pixels.indices) {
            val color = pixels[i]
            // Canale Y = Blu, M = Verde, C = Rosso
            yPlane[i] = (Color.blue(color) and 0xFF).toByte()
            mPlane[i] = (Color.green(color) and 0xFF).toByte()
            cPlane[i] = (Color.red(color) and 0xFF).toByte()
        }

        val padding10 = ByteArray(10) { 0 }

        // 7. IMAGE YPLANE
        val yCmd = buildDnpCommand("IMAGE", "YPLANE", bmpFileAndDataSize + 24)
        stream.write(yCmd)
        stream.write(bmpHeader)
        stream.write(yPlane)
        stream.write(padding10)

        // 8. IMAGE MPLANE
        val mCmd = buildDnpCommand("IMAGE", "MPLANE", bmpFileAndDataSize + 24)
        stream.write(mCmd)
        stream.write(bmpHeader)
        stream.write(mPlane)
        stream.write(padding10)

        // 9. IMAGE CPLANE
        val cCmd = buildDnpCommand("IMAGE", "CPLANE", bmpFileAndDataSize + 24)
        stream.write(cCmd)
        stream.write(bmpHeader)
        stream.write(cPlane)
        stream.write(padding10)

        // 10. CNTRL START (Avvio stampa)
        val startCmd = buildDnpCommand("CNTRL", "START", 0)
        stream.write(startCmd)

        return stream.toByteArray()
    }

    /**
     * Costruisce l'header BMP a 8-bit in scala di grigi per DNP (1088 byte totali)
     */
    private fun buildDnpBmpHeader(width: Int, height: Int, imageSize: Int): ByteArray {
        val buffer = ByteBuffer.allocate(1088).order(ByteOrder.LITTLE_ENDIAN)

        // --- 14-byte BITMAPFILEHEADER ---
        buffer.put(0x42.toByte()) // 'B'
        buffer.put(0x4D.toByte()) // 'M'
        buffer.putInt(imageSize + 1088) // File size
        buffer.putShort(0) // Reserved1
        buffer.putShort(0) // Reserved2
        buffer.putInt(1088) // Offset a pixel data (0x0440)

        // --- 40-byte BITMAPINFOHEADER ---
        buffer.putInt(40) // biSize
        buffer.putInt(width) // biWidth = 1920
        buffer.putInt(height) // biHeight = 1240
        buffer.putShort(1) // biPlanes
        buffer.putShort(8) // biBitCount (8 bpp)
        buffer.putInt(0) // biCompression (BI_RGB)
        buffer.putInt(imageSize) // biSizeImage
        buffer.putInt(11808) // biXPelsPerMeter (300 DPI)
        buffer.putInt(11808) // biYPelsPerMeter (300 DPI)
        buffer.putInt(256) // biClrUsed
        buffer.putInt(256) // biClrImportant

        // --- 1024-byte Grayscale Color Palette (256 RGBQUAD) ---
        for (i in 0 until 256) {
            val b = i.toByte()
            buffer.put(b) // Blue
            buffer.put(b) // Green
            buffer.put(b) // Red
            buffer.put(0.toByte()) // Reserved
        }

        return buffer.array()
    }
}
