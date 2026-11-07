package com.example.data.supabase

data class StationCloudRecord(
    val id: String,
    val name: String,
    val hardwareModel: String,
    val batteryLevel: Double,
    val isActive: Boolean,
    val lastSeen: String = ""
)

data class SupabaseVibrationPayload(
    val pumpId: Long,
    val deviceId: String,
    val rms: Double,
    val peakFrequency: Double,
    val peakToPeak: Double,
    val status: String,
    val isPumpOn: Boolean,
    val healthPercentage: Double,
    val notes: String
)

data class SupabaseConnectionResult(
    val isSuccess: Boolean,
    val message: String,
    val statusCode: Int = 0
)
