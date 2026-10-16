package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AgroViewModel
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.SemaforoGreen
import com.example.ui.theme.SemaforoYellow

@Composable
fun CalibrationGuideScreen(
    viewModel: AgroViewModel,
    modifier: Modifier = Modifier
) {
    val calibrationState by viewModel.calibrationState.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val pump by viewModel.currentPump.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D0A))
            .testTag("calibration_guide_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Column {
                Text(
                    text = "Montaje & Calibración de Línea Base",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Asegure lecturas precisas estandarizando la posición física y aprendiendo el patrón de vibración sana.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Live Calibration Wizard Action Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14241B)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EmeraldPrimaryDark.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .testTag("calibration_action_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1B432E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CompassCalibration,
                                contentDescription = null,
                                tint = SemaforoGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Línea Base Actual: ${String.format("%.2f", pump?.baselineRms ?: 1.90f)} m/s²",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Frecuencia de referencia: ${String.format("%.1f", pump?.baselineFreq ?: 49.5f)} Hz",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (calibrationState.isCalibrating) {
                        Column {
                            Text(
                                text = "Midiendo vibración en vivo: ${calibrationState.progressSeconds} / ${calibrationState.totalSeconds} s",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = SemaforoGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { calibrationState.progressSeconds.toFloat() / calibrationState.totalSeconds },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = SemaforoGreen,
                                trackColor = Color(0xFF0D1812)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "RMS instantáneo detectado: ${String.format("%.2f", telemetry.rms)} m/s²",
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    } else {
                        Text(
                            text = "Haga clic para realizar un ciclo de calibración de 10 segundos mientras la bomba funciona de manera estable.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.startCalibration() },
                            colors = ButtonDefaults.buttonColors(containerColor = SemaforoGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_start_wizard_calibration")
                        ) {
                            Icon(Icons.Default.CompassCalibration, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Iniciar Calibración (10s)",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // Section Title: Reglas de Instalación Física
        item {
            Text(
                text = "Protocolo de Instalación Física (Paso a Paso)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        // Step 1: Ubicación
        item {
            InstallationStepCard(
                stepNumber = 1,
                title = "Ubicación: Carcasa de la Bomba",
                description = "Coloque el teléfono pegado directamente a la carcasa, lo más cerca posible del rodamiento o del eje del motor, donde mejor se transmite la vibración mecánica. No sirve colocarlo sobre la base de hormigón ni en superficies blandas.",
                icon = Icons.Default.Sensors,
                badgeText = "Crítico para precisión"
            )
        }

        // Step 2: Orientación
        item {
            InstallationStepCard(
                stepNumber = 2,
                title = "Orientación Estandarizada",
                description = "Mantenga siempre la misma orientación en todas las bombas (pantalla hacia arriba y el borde superior apuntando hacia el motor). El acelerómetro mide en 3 ejes (X, Y, Z); si el teléfono se gira, los datos de los ejes pierden comparabilidad.",
                icon = Icons.Default.ScreenRotation,
                badgeText = "Ejes X, Y, Z"
            )
        }

        // Step 3: Sujeción
        item {
            InstallationStepCard(
                stepNumber = 3,
                title = "Sujeción Rígida (Sin Holgura)",
                description = "Fije firmemente mediante imán de neodimio de alta potencia, abrazadera metálica o soporte atornillado a la carcasa. Si el soporte queda flojo o vibra por su cuenta, falseará las mediciones con picos espurios.",
                icon = Icons.Default.Lock,
                badgeText = "Imán / Abrazadera"
            )
        }

        // Step 4: Protección Ambiental
        item {
            InstallationStepCard(
                stepNumber = 4,
                title = "Caja Sellada (Polvo y Agua)",
                description = "Instale una caja plástica estanca (IP65) con visor transparente para observar el semáforo en el sitio. Evita que salpicaduras de riego, humedad y tierra dañen el celular.",
                icon = Icons.Default.InvertColors,
                badgeText = "Protección IP65"
            )
        }

        // Step 5: Seguridad Térmica y Batería (Important)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF261D13)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SemaforoYellow.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = SemaforoYellow,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Seguridad: Baterías Viejas y Calor",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = SemaforoYellow
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Las baterías antiguas no toleran el calor del motor y pueden hincharse.\n" +
                                "• Recomendación: Si el teléfono lo permite, retire la batería e instale alimentación directa por cargador USB.\n" +
                                "• Si no puede retirarse, revise periódicamente que la tapa trasera no esté inflada y use un soporte que aísle el calor directo del block.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun InstallationStepCard(
    stepNumber: Int,
    title: String,
    description: String,
    icon: ImageVector,
    badgeText: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A15)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimaryDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber.toString(),
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E2923))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA7F3D0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                color = Color(0xFFCBD5E1)
            )
        }
    }
}
