package com.example.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class DeviceDto(
    @DocumentId val id: String = "",
    val name: String = "",
    val hardwareModel: String = "",
    val batteryLevel: Double = 100.0,
    val isActive: Boolean = true,
    @ServerTimestamp val lastSeen: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PumpProfileDto(
    @DocumentId val id: String = "",
    val deviceId: String = "",
    val name: String = "",
    val sector: String = "",
    val baselineRms: Double = 1.85,
    val yellowMultiplier: Double = 1.35,
    val redMultiplier: Double = 2.0,
    val alertPhoneNumber: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VibrationReadingDto(
    @DocumentId val id: String = "",
    val pumpId: String = "",
    val deviceId: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null,
    val rms: Double = 0.0,
    val peakFrequency: Double = 0.0,
    val peakToPeak: Double = 0.0,
    val status: String = "OPTIMAL",
    val healthPercentage: Double = 100.0
)

@IgnoreExtraProperties
data class MaintenanceLogDto(
    @DocumentId val id: String = "",
    val pumpId: String = "",
    val deviceId: String = "",
    @ServerTimestamp val date: Timestamp? = null,
    val description: String = "",
    val costUsd: Double = 0.0,
    val technician: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
