package com.quissicutdeus.sniddle.game

import com.quissicutdeus.sniddle.game.model.ArenaSeason
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Which floor the title screen and each new game get: wrapping paper all December, else the season's, dirt or space. */
class ArenaFloorTest {
    @Test fun wrappingPaperIsForDecemberOnly() {
        assertNull(ArenaSeason.festive(11, 30))
        assertEquals(ArenaSeason.WRAPPING_PAPER, ArenaSeason.festive(12, 1))
        assertEquals(ArenaSeason.WRAPPING_PAPER, ArenaSeason.festive(12, 31))
        assertNull(ArenaSeason.festive(1, 1))

        assertEquals(ArenaSeason.AUTUMN, ArenaSeason.forTitle(11, 30))
        assertEquals(ArenaSeason.WRAPPING_PAPER, ArenaSeason.forTitle(12, 1))
        assertEquals(ArenaSeason.WRAPPING_PAPER, ArenaSeason.forTitle(12, 31))
        assertEquals(ArenaSeason.SNOW, ArenaSeason.forTitle(1, 1))
    }

    @Test fun everyGameInDecemberIsOnWrappingPaper() {
        val rng = Random(1)
        for (day in 1..31) {
            repeat(50) { assertEquals(ArenaSeason.WRAPPING_PAPER, ArenaSeason.forGame(12, day, rng), "December $day") }
        }
        val edges = (List(200) { ArenaSeason.forGame(11, 30, rng) } + List(200) { ArenaSeason.forGame(1, 1, rng) }).toSet()
        assertEquals(
            setOf(ArenaSeason.AUTUMN, ArenaSeason.SNOW, ArenaSeason.DIRT, ArenaSeason.SPACE),
            edges,
            "30 November and 1 January are ordinary days",
        )
    }

    @Test fun aGameIsOnTheSeasonsFloorAboutHalfTheTimeAndDirtOrSpaceAQuarterEach() {
        val rng = Random(2026)
        val draws = 20_000
        // December is all wrapping paper (above).
        for (month in 1..11) {
            val counts = List(draws) { ArenaSeason.forGame(month, 5, rng) }.groupingBy { it }.eachCount()
            assertEquals(setOf(ArenaSeason.forMonth(month), ArenaSeason.DIRT, ArenaSeason.SPACE), counts.keys, "month $month")

            fun share(floor: ArenaSeason) = counts.getValue(floor).toFloat() / draws
            assertTrue(share(ArenaSeason.forMonth(month)) in 0.47f..0.53f, "season's floor ${share(ArenaSeason.forMonth(month))}")
            assertTrue(share(ArenaSeason.DIRT) in 0.22f..0.28f, "dirt ${share(ArenaSeason.DIRT)}")
            assertTrue(share(ArenaSeason.SPACE) in 0.22f..0.28f, "space ${share(ArenaSeason.SPACE)}")
        }
    }

    @Test fun theTitleIsNeverDirtOrSpace() {
        for (month in 1..12) {
            for (day in 1..31) {
                val floor = ArenaSeason.forTitle(month, day)
                assertTrue(floor != ArenaSeason.DIRT && floor != ArenaSeason.SPACE, "$month/$day got $floor")
            }
        }
    }

    @Test fun everyFloorHasItsOwnSavedKey() {
        assertEquals(
            ArenaSeason.entries.size,
            ArenaSeason.entries
                .map { it.key }
                .toSet()
                .size,
        )
        for (floor in ArenaSeason.entries) assertEquals(floor, ArenaSeason.byKey(floor.key))
        assertNull(ArenaSeason.byKey("lava"))
    }
}
