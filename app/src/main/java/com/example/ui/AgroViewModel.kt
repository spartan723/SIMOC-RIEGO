package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alert.AutonomousAlertDispatcher
import com.example.alert.DispatchResult
import com.example.data.AgroDatabase
import com.example.data.AgroRepository
import com.example.data.model.MaintenanceRecord
import com.example.data.model.PumpProfile
import com.example.data.model.VibrationReading
import com.example.data.supabase.StationCloudRecord
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SupabaseVibrationPayload
import com.example.device.DeviceManager
import com.example.sensor.HardwareSensorMonitor
import com.example.sensor.LiveTelemetry
import com.example.sensor.OperationalStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "AgroViewModel"

data class CalibrationState(
    val isCalibrating: Boolean = false,
    val progressSeconds: Int = 0,
    val totalSeconds: Int = 10,
    val samples: List<Float> = emptyList()
)

data class MechanicalHealthSummary(
    val healthScore: Float = 95f,
    val bearingWearIndex: Float = 0.12f,
    val cavitationRisk: String = "BAJO",
    val estimatedDaysToService: Int = 180,
    val operatingHours: Float = 184.2f,
    val totalAlertsRecorded: Int = 0
)

class AgroViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("bombavigia_app_prefs", Context.MODE_PRIVATE)

    val deviceManager = DeviceManager(application)
    val deviceId: String = deviceManager.deviceId
    val deviceModel: String = deviceManager.hardwareModel

    private val database = AgroDatabase.getDatabase(application, viewModelScope)
    private val localRepo = AgroRepository(
        database.pumpDao(),
        database.vibrationReadingDao(),
        database.maintenanceRecordDao()
    )

    // Supabase Cloud Client
    val supabaseClient = SupabaseClient(application)

    val sensorMonitor = HardwareSensorMonitor(application)
    val telemetry: StateFlow<LiveTelemetry> = sensorMonitor.telemetry

    val alertDispatcher = AutonomousAlertDispatcher(application)

    // Room Flows
    val currentPump: StateFlow<PumpProfile?> = localRepo.getPump(1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentReadings: StateFlow<List<VibrationReading>> = localRepo.getRecentReadings(1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alertEvents: StateFlow<List<VibrationReading>> = localRepo.getAlertEvents(1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val maintenanceRecords: StateFlow<List<MaintenanceRecord>> = localRepo.getMaintenanceRecords(1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSavings: StateFlow<Double?> = localRepo.getTotalSavings(1)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1070.0)

    // Supabase Cloud Synchronized States
    private val _cloudDevices = MutableStateFlow<List<StationCloudRecord>>(emptyList())
    val cloudDevices: StateFlow<List<StationCloudRecord>> = _cloudDevices.asStateFlow()

    private val _supabaseStatus = MutableStateFlow(
        if (supabaseClient.isConfigured) "Configurado" else "No configurado (Solo Local)"
    )
    val supabaseStatus: StateFlow<String> = _supabaseStatus.asStateFlow()

    private val _calibrationState = MutableStateFlow(CalibrationState())
    val calibrationState: StateFlow<CalibrationState> = _calibrationState.asStateFlow()

    private val _healthSummary = MutableStateFlow(MechanicalHealthSummary())
    val healthSummary: StateFlow<MechanicalHealthSummary> = _healthSummary.asStateFlow()

    private val _lastDispatchResult = MutableStateFlow<DispatchResult?>(null)
    val lastDispatchResult: StateFlow<DispatchResult?> = _lastDispatchResult.asStateFlow()

    // Configurable Alert Channels
    var telegramToken: String
        get() = prefs.getString("tg_token", "") ?: ""
        set(v) = prefs.edit().putString("tg_token", v.trim()).apply()

    var telegramChatId: String
        get() = prefs.getString("tg_chat", "") ?: ""
        set(v) = prefs.edit().putString("tg_chat", v.trim()).apply()

    var webhookUrl: String
        get() = prefs.getString("webhook_url", "") ?: ""
        set(v) = prefs.edit().putString("webhook_url", v.trim()).apply()

    var autoSmsEnabled: Boolean
        get() = prefs.getBoolean("auto_sms", true)
        set(v) = prefs.edit().putBoolean("auto_sms", v).apply()

    private var telemetryCollectionJob: Job? = null
    private var calibrationJob: Job? = null

    init {
        startTelemetryLoop()
        syncStationToSupabase()
        refreshCloudStations()
    }

    fun updateSupabaseCredentials(url: String, key: String) {
        supabaseClient.supabaseUrl = url
        supabaseClient.supabaseAnonKey = key
        _supabaseStatus.value = if (supabaseClient.isConfigured) "Configurado" else "No configurado"
        if (supabaseClient.isConfigured) {
            syncStationToSupabase()
            refreshCloudStations()
        }
    }

    fun testSupabase(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = supabaseClient.testConnection()
            _supabaseStatus.value = if (res.isSuccess) "Conectado a Supabase" else "Error: ${res.message}"
            onResult(res.isSuccess, res.message)
            if (res.isSuccess) {
                syncStationToSupabase()
                refreshCloudStations()
            }
        }
    }

    private fun syncStationToSupabase() {
        if (!supabaseClient.isConfigured) return
        viewModelScope.launch {
            try {
                val station = deviceManager.toStationRecord()
                supabaseClient.upsertStation(station)
                Log.d(TAG, "Station synced to Supabase: ${station.id}")
            } catch (e: Exception) {
                Log.w(TAG, "Could not sync station to Supabase: ${e.message}")
            }
        }
    }

    fun refreshCloudStations() {
        if (!supabaseClient.isConfigured) return
        viewModelScope.launch {
            val res = supabaseClient.fetchStations()
            res.onSuccess { stations ->
                _cloudDevices.value = stations
            }
        }
    }

    private fun startTelemetryLoop() {
        telemetryCollectionJob?.cancel()
        telemetryCollectionJob = viewModelScope.launch {
            var sampleCounter = 0
            while (true) {
                delay(2000)
                val t = telemetry.value
                val pump = currentPump.value

                // Update thresholds in sensor monitor from pump profile
                if (pump != null) {
                    sensorMonitor.baselineRms = pump.baselineRms
                    sensorMonitor.yellowMultiplier = pump.yellowThresholdMultiplier
                    sensorMonitor.redMultiplier = pump.redThresholdMultiplier
                }

                // Persist periodic or anomalous readings to Room
                val isAnomalous = t.status == OperationalStatus.WARNING || t.status == OperationalStatus.CRITICAL
                sampleCounter++

                if (isAnomalous || sampleCounter % 5 == 0) {
                    val reading = VibrationReading(
                        pumpId = pump?.id ?: 1L,
                        deviceId = deviceId,
                        rms = t.rms,
                        peakFrequency = t.peakFrequency,
                        peakToPeak = t.peakToPeak,
                        status = when (t.status) {
                            OperationalStatus.OPTIMAL -> "NORMAL"
                            OperationalStatus.WARNING -> "ADVERTENCIA"
                            OperationalStatus.CRITICAL -> "CRITICO"
                            OperationalStatus.OFF -> "APAGADA"
                        },
                        isPumpOn = t.isPumpRunning,
                        healthPercentage = t.healthPercentage
                    )
                    localRepo.insertReading(reading)

                    // Cloud Sync to Supabase if configured
                    if (supabaseClient.isConfigured) {
                        launch {
                            supabaseClient.insertReading(
                                SupabaseVibrationPayload(
                                    pumpId = pump?.id ?: 1L,
                                    deviceId = deviceId,
                                    rms = t.rms.toDouble(),
                                    peakFrequency = t.peakFrequency.toDouble(),
                                    peakToPeak = t.peakToPeak.toDouble(),
                                    status = reading.status,
                                    isPumpOn = t.isPumpRunning,
                                    healthPercentage = t.healthPercentage.toDouble(),
                                    notes = "Vigía Agro IoT - ${t.statusText}"
                                )
                            )
                        }
                        if (sampleCounter % 15 == 0) {
                            refreshCloudStations()
                        }
                    }

                    // Autonomous Alert Dispatch
                    if (isAnomalous && autoSmsEnabled && pump != null) {
                        launch {
                            val res = alertDispatcher.dispatchAlert(
                                pumpName = pump.name,
                                sector = pump.sector,
                                status = t.statusText,
                                rms = t.rms,
                                threshold = pump.baselineRms * pump.yellowThresholdMultiplier,
                                phoneNumber = pump.alertPhoneNumber,
                                telegramToken = telegramToken.ifBlank { null },
                                telegramChatId = telegramChatId.ifBlank { null },
                                webhookUrl = webhookUrl.ifBlank { null }
                            )
                            _lastDispatchResult.value = res
                        }
                    }
                }

                // Update mechanical health summary derived from real readings
                updateHealthSummary(t, pump)
            }
        }
    }

    private fun updateHealthSummary(t: LiveTelemetry, pump: PumpProfile?) {
        val baseRms = pump?.baselineRms ?: 1.85f
        val ratio = if (baseRms > 0f) t.rms / baseRms else 1.0f

        val wearIndex = ((ratio - 1.0f).coerceAtLeast(0f) * 0.35f).coerceIn(0.05f, 0.95f)
        val cavitation = when {
            t.peakFrequency > 70f && t.rms > baseRms * 1.3f -> "ALTO (Implosión detectada)"
            t.peakFrequency > 55f -> "MODERADO"
            else -> "BAJO"
        }

        val estimatedDays = when {
            t.status == OperationalStatus.CRITICAL -> 2
            t.status == OperationalStatus.WARNING -> 14
            ratio > 1.2f -> 45
            else -> 180
        }

        _healthSummary.value = MechanicalHealthSummary(
            healthScore = t.healthPercentage,
            bearingWearIndex = wearIndex,
            cavitationRisk = cavitation,
            estimatedDaysToService = estimatedDays,
            operatingHours = pump?.totalOperatingHours ?: 184.2f,
            totalAlertsRecorded = alertEvents.value.size
        )
    }

    fun startCalibration(durationSeconds: Int = 10) {
        if (_calibrationState.value.isCalibrating) return
        calibrationJob?.cancel()

        calibrationJob = viewModelScope.launch {
            _calibrationState.value = CalibrationState(
                isCalibrating = true,
                progressSeconds = 0,
                totalSeconds = durationSeconds,
                samples = emptyList()
            )

            val collectedSamples = mutableListOf<Float>()
            for (sec in 1..durationSeconds) {
                delay(1000)
                val currentRms = telemetry.value.rms
                collectedSamples.add(currentRms)
                _calibrationState.value = _calibrationState.value.copy(
                    progressSeconds = sec,
                    samples = collectedSamples.toList()
                )
            }

            // Calculate new baseline
            if (collectedSamples.isNotEmpty()) {
                val avgRms = collectedSamples.average().toFloat()
                val peakFreq = telemetry.value.peakFrequency
                currentPump.value?.let { pump ->
                    val updated = pump.copy(
                        baselineRms = avgRms,
                        baselineFreq = peakFreq,
                        isCalibrated = true,
                        lastCalibrationTime = System.currentTimeMillis()
                    )
                    localRepo.updatePump(updated)
                }
            }

            _calibrationState.value = _calibrationState.value.copy(isCalibrating = false)
        }
    }

    fun updatePumpSettings(
        name: String,
        sector: String,
        phone: String,
        yellowThreshold: Float,
        redThreshold: Float,
        tgToken: String,
        tgChat: String,
        webhook: String,
        autoSms: Boolean
    ) {
        telegramToken = tgToken
        telegramChatId = tgChat
        webhookUrl = webhook
        autoSmsEnabled = autoSms

        viewModelScope.launch {
            currentPump.value?.let { pump ->
                val updated = pump.copy(
                    name = name.trim(),
                    sector = sector.trim(),
                    alertPhoneNumber = phone.trim(),
                    yellowThresholdMultiplier = yellowThreshold,
                    redThresholdMultiplier = redThreshold
                )
                localRepo.updatePump(updated)
            }
        }
    }

    fun testAutonomousDispatch() {
        viewModelScope.launch {
            val pump = currentPump.value ?: return@launch
            val res = alertDispatcher.dispatchAlert(
                pumpName = pump.name,
                sector = pump.sector,
                status = "PRUEBA OPERATIVA CENTINELA",
                rms = 3.85f,
                threshold = pump.baselineRms * pump.yellowThresholdMultiplier,
                phoneNumber = pump.alertPhoneNumber,
                telegramToken = telegramToken.ifBlank { null },
                telegramChatId = telegramChatId.ifBlank { null },
                webhookUrl = webhookUrl.ifBlank { null },
                forceBypassDebounce = true
            )
            _lastDispatchResult.value = res
        }
    }
}
