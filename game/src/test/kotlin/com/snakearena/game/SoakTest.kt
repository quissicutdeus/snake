package com.snakearena.game

import com.snakearena.game.model.Snake
import com.snakearena.game.sim.GameSim
import com.snakearena.game.sim.SimOptions
import com.snakearena.game.sim.SteerInput
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class SoakTest {
    private fun check(sim: GameSim, step: Int) {
        assertEquals(GameConfig.PELLET_COUNT, sim.world.pellets.size)
        for (s in sim.world.snakes) {
            if (!s.alive) continue
            sane(s, step)
            val r = s.radius
            assertTrue(s.x >= r - 0.01f && s.x <= sim.world.width - r + 0.01f, "x out of arena at $step: $s ${s.x}")
            assertTrue(s.y >= r - 0.01f && s.y <= sim.world.height - r + 0.01f, "y out of arena at $step: $s ${s.y}")
        }
        for (p in sim.world.pellets) if (p.x.isNaN() || p.y.isNaN()) fail("NaN pellet at $step")
        for (t in sim.world.tailPieces) if (t.x.isNaN() || t.y.isNaN() || t.value.isNaN()) fail("NaN piece at $step")
    }

    private fun sane(s: Snake, step: Int) {
        if (s.x.isNaN() || s.y.isNaN() || s.heading.isNaN() || s.length.isNaN()) fail("NaN at step $step: $s")
        assertTrue(s.length >= GameConfig.MIN_LENGTH, "too short at $step: $s")
        assertTrue(s.segCount >= 1)
        for (i in 0 until s.segCount) {
            if (s.segX[i].isNaN() || s.segY[i].isNaN() || s.segR[i].isNaN()) fail("NaN segment at $step: $s")
        }
    }

    @Test fun tenThousandStepsOfAiOnlyRunCleanly() {
        val sim = GameSim(seed = 2024, options = SimOptions(spawnPlayer = false))
        var eaten = 0
        repeat(10_000) { i ->
            sim.step()
            eaten += sim.events.count { it is com.snakearena.game.sim.GameEvent.AteSnake }
            sim.events.clear()
            check(sim, i)
        }
        assertTrue(sim.world.snakes.count { it.alive } >= GameConfig.AI_TARGET_COUNT - 2)
        println("AI soak: $eaten snakes eaten, ${sim.world.tailPieces.size} tail pieces lying around, " +
            "biggest ${sim.leaderboard().first().length.toInt()}")
    }

    @Test fun tenThousandStepsWithARandomlySteeringPlayerRunCleanly() {
        val sim = GameSim(seed = 99)
        val input = SteerInput()
        val rng = Random(5)
        var heading = 0f
        repeat(10_000) { i ->
            if (i % 30 == 0) {
                heading += (rng.nextFloat() - 0.5f) * 3f
                if (rng.nextInt(5) == 0) input.release() else input.set(heading)
            }
            sim.update(GameConfig.STEP, input)
            check(sim, i)
        }
        val p = sim.player!!
        println("Player soak: gameOver=${sim.gameOver} hearts=${p.hearts} best=${p.score} eaten=${p.snakesEaten}")
    }
}
