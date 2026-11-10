package com.snakearena.game

import com.snakearena.game.sim.Contact
import com.snakearena.game.sim.GameEvent
import com.snakearena.game.sim.SizeRelation
import com.snakearena.game.sim.SteerInput
import com.snakearena.game.sim.compareSize
import com.snakearena.game.sim.resolveContact
import com.snakearena.game.sim.sever
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RulesTest {

    @Test fun sizesWithinTenPercentCountAsEqual() {
        assertEquals(SizeRelation.EQUAL, compareSize(100f, 109f))
        assertEquals(SizeRelation.EQUAL, compareSize(109f, 100f))
        assertEquals(SizeRelation.SMALLER, compareSize(100f, 111f))
        assertEquals(SizeRelation.BIGGER, compareSize(111f, 100f))
    }

    @Test fun headHitsSmallerBodyAtSegmentKThenVictimKeepsKAndTheRestBecomesTailPieces() {
        val sim = emptySim()
        val victim = sim.ai(2000f, 2000f, EAST, 300f)
        val attacker = sim.player(1900f, 2100f, NORTH, 600f)
        val k = 12
        val cutAt = victim.segD[k]
        sim.resolveContact(Contact(attacker, victim, k))

        assertEquals(cutAt, victim.length, 0.01f, "front part up to segment k survives")
        assertTrue(victim.alive)
        val pieces = sim.world.tailPieces
        assertTrue(pieces.isNotEmpty())
        assertEquals(300f - cutAt, pieces.sumOf { it.value.toDouble() }.toFloat(), 0.01f, "the rest becomes tail pieces")
        // Pieces lie where the old body was (behind the cut, on the old line).
        assertTrue(pieces.all { it.x < 2000f - cutAt + 1f && kotlin.math.abs(it.y - 2000f) < 0.01f })
        assertEquals(600f, attacker.length, "cutting doesn't feed you; eating the pieces does")
        assertTrue(sim.events.any { it is GameEvent.TailCut })
    }

    @Test fun biggerHeadDrivingIntoASmallerBodyCutsIt() {
        val sim = emptySim()
        val victim = sim.ai(2000f, 2000f, EAST, 300f)
        val attacker = sim.player(1850f, 2100f, NORTH, 600f)
        sim.stepUntil { sim.world.tailPieces.isNotEmpty() }
        assertTrue(victim.length < 300f && victim.length >= GameConfig.MIN_LENGTH)
        val total = victim.length + sim.world.tailPieces.sumOf { it.value.toDouble() }.toFloat()
        assertEquals(300f, total, 0.5f, "no length is lost: it all lies on the ground")
        assertTrue(attacker.alive)
        assertEquals(GameConfig.PLAYER_HEARTS, attacker.hearts)
    }

    @Test fun cuttingNeverLeavesASnakeBelowMinimumLength() {
        val sim = emptySim()
        val victim = sim.ai(2000f, 2000f, EAST, 300f)
        val attacker = sim.player(2000f, 1900f, SOUTH, 900f)
        sim.sever(victim, 1, attacker)
        assertEquals(GameConfig.MIN_LENGTH, victim.length)
    }

    @Test fun losingYourTailMakesYouFaster() {
        val sim = emptySim()
        val victim = sim.ai(2000f, 2000f, EAST, 600f)
        val attacker = sim.player(1500f, 1900f, SOUTH, 1500f)
        val before = victim.speed
        sim.sever(victim, 20, attacker)
        assertTrue(victim.speed > before)
    }

    @Test fun anyoneCanEatDroppedTailPieces() {
        val sim = emptySim()
        val victim = sim.ai(2000f, 2000f, EAST, 400f)
        val attacker = sim.player(1900f, 2300f, NORTH, 1200f)
        sim.sever(victim, 4, attacker)
        val dropped = sim.world.tailPieces.sumOf { it.value.toDouble() }.toFloat()
        assertTrue(dropped > 0f)
        victim.alive = false // out of the way
        attacker.alive = false
        val scavenger = sim.ai(1500f, 2000f, EAST, 100f, name = "Scavenger")
        sim.stepUntil { sim.world.tailPieces.isEmpty() }
        assertEquals(100f + dropped, scavenger.length, 0.01f)
    }

    @Test fun headToHeadTheBiggerSnakeEatsTheSmallerOneWhole() {
        val sim = emptySim()
        val big = sim.player(1000f, 1000f, EAST, 600f)
        val small = sim.ai(1100f, 1000f, WEST, 200f)
        sim.stepUntil { !small.alive }
        assertEquals(600f + 200f * GameConfig.EAT_WHOLE_GAIN, big.length, 0.01f)
        assertEquals(1, big.snakesEaten)
        assertFalse(sim.world.snakes.contains(small))
        assertTrue(sim.events.any { it is GameEvent.AteSnake })
    }

    @Test fun hittingABiggerSnakeBouncesYouLosesAHeartAndMakesYouBlink() {
        val sim = emptySim()
        val big = sim.ai(2300f, 2000f, EAST, 600f)
        val kid = sim.player(2000f, 2100f, NORTH, 150f)
        val input = SteerInput().apply { set(NORTH) }
        sim.stepUntil { kid.hearts < GameConfig.PLAYER_HEARTS || run { sim.step(input); false } }
        assertEquals(GameConfig.PLAYER_HEARTS - 1, kid.hearts)
        assertTrue(kid.alive)
        assertTrue(kid.invulnerable > GameConfig.HURT_INVULNERABLE_SECONDS - 0.05f)
        assertTrue(sin(kid.heading) > 0.5f, "bounced back (now heading south-ish): ${kid.heading}")
        assertEquals(600f, big.length, "nothing happens to the bigger snake")

        // While blinking you can't lose another heart, even pushing straight back in.
        repeat(60) { sim.step(input) }
        assertEquals(GameConfig.PLAYER_HEARTS - 1, kid.hearts)
    }

    @Test fun aBiggerHeadHittingYourBodyKnocksYourTailOffButKeepsYourHearts() {
        val sim = emptySim()
        val kid = sim.player(2000f, 2000f, EAST, 300f)
        val big = sim.ai(1850f, 2150f, NORTH, 900f)
        sim.stepUntil { sim.world.tailPieces.isNotEmpty() }
        assertTrue(kid.length < 300f)
        assertEquals(GameConfig.PLAYER_HEARTS, kid.hearts)
        assertTrue(kid.alive)
        assertEquals(900f, big.length)
        assertEquals(300f, kid.bestLength, "score remembers the longest length")
    }

    @Test fun aBiggerHeadHittingYourHeadCostsAHeartAndBouncesYou() {
        val sim = emptySim()
        val kid = sim.player(1000f, 1000f, EAST, 150f)
        val big = sim.ai(1100f, 1000f, WEST, 600f)
        sim.stepUntil { kid.hearts < GameConfig.PLAYER_HEARTS }
        assertTrue(kid.alive)
        assertEquals(600f, big.length)
        assertTrue(cos(kid.heading) < 0f, "bounced backwards")
        assertFalse(sim.gameOver)
    }

    @Test fun atZeroHeartsYouAreEatenAndTheGameIsOver() {
        val sim = emptySim()
        val kid = sim.player(1000f, 1000f, EAST, 150f)
        kid.hearts = 1
        val big = sim.ai(1100f, 1000f, WEST, 600f)
        sim.stepUntil { sim.gameOver }
        assertFalse(kid.alive)
        assertEquals(0, kid.hearts)
        assertEquals(600f + 150f * GameConfig.EAT_WHOLE_GAIN, big.length, 0.01f)
        assertTrue(sim.events.any { it is GameEvent.PlayerEaten })
    }

    @Test fun sameSizeSnakesBothBounceWithoutDamage() {
        val sim = emptySim()
        val kid = sim.player(1000f, 1000f, EAST, 150f)
        val other = sim.ai(1080f, 1000f, WEST, 160f)
        sim.stepUntil { sim.events.any { it is GameEvent.Bounced } }
        assertTrue(kid.alive && other.alive)
        assertEquals(GameConfig.PLAYER_HEARTS, kid.hearts)
        assertEquals(150f, kid.length)
        assertEquals(160f, other.length)
        assertTrue(cos(kid.heading) < 0f, "kid bounced back")
        assertTrue(cos(other.heading) > 0f, "other bounced back")
        assertTrue(sim.world.tailPieces.isEmpty())
        assertEquals(2, sim.events.count { it is GameEvent.Bounced })
    }

    @Test fun aiSnakesFollowTheSameRulesAgainstEachOther() {
        val sim = emptySim()
        val big = sim.ai(1000f, 1000f, EAST, 500f, name = "Big")
        val small = sim.ai(1100f, 1000f, WEST, 120f, name = "Small")
        sim.stepUntil { !small.alive }
        assertEquals(500f + 60f, big.length, 0.01f)

        // AI head into a bigger AI's body: it has one heart, so it gets eaten.
        val wall = sim.ai(2300f, 2000f, EAST, 800f, name = "Wall")
        val bumper = sim.ai(2000f, 2100f, NORTH, 200f, name = "Bumper")
        sim.stepUntil { !bumper.alive }
        assertEquals(800f + 100f, wall.length, 0.01f)
    }

    @Test fun protectedSnakesCannotBeCutAgainRightAway() {
        val sim = emptySim()
        val victim = sim.ai(2000f, 2000f, EAST, 600f)
        val attacker = sim.player(1900f, 2100f, NORTH, 1500f)
        sim.resolveContact(Contact(attacker, victim, 30))
        val afterFirst = victim.length
        sim.resolveContact(Contact(attacker, victim, 10))
        assertEquals(afterFirst, victim.length)
    }
}
