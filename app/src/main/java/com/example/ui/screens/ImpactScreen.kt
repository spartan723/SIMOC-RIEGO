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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MaintenanceRecord
import com.example.ui.AgroViewModel
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.SemaforoGreen
import com.example.ui.theme.SemaforoYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImpactScreen(
    viewModel: AgroViewModel,
    modifier: Modifier = Modifier
) {
    val totalSavings by viewModel.totalSavings.collectAsState()
    val maintenanceRecords by viewModel.maintenanceRecords.collectAsState()

    var showAddMaintenanceDialog by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D0A))
            .testTag("impact_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Innovación & Impacto Comunitario",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Democratizando el mantenimiento predictivo usando celulares rescatados del cajón.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Highlight Card: Impacto Económico Medible
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14241B)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, EmeraldPrimaryDark, RoundedCornerShape(20.dp))
                    .testTag("savings_summary_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AHORRO ECONÓMICO ACUMULADO",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = Color(0xFFA7F3D0)
                        )
                        Icon(Icons.Default.Savings, contentDescription = null, tint = SemaforoGreen, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "$${String.format(Locale.US, "%,.0f", totalSavings ?: 830.0)} USD",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Evitando reparaciones de urgencia, traslados y rebobinados de motores en plena cosecha de riego.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ImpactPill(label = "2 Bombas Salvadas", icon = Icons.Default.Build, modifier = Modifier.weight(1f))
                        ImpactPill(label = "96 h Riego Protegido", icon = Icons.Default.Timeline, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Section: Los 5 Pilares de Innovación
        item {
            Text(
                text = "¿Dónde está la Innovación?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        item {
            InnovationCard(
                title = "1. Hardware a Costo Casi Cero",
                description = "Un teléfono de hace 6-8 años reemplaza equipos industriales de \$400-\$1.000 USD. Tiene acelerómetro triaxial de alta resolución, CPU, pantalla táctil y módem ya fabricados.",
                icon = Icons.Default.Recycling,
                badge = "Economía Circular"
            )
        }

        item {
            InnovationCard(
                title = "2. Diseñado para el Productor, no para el Ingeniero",
                description = "En vez de gráficos complejos de FFT y espectros FFT industriales, entrega un semáforo visual (Verde, Amarillo, Rojo) con instrucciones claras en lenguaje directo.",
                icon = Icons.Default.Lightbulb,
                badge = "Adopción Real"
            )
        }

        item {
            InnovationCard(
                title = "3. Calibración Adaptativa por Equipo",
                description = "No impone umbrales teóricos rígidos de fábrica. Aprende la línea base de bombas con piezas reparadas, adaptadas o motores antiguos típicos del campo.",
                icon = Icons.Default.Timeline,
                badge = "Línea Base Propia"
            )
        }

        item {
            InnovationCard(
                title = "4. Resiliencia en Poca Conectividad",
                description = "El cerebro procesa todo localmente. No transmite audio ni miles de lecturas crudas: resume el estado en pocos bytes aptos para SMS o WhatsApp en zonas rurales remotas.",
                icon = Icons.Default.Eco,
                badge = "100% Offline First"
            )
        }

        item {
            InnovationCard(
                title = "5. Replicable en Redes de Riego Comunitarias",
                description = "Permite equipar bombas centrífugas, sumergibles de pozo profundo o motores de bombeo a combustión en cooperativas agrícolas completas, estandarizando la alerta temprana con celulares donados.",
                icon = Icons.Default.Groups,
                badge = "Multi-Bomba"
            )
        }

        // Section: Bitácora de Mantenimientos Preventivos
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bitácora de Mantenimientos",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Button(
                    onClick = { showAddMaintenanceDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B3825)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = SemaforoGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Registrar", color = Color(0xFF4ADE80), fontSize = 12.sp)
                }
            }
        }

        items(maintenanceRecords, key = { it.id }) { record ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131A15)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = record.actionType,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E3825))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "+$${record.estimatedRepairSavedUsd.toInt()} USD ahorrados",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4ADE80)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = record.description,
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${dateFormat.format(Date(record.timestamp))} • ${record.technicianName}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add Maintenance Dialog
    if (showAddMaintenanceDialog) {
        var actionType by remember { mutableStateOf("Engrase de rodamientos") }
        var description by remember { mutableStateOf("Inspección preventiva tras aviso amarillo de vibración.") }
        var savedUsd by remember { mutableStateOf("150") }

        AlertDialog(
            onDismissRequest = { showAddMaintenanceDialog = false },
            title = {
                Text("Registrar Mantenimiento Preventivo", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = actionType,
                        onValueChange = { actionType = it },
                        label = { Text("Tipo de Acción") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Detalle de la intervención") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = savedUsd,
                        onValueChange = { savedUsd = it },
                        label = { Text("Ahorro estimado en reparación (USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = savedUsd.toDoubleOrNull() ?: 100.0
                        viewModel.addMaintenance(actionType, description, amount)
                        showAddMaintenanceDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SemaforoGreen)
                ) {
                    Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMaintenanceDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ImpactPill(label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1B3825))
            .padding(vertical = 8.dp, horizontal = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = SemaforoGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 11.sp, color = Color(0xFFA7F3D0), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InnovationCard(
    title: String,
    description: String,
    icon: ImageVector,
    badge: String
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
                    Icon(icon, contentDescription = null, tint = EmeraldPrimaryDark, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E2923))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(badge, fontSize = 10.sp, color = Color(0xFFCBD5E1))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, fontSize = 12.sp, color = Color(0xFFCBD5E1), lineHeight = 18.sp)
        }
    }
}
