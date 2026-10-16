package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor.OperationalStatus
import com.example.sensor.SimulationScenario
import com.example.ui.AgroViewModel
import com.example.ui.components.SemaforoIndicator
import com.example.ui.components.WaveformCanvas
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.SemaforoGreen
import com.example.ui.theme.SemaforoRed
import com.example.ui.theme.SemaforoYellow

@Composable
fun SemaforoScreen(
    viewModel: AgroViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val telemetry by viewModel.telemetry.collectAsState()
    val pump by viewModel.currentPump.collectAsState()
    val currentScenario by viewModel.currentScenario.collectAsState()
    val calibrationState by viewModel.calibrationState.collectAsState()
    val keepAwake by viewModel.keepScreenAwake.collectAsState()

    var showScenarioDialog by remember { mutableStateOf(false) }
    var showSendAlertDialog by remember { mutableStateOf(false) }

    // Keep screen awake for permanent monitor on pump chassis/wall box
    DisposableEffect(keepAwake) {
        val window = (context as? Activity)?.window
        if (keepAwake) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D0A))
            .testTag("semaforo_screen_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pump Identity & Installation Header Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131F17)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("pump_header_card")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1B432E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = SemaforoGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = pump?.name ?: "Bomba de Riego Pozo 01",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "${pump?.sector ?: "Sector Riego"} • ${pump?.pumpType ?: "Centrífuga 7.5 HP"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0D1610))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${String.format("%.1f", pump?.totalOperatingHours ?: 140f)} h",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
            }
        }

        // Active sensor status chip & Kiosk toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sensor source badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (currentScenario == SimulationScenario.REAL_SENSOR) Color(0xFF1B3825) else Color(0xFF382914),
                    modifier = Modifier.clickable { showScenarioDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (currentScenario == SimulationScenario.REAL_SENSOR) SemaforoGreen else SemaforoYellow
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentScenario == SimulationScenario.REAL_SENSOR) "Sensor: Acelerómetro Físico" else "Prueba: ${currentScenario.name.replace("SIM_", "")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Kiosk Screen Awake Toggle
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (keepAwake) Color(0xFF1B3825) else Color(0xFF1E2923),
                    modifier = Modifier.clickable { viewModel.toggleKeepScreenAwake() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Pantalla siempre encendida",
                            tint = if (keepAwake) SemaforoYellow else Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (keepAwake) "Kiosco ON" else "Kiosco OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (keepAwake) Color(0xFFE2E8F0) else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // Calibration in-progress banner
        if (calibrationState.isCalibrating) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3828)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("calibration_progress_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CompassCalibration, contentDescription = null, tint = SemaforoGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Calibrando Línea Base en Vivo (${calibrationState.progressSeconds}/${calibrationState.totalSeconds}s)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { calibrationState.progressSeconds.toFloat() / calibrationState.totalSeconds },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = SemaforoGreen,
                            trackColor = Color(0xFF0F1E16),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Mantenga la bomba operando a régimen normal y estable.",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }
        }

        // Primary Large Traffic Light (Semáforo)
        item {
            SemaforoIndicator(
                status = telemetry.status,
                statusMessage = telemetry.statusMessage,
                ratioToBaseline = telemetry.ratioToBaseline,
                healthPercentage = telemetry.healthPercentage,
                isoClassification = telemetry.isoClassification
            )
        }

        // Real-Time Oscilloscope Waveform (Vibración de 100 Hz)
        item {
            WaveformCanvas(
                samples = telemetry.waveformSamples,
                status = telemetry.status,
                isPumpRunning = telemetry.isPumpRunning
            )
        }

        // Telemetry Grid Tiles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "INTENSIDAD (RMS)",
                        value = "${String.format("%.2f", telemetry.rms)} m/s²",
                        subtitle = "Línea base: ${String.format("%.2f", viewModel.sensorMonitor.baselineRms)}",
                        icon = Icons.Default.Speed,
                        statusColor = when (telemetry.status) {
                            OperationalStatus.OFF -> Color(0xFF94A3B8)
                            OperationalStatus.NORMAL -> SemaforoGreen
                            OperationalStatus.WARNING -> SemaforoYellow
                            OperationalStatus.CRITICAL -> SemaforoRed
                        },
                        modifier = Modifier.weight(1f).testTag("metric_card_rms")
                    )
                    MetricCard(
                        title = "FRECUENCIA PICO",
                        value = if (telemetry.isPumpRunning) "${String.format("%.1f", telemetry.peakFrequency)} Hz" else "-- Hz",
                        subtitle = "Giro eje: ~50 Hz",
                        icon = Icons.Default.LinearScale,
                        statusColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f).testTag("metric_card_freq")
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "ESTADO MOTOR",
                        value = if (telemetry.isPumpRunning) "BOMBEO ACTIVO" else "EN REPOSO",
                        subtitle = "Umbral: >0.20 m/s²",
                        icon = Icons.Default.PowerSettingsNew,
                        statusColor = if (telemetry.isPumpRunning) SemaforoGreen else Color(0xFF64748B),
                        modifier = Modifier.weight(1f).testTag("metric_card_motor")
                    )
                    MetricCard(
                        title = "PICO A PICO",
                        value = "${String.format("%.2f", telemetry.peakToPeak)} m/s²",
                        subtitle = "Amplitud máxima",
                        icon = Icons.Default.FlashOn,
                        statusColor = Color(0xFFA78BFA),
                        modifier = Modifier.weight(1f).testTag("metric_card_pkpk")
                    )
                }
            }
        }

        // ISO 10816 Severity Classification Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131A15)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "NORMA DE VIBRACIÓN EN BOMBAS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = telemetry.isoClassification,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = when (telemetry.status) {
                                OperationalStatus.NORMAL -> SemaforoGreen
                                OperationalStatus.WARNING -> SemaforoYellow
                                OperationalStatus.CRITICAL -> SemaforoRed
                                else -> Color(0xFFCBD5E1)
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2923))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ISO 10816-3",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA7F3D0)
                        )
                    }
                }
            }
        }

        // Action Buttons Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.startCalibration() },
                        enabled = !calibrationState.isCalibrating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B3825),
                            contentColor = Color(0xFF4ADE80)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("btn_calibrate")
                    ) {
                        Icon(Icons.Default.CompassCalibration, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Calibrar Base", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showSendAlertDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("btn_send_alert")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Avisar / SMS", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { showScenarioDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_simulation_scenarios")
                ) {
                    Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Probar Escenarios de Falla en Bomba", fontWeight = FontWeight.Medium)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Simulation Scenarios Dialog (100% Water Pump Focused)
    if (showScenarioDialog) {
        AlertDialog(
            onDismissRequest = { showScenarioDialog = false },
            title = {
                Text("Simulador de Fallas en Bomba", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Seleccione un escenario para probar la detección y las recomendaciones del vigía:",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ScenarioOptionItem(
                        title = "Acelerómetro Físico Real",
                        subtitle = "Mide vibración real del teléfono en la bomba",
                        selected = currentScenario == SimulationScenario.REAL_SENSOR,
                        onClick = {
                            viewModel.setSimulationScenario(SimulationScenario.REAL_SENSOR)
                            showScenarioDialog = false
                        }
                    )
                    ScenarioOptionItem(
                        title = "Bomba Sana (1.85 m/s² - Verde)",
                        subtitle = "Rodamientos lubricados, funcionamiento suave",
                        selected = currentScenario == SimulationScenario.SIM_HEALTHY_PUMP,
                        onClick = {
                            viewModel.setSimulationScenario(SimulationScenario.SIM_HEALTHY_PUMP)
                            showScenarioDialog = false
                        }
                    )
                    ScenarioOptionItem(
                        title = "Falta de Lubricación (2.45 m/s²)",
                        subtitle = "Aviso preventivo antes de daño mecánico",
                        selected = currentScenario == SimulationScenario.SIM_LUBRICATION_NEEDED,
                        onClick = {
                            viewModel.setSimulationScenario(SimulationScenario.SIM_LUBRICATION_NEEDED)
                            showScenarioDialog = false
                        }
                    )
                    ScenarioOptionItem(
                        title = "Rodamiento con Desgaste (3.2 m/s²)",
                        subtitle = "Alerta amarilla: programar cambio de rodamiento",
                        selected = currentScenario == SimulationScenario.SIM_BEARING_WEAR,
                        onClick = {
                            viewModel.setSimulationScenario(SimulationScenario.SIM_BEARING_WEAR)
                            showScenarioDialog = false
                        }
                    )
                    ScenarioOptionItem(
                        title = "Desbalance de Impulsor (4.7 m/s²)",
                        subtitle = "Alarma roja crítica: vibración peligrosa",
                        selected = currentScenario == SimulationScenario.SIM_IMPELLER_IMBALANCE,
                        onClick = {
                            viewModel.setSimulationScenario(SimulationScenario.SIM_IMPELLER_IMBALANCE)
                            showScenarioDialog = false
                        }
                    )
                    ScenarioOptionItem(
                        title = "Cavitación / Entrada de Aire",
                        subtitle = "Burbujas implosionando en impulsor",
                        selected = currentScenario == SimulationScenario.SIM_PUMP_CAVITATION,
                        onClick = {
                            viewModel.setSimulationScenario(SimulationScenario.SIM_PUMP_CAVITATION)
                            showScenarioDialog = false
                        }
                    )
                    ScenarioOptionItem(
                        title = "Bomba Apagada (<0.1 m/s²)",
                        subtitle = "En reposo, no emite falsas alarmas",
                        selected = currentScenario == SimulationScenario.SIM_PUMP_OFF,
                        onClick = {
                            viewModel.setSimulationScenario(SimulationScenario.SIM_PUMP_OFF)
                            showScenarioDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showScenarioDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Send Alert Dialog (WhatsApp / SMS)
    if (showSendAlertDialog) {
        AlertDialog(
            onDismissRequest = { showSendAlertDialog = false },
            title = {
                Text("Avisar al Productor / Cooperativa", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Envía un reporte comprimido en pocos bytes por WhatsApp o SMS para avisar al productor antes de que la bomba se queme:",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2923)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (telemetry.status == OperationalStatus.CRITICAL) {
                                "🚨 ALERTA: ${pump?.name ?: "Bomba 1"}: Vibración ${String.format("%.1f", telemetry.ratioToBaseline)}x mayor a lo normal. Detener y revisar rodamientos/impulsor."
                            } else if (telemetry.status == OperationalStatus.WARNING) {
                                "⚠️ AVISO: ${pump?.name ?: "Bomba 1"}: Vibración ${String.format("%.1f", telemetry.ratioToBaseline)}x sobre línea base. Programar engrase esta semana."
                            } else {
                                "✅ ${pump?.name ?: "Bomba 1"}: Operando normal (${String.format("%.2f", telemetry.rms)} m/s²)."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendAlert(context, viaWhatsApp = true)
                        showSendAlertDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Text("WhatsApp", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.sendAlert(context, viaWhatsApp = false)
                        showSendAlertDialog = false
                    }
                ) {
                    Text("SMS Directo")
                }
            }
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A15)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF94A3B8)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFCBD5E1),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ScenarioOptionItem(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Color(0xFF1B3825) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (selected) SemaforoGreen else Color(0xFF64748B))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8))
            }
        }
    }
}
