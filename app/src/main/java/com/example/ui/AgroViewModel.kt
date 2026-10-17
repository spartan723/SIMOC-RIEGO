package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiDiagnosticEngine
import com.example.ai.GeminiDiagnosticResult
import com.example.alert.AutonomousAlertDispatcher
import com.example.alert.DispatchResult
import com.example.data.AgroDatabase
import com.example.data.AgroRepository
import com.example.data.model.MaintenanceRecord
import com.example.data.model.PumpProfile
import com.example.data.model.VibrationReading
import com.example.sensor.AcousticCavitationDetector
import com.example.sensor.HardwareSensorMonitor
import com.example.sensor.LiveTelemetry
import com.example.sensor.OperationalStatus
import com.example.sensor.PumpFaultType
import com.example.sensor.SimulationScenario
import com.example.service.PumpCentinelaService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalibrationState(
    val isCalibrating: Boolean = false,
    val progressSeconds: Int = 0,
    val totalSeconds: Int = 10,
    val capturedSamples: List<Float> = emptyList()
)

class AgroViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("simoc_centinela_prefs", Context.MODE_PRIVATE)

    private val database = AgroDatabase.getDatabase(application, viewModelScope)
    private val repository = AgroRepository(
        database.pumpDao(),
        database.vibrationReadingDao(),
        database.maintenanceRecordDao()
    )

    val sensorMonitor = HardwareSensorMonitor(application)
    val telemetry: StateFlow<LiveTelemetry> = sensorMonitor.telemetry

    val currentPump: StateFlow<PumpProfile?> = repository.getPump(1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentReadings: StateFlow<List<VibrationReading>> = repository.getRecentReadings(1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alertEvents: StateFlow<List<VibrationReading>> = repository.getAlertEvents(1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val maintenanceRecords: StateFlow<List<MaintenanceRecord>> = repository.getMaintenanceRecords(1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSavings: StateFlow<Double?> = repository.getTotalSavings(1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 830.0)

    private val _currentScenario = MutableStateFlow(SimulationScenario.REAL_SENSOR)
    val currentScenario: StateFlow<SimulationScenario> = _currentScenario.asStateFlow()

    private val _calibrationState = MutableStateFlow(CalibrationState())
    val calibrationState: StateFlow<CalibrationState> = _calibrationState.asStateFlow()

    private val _keepScreenAwake = MutableStateFlow(true)
    val keepScreenAwake: StateFlow<Boolean> = _keepScreenAwake.asStateFlow()

    // 24/7 Foreground Service State
    private val _isServiceRunning = MutableStateFlow(PumpCentinelaService.isServiceRunning)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    // OLED Burn-in Protection / Modo Centinela
    private val _isScreenSaverActive = MutableStateFlow(false)
    val isScreenSaverActive: StateFlow<Boolean> = _isScreenSaverActive.asStateFlow()

    // Acoustic Cavitation Detector
    private val acousticDetector = AcousticCavitationDetector(application)
    private val _isSamplingAudio = MutableStateFlow(false)
    val isSamplingAudio: StateFlow<Boolean> = _isSamplingAudio.asStateFlow()

    // Gemini AI Diagnostic
    private val geminiEngine = GeminiDiagnosticEngine()
    private val _geminiDiagnostic = MutableStateFlow<GeminiDiagnosticResult?>(null)
    val geminiDiagnostic: StateFlow<GeminiDiagnosticResult?> = _geminiDiagnostic.asStateFlow()

    private val _isLoadingAi = MutableStateFlow(false)
    val isLoadingAi: StateFlow<Boolean> = _isLoadingAi.asStateFlow()

    // Autonomous Alert Dispatcher
    private val alertDispatcher = AutonomousAlertDispatcher(application)
    private val _lastDispatchResults = MutableStateFlow<List<DispatchResult>>(emptyList())
    val lastDispatchResults: StateFlow<List<DispatchResult>> = _lastDispatchResults.asStateFlow()

    // Configurable Settings
    var telegramBotToken: String
        get() = prefs.getString("telegram_bot_token", "") ?: ""
        set(value) = prefs.edit().putString("telegram_bot_token", value).apply()

    var telegramChatId: String
        get() = prefs.getString("telegram_chat_id", "") ?: ""
        set(value) = prefs.edit().putString("telegram_chat_id", value).apply()

    var webhookUrl: String
        get() = prefs.getString("webhook_url", "") ?: ""
        set(value) = prefs.edit().putString("webhook_url", value).apply()

    var autoSmsEnabled: Boolean
        get() = prefs.getBoolean("auto_sms_enabled", false)
        set(value) = prefs.edit().putBoolean("auto_sms_enabled", value).apply()

    var autoBootEnabled: Boolean
        get() = prefs.getBoolean("auto_boot_enabled", true)
        set(value) = prefs.edit().putBoolean("auto_boot_enabled", value).apply()

    private var simulationTickerJob: Job? = null
    private var autoSaveJob: Job? = null

    init {
        sensorMonitor.startListening()

        // Sync baseline from repository
        viewModelScope.launch {
            repository.getPump(1).collect { pump ->
                if (pump != null) {
                    sensorMonitor.baselineRms = pump.baselineRms
                    sensorMonitor.baselineFreq = pump.baselineFreq
                    sensorMonitor.yellowThresholdMultiplier = pump.yellowThresholdMultiplier
                    sensorMonitor.redThresholdMultiplier = pump.redThresholdMultiplier
                }
            }
        }

        // Auto Wake-Up: If in Centinela screen saver and status changes to Warning or Critical, spring awake
        viewModelScope.launch {
            telemetry.collect { t ->
                if (_isScreenSaverActive.value && (t.status == OperationalStatus.WARNING || t.status == OperationalStatus.CRITICAL)) {
                    _isScreenSaverActive.value = false
                }
            }
        }

        startAutoLogging()
    }

    fun setSimulationScenario(scenario: SimulationScenario) {
        _currentScenario.value = scenario
        sensorMonitor.setSimulationScenario(scenario)

        simulationTickerJob?.cancel()
        if (scenario != SimulationScenario.REAL_SENSOR) {
            simulationTickerJob = viewModelScope.launch {
                var elapsed = 0f
                while (true) {
                    delay(50)
                    elapsed += 0.05f
                    sensorMonitor.updateSimulationTick(elapsed)
                }
            }
        }
    }

    fun toggleKeepScreenAwake() {
        _keepScreenAwake.value = !_keepScreenAwake.value
    }

    fun setScreenSaverActive(active: Boolean) {
        _isScreenSaverActive.value = active
    }

    fun toggleCentinelaService(context: Context) {
        if (_isServiceRunning.value) {
            PumpCentinelaService.stopService(context)
            _isServiceRunning.value = false
            Toast.makeText(context, "Servicio Centinela 24/7 detenido", Toast.LENGTH_SHORT).show()
        } else {
            PumpCentinelaService.startService(context)
            _isServiceRunning.value = true
            Toast.makeText(context, "Servicio Centinela 24/7 activado en segundo plano", Toast.LENGTH_SHORT).show()
        }
    }

    fun sampleAcousticNoise() {
        if (_isSamplingAudio.value) return
        viewModelScope.launch {
            _isSamplingAudio.value = true
            val isSimCavitation = _currentScenario.value == SimulationScenario.SIM_PUMP_CAVITATION
            val acoustic = acousticDetector.sampleAcoustics(isSimCavitation)
            sensorMonitor.updateAcousticTelemetry(acoustic)
            _isSamplingAudio.value = false
        }
    }

    fun requestAiDiagnostic(customApiKey: String? = null) {
        if (_isLoadingAi.value) return
        val pump = currentPump.value ?: return
        val currentTelemetry = telemetry.value

        viewModelScope.launch {
            _isLoadingAi.value = true
            val result = geminiEngine.analyzePumpHealth(pump, currentTelemetry, customApiKey)
            _geminiDiagnostic.value = result
            _isLoadingAi.value = false
        }
    }

    fun testAutonomousDispatch() {
        val pump = currentPump.value ?: return
        val currentTelemetry = telemetry.value

        viewModelScope.launch {
            val results = alertDispatcher.dispatchAutonomousAlert(
                pump = pump,
                telemetry = currentTelemetry,
                telegramBotToken = telegramBotToken,
                telegramChatId = telegramChatId,
                webhookUrl = webhookUrl,
                forceDispatch = true
            )
            _lastDispatchResults.value = results
            Toast.makeText(
                getApplication(),
                "Despacho ejecutado: ${results.count { it.isSuccess }} exitosos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun startCalibration() {
        if (_calibrationState.value.isCalibrating) return

        viewModelScope.launch {
            _calibrationState.value = CalibrationState(
                isCalibrating = true,
                progressSeconds = 0,
                totalSeconds = 10,
                capturedSamples = emptyList()
            )

            val samples = mutableListOf<Float>()
            for (sec in 1..10) {
                delay(1000)
                samples.add(telemetry.value.rms)
                _calibrationState.value = _calibrationState.value.copy(
                    progressSeconds = sec,
                    capturedSamples = samples.toList()
                )
            }

            val avgRms = if (samples.isNotEmpty()) samples.average().toFloat().coerceAtLeast(0.4f) else 1.90f
            val avgFreq = if (telemetry.value.peakFrequency > 10f) telemetry.value.peakFrequency else 49.5f

            sensorMonitor.baselineRms = avgRms
            sensorMonitor.baselineFreq = avgFreq
            repository.updateBaseline(1, avgRms, avgFreq)

            _calibrationState.value = CalibrationState(isCalibrating = false)

            Toast.makeText(
                getApplication(),
                "Calibración exitosa: Línea base fijada en ${String.format("%.2f", avgRms)} m/s²",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun startAutoLogging() {
        autoSaveJob = viewModelScope.launch {
            while (true) {
                delay(15000)
                val t = telemetry.value
                val statusStr = when (t.status) {
                    OperationalStatus.OFF -> "APAGADA"
                    OperationalStatus.NORMAL -> "NORMAL"
                    OperationalStatus.WARNING -> "ADVERTENCIA"
                    OperationalStatus.CRITICAL -> "CRITICO"
                }

                repository.saveReading(
                    VibrationReading(
                        pumpId = 1,
                        timestamp = System.currentTimeMillis(),
                        rms = t.rms,
                        peakFrequency = t.peakFrequency,
                        peakToPeak = t.peakToPeak,
                        status = statusStr,
                        isPumpOn = t.isPumpRunning,
                        notes = "${t.isoClassification} - ${t.faultType.name}",
                        mode = "BOMBA"
                    )
                )
            }
        }
    }

    fun sendAlert(context: Context, viaWhatsApp: Boolean = true) {
        val t = telemetry.value
        val pump = currentPump.value
        val pumpName = pump?.name ?: "Bomba de Riego Pozo 01"
        val sector = pump?.sector ?: "Sector Riego"
        val phone = pump?.alertPhoneNumber ?: "+5491123456789"

        val faultDescription = when (t.faultType) {
            PumpFaultType.BEARING_WEAR -> "Desgaste de rodamiento o falta de lubricación"
            PumpFaultType.IMPELLER_IMBALANCE -> "Desbalance severo en impulsor / eje"
            PumpFaultType.CAVITATION -> "Cavitación o entrada de aire en la succión"
            PumpFaultType.LUBRICATION_NEEDED -> "Lubricación requerida próximamente"
            PumpFaultType.MOTOR_STOPPED -> "Motor apagado / fuera de servicio"
            PumpFaultType.NONE -> "Operación en parámetros normales"
        }

        val message = when (t.status) {
            OperationalStatus.CRITICAL ->
                "🚨 *ALERTA CRÍTICA BOMBAVIGIA*\n" +
                "💧 Bomba: $pumpName ($sector)\n" +
                "⚠️ Diagnóstico: $faultDescription\n" +
                "📊 Vibración actual: ${String.format("%.2f", t.rms)} m/s² (${String.format("%.1f", t.ratioToBaseline)}x sobre línea base).\n" +
                "⚙️ Frecuencia: ${String.format("%.1f", t.peakFrequency)} Hz | Salud: ${t.healthPercentage}%\n" +
                "🔧 Acción: Detener equipo y revisar rodamientos/anclaje antes de quemar bobinado."

            OperationalStatus.WARNING ->
                "⚠️ *AVISO PREVENTIVO BOMBAVIGIA*\n" +
                "💧 Bomba: $pumpName ($sector)\n" +
                "🔍 Detección: Vibración elevada (${String.format("%.1f", t.ratioToBaseline)}x sobre normal).\n" +
                "📊 RMS: ${String.format("%.2f", t.rms)} m/s² (Base: ${String.format("%.2f", sensorMonitor.baselineRms)} m/s²).\n" +
                "🛠️ Acción: Programar engrase e inspección mecánica durante esta semana."

            OperationalStatus.NORMAL ->
                "✅ *REPORTE DE ESTADO BOMBAVIGIA*\n" +
                "💧 Bomba: $pumpName operando óptimamente.\n" +
                "📊 Vibración: ${String.format("%.2f", t.rms)} m/s² | Salud: ${t.healthPercentage}%\n" +
                "👍 Rodamientos y eje en condición estable (${t.isoClassification})."

            OperationalStatus.OFF ->
                "ℹ️ *ESTADO DE REPOSO BOMBAVIGIA*\n" +
                "💧 Bomba: $pumpName se encuentra en reposo (apagada)."
        }

        try {
            if (viaWhatsApp) {
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=${phone.filter { it.isDigit() }}&text=${Uri.encode(message)}")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } else {
                val smsUri = Uri.parse("smsto:${phone.filter { it.isDigit() || it == '+' }}")
                val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                    putExtra("sms_body", message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir app de mensajería: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun addMaintenance(actionType: String, description: String, estimatedSaved: Double) {
        viewModelScope.launch {
            repository.addMaintenanceRecord(
                MaintenanceRecord(
                    pumpId = 1,
                    timestamp = System.currentTimeMillis(),
                    actionType = actionType,
                    description = description,
                    estimatedRepairSavedUsd = estimatedSaved,
                    technicianName = "Productor / Operador"
                )
            )
            Toast.makeText(getApplication(), "Mantenimiento registrado. ¡Ahorro sumado!", Toast.LENGTH_SHORT).show()
        }
    }

    fun updatePumpSettings(
        name: String,
        sector: String,
        phone: String,
        yellowThresh: Float,
        redThresh: Float,
        telegramToken: String = telegramBotToken,
        telegramChat: String = telegramChatId,
        customWebhook: String = webhookUrl,
        enableAutoSms: Boolean = autoSmsEnabled,
        enableAutoBoot: Boolean = autoBootEnabled
    ) {
        val current = currentPump.value ?: return
        telegramBotToken = telegramToken
        telegramChatId = telegramChat
        webhookUrl = customWebhook
        autoSmsEnabled = enableAutoSms
        autoBootEnabled = enableAutoBoot

        viewModelScope.launch {
            val updated = current.copy(
                name = name,
                sector = sector,
                alertPhoneNumber = phone,
                yellowThresholdMultiplier = yellowThresh,
                redThresholdMultiplier = redThresh
            )
            repository.updatePump(updated)
            sensorMonitor.yellowThresholdMultiplier = yellowThresh
            sensorMonitor.redThresholdMultiplier = redThresh
            Toast.makeText(getApplication(), "Configuración industrial guardada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorMonitor.stopListening()
        simulationTickerJob?.cancel()
        autoSaveJob?.cancel()
    }
}
