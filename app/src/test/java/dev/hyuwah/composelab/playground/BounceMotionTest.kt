package dev.hyuwah.composelab.playground

import dev.hyuwah.composelab.ui.playground.button.BounceMotion
import dev.hyuwah.composelab.ui.playground.button.bounceLift
import dev.hyuwah.composelab.ui.playground.button.bounceScale
import org.junit.Assert.assertEquals
import org.junit.Test

/** Value tests for chapter 1: the press progress must map to exact pixels, not "looks fine". */
class BounceMotionTest {

    @Test
    fun scaleAtRestIsOne() {
        assertEquals(1f, bounceScale(0f), 1e-5f)
    }

    @Test
    fun scaleAtFullPressIsPressedScale() {
        assertEquals(BounceMotion.PressedScale, bounceScale(1f), 1e-5f)
        assertEquals(0.95f, bounceScale(1f), 1e-5f)
    }

    @Test
    fun scaleIsLinearInProgress() {
        assertEquals(0.975f, bounceScale(0.5f), 1e-5f)
        assertEquals(0.9875f, bounceScale(0.25f), 1e-5f)
    }

    @Test
    fun scaleClampsOutsideZeroToOne() {
        assertEquals(1f, bounceScale(-1f), 1e-5f)
        assertEquals(BounceMotion.PressedScale, bounceScale(2f), 1e-5f)
    }

    @Test
    fun liftIsZeroAtRestAndFullDepthAtPress() {
        assertEquals(0f, bounceLift(0f, 4f), 1e-5f)
        assertEquals(2f, bounceLift(0.5f, 4f), 1e-5f)
        assertEquals(4f, bounceLift(1f, 4f), 1e-5f)
    }
}
