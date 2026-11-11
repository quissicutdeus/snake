package com.quissicutdeus.sniddle.game

import com.quissicutdeus.sniddle.game.sim.Thumbstick
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ThumbstickTest {
    private fun stick() = Thumbstick(deadZone = 10f, leash = 100f)

    @Test fun headsTheWayTheThumbIsDraggedFromWhereItLanded() {
        val s = stick()
        // Lands at the bottom of the screen; dragging up means "go up".
        s.press(500f, 1800f)
        assertNull(s.heading(), "no drag yet: keep going")
        s.move(500f, 1750f)
        assertEquals(-PI.toFloat() / 2, checkNotNull(s.heading()), 1e-4f)
    }

    @Test fun smallWigglesInsideTheDeadZoneChangeNothing() {
        val s = stick()
        s.press(0f, 0f)
        s.move(6f, -5f)
        assertNull(s.heading())
    }

    @Test fun aShortFlickBackTurnsAroundAfterALongDrag() {
        val s = stick()
        s.press(0f, 0f)
        s.move(600f, 0f) // dragged far right: the centre follows, staying 100 px behind
        assertEquals(500f, s.centerX, 1e-3f)
        assertEquals(0f, checkNotNull(s.heading()), 1e-4f)
        // Back left by less than the leash: now heading left.
        s.move(480f, 0f)
        assertEquals(PI.toFloat(), checkNotNull(s.heading()), 1e-4f)
    }

    @Test fun lettingGoForgetsTheDirectionAndTheNextTouchStartsFresh() {
        val s = stick()
        s.press(0f, 0f)
        s.move(50f, 0f)
        s.release()
        assertNull(s.heading())
        s.press(300f, 300f)
        assertNull(s.heading())
        s.move(300f, 350f)
        assertNotNull(s.heading())
        assertEquals(PI.toFloat() / 2, checkNotNull(s.heading()), 1e-4f)
    }
}
