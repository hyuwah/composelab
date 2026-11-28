package dev.hyuwah.composelab.casestudy

import dev.hyuwah.composelab.ui.casestudy.speak.AmplitudeMotion
import dev.hyuwah.composelab.ui.casestudy.speak.FakeAmplitude
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAmplitudeTest {

    @Test
    fun sameSeedSameTimes() {
        val first = FakeAmplitude(7L)
        val second = FakeAmplitude(7L)
        listOf(0L, 500L, 1_000L, 2_200L, 4_400L).forEach { time ->
            assertEquals(first.valueAt(time), second.valueAt(time), 1e-6f)
        }
    }

    @Test
    fun staysInZeroToOne() {
        val amplitude = FakeAmplitude(7L)
        var sawHigh = false
        var sawLow = false
        val end = AmplitudeMotion.PeriodMillis * 3
        var time = 0L
        while (time <= end) {
            val value = amplitude.valueAt(time)
            assertTrue("value $value out of range at $time", value in 0f..1f)
            if (value > 0.6f) sawHigh = true
            if (value < 0.4f) sawLow = true
            time += AmplitudeMotion.FrameMillis
        }
        assertTrue("signal never exceeded 0.6f", sawHigh)
        assertTrue("signal never dropped below 0.4f", sawLow)
    }

    @Test
    fun seedChangesSignal() {
        val seven = FakeAmplitude(7L)
        val fortyTwo = FakeAmplitude(42L)
        val time = 1_200L
        assertTrue(
            "seeds produced identical values at $time",
            kotlin.math.abs(seven.valueAt(time) - fortyTwo.valueAt(time)) > 1e-3f,
        )
    }

    @Test
    fun periodIsRepeating() {
        val amplitude = FakeAmplitude(7L)
        listOf(0L, 120L, 730L, 1_500L, 3_100L).forEach { time ->
            assertEquals(
                amplitude.valueAt(time),
                amplitude.valueAt(time + AmplitudeMotion.PeriodMillis),
                1e-4f,
            )
        }
    }
}
