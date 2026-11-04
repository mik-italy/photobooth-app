package com.photobooth.app.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.photobooth.app.data.BgScaleMode
import com.photobooth.app.data.LayoutMode
import com.photobooth.app.data.SettingsData
import com.photobooth.app.printer.CanonSelphyPrinter
import com.photobooth.app.printer.PrinterState
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun SettingsDialog(
    currentSettings: SettingsData,
    printer: CanonSelphyPrinter,
    onSave: (SettingsData) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Settings fields
    var printerIp by remember { mutableStateOf(currentSettings.printerIp) }
    var printerPort by remember { mutableStateOf(currentSettings.printerPort.toString()) }
    var eventTitle by remember { mutableStateOf(currentSettings.eventTitle) }
    var eventSubtitle by remember { mutableStateOf(currentSettings.eventSubtitle) }
    var layoutMode by remember { mutableStateOf(currentSettings.layoutMode) }
    var stripBgColor by remember { mutableStateOf(currentSettings.stripBackgroundColor) }
    var stripTextColor by remember { mutableStateOf(currentSettings.stripTextColor) }
    var titleFontSize by remember { mutableStateOf(currentSettings.titleFontSize.toString()) }
    var customBgPath by remember { mutableStateOf(currentSettings.customBgImagePath) }
    var bgScaleMode by remember { mutableStateOf(currentSettings.bgScaleMode) }
    var countdownSec by remember { mutableStateOf(currentSettings.countdownSeconds.toString()) }
    var waitForPrinter by remember { mutableStateOf(currentSettings.waitForPrinterFinish) }
    var timeoutSec by remember { mutableStateOf(currentSettings.printTimeoutSeconds.toString()) }

    // Test connection state
    val scope = rememberCoroutineScope()
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var testResultColor by remember { mutableStateOf(Color.Gray) }

    // Image Picker for custom background
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val targetFile = File(context.filesDir, "custom_bg_${System.currentTimeMillis()}.png")
                val outputStream = FileOutputStream(targetFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                customBgPath = targetFile.absolutePath
                Toast.makeText(context, "Immagine di sfondo caricata!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Errore caricamento: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun performSave() {
        keyboardController?.hide()
        val updated = currentSettings.copy(
            printerIp = printerIp.trim(),
            printerPort = printerPort.toIntOrNull() ?: 631,
            eventTitle = eventTitle.trim(),
            eventSubtitle = eventSubtitle.trim(),
            layoutMode = layoutMode,
            stripBackgroundColor = stripBgColor,
            stripTextColor = stripTextColor,
            titleFontSize = titleFontSize.toIntOrNull() ?: 46,
            customBgImagePath = customBgPath,
            bgScaleMode = bgScaleMode,
            countdownSeconds = countdownSec.toIntOrNull() ?: 5,
            waitForPrinterFinish = waitForPrinter,
            printTimeoutSeconds = timeoutSec.toIntOrNull() ?: 45
        )
        onSave(updated)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // TOP HEADER: Title + SALVA BUTTON IN ALTO + Chiudi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Impostazioni Photobooth",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { performSave() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SALVA", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi",
                                tint = Color.White
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.15f))

                // SECTION 1: Modalità Layout
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhotoAlbum, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FORMATO DI STAMPA PREDEFINITO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = layoutMode == LayoutMode.STRIP_4_PHOTOS,
                        onClick = { layoutMode = LayoutMode.STRIP_4_PHOTOS },
                        label = { Text("Doppia Striscia 2x6 (4 Foto)", fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE91E63),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = layoutMode == LayoutMode.SINGLE_PHOTO,
                        onClick = { layoutMode = LayoutMode.SINGLE_PHOTO },
                        label = { Text("Foto Unica Orizzontale 10x15 (1 Foto)", fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE91E63),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.1f))

                // SECTION 2: Testi, Font Playfair Bold Italic, Dimensioni e Colori
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TESTI, FONT E COLORI",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = eventTitle,
                    onValueChange = { eventTitle = it },
                    label = { Text("Titolo Evento (Playfair Bold Italic - maiuscole/minuscole libere)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF2C2C34),
                        unfocusedContainerColor = Color(0xFF2C2C34)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = eventSubtitle,
                    onValueChange = { eventSubtitle = it },
                    label = { Text("Data / Luogo (es. 15 Luglio 2026)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF2C2C34),
                        unfocusedContainerColor = Color(0xFF2C2C34)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Dimensione Font Titolo & Contrasto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = titleFontSize,
                        onValueChange = { titleFontSize = it },
                        label = { Text("Dimensione Font Titolo (default 46)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2C2C34),
                            unfocusedContainerColor = Color(0xFF2C2C34)
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("Contrasto Testo:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Viene applicata automaticamente un'ombra a contrasto per garantire la massima leggibilità su qualsiasi foto/sfondo.",
                            color = Color(0xFFFFD700),
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Colore del Testo
                Text("Colore del Testo:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val textColors = listOf(
                        "#181818" to "Nero",
                        "#FFFFFF" to "Bianco",
                        "#D4AF37" to "Oro",
                        "#1A237E" to "Blu Notte",
                        "#E91E63" to "Rosa"
                    )
                    textColors.forEach { (hex, label) ->
                        val isSelected = stripTextColor.equals(hex, ignoreCase = true)
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Black }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF00E676) else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { stripTextColor = hex }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.1f))

                // SECTION 3: Sfondo Immagine & Proporzioni
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AspectRatio, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "IMMAGINE DI SFONDO & PROPORZIONE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") }
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFFFFD700))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (customBgPath != null) "Cambia Immagine Sfondo" else "Carica Immagine Sfondo", color = Color.White)
                    }

                    if (customBgPath != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = {
                                customBgPath = null
                                Toast.makeText(context, "Sfondo personalizzato rimosso", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Rimuovi", tint = Color(0xFFF44336))
                        }
                        Text("Immagine attiva", color = Color(0xFF4CAF50), fontSize = 12.sp)
                    }
                }

                if (customBgPath != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Proporzione / Adattamento Immagine di Sfondo:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = bgScaleMode == BgScaleMode.CENTER_CROP,
                            onClick = { bgScaleMode = BgScaleMode.CENTER_CROP },
                            label = { Text("Ritaglio Centrato (Copre tutto)", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE91E63),
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = bgScaleMode == BgScaleMode.FIT_XY,
                            onClick = { bgScaleMode = BgScaleMode.FIT_XY },
                            label = { Text("Riempi / Fit XY", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE91E63),
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = bgScaleMode == BgScaleMode.FIT_CENTER,
                            onClick = { bgScaleMode = BgScaleMode.FIT_CENTER },
                            label = { Text("Mantieni Proporzioni", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE91E63),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.1f))

                // SECTION 4: Canon SELPHY Printer
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STAMPANTE CANON SELPHY CP1500",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = printerIp,
                        onValueChange = { printerIp = it },
                        label = { Text("Indirizzo IP Stampante") },
                        modifier = Modifier.weight(2f),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2C2C34),
                            unfocusedContainerColor = Color(0xFF2C2C34)
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    OutlinedTextField(
                        value = printerPort,
                        onValueChange = { printerPort = it },
                        label = { Text("Porta IPP") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2C2C34),
                            unfocusedContainerColor = Color(0xFF2C2C34)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                keyboardController?.hide()
                                isTestingConnection = true
                                testResultText = null
                                val port = printerPort.toIntOrNull() ?: 631
                                val res = printer.getPrinterStatus(printerIp.trim(), port)
                                isTestingConnection = false
                                testResultText = res.message
                                testResultColor = when (res.state) {
                                    PrinterState.IDLE -> Color(0xFF4CAF50)
                                    PrinterState.PROCESSING -> Color(0xFFFFC107)
                                    else -> Color(0xFFF44336)
                                }
                            }
                        },
                        enabled = !isTestingConnection
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Testa Connessione", color = Color.White)
                    }

                    if (testResultText != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = testResultText ?: "",
                            color = testResultColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Blocco Stampa
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Attendi fine stampa prima del prossimo scatto",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Blocca l'app finché la SELPHY non finisce per evitare accavallamenti",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = waitForPrinter,
                        onCheckedChange = { waitForPrinter = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFE91E63),
                            checkedTrackColor = Color(0x66E91E63)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = countdownSec,
                    onValueChange = { countdownSec = it },
                    label = { Text("Secondi Countdown Scatto (default 5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF2C2C34),
                        unfocusedContainerColor = Color(0xFF2C2C34)
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Buttons: Chiudi / Salva
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Chiudi", color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { performSave() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salva Impostazioni")
                    }
                }
            }
        }
    }
}
