package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.sensor.OperationalStatus
import com.example.sensor.PumpFaultType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BOMBAVIGIA", appName)
  }

  @Test
  fun `rms vibration calculation test`() {
    val samples = listOf(1.5f, -1.5f, 1.5f, -1.5f)
    var sumSq = 0f
    samples.forEach { sumSq += it * it }
    val rms = sqrt(sumSq / samples.size)
    assertEquals(1.5f, rms, 0.001f)
  }

  @Test
  fun `pump state detection logic test`() {
    val runningRms = 1.95f
    val idleRms = 0.05f
    val threshold = 0.20f

    assertTrue("Running pump RMS exceeds threshold", runningRms > threshold)
    assertTrue("Idle pump RMS below threshold", idleRms < threshold)
  }

  @Test
  fun `pump baseline ratio threshold test`() {
    val baseline = 1.90f
    val yellowMultiplier = 1.35f
    val redMultiplier = 2.0f

    val normalVibration = 2.10f
    val warningVibration = 2.90f
    val criticalVibration = 4.50f

    val normalRatio = normalVibration / baseline
    val warningRatio = warningVibration / baseline
    val criticalRatio = criticalVibration / baseline

    assertTrue("Normal ratio below yellow threshold", normalRatio < yellowMultiplier)
    assertTrue("Warning ratio between yellow and red", warningRatio in yellowMultiplier..redMultiplier)
    assertTrue("Critical ratio exceeds red threshold", criticalRatio > redMultiplier)
  }
}
