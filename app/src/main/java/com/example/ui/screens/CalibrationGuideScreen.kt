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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sensors
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AgroViewModel
import com.example.ui.theme.SemaforoGreen

@Composable
fun CalibrationGuideScreen(viewModel: AgroViewModel) {
    val pump by viewModel.currentPump.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val calState by viewModel.calibrationState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141F17)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CompassCalibration,
                        contentDescription = "Calibración",
                        tint = SemaforoGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "MONTAJE Y CALIBRACIÓN DE LÍNEA BASE",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Para que cada celular funcione como una estación de monitoreo precisa:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE2E8F0)
                )

                Spacer(modifier = Modifier.height(8.dp))

                MountingStep(
                    step = "1",
                    text = "Fije el teléfono rígidamente al cabezal de la bomba o caja de rodamientos (soporte magnético o abrazadera metálica)."
                )
                MountingStep(
                    step = "2",
                    text = "Encienda la bomba de riego a régimen normal de trabajo y espere 60 segundos hasta estabilizar la presión."
                )
                MountingStep(
                    step = "3",
                    text = "Inicie la calibración de 10 segundos para fijar el patrón de vibración sana (Línea Base RMS)."
                )
            }
        }

        // Live Calibration Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141F17)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A2B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "LÍNEA BASE ACTUAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "%.2f m/s² RMS".format(pump?.baselineRms ?: 1.85f),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = SemaforoGreen
                )
                Text(
                    text = "Lectura en vivo: %.2f m/s²".format(telemetry.rms),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE2E8F0)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (calState.isCalibrating) {
                    Text(
                        text = "Midiendo vibración en régimen continuo: ${calState.progressSeconds} / ${calState.totalSeconds} s",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA7F3D0)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { calState.progressSeconds.toFloat() / calState.totalSeconds.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = SemaforoGreen,
                        trackColor = Color(0xFF1E3A2B)
                    )
                } else {
                    Button(
                        onClick = { viewModel.startCalibration() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SemaforoGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_start_calibration")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Calibrar",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calibrar Línea Base (10s)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MountingStep(step: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF064E3B)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = SemaforoGreen
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFCBD5E1),
            modifier = Modifier.weight(1f)
        )
    }
}
