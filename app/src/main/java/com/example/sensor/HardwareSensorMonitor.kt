package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

enum class OperationalStatus {
    OFF,         // Pump is stopped (avoids false alarms while idle)
    NORMAL,      // Green: Normal safe operation
    WARNING,     // Yellow: Elevated vibration, wear detected
    CRITICAL     // Red: Immediate danger, bearing damage or severe imbalance
}

enum class PumpFaultType {
    NONE,
    LUBRICATION_NEEDED,
    BEARING_WEAR,
    IMPELLER_IMBALANCE,
    CAVITATION,
    MOTOR_STOPPED
}

enum class SimulationScenario {
    REAL_SENSOR,
    SIM_HEALTHY_PUMP,
    SIM_LUBRICATION_NEEDED,
    SIM_BEARING_WEAR,
    SIM_IMPELLER_IMBALANCE,
    SIM_PUMP_CAVITATION,
    SIM_PUMP_OFF
}

data class LiveTelemetry(
    val rms: Float = 0f,
    val peakFrequency: Float = 0f,
    val peakToPeak: Float = 0f,
    val isPumpRunning: Boolean = false,
    val status: OperationalStatus = OperationalStatus.NORMAL,
    val faultType: PumpFaultType = PumpFaultType.NONE,
    val ratioToBaseline: Float = 1.0f,
    val healthPercentage: Int = 100,
    val isoClassification: String = "Zona A: Condición Excelente",
    val statusMessage: String = "Inicializando sensores...",
    val rawX: Float = 0f,
    val rawY: Float = 0f,
    val rawZ: Float = 9.8f,
    val dynamicMagnitude: Float = 0f,
    val waveformSamples: List<Float> = emptyList(),
    val fftSpectrum: List<FftPoint> = emptyList(),
    val acousticTelemetry: AcousticTelemetry = AcousticTelemetry(),
    val isHardwareSensorAvailable: Boolean = true
)

class HardwareSensorMonitor(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _telemetry = MutableStateFlow(LiveTelemetry())
    val telemetry: StateFlow<LiveTelemetry> = _telemetry.asStateFlow()

    private var simulationScenario = SimulationScenario.REAL_SENSOR

    // Baseline configuration learned during healthy pump operation
    var baselineRms: Float = 1.90f
    var baselineFreq: Float = 49.5f
    var yellowThresholdMultiplier: Float = 1.35f
    var redThresholdMultiplier: Float = 2.0f

    // Gravity filter for linear acceleration isolation
    private val gravity = FloatArray(3) { 0f }
    private var hasGravityInitialized = false

    // Sliding window buffer (keeps dynamic measurements for RMS and peak calculations)
    private val windowCapacity = 64
    private val dynamicBuffer = FloatArray(windowCapacity)
    private var bufferIndex = 0
    private var sampleCount = 0

    // Recent waveform ring buffer for oscilloscope
    private val waveformBuffer = ArrayList<Float>(40)

    // Frequency zero crossing estimation
    private var lastCrossTimeMs: Long = 0
    private var crossCount = 0
    private var lastSignPositive = false
    private var estimatedFrequency = 49.5f

    // Alert rate limiting for haptic feedback
    private var lastHapticAlertMs: Long = 0

    init {
        repeat(30) { waveformBuffer.add(0f) }
    }

    fun startListening() {
        if (accelerometer != null && simulationScenario == SimulationScenario.REAL_SENSOR) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            _telemetry.value = _telemetry.value.copy(isHardwareSensorAvailable = true)
        } else {
            _telemetry.value = _telemetry.value.copy(
                isHardwareSensorAvailable = accelerometer != null
            )
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    fun setSimulationScenario(scenario: SimulationScenario) {
        simulationScenario = scenario
        if (scenario != SimulationScenario.REAL_SENSOR) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun calibrateCurrentAsBaseline() {
        val current = _telemetry.value
        if (current.rms > 0.25f) {
            baselineRms = current.rms
            baselineFreq = if (current.peakFrequency > 10f) current.peakFrequency else 49.5f
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (simulationScenario != SimulationScenario.REAL_SENSOR) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // High-pass filtering to isolate dynamic mechanical vibration from static gravity
        val alpha = 0.82f
        if (!hasGravityInitialized) {
            gravity[0] = x
            gravity[1] = y
            gravity[2] = z
            hasGravityInitialized = true
        } else {
            gravity[0] = alpha * gravity[0] + (1 - alpha) * x
            gravity[1] = alpha * gravity[1] + (1 - alpha) * y
            gravity[2] = alpha * gravity[2] + (1 - alpha) * z
        }

        val dynX = x - gravity[0]
        val dynY = y - gravity[1]
        val dynZ = z - gravity[2]
        val dynamicMagnitude = sqrt(dynX * dynX + dynY * dynY + dynZ * dynZ)

        // Store into circular buffer
        dynamicBuffer[bufferIndex] = dynamicMagnitude
        bufferIndex = (bufferIndex + 1) % windowCapacity
        if (sampleCount < windowCapacity) sampleCount++

        // Zero-crossing frequency estimation
        val isPositive = dynY > 0.05f
        if (isPositive != lastSignPositive) {
            crossCount++
            lastSignPositive = isPositive
            val nowMs = System.currentTimeMillis()
            if (nowMs - lastCrossTimeMs >= 1000) {
                estimatedFrequency = (crossCount / 2f) * (1000f / (nowMs - lastCrossTimeMs).coerceAtLeast(1))
                crossCount = 0
                lastCrossTimeMs = nowMs
            }
        }

        // Calculate RMS: sqrt( sum(x^2) / N )
        var sumSquares = 0f
        var maxPeak = 0f
        for (i in 0 until sampleCount) {
            val v = dynamicBuffer[i]
            sumSquares += v * v
            if (v > maxPeak) maxPeak = v
        }
        val calculatedRms = sqrt(sumSquares / sampleCount.coerceAtLeast(1))

        // Oscilloscope buffer
        synchronized(waveformBuffer) {
            if (waveformBuffer.size >= 40) waveformBuffer.removeAt(0)
            waveformBuffer.add(dynY)
        }

        processPumpTelemetry(
            rawX = x,
            rawY = y,
            rawZ = z,
            dynMag = dynamicMagnitude,
            rms = calculatedRms,
            peakFreq = if (estimatedFrequency in 10f..150f) estimatedFrequency else 49.8f,
            peakToPeak = maxPeak * 2f
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun updateSimulationTick(timeSeconds: Float) {
        if (simulationScenario == SimulationScenario.REAL_SENSOR) return

        when (simulationScenario) {
            SimulationScenario.SIM_HEALTHY_PUMP -> {
                // Smooth sine wave of 50 Hz motor with low noise
                val noise = (sin(timeSeconds * 50.0 * 2 * Math.PI) * 1.8 + sin(timeSeconds * 100.0) * 0.3 + (Math.random() - 0.5) * 0.3).toFloat()
                val rms = 1.85f + (sin(timeSeconds * 0.5) * 0.06).toFloat()
                pushSimulationWave(noise)
                processPumpTelemetry(
                    rawX = 0.1f, rawY = 0.2f, rawZ = 9.8f,
                    dynMag = abs(noise),
                    rms = rms,
                    peakFreq = 49.9f,
                    peakToPeak = 3.5f
                )
            }
            SimulationScenario.SIM_LUBRICATION_NEEDED -> {
                // Mild high-frequency vibration
                val noise = (sin(timeSeconds * 50.0 * 2 * Math.PI) * 2.2 + sin(timeSeconds * 120.0) * 0.8 + (Math.random() - 0.5) * 0.5).toFloat()
                val rms = 2.45f + (sin(timeSeconds * 0.7) * 0.12).toFloat()
                pushSimulationWave(noise)
                processPumpTelemetry(
                    rawX = 0.3f, rawY = 0.4f, rawZ = 9.7f,
                    dynMag = abs(noise),
                    rms = rms,
                    peakFreq = 50.4f,
                    peakToPeak = 4.8f
                )
            }
            SimulationScenario.SIM_BEARING_WEAR -> {
                // Higher vibration with strong 2nd/3rd harmonics typical of bearing spalling
                val noise = (sin(timeSeconds * 50.0 * 2 * Math.PI) * 2.9 + sin(timeSeconds * 150.0) * 1.6 + (Math.random() - 0.5) * 0.9).toFloat()
                val rms = 3.20f + (sin(timeSeconds * 0.9) * 0.25).toFloat()
                pushSimulationWave(noise)
                processPumpTelemetry(
                    rawX = 0.7f, rawY = 0.8f, rawZ = 9.5f,
                    dynMag = abs(noise),
                    rms = rms,
                    peakFreq = 53.6f,
                    peakToPeak = 6.8f
                )
            }
            SimulationScenario.SIM_IMPELLER_IMBALANCE -> {
                // Large 1X RPM dominant vibration (>2x baseline)
                val noise = (sin(timeSeconds * 50.0 * 2 * Math.PI) * 4.6 + (Math.random() - 0.5) * 1.2).toFloat()
                val rms = 4.70f + (sin(timeSeconds * 1.1) * 0.35).toFloat()
                pushSimulationWave(noise)
                processPumpTelemetry(
                    rawX = 1.4f, rawY = 1.6f, rawZ = 9.1f,
                    dynMag = abs(noise),
                    rms = rms,
                    peakFreq = 48.2f,
                    peakToPeak = 9.6f
                )
            }
            SimulationScenario.SIM_PUMP_CAVITATION -> {
                // Turbulent random high-amplitude pops (imploding vapor bubbles)
                val pop = if (Math.random() > 0.7) (Math.random() * 4.0).toFloat() else 0f
                val noise = (sin(timeSeconds * 50.0 * 2 * Math.PI) * 3.2 + pop + (Math.random() - 0.5) * 2.0).toFloat()
                val rms = 4.25f + (sin(timeSeconds * 1.5) * 0.4).toFloat()
                pushSimulationWave(noise)
                processPumpTelemetry(
                    rawX = 1.1f, rawY = 1.3f, rawZ = 9.3f,
                    dynMag = abs(noise),
                    rms = rms,
                    peakFreq = 51.0f,
                    peakToPeak = 9.1f
                )
            }
            SimulationScenario.SIM_PUMP_OFF -> {
                val noise = ((Math.random() - 0.5) * 0.05).toFloat()
                pushSimulationWave(noise)
                processPumpTelemetry(
                    rawX = 0.01f, rawY = 0.01f, rawZ = 9.81f,
                    dynMag = 0.02f,
                    rms = 0.04f,
                    peakFreq = 0f,
                    peakToPeak = 0.08f
                )
            }
            SimulationScenario.REAL_SENSOR -> {}
        }
    }

    private fun pushSimulationWave(value: Float) {
        synchronized(waveformBuffer) {
            if (waveformBuffer.size >= 40) waveformBuffer.removeAt(0)
            waveformBuffer.add(value)
        }
    }

    private fun processPumpTelemetry(
        rawX: Float,
        rawY: Float,
        rawZ: Float,
        dynMag: Float,
        rms: Float,
        peakFreq: Float,
        peakToPeak: Float
    ) {
        val samples: List<Float>
        synchronized(waveformBuffer) {
            samples = waveformBuffer.toList()
        }

        val isRunning = rms >= 0.20f
        val ratio = if (baselineRms > 0.01f) rms / baselineRms else 1.0f

        val (status, fault, health, isoZone, msg) = when {
            !isRunning -> Pentuple(
                OperationalStatus.OFF,
                PumpFaultType.MOTOR_STOPPED,
                100,
                "Bomba en Reposo",
                "Bomba apagada. En espera de inicio de ciclo de riego."
            )
            ratio < 1.18f -> Pentuple(
                OperationalStatus.NORMAL,
                PumpFaultType.NONE,
                98,
                "Zona A: Excelente (ISO 10816)",
                "Bomba normal. Operación y rodamientos perfectamente estables."
            )
            ratio < yellowThresholdMultiplier -> Pentuple(
                OperationalStatus.NORMAL,
                PumpFaultType.LUBRICATION_NEEDED,
                85,
                "Zona B: Bueno / Admisible",
                "Operación admisible. Sugerir engrase preventivo en próxima mantención."
            )
            ratio < redThresholdMultiplier -> Pentuple(
                OperationalStatus.WARNING,
                PumpFaultType.BEARING_WEAR,
                62,
                "Zona C: Alerta de Mantenimiento",
                "Vibración elevada (${String.format("%.1f", ratio)}x sobre línea base). Revisar rodamientos esta semana."
            )
            ratio < 2.5f -> {
                triggerHapticWarning()
                Pentuple(
                    OperationalStatus.CRITICAL,
                    PumpFaultType.IMPELLER_IMBALANCE,
                    35,
                    "Zona D: Peligro de Daño",
                    "¡PELIGRO! Desbalance severo o rodamiento dañado (${String.format("%.1f", ratio)}x). Detener y revisar."
                )
            }
            else -> {
                triggerHapticWarning()
                Pentuple(
                    OperationalStatus.CRITICAL,
                    PumpFaultType.CAVITATION,
                    20,
                    "Zona D: Cavitación Crítica",
                    "¡ALARMA CRÍTICA! Vibración extrema. Posible entrada de aire o daño en impulsor."
                )
            }
        }

        // Compute real Fast Fourier Transform (FFT) on 64-sample buffer
        val fftPoints = FftEngine.computeFft(dynamicBuffer, 100f)

        _telemetry.value = LiveTelemetry(
            rms = rms,
            peakFrequency = if (isRunning) peakFreq else 0f,
            peakToPeak = peakToPeak,
            isPumpRunning = isRunning,
            status = status,
            faultType = fault,
            ratioToBaseline = ratio,
            healthPercentage = health,
            isoClassification = isoZone,
            statusMessage = msg,
            rawX = rawX,
            rawY = rawY,
            rawZ = rawZ,
            dynamicMagnitude = dynMag,
            waveformSamples = samples,
            fftSpectrum = fftPoints,
            acousticTelemetry = _telemetry.value.acousticTelemetry,
            isHardwareSensorAvailable = accelerometer != null
        )
    }

    fun updateAcousticTelemetry(acoustic: AcousticTelemetry) {
        _telemetry.value = _telemetry.value.copy(acousticTelemetry = acoustic)
    }

    private fun triggerHapticWarning() {
        val now = System.currentTimeMillis()
        if (now - lastHapticAlertMs > 3000) {
            lastHapticAlertMs = now
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 250, 100, 250), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(350)
                    }
                }
            } catch (_: Exception) {}
        }
    }
}

private data class Pentuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
