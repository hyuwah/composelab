package dev.hyuwah.composelab.ui.playground.button

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hyuwah.composelab.ui.experiments.Experiment
import dev.hyuwah.composelab.ui.experiments.ExperimentScaffold
import dev.hyuwah.composelab.ui.theme.ComposeLabTheme

/**
 * Article 1 catalog: the button experiment, split into the four chapters of "Button beyond Material".
 * Each chapter owns its own file, preview and freeze-frame control; this file only puts them in order.
 */
object ButtonExperiment : Experiment {
    override val id = "button"
    override val title = "Button beyond Material"
    override val description = "Four chapters on one button. Each chapter has its own file, preview and freeze switch."

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
            ) {
                Text(
                    text = "Freeze any chapter to hold one frame and drag through it. Durations and springs " +
                        "are named constants at the top of each file, ready to tune.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ChapterFrame(
                    index = "01",
                    title = "Bounce",
                    api = "animateFloatAsState · Modifier.graphicsLayer",
                    modifier = Modifier.padding(vertical = 20.dp),
                ) { BounceChapter() }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                ChapterFrame(
                    index = "02",
                    title = "Interruption",
                    api = "Animatable · animateTo · initialVelocity",
                    modifier = Modifier.padding(vertical = 20.dp),
                ) { InterruptionChapter() }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                ChapterFrame(
                    index = "03",
                    title = "Morph",
                    api = "Animatable<Float> · lerp · AnimatedContent + SizeTransform",
                    modifier = Modifier.padding(vertical = 20.dp),
                ) { MorphChapter() }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))

                ChapterFrame(
                    index = "04",
                    title = "Polish",
                    api = "semantics · liveRegion · indication",
                    modifier = Modifier.padding(vertical = 20.dp),
                ) { PolishChapter() }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, heightDp = 1400)
@Composable
private fun ButtonExperimentPreview() {
    ComposeLabTheme { ButtonExperiment.Content(onBack = {}, modifier = Modifier) }
}
