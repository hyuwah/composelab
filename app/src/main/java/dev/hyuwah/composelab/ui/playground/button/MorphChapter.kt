/**
 * Chapter 3 — the morph: one `Animatable<Float>` progress plus pure lerps for corner and colors,
 * while width follows the content. This is what `animate*AsState` does for a single property only.
 */
package dev.hyuwah.composelab.ui.playground.button

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import dev.hyuwah.composelab.ui.theme.ComposeLabTheme

/** Everything that changes between states, in one value. Pure data — easy to lerp, easy to test. */
@Immutable
data class MorphVisuals(
    val container: Color,
    val content: Color,
    val corner: Dp,
)

object MorphMotion {
    /** The single progress axis every morph property is derived from. */
    val ProgressSpec: AnimationSpec<Float> = tween(durationMillis = 320, easing = FastOutSlowInEasing)

    const val IdleCornerDp = 28f
    const val LoadingCornerDp = 18f
    const val SuccessCornerDp = 26f
    const val FailedCornerDp = 22f

    /** Content swap inside the morphing container. */
    val ContentSpec: FiniteAnimationSpec<Float> = tween(durationMillis = 160)
    const val ContentScaleFrom = 0.9f
}

/** Theme-aware visuals per state. */
@Composable
fun morphVisuals(state: ActionState): MorphVisuals = when (state) {
    ActionState.Idle -> MorphVisuals(
        container = MaterialTheme.colorScheme.primary,
        content = MaterialTheme.colorScheme.onPrimary,
        corner = MorphMotion.IdleCornerDp.dp,
    )
    ActionState.Loading -> MorphVisuals(
        container = MaterialTheme.colorScheme.secondaryContainer,
        content = MaterialTheme.colorScheme.onSecondaryContainer,
        corner = MorphMotion.LoadingCornerDp.dp,
    )
    ActionState.Success -> MorphVisuals(
        container = MaterialTheme.colorScheme.tertiary,
        content = MaterialTheme.colorScheme.onTertiary,
        corner = MorphMotion.SuccessCornerDp.dp,
    )
    ActionState.Failed -> MorphVisuals(
        container = MaterialTheme.colorScheme.errorContainer,
        content = MaterialTheme.colorScheme.onErrorContainer,
        corner = MorphMotion.FailedCornerDp.dp,
    )
}

/** Pure: the whole morph between two visual states. `t = 0` is [from], `t = 1` is [to]. */
fun lerpMorph(from: MorphVisuals, to: MorphVisuals, t: Float): MorphVisuals {
    val f = t.coerceIn(0f, 1f)
    return MorphVisuals(
        container = lerpColor(from.container, to.container, f),
        content = lerpColor(from.content, to.content, f),
        corner = lerpDp(from.corner, to.corner, f),
    )
}

/**
 * Animates toward the visuals of [state]. One `Animatable` carries the progress, so an
 * interrupted morph starts from the current frame instead of snapping back.
 *
 * @param scrubT when non-null, bypasses the animation and returns the Idle → Loading frame at
 *   that progress, so a single frame can be inspected.
 */
@Composable
fun rememberMorphVisuals(state: ActionState, scrubT: Float? = null): MorphVisuals {
    val target = morphVisuals(state)
    val idle = morphVisuals(ActionState.Idle)
    val busy = morphVisuals(ActionState.Loading)
    val from = remember { mutableStateOf(target) }
    val to = remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(target, scrubT) {
        if (scrubT != null) {
            from.value = idle
            to.value = busy
            progress.snapTo(scrubT)
        } else if (to.value != target) {
            // Bake the frame we are on into `from`, so retargeting mid-morph never jumps.
            from.value = lerpMorph(from.value, to.value, progress.value)
            to.value = target
            progress.snapTo(0f)
            progress.animateTo(1f, MorphMotion.ProgressSpec)
        }
    }

    return if (scrubT != null) {
        lerpMorph(idle, busy, scrubT)
    } else {
        lerpMorph(from.value, to.value, progress.value)
    }
}

/** A container that takes its shape and colors from [visuals]; no animation knowledge of its own. */
@Composable
fun MorphContainer(
    visuals: MorphVisuals,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    PressableSurface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(visuals.corner),
        containerColor = visuals.container,
        contentColor = visuals.content,
        content = content,
    )
}

/**
 * The morphing button: shape and color from [rememberMorphVisuals], width from the content swap.
 * Article 2 reuses [MorphContainer] and [rememberMorphVisuals] with a different state source.
 */
@Composable
fun MorphingActionButton(
    state: ActionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scrubT: Float? = null,
) {
    val visuals = rememberMorphVisuals(state, scrubT)
    MorphContainer(visuals = visuals, onClick = onClick, modifier = modifier) {
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (
                    fadeIn(MorphMotion.ContentSpec) + scaleIn(MorphMotion.ContentSpec, MorphMotion.ContentScaleFrom)
                    ) togetherWith (
                    fadeOut(MorphMotion.ContentSpec) + scaleOut(MorphMotion.ContentSpec, MorphMotion.ContentScaleFrom)
                    ) using SizeTransform(clip = false)
            },
            label = "actionContent",
        ) { animated -> ActionContent(animated) }
    }
}

@Composable
fun MorphChapter(modifier: Modifier = Modifier) {
    val runner = rememberActionRunner()
    var frozen by remember { mutableStateOf(false) }
    var t by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (frozen) {
            MorphingActionButton(state = ActionState.Idle, onClick = {}, scrubT = t)
        } else {
            MorphingActionButton(state = runner.state, onClick = runner::startOrReset)
        }
        Text(text = "Animatable<Float> + lerp", style = MaterialTheme.typography.labelSmall)
        ScrubRow(
            label = "Freeze morph (Idle → Loading)",
            frozen = frozen,
            onFrozenChange = { frozen = it },
            value = t,
            onValueChange = { t = it },
        )
    }
}

@Preview(name = "Chapter (interactive)", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MorphChapterPreview() {
    ComposeLabTheme { MorphChapter() }
}

@Preview(name = "State · Idle", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MorphIdlePreview() {
    ComposeLabTheme { MorphingActionButton(state = ActionState.Idle, onClick = {}, modifier = Modifier.padding(12.dp)) }
}

@Preview(name = "State · Loading", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MorphLoadingPreview() {
    ComposeLabTheme { MorphingActionButton(state = ActionState.Loading, onClick = {}, modifier = Modifier.padding(12.dp)) }
}

@Preview(name = "State · Success", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MorphSuccessPreview() {
    ComposeLabTheme { MorphingActionButton(state = ActionState.Success, onClick = {}, modifier = Modifier.padding(12.dp)) }
}

@Preview(name = "State · Failed", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun MorphFailedPreview() {
    ComposeLabTheme { MorphingActionButton(state = ActionState.Failed, onClick = {}, modifier = Modifier.padding(12.dp)) }
}

@Preview(name = "State · all four", showBackground = true, backgroundColor = 0xFFFFFFFF, heightDp = 360)
@Composable
private fun MorphAllStatesPreview() {
    ComposeLabTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            listOf(ActionState.Idle, ActionState.Loading, ActionState.Success, ActionState.Failed).forEach { state ->
                MorphingActionButton(state = state, onClick = {})
            }
        }
    }
}
