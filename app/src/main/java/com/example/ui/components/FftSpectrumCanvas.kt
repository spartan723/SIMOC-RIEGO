package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor.FftPoint
import com.example.sensor.OperationalStatus
import com.example.ui.theme.SemaforoGreen
import com.example.ui.theme.SemaforoRed
import com.example.ui.theme.SemaforoYellow

@Composable
fun FftSpectrumCanvas(
    fftPoints: List<FftPoint>,
    peakFrequencyHz: Float,
    status: OperationalStatus,
    modifier: Modifier = Modifier
) {
    val barColor = when (status) {
        OperationalStatus.OFF -> Color(0xFF64748B)
        OperationalStatus.NORMAL -> SemaforoGreen
        OperationalStatus.WARNING -> SemaforoYellow
        OperationalStatus.CRITICAL -> SemaforoRed
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F1713))
            .padding(14.dp)
            .testTag("fft_spectrum_canvas_box")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = "Espectro FFT",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ESPECTRO DE FRECUENCIA FFT (0 - 50 Hz)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E2923))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Pico: ${String.format("%.1f", peakFrequencyHz)} Hz (1X)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF38BDF8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .testTag("fft_canvas")
            ) {
                val width = size.width
                val height = size.height
                val numBars = fftPoints.size.coerceAtLeast(1)
                val barWidth = (width / numBars) * 0.75f
                val gap = (width / numBars) * 0.25f

                // Draw background grid lines (Amplitude levels)
                for (lvl in 1..3) {
                    val y = height * (lvl / 4f)
                    drawLine(
                        color = Color(0xFF1E2923),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                // Draw ISO threshold line at 75% height
                val isoLimitY = height * 0.28f
                drawLine(
                    color = SemaforoRed.copy(alpha = 0.6f),
                    start = Offset(0f, isoLimitY),
                    end = Offset(width, isoLimitY),
                    strokeWidth = 1.5f
                )

                // Draw frequency spectrum bars
                val maxMag = 4.0f // Scaling ceiling
                fftPoints.forEachIndexed { index, point ->
                    val barHeight = (point.magnitude / maxMag * height).coerceIn(4f, height)
                    val x = index * (barWidth + gap)
                    val y = height - barHeight

                    val currentBarColor = if (point.isHarmonicPeak) {
                        if (status == OperationalStatus.CRITICAL) SemaforoRed else Color(0xFF38BDF8)
                    } else {
                        barColor.copy(alpha = 0.8f)
                    }

                    drawRect(
                        color = currentBarColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Frequency axis markers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("0 Hz", fontSize = 10.sp, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.weight(1f))
                Text("12.5 Hz", fontSize = 10.sp, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.weight(1f))
                Text("25 Hz", fontSize = 10.sp, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.weight(1f))
                Text("37.5 Hz", fontSize = 10.sp, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.weight(1f))
                Text("50 Hz (1X)", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
            }
        }
    }
}
