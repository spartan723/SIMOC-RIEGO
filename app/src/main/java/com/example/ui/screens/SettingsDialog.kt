package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PumpProfile
import com.example.data.supabase.SupabaseClient
import com.example.ui.theme.SemaforoGreen
import com.example.ui.theme.SemaforoYellow

@Composable
fun SettingsDialog(
    currentPump: PumpProfile?,
    deviceId: String,
    deviceModel: String,
    telegramTokenInit: String,
    telegramChatInit: String,
    webhookUrlInit: String,
    autoSmsInit: Boolean,
    supabaseUrlInit: String,
    supabaseAnonKeyInit: String,
    onTestAutonomousDispatch: () -> Unit,
    onTestSupabase: (url: String, key: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        sector: String,
        phone: String,
        yellowThresh: Float,
        redThresh: Float,
        tgToken: String,
        tgChat: String,
        webhook: String,
        autoSms: Boolean,
        supabaseUrl: String,
        supabaseAnonKey: String
    ) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(currentPump?.name ?: "Bomba Pozo Profundo #1") }
    var sector by remember { mutableStateOf(currentPump?.sector ?: "Sector Cuadro 4") }
    var phone by remember { mutableStateOf(currentPump?.alertPhoneNumber ?: "+5491123456789") }
    var yellowThresh by remember { mutableStateOf(currentPump?.yellowThresholdMultiplier?.toString() ?: "1.35") }
    var redThresh by remember { mutableStateOf(currentPump?.redThresholdMultiplier?.toString() ?: "2.0") }

    var tgToken by remember { mutableStateOf(telegramTokenInit) }
    var tgChat by remember { mutableStateOf(telegramChatInit) }
    var webhook by remember { mutableStateOf(webhookUrlInit) }
    var autoSms by remember { mutableStateOf(autoSmsInit) }

    var supabaseUrl by remember { mutableStateOf(supabaseUrlInit) }
    var supabaseAnonKey by remember { mutableStateOf(supabaseAnonKeyInit) }
    var isTestingSupabase by remember { mutableStateOf(false) }
    var supabaseTestFeedback by remember { mutableStateOf<String?>(null) }
    var supabaseFeedbackIsSuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141F17),
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = SemaforoGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configuración y Red",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Device Hardware Info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D140F), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E3A2B), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = SemaforoGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ID Estación Único",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("ID Estación", deviceId))
                                    Toast.makeText(context, "ID copiado: $deviceId", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar ID",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = deviceId,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                        Text(
                            text = deviceModel,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Supabase Cloud Section
                Text(
                    text = "Base de Datos Cloud (Supabase)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFA7F3D0)
                )

                OutlinedTextField(
                    value = supabaseUrl,
                    onValueChange = { supabaseUrl = it; supabaseTestFeedback = null },
                    label = { Text("Supabase Project URL") },
                    placeholder = { Text("https://xyzcompany.supabase.co") },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = supabaseAnonKey,
                    onValueChange = { supabaseAnonKey = it; supabaseTestFeedback = null },
                    label = { Text("Supabase Anon Public Key") },
                    placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR5cCI6...") },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isTestingSupabase = true
                            supabaseTestFeedback = null
                            onTestSupabase(supabaseUrl, supabaseAnonKey) { success, msg ->
                                isTestingSupabase = false
                                supabaseFeedbackIsSuccess = success
                                supabaseTestFeedback = msg
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (supabaseFeedbackIsSuccess) SemaforoGreen else Color(0xFFA7F3D0)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isTestingSupabase) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SemaforoGreen)
                        } else {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Probar", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Supabase SQL", SupabaseClient.SCHEMA_SQL))
                            Toast.makeText(context, "Script SQL copiado. Pégalo en SQL Editor de Supabase.", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(imageVector = Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copiar SQL", fontSize = 12.sp)
                    }
                }

                supabaseTestFeedback?.let { feedback ->
                    Text(
                        text = feedback,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (supabaseFeedbackIsSuccess) SemaforoGreen else SemaforoYellow,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Pump Profile Section
                Text(
                    text = "Parámetros de la Bomba",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFA7F3D0)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Equipo") },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("input_pump_name")
                )

                OutlinedTextField(
                    value = sector,
                    onValueChange = { sector = it },
                    label = { Text("Sector / Cuadro Agrícola") },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono Alerta SMS") },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = yellowThresh,
                        onValueChange = { yellowThresh = it },
                        label = { Text("Factor Advertencia (x Línea Base)") },
                        colors = textFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = redThresh,
                        onValueChange = { redThresh = it },
                        label = { Text("Factor Crítico (x Línea Base)") },
                        colors = textFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Autonomous Dispatch Section
                Text(
                    text = "Canales de Despacho Centinela",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFA7F3D0)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Envío Automático de SMS ante Peligro",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Switch(
                        checked = autoSms,
                        onCheckedChange = { autoSms = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = SemaforoGreen
                        )
                    )
                }

                OutlinedTextField(
                    value = tgToken,
                    onValueChange = { tgToken = it },
                    label = { Text("Telegram Bot Token (Opcional)") },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tgChat,
                    onValueChange = { tgChat = it },
                    label = { Text("Telegram Chat ID (Opcional)") },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = webhook,
                    onValueChange = { webhook = it },
                    label = { Text("Webhook URL (Opcional)") },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onTestAutonomousDispatch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E3A2B),
                        contentColor = Color(0xFFA7F3D0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Probar Despacho Autónomo Ahora")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val y = yellowThresh.toFloatOrNull() ?: 1.35f
                    val r = redThresh.toFloatOrNull() ?: 2.0f
                    onSave(name, sector, phone, y, r, tgToken, tgChat, webhook, autoSms, supabaseUrl, supabaseAnonKey)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SemaforoGreen,
                    contentColor = Color.Black
                ),
                modifier = Modifier.testTag("btn_save_settings")
            ) {
                Text("Guardar Cambios", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SemaforoGreen,
    unfocusedBorderColor = Color(0xFF26392B),
    focusedLabelColor = SemaforoGreen,
    unfocusedLabelColor = Color(0xFF94A3B8),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
