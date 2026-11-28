/**
 * Chapter 3 — press-and-hold morph: the article 1 state machine and morph visuals, retargeted by a
 * hold gesture instead of a tap. Nothing about the morph is reimplemented here.
 */
package dev.hyuwah.composelab.ui.casestudy.speak

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hyuwah.composelab.ui.playground.button.ActionState
import dev.hyuwah.composelab.ui.playground.button.actionSemantics
import dev.hyuwah.composelab.ui.playground.button.rememberMorphVisuals
import dev.hyuwah.composelab.ui.theme.ComposeLabTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object CtaMotion {
    /** How long "Sent" stays before the button morphs back to Idle. */
    const val SuccessDwellMillis = 900L

    /** Failure reads slower than success: the reader has to notice it and decide what to do next. */
    const val FailedDwellMillis = 2_400L

    /** A hold shorter than this is rejected instead of sent. */
    const val MinHoldMillis = 5_000L

    val ContentSpec: FiniteAnimationSpec<Float> = tween(durationMillis = 160)
    const val ContentScaleFrom = 0.92f

    /** Dot size range while listening, in dp. */
    const val DotMinDp = 8f
    const val DotMaxDp = 22f

    /** Countdown ring shown until the minimum hold is reached. */
    const val RingSizeDp = 18f
    const val RingStrokeDp = 2.5f
    const val RingTrackAlpha = 0.3f

    const val MinHeightDp = 56f
    const val HorizontalPaddingDp = 30f
    const val VerticalPaddingDp = 16f
}

/** Pure: a released hold becomes [ActionState.Success] only when it lasted long enough. */
fun releaseOutcome(heldMillis: Long): ActionState =
    if (heldMillis >= CtaMotion.MinHoldMillis) ActionState.Success else ActionState.Failed

/** Pure: how long a finished result stays on screen. Failure gets longer than success. */
fun resultDwellMillis(outcome: ActionState): Long = when (outcome) {
    ActionState.Failed -> CtaMotion.FailedDwellMillis
    else -> CtaMotion.SuccessDwellMillis
}

/**
 * Hold to record, release to send. Uses [ActionState] and [rememberMorphVisuals] from article 1;
 * [amplitude] (0f..1f) only drives the listening dot, so the recording state is visibly alive.
 */
@Composable
fun SpeakCta(
    amplitude: Float,
    modifier: Modifier = Modifier,
    onStateChange: (ActionState) -> Unit = {},
) {
    var state by remember { mutableStateOf<ActionState>(ActionState.Idle) }
    var heldMillis by remember { mutableLongStateOf(0L) }
    var pressStartedAt by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state) { onStateChange(state) }

    // Ticks only while pressed, so the countdown ring has something to show.
    LaunchedEffect(state) {
        while (state == ActionState.Loading) {
            withFrameNanos { heldMillis = (System.nanoTime() - pressStartedAt) / 1_000_000 }
        }
    }

    SpeakCtaPill(
        state = state,
        amplitude = amplitude,
        holdProgress = heldMillis.toFloat() / CtaMotion.MinHoldMillis,
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressStartedAt = System.nanoTime()
                    heldMillis = 0L
                    state = ActionState.Loading
                    val released = tryAwaitRelease()
                    val held = (System.nanoTime() - pressStartedAt) / 1_000_000
                    val outcome = if (released) releaseOutcome(held) else ActionState.Idle
                    state = outcome
                    if (outcome != ActionState.Idle) {
                        scope.launch {
                            delay(resultDwellMillis(outcome))
                            if (state == outcome) state = ActionState.Idle
                        }
                    }
                },
            )
        },
    )
}

/**
 * Stateless pill: state and hold progress in, pixels out. The gesture layer and the per-state
 * previews both draw through this, so a preview cannot drift from the real button.
 */
@Composable
fun SpeakCtaPill(
    state: ActionState,
    amplitude: Float,
    holdProgress: Float,
    modifier: Modifier = Modifier,
) {
    val visuals = rememberMorphVisuals(state)
    val shape = RoundedCornerShape(visuals.corner)

    Box(
        modifier = modifier
            .clip(shape)
            .background(visuals.container)
            .actionSemantics(state)
            .heightIn(min = CtaMotion.MinHeightDp.dp)
            .padding(
                horizontal = CtaMotion.HorizontalPaddingDp.dp,
                vertical = CtaMotion.VerticalPaddingDp.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (
                    fadeIn(CtaMotion.ContentSpec) + scaleIn(CtaMotion.ContentSpec, CtaMotion.ContentScaleFrom)
                    ) togetherWith (
                    fadeOut(CtaMotion.ContentSpec) + scaleOut(CtaMotion.ContentSpec, CtaMotion.ContentScaleFrom)
                    ) using SizeTransform(clip = false)
            },
            label = "ctaContent",
        ) { animated ->
            CtaContent(
                state = animated,
                amplitude = amplitude,
                color = visuals.content,
                holdProgress = holdProgress,
            )
        }
    }
}

@Composable
private fun CtaContent(
    state: ActionState,
    amplitude: Float,
    color: Color,
    holdProgress: Float,
) {
    when (state) {
        ActionState.Idle -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Rounded.Mic, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(text = "Hold to speak", color = color, style = MaterialTheme.typography.labelLarge)
        }
        ActionState.Loading -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "Listening", color = color, style = MaterialTheme.typography.labelLarge)
            val fraction = holdProgress.coerceIn(0f, 1f)
            if (fraction < 1f) {
                // Countdown to the 5 s minimum, so a rejected hold is never a surprise.
                CircularProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.size(CtaMotion.RingSizeDp.dp),
                    color = color,
                    trackColor = color.copy(alpha = CtaMotion.RingTrackAlpha),
                    strokeWidth = CtaMotion.RingStrokeDp.dp,
                )
            } else {
                val dot = CtaMotion.DotMinDp + (CtaMotion.DotMaxDp - CtaMotion.DotMinDp) * amplitude.coerceIn(0f, 1f)
                Box(Modifier.size(dot.dp).clip(CircleShape).background(color))
            }
        }
        ActionState.Success -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text(text = "Sent", color = color, style = MaterialTheme.typography.labelLarge)
        }
        ActionState.Failed -> Text(
            text = "Too short, hold ${CtaMotion.MinHoldMillis / 1000}s",
            color = color,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Preview(name = "Live (interactive)", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun SpeakCtaPreview() {
    ComposeLabTheme { SpeakCta(amplitude = 0.6f, modifier = Modifier.padding(12.dp)) }
}

@Preview(name = "Idle", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun SpeakCtaIdlePreview() {
    ComposeLabTheme {
        SpeakCtaPill(state = ActionState.Idle, amplitude = 0f, holdProgress = 0f, modifier = Modifier.padding(12.dp))
    }
}

@Preview(name = "Listening, counting down", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun SpeakCtaListeningPreview() {
    ComposeLabTheme {
        SpeakCtaPill(state = ActionState.Loading, amplitude = 0.4f, holdProgress = 0.45f, modifier = Modifier.padding(12.dp))
    }
}

@Preview(name = "Listening, past the minimum", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun SpeakCtaReadyPreview() {
    ComposeLabTheme {
        SpeakCtaPill(state = ActionState.Loading, amplitude = 0.85f, holdProgress = 1f, modifier = Modifier.padding(12.dp))
    }
}

@Preview(name = "Too short", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun SpeakCtaTooShortPreview() {
    ComposeLabTheme {
        SpeakCtaPill(state = ActionState.Failed, amplitude = 0f, holdProgress = 0f, modifier = Modifier.padding(12.dp))
    }
}

@Preview(name = "Sent", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun SpeakCtaSentPreview() {
    ComposeLabTheme {
        SpeakCtaPill(state = ActionState.Success, amplitude = 0f, holdProgress = 0f, modifier = Modifier.padding(12.dp))
    }
}
