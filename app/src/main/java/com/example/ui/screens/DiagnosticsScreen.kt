package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VibrationReading
import com.example.ui.AgroViewModel
import com.example.ui.theme.SemaforoGreen
import com.example.ui.theme.SemaforoOff
import com.example.ui.theme.SemaforoRed
import com.example.ui.theme.SemaforoYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(viewModel: AgroViewModel) {
    val healthSummary by viewModel.healthSummary.collectAsState()
    val allReadings by viewModel.recentReadings.collectAsState()
    val alertEvents by viewModel.alertEvents.collectAsState()

    var selectedFilter by remember { mutableStateOf("TODOS") }

    val filteredReadings = remember(allReadings, selectedFilter) {
        when (selectedFilter) {
            "ALERTAS" -> allReadings.filter { it.status == "ADVERTENCIA" || it.status == "CRITICO" }
            "OPTIMOS" -> allReadings.filter { it.status == "NORMAL" }
            else -> allReadings
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Real Mechanical Health Summary Header
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141F17)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B)),
                modifier = Modifier.fillMaxWidth().testTag("card_health_summary")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Diagnóstico Real",
                                tint = SemaforoGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RESUMEN DE SALUD MECÁNICA REAL",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SemaforoGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "%.0f%% SALUD".format(healthSummary.healthScore),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                color = SemaforoGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HealthMetricItem(
                            label = "Desgaste Rodamientos",
                            value = "%.2f x".format(healthSummary.bearingWearIndex),
                            indicatorColor = if (healthSummary.bearingWearIndex > 1.5f) SemaforoRed else SemaforoGreen
                        )
                        HealthMetricItem(
                            label = "Riesgo Cavitación",
                            value = healthSummary.cavitationRisk,
                            indicatorColor = when (healthSummary.cavitationRisk) {
                                "ELEVADO" -> SemaforoRed
                                "MEDIO" -> SemaforoYellow
                                else -> SemaforoGreen
                            }
                        )
                        HealthMetricItem(
                            label = "Días para Service",
                            value = "${healthSummary.estimatedDaysToService} d",
                            indicatorColor = if (healthSummary.estimatedDaysToService < 30) SemaforoYellow else SemaforoGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Horas Operativas Totales: %.1f h".format(healthSummary.operatingHours),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Alertas registradas: ${alertEvents.size}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (alertEvents.isNotEmpty()) SemaforoYellow else Color(0xFFA7F3D0)
                        )
                    }
                }
            }
        }

        item {
            // Filter Chips & Header for History
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HISTORIAL COMPLETO DE TELEMETRÍA",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "${filteredReadings.size} registros",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA7F3D0)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedFilter == "TODOS",
                        onClick = { selectedFilter = "TODOS" },
                        label = { Text("Todos (${allReadings.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SemaforoGreen,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "ALERTAS",
                        onClick = { selectedFilter = "ALERTAS" },
                        label = { Text("Alertas (${allReadings.count { it.status == "ADVERTENCIA" || it.status == "CRITICO" }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SemaforoYellow,
                            selectedLabelColor = Color.Black
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "OPTIMOS",
                        onClick = { selectedFilter = "OPTIMOS" },
                        label = { Text("Normales") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SemaforoGreen,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        if (filteredReadings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Recopilando telemetría del sensor en tiempo real...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF64748B)
                    )
                }
            }
        } else {
            items(filteredReadings, key = { it.id }) { reading ->
                ReadingItemCard(reading = reading)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun HealthMetricItem(
    label: String,
    value: String,
    indicatorColor: Color
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = Color(0xFF94A3B8))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            Text(text = value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
    }
}

@Composable
fun ReadingItemCard(reading: VibrationReading) {
    val statusColor = when (reading.status) {
        "NORMAL" -> SemaforoGreen
        "ADVERTENCIA" -> SemaforoYellow
        "CRITICO" -> SemaforoRed
        else -> SemaforoOff
    }

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()) }
    val formattedTime = remember(reading.timestamp) { dateFormat.format(Date(reading.timestamp)) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141F17)),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Text(
                        text = reading.status,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                    Text(
                        text = "• $formattedTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
                Text(
                    text = "Vibración RMS: %.2f m/s² | Pico: %.1f Hz".format(reading.rms, reading.peakFrequency),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Color.White
                )
                if (reading.deviceId.isNotBlank()) {
                    Text(
                        text = "Estación: ${reading.deviceId}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        ),
                        color = Color(0xFFA7F3D0)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "%.0f%%".format(reading.healthPercentage),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColor
                )
            }
        }
    }
}
