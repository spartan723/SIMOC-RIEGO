package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onDismiss: () -> Unit,
    onSave: (name: String, sector: String, phone: String, yellowThresh: Float, redThresh: Float) -> Unit
) {
    var name by remember { mutableStateOf(currentPump?.name ?: "Bomba de Riego Pozo 01") }
    var sector by remember { mutableStateOf(currentPump?.sector ?: "Cooperativa Agrícola") }
    var phone by remember { mutableStateOf(currentPump?.alertPhoneNumber ?: "+5491123456789") }
    var yellowThresh by remember { mutableFloatStateOf(currentPump?.yellowThresholdMultiplier ?: 1.35f) }
    var redThresh by remember { mutableFloatStateOf(currentPump?.redThresholdMultiplier ?: 2.0f) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Parámetros del Vigía", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Identificador de la Bomba") },
                    modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Umbral Alerta Amarilla: ${String.format("%.2f", yellowThresh)}x línea base",
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
                    text = "Umbral Alarma Roja (Crítica): ${String.format("%.2f", redThresh)}x línea base",
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, sector, phone, yellowThresh, redThresh)
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
