package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

private const val TAG = "FirestoreAgroRepo"

class FirestoreAgroRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    /**
     * Registers or refreshes the heartbeat of this physical Android device station.
     */
    suspend fun registerOrUpdateDevice(userId: String, device: DeviceDto) {
        if (userId.isBlank() || device.id.isBlank()) return
        val docRef = db.collection("users").document(userId)
            .collection("devices").document(device.id)

        try {
            val docSnapshot = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "id" to device.id,
                "name" to device.name,
                "hardwareModel" to device.hardwareModel,
                "batteryLevel" to device.batteryLevel,
                "isActive" to true,
                "lastSeen" to FieldValue.serverTimestamp()
            )
            if (!docSnapshot.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                docRef.set(payload, SetOptions.merge()).await()
            }
            Log.d(TAG, "Device registered/updated successfully: ${device.id}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error registering device ${device.id}", e)
        }
    }

    /**
     * Real-time stream of all monitoring devices owned by the user.
     */
    fun observeDevices(userId: String): Flow<List<DeviceDto>> {
        return db.collection("users").document(userId)
            .collection("devices")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DeviceDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    /**
     * Saves or updates a pump profile in the cloud.
     */
    suspend fun savePump(userId: String, pump: PumpProfileDto) {
        if (userId.isBlank()) return
        val pumpId = pump.id.ifBlank { "pump_1" }
        val docRef = db.collection("users").document(userId)
            .collection("pumps").document(pumpId)

        try {
            val doc = docRef.get().await()
            val payload = mutableMapOf<String, Any>(
                "id" to pumpId,
                "deviceId" to pump.deviceId,
                "name" to pump.name,
                "sector" to pump.sector,
                "baselineRms" to pump.baselineRms,
                "yellowMultiplier" to pump.yellowMultiplier,
                "redMultiplier" to pump.redMultiplier,
                "alertPhoneNumber" to pump.alertPhoneNumber,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!doc.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                docRef.set(payload, SetOptions.merge()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error saving pump $pumpId", e)
        }
    }

    /**
     * Real-time stream of pump profiles.
     */
    fun observePumps(userId: String): Flow<List<PumpProfileDto>> {
        return db.collection("users").document(userId)
            .collection("pumps")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PumpProfileDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    /**
     * Appends an immutable vibration telemetry reading to Firestore.
     */
    suspend fun saveReading(userId: String, reading: VibrationReadingDto) {
        if (userId.isBlank()) return
        val readingId = reading.id.ifBlank {
            "rdg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        }
        val docRef = db.collection("users").document(userId)
            .collection("readings").document(readingId)

        try {
            val payload = mapOf(
                "id" to readingId,
                "pumpId" to reading.pumpId.ifBlank { "pump_1" },
                "deviceId" to reading.deviceId,
                "timestamp" to FieldValue.serverTimestamp(),
                "rms" to reading.rms,
                "peakFrequency" to reading.peakFrequency,
                "peakToPeak" to reading.peakToPeak,
                "status" to reading.status,
                "healthPercentage" to reading.healthPercentage
            )
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error saving reading $readingId", e)
        }
    }

    /**
     * Real-time stream of latest vibration readings.
     */
    fun observeReadings(userId: String, limit: Long = 100): Flow<List<VibrationReadingDto>> {
        return db.collection("users").document(userId)
            .collection("readings")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(VibrationReadingDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }

    /**
     * Saves a maintenance log.
     */
    suspend fun saveMaintenance(userId: String, log: MaintenanceLogDto) {
        if (userId.isBlank()) return
        val logId = log.id.ifBlank {
            "mnt_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        }
        val docRef = db.collection("users").document(userId)
            .collection("maintenance").document(logId)

        try {
            val payload = mapOf(
                "id" to logId,
                "pumpId" to log.pumpId.ifBlank { "pump_1" },
                "deviceId" to log.deviceId,
                "date" to FieldValue.serverTimestamp(),
                "description" to log.description,
                "costUsd" to log.costUsd,
                "technician" to log.technician,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error saving maintenance $logId", e)
        }
    }

    /**
     * Real-time stream of maintenance logs.
     */
    fun observeMaintenance(userId: String): Flow<List<MaintenanceLogDto>> {
        return db.collection("users").document(userId)
            .collection("maintenance")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(MaintenanceLogDto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
    }
}
