package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pump_profiles")
data class PumpProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceId: String = "",
    val name: String,
    val sector: String,
    val pumpType: String = "Bomba Centrífuga 5.5 HP",
    val baselineRms: Float = 1.85f,
    val baselineFreq: Float = 48.0f,
    val yellowThresholdMultiplier: Float = 1.35f,
    val redThresholdMultiplier: Float = 2.0f,
    val alertPhoneNumber: String = "+5491123456789",
    val isRunning: Boolean = false,
    val isCalibrated: Boolean = true,
    val totalOperatingHours: Float = 142.5f,
    val lastCalibrationTime: Long = System.currentTimeMillis()
)
