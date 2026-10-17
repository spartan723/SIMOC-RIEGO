package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PumpProfile
import com.example.ui.theme.SemaforoGreen
import com.example.ui.theme.SemaforoRed
import com.example.ui.theme.SemaforoYellow

@Composable
fun SettingsDialog(
    currentPump: PumpProfile?,
    telegramTokenInit: String = "",
    telegramChatInit: String = "",
    webhookUrlInit: String = "",
    autoSmsInit: Boolean = false,
    autoBootInit: Boolean = true,
    onTestAutonomousDispatch: () -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        sector: String,
        phone: String,
        yellowThresh: Float,
        redThresh: Float,
        telegramToken: String,
        telegramChat: String,
        webhookUrl: String,
        autoSms: Boolean,
        autoBoot: Boolean
    ) -> Unit
) {
    var name by remember { mutableStateOf(currentPump?.name ?: "Bomba de Riego Pozo 01") }
    var sector by remember { mutableStateOf(currentPump?.sector ?: "Cooperativa Agrícola") }
    var phone by remember { mutableStateOf(currentPump?.alertPhoneNumber ?: "+5491123456789") }
    var yellowThresh by remember { mutableFloatStateOf(currentPump?.yellowThresholdMultiplier ?: 1.35f) }
    var redThresh by remember { mutableFloatStateOf(currentPump?.redThresholdMultiplier ?: 2.0f) }

    var telegramToken by remember { mutableStateOf(telegramTokenInit) }
    var telegramChat by remember { mutableStateOf(telegramChatInit) }
    var webhookUrl by remember { mutableStateOf(webhookUrlInit) }
    var autoSms by remember { mutableStateOf(autoSmsInit) }
    var autoBoot by remember { mutableStateOf(autoBootInit) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Parámetros Industriales 24/7", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "DATOS DE LA BOMBA",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = Color(0xFF94A3B8)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Identificador de la Bomba") },
                    modifier = Modifier.fillMaxWidth().testTag("input_pump_name")
                )

                OutlinedTextField(
                    value = sector,
                    onValueChange = { sector = it },
                    label = { Text("Comunidad o Sector de Riego") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Teléfono de Aviso (WhatsApp / SMS)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("input_pump_phone")
                )

                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "UMBRALES DE VIBRACIÓN (ISO 10816)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = Color(0xFF94A3B8)
                )

                Text(
                    text = "Alerta Amarilla (Mantenimiento): ${String.format("%.2f", yellowThresh)}x línea base",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SemaforoYellow
                )
                Slider(
                    value = yellowThresh,
                    onValueChange = { yellowThresh = it },
                    valueRange = 1.15f..1.80f,
                    colors = SliderDefaults.colors(thumbColor = SemaforoYellow, activeTrackColor = SemaforoYellow)
                )

                Text(
                    text = "Alarma Roja (Peligro Crítico): ${String.format("%.2f", redThresh)}x línea base",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SemaforoRed
                )
                Slider(
                    value = redThresh,
                    onValueChange = { redThresh = it },
                    valueRange = 1.80f..3.0f,
                    colors = SliderDefaults.colors(thumbColor = SemaforoRed, activeTrackColor = SemaforoRed)
                )

                HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "AUTOMATIZACIÓN 24/7 EN CAMPO",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                    color = Color(0xFF94A3B8)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Arranque tras Corte Eléctrico", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Inicia BOMBAVIGIA automáticamente al recibir corriente", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                    Switch(
                        checked = autoBoot,
                        onCheckedChange = { autoBoot = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = SemaforoGreen, checkedTrackColor = Color(0xFF1B3825))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Despacho Automático SMS a Deshoras", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Envía SMS sin clics si la vibración crítica dura >30s", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                    Switch(
                        checked = autoSms,
                        onCheckedChange = { autoSms = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = SemaforoGreen, checkedTrackColor = Color(0xFF1B3825))
                    )
                }

                OutlinedTextField(
                    value = telegramToken,
                    onValueChange = { telegramToken = it },
                    label = { Text("Telegram Bot Token (Opcional)") },
                    placeholder = { Text("123456:ABC-DEF...") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = telegramChat,
                    onValueChange = { telegramChat = it },
                    label = { Text("Telegram Chat ID / Canal") },
                    placeholder = { Text("-1001234567890") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = { webhookUrl = it },
                    label = { Text("Webhook URL Remoto (HTTP POST)") },
                    placeholder = { Text("https://mi-cooperativa.org/api/bomba") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = onTestAutonomousDispatch,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Probar Despacho Autónomo Ahora")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, sector, phone, yellowThresh, redThresh, telegramToken, telegramChat, webhookUrl, autoSms, autoBoot)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SemaforoGreen)
            ) {
                Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
