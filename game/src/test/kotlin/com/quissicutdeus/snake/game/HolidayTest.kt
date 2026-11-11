package com.quissicutdeus.snake.game

import com.quissicutdeus.snake.game.model.Hat
import com.quissicutdeus.snake.game.model.Holiday
import com.quissicutdeus.snake.game.model.Pattern
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.model.Stats
import com.quissicutdeus.snake.game.model.Unlocks
import com.quissicutdeus.snake.game.sim.GameSim
import com.quissicutdeus.snake.game.sim.SimOptions
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HolidayTest {
    @Test fun eachHolidayHasItsDays() {
        assertEquals(setOf(Holiday.HALLOWEEN), Holiday.on(2026, 10, 31))
        assertEquals(setOf(Holiday.THANKSGIVING), Holiday.on(2026, 11, 26))
        assertEquals(setOf(Holiday.THANKSGIVING, Holiday.FLAG_DAYS), Holiday.on(2026, 11, 11)) // Veterans Day
        assertEquals(setOf(Holiday.CHRISTMAS), Holiday.on(2026, 12, 25))
        assertEquals(setOf(Holiday.FLAG_DAYS), Holiday.on(2026, 7, 4))
        assertEquals(setOf(Holiday.FLAG_DAYS), Holiday.on(2026, 5, 26)) // Memorial Day
        assertEquals(setOf(Holiday.FLAG_DAYS), Holiday.on(2026, 2, 16)) // Presidents' Day
        assertEquals(setOf(Holiday.ST_PATRICK), Holiday.on(2026, 3, 15))
        assertEquals(emptySet(), Holiday.on(2026, 9, 30))
    }

    @Test fun playingOnAHolidayKeepsItsLooksForGood() {
        val fresh = Stats()
        assertFalse(Unlocks.isUnlocked(Hat.SANTA, fresh))
        val christmas = fresh.after(score = 20, eaten = 0, season = Holiday.on(2026, 12, 24))
        assertEquals(listOf(Hat.SANTA), Unlocks.newlyUnlocked(fresh, christmas).map { it.item })
        val january = christmas.after(score = 20, eaten = 0, season = Holiday.on(2026, 1, 15))
        assertTrue(Unlocks.isUnlocked(Hat.SANTA, january))
        val july = january.after(score = 20, eaten = 0, season = Holiday.on(2026, 7, 4))
        assertEquals(listOf(Pattern.FLAG, Hat.STAR_HAT), Unlocks.newlyUnlocked(january, july).map { it.item })
    }

    @Test fun lockedHolidayLooksSayWhenToComeBack() {
        val ghost = Unlocks.ruleFor(Hat.GHOST)!!
        assertTrue(ghost.isHoliday)
        assertFalse(Unlocks.ruleFor(Hat.WIZARD)!!.isHoliday)
        assertEquals("Play in October", ghost.goal)
        assertEquals("Comes out in October!", ghost.hint(Stats()))
        assertEquals(0, ghost.progress(Stats()))
        assertEquals("You did it!", ghost.hint(Stats(holidays = setOf(Holiday.HALLOWEEN))))
    }

    @Test fun holidaysSurviveSavingAndOldSavesStillLoad() {
        val s = Stats(250, 41, 33, setOf(Holiday.CHRISTMAS, Holiday.HALLOWEEN))
        assertEquals("250,41,33,CHRISTMAS+HALLOWEEN", s.encode())
        assertEquals(s, Stats.decode(s.encode()))
        assertEquals("250,41,33", Stats(250, 41, 33).encode())
        assertEquals(Stats(250, 41, 33), Stats.decode("250,41,33"))
        // A holiday this version doesn't know about is skipped, not a reason to lose the totals.
        assertEquals(Stats(1, 2, 3, setOf(Holiday.CHRISTMAS)), Stats.decode("1,2,3,CHRISTMAS+GROUNDHOG_DAY"))
        assertEquals(null, Stats.decode("1,2,3,4,5"))
    }

    @Test fun computerSnakesDressForTheSeason() {
        val rng = Random(3)
        val styles = List(400) { Holiday.dressUp(SnakeStyle.random(rng), rng, setOf(Holiday.CHRISTMAS)) }
        assertTrue(styles.count { it.hat == Hat.SANTA } > 120, "plenty of Santa hats")
        assertTrue(styles.none { it.hat == Hat.GHOST || it.hat == Hat.TURKEY || it.hat == Hat.STAR_HAT || it.pattern == Pattern.FLAG })
        val offSeason = List(400) { Holiday.dressUp(SnakeStyle.random(rng), rng, emptySet()) }
        assertTrue(offSeason.none { Holiday.of(it.hat) != null || Holiday.of(it.pattern) != null })
    }

    @Test fun theSpawnerDressesComputerSnakesUp() {
        val sim = GameSim(seed = 5, options = SimOptions(season = setOf(Holiday.HALLOWEEN)))
        val ais = sim.world.snakes.filter { !it.isPlayer }
        assertTrue(ais.isNotEmpty())
        assertTrue(ais.any { it.style.hat == Hat.GHOST })
        assertTrue(ais.none { it.style.hat == Hat.SANTA })
    }
}
