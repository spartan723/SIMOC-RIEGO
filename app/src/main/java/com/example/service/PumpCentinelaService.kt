package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.alert.AutonomousAlertDispatcher
import com.example.data.AgroDatabase
import com.example.data.AgroRepository
import com.example.data.model.VibrationReading
import com.example.sensor.HardwareSensorMonitor
import com.example.sensor.OperationalStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class PumpCentinelaService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var wakeLock: PowerManager.WakeLock? = null
    private var monitorLoopJob: Job? = null

    private lateinit var sensorMonitor: HardwareSensorMonitor
    private lateinit var alertDispatcher: AutonomousAlertDispatcher
    private lateinit var repository: AgroRepository

    companion object {
        const val CHANNEL_ID = "simoc_pump_centinela_channel"
        const val NOTIFICATION_ID = 1001

        @Volatile
        var isServiceRunning: Boolean = false
            private set

        fun startService(context: Context) {
            val intent = Intent(context, PumpCentinelaService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, PumpCentinelaService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true

        val database = AgroDatabase.getDatabase(applicationContext, serviceScope)
        repository = AgroRepository(
            database.pumpDao(),
            database.vibrationReadingDao(),
            database.maintenanceRecordDao()
        )
        sensorMonitor = HardwareSensorMonitor(applicationContext)
        alertDispatcher = AutonomousAlertDispatcher(applicationContext)

        // Acquire WakeLock to guarantee uninterrupted CPU execution 24/7 in pump box
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "BOMBAVIGIA:PumpCentinelaWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(24 * 60 * 60 * 1000L) // 24 hours lock with renewal
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Iniciando Centinela 24/7..."))

        sensorMonitor.startListening()
        startMonitoringLoop()
    }

    private fun startMonitoringLoop() {
        monitorLoopJob = serviceScope.launch {
            var counter = 0
            while (isServiceRunning) {
                delay(5000) // Telemetry sample loop
                val telemetry = sensorMonitor.telemetry.value
                val pump = repository.getPump(1).firstOrNull() ?: continue

                counter++
                // Update persistent notification every 10 seconds
                if (counter % 2 == 0) {
                    val statusText = "RMS: ${String.format("%.2f", telemetry.rms)} m/s² | ${telemetry.status.name} (${String.format("%.1f", telemetry.ratioToBaseline)}x)"
                    updateNotification(statusText)
                }

                // Check for autonomous dispatch when in warning or critical state
                if (telemetry.status == OperationalStatus.CRITICAL || telemetry.status == OperationalStatus.WARNING) {
                    alertDispatcher.dispatchAutonomousAlert(
                        pump = pump,
                        telemetry = telemetry,
                        forceDispatch = false
                    )
                }

                // Periodic database persistence every 15 seconds
                if (counter % 3 == 0) {
                    repository.saveReading(
                        VibrationReading(
                            pumpId = pump.id,
                            timestamp = System.currentTimeMillis(),
                            rms = telemetry.rms,
                            peakFrequency = telemetry.peakFrequency,
                            peakToPeak = telemetry.peakToPeak,
                            status = telemetry.status.name,
                            isPumpOn = telemetry.isPumpRunning,
                            notes = "Centinela 24/7 - ${telemetry.isoClassification}",
                            mode = "BOMBA"
                        )
                    )
                }
            }
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BOMBAVIGIA: Centinela 24/7 Activo")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BOMBAVIGIA Centinela 24/7",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoreo ininterrumpido de vibraciones y fallas en bomba de riego"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        isServiceRunning = false
        monitorLoopJob?.cancel()
        sensorMonitor.stopListening()

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
