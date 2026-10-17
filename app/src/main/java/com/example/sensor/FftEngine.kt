package com.example.sensor

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class FftPoint(
    val frequencyHz: Float,
    val magnitude: Float,
    val isHarmonicPeak: Boolean = false,
    val harmonicLabel: String? = null
)

object FftEngine {

    /**
     * Computes the Fast Fourier Transform of real-valued input array using radix-2 Cooley-Tukey.
     * The size of [input] must be a power of 2 (e.g. 32, 64, 128).
     */
    fun computeFft(input: FloatArray, samplingRateHz: Float = 100f): List<FftPoint> {
        val n = input.size
        if (n == 0 || (n and (n - 1)) != 0) {
            return emptyList()
        }

        // Apply Hanning Window to reduce spectral leakage
        val real = DoubleArray(n)
        val imag = DoubleArray(n) { 0.0 }
        for (i in 0 until n) {
            val window = 0.5 * (1.0 - cos(2.0 * PI * i / (n - 1)))
            real[i] = input[i].toDouble() * window
        }

        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR
                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        // Cooley-Tukey computation
        var len = 2
        while (len <= n) {
            val half = len shr 1
            val angle = -2.0 * PI / len
            val wStepR = cos(angle)
            val wStepI = sin(angle)

            var i = 0
            while (i < n) {
                var wR = 1.0
                var wI = 0.0
                for (k in 0 until half) {
                    val uR = real[i + k]
                    val uI = imag[i + k]
                    val vR = real[i + k + half] * wR - imag[i + k + half] * wI
                    val vI = real[i + k + half] * wI + imag[i + k + half] * wR

                    real[i + k] = uR + vR
                    imag[i + k] = uI + vI
                    real[i + k + half] = uR - vR
                    imag[i + k + half] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len = len shl 1
        }

        // Calculate magnitude for positive half of frequencies (0 to Nyquist)
        val halfN = n / 2
        val freqResolution = samplingRateHz / n
        val points = ArrayList<FftPoint>(halfN)

        for (k in 0 until halfN) {
            val mag = (sqrt(real[k] * real[k] + imag[k] * imag[k]) * (2.0 / n)).toFloat()
            val freq = k * freqResolution

            // Detect 1X RPM (around 48-52 Hz) and 2X RPM harmonic
            val is1X = freq in 46f..54f
            val is2X = freq in 95f..105f
            val label = when {
                is1X -> "1X (Giro)"
                is2X -> "2X (Desalineación)"
                else -> null
            }

            points.add(
                FftPoint(
                    frequencyHz = freq,
                    magnitude = mag,
                    isHarmonicPeak = is1X || is2X,
                    harmonicLabel = label
                )
            )
        }

        return points
    }
}
