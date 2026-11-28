/**
 * Chapter 2 — `Animatable`: a value you own — startable, cancellable, resumable — plus the velocity
 * it still had at the moment you interrupted it. That is what `animate*AsState` cannot give you.
 */
package dev.hyuwah.composelab.ui.playground.button

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hyuwah.composelab.ui.theme.ComposeLabTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object InterruptionMotion {
    /** Press-in spring. */
    val PressIn: AnimationSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 1800f)

    /** Release spring: the velocity carried into `animateTo` decides whether it settles or bounces once. */
    val Settle: AnimationSpec<Float> = spring(dampingRatio = 0.55f, stiffness = 1100f)

    /** Horizontal stretch at full press. */
    const val SquashX = 0.06f

    /** Vertical squash at full press. */
    const val SquashY = 0.04f

    /** How far into the press the "interrupted" demo reverses direction. */
    const val InterruptAfterMillis = 90L
}

/** Pure: horizontal scale for a press progress. */
fun interruptionSquashX(progress: Float): Float = 1f + InterruptionMotion.SquashX * progress.coerceIn(0f, 1f)

/** Pure: vertical scale for a press progress. */
fun interruptionSquashY(progress: Float): Float = 1f - InterruptionMotion.SquashY * progress.coerceIn(0f, 1f)

@Composable
fun InterruptionChapter(modifier: Modifier = Modifier) {
    var frozen by remember { mutableStateOf(false) }
    var scrubbed by remember { mutableFloatStateOf(0f) }

    // One Animatable per demo: two independent values, so each lesson can be watched alone.
    val hold = remember { Animatable(0f) }
    val interrupted = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    var carriedVelocity by remember { mutableFloatStateOf(0f) }
    var interruptJob by remember { mutableStateOf<Job?>(null) }

    // Frozen mode is a plain snapTo: the same Animatables, parked on one frame.
    LaunchedEffect(frozen, scrubbed) {
        if (frozen) {
            hold.snapTo(scrubbed)
            interrupted.snapTo(scrubbed)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        DemoBlock(
            index = "1",
            title = "Release keeps the velocity",
            caption = "Press starts a spring in. Release animates back with initialVelocity set to the " +
                "velocity at that moment, so the pill does not stop dead.",
        ) {
            SquashSurface(
                progress = hold.value,
                modifier = if (frozen) Modifier else Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            val pressJob = scope.launch { hold.animateTo(1f, InterruptionMotion.PressIn) }
                            tryAwaitRelease()
                            pressJob.cancel()
                            val carried = hold.velocity
                            carriedVelocity = carried
                            hold.animateTo(0f, InterruptionMotion.Settle, initialVelocity = carried)
                        },
                    )
                },
            ) { Text(text = "Hold me", color = MaterialTheme.colorScheme.onPrimary) }
            ValueReadout(label = "value / carried", value = hold.value, velocity = carriedVelocity)
        }

        DemoBlock(
            index = "2",
            title = "Reversed mid-flight",
            caption = "animateTo(1f), then animateTo(0f) ${InterruptionMotion.InterruptAfterMillis} ms later. " +
                "The value continues from the last frame instead of restarting at zero.",
        ) {
            SquashSurface(
                progress = interrupted.value,
                modifier = if (frozen) Modifier else Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            interruptJob?.cancel()
                            interruptJob = scope.launch { interrupted.animateTo(1f, InterruptionMotion.PressIn) }
                            val reverse = scope.launch {
                                delay(InterruptionMotion.InterruptAfterMillis)
                                interrupted.animateTo(0f, InterruptionMotion.Settle)
                            }
                            tryAwaitRelease()
                            reverse.cancel()
                        },
                    )
                },
            ) { Text(text = "Interrupt", color = MaterialTheme.colorScheme.onPrimary) }
            ValueReadout(label = "value / velocity", value = interrupted.value, velocity = interrupted.velocity)
        }

        ScrubRow(
            label = "Freeze press progress (both demos)",
            frozen = frozen,
            onFrozenChange = { frozen = it },
            value = scrubbed,
            onValueChange = { scrubbed = it },
        )
    }
}

/** Numbered lesson: one idea, its caption, and the thing to press. */
@Composable
private fun DemoBlock(
    index: String,
    title: String,
    caption: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "$index · $title",
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** Live value/velocity line. Reads the Animatable here, so only this Text recomposes every frame. */
@Composable
private fun ValueReadout(label: String, value: Float, velocity: Float) {
    Text(
        text = "$label  %.2f / %+.1f".format(value, velocity),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
    )
}

/** Same pill as the other chapters, but the press gesture drives the [progress] directly. */
@Composable
private fun SquashSurface(
    progress: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = interruptionSquashX(progress)
                scaleY = interruptionSquashY(progress)
            }
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.primary)
            .heightIn(min = 52.dp)
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun InterruptionChapterPreview() {
    ComposeLabTheme { InterruptionChapter() }
}
