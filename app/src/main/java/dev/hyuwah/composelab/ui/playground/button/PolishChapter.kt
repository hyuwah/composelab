/**
 * Chapter 4 — polish: what each morph state means to TalkBack, how indication behaves against a
 * shape that moves, and why "mute the ripple" needs a hand-built container.
 */
package dev.hyuwah.composelab.ui.playground.button

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import dev.hyuwah.composelab.ui.theme.ComposeLabTheme
import kotlinx.coroutines.delay

/** Every tuning number in this chapter. */
object ButtonMotion {
    /** The single axis the demo pill morphs on; corner, container and content all follow it. */
    val ShapeSpec: AnimationSpec<Float> = tween(durationMillis = 280, easing = FastOutSlowInEasing)

    const val IdleCornerDp = 28f
    const val BusyCornerDp = 12f

    /** Bounded with an unspecified radius means "fill this node", so the clip shapes the ripple. */
    const val RippleBounded = true
    val RippleRadius: Dp = Dp.Unspecified

    /** How long the demo stays busy, and how long a finished readout line stays legible. */
    const val BusyDwellMillis = 1_600L
    const val ReadoutDwellMillis = 1_400L

    const val IconSizeDp = 18f
    const val StrokeWidthDp = 2.5f
    const val ContentGapDp = 10f
    const val ContentPaddingHorizontalDp = 24f
    const val ContentPaddingVerticalDp = 14f

    const val ReadoutMinHeightDp = 44f
    const val SectionGapDp = 20f
    const val ChapterGapDp = 32f

    const val ModeDotSizeDp = 8f
    const val ModeGapDp = 8f
    const val ModeRowGapDp = 2f
    const val ModePaddingHorizontalDp = 6f
    const val ModePaddingVerticalDp = 6f
    const val ModeCornerDp = 10f
}

private const val ActionLabel = "Run action"

/** What TalkBack gets at one state: the icon-only fallback name, and the spoken state. */
private data class Announcement(val content: String?, val state: String)

private fun announcement(state: ActionState, label: String): Announcement = when (state) {
    ActionState.Idle -> Announcement(content = null, state = "Ready")
    ActionState.Loading -> Announcement(content = "$label, working", state = "In progress")
    ActionState.Success -> Announcement(content = "$label, complete", state = "Succeeded")
    ActionState.Failed -> Announcement(content = "$label, failed", state = "Failed")
}

/**
 * All per-state accessibility in one place: a polite live region so the change is spoken without
 * focus, a spoken state, and a name for the states whose visual is an icon alone.
 */
fun Modifier.polishedSemantics(state: ActionState, label: String): Modifier {
    val said = announcement(state, label)
    return semantics {
        liveRegion = LiveRegionMode.Polite
        stateDescription = said.state
        if (said.content != null) contentDescription = said.content
    }
}

/** The readout's copy, taken from the same table so the two cannot drift apart. */
private fun spokenLine(state: ActionState, label: String): String {
    val said = announcement(state, label)
    return "\u201C${said.content ?: label}, ${said.state}\u201D"
}

@Composable
fun PolishChapter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ButtonMotion.ChapterGapDp.dp),
    ) {
        AnnouncedStateDemo()
        RippleInterplayDemo()
    }
}

/**
 * The chapter 3 button with the accessibility story filled in: the same morph, but the icon-only
 * states are named and every change lands in a live region.
 */
@Composable
private fun AnnouncedStateDemo() {
    val runner = rememberActionRunner()
    var shown by remember { mutableStateOf(runner.state) }

    // TalkBack speaks at once; this readout holds a result for a beat so it can be read before
    // the line falls back to "Ready".
    LaunchedEffect(runner.state) {
        if (runner.state == ActionState.Idle) delay(ButtonMotion.ReadoutDwellMillis)
        shown = runner.state
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ButtonMotion.SectionGapDp.dp),
    ) {
        MorphingActionButton(
            state = runner.state,
            onClick = runner::startOrReset,
            modifier = Modifier.polishedSemantics(runner.state, ActionLabel),
        )
        Text(
            text = "TalkBack: ${spokenLine(shown, ActionLabel)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonMotion.ReadoutMinHeightDp.dp),
        )
        Caption(
            "stateDescription plus a polite live region. The spinner, check and error states get spoken " +
                "names instead of relying on color.",
        )
    }
}

/** Indication choices for the pill. `None` is the mode M3's own `Surface(onClick)` cannot offer. */
private enum class RippleMode(val label: String) {
    Default("Default ripple"),
    MutedWhileBusy("Muted while busy"),
    None("No ripple"),
}

/**
 * The teaching point: the same press, three indications. The pill's shape and colors animate, and
 * the clip that follows the shape is also what contains the ripple.
 */
@Composable
private fun RippleInterplayDemo() {
    var busy by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(RippleMode.Default) }

    LaunchedEffect(busy) {
        if (busy) {
            delay(ButtonMotion.BusyDwellMillis)
            busy = false
        }
    }

    val ripple = ripple(bounded = ButtonMotion.RippleBounded, radius = ButtonMotion.RippleRadius)
    val indication: Indication? = when (mode) {
        RippleMode.Default -> ripple
        RippleMode.MutedWhileBusy -> if (busy) null else ripple
        RippleMode.None -> null
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ButtonMotion.SectionGapDp.dp),
    ) {
        RipplePill(busy = busy, indication = indication, onClick = { busy = !busy })
        RippleModePicker(selected = mode, onSelect = { mode = it })
        Caption(
            "indication = null really is no ripple. M3's Surface(onClick) has no switch for that, " +
                "so this pill is a local clickable clipped to its animated shape.",
        )
    }
}

/** A hand-built pressable pill: clip first, then background, then clickable with a chosen indication. */
@Composable
private fun RipplePill(
    busy: Boolean,
    indication: Indication?,
    onClick: () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = if (busy) 1f else 0f,
        animationSpec = ButtonMotion.ShapeSpec,
        label = "pillMorph",
    )
    val shape = RoundedCornerShape(
        lerpDp(ButtonMotion.IdleCornerDp.dp, ButtonMotion.BusyCornerDp.dp, progress),
    )
    val container = lerpColor(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondaryContainer,
        progress,
    )
    val content = lerpColor(
        MaterialTheme.colorScheme.onPrimary,
        MaterialTheme.colorScheme.onSecondaryContainer,
        progress,
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(shape)
            .background(container)
            .clickable(
                interactionSource = interactionSource,
                indication = indication,
                role = Role.Button,
                onClick = onClick,
            )
            .heightIn(min = ButtonGeometry.Height)
            .padding(
                horizontal = ButtonMotion.ContentPaddingHorizontalDp.dp,
                vertical = ButtonMotion.ContentPaddingVerticalDp.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ButtonMotion.ContentGapDp.dp),
        ) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ButtonMotion.IconSizeDp.dp),
                    color = content,
                    strokeWidth = ButtonMotion.StrokeWidthDp.dp,
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Sync,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(ButtonMotion.IconSizeDp.dp),
                )
            }
            Text(
                text = if (busy) "Syncing" else "Sync now",
                style = MaterialTheme.typography.labelLarge,
                color = content,
            )
        }
    }
}

/** Three mutually exclusive modes, so the difference is one tap away instead of one line of code. */
@Composable
private fun RippleModePicker(selected: RippleMode, onSelect: (RippleMode) -> Unit) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(ButtonMotion.ModeRowGapDp.dp),
    ) {
        RippleMode.entries.forEach { option ->
            val isSelected = option == selected
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(ButtonMotion.ModeCornerDp.dp))
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    )
                    .padding(
                        horizontal = ButtonMotion.ModePaddingHorizontalDp.dp,
                        vertical = ButtonMotion.ModePaddingVerticalDp.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ButtonMotion.ModeGapDp.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(ButtonMotion.ModeDotSizeDp.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                        ),
                )
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelMedium,
                    color =
                        if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(name = "Chapter (interactive)", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PolishChapterPreview() {
    ComposeLabTheme { PolishChapter() }
}

@Preview(name = "Pill · idle", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PolishPillIdlePreview() {
    ComposeLabTheme {
        Column(modifier = Modifier.padding(12.dp)) {
            RipplePill(busy = false, indication = ripple(), onClick = {})
        }
    }
}

@Preview(name = "Pill · busy", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PolishPillBusyPreview() {
    ComposeLabTheme {
        Column(modifier = Modifier.padding(12.dp)) {
            RipplePill(busy = true, indication = ripple(), onClick = {})
        }
    }
}
