package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor.OperationalStatus
import com.example.ui.AgroViewModel
import com.example.ui.theme.SemaforoGreen
import com.example.ui.theme.SemaforoOff
import com.example.ui.theme.SemaforoRed
import com.example.ui.theme.SemaforoYellow

@Composable
fun SemaforoScreen(viewModel: AgroViewModel) {
    val telemetry by viewModel.telemetry.collectAsState()
    val pump by viewModel.currentPump.collectAsState()
    val lastDispatch by viewModel.lastDispatchResult.collectAsState()
    val cloudDevices by viewModel.cloudDevices.collectAsState()

    val statusColor by animateColorAsState(
        targetValue = when (telemetry.status) {
            OperationalStatus.OPTIMAL -> SemaforoGreen
            OperationalStatus.WARNING -> SemaforoYellow
            OperationalStatus.CRITICAL -> SemaforoRed
            OperationalStatus.OFF -> SemaforoOff
        },
        label = "StatusColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Device Station Banner with unique ID
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141F17)),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B)),
            modifier = Modifier.fillMaxWidth().testTag("card_device_station_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "ID Estación",
                        tint = SemaforoGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ESTACIÓN: ${viewModel.deviceId}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SemaforoGreen)
                    )
                    Text(
                        text = "Cloud Activa (${cloudDevices.size} est.)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA7F3D0)
                    )
                }
            }
        }

        // Semáforo Indicator Beacon
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(statusColor.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
                .border(4.dp, statusColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Vibración",
                    tint = statusColor,
                    modifier = Modifier.size(38.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "%.2f".format(telemetry.rms),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
                Text(
                    text = "m/s² RMS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBD5E1)
                )
            }
        }

        // Status Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(statusColor.copy(alpha = 0.15f))
                .border(1.dp, statusColor, RoundedCornerShape(8.dp))
                .padding(vertical = 10.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = telemetry.statusText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = statusColor,
                textAlign = TextAlign.Center
            )
        }

        // Real-Time Vibration Telemetry Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = "FRECUENCIA PICO",
                value = if (telemetry.isPumpRunning) "%.1f Hz".format(telemetry.peakFrequency) else "0.0 Hz",
                subtitle = "Armónico Dominante",
                icon = Icons.Default.Waves,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "SALUD MECÁNICA",
                value = "%.0f%%".format(telemetry.healthPercentage),
                subtitle = "Norma ISO 10816-3",
                icon = Icons.Default.Bolt,
                tint = SemaforoGreen,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "PICO A PICO",
                value = "%.1f".format(telemetry.peakToPeak),
                subtitle = "m/s² Excursión",
                icon = Icons.Default.Sensors,
                tint = Color(0xFFA78BFA),
                modifier = Modifier.weight(1f)
            )
        }

        // Threshold Reference Card
        pump?.let { p ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141F17)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "CALIBRACIÓN DE REFERENCIA (ISO 10816-3)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFA7F3D0)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Línea Base", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            Text(text = "%.2f m/s²".format(p.baselineRms), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                        Column {
                            Text(text = "Umbral Amarillo", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            Text(text = "%.2f m/s²".format(p.baselineRms * p.yellowThresholdMultiplier), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = SemaforoYellow)
                        }
                        Column {
                            Text(text = "Umbral Rojo", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            Text(text = "%.2f m/s²".format(p.baselineRms * p.redThresholdMultiplier), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = SemaforoRed)
                        }
                    }
                }
            }
        }

        // Autonomous Dispatch Feedback Banner
        AnimatedVisibility(visible = lastDispatch != null) {
            lastDispatch?.let { dispatch ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Despacho",
                            tint = SemaforoYellow,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = dispatch.details,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }

        // Quick Alert Test Button
        Button(
            onClick = { viewModel.testAutonomousDispatch() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E3A2B),
                contentColor = Color(0xFFA7F3D0)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_test_dispatch")
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = "Probar Despacho",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Probar Despacho Autónomo (SMS/Notif)", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141F17)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}
