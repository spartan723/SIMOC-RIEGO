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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.ui.theme.SemaforoGreen
import com.example.ui.theme.SemaforoRed
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
    onTestAutonomousDispatch: () -> Unit,
    onSignOut: () -> Unit,
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
        autoSms: Boolean
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141F17),
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configuración",
                    tint = SemaforoGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Configuración de Bomba y Estación",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
                // Device ID Information Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D140F), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E3A2B), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ID ÚNICO DE ESTACIÓN",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFA7F3D0)
                            )
                            Text(
                                text = deviceId,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            Text(
                                text = deviceModel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("ID de Dispositivo", deviceId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "ID de estación copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar ID",
                                tint = SemaforoGreen
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la Bomba") },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("input_pump_name")
                )

                OutlinedTextField(
                    value = sector,
                    onValueChange = { sector = it },
                    label = { Text("Sector / Cuadro Agrícola") },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("input_pump_sector")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono de Alerta SMS") },
                    colors = textFieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("input_alert_phone")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = yellowThresh,
                        onValueChange = { yellowThresh = it },
                        label = { Text("Mult. Amarillo (x)") },
                        colors = textFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = redThresh,
                        onValueChange = { redThresh = it },
                        label = { Text("Mult. Rojo (x)") },
                        colors = textFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Envío automático de SMS",
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

                Spacer(modifier = Modifier.height(6.dp))

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

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SemaforoRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Cerrar Sesión")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cerrar Sesión de Google")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val y = yellowThresh.toFloatOrNull() ?: 1.35f
                    val r = redThresh.toFloatOrNull() ?: 2.0f
                    onSave(name, sector, phone, y, r, tgToken, tgChat, webhook, autoSms)
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
                Text("Cancelar", color = Color(0xFF94A3B8))
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
