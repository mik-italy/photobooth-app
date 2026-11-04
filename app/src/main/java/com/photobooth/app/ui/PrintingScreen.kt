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
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
    isCompleted: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val progress = if (totalTimeoutSeconds > 0) {
        ((totalTimeoutSeconds - remainingSeconds).toFloat() / totalTimeoutSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF16192E),
                        Color(0xFF0F101A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            if (!isCompleted) {
                // Printing in progress state
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(130.dp),
                        color = Color(0xFFE91E63),
                        trackColor = Color(0x33E91E63),
                        strokeWidth = 8.dp
                    )
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(54.dp)
                            .scale(pulseScale)
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = "STAMPA IN CORSO...",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = statusMessage.ifEmpty { "La Canon SELPHY CP1500 sta imprimendo la tua foto." },
                    fontSize = 18.sp,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Processo di sublimazione termica a 4 passaggi (circa 45 sec).\nAttendi il termine per evitare inceppamenti o sovrapposizioni.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Pass indication dots (Yellow -> Magenta -> Cyan -> Overcoat)
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val passIndex = (progress * 4).toInt().coerceIn(0, 3)
                    val colors = listOf(
                        Color(0xFFFFEB3B) to "Giallo",
                        Color(0xFFE91E63) to "Magenta",
                        Color(0xFF00BCD4) to "Ciano",
                        Color(0xFFFFFFFF) to "Lucido"
                    )

                    colors.forEachIndexed { index, (color, label) ->
                        val isActive = index <= passIndex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (index == passIndex) 20.dp else 14.dp)
                                    .background(
                                        color = if (isActive) color else color.copy(alpha = 0.2f),
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

                Spacer(modifier = Modifier.height(28.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .width(360.dp)
                        .height(8.dp),
                    color = Color(0xFFE91E63),
                    trackColor = Color(0x33FFFFFF)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tempo residuo stimato: ${remainingSeconds}s",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

            } else {
                // Completed State
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
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(96.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "STAMPA COMPLETATA!",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Ritira la tua doppia striscia dalla Canon SELPHY! 📸✨",
                            fontSize = 20.sp,
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Ritorno automatico alla schermata iniziale...",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
