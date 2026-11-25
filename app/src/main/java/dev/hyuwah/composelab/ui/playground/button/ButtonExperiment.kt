package dev.hyuwah.composelab.ui.playground.button

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.hyuwah.composelab.ui.experiments.Experiment
import dev.hyuwah.composelab.ui.experiments.ExperimentScaffold
import dev.hyuwah.composelab.ui.theme.ComposeLabTheme

/**
 * Experiment: a single button. Everything for this playground lives in this file.
 * Start here: animate*AsState, interactionSource, graphicsLayer, custom ripple.
 */
object ButtonExperiment : Experiment {
    override val id = "button"
    override val title = "Button playground"
    override val description = "A single button. Motion, animation and UI experiments go here."

    @Composable
    override fun Content(onBack: () -> Unit, modifier: Modifier) {
        ExperimentScaffold(
            title = title,
            description = description,
            onBack = onBack,
            modifier = modifier,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SimpleButton()
                BouncingButton()
                MorphingButton()
            }
        }
    }
}

@Composable
private fun SimpleButton() {
    Button(onClick = { /* experiment */ }) {
        Text("Tap me")
    }
}

@Composable
private fun BouncingButton() {
    val interactionSource = remember { MutableInteractionSource() }
    val pressProgress = rememberPressProgress(interactionSource)
    Button(
        onClick = { /* experiment */ },
        interactionSource = interactionSource,
        modifier = Modifier.bouncePress(pressProgress)
    ) {
        Text("Bounce me")
    }
}
fun Modifier.bouncePress(pressProgress: State<Float>, depth: Dp = 4.dp): Modifier {
    return this.graphicsLayer {
        translationY = depth.toPx() * pressProgress.value
        val s = 1f - 0.05f * pressProgress.value
        scaleX = s
        scaleY = s
    }
}
@Composable
fun rememberPressProgress(
    source: MutableInteractionSource,
    spec: AnimationSpec<Float> = spring(dampingRatio = 0.4f, stiffness = 800f),
): State<Float> {
    val pressed by source.collectIsPressedAsState()
    return animateFloatAsState(if (pressed) 1f else 0f, spec, label = "pressProgress")
}

@Composable
private fun MorphingButton() {
    var isClicked by remember { mutableStateOf(false) }
    val text by remember(isClicked) {
        mutableStateOf(if (isClicked) "Reset" else "Morph me")
    }
    Button(
        onClick = { isClicked = !isClicked }
    ) {
        Text(text)
    }
}

@Preview(showBackground = true)
@Composable
private fun ButtonExperimentPreview() {
    ComposeLabTheme {
        ButtonExperiment.Content(onBack = {})
    }
}
