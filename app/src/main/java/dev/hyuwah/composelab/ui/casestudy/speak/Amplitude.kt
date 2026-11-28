/**
 * Teaches a deterministic stand-in for microphone amplitude: pure math, no mic, no clock.
 * The signal is one slow breath plus three octaves of value noise, sampled per frame.
 */
package dev.hyuwah.composelab.ui.casestudy.speak

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.hyuwah.composelab.ui.theme.ComposeLabTheme
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

object AmplitudeMotion {
    const val Seed: Long = 7L
    const val PeriodMillis: Long = 4_400L      // one full breath at 1x
    const val FrameMillis: Long = 16L          // ~60fps sampling step
    const val BaseWeight: Float = 0.55f
    const val NoiseWeight: Float = 0.45f
    const val Octaves: Int = 3                 // noise octaves: f, 2f, 4f

    internal val OctaveFrequencies = floatArrayOf(8f, 16f, 32f)
    internal val OctaveAmplitudes = floatArrayOf(1f, 0.5f, 0.25f)
}

/**
 * Deterministic fake amplitude generator; the same seed and time always give the same value.
 */
class FakeAmplitude(
    private val seed: Long = AmplitudeMotion.Seed,
    private val periodMillis: Long = AmplitudeMotion.PeriodMillis,
) {
    /** Deterministic 0f..1f amplitude for [timeMillis]; same inputs always give the same output. */
    fun valueAt(timeMillis: Long): Float {
        val cycle = ((timeMillis % periodMillis) + periodMillis) % periodMillis
        val phase = cycle / periodMillis.toFloat()

        val base = 0.5f + 0.5f * sin(2.0 * PI * phase).toFloat()

        var noise = 0f
        var totalWeight = 0f
        for (octave in 0 until AmplitudeMotion.Octaves) {
            val frequency = AmplitudeMotion.OctaveFrequencies[octave]
            val weight = AmplitudeMotion.OctaveAmplitudes[octave]
            noise += weight * valueNoise(seed, octave, phase * frequency)
            totalWeight += weight
        }
        noise /= totalWeight

        val shaped = AmplitudeMotion.BaseWeight * base +
            AmplitudeMotion.NoiseWeight * (0.5f + 0.5f * noise)
        return shaped.coerceIn(0f, 1f)
    }

    private fun valueNoise(seed: Long, octave: Int, x: Float): Float {
        val lattice = floor(x).toInt()
        val fraction = x - lattice
        val smooth = fraction * fraction * (3f - 2f * fraction) // smoothstep
        val start = hash(seed, octave, lattice)
        val end = hash(seed, octave, lattice + 1)
        return start + (end - start) * smooth
    }

    private fun hash(seed: Long, octave: Int, index: Int): Float {
        var h = index.toLong() * 374761393L + octave * 668265263L + seed * 1274126177L
        h = h xor (h shr 13)
        h *= 1274126177L
        h = h xor (h shr 16)
        val unit = (h and 0xFFFF).toFloat() / 0xFFFF.toFloat()
        return unit * 2f - 1f
    }
}

/**
 * Returns live amplitude driven by [withFrameNanos], or a fixed [frozenValue] when supplied.
 */
@Composable
fun rememberFakeAmplitude(
    seed: Long = AmplitudeMotion.Seed,
    periodMillis: Long = AmplitudeMotion.PeriodMillis,
    enabled: Boolean = true,
    frozenValue: Float? = null,
): State<Float> {
    val state = remember(seed, periodMillis) { mutableFloatStateOf(0f) }
    val generator = remember(seed, periodMillis) { FakeAmplitude(seed, periodMillis) }
    val frozen = rememberUpdatedState(frozenValue)

    if (enabled && frozenValue == null) {
        LaunchedEffect(seed, periodMillis) {
            while (true) {
                withFrameNanos { now ->
                    state.floatValue = generator.valueAt(now / 1_000_000L)
                }
            }
        }
    }

    // One unconditional State, so freezing never changes the composition structure.
    val chosen = remember {
        object : State<Float> {
            override val value: Float
                get() = frozen.value ?: state.floatValue
        }
    }
    return chosen
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun FakeAmplitudePreview() {
    ComposeLabTheme {
        val sampler = remember { FakeAmplitude() }
        val quarter = AmplitudeMotion.PeriodMillis / 4
        val half = AmplitudeMotion.PeriodMillis / 2
        val labels = listOf(0L to "t=0", quarter to "t=1/4", half to "t=1/2")
        val text = labels.joinToString(separator = "   ") { (time, label) ->
            "$label → %.3f".format(sampler.valueAt(time))
        }
        Text(
            text = "Deterministic amplitude (seed ${AmplitudeMotion.Seed})\n$text",
            modifier = Modifier,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
