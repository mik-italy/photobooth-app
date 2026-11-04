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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PauseCircle
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import com.photobooth.app.data.FontChoice
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

    // Password / PIN check (Nuova password: 3552)
    var isUnlocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    val pinFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isUnlocked) {
        if (!isUnlocked) {
            delay(250)
            pinFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Settings fields
    var printerIp by remember { mutableStateOf(currentSettings.printerIp) }
    var printerPort by remember { mutableStateOf(currentSettings.printerPort.toString()) }
    var eventTitle by remember { mutableStateOf(currentSettings.eventTitle) }
    var showTitleOnPrints by remember { mutableStateOf(currentSettings.showTitleOnPrints) }
    var fontChoice by remember { mutableStateOf(currentSettings.fontChoice) }
    var layoutMode by remember { mutableStateOf(currentSettings.layoutMode) }
    var stripBgColor by remember { mutableStateOf(currentSettings.stripBackgroundColor) }
    var stripTextColor by remember { mutableStateOf(currentSettings.stripTextColor) }
    var titleFontSize by remember { mutableStateOf(currentSettings.titleFontSize.toString()) }
    var stripBgPath by remember { mutableStateOf(currentSettings.stripBgImagePath) }
    var singleBgPath by remember { mutableStateOf(currentSettings.singlePhotoBgImagePath) }
    var isPaused by remember { mutableStateOf(currentSettings.isPaused) }
    var countdownSec by remember { mutableStateOf(currentSettings.countdownSeconds.toString()) }
    var waitForPrinter by remember { mutableStateOf(currentSettings.waitForPrinterFinish) }
    var timeoutSec by remember { mutableStateOf(currentSettings.printTimeoutSeconds.toString()) }

    val scope = rememberCoroutineScope()
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var testResultColor by remember { mutableStateOf(Color.Gray) }

    // Palette Colori Sfondo Predefiniti
    val bgPalette = listOf(
        "#FFFFFF" to "Bianco",
        "#000000" to "Nero",
        "#F7F5F0" to "Avorio",
        "#FBEAEB" to "Rosa Cipria",
        "#DDE5B6" to "Salvia",
        "#E6EFF5" to "Azzurro",
        "#1B263B" to "Navy",
        "#3D0C11" to "Borgogna",
        "#D4AF37" to "Oro Chiaro",
        "#2B2D42" to "Antracite"
    )

    // Palette Colori Testo Predefiniti
    val textPalette = listOf(
        "#000000" to "Nero",
        "#FFFFFF" to "Bianco",
        "#D4AF37" to "Oro",
        "#8A795D" to "Bronzo",
        "#1A237E" to "Blu Notte",
        "#B71C1C" to "Bordeaux",
        "#2E7D32" to "Verde",
        "#4A4A4A" to "Grigio",
        "#C5A059" to "Champagne",
        "#D81B60" to "Rosa"
    )

    // Image Picker per Sfondo 4 FOTO (Doppia Striscia A5)
    val stripBgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val targetFile = File(context.filesDir, "strip_bg_${System.currentTimeMillis()}.png")
                val outputStream = FileOutputStream(targetFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                stripBgPath = targetFile.absolutePath
                Toast.makeText(context, "Sfondo 4 FOTO A5 caricato!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Image Picker per Sfondo 1 FOTO (Foto Orizzontale A5)
    val singleBgPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val targetFile = File(context.filesDir, "single_bg_${System.currentTimeMillis()}.png")
                val outputStream = FileOutputStream(targetFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                singleBgPath = targetFile.absolutePath
                Toast.makeText(context, "Sfondo 1 FOTO A5 caricato!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun performSave() {
        keyboardController?.hide()
        val updated = currentSettings.copy(
            printerIp = printerIp.trim(),
            printerPort = printerPort.toIntOrNull() ?: 631,
            eventTitle = eventTitle.trim(),
            showTitleOnPrints = showTitleOnPrints,
            fontChoice = fontChoice,
            layoutMode = layoutMode,
            stripBackgroundColor = stripBgColor.trim(),
            stripTextColor = stripTextColor.trim(),
            titleFontSize = titleFontSize.toIntOrNull() ?: 48,
            stripBgImagePath = stripBgPath,
            singlePhotoBgImagePath = singleBgPath,
            isPaused = isPaused,
            adminPin = "3552",
            countdownSeconds = countdownSec.toIntOrNull() ?: 5,
            waitForPrinterFinish = waitForPrinter,
            printTimeoutSeconds = timeoutSec.toIntOrNull() ?: 40
        )
        onSave(updated)
        onDismiss() // Chiude subito la schermata impostazioni
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181E)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            if (!isUnlocked) {
                // Schermata di richiesta PIN (3552)
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Impostazioni Riservate",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Inserisci il codice di sicurezza per accedere",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            enteredPin = it
                            pinError = false
                        },
                        label = { Text("Password", color = Color.White.copy(alpha = 0.7f)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinError,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF26262E),
                            unfocusedContainerColor = Color(0xFF26262E)
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .focusRequester(pinFocusRequester)
                    )

                    if (pinError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Codice errato!",
                            color = Color(0xFFFF5252),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Annulla", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (enteredPin == "3552") {
                                    isUnlocked = true
                                } else {
                                    pinError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Accedi", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Schermata Pannello Impostazioni
                Column(
                    modifier = Modifier
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // TOP BAR CON PULSANTE SALVA IN EVIDENZA
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
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SALVA", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Color.White)
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.15f))

                    // SECTION 0: STATO PHOTOBOOTH / IN PAUSA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PauseCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "STATO SERVIZIO: IN PAUSA",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Mostra avviso 'Chiama un operatore' e blocca l'uso agli ospiti",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }

                        Switch(
                            checked = isPaused,
                            onCheckedChange = { isPaused = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF555555)
                            )
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.1f))

                    // SECTION 1: Formato Predefinito (Foglio A5)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoAlbum, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "FORMATO DI STAMPA PREDEFINITO (A5 - 148 x 210 mm)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Risoluzione di stampa: 300 DPI (1748 x 2480 px)",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = layoutMode == LayoutMode.STRIP_4_PHOTOS,
                            onClick = { layoutMode = LayoutMode.STRIP_4_PHOTOS },
                            label = { Text("4 FOTO (Doppia Striscia A5)", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.White,
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF2A2A34),
                                labelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                        FilterChip(
                            selected = layoutMode == LayoutMode.SINGLE_PHOTO,
                            onClick = { layoutMode = LayoutMode.SINGLE_PHOTO },
                            label = { Text("1 FOTO (Foto Orizzontale A5)", fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.White,
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF2A2A34),
                                labelColor = Color.White.copy(alpha = 0.7f)
                            )
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.1f))

                    // SECTION 2: Testo Evento, Font e Colore Testo con Palette
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TESTO EVENTO E PERSONALIZZAZIONE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { eventTitle = it },
                        label = { Text("Titolo Evento (sempre visibile sulla Home dell'app)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF26262E),
                            unfocusedContainerColor = Color(0xFF26262E)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // SWITCH: Mostra titolo sulle foto stampate (Vale SOLO per le foto, non per la home!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mostra titolo sulle foto stampate",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Se disattivato, il titolo non viene stampato sulle foto ma rimane in Home",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = showTitleOnPrints,
                            onCheckedChange = { showTitleOnPrints = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF555555)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // SCELTA FONT (3-4 font famosi)
                    Text("Scegli il Font:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val fonts = listOf(
                            FontChoice.PLAYFAIR to "Playfair",
                            FontChoice.MONTSERRAT to "Montserrat",
                            FontChoice.DANCING_SCRIPT to "Dancing",
                            FontChoice.CINZEL to "Cinzel"
                        )
                        fonts.forEach { (choice, label) ->
                            FilterChip(
                                selected = fontChoice == choice,
                                onClick = { fontChoice = choice },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color.White,
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF2A2A34),
                                    labelColor = Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // COLORE TESTO CON PALETTE
                    Text("Colore del Testo:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        textPalette.forEach { (hex, _) ->
                            val parsed = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Black }
                            val isSelected = stripTextColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parsed)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color(0xFF555555),
                                        shape = CircleShape
                                    )
                                    .clickable { stripTextColor = hex }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = stripTextColor,
                            onValueChange = { stripTextColor = it },
                            label = { Text("Colore Testo (#HEX)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF26262E),
                                unfocusedContainerColor = Color(0xFF26262E)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = titleFontSize,
                            onValueChange = { titleFontSize = it },
                            label = { Text("Dimensione Base Font") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF26262E),
                                unfocusedContainerColor = Color(0xFF26262E)
                            )
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.1f))

                    // SECTION 3: SFONDI COLORATI CON PALETTE E IMMAGINI
                    Text(
                        text = "COLORE SFONDO E IMMAGINI (FOGLIO A5)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Palette Colore Sfondo Tinta Unita:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bgPalette.forEach { (hex, _) ->
                            val parsed = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.White }
                            val isSelected = stripBgColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parsed)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color(0xFF555555),
                                        shape = CircleShape
                                    )
                                    .clickable { stripBgColor = hex }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = stripBgColor,
                        onValueChange = { stripBgColor = it },
                        label = { Text("Colore Sfondo (#HEX)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.5f),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF26262E),
                            unfocusedContainerColor = Color(0xFF26262E)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sfondo Immagine: 4 FOTO A5
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Immagine di sfondo per 4 FOTO (A5):", color = Color.White, fontSize = 13.sp)
                            Text(
                                text = if (stripBgPath != null) "✓ Immagine attiva" else "Usa colore tinta unita ($stripBgColor)",
                                color = if (stripBgPath != null) Color(0xFF81C784) else Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                        Row {
                            OutlinedButton(onClick = { stripBgPicker.launch("image/*") }) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (stripBgPath != null) "Cambia" else "Carica", color = Color.White)
                            }
                            if (stripBgPath != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { stripBgPath = null }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Rimuovi", tint = Color(0xFFFF5252))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sfondo Immagine: 1 FOTO A5
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Immagine di sfondo per 1 FOTO (A5):", color = Color.White, fontSize = 13.sp)
                            Text(
                                text = if (singleBgPath != null) "✓ Immagine attiva" else "Usa colore tinta unita ($stripBgColor)",
                                color = if (singleBgPath != null) Color(0xFF81C784) else Color.White.copy(alpha = 0.5f),
                                fontSize = 12.sp
                            )
                        }
                        Row {
                            OutlinedButton(onClick = { singleBgPicker.launch("image/*") }) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (singleBgPath != null) "Cambia" else "Carica", color = Color.White)
                            }
                            if (singleBgPath != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(onClick = { singleBgPath = null }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Rimuovi", tint = Color(0xFFFF5252))
                                }
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 14.dp), color = Color.White.copy(alpha = 0.1f))

                    // SECTION 4: Stampante Canon SELPHY CP1500
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STAMPANTE CANON SELPHY CP1500",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = printerIp,
                            onValueChange = { printerIp = it },
                            label = { Text("Indirizzo IP Stampante") },
                            modifier = Modifier.weight(2f),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF26262E),
                                unfocusedContainerColor = Color(0xFF26262E)
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedTextField(
                            value = printerPort,
                            onValueChange = { printerPort = it },
                            label = { Text("Porta IPP") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF26262E),
                                unfocusedContainerColor = Color(0xFF26262E)
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
                                        PrinterState.IDLE -> Color(0xFF81C784)
                                        PrinterState.PROCESSING -> Color(0xFFFFD54F)
                                        else -> Color(0xFFFF5252)
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
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF555555)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = countdownSec,
                        onValueChange = { countdownSec = it },
                        label = { Text("Secondi Countdown Scatto (default 5)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF26262E),
                            unfocusedContainerColor = Color(0xFF26262E)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // BOTTOM ACTION BAR
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salva Impostazioni", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }
    }
}
