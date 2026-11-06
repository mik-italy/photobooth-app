package com.photobooth.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photobooth.app.R
import com.photobooth.app.data.FontChoice
import com.photobooth.app.data.LayoutMode
import com.photobooth.app.data.PrinterType
import com.photobooth.app.data.SettingsData
import com.photobooth.app.printer.PrinterState

@Composable
fun IdleScreen(
    settings: SettingsData,
    printerState: PrinterState,
    onStartSession: () -> Unit,
    onToggleLayoutMode: (LayoutMode) -> Unit,
    onOpenSettings: () -> Unit
) {
    val titleFont = remember(settings.fontChoice) {
        try {
            when (settings.fontChoice) {
                FontChoice.PLAYFAIR -> FontFamily(Font(R.font.playfair_display_bold_italic, FontWeight.Bold, FontStyle.Italic))
                FontChoice.MONTSERRAT -> FontFamily(Font(R.font.montserrat_bold, FontWeight.Bold))
                FontChoice.DANCING_SCRIPT -> FontFamily(Font(R.font.dancingscript_bold, FontWeight.Bold))
                FontChoice.CINZEL -> FontFamily(Font(R.font.cinzel_bold, FontWeight.Bold))
            }
        } catch (_: Exception) {
            FontFamily.Serif
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onStartSession()
            }
    ) {
        // Top Printer Status Indicator Badge (Minimale monocromatico)
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFF141414),
            border = BorderStroke(1.dp, Color(0xFF262626))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isDnp = settings.printerType == PrinterType.DNP_DS40_USB
                val printerName = if (isDnp) "DNP DS40 (USB)" else "Canon SELPHY"

                val (dotColor, statusMsg) = when (printerState) {
                    PrinterState.IDLE -> Pair(Color.White, "Pronta")
                    PrinterState.PROCESSING -> Pair(Color(0xFFDDDDDD), "In Stampa...")
                    PrinterState.STOPPED -> Pair(Color(0xFF888888), if (isDnp) "Attenzione / Carta" else "Attenzione / Carta")
                    PrinterState.OFFLINE -> Pair(Color(0xFF555555), if (isDnp) "Disconnessa (USB)" else "Offline")
                    PrinterState.UNKNOWN -> Pair(Color(0xFF666666), "Connessione...")
                }

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(dotColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = if (isDnp) Icons.Default.Usb else Icons.Default.Print,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$printerName: $statusMsg",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Settings button (Icona monocromatica, protetta da password 3552)
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.size(32.dp)
            )
        }

        // Contenuto Centrale Minimale Monocromatico
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Titolo Evento (Sempre visibile in home, strictly su una riga sola)
            if (settings.eventTitle.isNotBlank()) {
                Text(
                    text = settings.eventTitle,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = titleFont,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(44.dp))
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Pulsante Inizia (Minimale Bianco & Nero ad alto impatto)
            Button(
                onClick = onStartSession,
                modifier = Modifier
                    .scale(pulseScale)
                    .height(92.dp)
                    .width(460.dp),
                shape = RoundedCornerShape(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "TOCCA PER INIZIARE",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Color.Black
                    )
                }
            }
        }

        // Selettore Formato Inferiore: "4 FOTO", "3 FOTO", "2 FOTO" e "1 FOTO" (Minimale Monocromatico)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFF141414),
            border = BorderStroke(1.dp, Color(0xFF262626))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    LayoutMode.STRIP_4_PHOTOS to "4 FOTO",
                    LayoutMode.STRIP_3_PHOTOS to "3 FOTO",
                    LayoutMode.STRIP_2_PHOTOS to "2 FOTO",
                    LayoutMode.SINGLE_PHOTO to "1 FOTO"
                ).forEach { (mode, label) ->
                    val isSelected = settings.layoutMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleLayoutMode(mode) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.White,
                            selectedLabelColor = Color.Black,
                            containerColor = Color.Transparent,
                            labelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        border = null,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.height(44.dp)
                    )
                }
            }
        }
    }
}
