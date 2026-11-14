package com.quissicutdeus.sniddle.game

import com.quissicutdeus.sniddle.game.model.dist
import com.quissicutdeus.sniddle.game.sim.GameSim
import com.quissicutdeus.sniddle.game.sim.SizeRelation
import com.quissicutdeus.sniddle.game.sim.Spawner
import com.quissicutdeus.sniddle.game.sim.compareSize
import kotlin.math.abs
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
        assertTrue(refilled, "the arena filled back up to ${GameConfig.AI_TARGET_COUNT} snails")
    }

    /**
     * A big kid gets big snails (up to 2.3 times as long). Laid out straight,
     * their bodies used to cross the kid's screen, or poke out through the
     * fence, from a head that was off-screen. Now they arrive coiled.
     */
    @Test fun bigSnailsArriveCoiledUpOutOfSightAndInsideTheFence() {
        val sim = gameWithKid(2150f, seed = 5)
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
     * A snail a kid has had on screen only leaves by being eaten: a giant left
     * over the size cap when the kid shrinks, or one that has become tiny next
     * to a big kid, stays even far out of sight. One no kid has ever seen can
     * still be retired to keep the mix right.
     */
    @Test fun aSnailTheKidHasSeenIsNeverQuietlyRemoved() {
        val sim = emptySim()
        sim.player(500f, 500f, SOUTH, 1500f)
        // Laid out so nobody meets anybody in the 5 seconds below.
        val giant = sim.ai(3400f, 300f, SOUTH, sim.aiMaxLength() * 1.15f, name = "Giant")
        val tiny = sim.ai(600f, 3600f, WEST, 100f, name = "Tiny")
        val unseenGiant = sim.ai(2400f, 300f, SOUTH, sim.aiMaxLength() * 1.15f, name = "Unseen giant")
        giant.seenByKid = true
        tiny.seenByKid = true
        // Far longer than the spawner takes to retire a misfit (it looks every 0.7 s).
        sim.steps(60 * 5)
        assertTrue(giant.alive, "a giant over the cap the kid has seen stays")
        assertTrue(tiny.alive, "a snail the kid has seen stays, however tiny next to the kid")
        assertTrue(!unseenGiant.alive, "a giant no kid has seen is retired")
    }

    /**
     * A big kid in the very middle of the arena sees most of it. Measured
     * against the widest screen any phone could have, nowhere is out of sight
     * and the arena would slowly empty; against the kid's real screen (a
     * landscape phone here) there is still room above and below it.
     */
    @Test fun aBigKidInTheMiddleStillLeavesRoomForNewSnails() {
        for (length in listOf(1500f, 2500f, 4000f)) {
            val sim = emptySim()
            val p = sim.player(sim.world.width / 2f, sim.world.height / 2f, EAST, length)
            val halfH = GameConfig.viewSpanFor(p.radius) / 2f
            sim.setView(p, p.x, p.y, halfH * 2.2f, halfH)
            val spawner = Spawner()
            val made = (1..10).mapNotNull { spawner.spawnOne(sim) }
            assertTrue(made.size >= 8, "a kid $length long in the middle left room for only ${made.size} of 10 snails")
            for (s in made) {
                for (i in 0 until s.segCount) {
                    val onScreen = abs(s.segX[i] - p.x) < halfH * 2.2f && abs(s.segY[i] - p.y) < halfH
                    assertTrue(!onScreen, "${s.name} appeared on the kid's screen")
                }
            }
        }
    }

    /**
     * A new snail's head is in the middle of its coil, so the outside rings a
     * kid bumps into first are its tail end: a bite there snips off a little,
     * not most of the snail.
     */
    @Test fun aNewSnailsHeadIsInTheMiddleOfItsCoil() {
        val sim = gameWithKid(1500f, seed = 9)
        val s =
            sim.world.snails
                .filter { !it.isPlayer }
                .maxBy { it.length }
        val cx = (0 until s.segCount).map { s.segX[it] }.average().toFloat()
        val cy = (0 until s.segCount).map { s.segY[it] }.average().toFloat()
        val outermost = (0 until s.segCount).maxBy { dist(s.segX[it], s.segY[it], cx, cy) }
        assertTrue(dist(s.x, s.y, cx, cy) < s.radius * 4, "the head is near the middle")
        assertTrue(outermost > s.segCount * 3 / 4, "the outer ring is the tail end (outermost circle $outermost of ${s.segCount})")
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
