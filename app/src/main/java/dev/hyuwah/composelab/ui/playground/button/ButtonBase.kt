package dev.hyuwah.composelab.ui.playground.button

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Shared skeleton for the four button chapters: the container, the action state machine and the
 * inspection chrome. Behaviour is unchanged from the original single-file experiment — only the
 * location moved, so each chapter file can be read on its own.
 */

/** Discrete state of an async action. Sealed, not `enum` + flags — a result can carry data later. */
sealed interface ActionState {
    data object Idle : ActionState
    data object Loading : ActionState
    data object Success : ActionState
    data object Failed : ActionState
}

/** Sizes the chapters agree on. Motion lives in each chapter file, never here. */
object ButtonGeometry {
    val Shape: Shape = RoundedCornerShape(percent = 50)
    val Height: Dp = 56.dp
    val ContentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
}

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
    shape: Shape = ButtonGeometry.Shape,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    minHeight: Dp = 48.dp,
    contentPadding: PaddingValues = ButtonGeometry.ContentPadding,
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

/** Press progress from the interaction stream: 0f at rest, 1f fully pressed, spring-smoothed. */
@Composable
fun rememberPressProgress(
    source: MutableInteractionSource,
    spec: AnimationSpec<Float> = spring(dampingRatio = 0.4f, stiffness = 900f),
): State<Float> {
    val pressed by source.collectIsPressedAsState()
    return animateFloatAsState(if (pressed) 1f else 0f, spec, label = "pressProgress")
}

// ---------------------------------------------------------------------------------------------
// The state machine, hoisted out of the UI so every chapter and article 2 share one truth.
// ---------------------------------------------------------------------------------------------

private const val FakeWorkMillis = 1_800L
private const val ResultDwellMillis = 1_400L

/**
 * Drives [ActionState] for a fake request. `simulate` is the only place that moves `state`
 * forward, so a tap during the result window cancels the pending auto-reset instead of racing it.
 */
@Stable
class ActionRunner {
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
fun rememberActionRunner(): ActionRunner {
    val runner = remember { ActionRunner() }
    // Keyed on `state`, so a tap during the result window cancels the pending auto-reset.
    LaunchedEffect(runner.state) { runner.simulate() }
    return runner
}

// ---------------------------------------------------------------------------------------------
// Content shared by every stateful chapter
// ---------------------------------------------------------------------------------------------

@Composable
fun ActionContent(state: ActionState) {
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
fun LabeledContent(icon: @Composable () -> Unit, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** The visual change is icon-only, so without this TalkBack would announce nothing. */
fun Modifier.actionSemantics(state: ActionState): Modifier = semantics {
    liveRegion = LiveRegionMode.Polite
    stateDescription = when (state) {
        ActionState.Idle -> "Ready"
        ActionState.Loading -> "In progress"
        ActionState.Success -> "Succeeded"
        ActionState.Failed -> "Failed"
    }
}

// ---------------------------------------------------------------------------------------------
// Inspection chrome: freeze a frame, then drag it. Every chapter gets the same control.
// ---------------------------------------------------------------------------------------------

/**
 * Freeze toggle + 0..1 slider. Off = the live animation; on = the value under the thumb, so a
 * single frame can be inspected or screenshotted.
 */
@Composable
fun ScrubRow(
    label: String,
    frozen: Boolean,
    onFrozenChange: (Boolean) -> Unit,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (frozen) value.toString().take(5) else "live",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
            )
            Switch(checked = frozen, onCheckedChange = onFrozenChange)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = frozen,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** One labeled slot in a catalog page: chapter number + title + the Compose API it teaches. */
@Composable
fun ChapterFrame(
    index: String,
    title: String,
    api: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "$index · $title",
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = api,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
