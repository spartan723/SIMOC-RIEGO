package com.example.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

data class AcousticTelemetry(
    val acousticRmsDb: Float = 0f,
    val highFrequencyEnergyRatio: Float = 0.1f, // 2kHz - 8kHz cavitation band
    val isCavitationDetected: Boolean = false,
    val isMicPermissionGranted: Boolean = false,
    val statusText: String = "Micrófono en espera"
)

class AcousticCavitationDetector(private val context: Context) {

    private val sampleRate = 22050
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    suspend fun sampleAcoustics(isSimulatedCavitation: Boolean = false): AcousticTelemetry = withContext(Dispatchers.IO) {
        if (isSimulatedCavitation) {
            // Simulated acoustic telemetry for demonstration
            return@withContext AcousticTelemetry(
                acousticRmsDb = 74.5f,
                highFrequencyEnergyRatio = 0.68f,
                isCavitationDetected = true,
                isMicPermissionGranted = true,
                statusText = "¡Cavitación acústica detectada en rango 2-8 kHz! (Ruido de pedregullo)"
            )
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return@withContext AcousticTelemetry(
                acousticRmsDb = 0f,
                highFrequencyEnergyRatio = 0.1f,
                isCavitationDetected = false,
                isMicPermissionGranted = false,
                statusText = "Permiso de micrófono no otorgado (Opcional para cavitación)"
            )
        }

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize <= 0) {
            return@withContext AcousticTelemetry(statusText = "Hardware de audio no disponible")
        }

        var audioRecord: AudioRecord? = null
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize * 2
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                return@withContext AcousticTelemetry(statusText = "No se pudo inicializar micrófono")
            }

            audioRecord.startRecording()
            val audioBuffer = ShortArray(minBufferSize)
            val readCount = audioRecord.read(audioBuffer, 0, audioBuffer.size)

            if (readCount <= 0) {
                return@withContext AcousticTelemetry(statusText = "Sin datos de audio")
            }

            // Calculate RMS
            var sumSq = 0.0
            var highFreqChanges = 0
            for (i in 0 until readCount) {
                val v = audioBuffer[i].toDouble()
                sumSq += v * v
                if (i > 0 && abs(audioBuffer[i] - audioBuffer[i - 1]) > 4000) {
                    highFreqChanges++
                }
            }

            val rms = sqrt(sumSq / readCount)
            val db = if (rms > 1.0) (20.0 * log10(rms / 32767.0) + 90.0).toFloat().coerceIn(20f, 110f) else 25f
            val highFreqRatio = (highFreqChanges.toFloat() / readCount.coerceAtLeast(1) * 3f).coerceIn(0.05f, 1.0f)
            val isCavitation = db > 68f && highFreqRatio > 0.45f

            AcousticTelemetry(
                acousticRmsDb = db,
                highFrequencyEnergyRatio = highFreqRatio,
                isCavitationDetected = isCavitation,
                isMicPermissionGranted = true,
                statusText = if (isCavitation) "Alerta: Ruido de implosión / cavitación en succión" else "Firma acústica normal (${String.format("%.0f", db)} dB)"
            )
        } catch (e: Exception) {
            AcousticTelemetry(statusText = "Error de muestreo acústico: ${e.message}")
        } finally {
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (_: Exception) {}
        }
    }
}
