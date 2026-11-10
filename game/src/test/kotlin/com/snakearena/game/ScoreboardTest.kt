package com.snakearena.game

import com.snakearena.game.model.Eyes
import com.snakearena.game.model.Hat
import com.snakearena.game.model.Pattern
import com.snakearena.game.model.ScoreEntry
import com.snakearena.game.model.SnakeStyle
import com.snakearena.game.sim.Scoreboard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoreboardTest {
    private fun entry(score: Int, time: Long, name: String = "Kid$time") =
        ScoreEntry(name, score, snakesEaten = score / 10, style = SnakeStyle.DEFAULT, timestamp = time)

    private fun fullBoard() = (1..10).map { entry(score = it * 10, time = it.toLong()) }

    @Test fun keepsTheTopTenSortedBestFirst() {
        var board = emptyList<ScoreEntry>()
        for ((i, s) in listOf(40, 90, 10, 70, 30, 100, 20, 60, 80, 50, 110, 5).withIndex()) {
            board = Scoreboard.insert(board, entry(s, i.toLong())).entries
        }
        assertEquals(10, board.size)
        assertEquals(listOf(110, 100, 90, 80, 70, 60, 50, 40, 30, 20), board.map { it.score })
    }

    @Test fun insertTellsWhereTheNewEntryLanded() {
        val board = fullBoard()
        val r = Scoreboard.insert(board, entry(55, 100))
        assertEquals(5, r.index)
        assertEquals(55, r.entries[5].score)
        assertEquals(10, r.entries.size)
        assertFalse(r.entries.any { it.score == 10 }, "the old last place drops off")

        val miss = Scoreboard.insert(board, entry(5, 101))
        assertEquals(-1, miss.index)
        assertEquals(board.sortedByDescending { it.score }, miss.entries)
    }

    @Test fun equalScoresKeepTheOlderRunFirst() {
        val board = listOf(entry(50, 1, "Old"))
        val r = Scoreboard.insert(board, entry(50, 2, "New"))
        assertEquals(listOf("Old", "New"), r.entries.map { it.name })
        assertEquals(1, r.index)
    }

    @Test fun qualifyingScores() {
        assertTrue(Scoreboard.qualifies(emptyList(), 1))
        assertFalse(Scoreboard.qualifies(emptyList(), 0))
        val board = fullBoard() // lowest is 10
        assertFalse(Scoreboard.qualifies(board, 9))
        assertFalse(Scoreboard.qualifies(board, 10), "tying last place isn't enough")
        assertTrue(Scoreboard.qualifies(board, 11))
        assertTrue(Scoreboard.qualifies(board.take(9), 1), "a list with room takes anything")
    }

    @Test fun namesAreTidied() {
        assertEquals("Mia", Scoreboard.cleanName("  Mia \n"))
        assertEquals("Big Sis", Scoreboard.cleanName("Big    Sis"))
        assertEquals("Supercalifra", Scoreboard.cleanName("Supercalifragilistic"))
        assertEquals(Scoreboard.DEFAULT_NAME, Scoreboard.cleanName("   "))
        assertEquals("AB", Scoreboard.cleanName("A\tB"))
    }

    @Test fun rememberedNamesPutTheLatestFirstWithoutDuplicates() {
        var names = emptyList<String>()
        names = Scoreboard.rememberName(names, "Mia")
        names = Scoreboard.rememberName(names, "Leo")
        names = Scoreboard.rememberName(names, "mia ")
        assertEquals(listOf("mia", "Leo"), names)
        repeat(20) { names = Scoreboard.rememberName(names, "Kid $it") }
        assertEquals(Scoreboard.MAX_REMEMBERED_NAMES, names.size)
        assertEquals("Kid 19", names.first())
        assertEquals(names, Scoreboard.decodeNames(Scoreboard.encodeNames(names)))
    }

    @Test fun saveFormatRoundTripsAndSurvivesGarbage() {
        val style = SnakeStyle(color1 = 3, color2 = 9, pattern = Pattern.RAINBOW, eyes = Eyes.SUNGLASSES, hat = Hat.CROWN)
        val board = listOf(
            ScoreEntry("Mia", 120, 3, style, 1_700_000_000_000),
            ScoreEntry("Leo", 80, 0, SnakeStyle.DEFAULT, 1_700_000_000_500),
        )
        val text = Scoreboard.encode(board)
        assertEquals(board, Scoreboard.decode(text))
        val damaged = text + "\nnot a score line\n12\tx\t3\t\tBob\n"
        assertEquals(board, Scoreboard.decode(damaged))
        assertEquals(emptyList(), Scoreboard.decode(null))
    }

    @Test fun snakeStyleSurvivesSavingAndBadInput() {
        val style = SnakeStyle(color1 = 11, color2 = 0, pattern = Pattern.CHECKER, eyes = Eyes.SLEEPY, hat = Hat.BOW)
        assertEquals(style, SnakeStyle.decode(style.encode()))
        assertEquals(SnakeStyle.DEFAULT, SnakeStyle.decode("garbage"))
        assertEquals(SnakeStyle.DEFAULT.copy(color1 = 2), SnakeStyle.decode("2,99,NOPE"))
    }
}
