package com.quissicutdeus.sniddle.game

import com.quissicutdeus.sniddle.game.model.Eyes
import com.quissicutdeus.sniddle.game.model.Hat
import com.quissicutdeus.sniddle.game.model.PowerType
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.sim.GameSim
import com.quissicutdeus.sniddle.game.sim.SavedRun
import com.quissicutdeus.sniddle.game.sim.SteerInput
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SavedRunTest {
    private val style = SnailStyle(color1 = 3, color2 = 7, eyes = Eyes.GOOGLY, hat = Hat.CROWN)

    /** A game a while in: the player has grown, been hurt, eaten a snail and has a power running. */
    private fun gameInProgress(): GameSim {
        val sim = GameSim(seed = 5, playerStyle = style, playerName = "Me; and you")
        val p = sim.player!!
        p.grow(900f)
        p.shrinkTo(700f)
        p.hearts = 2
        p.snailsEaten = 4
        p.givePower(PowerType.ARMOR)
        sim.biggestSeconds = 12.5f
        return sim
    }

    @Test fun aSavedGameComesBackWithTheSameSnail() {
        val before = gameInProgress()
        val text = before.snapshot()!!.encode()
        assertTrue(text.length < 200, "saved text is ${text.length} chars")

        val after = GameSim(seed = 99, resume = SavedRun.decode(text))
        val was = before.player!!
        val now = after.player!!
        assertEquals(was.length, now.length)
        assertEquals(was.score, now.score)
        assertEquals(2, now.hearts)
        assertEquals(4, now.snailsEaten)
        assertEquals(style, now.style)
        assertEquals("Me; and you", now.name)
        assertTrue(abs(now.powerLeft(PowerType.ARMOR) - was.powerLeft(PowerType.ARMOR)) < 1e-4f)
        assertTrue(now.invulnerable > 0f, "a moment of protection after coming back")
        assertEquals(12.5f, after.biggestSeconds)
        assertTrue(after.world.snails.count { !it.isPlayer } >= GameConfig.AI_TARGET_COUNT - 2, "the world is filled again")
    }

    @Test fun aFinishedGameHasNothingToSave() {
        val sim = gameInProgress()
        sim.player!!.alive = false
        assertNull(sim.snapshot())
    }

    @Test fun garbageOrOldSavesAreIgnored() {
        assertNull(SavedRun.decode(null))
        assertNull(SavedRun.decode(""))
        assertNull(SavedRun.decode("run0;1;2;3;4;5;6;7"))
        assertNull(SavedRun.decode("run1;NaN;300;2;0;0,0,0;1,2,STRIPES,GOOGLY,NONE;You"))
        assertNull(SavedRun.decode("run1;300;300;0;0;0,0,0;1,2,STRIPES,GOOGLY,NONE;You"))
        assertNotNull(SavedRun.decode(gameInProgress().snapshot()!!.encode()))
    }

    /** A game paused by the previous version still comes back after an update. */
    @Test fun aGameSavedByTheLastVersionStillLoads() {
        val run = assertNotNull(SavedRun.decode("run1;300.0;320.0;2;1;0.0,0.0,0.0;1,2,STRIPES,GOOGLY,NONE;Me; too"))
        assertEquals(300f, run.length)
        assertEquals(2, run.hearts)
        assertEquals("Me; too", run.name)
        assertEquals(0f, run.biggestSeconds)
    }

    @Test fun aVeryLongSnailComesBackInsideTheFenceAndPlaysOn() {
        val run = gameInProgress().snapshot()!!.copy(length = 20_000f, bestLength = 20_000f)
        val sim = GameSim(seed = 1, resume = run)
        val p = sim.player!!
        for (i in 0 until p.segCount) {
            assertTrue(p.segX[i] in 0f..sim.world.width && p.segY[i] in 0f..sim.world.height, "segment $i outside")
        }
        // Then a couple of minutes of wandering about without anything breaking.
        val input = SteerInput()
        val rng = Random(3)
        repeat(7_200) { i ->
            if (i % 60 == 0) input.set(rng.nextFloat() * 6.28f)
            sim.step(input)
            sim.events.clear()
            assertTrue(!p.x.isNaN() && !p.y.isNaN() && p.length >= GameConfig.MIN_LENGTH, "broken at step $i")
        }
    }
}
