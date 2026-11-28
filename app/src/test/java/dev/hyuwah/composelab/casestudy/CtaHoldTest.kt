package dev.hyuwah.composelab.casestudy

import dev.hyuwah.composelab.ui.casestudy.speak.CtaMotion
import dev.hyuwah.composelab.ui.casestudy.speak.releaseOutcome
import dev.hyuwah.composelab.ui.casestudy.speak.resultDwellMillis
import dev.hyuwah.composelab.ui.playground.button.ActionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Value tests for the hold threshold: a short press must never become "Sent". */
class CtaHoldTest {

    @Test
    fun tapIsRejected() {
        assertEquals(ActionState.Failed, releaseOutcome(0L))
        assertEquals(ActionState.Failed, releaseOutcome(120L))
    }

    @Test
    fun justUnderTheMinimumIsRejected() {
        assertEquals(ActionState.Failed, releaseOutcome(CtaMotion.MinHoldMillis - 1))
    }

    @Test
    fun exactlyTheMinimumSends() {
        assertEquals(ActionState.Success, releaseOutcome(CtaMotion.MinHoldMillis))
    }

    @Test
    fun longHoldSends() {
        assertEquals(ActionState.Success, releaseOutcome(CtaMotion.MinHoldMillis * 3))
    }

    @Test
    fun failureLingersLongerThanSuccess() {
        assertTrue(resultDwellMillis(ActionState.Failed) > resultDwellMillis(ActionState.Success))
        assertEquals(CtaMotion.FailedDwellMillis, resultDwellMillis(ActionState.Failed))
        assertEquals(CtaMotion.SuccessDwellMillis, resultDwellMillis(ActionState.Success))
    }
}
