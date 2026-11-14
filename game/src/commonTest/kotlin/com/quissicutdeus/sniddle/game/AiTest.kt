package com.quissicutdeus.sniddle.game

import com.quissicutdeus.sniddle.game.model.Pellet
import com.quissicutdeus.sniddle.game.model.dist
import com.quissicutdeus.sniddle.game.sim.AiMode
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

    @Test fun aiFleesFromBiggerSnails() {
        val sim = emptySim(aiEnabled = true)
        val small = sim.ai(2000f, 2000f, EAST, 100f, name = "Small")
        val big = sim.ai(2250f, 2000f, NORTH, 800f, name = "Big")
        sim.brains.remove(big) // just drives straight
        sim.steps(30)
        assertEquals(AiMode.FLEE, sim.brains.getValue(small).mode)
        sim.steps(150)
        assertTrue(small.alive)
        assertTrue(dist(small.x, small.y, big.x, big.y) > 250f)
    }

    @Test fun aiChasesSmallerSnails() {
        val sim = emptySim(aiEnabled = true)
        val hunter = sim.ai(2000f, 2000f, NORTH, 600f, name = "Hunter")
        val prey = sim.ai(2300f, 2000f, SOUTH, 150f, name = "Prey")
        sim.brains.remove(prey)
        var chased = false
        repeat(180) {
            sim.step()
            if (sim.brains.getValue(hunter).mode == AiMode.CHASE) chased = true
        }
        assertTrue(chased)
    }

    /** A snail over the size cap (the kid shrank since it grew) doesn't hunt the kid; one under it does. */
    @Test fun aGiantOverTheSizeCapLeavesTheKidAlone() {
        // Whether to chase is a dice roll, so the snail under the cap gets a few games to show it would.
        for ((over, name) in listOf(true to "Giant", false to "Big")) {
            val chased =
                (1L..5L).count { seed ->
                    val sim = emptySim(aiEnabled = true, seed = seed)
                    sim.player(2000f, 2000f, SOUTH, 150f)
                    val hunter = sim.ai(2300f, 2000f, NORTH, sim.aiMaxLength() * (if (over) 1.2f else 0.9f), name = name)
                    var chased = false
                    repeat(180) {
                        sim.step()
                        if (sim.brains[hunter]?.mode == AiMode.CHASE) chased = true
                    }
                    chased
                }
            if (over) assertEquals(0, chased, "$name, over the cap, chased the kid in $chased of 5 games")
            if (!over) assertTrue(chased > 0, "$name, under the cap, never chased the kid")
        }
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
