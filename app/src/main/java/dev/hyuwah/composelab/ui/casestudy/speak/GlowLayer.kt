/**
 * Teaches gated light: the halos only bloom while listening (intensity 0..1), and additive light never
 * works on white — on light surfaces switch to SrcOver with a saturated, darker halo instead of Plus.
 */
package dev.hyuwah.composelab.ui.casestudy.speak

import android.content.res.Configuration
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hyuwah.composelab.ui.theme.ComposeLabTheme

object GlowMotion {
    /** How long the glow takes to fade in as listening starts and out when it stops. */
    internal const val GateDurationMs: Int = 240
    internal val GateSpec: AnimationSpec<Float> = tween(durationMillis = GateDurationMs)

    /** Dark surface: additive light reads well, so keep it bright and low-alpha. */
    const val DarkMinRadius: Float = 0.55f
    const val DarkMaxRadius: Float = 1.15f
    const val DarkMinAlpha: Float = 0.10f
    const val DarkMaxAlpha: Float = 0.55f

    /** Light surface: additive washes out on white — go opaque, wider and stronger. */
    const val LightMinRadius: Float = 0.62f
    const val LightMaxRadius: Float = 1.28f
    const val LightMinAlpha: Float = 0.20f
    const val LightMaxAlpha: Float = 0.78f

    const val StopInner: Float = 0.0f
    const val StopMid: Float = 0.62f
    const val StopOuter: Float = 1.0f

    internal const val BoxSizeDp = 260f
    internal const val MidRadiusFactor = 0.6f
    internal const val CoreRadiusFactor = 0.28f
    internal const val MidAlphaFactor = 0.7f
    internal const val CoreAlphaFactor = 0.55f
    internal const val MidStopAlphaFactor = 0.45f
    internal const val WarmMix = 0.65f

    /** Light mode pulls the halo toward black so it stays saturated and reads as a real halo. */
    internal const val LightDeepenMix = 0.25f
    internal const val LightCoreDarken = 0.22f
}

/**
 * Draws the amplitude-shaped glow halo on its own — no tap target, no content.
 *
 * [intensity] is the listening gate (0 = off, 1 = full); it scales alpha only, so the glow fades
 * in and out without changing the breathing shape that [amplitude] gives it.
 */
@Composable
fun SpeakGlow(
    amplitude: Float,
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
) {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() > 0.5f
    val blendMode = if (isLight) BlendMode.SrcOver else BlendMode.Plus

    val warm = lerp(scheme.primary, scheme.tertiary, GlowMotion.WarmMix)
    val haloColor = if (isLight) lerp(warm, Color.Black, GlowMotion.LightDeepenMix) else warm
    val midColor = if (isLight) haloColor else scheme.primary
    val coreColor = if (isLight) {
        lerp(haloColor, Color.Black, GlowMotion.LightCoreDarken)
    } else {
        lerp(haloColor, Color.White, GlowMotion.WarmMix)
    }

    Canvas(modifier = modifier) {
        val amp = amplitude.coerceIn(0f, 1f)
        val gate = intensity.coerceIn(0f, 1f)

        val minRadius = if (isLight) GlowMotion.LightMinRadius else GlowMotion.DarkMinRadius
        val maxRadius = if (isLight) GlowMotion.LightMaxRadius else GlowMotion.DarkMaxRadius
        val minAlpha = if (isLight) GlowMotion.LightMinAlpha else GlowMotion.DarkMinAlpha
        val maxAlpha = if (isLight) GlowMotion.LightMaxAlpha else GlowMotion.DarkMaxAlpha

        val haloFraction = minRadius + (maxRadius - minRadius) * amp
        val peakAlpha = (minAlpha + (maxAlpha - minAlpha) * amp) * gate
        if (peakAlpha <= 0f) return@Canvas

        val baseRadius = size.minDimension / 2f
        val haloRadius = baseRadius * haloFraction

        drawHalo(center, haloRadius, haloColor, peakAlpha, blendMode)
        drawHalo(
            center = center,
            radius = haloRadius * GlowMotion.MidRadiusFactor,
            color = midColor,
            alpha = peakAlpha * GlowMotion.MidAlphaFactor,
            blendMode = blendMode,
        )
        drawHalo(
            center = center,
            radius = haloRadius * GlowMotion.CoreRadiusFactor,
            color = coreColor,
            alpha = peakAlpha * GlowMotion.CoreAlphaFactor,
            blendMode = blendMode,
        )
    }
}

/** Draws an amplitude-driven glow halo and centres [content] on top of it. */
@Composable
fun SpeakGlowButton(
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(GlowMotion.BoxSizeDp.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SpeakGlow(amplitude = amplitude, intensity = intensity, modifier = Modifier.fillMaxSize())

        Box(modifier = Modifier.padding(4.dp), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

private fun DrawScope.drawHalo(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
    blendMode: BlendMode,
) {
    val brush = Brush.radialGradient(
        colorStops = arrayOf(
            GlowMotion.StopInner to color.copy(alpha = alpha),
            GlowMotion.StopMid to color.copy(alpha = alpha * GlowMotion.MidStopAlphaFactor),
            GlowMotion.StopOuter to Color.Transparent,
        ),
        center = center,
        radius = radius,
    )
    drawCircle(
        brush = brush,
        radius = radius,
        center = center,
        blendMode = blendMode,
    )
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFF4F1FA,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    widthDp = 320,
    heightDp = 320,
)
@Composable
private fun SpeakGlowLightPreview() {
    ComposeLabTheme(darkTheme = false, dynamicColor = false) {
        SpeakGlowButton(amplitude = 0.9f, intensity = 1f, onClick = {}) {
            PreviewButtonLabel()
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF101014,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = 320,
    heightDp = 320,
)
@Composable
private fun SpeakGlowDarkPreview() {
    ComposeLabTheme(darkTheme = true, dynamicColor = false) {
        SpeakGlowButton(amplitude = 0.9f, intensity = 1f, onClick = {}) {
            PreviewButtonLabel()
        }
    }
}

@Composable
private fun PreviewButtonLabel() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .background(scheme.primary, CircleShape)
            .padding(horizontal = 28.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Hold to speak", color = scheme.onPrimary)
    }
}
