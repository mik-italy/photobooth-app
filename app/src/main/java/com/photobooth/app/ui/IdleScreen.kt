package com.photobooth.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photobooth.app.R
import com.photobooth.app.data.FontChoice
import com.photobooth.app.data.LayoutMode
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
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0F14),
                        Color(0xFF181622),
                        Color(0xFF0A0A10)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onStartSession()
            }
    ) {
        // Top Printer Status Indicator Badge
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
            shape = RoundedCornerShape(30.dp),
            color = Color(0x33000000)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (color, text) = when (printerState) {
                    PrinterState.IDLE -> Pair(Color(0xFF4CAF50), "Canon SELPHY: Pronta")
                    PrinterState.PROCESSING -> Pair(Color(0xFFFFC107), "Canon SELPHY: Stampa in corso...")
                    PrinterState.STOPPED -> Pair(Color(0xFFF44336), "Canon SELPHY: Errore / Carta")
                    PrinterState.OFFLINE -> Pair(Color(0xFFFF5722), "Canon SELPHY: Offline (${settings.printerIp})")
                    PrinterState.UNKNOWN -> Pair(Color.Gray, "Canon SELPHY: In attesa")
                }

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = text,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Settings button (Protetta da password 3552)
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(34.dp)
            )
        }

        // Center Content (Titolo ancora più grande e pulsante ingrandito)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (settings.showTitleText && settings.eventTitle.isNotBlank()) {
                Text(
                    text = settings.eventTitle,
                    fontSize = 72.sp, // ANCORA PIÙ GRANDE
                    fontWeight = FontWeight.Bold,
                    fontFamily = titleFont,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(48.dp))
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Main Start Button (ANCORA PIÙ GRANDE)
            Button(
                onClick = onStartSession,
                modifier = Modifier
                    .scale(pulseScale)
                    .height(96.dp) // Ingrandito
                    .width(460.dp), // Ingrandito
                shape = RoundedCornerShape(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE91E63)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "TOCCA PER INIZIARE",
                        fontSize = 28.sp, // Testo ingrandito
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )
                }
            }
        }

        // BOTTOM SELECTOR: "4 FOTO" e "1 FOTO" (Senza emoji)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp),
            shape = RoundedCornerShape(30.dp),
            color = Color(0x661E1E2E)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = settings.layoutMode == LayoutMode.STRIP_4_PHOTOS,
                    onClick = { onToggleLayoutMode(LayoutMode.STRIP_4_PHOTOS) },
                    label = { Text("4 FOTO", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF3F51B5),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0x22FFFFFF),
                        labelColor = Color.White.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(44.dp)
                )

                FilterChip(
                    selected = settings.layoutMode == LayoutMode.SINGLE_PHOTO,
                    onClick = { onToggleLayoutMode(LayoutMode.SINGLE_PHOTO) },
                    label = { Text("1 FOTO", fontSize = 15.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF3F51B5),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0x22FFFFFF),
                        labelColor = Color.White.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(44.dp)
                )
            }
        }
    }
}
