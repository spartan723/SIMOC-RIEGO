package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiDiagnosticResult
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.SemaforoGreen
import com.example.ui.theme.SemaforoRed
import com.example.ui.theme.SemaforoYellow

@Composable
fun GeminiAiDiagnosticCard(
    diagnosticResult: GeminiDiagnosticResult?,
    isLoadingAi: Boolean,
    onRequestAiAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141D17)),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, EmeraldPrimaryDark.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .testTag("gemini_diagnostic_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1B432E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DIAGNÓSTICO EXPERTO CON IA",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = Color(0xFFA7F3D0)
                        )
                        Text(
                            text = "Análisis de Causa Raíz (Gemini 3.5)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Button(
                    onClick = onRequestAiAnalysis,
                    enabled = !isLoadingAi,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B3825),
                        contentColor = Color(0xFF4ADE80)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_request_gemini")
                ) {
                    if (isLoadingAi) {
                        CircularProgressIndicator(
                            color = Color(0xFF4ADE80),
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analizando...", fontSize = 11.sp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Consultar IA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (diagnosticResult != null) {
                // Urgency badge & Root cause
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Causa Probable: ${diagnosticResult.rootCause}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (diagnosticResult.urgencyLevel.contains("Crítica", ignoreCase = true) || diagnosticResult.urgencyLevel.contains("Alta", ignoreCase = true)) {
                                    SemaforoRed.copy(alpha = 0.2f)
                                } else if (diagnosticResult.urgencyLevel.contains("Moderada", ignoreCase = true)) {
                                    SemaforoYellow.copy(alpha = 0.2f)
                                } else {
                                    SemaforoGreen.copy(alpha = 0.2f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = diagnosticResult.urgencyLevel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (diagnosticResult.urgencyLevel.contains("Crítica", ignoreCase = true) || diagnosticResult.urgencyLevel.contains("Alta", ignoreCase = true)) {
                                SemaforoRed
                            } else if (diagnosticResult.urgencyLevel.contains("Moderada", ignoreCase = true)) {
                                SemaforoYellow
                            } else {
                                SemaforoGreen
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = diagnosticResult.summary,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Pasos de revisión sugeridos en caseta:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFA7F3D0)
                )

                Spacer(modifier = Modifier.height(6.dp))

                diagnosticResult.recommendedActions.forEachIndexed { idx, action ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${idx + 1}.",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4ADE80),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = action,
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                Text(
                    text = "Haga clic en 'Consultar IA' para que Gemini analice la relación RMS, frecuencia de giro y firma acústica para sugerir qué pieza revisar primero.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
