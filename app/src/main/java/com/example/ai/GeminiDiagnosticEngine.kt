package com.example.ai

import com.example.BuildConfig
import com.example.data.model.PumpProfile
import com.example.sensor.LiveTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiDiagnosticResult(
    val summary: String,
    val rootCause: String,
    val urgencyLevel: String, // "Crítica (Inmediata)", "Moderada (Esta semana)", "Baja (Monitorear)"
    val recommendedActions: List<String>,
    val rawModelResponse: String = "",
    val isAiGenerated: Boolean = true
)

class GeminiDiagnosticEngine {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzePumpHealth(
        pump: PumpProfile,
        telemetry: LiveTelemetry,
        customApiKey: String? = null
    ): GeminiDiagnosticResult = withContext(Dispatchers.IO) {
        val apiKey = if (!customApiKey.isNullOrBlank()) customApiKey else BuildConfig.GEMINI_API_KEY

        val prompt = """
            Eres un ingeniero mecánico especialista en bombas de riego centrífugas y mantenimiento predictivo (Norma ISO 10816).
            Analiza la siguiente telemetría en vivo registrada por el sensor del celular adosado al motor:
            
            - Equipo: ${pump.name} (${pump.pumpType})
            - Sector: ${pump.sector}
            - Horas de Operación Acumuladas: ${pump.totalOperatingHours} h
            - Línea Base Calibrada (Bomba Sana): ${String.format("%.2f", pump.baselineRms)} m/s²
            - Vibración Actual (RMS): ${String.format("%.2f", telemetry.rms)} m/s² (Ratio: ${String.format("%.1f", telemetry.ratioToBaseline)}x)
            - Frecuencia Dominante: ${String.format("%.1f", telemetry.peakFrequency)} Hz
            - Amplitud Pico a Pico: ${String.format("%.2f", telemetry.peakToPeak)} m/s²
            - Detección Acústica de Cavitación: ${if (telemetry.acousticTelemetry.isCavitationDetected) "POSITIVA (Ruido de implosión en succión)" else "Negativa / Normal"}
            - Clasificación ISO Actual: ${telemetry.isoClassification}
            
            Por favor, responde en español de manera directa, práctica y comprensible para un productor agrícola o encargado de riego rural.
            Estructura tu respuesta exactamente con estas secciones:
            1. Diagnóstico Principal: (1 o 2 oraciones claras)
            2. Causa Raíz Probable: (Cavitación, rodamiento gastado, desalineación o desbalance)
            3. Urgencia: (Inmediata / Esta semana / Observación)
            4. 3 Pasos de Revisión en Terreno: (Pasos físicos concretos que el operador puede hacer antes de llamar al taller)
        """.trimIndent()

        // If no valid API key is present, fallback to expert local rules engine without failing
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateLocalExpertRuleDiagnostic(pump, telemetry)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        }
                        put("parts", partsArray)
                    })
                }
                put("contents", contentsArray)
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBodyStr = response.body?.string() ?: ""
                response.close()
                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text") ?: ""

                if (text.isNotBlank()) {
                    return@withContext parseGeminiResponse(text)
                }
            }
            response.close()
            // Fallback to deterministic local diagnostic if network or quota issue
            generateLocalExpertRuleDiagnostic(pump, telemetry)
        } catch (e: Exception) {
            generateLocalExpertRuleDiagnostic(pump, telemetry)
        }
    }

    private fun parseGeminiResponse(rawText: String): GeminiDiagnosticResult {
        val lines = rawText.lines()
        var summary = ""
        var rootCause = ""
        var urgency = "Moderada (Esta semana)"
        val actions = mutableListOf<String>()

        lines.forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.contains("Diagnóstico Principal", ignoreCase = true) ->
                    summary = trimmed.substringAfter(":").trim()
                trimmed.contains("Causa Raíz", ignoreCase = true) ->
                    rootCause = trimmed.substringAfter(":").trim()
                trimmed.contains("Urgencia", ignoreCase = true) ->
                    urgency = trimmed.substringAfter(":").trim()
                trimmed.startsWith("-") || trimmed.startsWith("•") || (trimmed.length > 2 && trimmed[0].isDigit() && trimmed[1] == '.') -> {
                    val cleanAction = trimmed.replaceFirst(Regex("^[-•\\d.\\s]+"), "").trim()
                    if (cleanAction.isNotBlank() && actions.size < 4) {
                        actions.add(cleanAction)
                    }
                }
            }
        }

        if (summary.isBlank()) {
            summary = lines.take(3).joinToString(" ").take(180)
        }
        if (rootCause.isBlank()) {
            rootCause = "Fatiga en rodamiento o desbalance de masa rotante"
        }
        if (actions.isEmpty()) {
            actions.add("Inspeccionar juego radial en el eje motriz con el motor desenergizado")
            actions.add("Aplicar grasa de litio complejo en niples de rodamiento")
            actions.add("Verificar apriete de pernos de anclaje a la bancada")
        }

        return GeminiDiagnosticResult(
            summary = summary,
            rootCause = rootCause,
            urgencyLevel = urgency,
            recommendedActions = actions,
            rawModelResponse = rawText,
            isAiGenerated = true
        )
    }

    private fun generateLocalExpertRuleDiagnostic(pump: PumpProfile, telemetry: LiveTelemetry): GeminiDiagnosticResult {
        return when {
            telemetry.acousticTelemetry.isCavitationDetected || telemetry.ratioToBaseline > 2.5f -> {
                GeminiDiagnosticResult(
                    summary = "Vibración severa con pulsos acústicos compatibles con cavitación y desbalance de impulsor.",
                    rootCause = "Entrada de aire en succión o nivel dinámico de pozo por debajo de la cota de bombeo.",
                    urgencyLevel = "Crítica (Inmediata)",
                    recommendedActions = listOf(
                        "Comprobar nivel dinámico de agua en el pozo y altura de aspiración",
                        "Verificar que la válvula de pie no esté obstruida con arena o ramas",
                        "Inspeccionar sello mecánico y apriete de brida de succión"
                    ),
                    isAiGenerated = false
                )
            }
            telemetry.ratioToBaseline > pump.redThresholdMultiplier -> {
                GeminiDiagnosticResult(
                    summary = "Amplitud vibratoria 2X sobre la línea base. Peligro de daño inminente a bobinado.",
                    rootCause = "Pista de rodamiento fisurada o desbalance dinámico del eje motor.",
                    urgencyLevel = "Alta (Detener antes del próximo turno)",
                    recommendedActions = listOf(
                        "Desconectar energía y verificar juego radial y axial del eje con la mano",
                        "Inspeccionar temperatura al tacto en la tapa delantera del rodamiento",
                        "Programar recambio de rodamientos SKF / NSK antes de enclavamiento"
                    ),
                    isAiGenerated = false
                )
            }
            telemetry.ratioToBaseline > pump.yellowThresholdMultiplier -> {
                GeminiDiagnosticResult(
                    summary = "Incremento gradual de vibración. Comportamiento en Zona C de mantenimiento preventivo.",
                    rootCause = "Falta de lubricación o desalineación leve entre bomba y motor.",
                    urgencyLevel = "Moderada (Esta semana)",
                    recommendedActions = listOf(
                        "Engrasar rodamientos con grasa para alta temperatura (NLGI 2)",
                        "Revisar apriete de los 4 pernos de sujeción a la base de hormigón",
                        "Verificar que el acople flexible no tenga gomas rotas o desgastadas"
                    ),
                    isAiGenerated = false
                )
            }
            else -> {
                GeminiDiagnosticResult(
                    summary = "Bomba operando en óptimas condiciones mecánicas dentro de la Zona A/B de la norma ISO.",
                    rootCause = "Ninguna anomalía detectada. Alineación y rodamientos estables.",
                    urgencyLevel = "Baja (Operación Normal)",
                    recommendedActions = listOf(
                        "Continuar con el régimen de riego habitual",
                        "Mantener limpio el visor transparente de la caja del sensor",
                        "Revisar nivel de aceite o lubricante en el próximo intervalo mensual"
                    ),
                    isAiGenerated = false
                )
            }
        }
    }
}
