package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vibration_readings")
data class VibrationReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pumpId: Long = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val rms: Float,
    val peakFrequency: Float,
    val peakToPeak: Float,
    val status: String, // "NORMAL", "ADVERTENCIA", "CRITICO", "APAGADA"
    val isPumpOn: Boolean,
    val notes: String = "",
    val mode: String = "BOMBA" // "BOMBA" o "LADERA"
)
