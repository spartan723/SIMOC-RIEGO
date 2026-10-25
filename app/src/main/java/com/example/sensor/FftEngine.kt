package com.example.sensor

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object FftEngine {

    data class FftPoint(
        val frequencyHz: Float,
        val magnitude: Float,
        val isHarmonicPeak: Boolean = false
    )

    data class FftResult(
        val peakFrequencyHz: Float,
        val peakMagnitude: Float,
        val spectrum: FloatArray
    )

    /**
     * Computes Fast Fourier Transform on signal samples at sampleRateHz.
     * N must be a power of 2.
     */
    fun computeFft(samples: FloatArray, sampleRateHz: Float): FftResult {
        val n = samples.size
        if (n == 0 || (n and (n - 1)) != 0) {
            return FftResult(0f, 0f, FloatArray(0))
        }

        // Apply Hann window to reduce spectral leakage
        val real = FloatArray(n)
        val imag = FloatArray(n)
        for (i in 0 until n) {
            val window = (0.5 * (1.0 - cos(2.0 * PI * i / (n - 1)))).toFloat()
            real[i] = samples[i] * window
            imag[i] = 0f
        }

        // Bit reversal
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]; real[i] = real[j]; real[j] = tempR
                val tempI = imag[i]; imag[i] = imag[j]; imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        // Cooley-Tukey Radix-2 FFT
        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val angle = (-2.0 * PI / len).toFloat()
            val wStepR = cos(angle.toDouble()).toFloat()
            val wStepI = sin(angle.toDouble()).toFloat()

            var i = 0
            while (i < n) {
                var wR = 1.0f
                var wI = 0.0f
                for (k in 0 until halfLen) {
                    val uR = real[i + k]
                    val uI = imag[i + k]
                    val vR = real[i + k + halfLen] * wR - imag[i + k + halfLen] * wI
                    val vI = real[i + k + halfLen] * wI + imag[i + k + halfLen] * wR

                    real[i + k] = uR + vR
                    imag[i + k] = uI + vI
                    real[i + k + halfLen] = uR - vR
                    imag[i + k + halfLen] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len = len shl 1
        }

        // Compute magnitude spectrum for positive frequencies [0..n/2]
        val numBins = n / 2
        val spectrum = FloatArray(numBins)
        var maxMag = 0f
        var maxBin = 0

        // Skip DC component (index 0)
        for (bin in 1 until numBins) {
            val mag = sqrt((real[bin] * real[bin] + imag[bin] * imag[bin]).toDouble()).toFloat() / (n / 2)
            spectrum[bin] = mag
            if (mag > maxMag) {
                maxMag = mag
                maxBin = bin
            }
        }

        val peakFreq = (maxBin * sampleRateHz) / n
        return FftResult(
            peakFrequencyHz = peakFreq,
            peakMagnitude = maxMag,
            spectrum = spectrum
        )
    }
}
