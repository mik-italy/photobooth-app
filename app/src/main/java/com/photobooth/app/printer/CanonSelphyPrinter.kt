package com.photobooth.app.printer

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.print.PrintHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

enum class PrinterState {
    IDLE,
    PROCESSING,
    STOPPED,
    OFFLINE,
    UNKNOWN
}

data class PrinterStatusResult(
    val state: PrinterState,
    val message: String
)

class CanonSelphyPrinter(private val context: Context) {

    companion object {
        private const val TAG = "CanonSelphyPrinter"
        private const val IPP_MIME = "application/ipp"

        // IPP Tag constants
        private const val TAG_OPERATION_ATTRIBUTES: Byte = 0x01
        private const val TAG_JOB_ATTRIBUTES: Byte = 0x02
        private const val TAG_END_OF_ATTRIBUTES: Byte = 0x03

        // IPP Value tags
        private const val TAG_INTEGER: Byte = 0x21
        private const val TAG_ENUM: Byte = 0x23
        private const val TAG_NAME_WITHOUT_LANGUAGE: Byte = 0x42
        private const val TAG_KEYWORD: Byte = 0x44
        private const val TAG_URI: Byte = 0x45
        private const val TAG_CHARSET: Byte = 0x47
        private const val TAG_NATURAL_LANGUAGE: Byte = 0x48
        private const val TAG_MIME_MEDIA_TYPE: Byte = 0x49

        // IPP Operations
        private const val OP_PRINT_JOB: Short = 0x0002
        private const val OP_GET_PRINTER_ATTRIBUTES: Short = 0x000B

        // IPP Status
        private const val STATUS_OK: Short = 0x0000
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Checks if the printer is currently idle, processing a job, or stopped (error).
     */
    suspend fun getPrinterStatus(ip: String, port: Int = 631): PrinterStatusResult =
        withContext(Dispatchers.IO) {
            try {
                val requestBytes = buildGetPrinterAttributesRequest(ip, port)
                val url = "http://$ip:$port/ipp/print"

                val request = Request.Builder()
                    .url(url)
                    .post(requestBytes.toRequestBody(IPP_MIME.toMediaType()))
                    .header("User-Agent", "Photobooth-Android")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext PrinterStatusResult(
                        PrinterState.OFFLINE,
                        "HTTP ${response.code}: Impossibile contattare la stampante"
                    )
                }

                val responseBytes = response.body?.bytes()
                    ?: return@withContext PrinterStatusResult(PrinterState.OFFLINE, "Risposta vuota")

                val status = parsePrinterAttributesResponse(responseBytes)
                return@withContext status
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get printer status from $ip:$port", e)
                return@withContext PrinterStatusResult(
                    PrinterState.OFFLINE,
                    "Offline o non raggiungibile (${e.localizedMessage ?: "timeout"})"
                )
            }
        }

    /**
     * Sends a direct IPP print job to the Canon SELPHY printer.
     */
    suspend fun printDirect(
        ip: String,
        port: Int = 631,
        jpegBytes: ByteArray,
        jobName: String = "Photobooth Double Strip"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val ippHeaderAndAttrs = buildPrintJobRequest(ip, port, jobName)
            val fullPayload = ByteArray(ippHeaderAndAttrs.size + jpegBytes.size)
            System.arraycopy(ippHeaderAndAttrs, 0, fullPayload, 0, ippHeaderAndAttrs.size)
            System.arraycopy(jpegBytes, 0, fullPayload, ippHeaderAndAttrs.size, jpegBytes.size)

            val url = "http://$ip:$port/ipp/print"
            val request = Request.Builder()
                .url(url)
                .post(fullPayload.toRequestBody(IPP_MIME.toMediaType()))
                .header("User-Agent", "Photobooth-Android")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Errore stampante HTTP ${response.code}")
                )
            }

            val bodyBytes = response.body?.bytes() ?: byteArrayOf()
            if (bodyBytes.size >= 4) {
                val statusCode = ByteBuffer.wrap(bodyBytes, 2, 2).short
                if (statusCode != STATUS_OK && statusCode.toInt() != 0x0001) { // 0x0001 is ok-attribute-or-value-substituted
                    return@withContext Result.failure(
                        Exception("Errore IPP stampante: 0x${Integer.toHexString(statusCode.toInt())}")
                    )
                }
            }

            Log.i(TAG, "Stampa inviata con successo alla Canon SELPHY CP1500 ($ip:$port)")
            return@withContext Result.success("Stampa inviata con successo!")
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante l'invio della stampa", e)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Polls the printer status until it returns to IDLE or timeout is reached.
     * Crucial to avoid overlapping prints on the Canon SELPHY CP1500!
     */
    suspend fun waitForPrintCompletion(
        ip: String,
        port: Int = 631,
        maxWaitSeconds: Int = 50,
        onProgressUpdate: (remainingSeconds: Int, status: String) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        var elapsed = 0
        var hasSeenProcessing = false

        while (elapsed < maxWaitSeconds) {
            val remaining = maxWaitSeconds - elapsed
            val statusResult = getPrinterStatus(ip, port)

            when (statusResult.state) {
                PrinterState.PROCESSING -> {
                    hasSeenProcessing = true
                    onProgressUpdate(remaining, "La Canon SELPHY sta stampando...")
                }
                PrinterState.IDLE -> {
                    // If we saw it processing and now it is idle, it is done!
                    if (hasSeenProcessing || elapsed >= 30) {
                        onProgressUpdate(0, "Stampa completata!")
                        return@withContext true
                    } else {
                        onProgressUpdate(remaining, "Preparazione foglio...")
                    }
                }
                PrinterState.STOPPED -> {
                    onProgressUpdate(remaining, "Attenzione: stampante in pausa o carta/inchiostro esauriti!")
                }
                PrinterState.OFFLINE, PrinterState.UNKNOWN -> {
                    // Fallback to countdown if IPP polling fails
                    onProgressUpdate(remaining, "Stampa in corso... (${remaining}s)")
                }
            }

            delay(2000)
            elapsed += 2
        }

        // Timeout reached, assume print finished
        return@withContext true
    }

    /**
     * Fallback standard Android Print Service dialog
     */
    fun printViaAndroidPrintManager(bitmap: Bitmap, jobName: String = "Photobooth Print") {
        val printHelper = PrintHelper(context).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT
            colorMode = PrintHelper.COLOR_MODE_COLOR
        }
        printHelper.printBitmap(jobName, bitmap)
    }

    // --- IPP Protocol Encoders ---

    private fun buildGetPrinterAttributesRequest(ip: String, port: Int): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Version: IPP 2.0 (0x0200)
        dos.writeShort(0x0200)
        // Operation: Get-Printer-Attributes (0x000B)
        dos.writeShort(OP_GET_PRINTER_ATTRIBUTES.toInt())
        // Request ID: 1
        dos.writeInt(1)

        // Operation attributes group tag
        dos.writeByte(TAG_OPERATION_ATTRIBUTES.toInt())

        writeAttribute(dos, TAG_CHARSET, "attributes-charset", "utf-8")
        writeAttribute(dos, TAG_NATURAL_LANGUAGE, "attributes-natural-language", "en")
        writeAttribute(dos, TAG_URI, "printer-uri", "ipp://$ip:$port/ipp/print")

        // End of attributes
        dos.writeByte(TAG_END_OF_ATTRIBUTES.toInt())
        dos.flush()
        return baos.toByteArray()
    }

    private fun buildPrintJobRequest(ip: String, port: Int, jobName: String): ByteArray {
        val baos = ByteArrayOutputStream()
        val dos = DataOutputStream(baos)

        // Version: IPP 2.0 (0x0200)
        dos.writeShort(0x0200)
        // Operation: Print-Job (0x0002)
        dos.writeShort(OP_PRINT_JOB.toInt())
        // Request ID: 2
        dos.writeInt(2)

        // Operation attributes
        dos.writeByte(TAG_OPERATION_ATTRIBUTES.toInt())
        writeAttribute(dos, TAG_CHARSET, "attributes-charset", "utf-8")
        writeAttribute(dos, TAG_NATURAL_LANGUAGE, "attributes-natural-language", "en")
        writeAttribute(dos, TAG_URI, "printer-uri", "ipp://$ip:$port/ipp/print")
        writeAttribute(dos, TAG_NAME_WITHOUT_LANGUAGE, "requesting-user-name", "Photobooth")
        writeAttribute(dos, TAG_NAME_WITHOUT_LANGUAGE, "job-name", jobName)
        writeAttribute(dos, TAG_MIME_MEDIA_TYPE, "document-format", "image/jpeg")

        // Job attributes
        dos.writeByte(TAG_JOB_ATTRIBUTES.toInt())
        // Canon SELPHY 4x6 / Postcard media name in IPP
        writeAttribute(dos, TAG_KEYWORD, "media", "na_index-4x6_4x6in")

        // End of attributes
        dos.writeByte(TAG_END_OF_ATTRIBUTES.toInt())
        dos.flush()
        return baos.toByteArray()
    }

    private fun writeAttribute(dos: DataOutputStream, tag: Byte, name: String, value: String) {
        dos.writeByte(tag.toInt())
        val nameBytes = name.toByteArray(Charsets.UTF_8)
        dos.writeShort(nameBytes.size)
        dos.write(nameBytes)
        val valueBytes = value.toByteArray(Charsets.UTF_8)
        dos.writeShort(valueBytes.size)
        dos.write(valueBytes)
    }

    private fun parsePrinterAttributesResponse(bytes: ByteArray): PrinterStatusResult {
        try {
            if (bytes.size < 8) {
                return PrinterStatusResult(PrinterState.UNKNOWN, "Risposta troppo corta")
            }
            val buffer = ByteBuffer.wrap(bytes)
            val version = buffer.short
            val statusCode = buffer.short
            val requestId = buffer.int

            var printerState: PrinterState = PrinterState.UNKNOWN
            var stateReason: String = ""

            while (buffer.hasRemaining()) {
                val tag = buffer.get()
                if (tag == TAG_END_OF_ATTRIBUTES) break
                if (tag.toInt() in 0x01..0x0F) {
                    // Group delimiter tag
                    continue
                }

                // Attribute
                if (buffer.remaining() < 2) break
                val nameLen = buffer.short.toInt()
                val name = if (nameLen > 0 && buffer.remaining() >= nameLen) {
                    val nameBytes = ByteArray(nameLen)
                    buffer.get(nameBytes)
                    String(nameBytes, Charsets.UTF_8)
                } else ""

                if (buffer.remaining() < 2) break
                val valLen = buffer.short.toInt()
                if (valLen > 0 && buffer.remaining() >= valLen) {
                    val valBytes = ByteArray(valLen)
                    buffer.get(valBytes)

                    if (name == "printer-state" && tag == TAG_ENUM && valLen == 4) {
                        val stateVal = ByteBuffer.wrap(valBytes).int
                        printerState = when (stateVal) {
                            3 -> PrinterState.IDLE
                            4 -> PrinterState.PROCESSING
                            5 -> PrinterState.STOPPED
                            else -> PrinterState.UNKNOWN
                        }
                    } else if (name == "printer-state-reasons") {
                        stateReason = String(valBytes, Charsets.UTF_8)
                    }
                }
            }

            val desc = when (printerState) {
                PrinterState.IDLE -> "Pronta per stampare"
                PrinterState.PROCESSING -> "Stampa in corso..."
                PrinterState.STOPPED -> "Bloccata (Controllare carta o nastro: $stateReason)"
                PrinterState.OFFLINE -> "Non in linea"
                PrinterState.UNKNOWN -> "Stato stampante sconosciuto"
            }
            return PrinterStatusResult(printerState, desc)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing IPP response", e)
            return PrinterStatusResult(PrinterState.UNKNOWN, "Errore analisi risposta: ${e.message}")
        }
    }
}
