package com.quissicutdeus.sniddle.game

import com.quissicutdeus.sniddle.game.model.PowerType
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.model.dist
import com.quissicutdeus.sniddle.game.sim.GameSim
import com.quissicutdeus.sniddle.game.sim.SavedRun
import com.quissicutdeus.sniddle.game.sim.SizeRelation
import com.quissicutdeus.sniddle.game.sim.compareSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpawnerTest {
    @Test fun startsWithAMixAroundThePlayersSizeAllOffScreen() {
        val sim = GameSim(seed = 7)
        val p = sim.player!!
        val ais = sim.world.snails.filter { !it.isPlayer }
        assertEquals(GameConfig.AI_TARGET_COUNT, ais.size)
        val smaller = ais.count { compareSize(it.length, p.length) == SizeRelation.SMALLER }
        val bigger = ais.count { compareSize(it.length, p.length) == SizeRelation.BIGGER }
        assertEquals(ais.size, smaller + bigger, "spawns are clearly smaller or bigger, never 'equal'")
        val fraction = smaller.toFloat() / ais.size
        assertTrue(fraction in 0.5f..0.7f, "about 60% smaller, was $fraction")
        val minD = GameConfig.offscreenDistance(p.radius)
        assertTrue(ais.all { dist(it.x, it.y, p.x, p.y) >= minD }, "all spawn off-screen")
    }

    @Test fun refillsAfterSnailsAreEatenAndStillSpawnsOffScreen() {
        val sim = GameSim(seed = 11)
        val p = sim.player!!
        sim.world.snails
            .filter { !it.isPlayer }
            .take(4)
            .forEach { it.alive = false }
        var refilled = false
        repeat(600) {
            sim.step()
            if (sim.world.snails.count { !it.isPlayer && it.alive } == GameConfig.AI_TARGET_COUNT) refilled = true
            if (!p.alive) return@repeat
            val minD = GameConfig.offscreenDistance(p.radius) - 20f
            sim.world.snails.filter { !it.isPlayer && it.age <= GameConfig.STEP * 1.5f }.forEach {
                assertTrue(dist(it.x, it.y, p.x, p.y) >= minD, "new snail ${it.name} spawned on-screen")
            }
        }
        // (The count itself moves around: the spawner also retires far-away snails that
        // no longer fit the size mix, e.g. after the player loses its tail.)
        assertTrue(refilled, "the arena filled back up to ${GameConfig.AI_TARGET_COUNT} snails")
    }

    /**
     * A big kid gets big snails (up to 2.3 times as long). Laid out straight,
     * their bodies used to cross the kid's screen, or poke out through the
     * fence, from a head that was off-screen. Now they arrive coiled.
     */
    @Test fun bigSnailsArriveCoiledUpOutOfSightAndInsideTheFence() {
        val run = SavedRun("Kid", SnailStyle.DEFAULT, 2150f, 2150f, 3, 0, List(PowerType.entries.size) { 0f })
        val sim = GameSim(seed = 5, resume = run)
        val p = sim.player!!
        var checked = 0

        fun checkNew(age: Float) {
            for (s in sim.world.snails) {
                if (s.isPlayer || !s.alive || s.age > age) continue
                checked++
                val seen = GameConfig.offscreenDistance(p.radius)
                for (i in 0 until s.segCount) {
                    val d = dist(s.segX[i], s.segY[i], p.x, p.y)
                    assertTrue(d + s.segR[i] >= seen, "${s.name}'s body starts on the kid's screen (circle $i, $d from the kid)")
                    assertTrue(
                        s.segX[i] in 0f..sim.world.width && s.segY[i] in 0f..sim.world.height,
                        "${s.name}'s body starts outside the fence (circle $i at ${s.segX[i]}, ${s.segY[i]})",
                    )
                }
            }
        }
        checkNew(age = 0f) // the snails the game starts with
        repeat(60 * 60) {
            sim.step()
            if (p.alive) checkNew(age = GameConfig.STEP * 1.5f)
        }
        assertTrue(checked > GameConfig.AI_TARGET_COUNT, "only $checked new snails were checked")
    }

    /**
     * When the kid shrinks (their tail bitten off), snails can be left over the
     * size cap. Once out of sight they go, even if not quite big enough to be
     * a misfit in the size mix.
     */
    @Test fun giantsLeftOverTheSizeCapAreRetiredOnceOutOfSight() {
        val sim = emptySim()
        sim.player(500f, 500f, EAST, 150f)
        val cap = sim.aiMaxLength()
        val giant = sim.ai(3400f, 3400f, NORTH, cap * 1.15f, name = "Giant")
        val big = sim.ai(3400f, 1200f, NORTH, cap * 0.9f, name = "Big")
        val nearGiant = sim.ai(500f, 800f, EAST, cap * 1.15f, name = "Near")
        // Enough smaller snails that the size mix is fine, so only the cap can retire anyone.
        repeat(4) { sim.ai(3600f, 600f + it * 700f, NORTH, 100f, name = "Small $it") }
        sim.steps(60)
        assertTrue(!giant.alive, "a giant over the cap, out of sight, is retired")
        assertTrue(big.alive, "a big snail under the cap stays")
        assertTrue(nearGiant.alive, "a giant the kid can see doesn't vanish in front of them")
    }

    @Test fun aiNamesAreSillyAndUnique() {
        val sim = GameSim(seed = 3)
        val names =
            sim.world.snails
                .filter { !it.isPlayer }
                .map { it.name }
        assertEquals(names.size, names.toSet().size)
        assertTrue(names.all { it.isNotBlank() })
    }
}
