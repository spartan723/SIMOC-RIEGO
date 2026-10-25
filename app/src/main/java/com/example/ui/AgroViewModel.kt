package com.example.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.alert.AutonomousAlertDispatcher
import com.example.alert.DispatchResult
import com.example.data.AgroDatabase
import com.example.data.AgroRepository
import com.example.data.firebase.DeviceDto
import com.example.data.firebase.FirestoreAgroRepository
import com.example.data.firebase.MaintenanceLogDto
import com.example.data.firebase.PumpProfileDto
import com.example.data.firebase.VibrationReadingDto
import com.example.data.model.MaintenanceRecord
import com.example.data.model.PumpProfile
import com.example.data.model.VibrationReading
import com.example.device.DeviceManager
import com.example.sensor.HardwareSensorMonitor
import com.example.sensor.LiveTelemetry
import com.example.sensor.OperationalStatus
import com.google.firebase.firestore.FirebaseFirestore
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

    private val firestoreDb: FirebaseFirestore by lazy {
        val dbId = application.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }
    private val firestoreRepo: FirestoreAgroRepository by lazy {
        FirestoreAgroRepository(firestoreDb)
    }

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

    // Cloud Synchronized States
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _cloudDevices = MutableStateFlow<List<DeviceDto>>(emptyList())
    val cloudDevices: StateFlow<List<DeviceDto>> = _cloudDevices.asStateFlow()

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
    }

    /**
     * Initializes cloud sync when user authenticates with Google.
     */
    fun setAuthenticatedUser(userId: String) {
        if (_currentUserId.value == userId) return
        _currentUserId.value = userId

        viewModelScope.launch {
            try {
                // Register this physical device in Firestore
                val deviceDto = deviceManager.toDeviceDto()
                firestoreRepo.registerOrUpdateDevice(userId, deviceDto)

                // Sync current pump profile to Firestore
                currentPump.value?.let { pump ->
                    firestoreRepo.savePump(
                        userId,
                        PumpProfileDto(
                            id = "pump_${pump.id}",
                            deviceId = deviceId,
                            name = pump.name,
                            sector = pump.sector,
                            baselineRms = pump.baselineRms.toDouble(),
                            yellowMultiplier = pump.yellowThresholdMultiplier.toDouble(),
                            redMultiplier = pump.redThresholdMultiplier.toDouble(),
                            alertPhoneNumber = pump.alertPhoneNumber
                        )
                    )
                }

                // Listen to all devices owned by user
                launch {
                    firestoreRepo.observeDevices(userId).collect { devices ->
                        _cloudDevices.value = devices
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing user cloud sync", e)
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

                    // Cloud Sync to Firestore if signed in
                    _currentUserId.value?.let { uid ->
                        launch {
                            firestoreRepo.saveReading(
                                uid,
                                VibrationReadingDto(
                                    pumpId = "pump_${pump?.id ?: 1}",
                                    deviceId = deviceId,
                                    rms = t.rms.toDouble(),
                                    peakFrequency = t.peakFrequency.toDouble(),
                                    peakToPeak = t.peakToPeak.toDouble(),
                                    status = reading.status,
                                    healthPercentage = t.healthPercentage.toDouble()
                                )
                            )
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

                // Update Health Summary calculation
                updateHealthSummary(t, pump)
            }
        }
    }

    private fun updateHealthSummary(t: LiveTelemetry, pump: PumpProfile?) {
        val wearIndex = (t.rms / (pump?.baselineRms ?: 1.85f)).coerceIn(0.1f, 3.5f)
        val cavRisk = when {
            t.peakFrequency > 120f -> "ELEVADO"
            t.peakFrequency > 70f -> "MEDIO"
            else -> "BAJO"
        }
        val daysRemaining = when (t.status) {
            OperationalStatus.OPTIMAL -> 240
            OperationalStatus.WARNING -> 45
            OperationalStatus.CRITICAL -> 7
            OperationalStatus.OFF -> 365
        }
        _healthSummary.value = MechanicalHealthSummary(
            healthScore = t.healthPercentage,
            bearingWearIndex = wearIndex,
            cavitationRisk = cavRisk,
            estimatedDaysToService = daysRemaining,
            operatingHours = (pump?.totalOperatingHours ?: 184.2f) + (sampleCounterToHours()),
            totalAlertsRecorded = alertEvents.value.size
        )
    }

    private var sampleTicks = 0
    private fun sampleCounterToHours(): Float {
        sampleTicks++
        return (sampleTicks * 2f) / 3600f
    }

    fun startCalibration() {
        calibrationJob?.cancel()
        _calibrationState.value = CalibrationState(isCalibrating = true, progressSeconds = 0, totalSeconds = 10)
        val collected = mutableListOf<Float>()

        calibrationJob = viewModelScope.launch {
            for (sec in 1..10) {
                delay(1000)
                collected.add(telemetry.value.rms)
                _calibrationState.value = _calibrationState.value.copy(
                    progressSeconds = sec,
                    samples = collected.toList()
                )
            }
            // Finish calibration
            val avgBaseline = if (collected.isNotEmpty()) collected.average().toFloat() else 1.85f
            currentPump.value?.let { current ->
                val updated = current.copy(
                    baselineRms = (avgBaseline * 100).toInt() / 100f,
                    lastCalibrationTime = System.currentTimeMillis()
                )
                localRepo.updatePump(updated)

                // Sync to Firestore
                _currentUserId.value?.let { uid ->
                    firestoreRepo.savePump(
                        uid,
                        PumpProfileDto(
                            id = "pump_${updated.id}",
                            deviceId = deviceId,
                            name = updated.name,
                            sector = updated.sector,
                            baselineRms = updated.baselineRms.toDouble(),
                            yellowMultiplier = updated.yellowThresholdMultiplier.toDouble(),
                            redMultiplier = updated.redThresholdMultiplier.toDouble(),
                            alertPhoneNumber = updated.alertPhoneNumber
                        )
                    )
                }
            }
            _calibrationState.value = CalibrationState(isCalibrating = false)
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

                _currentUserId.value?.let { uid ->
                    firestoreRepo.savePump(
                        uid,
                        PumpProfileDto(
                            id = "pump_${updated.id}",
                            deviceId = deviceId,
                            name = updated.name,
                            sector = updated.sector,
                            baselineRms = updated.baselineRms.toDouble(),
                            yellowMultiplier = updated.yellowThresholdMultiplier.toDouble(),
                            redMultiplier = updated.redThresholdMultiplier.toDouble(),
                            alertPhoneNumber = updated.alertPhoneNumber
                        )
                    )
                }
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
