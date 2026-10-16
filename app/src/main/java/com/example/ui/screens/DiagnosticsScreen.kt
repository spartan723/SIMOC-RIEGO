package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VibrationReading
import com.example.ui.AgroViewModel
import com.example.ui.theme.SemaforoGreen
import com.example.ui.theme.SemaforoRed
import com.example.ui.theme.SemaforoYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    viewModel: AgroViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val readings by viewModel.recentReadings.collectAsState()
    val alerts by viewModel.alertEvents.collectAsState()
    val pump by viewModel.currentPump.collectAsState()

    val dateFormat = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D0A))
            .testTag("diagnostics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Share
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Historial & Diagnóstico",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "${readings.size} registros almacenados en el teléfono",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                Button(
                    onClick = {
                        val reportText = buildReport(pump?.name ?: "Bomba", readings, alerts)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Reporte Diagnóstico AgroVigía")
                            putExtra(Intent.EXTRA_TEXT, reportText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Compartir Reporte"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B3825)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = SemaforoGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exportar", color = Color(0xFF4ADE80), fontSize = 13.sp)
                }
            }
        }

        // Summary Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131A15)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "RESUMEN DE SALUD MECÁNICA",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryColumn(
                            title = "Alertas Totales",
                            value = alerts.size.toString(),
                            color = if (alerts.isNotEmpty()) SemaforoYellow else SemaforoGreen
                        )
                        SummaryColumn(
                            title = "Línea Base",
                            value = "${String.format("%.2f", pump?.baselineRms ?: 1.9f)} m/s²",
                            color = Color(0xFF38BDF8)
                        )
                        SummaryColumn(
                            title = "Horas Operadas",
                            value = "${String.format("%.1f", pump?.totalOperatingHours ?: 0f)} h",
                            color = Color(0xFFA78BFA)
                        )
                    }
                }
            }
        }

        // Section: Lista de Eventos y Ciclos
        item {
            Text(
                text = "Registro de Ciclos de Medición (Cada 15s)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        if (readings.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131A15)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Esperando próximos ciclos de lectura del sensor...",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(readings, key = { it.id }) { reading ->
                ReadingItemCard(reading = reading, dateFormat = dateFormat)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryColumn(title: String, value: String, color: Color) {
    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = color
        )
    }
}

@Composable
private fun ReadingItemCard(
    reading: VibrationReading,
    dateFormat: SimpleDateFormat
) {
    val (statusColor, statusLabel) = when (reading.status) {
        "NORMAL" -> Pair(SemaforoGreen, "NORMAL")
        "ADVERTENCIA" -> Pair(SemaforoYellow, "AVISO")
        "CRITICO" -> Pair(SemaforoRed, "PELIGRO")
        else -> Pair(Color(0xFF64748B), "APAGADA")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A15)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${String.format("%.2f", reading.rms)} m/s²  |  ${String.format("%.1f", reading.peakFrequency)} Hz",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = dateFormat.format(Date(reading.timestamp)),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = statusLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = statusColor
                )
            }
        }
    }
}

private fun buildReport(pumpName: String, readings: List<VibrationReading>, alerts: List<VibrationReading>): String {
    val sb = StringBuilder()
    sb.append("📊 REPORTE DE VIBRACIÓN SIMOC-RIEGO\n")
    sb.append("Equipo: $pumpName\n")
    sb.append("Fecha: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}\n")
    sb.append("Alertas registradas: ${alerts.size}\n\n")
    sb.append("Últimos registros de sensor:\n")
    readings.take(15).forEach {
        val dt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it.timestamp))
        sb.append("[$dt] RMS: ${String.format("%.2f", it.rms)} m/s² | ${String.format("%.1f", it.peakFrequency)} Hz | ${it.status}\n")
    }
    sb.append("\nGenerado por SIMOC-Riego - Sistema de Monitoreo y Control de Riego.")
    return sb.toString()
}
