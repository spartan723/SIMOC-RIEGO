package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenance_records")
data class MaintenanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pumpId: Long = 1,
    val deviceId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String,
    val description: String,
    val estimatedRepairSavedUsd: Double = 250.0,
    val technicianName: String = "Técnico Agrícola"
)
