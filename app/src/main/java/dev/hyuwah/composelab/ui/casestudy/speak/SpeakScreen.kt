/**
 * Chapter 4 — the composition: deterministic amplitude feeds the glow and the hold-to-speak CTA.
 * Frozen mode replaces the clock with a slider, so glow and morph can be inspected frame by frame.
 */
package dev.hyuwah.composelab.ui.casestudy.speak

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hyuwah.composelab.ui.experiments.Experiment
import dev.hyuwah.composelab.ui.experiments.ExperimentScaffold
import dev.hyuwah.composelab.ui.playground.button.ActionState
import dev.hyuwah.composelab.ui.playground.button.ScrubRow
import dev.hyuwah.composelab.ui.theme.ComposeLabTheme

object SpeakExperiment : Experiment {
    override val id = "speak"
    override val title = "Speak button"
    override val description = "A hold-to-speak button built from the article 1 states. Amplitude comes from a generator, so the app never asks for the microphone."

    @Composable
    override fun Content(onBack: () -> Unit, modifier: Modifier) {
        ExperimentScaffold(
            title = title,
            description = description,
            onBack = onBack,
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SpeakStage()
                Text(
                    text = "The amplitude comes from a fixed seed: a slow sine plus three octaves of value noise. " +
                        "Replaying the same seed replays the same curve.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Glow, CTA and the freeze control wired to one amplitude source. */
@Composable
fun SpeakStage(modifier: Modifier = Modifier) {
    var frozen by remember { mutableStateOf(false) }
    var scrubbed by remember { mutableFloatStateOf(0.4f) }
    var state by remember { mutableStateOf<ActionState>(ActionState.Idle) }

    val amplitude by rememberFakeAmplitude(
        enabled = !frozen,
        frozenValue = if (frozen) scrubbed else null,
    )

    // Glow only while listening — but always at full while frozen, so the demo can be inspected.
    val glowIntensity by animateFloatAsState(
        targetValue = if (frozen || state == ActionState.Loading) 1f else 0f,
        animationSpec = GlowMotion.GateSpec,
        label = "glowGate",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center,
        ) {
            SpeakGlow(
                amplitude = amplitude,
                intensity = glowIntensity,
                modifier = Modifier.size(300.dp),
            )
            SpeakCta(amplitude = amplitude, onStateChange = { state = it })
        }

        Text(
            text = "amplitude %.2f · state $state".format(amplitude),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ScrubRow(
            label = "Freeze amplitude",
            frozen = frozen,
            onFrozenChange = { frozen = it },
            value = scrubbed,
            onValueChange = { scrubbed = it },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, heightDp = 620)
@Composable
private fun SpeakStagePreview() {
    ComposeLabTheme { SpeakStage() }
}
