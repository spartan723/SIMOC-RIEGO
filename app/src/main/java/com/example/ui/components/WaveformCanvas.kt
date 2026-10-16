package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensor.OperationalStatus
import com.example.ui.theme.SemaforoGreen
import com.example.ui.theme.SemaforoOff
import com.example.ui.theme.SemaforoRed
import com.example.ui.theme.SemaforoYellow

@Composable
fun WaveformCanvas(
    samples: List<Float>,
    status: OperationalStatus,
    isPumpRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val waveColor = when (status) {
        OperationalStatus.OFF -> SemaforoOff
        OperationalStatus.NORMAL -> SemaforoGreen
        OperationalStatus.WARNING -> SemaforoYellow
        OperationalStatus.CRITICAL -> SemaforoRed
    }

    val animatedAlpha = remember { Animatable(0.8f) }
    LaunchedEffect(status) {
        if (status == OperationalStatus.CRITICAL) {
            animatedAlpha.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
        } else {
            animatedAlpha.snapTo(0.85f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F1713))
            .padding(14.dp)
            .testTag("waveform_canvas_box")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Osciloscopio",
                    tint = waveColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPumpRunning) "OSCILOSCOPIO DINÁMICO EN VIVO (100 Hz)" else "SENSOR EN REPOSO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (isPumpRunning) "DINÁMICA ACTIVA" else "STANDBY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = waveColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .testTag("live_waveform_canvas")
            ) {
                val width = size.width
                val height = size.height
                val midY = height / 2f

                // Draw subtle industrial grid lines
                val gridLines = 4
                for (i in 1..gridLines) {
                    val y = height * (i.toFloat() / (gridLines + 1))
                    drawLine(
                        color = Color(0xFF1E2923),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }
                // Center zero reference line
                drawLine(
                    color = Color(0xFF334155),
                    start = Offset(0f, midY),
                    end = Offset(width, midY),
                    strokeWidth = 1.5f
                )

                if (samples.size > 1) {
                    val path = Path()
                    val step = width / (samples.size - 1)
                    val maxVal = 6.0f // m/s² scale headroom

                    samples.forEachIndexed { index, sample ->
                        val normalizedY = midY - ((sample / maxVal) * (height / 2f)).coerceIn(-midY, midY)
                        val x = index * step
                        if (index == 0) {
                            path.moveTo(x, normalizedY)
                        } else {
                            path.lineTo(x, normalizedY)
                        }
                    }

                    drawPath(
                        path = path,
                        color = waveColor.copy(alpha = animatedAlpha.value),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
