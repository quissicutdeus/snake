package com.quissicutdeus.snake.game

import com.quissicutdeus.snake.game.audio.Music
import com.quissicutdeus.snake.game.audio.Sound
import com.quissicutdeus.snake.game.audio.SoundCues
import com.quissicutdeus.snake.game.audio.Synth
import com.quissicutdeus.snake.game.model.PowerType
import com.quissicutdeus.snake.game.sim.GameEvent
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SoundTest {
    private val frame = 1f / 60f

    @Test fun everySoundIsShortCleanAndNotClipped() {
        for (s in Sound.entries) {
            val pcm = Synth.render(s)
            val seconds = pcm.size.toFloat() / Synth.SAMPLE_RATE
            assertTrue(seconds in 0.03f..1.2f, "$s lasts $seconds s")
            val peak = pcm.maxOf { abs(it.toInt()) }
            assertTrue(peak in 3000..(Short.MAX_VALUE * 0.6f).toInt(), "$s peak $peak")
            // Starts and ends at (near) silence: no click.
            assertTrue(abs(pcm.first().toInt()) < 200 && abs(pcm.last().toInt()) < 200, "$s clicks")
        }
    }

    @Test fun theMusicLoopsCleanlyAndStaysUnderTheEffects() {
        val pcm = Music.render()
        assertEquals((Music.LOOP_SECONDS * Synth.SAMPLE_RATE).toInt(), pcm.size, "exactly one loop long")
        // Starts from zero (fading in) and ends in silence, so going round the loop doesn't click.
        assertTrue(abs(pcm.first().toInt()) < 50, "start ${pcm.first()}")
        assertTrue(pcm.takeLast(200).all { abs(it.toInt()) < 50 }, "the end isn't silent")
        val peak = pcm.maxOf { abs(it.toInt()) }
        val pop = Synth.render(Sound.PELLET_MID).maxOf { abs(it.toInt()) }
        assertTrue(peak in 1000..pop, "music peak $peak, food pop $pop")
    }

    @Test fun soundsComeOutTheSameEveryTime() {
        for (s in Sound.entries) assertContentEquals(Synth.render(s), Synth.render(s), "$s")
    }

    @Test fun foodPopsAreQuieterThanTheBigMoments() {
        val peak = { s: Sound -> Synth.render(s).maxOf { abs(it.toInt()) } }
        assertTrue(peak(Sound.PELLET_MID) < peak(Sound.CHOMP) / 2)
    }

    @Test fun onlyFoodAtThePlayersHeadPops() {
        val sim = emptySim()
        val kid = sim.player(1000f, 1000f, EAST, 40f)
        val cues = SoundCues()
        assertEquals(emptyList(), cues.onEvents(listOf(GameEvent.AteFood(1600f, 1000f, 0)), kid, frame))
        assertEquals(listOf(Sound.PELLET_LOW), cues.onEvents(listOf(GameEvent.AteFood(1010f, 1000f, 0)), kid, frame))
    }

    @Test fun aQuickRunOfFoodRisesInPitch() {
        val sim = emptySim()
        val kid = sim.player(1000f, 1000f, EAST, 40f)
        val cues = SoundCues()
        val food = listOf(GameEvent.AteFood(kid.x, kid.y, 0))
        val heard = (1..12).flatMap { cues.onEvents(food, kid, 0.1f) }
        assertEquals(Sound.PELLET_LOW, heard.first())
        assertEquals(Sound.PELLET_HIGH, heard.last())
        // After a pause the run starts low again.
        cues.onEvents(emptyList(), kid, 1f)
        assertEquals(listOf(Sound.PELLET_LOW), cues.onEvents(food, kid, 0.1f))
    }

    @Test fun theMostImportantBigSoundWinsAndPlaysWithAPop() {
        val sim = emptySim()
        val kid = sim.player(1000f, 1000f, EAST, 40f)
        val events =
            listOf(
                GameEvent.Bounced(kid.id, 0f, 0f),
                GameEvent.PowerUpTaken(PowerType.SPEED, 0f, 0f),
                GameEvent.AteSnake(kid.id, 0f, 0f, 0),
                GameEvent.AteFood(kid.x, kid.y, 0),
            )
        assertEquals(listOf(Sound.CHOMP, Sound.PELLET_LOW), SoundCues().onEvents(events, kid, frame))
    }

    @Test fun otherSnakesMomentsAreSilentAndRepeatsWait() {
        val sim = emptySim()
        val kid = sim.player(1000f, 1000f, EAST, 40f)
        val cues = SoundCues()
        assertEquals(emptyList(), cues.onEvents(listOf(GameEvent.Bounced(kid.id + 1, 0f, 0f)), kid, frame))
        val boing = listOf(GameEvent.Bounced(kid.id, 0f, 0f))
        assertEquals(listOf(Sound.BOING), cues.onEvents(boing, kid, frame))
        assertEquals(emptyList(), cues.onEvents(boing, kid, frame))
        assertEquals(listOf(Sound.BOING), cues.onEvents(boing, kid, SoundCues.REPEAT_SECONDS))
    }

    @Test fun leaningOnTheFenceSoundsOnce() {
        val sim = emptySim()
        val kid = sim.player(1000f, 1000f, EAST, 40f)
        val cues = SoundCues()
        val wall = listOf(GameEvent.HitWall(kid.id, 0f, 0f))
        assertEquals(listOf(Sound.WALL), cues.onEvents(wall, kid, frame / 2))
        repeat(240) { i -> assertEquals(emptyList(), cues.onEvents(if (i % 2 == 0) emptyList() else wall, kid, frame / 2)) }
    }
}
