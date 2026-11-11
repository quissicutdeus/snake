package com.quissicutdeus.snake

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The bedtime timer's count: what counts, when a day starts afresh, and what survives a restart. */
class PlayClockTest {
    private var saved: String? = null
    private var limit = 15
    private var date = LocalDate.of(2026, 11, 11)

    private fun clock() = PlayClock({ limit }, { saved }, { saved = it }, { date })

    /** [seconds] of frames at 60 a second. */
    private fun PlayClock.play(
        seconds: Int,
        running: Boolean = true,
    ) = repeat(seconds * 60) { onFrame(1f / 60f, running) }

    private val minute = PlayClock.MS_PER_MINUTE

    @Test fun onlyRunningGamesCount() {
        val c = clock()
        c.play(60)
        c.play(120, running = false) // paused, game over, or the title screen
        assertEquals(minute.toDouble(), c.playedMs.toDouble(), 20.0)
        assertEquals((14 * minute).toDouble(), c.leftMs()!!.toDouble(), 20.0)
        assertFalse(c.timeUp)
    }

    @Test fun timeUpAtTheLimitAndNeverBelowZero() {
        val c = clock()
        c.play(15 * 60 + 30)
        assertEquals(0L, c.leftMs())
        assertTrue(c.timeUp)
    }

    @Test fun offMeansNoLimitButTheCountGoesOn() {
        limit = 0
        val c = clock()
        c.play(20 * 60)
        assertNull(c.leftMs())
        assertFalse(c.timeUp)
        // Turning it on later in the day knows what's been played already.
        limit = 15
        assertTrue(c.timeUp)
    }

    @Test fun aNewDayStartsAfreshAtMidnight() {
        val c = clock()
        c.play(15 * 60)
        c.grant(10)
        assertFalse(c.timeUp)
        date = date.plusDays(1)
        assertEquals(0L, c.playedMs)
        assertEquals(15 * minute, c.leftMs()) // yesterday's extra time is gone too
    }

    @Test fun aRestartCarriesOnFromWhatWasSaved() {
        val c = clock()
        c.play(3 * 60)
        c.play(2) // not yet written: the app is about to go to the background
        c.flush()
        val after = clock() // Android closed the app; it starts again
        assertEquals(c.playedMs, after.playedMs)
        after.play(12 * 60)
        assertTrue(clock().timeUp)
    }

    @Test fun killedWithoutWarningLosesOnlyAFewSeconds() {
        val c = clock()
        c.play(10 * 60 + 4) // never flushed: the process just died
        val lost = c.playedMs - clock().playedMs
        assertTrue("lost $lost ms", lost in 0..5_000)
    }

    @Test fun turningTheClockBackGivesNoNewTime() {
        val c = clock()
        c.play(15 * 60)
        date = date.minusDays(1)
        assertTrue(c.timeUp)
        assertTrue(clock().timeUp)
        // ...and turning it forward again to the real day doesn't either.
        date = date.plusDays(1)
        assertTrue(c.timeUp)
    }

    @Test fun aGrownUpSettingTheTimerMovesAFutureDayToToday() {
        val c = clock()
        date = date.plusDays(30) // set ahead by mistake
        c.play(15 * 60)
        date = date.minusDays(30) // put right
        assertTrue(c.timeUp)
        c.settle()
        assertTrue(c.timeUp)
        date = date.plusDays(1) // the real next day starts afresh
        assertFalse(c.timeUp)
    }

    @Test fun moreTimeFromAGrownUp() {
        val c = clock()
        c.play(15 * 60)
        c.grant(10)
        assertEquals(10 * minute, c.leftMs())
        assertEquals(10 * minute, clock().leftMs()) // written straight away
    }

    @Test fun unreadableRecordsStartAfresh() {
        assertNull(PlayDay.decode(null))
        assertNull(PlayDay.decode("1,2"))
        assertNull(PlayDay.decode("a,b,c"))
        assertEquals(PlayDay(5, 6, 7), PlayDay.decode(PlayDay(5, 6, 7).encode()))
        saved = "rubbish"
        assertEquals(0L, clock().playedMs)
    }
}
