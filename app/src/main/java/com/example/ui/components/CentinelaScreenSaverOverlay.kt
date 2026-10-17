package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor.OperationalStatus
import com.example.ui.theme.SemaforoGreen
import com.example.ui.theme.SemaforoRed
import com.example.ui.theme.SemaforoYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CentinelaScreenSaverOverlay(
    status: OperationalStatus,
    currentRms: Float,
    healthPercentage: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        OperationalStatus.OFF -> Color(0xFF64748B)
        OperationalStatus.NORMAL -> SemaforoGreen
        OperationalStatus.WARNING -> SemaforoYellow
        OperationalStatus.CRITICAL -> SemaforoRed
    }

    val infiniteTransition = rememberInfiniteTransition(label = "centinelaHeartbeat")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (status == OperationalStatus.CRITICAL) 400 else 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onDismiss() }
            .testTag("centinela_screensaver_box"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Heartbeat status dot
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "MODO CENTINELA ACTIVO",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF475569)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${String.format("%.2f", currentRms)} m/s²",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = statusColor.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Salud: $healthPercentage% • ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
                fontSize = 12.sp,
                color = Color(0xFF334155)
            )

            Spacer(modifier = Modifier.height(36.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F1713))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Toque la pantalla para ver el semáforo",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}
