package com.quissicutdeus.snake.game

import com.quissicutdeus.snake.game.model.dist
import com.quissicutdeus.snake.game.sim.GameSim
import com.quissicutdeus.snake.game.sim.SizeRelation
import com.quissicutdeus.snake.game.sim.compareSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpawnerTest {
    @Test fun startsWithAMixAroundThePlayersSizeAllOffScreen() {
        val sim = GameSim(seed = 7)
        val p = sim.player!!
        val ais = sim.world.snakes.filter { !it.isPlayer }
        assertEquals(GameConfig.AI_TARGET_COUNT, ais.size)
        val smaller = ais.count { compareSize(it.length, p.length) == SizeRelation.SMALLER }
        val bigger = ais.count { compareSize(it.length, p.length) == SizeRelation.BIGGER }
        assertEquals(ais.size, smaller + bigger, "spawns are clearly smaller or bigger, never 'equal'")
        val fraction = smaller.toFloat() / ais.size
        assertTrue(fraction in 0.5f..0.7f, "about 60% smaller, was $fraction")
        val minD = GameConfig.offscreenDistance(p.radius)
        assertTrue(ais.all { dist(it.x, it.y, p.x, p.y) >= minD }, "all spawn off-screen")
    }

    @Test fun refillsAfterSnakesAreEatenAndStillSpawnsOffScreen() {
        val sim = GameSim(seed = 11)
        val p = sim.player!!
        sim.world.snakes
            .filter { !it.isPlayer }
            .take(4)
            .forEach { it.alive = false }
        var refilled = false
        repeat(600) {
            sim.step()
            if (sim.world.snakes.count { !it.isPlayer && it.alive } == GameConfig.AI_TARGET_COUNT) refilled = true
            if (!p.alive) return@repeat
            val minD = GameConfig.offscreenDistance(p.radius) - 20f
            sim.world.snakes.filter { !it.isPlayer && it.age <= GameConfig.STEP * 1.5f }.forEach {
                assertTrue(dist(it.x, it.y, p.x, p.y) >= minD, "new snake ${it.name} spawned on-screen")
            }
        }
        // (The count itself moves around: the spawner also retires far-away snakes that
        // no longer fit the size mix, e.g. after the player loses its tail.)
        assertTrue(refilled, "the arena filled back up to ${GameConfig.AI_TARGET_COUNT} snakes")
    }

    @Test fun aiNamesAreSillyAndUnique() {
        val sim = GameSim(seed = 3)
        val names =
            sim.world.snakes
                .filter { !it.isPlayer }
                .map { it.name }
        assertEquals(names.size, names.toSet().size)
        assertTrue(names.all { it.isNotBlank() })
    }
}
