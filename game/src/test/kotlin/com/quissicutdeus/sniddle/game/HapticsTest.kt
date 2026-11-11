package com.quissicutdeus.sniddle.game

import com.quissicutdeus.sniddle.game.model.PowerType
import com.quissicutdeus.sniddle.game.render.Buzz
import com.quissicutdeus.sniddle.game.render.Haptics
import com.quissicutdeus.sniddle.game.sim.GameEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HapticsTest {
    private val frame = 1f / 60f

    @Test fun onlyThePlayersOwnMomentsBuzz() {
        val h = Haptics()
        val others =
            listOf(
                GameEvent.AteSnail(eaterId = OTHER, x = 0f, y = 0f, color = 0),
                GameEvent.Hurt(OTHER, 0f, 0f),
                GameEvent.Bounced(OTHER, 0f, 0f),
                GameEvent.TailCut(attackerId = OTHER, victimId = 9, x = 0f, y = 0f, color = 0),
                GameEvent.AteFood(0f, 0f, 0),
            )
        assertNull(h.onEvents(others, ME, frame))
        assertEquals(Buzz.CHEER, h.onEvents(listOf(GameEvent.AteSnail(ME, 0f, 0f, 0)), ME, frame))
    }

    @Test fun theStrongestEventInAFrameWins() {
        val h = Haptics()
        val events =
            listOf(
                GameEvent.Bounced(ME, 0f, 0f),
                GameEvent.PlayerEaten(0f, 0f),
                GameEvent.PowerUpTaken(PowerType.SPEED, 0f, 0f),
            )
        assertEquals(Buzz.GOBBLED, h.onEvents(events, ME, frame))
    }

    @Test fun losingYourTailHurtsCuttingOneThuds() {
        val cut = { attacker: Int, victim: Int -> listOf(GameEvent.TailCut(attacker, victim, 0f, 0f, 0)) }
        assertEquals(Buzz.OUCH, Haptics().onEvents(cut(OTHER, ME), ME, frame))
        assertEquals(Buzz.THUD, Haptics().onEvents(cut(ME, OTHER), ME, frame))
    }

    @Test fun aRepeatWaitsForTheCooldownButAStrongerBuzzDoesNot() {
        val h = Haptics()
        val bounce = listOf(GameEvent.Bounced(ME, 0f, 0f))
        assertEquals(Buzz.THUD, h.onEvents(bounce, ME, frame))
        assertNull(h.onEvents(bounce, ME, frame))
        assertEquals(Buzz.OUCH, h.onEvents(listOf(GameEvent.Hurt(ME, 0f, 0f)), ME, frame))
        assertNull(h.onEvents(bounce, ME, frame))
        assertNull(h.onEvents(emptyList(), ME, Haptics.COOLDOWN_SECONDS))
        assertEquals(Buzz.THUD, h.onEvents(bounce, ME, frame))
    }

    @Test fun leaningOnTheFenceBuzzesOnceEvenOnA120HzScreen() {
        val h = Haptics()
        val wall = listOf(GameEvent.HitWall(ME, 0f, 0f))
        val half = frame / 2f
        assertEquals(Buzz.TAP, h.onEvents(wall, ME, half))
        // Two seconds against the fence; every other 120 Hz frame runs no step.
        repeat(240) { i ->
            assertNull(h.onEvents(if (i % 2 == 0) emptyList() else wall, ME, half), "frame $i")
        }
        // Away from it for a while, then back: buzzes again.
        repeat(60) { assertNull(h.onEvents(emptyList(), ME, half)) }
        assertEquals(Buzz.TAP, h.onEvents(wall, ME, half))
    }

    private companion object {
        const val ME = 1
        const val OTHER = 2
    }
}
