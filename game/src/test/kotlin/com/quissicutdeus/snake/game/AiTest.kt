package com.quissicutdeus.snake.game

import com.quissicutdeus.snake.game.model.Pellet
import com.quissicutdeus.snake.game.model.dist
import com.quissicutdeus.snake.game.sim.AiMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiTest {
    @Test fun aiSeeksNearbyFood() {
        val sim = emptySim(aiEnabled = true)
        val s = sim.ai(2000f, 2000f, EAST, 150f)
        sim.world.pellets.add(Pellet(2000f, 2160f, 10f, 6f, 0, 0f))
        sim.stepUntil(max = 180) { s.length > 150f }
        assertEquals(160f, s.length)
    }

    @Test fun aiFleesFromBiggerSnakes() {
        val sim = emptySim(aiEnabled = true)
        val small = sim.ai(2000f, 2000f, EAST, 100f, name = "Small")
        val big = sim.ai(2250f, 2000f, NORTH, 800f, name = "Big")
        big.brain = null // just drives straight
        sim.steps(30)
        assertEquals(AiMode.FLEE, small.brain!!.mode)
        sim.steps(150)
        assertTrue(small.alive)
        assertTrue(dist(small.x, small.y, big.x, big.y) > 250f)
    }

    @Test fun aiChasesSmallerSnakes() {
        val sim = emptySim(aiEnabled = true)
        val hunter = sim.ai(2000f, 2000f, NORTH, 600f, name = "Hunter")
        val prey = sim.ai(2300f, 2000f, SOUTH, 150f, name = "Prey")
        prey.brain = null
        var chased = false
        repeat(180) {
            sim.step()
            if (hunter.brain!!.mode == AiMode.CHASE) chased = true
        }
        assertTrue(chased)
    }

    @Test fun aiStaysInsideTheArena() {
        val sim = emptySim(aiEnabled = true)
        val s = sim.ai(3950f, 3950f, EAST, 300f)
        repeat(600) {
            sim.step()
            assertTrue(s.x in 0f..sim.world.width && s.y in 0f..sim.world.height)
        }
        assertTrue(dist(s.x, s.y, 3950f, 3950f) > 100f, "it got away from the corner")
    }
}
