package com.quissicutdeus.sniddle.game

import com.quissicutdeus.sniddle.game.model.Pellet
import com.quissicutdeus.sniddle.game.model.angleDiff
import com.quissicutdeus.sniddle.game.sim.SteerInput
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MovementTest {
    @Test fun headMovesForwardAtItsSpeed() {
        val sim = emptySim()
        val p = sim.player(1000f, 1000f, EAST, 150f)
        val speed = p.speed
        sim.steps(60)
        assertEquals(1000f + speed, p.x, 1f)
        assertEquals(1000f, p.y, 0.01f)
    }

    @Test fun turningIsRateLimited() {
        val sim = emptySim()
        val p = sim.player(1000f, 1000f, EAST, 150f)
        val input = SteerInput().apply { set(WEST) }
        sim.step(input)
        val maxTurn = GameConfig.TURN_RATE * GameConfig.STEP
        assertEquals(maxTurn, abs(angleDiff(EAST, p.heading)), 1e-4f)
        // Eventually it does face the finger.
        repeat(120) { sim.step(input) }
        assertEquals(0f, abs(angleDiff(p.heading, WEST)), 1e-3f)
    }

    @Test fun bodyFollowsTheHeadAndKeepsItsLength() {
        val sim = emptySim()
        val p = sim.player(1500f, 1500f, EAST, 300f)
        val input = SteerInput().apply { set(SOUTH) }
        repeat(200) { sim.step(input) }
        assertEquals(300f, p.visibleLength(), 4f)
        // The last body circle lies (roughly) on the path the head travelled, length behind it.
        assertTrue(p.segCount > 10)
        assertEquals(0f, p.segD[0])
        assertTrue(p.segD[p.segCount - 1] <= p.length)
    }

    @Test fun wallsBounceGentlyWithoutDamage() {
        val sim = emptySim()
        val p = sim.player(3900f, 2000f, EAST, 150f)
        val input = SteerInput().apply { set(EAST) } // keep pushing into the wall
        repeat(240) { sim.step(input) }
        assertTrue(p.x <= sim.world.width - p.radius + 0.01f, "stays inside: ${p.x}")
        assertEquals(GameConfig.PLAYER_HEARTS, p.hearts)
        assertTrue(p.alive)
    }

    @Test fun eatingFoodGrowsYouAndFoodIsReplaced() {
        val sim = emptySim(pellets = 0)
        val p = sim.player(1000f, 1000f, EAST, 150f)
        sim.world.pellets.add(Pellet(1060f, 1000f, 10f, 6f, 0, 0f))
        sim.steps(30)
        assertEquals(160f, p.length, 0.01f)
        assertEquals(1, sim.world.pellets.size, "eaten pellets respawn elsewhere")
        assertEquals(32, p.score)
    }

    @Test fun drawingBlendsBetweenStepsForSmoothHighRefreshScreens() {
        val sim = emptySim()
        val p = sim.player(1000f, 1000f, EAST, 150f)
        sim.step()
        val beforeX = p.segX[3]
        sim.step()
        val afterX = p.segX[3]
        p.prepareDraw(0f)
        assertEquals(beforeX, p.drawX[3], 1e-3f)
        p.prepareDraw(1f)
        assertEquals(afterX, p.drawX[3], 1e-3f)
        p.prepareDraw(0.5f)
        assertEquals((beforeX + afterX) / 2f, p.drawX[3], 1e-3f)

        // Real time between steps sets the blend: a 90 Hz frame is 2/3 of a 60 Hz step.
        val sim2 = emptySim()
        sim2.player(1000f, 1000f, EAST, 150f)
        sim2.update(1f / 90f, SteerInput())
        assertEquals(0, sim2.update(0f, SteerInput()))
        assertEquals(60f / 90f, sim2.renderAlpha, 1e-3f)
    }

    @Test fun visibleBodyCatchesUpAfterGrowing() {
        val sim = emptySim()
        val p = sim.player(1000f, 1000f, EAST, 150f)
        p.grow(150f)
        assertEquals(150f, p.visibleLength(), 4f) // tail waits...
        sim.steps(120)
        assertEquals(300f, p.visibleLength(), 4f) // ...until the head has moved far enough
    }
}
