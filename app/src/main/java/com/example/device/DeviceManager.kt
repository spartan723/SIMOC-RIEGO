package com.example.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.example.data.firebase.DeviceDto
import java.util.UUID

class DeviceManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("bombavigia_device_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DEVICE_ID = "pref_device_id"
        private const val KEY_DEVICE_NAME = "pref_device_name"
    }

    /**
     * Unique persistent ID for this physical device station.
     * Guaranteed alphanumeric [a-zA-Z0-9_\\-] format.
     */
    val deviceId: String
        get() {
            var id = prefs.getString(KEY_DEVICE_ID, null)
            if (id.isNullOrBlank()) {
                val cleanUuid = UUID.randomUUID().toString().replace("-", "").take(8).lowercase()
                id = "dev_$cleanUuid"
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }

    var deviceName: String
        get() {
            val defaultName = "Estación ${Build.MODEL.ifBlank { "Android" }}"
            return prefs.getString(KEY_DEVICE_NAME, defaultName) ?: defaultName
        }
        set(value) {
            prefs.edit().putString(KEY_DEVICE_NAME, value.trim()).apply()
        }

    val hardwareModel: String
        get() = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

    fun getBatteryLevel(): Double {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, intentFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                (level.toDouble() / scale.toDouble()) * 100.0
            } else {
                100.0
            }
        } catch (_: Exception) {
            100.0
        }
    }

    fun toDeviceDto(): DeviceDto {
        return DeviceDto(
            id = deviceId,
            name = deviceName,
            hardwareModel = hardwareModel,
            batteryLevel = getBatteryLevel(),
            isActive = true,
            lastSeen = null,
            createdAt = null
        )
    }
}
