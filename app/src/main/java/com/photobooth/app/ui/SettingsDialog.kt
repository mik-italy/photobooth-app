package com.photobooth.app.ui

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.photobooth.app.data.SettingsData
import com.photobooth.app.printer.CanonSelphyPrinter
import com.photobooth.app.printer.PrinterState
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    currentSettings: SettingsData,
    printer: CanonSelphyPrinter,
    onSave: (SettingsData) -> Unit,
    onDismiss: () -> Unit
) {
    var isUnlocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Settings fields
    var printerIp by remember { mutableStateOf(currentSettings.printerIp) }
    var printerPort by remember { mutableStateOf(currentSettings.printerPort.toString()) }
    var eventTitle by remember { mutableStateOf(currentSettings.eventTitle) }
    var eventSubtitle by remember { mutableStateOf(currentSettings.eventSubtitle) }
    var eventHashtag by remember { mutableStateOf(currentSettings.eventHashtag) }
    var stripBgColor by remember { mutableStateOf(currentSettings.stripBackgroundColor) }
    var stripTextColor by remember { mutableStateOf(currentSettings.stripTextColor) }
    var countdownSec by remember { mutableStateOf(currentSettings.countdownSeconds.toString()) }
    var waitForPrinter by remember { mutableStateOf(currentSettings.waitForPrinterFinish) }
    var timeoutSec by remember { mutableStateOf(currentSettings.printTimeoutSeconds.toString()) }

    // Test connection state
    val scope = rememberCoroutineScope()
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResultText by remember { mutableStateOf<String?>(null) }
    var testResultColor by remember { mutableStateOf(Color.Gray) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            if (!isUnlocked) {
                // PIN Screen
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Accesso Impostazioni Admin",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Inserisci il PIN per modificare la configurazione",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            enteredPin = it
                            pinError = false
                        },
                        label = { Text("PIN (default: 1234)", color = Color.White.copy(alpha = 0.7f)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinError,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2C2C34),
                            unfocusedContainerColor = Color(0xFF2C2C34)
                        ),
                        singleLine = true
                    )

                    if (pinError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "PIN errato!",
                            color = Color(0xFFF44336),
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

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
                                if (enteredPin == currentSettings.adminPin) {
                                    isUnlocked = true
                                } else {
                                    pinError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                        ) {
                            Text("Sblocca")
                        }
                    }
                }
            } else {
                // Settings Form
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
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
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Chiudi",
                                tint = Color.White
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.White.copy(alpha = 0.1f))

                    // SECTION 1: Strip Customization (Duplicated on both strips)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PERSONALIZZAZIONE STRISCIA 2X6 (DUPLICATA AUTOMATICAMENTE)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Configura la grafica per una striscia: verrà clonata identica nella seconda riga sul foglio 10x15.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { eventTitle = it },
                        label = { Text("Titolo Evento (es. MARCO & SARA)") },
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
                        label = { Text("Data / Luogo (es. 15 LUGLIO 2026)") },
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
                        value = eventHashtag,
                        onValueChange = { eventHashtag = it },
                        label = { Text("Separatore / Hashtag (es. #NoiDue o ✦ ✦ ✦)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2C2C34),
                            unfocusedContainerColor = Color(0xFF2C2C34)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Background color presets for strip
                    Text("Colore Sfondo Striscia:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val bgOptions = listOf(
                            "#FFFFFF" to "Bianco",
                            "#181818" to "Nero",
                            "#FDFBF7" to "Crema",
                            "#FCE4EC" to "Rosa"
                        )
                        bgOptions.forEach { (hex, label) ->
                            val isSelected = stripBgColor.equals(hex, ignoreCase = true)
                            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.White }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color(0xFFE91E63) else Color.Gray,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            stripBgColor = hex
                                            // Automatically adjust text color for contrast
                                            if (hex == "#181818") {
                                                stripTextColor = "#FFFFFF"
                                            } else {
                                                stripTextColor = "#181818"
                                            }
                                        }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.White.copy(alpha = 0.1f))

                    // SECTION 2: Canon SELPHY Printer
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
                    Spacer(modifier = Modifier.height(12.dp))

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

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 3: Printing Queue Lock
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

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = timeoutSec,
                        onValueChange = { timeoutSec = it },
                        label = { Text("Timeout stimato stampa (secondi - default 45s)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2C2C34),
                            unfocusedContainerColor = Color(0xFF2C2C34)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = countdownSec,
                        onValueChange = { countdownSec = it },
                        label = { Text("Secondi Countdown Scatto (3 o 5)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2C2C34),
                            unfocusedContainerColor = Color(0xFF2C2C34)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Save / Cancel Buttons
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
                                val updated = currentSettings.copy(
                                    printerIp = printerIp.trim(),
                                    printerPort = printerPort.toIntOrNull() ?: 631,
                                    eventTitle = eventTitle.trim(),
                                    eventSubtitle = eventSubtitle.trim(),
                                    eventHashtag = eventHashtag.trim(),
                                    stripBackgroundColor = stripBgColor,
                                    stripTextColor = stripTextColor,
                                    countdownSeconds = countdownSec.toIntOrNull() ?: 3,
                                    waitForPrinterFinish = waitForPrinter,
                                    printTimeoutSeconds = timeoutSec.toIntOrNull() ?: 45
                                )
                                onSave(updated)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
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
}
