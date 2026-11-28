package dev.hyuwah.composelab.playground

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.hyuwah.composelab.ui.playground.button.MorphVisuals
import dev.hyuwah.composelab.ui.playground.button.lerpMorph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Value tests for chapter 3: one progress axis, so corner and colors must agree at every t. */
class MorphMotionTest {

    private val idle = MorphVisuals(container = Color(0xFF000000), content = Color.White, corner = 8.dp)
    private val busy = MorphVisuals(container = Color(0xFFFFFFFF), content = Color.Black, corner = 28.dp)

    @Test
    fun tZeroIsExactlyFrom() {
        val v = lerpMorph(idle, busy, 0f)
        assertEquals(8.dp, v.corner)
        assertEquals(0f, v.container.red, 1e-4f)
    }

    @Test
    fun tOneIsExactlyTo() {
        val v = lerpMorph(idle, busy, 1f)
        assertEquals(28.dp, v.corner)
        assertEquals(1f, v.container.red, 1e-4f)
    }

    @Test
    fun midpointInterpolatesCornerAndColor() {
        val v = lerpMorph(idle, busy, 0.5f)
        assertEquals(18.dp, v.corner)
        // Compose lerps Color through its color space, so mid grey is not exactly 0.5 in sRGB.
        // What must hold: the midpoint sits strictly between the two endpoints.
        assertTrue("container midpoint ${v.container.red}", v.container.red in 0.2f..0.8f)
        assertTrue("content midpoint ${v.content.red}", v.content.red in 0.2f..0.8f)
    }

    @Test
    fun tIsClamped() {
        assertEquals(8.dp, lerpMorph(idle, busy, -1f).corner)
        assertEquals(28.dp, lerpMorph(idle, busy, 2f).corner)
    }
}
