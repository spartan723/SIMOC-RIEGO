package com.example.alert

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.data.model.PumpProfile
import com.example.sensor.LiveTelemetry
import com.example.sensor.OperationalStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class DispatchResult(
    val isSuccess: Boolean,
    val method: String, // "SMS", "TELEGRAM", "WEBHOOK"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AutonomousAlertDispatcher(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Cooldown trackers (prevent spamming alerts continuously)
    private var lastSmsSentTimeMs: Long = 0
    private var lastTelegramSentTimeMs: Long = 0
    private val alertCooldownMs: Long = 15 * 60 * 1000L // 15 minutes between automated alerts

    suspend fun dispatchAutonomousAlert(
        pump: PumpProfile,
        telemetry: LiveTelemetry,
        telegramBotToken: String = "",
        telegramChatId: String = "",
        webhookUrl: String = "",
        forceDispatch: Boolean = false
    ): List<DispatchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DispatchResult>()
        val now = System.currentTimeMillis()

        val alertText = "🚨 ALERTA BOMBAVIGIA\n" +
                "Bomba: ${pump.name} (${pump.sector})\n" +
                "Estado: ${telemetry.status.name} (${telemetry.faultType.name})\n" +
                "RMS: ${String.format("%.2f", telemetry.rms)} m/s² (Base: ${String.format("%.2f", pump.baselineRms)} m/s²)\n" +
                "Frecuencia: ${String.format("%.1f", telemetry.peakFrequency)} Hz | Salud: ${telemetry.healthPercentage}%\n" +
                "Acción: ${telemetry.statusMessage}"

        // 1. Autonomous SMS Dispatch (via SmsManager)
        if (pump.alertPhoneNumber.isNotBlank()) {
            if (forceDispatch || (now - lastSmsSentTimeMs > alertCooldownMs)) {
                val hasSmsPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED

                if (hasSmsPermission) {
                    try {
                        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.getSystemService(SmsManager::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            SmsManager.getDefault()
                        }

                        val cleanPhone = pump.alertPhoneNumber.filter { it.isDigit() || it == '+' }
                        smsManager.sendTextMessage(cleanPhone, null, alertText, null, null)
                        lastSmsSentTimeMs = now
                        results.add(DispatchResult(true, "SMS", "SMS autónomo enviado con éxito a $cleanPhone"))
                    } catch (e: Exception) {
                        results.add(DispatchResult(false, "SMS", "Error enviando SMS: ${e.message}"))
                    }
                } else {
                    results.add(DispatchResult(false, "SMS", "Permiso SEND_SMS no concedido"))
                }
            } else {
                results.add(DispatchResult(false, "SMS", "SMS en enfriamiento (cooldown)"))
            }
        }

        // 2. Telegram Bot Dispatch
        if (telegramBotToken.isNotBlank() && telegramChatId.isNotBlank()) {
            if (forceDispatch || (now - lastTelegramSentTimeMs > alertCooldownMs)) {
                try {
                    val url = "https://api.telegram.org/bot$telegramBotToken/sendMessage"
                    val json = """
                        {
                            "chat_id": "$telegramChatId",
                            "text": "${alertText.replace("\n", "\\n")}",
                            "parse_mode": "HTML"
                        }
                    """.trimIndent()

                    val body = json.toRequestBody("application/json".toMediaType())
                    val request = Request.Builder().url(url).post(body).build()
                    val response = httpClient.newCall(request).execute()

                    if (response.isSuccessful) {
                        lastTelegramSentTimeMs = now
                        results.add(DispatchResult(true, "TELEGRAM", "Mensaje enviado al grupo/canal de Telegram"))
                    } else {
                        results.add(DispatchResult(false, "TELEGRAM", "Telegram error: HTTP ${response.code}"))
                    }
                    response.close()
                } catch (e: Exception) {
                    results.add(DispatchResult(false, "TELEGRAM", "Error conectando con Telegram: ${e.message}"))
                }
            }
        }

        // 3. Custom Webhook Dispatch
        if (webhookUrl.isNotBlank() && webhookUrl.startsWith("http")) {
            try {
                val jsonPayload = """
                    {
                        "source": "BOMBAVIGIA",
                        "pumpId": ${pump.id},
                        "pumpName": "${pump.name}",
                        "timestamp": $now,
                        "rms": ${telemetry.rms},
                        "baselineRms": ${pump.baselineRms},
                        "ratio": ${telemetry.ratioToBaseline},
                        "peakFrequency": ${telemetry.peakFrequency},
                        "status": "${telemetry.status.name}",
                        "faultType": "${telemetry.faultType.name}",
                        "healthPercentage": ${telemetry.healthPercentage}
                    }
                """.trimIndent()

                val body = jsonPayload.toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(webhookUrl).post(body).build()
                val response = httpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    results.add(DispatchResult(true, "WEBHOOK", "Telemetría enviada a webhook remoto"))
                } else {
                    results.add(DispatchResult(false, "WEBHOOK", "Webhook error: HTTP ${response.code}"))
                }
                response.close()
            } catch (e: Exception) {
                results.add(DispatchResult(false, "WEBHOOK", "Error conectando con Webhook: ${e.message}"))
            }
        }

        results
    }
}
