package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class OperationalStatus {
    OPTIMAL,
    WARNING,
    CRITICAL,
    OFF
}

data class SensorEvaluation(
    val status: OperationalStatus,
    val statusText: String,
    val healthPercentage: Float
)

data class LiveTelemetry(
    val rms: Float = 1.85f,
    val peakFrequency: Float = 49.2f,
    val peakToPeak: Float = 4.2f,
    val status: OperationalStatus = OperationalStatus.OPTIMAL,
    val statusText: String = "OPERACIÓN ÓPTIMA",
    val isPumpRunning: Boolean = true,
    val healthPercentage: Float = 96.0f,
    val vibrationHistory: List<Float> = emptyList(),
    val spectrum: FloatArray = FloatArray(64)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LiveTelemetry
        return rms == other.rms &&
                peakFrequency == other.peakFrequency &&
                status == other.status &&
                isPumpRunning == other.isPumpRunning &&
                healthPercentage == other.healthPercentage
    }

    override fun hashCode(): Int {
        var result = rms.hashCode()
        result = 31 * result + peakFrequency.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + isPumpRunning.hashCode()
        result = 31 * result + healthPercentage.hashCode()
        return result
    }
}

class HardwareSensorMonitor(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _telemetry = MutableStateFlow(LiveTelemetry())
    val telemetry: StateFlow<LiveTelemetry> = _telemetry.asStateFlow()

    private val sampleBuffer = FloatArray(128)
    private var sampleIndex = 0
    private var lastSampleTimeMs = 0L

    var baselineRms: Float = 1.85f
    var yellowMultiplier: Float = 1.35f
    var redMultiplier: Float = 2.0f

    private val historyPoints = mutableListOf<Float>()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        startListening()
    }

    fun startListening() {
        accelerometer?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val now = System.currentTimeMillis()
        val dt = now - lastSampleTimeMs
        if (dt < 10) return // Limit sampling to ~100Hz
        lastSampleTimeMs = now

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]

        // Dynamic acceleration magnitude excluding gravity vector
        val rawMagnitude = sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
        val dynamicAcc = kotlin.math.abs(rawMagnitude - 9.81f)

        sampleBuffer[sampleIndex] = dynamicAcc
        sampleIndex = (sampleIndex + 1) % sampleBuffer.size

        if (sampleIndex == 0) {
            coroutineScope.launch {
                processBuffer()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun processBuffer() {
        val n = sampleBuffer.size
        var sumSquares = 0.0
        var minVal = Float.MAX_VALUE
        var maxVal = Float.MIN_VALUE

        for (v in sampleBuffer) {
            sumSquares += (v * v)
            if (v < minVal) minVal = v
            if (v > maxVal) maxVal = v
        }

        val rms = sqrt(sumSquares / n).toFloat()
        val p2p = maxVal - minVal

        // FFT spectral analysis (~100Hz sample rate)
        val fftResult = FftEngine.computeFft(sampleBuffer.clone(), sampleRateHz = 100f)

        val evaluation = evaluateHealth(rms)
        val isPumpOn = rms > 0.4f

        synchronized(historyPoints) {
            historyPoints.add(rms)
            if (historyPoints.size > 50) {
                historyPoints.removeAt(0)
            }
        }

        _telemetry.value = LiveTelemetry(
            rms = rms,
            peakFrequency = if (isPumpOn) fftResult.peakFrequencyHz else 0f,
            peakToPeak = p2p,
            status = evaluation.status,
            statusText = evaluation.statusText,
            isPumpRunning = isPumpOn,
            healthPercentage = evaluation.healthPercentage,
            vibrationHistory = synchronized(historyPoints) { historyPoints.toList() },
            spectrum = fftResult.spectrum
        )
    }

    private fun evaluateHealth(currentRms: Float): SensorEvaluation {
        if (currentRms < 0.4f) {
            return SensorEvaluation(OperationalStatus.OFF, "BOMBA DETENIDA", 100f)
        }

        val yellowThreshold = baselineRms * yellowMultiplier
        val redThreshold = baselineRms * redMultiplier

        return when {
            currentRms < yellowThreshold -> {
                // Healthy: 90% to 100%
                val factor = (currentRms / yellowThreshold).coerceIn(0f, 1f)
                val health = 100f - (factor * 10f)
                SensorEvaluation(OperationalStatus.OPTIMAL, "OPERACIÓN ÓPTIMA", health)
            }
            currentRms < redThreshold -> {
                // Warning: 60% to 89%
                val range = redThreshold - yellowThreshold
                val progress = if (range > 0f) (currentRms - yellowThreshold) / range else 0.5f
                val health = 89f - (progress.coerceIn(0f, 1f) * 29f)
                SensorEvaluation(OperationalStatus.WARNING, "ALERTA PREVENTIVA (VIBRACIÓN ALTA)", health)
            }
            else -> {
                // Critical: 10% to 59%
                val excess = (currentRms - redThreshold) / baselineRms
                val health = max(10f, 59f - (excess * 20f))
                SensorEvaluation(OperationalStatus.CRITICAL, "PELIGRO CRÍTICO (DESALINEACIÓN/CAVITACIÓN)", health)
            }
        }
    }
}
