package com.photobooth.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class DistractionMessage(
    val title: String,
    val subtitle: String,
    val hint: String
)

private val distractionMessages = listOf(
    DistractionMessage(
        title = "Preparatevi per la prossima foto!",
        subtitle = "Scegliete i gadget e mettetevi in posa!",
        hint = "🕶️ Occhiali  •  🎩 Cappelli  •  ✨ Sorrisi pronti"
    ),
    DistractionMessage(
        title = "Cercate il vostro look perfetto!",
        subtitle = "Provate parrucche, baffi e accessori stravaganti!",
        hint = "🎭 Maschere  •  🥸 Baffi finti  •  👑 Corone"
    ),
    DistractionMessage(
        title = "Chi farà la posa più divertente?",
        subtitle = "Sfida i tuoi amici: bacio, linguaccia o sorriso da star!",
        hint = "✌️ Segno vittoria  •  😘 Bacio  •  ⭐ Posa da star"
    ),
    DistractionMessage(
        title = "La stampante sta sfornando il ricordo!",
        subtitle = "Tra pochissimo la vostra stampa sarà pronta da ritirare.",
        hint = "📸 Carta fotografica  •  ✨ Finitura lucida"
    ),
    DistractionMessage(
        title = "Tutti pronti per il prossimo giro?",
        subtitle = "Raggruppatevi davanti all'obiettivo, si ricomincia!",
        hint = "🎉 Divertimento  •  🥳 Festa  •  💃 In posa"
    )
)

@Composable
fun PrintingScreen(
    remainingSeconds: Int,
    totalTimeoutSeconds: Int = 40,
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

    // Calcolo indice messaggio che scorre ogni 7 secondi
    val currentMessageIndex = remember(remainingSeconds, totalTimeoutSeconds) {
        val elapsed = (totalTimeoutSeconds - remainingSeconds).coerceAtLeast(0)
        (elapsed / 7) % distractionMessages.size
    }

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
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            if (!isCompleted) {
                // Badge Superiore: "STAMPA IN CORSO"
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF141414),
                    border = BorderStroke(1.dp, Color(0xFF262626)),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOfflineOrError) Icons.Default.WarningAmber else Icons.Default.Print,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(18.dp)
                                .scale(pulseScale)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isOfflineOrError) "ATTENZIONE STAMPANTE" else "STAMPA IN CORSO...",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // TESTI ANIMATI CHE SCORRONO PER DISTRARRE E INTRATTENERE
                AnimatedContent(
                    targetState = currentMessageIndex,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(500)) + slideInVertically { it / 3 })
                            .togetherWith(fadeOut(animationSpec = tween(350)) + slideOutVertically { -it / 3 })
                    },
                    label = "distractionAnimation"
                ) { index ->
                    val msg = distractionMessages[index]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = msg.title,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = msg.subtitle,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF181818),
                            border = BorderStroke(1.dp, Color(0xFF2A2A2A))
                        ) {
                            Text(
                                text = msg.hint,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Progress Bar Monocromatica
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .width(420.dp)
                        .height(8.dp),
                    color = Color.White,
                    trackColor = Color(0xFF222222)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isOfflineOrError) {
                        "Stampante non in linea (${statusMessage})"
                    } else {
                        "La foto sarà pronta tra circa ${remainingSeconds}s"
                    },
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Azione: Salta Attesa / Nuovo Scatto
                if (isOfflineOrError) {
                    Button(
                        onClick = onCancel,
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.height(52.dp)
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
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nuova Foto / Salta Attesa",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
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
                            text = "FOTO STAMPATA!",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Ritira la stampa dalla stampante Canon SELPHY!",
                            fontSize = 19.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Pronti per il prossimo scatto...",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
    }
}
