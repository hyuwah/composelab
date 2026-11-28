/**
 * Chapter 1 — press feedback with `animate*AsState`: one float, one spec, zero gesture code.
 * The press progress already exists in the interaction stream; this file only maps it to pixels.
 */
package dev.hyuwah.composelab.ui.playground.button

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hyuwah.composelab.ui.theme.ComposeLabTheme
import androidx.compose.ui.util.lerp

object BounceMotion {
    /** Rest scale of 1f, fully pressed 0.95f. */
    const val PressedScale = 0.95f

    /** How far the button travels down while pressed, in dp. */
    const val PressedLiftDp = 2f

    /** Press-in is fast and tight; release is a little softer. Feel is a knob, not a truth. */
    val PressIn: AnimationSpec<Float> = spring(dampingRatio = 0.35f, stiffness = 2200f)
    val PressOut: AnimationSpec<Float> = spring(dampingRatio = 0.45f, stiffness = 1200f)
}

/** Pure mapping, no Compose state: this is what the value tests assert. */
fun bounceScale(progress: Float): Float = lerp(1f, BounceMotion.PressedScale, progress.coerceIn(0f, 1f))

/** Vertical travel in px for a press progress; `liftPx` is the fully-pressed depth. */
fun bounceLift(progress: Float, liftPx: Float): Float = liftPx * progress.coerceIn(0f, 1f)

/**
 * Reads [pressProgress] inside `graphicsLayer`, so every frame is a draw-phase change:
 * no recomposition, no allocation, no text relayout.
 */
fun Modifier.bouncePress(pressProgress: State<Float>, liftPx: Float): Modifier = graphicsLayer {
    val scale = bounceScale(pressProgress.value)
    scaleX = scale
    scaleY = scale
    translationY = bounceLift(pressProgress.value, liftPx)
}

@Composable
fun BounceChapter(modifier: Modifier = Modifier) {
    var frozen by remember { mutableStateOf(false) }
    var scrubbed by remember { mutableFloatStateOf(0f) }
    val frozenProgress = rememberUpdatedState(scrubbed)
    val liftPx = with(LocalDensity.current) { BounceMotion.PressedLiftDp.dp.toPx() }

    Column(
        modifier = modifier,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PressableSurface(
            onClick = {},
            pressModifier = { live -> Modifier.bouncePress(if (frozen) frozenProgress else live, liftPx) },
        ) {
            LabeledContent(
                icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                text = "Press me",
            )
        }
        Text(text = "animate*AsState", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
        ScrubRow(
            label = "Freeze press progress",
            frozen = frozen,
            onFrozenChange = { frozen = it },
            value = scrubbed,
            onValueChange = { scrubbed = it },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun BounceChapterPreview() {
    ComposeLabTheme { BounceChapter() }
}
