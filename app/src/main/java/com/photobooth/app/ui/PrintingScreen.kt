package com.photobooth.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrintingScreen(
    remainingSeconds: Int,
    totalTimeoutSeconds: Int = 45,
    statusMessage: String,
    isCompleted: Boolean,
    onCancel: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val progress = if (totalTimeoutSeconds > 0) {
        ((totalTimeoutSeconds - remainingSeconds).toFloat() / totalTimeoutSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val isOfflineOrError = statusMessage.contains("offline", ignoreCase = true) ||
            statusMessage.contains("errore", ignoreCase = true) ||
            statusMessage.contains("non in linea", ignoreCase = true) ||
            statusMessage.contains("bloccata", ignoreCase = true) ||
            statusMessage.contains("impossibile", ignoreCase = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            if (!isCompleted) {
                // Indicatore circolare minimale monocromatico
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(130.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(126.dp),
                        color = Color.White,
                        trackColor = Color(0xFF222222),
                        strokeWidth = 6.dp
                    )
                    Icon(
                        imageVector = if (isOfflineOrError) Icons.Default.WarningAmber else Icons.Default.Print,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(50.dp)
                            .scale(pulseScale)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = if (isOfflineOrError) "STAMPANTE NON DISPONIBILE" else "STAMPA IN CORSO...",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = statusMessage.ifEmpty { "Invio alla Canon SELPHY CP1500..." },
                    fontSize = 17.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sublimazione termica Canon SELPHY (circa 45 sec per foglio)",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Passaggi monocromatici discreti (Giallo, Magenta, Ciano, Finitura)
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val passIndex = (progress * 4).toInt().coerceIn(0, 3)
                    val passes = listOf("Giallo", "Magenta", "Ciano", "Lucido")

                    passes.forEachIndexed { index, label ->
                        val isActive = index <= passIndex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (index == passIndex) 16.dp else 10.dp)
                                    .background(
                                        color = if (isActive) Color.White else Color(0xFF333333),
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = if (isActive) Color.White else Color.White.copy(alpha = 0.3f),
                                fontWeight = if (index == passIndex) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Progress Bar monocromatica
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .width(340.dp)
                        .height(6.dp),
                    color = Color.White,
                    trackColor = Color(0xFF222222)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Tempo residuo: ${remainingSeconds}s",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.45f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // PULSANTE PER USCIRE SUBITO DALLA SCHERMATA DEI 45" SE OFFLINE O PER ANNULLARE
                if (isOfflineOrError) {
                    Button(
                        onClick = onCancel,
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .height(52.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "STAMPANTE OFFLINE: TORNA ALLA HOME",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFF333333)),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Annulla ed Esci",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                    }
                }

            } else {
                // Completed State (Minimale Bianco & Nero)
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(animationSpec = tween(400)) + fadeIn()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Successo",
                            tint = Color.White,
                            modifier = Modifier.size(90.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "STAMPA COMPLETATA",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Ritira la tua foto dalla stampante Canon SELPHY",
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Ritorno automatico alla schermata iniziale...",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
    }
}
