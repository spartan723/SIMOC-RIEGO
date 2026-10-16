package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor.OperationalStatus
import com.example.ui.theme.SemaforoGreen
import com.example.ui.theme.SemaforoGreenGlow
import com.example.ui.theme.SemaforoOff
import com.example.ui.theme.SemaforoRed
import com.example.ui.theme.SemaforoRedGlow
import com.example.ui.theme.SemaforoYellow
import com.example.ui.theme.SemaforoYellowGlow

@Composable
fun SemaforoIndicator(
    status: OperationalStatus,
    statusMessage: String,
    ratioToBaseline: Float,
    healthPercentage: Int,
    isoClassification: String,
    modifier: Modifier = Modifier
) {
    val (targetColor, targetGlow, icon, titleText) = when (status) {
        OperationalStatus.OFF -> Quadruple(
            SemaforoOff,
            Color(0x2264748B),
            Icons.Default.PowerSettingsNew,
            "BOMBA EN ESPERA / APAGADA"
        )
        OperationalStatus.NORMAL -> Quadruple(
            SemaforoGreen,
            SemaforoGreenGlow,
            Icons.Default.CheckCircle,
            "BOMBA NORMAL (OPERACIÓN ÓPTIMA)"
        )
        OperationalStatus.WARNING -> Quadruple(
            SemaforoYellow,
            SemaforoYellowGlow,
            Icons.Default.WarningAmber,
            "VIBRACIÓN ELEVADA - REVISAR"
        )
        OperationalStatus.CRITICAL -> Quadruple(
            SemaforoRed,
            SemaforoRedGlow,
            Icons.Default.Warning,
            "PELIGRO CRÍTICO - DETENER BOMBA"
        )
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(400),
        label = "colorAnim"
    )

    // Pulsing halo for warning and critical states
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (status == OperationalStatus.CRITICAL) 1.14f else if (status == OperationalStatus.WARNING) 1.06f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (status == OperationalStatus.CRITICAL) 550 else 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141C16),
                        Color(0xFF0C120E)
                    )
                )
            )
            .border(2.dp, animatedColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .padding(vertical = 24.dp, horizontal = 16.dp)
            .testTag("semaforo_indicator_card"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Outer pulsing ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(190.dp)
                .scale(pulseScale)
        ) {
            // Glow aura
            Box(
                modifier = Modifier
                    .size(175.dp)
                    .clip(CircleShape)
                    .background(targetGlow)
            )

            // Inner solid traffic light circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(140.dp)
                    .shadow(elevation = 16.dp, shape = CircleShape, spotColor = animatedColor)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                animatedColor,
                                animatedColor.copy(alpha = 0.85f),
                                Color(0xFF111814)
                            )
                        )
                    )
                    .border(4.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                    .testTag("semaforo_light_circle")
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = titleText,
                    tint = Color.White,
                    modifier = Modifier.size(68.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large prominent title
        Text(
            text = titleText,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            ),
            color = animatedColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("semaforo_title_text")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Direct plain-language instruction for farmer / field worker
        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                lineHeight = 22.sp
            ),
            color = Color(0xFFF1F5F9),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .testTag("semaforo_instruction_text")
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status != OperationalStatus.OFF) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2923))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Vibración: ${String.format("%.1f", ratioToBaseline)}x de línea base",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E2923))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Salud: $healthPercentage%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = animatedColor
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
