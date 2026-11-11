package com.quissicutdeus.snake.game

import com.quissicutdeus.snake.game.model.ArenaSeason
import com.quissicutdeus.snake.game.model.Birthday
import com.quissicutdeus.snake.game.model.Dates
import com.quissicutdeus.snake.game.model.Hat
import com.quissicutdeus.snake.game.model.Holiday
import com.quissicutdeus.snake.game.model.Pattern
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.model.Stats
import com.quissicutdeus.snake.game.model.Unlocks
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Easter, birthdays, the spring holidays and the arena's seasons. */
class CalendarTest {
    @Test fun easterSundayForKnownYears() {
        // Days after the end of February: 31 is 31 March, 32 is 1 April.
        val known =
            mapOf(
                2000 to 31 + 23, // 23 April
                2008 to 23, // 23 March, about as early as it gets
                2011 to 31 + 24,
                2018 to 31 + 1,
                2019 to 31 + 21,
                2024 to 31, // 31 March
                2025 to 31 + 20,
                2026 to 31 + 5,
                2027 to 28,
                2038 to 31 + 25, // 25 April, the latest it can be
                2285 to 22, // 22 March, the earliest it can be
            )
        for ((year, day) in known) assertEquals(day, Dates.easterSunday(year), "Easter $year")
    }

    @Test fun easterRunsFromTheSundayBeforeToEasterMonday() {
        // 2026: Easter Sunday is 5 April, so 29 March through 6 April.
        assertFalse(Holiday.EASTER in Holiday.on(2026, 3, 28))
        assertTrue(Holiday.EASTER in Holiday.on(2026, 3, 29))
        assertTrue(Holiday.EASTER in Holiday.on(2026, 3, 31))
        assertTrue(Holiday.EASTER in Holiday.on(2026, 4, 1)) // across the end of March
        assertTrue(Holiday.EASTER in Holiday.on(2026, 4, 6))
        assertFalse(Holiday.EASTER in Holiday.on(2026, 4, 7))
        // 2024: Easter Sunday was 31 March, so 24 March through 1 April.
        assertTrue(Holiday.EASTER in Holiday.on(2024, 3, 24))
        assertTrue(Holiday.EASTER in Holiday.on(2024, 4, 1))
        assertFalse(Holiday.EASTER in Holiday.on(2024, 4, 2))
        // The same day can be Easter one year and not the next.
        assertTrue(Holiday.EASTER in Holiday.on(2025, 4, 20))
        assertFalse(Holiday.EASTER in Holiday.on(2026, 4, 20))
        // Never outside March and April, whatever the year.
        assertFalse((1900..2200).any { Holiday.EASTER in Holiday.on(it, 5, 1) || Holiday.EASTER in Holiday.on(it, 2, 28) })
    }

    @Test fun theSpringHolidaysHaveTheirDays() {
        assertEquals(setOf(Holiday.VALENTINE), Holiday.on(2026, 2, 7))
        assertEquals(setOf(Holiday.VALENTINE, Holiday.FLAG_DAYS), Holiday.on(2026, 2, 14)) // with Presidents' Day
        assertEquals(setOf(Holiday.FLAG_DAYS), Holiday.on(2026, 2, 15))
        assertEquals(emptySet(), Holiday.on(2026, 2, 6))
        assertEquals(setOf(Holiday.ST_PATRICK), Holiday.on(2026, 3, 17))
        assertEquals(emptySet(), Holiday.on(2026, 3, 9))
        assertEquals(emptySet(), Holiday.on(2026, 3, 18))
        // An early Easter can run into St Patrick's week: 2008's started on 16 March.
        assertEquals(setOf(Holiday.ST_PATRICK, Holiday.EASTER), Holiday.on(2008, 3, 17))
    }

    @Test fun aBirthdayIsOnlyOnThatDay() {
        val september = Birthday(9, 12)
        assertTrue(september.isOn(2026, 9, 12))
        assertFalse(september.isOn(2026, 9, 11))
        assertFalse(september.isOn(2026, 9, 13))
        assertFalse(september.isOn(2026, 12, 9))
        assertEquals(setOf(Holiday.BIRTHDAY), Holiday.on(2026, 9, 12, september))
        assertEquals(emptySet(), Holiday.on(2026, 9, 13, september))
        assertEquals(emptySet(), Holiday.on(2026, 9, 12, null))
        // It's no holiday by the date alone, and joins whatever else is on.
        assertTrue((1..12).none { m -> (1..Dates.daysIn(m)).any { d -> Holiday.BIRTHDAY in Holiday.on(2026, m, d) } })
        assertEquals(setOf(Holiday.CHRISTMAS, Holiday.BIRTHDAY), Holiday.on(2026, 12, 25, Birthday(12, 25)))
    }

    @Test fun aLeapDayBirthdayIsOnThe28thInOtherYears() {
        val leap = Birthday(2, 29)
        assertTrue(leap.isOn(2028, 2, 29))
        assertFalse(leap.isOn(2028, 2, 28)) // a leap year has the real day
        assertTrue(leap.isOn(2026, 2, 28))
        assertFalse(leap.isOn(2026, 3, 1))
        assertTrue(leap.isOn(2100, 2, 28)) // not a leap year
        assertTrue(leap.isOn(2000, 2, 29)) // a leap year
        assertFalse(leap.isOn(2000, 2, 28))
        // Someone born on the 28th still has just the 28th.
        assertTrue(Birthday(2, 28).isOn(2028, 2, 28))
        assertFalse(Birthday(2, 28).isOn(2028, 2, 29))
    }

    @Test fun leapYears() {
        assertTrue(Dates.isLeapYear(2024))
        assertTrue(Dates.isLeapYear(2000))
        assertFalse(Dates.isLeapYear(2026))
        assertFalse(Dates.isLeapYear(1900))
        assertFalse(Dates.isLeapYear(2100))
    }

    @Test fun birthdaysAreSavedAsMonthAndDay() {
        assertEquals(1225, Birthday(12, 25).encode())
        assertEquals(Birthday(2, 29), Birthday.decode(Birthday(2, 29).encode()))
        assertEquals(Birthday(1, 1), Birthday.decode(101))
        assertNull(Birthday.decode(null))
        assertNull(Birthday.decode(230)) // 30 February
        assertNull(Birthday.decode(1301))
        assertNull(Birthday.decode(400)) // 0 April
        assertNull(Birthday.decode(-5))
        assertFailsWith<IllegalArgumentException> { Birthday(4, 31) }
        assertEquals(listOf(31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31), (1..12).map(Dates::daysIn))
    }

    @Test fun theBirthdayHatIsKeptForGood() {
        val rule = Unlocks.ruleFor(Hat.BIRTHDAY_CAKE)!!
        assertTrue(rule.isHoliday) // no progress bar
        assertEquals("On your birthday!", rule.goal)
        val fresh = Stats()
        assertFalse(Unlocks.isUnlocked(Hat.BIRTHDAY_CAKE, fresh))
        val birthday = fresh.after(score = 20, eaten = 0, season = Holiday.on(2026, 9, 12, Birthday(9, 12)))
        assertEquals(listOf(Hat.BIRTHDAY_CAKE), Unlocks.newlyUnlocked(fresh, birthday).map { it.item })
        val later = Stats.decode(birthday.after(score = 5, eaten = 0, season = Holiday.on(2026, 9, 1)).encode())!!
        assertTrue(Unlocks.isUnlocked(Hat.BIRTHDAY_CAKE, later))
    }

    @Test fun theNewHolidayLooksUnlockOnTheirDays() {
        val fresh = Stats()
        assertEquals(listOf(Pattern.HEARTS), Unlocks.newlyUnlocked(fresh, fresh.after(1, 0, Holiday.on(2026, 2, 10))).map { it.item })
        assertEquals(listOf(Hat.SHAMROCK), Unlocks.newlyUnlocked(fresh, fresh.after(1, 0, Holiday.on(2026, 3, 12))).map { it.item })
        assertEquals(listOf(Hat.BUNNY), Unlocks.newlyUnlocked(fresh, fresh.after(1, 0, Holiday.on(2026, 4, 3))).map { it.item })
    }

    @Test fun onAKidsBirthdayTheComputerSnakesWearPartyHats() {
        val rng = Random(9)

        fun partyHats(season: Set<Holiday>) =
            List(400) { Holiday.dressUp(SnakeStyle.random(rng), rng, season) }.count { it.hat == Hat.PARTY }
        val usual = partyHats(emptySet())
        val birthday = partyHats(setOf(Holiday.BIRTHDAY))
        assertTrue(usual < 80, "about one in ${Hat.entries.size} on a usual day: $usual")
        assertTrue(birthday > 140, "lots on a birthday: $birthday")
    }

    @Test fun computerSnakesOnlyWearTheLooksOfWhatsOn() {
        val rng = Random(4)
        val seasonal = Holiday.entries.flatMap { it.looks }
        val offSeason = List(600) { Holiday.dressUp(SnakeStyle.random(rng), rng, emptySet()) }
        assertTrue(offSeason.none { it.hat in seasonal || it.pattern in seasonal })
        val valentine = List(600) { Holiday.dressUp(SnakeStyle.random(rng), rng, Holiday.on(2026, 2, 10)) }
        assertTrue(valentine.count { it.pattern == Pattern.HEARTS } > 180, "plenty of hearts")
        assertTrue(valentine.none { it.hat in seasonal || (it.pattern in seasonal && it.pattern != Pattern.HEARTS) })
        val easter = List(600) { Holiday.dressUp(SnakeStyle.random(rng), rng, Holiday.on(2026, 4, 5)) }
        assertTrue(easter.count { it.hat == Hat.BUNNY } > 180, "plenty of bunny ears")
        assertTrue(easter.none { it.hat == Hat.SHAMROCK || it.hat == Hat.BIRTHDAY_CAKE || it.pattern == Pattern.HEARTS })
    }

    @Test fun theArenaFollowsTheSeasons() {
        assertEquals(
            listOf("SNOW", "SNOW", "GRASS", "GRASS", "GRASS", "GRASS", "GRASS", "GRASS", "GRASS", "AUTUMN", "AUTUMN", "SNOW"),
            (1..12).map { ArenaSeason.forMonth(it).name },
        )
    }
}
