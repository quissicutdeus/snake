package com.quissicutdeus.snake.game

import com.quissicutdeus.snake.game.sim.GameEvent
import com.quissicutdeus.snake.game.sim.GameSim
import com.quissicutdeus.snake.game.sim.SimOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BiggestTest {
    private fun GameSim.cheers(): Int = events.count { it is GameEvent.BecameBiggest }.also { events.clear() }

    @Test fun becomingTheBiggestSnakeCheersOnceAndCountsTheTime() {
        val sim = emptySim()
        val kid = sim.player(1000f, 1000f, EAST, 300f)
        // Close by: the spawner quietly removes bigger snakes far away.
        sim.ai(1000f, 1300f, EAST, 400f)
        sim.steps(10)
        assertFalse(sim.playerOnTop)
        assertEquals(0, sim.cheers())

        kid.grow(200f)
        sim.steps(60)
        assertTrue(sim.playerOnTop)
        assertEquals(1, sim.cheers())
        assertTrue(sim.biggestSeconds in 0.9f..1.1f, "one second on top, got ${sim.biggestSeconds}")

        // Trading places right away doesn't cheer again...
        kid.shrinkTo(300f)
        sim.step()
        assertFalse(sim.playerOnTop)
        kid.grow(200f)
        sim.step()
        assertTrue(sim.playerOnTop)
        assertEquals(0, sim.cheers())

        // ...but climbing back up after a while does.
        kid.shrinkTo(300f)
        sim.step()
        // Half a minute later (just the clock: 30 s of real play would run into the fence).
        sim.sinceBiggestCheer = GameConfig.BIGGEST_CHEER_GAP_SECONDS
        kid.grow(200f)
        sim.step()
        assertEquals(1, sim.cheers())
    }

    @Test fun startingOnTopIsNoNews() {
        // Alone in the arena, the player is the biggest from the very start: nothing to cheer about.
        val sim = GameSim(seed = 3, options = SimOptions(aiCount = 0, powerUps = false))
        assertTrue(sim.playerOnTop)
        sim.steps(30)
        assertEquals(0, sim.cheers())
        assertTrue(sim.biggestSeconds > 0f)
    }
}
