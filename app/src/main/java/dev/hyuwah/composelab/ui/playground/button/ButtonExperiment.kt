package dev.hyuwah.composelab.ui.playground.button

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hyuwah.composelab.ui.experiments.Experiment
import dev.hyuwah.composelab.ui.experiments.ExperimentScaffold
import dev.hyuwah.composelab.ui.theme.ComposeLabTheme
import kotlinx.coroutines.delay

/**
 * Experiment: buttons as an additive ladder.
 *
 * Every rung below is [PressableSurface] plus exactly one idea:
 *
 *   PressableSurface              → container: shape, color, ripple, press progress, content slot
 *   1. SimpleButton    = base + nothing
 *   2. BouncingButton  = rung 1 + Modifier.bouncePress(...)
 *   3. ActionButton    = rung 2 + ActionRunner (4 discrete states, instant swap)
 *   4. MorphingButton  = rung 3 + animate*AsState + AnimatedContent
 *
 * The point of the file is the delta, so each rung is deliberately short: if a rung starts
 * growing, the thing it adds belongs in [PressableSurface] or in a modifier, not in the rung.
 *
 * Rung 3 is where the jump is: the button changes width and color with no transition at all.
 * That is deliberate — rung 4 is the same code with the jump smoothed out.
 */
object ButtonExperiment : Experiment {
    override val id = "button"
    override val title = "Button playground"
    override val description = "One container, four rungs. Each advanced button = the one above it + one idea."

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
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
            ) {
                Rung(caption = "1 · PressableSurface", note = "container only") {
                    SimpleButton()
                }
                Rung(caption = "2 · + Modifier.bouncePress", note = "motion is a Modifier, not a component") {
                    BouncingButton()
                }
                Rung(caption = "3 · + ActionRunner", note = "4 discrete states — watch the jump") {
                    ActionButton()
                }
                Rung(caption = "4 · + animate*AsState + AnimatedContent", note = "same states, smoothed") {
                    MorphingButton()
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// The container. Thin on purpose: it owns interaction + slots, never content.
// ---------------------------------------------------------------------------------------------

private val PillShape = RoundedCornerShape(percent = 50)

/**
 * A pressable container: shape, colors, ripple, interaction source, press progress, content slot.
 *
 * Built on `Surface(onClick = …)` rather than `Box + clickable` so we keep
 * `minimumInteractiveComponentSize` (48dp touch target), `role = Role.Button`, ripple indication
 * and `LocalContentColor` propagation for free.
 *
 * @param pressModifier turns the press progress (0f → rest, 1f → pressed) into a Modifier.
 *   Applied *after* the caller's modifier, so the animated value is read inside `graphicsLayer`
 *   and never triggers a recomposition.
 */
@Composable
fun PressableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = PillShape,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    minHeight: Dp = 48.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    pressModifier: (State<Float>) -> Modifier = { Modifier },
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressProgress = rememberPressProgress(interactionSource)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.then(pressModifier(pressProgress)),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = minHeight)
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
            content = { content() },
        )
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
    spec: AnimationSpec<Float> = spring(dampingRatio = 0.4f, stiffness = 900f),
): State<Float> {
    val pressed by source.collectIsPressedAsState()
    return animateFloatAsState(if (pressed) 1f else 0f, spec, label = "pressProgress")
}

// ---------------------------------------------------------------------------------------------
// Rung 1: base + nothing
// ---------------------------------------------------------------------------------------------

@Composable
private fun SimpleButton() {
    PressableSurface(onClick = { /* experiment */ }) {
        Text("Tap me", style = MaterialTheme.typography.labelLarge)
    }
}

// ---------------------------------------------------------------------------------------------
// Rung 2: base + one modifier
// ---------------------------------------------------------------------------------------------

@Composable
private fun BouncingButton() {
    PressableSurface(
        onClick = { /* experiment */ },
        pressModifier = { progress -> Modifier.bouncePress(progress) },
    ) {
        Text("Bounce me", style = MaterialTheme.typography.labelLarge)
    }
}

// ---------------------------------------------------------------------------------------------
// Rung 3: rung 2 + a state machine. No animation anywhere in this section on purpose.
// ---------------------------------------------------------------------------------------------

/** Discrete state of an async action. Sealed, not `enum` + flags — the result can carry data later. */
sealed interface ActionState {
    data object Idle : ActionState
    data object Loading : ActionState
    data object Success : ActionState
    data object Failed : ActionState
}

private const val FakeWorkMillis = 1_800L
private const val ResultDwellMillis = 1_400L

private val ButtonHeight = 56.dp

/**
 * The state machine, hoisted out of the UI so rungs 3 and 4 share it and differ only in
 * *presentation*. Rung 3 renders `state` with a plain `when`; rung 4 wraps the same value in
 * `AnimatedContent`. If this lived inside a composable, the two rungs would drift.
 */
@Stable
private class ActionRunner {
    var state by mutableStateOf<ActionState>(ActionState.Idle)
        private set

    private var run by mutableIntStateOf(0)

    /** Tap handler: start a run, or cut the result dwell short. */
    fun startOrReset() {
        if (state == ActionState.Idle) {
            run += 1
            state = ActionState.Loading
        } else {
            state = ActionState.Idle
        }
    }

    /** The fake request. Every 3rd run fails so the failure path is visible without faking it. */
    suspend fun simulate() {
        when (state) {
            ActionState.Idle -> Unit
            ActionState.Loading -> {
                delay(FakeWorkMillis)
                state = if (run % 3 == 0) ActionState.Failed else ActionState.Success
            }
            ActionState.Success, ActionState.Failed -> {
                delay(ResultDwellMillis)
                state = ActionState.Idle
            }
        }
    }
}

@Composable
private fun rememberActionRunner(): ActionRunner {
    val runner = remember { ActionRunner() }
    // Keyed on `state`, so a tap during the result window cancels the pending auto-reset
    // instead of racing it.
    LaunchedEffect(runner.state) { runner.simulate() }
    return runner
}

@Composable
private fun ActionButton() {
    val action = rememberActionRunner()

    PressableSurface(
        onClick = action::startOrReset,
        // Guard against double submits: the only state that ignores taps is Loading.
        enabled = action.state != ActionState.Loading,
        modifier = Modifier.actionSemantics(action.state),
        minHeight = ButtonHeight,
        contentPadding = PaddingValues(horizontal = 24.dp),
        pressModifier = { progress -> Modifier.bouncePress(progress) },
    ) {
        // Instant swap: no color animation, no corner animation, no AnimatedContent.
        // The width and color jump straight to the next state — that is what rung 4 fixes.
        ActionContent(action.state)
    }
}

// ---------------------------------------------------------------------------------------------
// Rung 4: rung 3 + animation. Same runner, same content, only the presentation is smoothed.
// ---------------------------------------------------------------------------------------------

// Material 3 has no "success" color role, so this experiment keeps its own pair instead of
// hacking `tertiary` into the theme.
private val SuccessContainer = Color(0xFF2E7D32)
private val OnSuccessContainer = Color(0xFFF1FFF0)

@Composable
private fun MorphingButton() {
    val action = rememberActionRunner()

    // One animated property per visual axis. Not one float driving all four.
    val containerColor by animateColorAsState(
        targetValue = when (action.state) {
            ActionState.Idle -> MaterialTheme.colorScheme.primary
            ActionState.Loading -> MaterialTheme.colorScheme.secondaryContainer
            ActionState.Success -> SuccessContainer
            ActionState.Failed -> MaterialTheme.colorScheme.error
        },
        animationSpec = tween(durationMillis = 400),
        label = "containerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = when (action.state) {
            ActionState.Idle -> MaterialTheme.colorScheme.onPrimary
            ActionState.Loading -> MaterialTheme.colorScheme.onSecondaryContainer
            ActionState.Success -> OnSuccessContainer
            ActionState.Failed -> MaterialTheme.colorScheme.onError
        },
        animationSpec = tween(durationMillis = 400),
        label = "contentColor",
    )
    val cornerRadius by animateDpAsState(
        targetValue = when (action.state) {
            ActionState.Idle, ActionState.Loading -> ButtonHeight / 2
            ActionState.Success, ActionState.Failed -> 14.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "cornerRadius",
    )

    PressableSurface(
        onClick = action::startOrReset,
        enabled = action.state != ActionState.Loading,
        modifier = Modifier.actionSemantics(action.state),
        shape = RoundedCornerShape(cornerRadius),
        containerColor = containerColor,
        contentColor = contentColor,
        minHeight = ButtonHeight,
        contentPadding = PaddingValues(horizontal = 24.dp),
        // Everything rung 2 and 3 had, unchanged.
        pressModifier = { progress -> Modifier.bouncePress(progress) },
    ) {
        AnimatedContent(
            targetState = action.state,
            contentAlignment = Alignment.Center,
            transitionSpec = {
                val enter = fadeIn(tween(220)) +
                    scaleIn(initialScale = 0.8f, animationSpec = tween(220))
                val exit = fadeOut(tween(140)) +
                    scaleOut(targetScale = 0.8f, animationSpec = tween(140))
                // SizeTransform owns the width morph. Do NOT also put animateContentSize() on the
                // container — two size animators fight and the button jitters.
                (enter togetherWith exit).using(
                    SizeTransform(
                        clip = false,
                        sizeAnimationSpec = { _, _ ->
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            )
                        },
                    )
                )
            },
            label = "actionContent",
        ) { target ->
            ActionContent(target)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Shared by rungs 3 and 4 — the only difference between them is how this value is presented.
// ---------------------------------------------------------------------------------------------

@Composable
private fun ActionContent(state: ActionState) {
    when (state) {
        ActionState.Idle -> LabeledContent(
            icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
            text = "Run action",
        )
        ActionState.Loading -> CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = LocalContentColor.current,
            strokeWidth = 2.5.dp,
        )
        ActionState.Success -> LabeledContent(
            icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp)) },
            text = "Success",
        )
        ActionState.Failed -> LabeledContent(
            icon = { Icon(Icons.Rounded.ErrorOutline, contentDescription = null, modifier = Modifier.size(18.dp)) },
            text = "Failed",
        )
    }
}

@Composable
private fun LabeledContent(icon: @Composable () -> Unit, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** The visual change is icon-only, so without this TalkBack would announce nothing. */
private fun Modifier.actionSemantics(state: ActionState): Modifier = semantics {
    liveRegion = LiveRegionMode.Polite
    stateDescription = when (state) {
        ActionState.Idle -> "Ready"
        ActionState.Loading -> "In progress"
        ActionState.Success -> "Succeeded"
        ActionState.Failed -> "Failed"
    }
}

// ---------------------------------------------------------------------------------------------
// Comparison chrome
// ---------------------------------------------------------------------------------------------

@Composable
private fun Rung(caption: String, note: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = caption,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = note,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Preview(showBackground = true)
@Composable
private fun ButtonExperimentPreview() {
    ComposeLabTheme {
        ButtonExperiment.Content(onBack = {})
    }
}
