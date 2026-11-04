package com.photobooth.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photobooth.app.printer.PrinterState
import kotlinx.coroutines.delay

@Composable
fun PreviewScreen(
    composedBitmap: Bitmap,
    printerState: PrinterState,
    onPrintClicked: (copies: Int) -> Unit,
    onRetakeClicked: () -> Unit,
    onHomeClicked: () -> Unit,
    onTimeout: () -> Unit
) {
    var remainingInactivitySeconds by remember { mutableIntStateOf(60) }
    var copiesCount by remember { mutableIntStateOf(1) } // Scelta numero copie

    LaunchedEffect(Unit) {
        while (remainingInactivitySeconds > 0) {
            delay(1000)
            remainingInactivitySeconds--
        }
        onTimeout()
    }

    val isPrinterBusy = printerState == PrinterState.PROCESSING
    val bitmapAspectRatio = composedBitmap.width.toFloat() / composedBitmap.height.toFloat()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121216))
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Sheet Preview
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight(0.92f)
                    .aspectRatio(bitmapAspectRatio)
                    .shadow(16.dp, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp),
                color = Color.White
            ) {
                Image(
                    bitmap = composedBitmap.asImageBitmap(),
                    contentDescription = "Anteprima Stampa Photobooth",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Right Column: Controls, Copies selector & Actions
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ecco le tue foto!",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Scegli quante copie stampare con la Canon SELPHY.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // SELETTORE NUMERO COPIE DA STAMPARE
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0x33FFFFFF),
                modifier = Modifier.padding(bottom = 18.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Copie da stampare:",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    FilledIconButton(
                        onClick = { if (copiesCount > 1) copiesCount-- },
                        enabled = copiesCount > 1,
                        modifier = Modifier.size(38.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF3F51B5))
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Meno copie", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "$copiesCount",
                        color = Color(0xFFFFD700),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    FilledIconButton(
                        onClick = { if (copiesCount < 6) copiesCount++ },
                        enabled = copiesCount < 6,
                        modifier = Modifier.size(38.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF3F51B5))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Più copie", tint = Color.White)
                    }
                }
            }

            // Printer Busy Warning Banner if active
            if (isPrinterBusy) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x33FFC107),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFFFFC107),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Stampante occupata. Attendere...",
                            color = Color(0xFFFFE082),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Print Button (Displays copies number dynamically)
            Button(
                onClick = { onPrintClicked(copiesCount) },
                enabled = !isPrinterBusy,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(68.dp),
                shape = RoundedCornerShape(34.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE91E63),
                    disabledContainerColor = Color(0xFF616161)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isPrinterBusy) "STAMPANTE OCCUPATA..." else "STAMPA $copiesCount ${if (copiesCount == 1) "COPIA" else "COPIE"}",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Retake Button
            OutlinedButton(
                onClick = onRetakeClicked,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rifai gli scatti",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Return to Home Button
            OutlinedButton(
                onClick = onHomeClicked,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(alpha = 0.8f)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Torna alla Home",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Ritorno automatico alla schermata iniziale tra ${remainingInactivitySeconds}s",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }
    }
}
