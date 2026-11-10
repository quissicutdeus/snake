package com.quissicutdeus.snake.game

import com.quissicutdeus.snake.game.model.PowerType
import com.quissicutdeus.snake.game.model.PowerUp
import com.quissicutdeus.snake.game.render.Mood
import com.quissicutdeus.snake.game.render.SceneRenderer
import com.quissicutdeus.snake.game.sim.AiMode
import com.quissicutdeus.snake.game.sim.GameEvent
import com.quissicutdeus.snake.game.sim.GameSim
import com.quissicutdeus.snake.game.sim.SimOptions
import com.quissicutdeus.snake.game.sim.updatePowerUps
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PowerUpTest {
    @Test fun touchingAPowerUpGivesThePowerAndRemovesIt() {
        val sim = emptySim()
        val kid = sim.player(1000f, 1000f, EAST, 150f)
        sim.world.powerUps.add(PowerUp(PowerType.SPEED, 1060f, 1000f))
        sim.stepUntil { sim.world.powerUps.isEmpty() }
        assertTrue(kid.hasPower(PowerType.SPEED))
        assertEquals(PowerType.SPEED.seconds, kid.powerLeft(PowerType.SPEED), 0.1f)
        assertTrue(sim.events.any { it is GameEvent.PowerUpTaken && it.type == PowerType.SPEED })
    }

    @Test fun onlyThePlayerCanPickThemUp() {
        val sim = emptySim()
        sim.player(200f, 200f, EAST, 150f)
        val bot = sim.ai(1000f, 1000f, EAST, 150f)
        sim.world.powerUps.add(PowerUp(PowerType.STRENGTH, 1060f, 1000f))
        sim.steps(60)
        assertEquals(1, sim.world.powerUps.size)
        assertFalse(bot.hasPower(PowerType.STRENGTH))
    }

    @Test fun powersWearOff() {
        val sim = emptySim()
        val kid = sim.player(1000f, 1000f, EAST, 150f)
        kid.givePower(PowerType.ARMOR)
        sim.steps((PowerType.ARMOR.seconds * 60).toInt() - 30)
        assertTrue(kid.hasPower(PowerType.ARMOR))
        sim.steps(40)
        assertFalse(kid.hasPower(PowerType.ARMOR))
    }

    @Test fun superSpeedMakesYouFasterAndTurnQuicker() {
        val sim = emptySim()
        val kid = sim.player(1000f, 1000f, EAST, 300f)
        val normal = kid.speed
        kid.givePower(PowerType.SPEED)
        assertEquals(normal * GameConfig.SPEED_POWER_FACTOR, kid.speed, 0.01f)
    }

    @Test fun superStrengthEatsABiggerSnakeHeadOn() {
        val sim = emptySim()
        val kid = sim.player(1000f, 1000f, EAST, 150f)
        kid.givePower(PowerType.STRENGTH)
        val big = sim.ai(1100f, 1000f, WEST, 600f)
        sim.stepUntil { !big.alive }
        assertEquals(150f + 600f * GameConfig.EAT_WHOLE_GAIN, kid.length, 0.5f)
        assertEquals(1, kid.snakesEaten)
        assertEquals(GameConfig.PLAYER_HEARTS, kid.hearts)
    }

    @Test fun superStrengthSnipsABiggerSnakesTailInsteadOfGettingHurt() {
        val sim = emptySim()
        val big = sim.ai(2300f, 2000f, EAST, 900f)
        val kid = sim.player(2000f, 2100f, NORTH, 150f)
        kid.givePower(PowerType.STRENGTH)
        sim.stepUntil { sim.world.tailPieces.isNotEmpty() }
        assertTrue(big.length < 900f)
        assertEquals(GameConfig.PLAYER_HEARTS, kid.hearts)
    }

    @Test fun superArmorStopsBitesAndBumps() {
        // A bigger head hitting your body: no tail lost, the attacker bounces off.
        val sim = emptySim()
        val kid = sim.player(2000f, 2000f, EAST, 300f)
        kid.givePower(PowerType.ARMOR)
        val big = sim.ai(1850f, 2150f, NORTH, 900f)
        sim.stepUntil { sim.events.any { it is GameEvent.Blocked } }
        sim.steps(30)
        assertEquals(300f, kid.length)
        assertTrue(sim.world.tailPieces.isEmpty())
        assertTrue(sin(big.heading) > 0f, "the attacker bounced back down")

        // Running into a bigger snake (head-on): you bounce but keep all your hearts.
        val sim2 = emptySim()
        val kid2 = sim2.player(1000f, 1000f, EAST, 150f)
        kid2.givePower(PowerType.ARMOR)
        sim2.ai(1100f, 1000f, WEST, 600f)
        sim2.stepUntil { sim2.events.any { it is GameEvent.Blocked } }
        assertEquals(GameConfig.PLAYER_HEARTS, kid2.hearts)
        assertTrue(kid2.alive)
        assertFalse(sim2.gameOver)
    }

    @Test fun snakesAreScaredOfSuperStrengthAndRunAway() {
        val sim = emptySim(aiEnabled = true)
        val kid = sim.player(2000f, 2000f, EAST, 150f)
        val big = sim.ai(2200f, 2000f, NORTH, 600f)
        assertEquals(Mood.ANGRY, SceneRenderer.moodFor(big, kid))
        kid.givePower(PowerType.STRENGTH)
        assertEquals(Mood.SCARED, SceneRenderer.moodFor(big, kid))
        sim.steps(20)
        assertEquals(AiMode.FLEE, big.brain!!.mode)
    }

    @Test fun powerUpsAppearOverTimeUpToTheMaximumAndVanishLater() {
        val sim = GameSim(seed = 4, options = SimOptions(aiCount = 0))
        assertEquals(GameConfig.POWER_UP_START, sim.world.powerUps.size)
        // Long enough for several to appear; none should be picked up by the idle player at the centre.
        sim.world.powerUps.clear()
        repeat(((GameConfig.POWER_UP_INTERVAL * (GameConfig.POWER_UP_MAX + 2)) * 60).toInt()) { sim.updatePowerUps(GameConfig.STEP) }
        assertTrue(sim.world.powerUps.size in 1..GameConfig.POWER_UP_MAX, "had ${sim.world.powerUps.size}")
        val m = GameConfig.POWER_UP_WALL_MARGIN
        assertTrue(sim.world.powerUps.all { it.x in m..(sim.world.width - m) && it.y in m..(sim.world.height - m) })
        repeat(((GameConfig.POWER_UP_LIFETIME + 1f) * 60).toInt()) { sim.updatePowerUps(GameConfig.STEP) }
        assertTrue(sim.world.powerUps.all { it.age <= GameConfig.POWER_UP_LIFETIME })
    }
}
