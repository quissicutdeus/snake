package com.quissicutdeus.sniddle.game

import com.quissicutdeus.sniddle.game.sim.hiddenFrom
import com.quissicutdeus.sniddle.game.sim.watchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SightTest {
    @Test fun withoutAReportedScreenItIsACircleAsWideAsTheWidestPhone() {
        val sim = emptySim()
        val kid = sim.player(2000f, 2000f, EAST, GameConfig.PLAYER_START_LENGTH)
        val far = GameConfig.offscreenDistance(kid.radius)
        val reach = 100f
        assertTrue(sim.hiddenFrom(kid, kid.x + far + reach, kid.y, reach), "just past the circle")
        assertFalse(sim.hiddenFrom(kid, kid.x + far + reach - 1f, kid.y, reach), "a coil reaching into the circle")
        assertFalse(sim.hiddenFrom(kid, kid.x, kid.y + far, reach), "same distance, any direction")
    }

    @Test fun aReportedScreenIsItsRectanglePlusAMargin() {
        val sim = emptySim()
        val kid = sim.player(2000f, 2000f, EAST, GameConfig.PLAYER_START_LENGTH)
        sim.setView(kid, 2000f, 2000f, 600f, 300f)
        val reach = 50f
        val edge = reach + GameConfig.SPAWN_VIEW_MARGIN
        assertTrue(sim.hiddenFrom(kid, 2000f + 600f + edge, 2000f, reach), "right of the screen")
        assertFalse(sim.hiddenFrom(kid, 2000f + 600f + edge - 1f, 2000f, reach), "just reaching the margin")
        assertTrue(sim.hiddenFrom(kid, 2000f, 2000f - 300f - edge, reach), "above the short side")
        assertFalse(sim.hiddenFrom(kid, 2000f + 600f, 2000f + 300f, reach), "the corner is on screen")
        assertTrue(sim.hiddenFrom(kid, 2000f + 600f + edge, 2000f + 300f + edge, reach), "past the corner")
    }

    @Test fun onlyKidsStillPlayingAreWatching() {
        val sim = emptySim()
        val a = sim.player(1000f, 1000f, EAST, GameConfig.PLAYER_START_LENGTH)
        val b = sim.player(3000f, 3000f, EAST, GameConfig.PLAYER_START_LENGTH)
        sim.ai(2000f, 2000f, EAST, GameConfig.PLAYER_START_LENGTH)
        assertEquals(listOf(a, b), sim.watchers())
        a.alive = false
        assertEquals(listOf(b), sim.watchers())
    }
}
