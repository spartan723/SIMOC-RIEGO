package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor.AcousticTelemetry
import com.example.ui.theme.SemaforoGreen
import com.example.ui.theme.SemaforoRed
import com.example.ui.theme.SemaforoYellow

@Composable
fun AcousticCavitationCard(
    acoustic: AcousticTelemetry,
    onSampleAudioClick: () -> Unit,
    isSamplingAudio: Boolean,
    modifier: Modifier = Modifier
) {
    val statusColor = if (acoustic.isCavitationDetected) SemaforoRed else SemaforoGreen

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A15)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (acoustic.isCavitationDetected) SemaforoRed.copy(alpha = 0.6f) else Color(0xFF1E2923),
                RoundedCornerShape(16.dp)
            )
            .testTag("acoustic_cavitation_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E2923)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (acoustic.isCavitationDetected) SemaforoRed else Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DETECCIÓN ACÚSTICA DE CAVITACIÓN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = if (acoustic.isCavitationDetected) "¡Riesgo de Cavitación!" else "Firma Acústica Estable",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                    }
                }

                Button(
                    onClick = onSampleAudioClick,
                    enabled = !isSamplingAudio,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E2923),
                        contentColor = Color(0xFFCBD5E1)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isSamplingAudio) "Muestreando..." else "Muestrear Mic",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar for 2 kHz - 8 kHz cavitation high frequency energy
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Energía HF (2-8 kHz):",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { acoustic.highFrequencyEnergyRatio },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = statusColor,
                    trackColor = Color(0xFF0D140F)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(acoustic.highFrequencyEnergyRatio * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = acoustic.statusText,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp
            )
        }
    }
}
