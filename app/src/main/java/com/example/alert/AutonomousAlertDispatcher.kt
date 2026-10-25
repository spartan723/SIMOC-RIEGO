package com.example.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val TAG = "AlertDispatcher"

data class DispatchResult(
    val smsSent: Boolean,
    val notificationSent: Boolean,
    val webhookSent: Boolean,
    val details: String
)

class AutonomousAlertDispatcher(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private var lastDispatchTimeMs = 0L
    private val debounceWindowMs = 60_000L // 1 minute debounce between automated dispatches

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "bombavigia_alerts",
                "Alertas Críticas BOMBAVIGIA",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de vibración anómala y parada de emergencia de bomba"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    suspend fun dispatchAlert(
        pumpName: String,
        sector: String,
        status: String,
        rms: Float,
        threshold: Float,
        phoneNumber: String,
        telegramToken: String? = null,
        telegramChatId: String? = null,
        webhookUrl: String? = null,
        forceBypassDebounce: Boolean = false
    ): DispatchResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!forceBypassDebounce && (now - lastDispatchTimeMs < debounceWindowMs)) {
            return@withContext DispatchResult(
                smsSent = false,
                notificationSent = false,
                webhookSent = false,
                details = "Alerta suprimida por ventana de antispam (último despacho hace ${(now - lastDispatchTimeMs)/1000}s)"
            )
        }
        lastDispatchTimeMs = now

        val message = "🚨 ALERTA BOMBAVIGIA: $pumpName en $sector registra estado $status. " +
                "Vibración: %.2f m/s² (Umbral: %.2f m/s²). Verifique acople o rodamientos urgentemente.".format(rms, threshold)

        var notifOk = false
        var smsOk = false
        var webhookOk = false

        // 1. Android Notification
        try {
            val builder = NotificationCompat.Builder(context, "bombavigia_alerts")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("BOMBAVIGIA: $status en $pumpName")
                .setContentText("RMS: %.2f m/s² supera umbral %.2f".format(rms, threshold))
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)

            notificationManager?.notify(101, builder.build())
            notifOk = true
        } catch (e: Exception) {
            Log.e(TAG, "Error posting notification", e)
        }

        // 2. Autonomous SMS
        if (phoneNumber.isNotBlank() && phoneNumber.length >= 7) {
            try {
                val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                smsOk = true
                Log.d(TAG, "Autonomous SMS dispatched to $phoneNumber")
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching SMS to $phoneNumber", e)
            }
        }

        // 3. Telegram Webhook (if configured)
        if (!telegramToken.isNullOrBlank() && !telegramChatId.isNullOrBlank()) {
            try {
                val encodedMsg = URLEncoder.encode(message, "UTF-8")
                val tgUrl = URL("https://api.telegram.org/bot$telegramToken/sendMessage?chat_id=$telegramChatId&text=$encodedMsg")
                val connection = (tgUrl.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                }
                if (connection.responseCode in 200..299) {
                    webhookOk = true
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error sending Telegram webhook", e)
            }
        }

        // 4. Generic Webhook (if configured)
        if (!webhookUrl.isNullOrBlank() && webhookUrl.startsWith("http")) {
            try {
                val url = URL(webhookUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }
                val jsonPayload = """{"alert":"$status","pump":"$pumpName","rms":$rms,"threshold":$threshold}"""
                connection.outputStream.use { os ->
                    os.write(jsonPayload.toByteArray())
                }
                if (connection.responseCode in 200..299) {
                    webhookOk = true
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error dispatching generic webhook", e)
            }
        }

        val summary = buildString {
            append("Notificación: ${if (notifOk) "Enviada" else "Falló"}; ")
            append("SMS ($phoneNumber): ${if (smsOk) "Despachado" else "No despachado"}; ")
            append("Webhook: ${if (webhookOk) "Recibido" else "Sin configurar"}")
        }

        DispatchResult(
            smsSent = smsOk,
            notificationSent = notifOk,
            webhookSent = webhookOk,
            details = summary
        )
    }
}
