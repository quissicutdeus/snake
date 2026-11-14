package com.quissicutdeus.sniddle.game

import com.quissicutdeus.sniddle.game.model.EyeColor
import com.quissicutdeus.sniddle.game.model.Eyes
import com.quissicutdeus.sniddle.game.model.Hat
import com.quissicutdeus.sniddle.game.model.Pattern
import com.quissicutdeus.sniddle.game.model.ScoreEntry
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.sim.Scoreboard
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoreboardTest {
    private fun entry(
        score: Int,
        time: Long,
        name: String = "Kid$time",
    ) = ScoreEntry(name, score, snailsEaten = score / 10, style = SnailStyle.DEFAULT, timestamp = time)

    private fun fullBoard() = (1..10).map { entry(score = it * 10, time = it.toLong()) }

    @Test fun keepsTheTopTenSortedBestFirst() {
        var board = emptyList<ScoreEntry>()
        for ((i, s) in listOf(40, 90, 10, 70, 30, 100, 20, 60, 80, 50, 110, 5).withIndex()) {
            board = Scoreboard.insert(board, entry(s, i.toLong()))
        }
        assertEquals(10, board.size)
        assertEquals(listOf(110, 100, 90, 80, 70, 60, 50, 40, 30, 20), board.map { it.score })
    }

    @Test fun aNewEntryLandsInItsPlace() {
        val board = fullBoard()
        val new = entry(55, 100)
        val r = Scoreboard.insert(board, new)
        assertEquals(5, r.indexOf(new))
        assertEquals(10, r.size)
        assertFalse(r.any { it.score == 10 }, "the old last place drops off")

        val low = entry(5, 101)
        val miss = Scoreboard.insert(board, low)
        assertFalse(low in miss)
        assertEquals(board.sortedByDescending { it.score }, miss)
    }

    @Test fun equalScoresKeepTheOlderRunFirst() {
        val board = listOf(entry(50, 1, "Old"))
        val r = Scoreboard.insert(board, entry(50, 2, "New"))
        assertEquals(listOf("Old", "New"), r.map { it.name })
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
        assertEquals("Caleb", Scoreboard.cleanName("  Caleb \n"))
        assertEquals("Big Sis", Scoreboard.cleanName("Big    Sis"))
        assertEquals("Supercalifra", Scoreboard.cleanName("Supercalifragilistic"))
        assertEquals(Scoreboard.DEFAULT_NAME, Scoreboard.cleanName("   "))
        assertEquals("AB", Scoreboard.cleanName("A\tB"))
    }

    @Test fun plainNamesAreCutByLetters() {
        assertEquals("Caleb", Scoreboard.fitName("Caleb"))
        assertEquals("Supercalifra", Scoreboard.fitName("Supercalifragilistic"))
        assertEquals("ab", Scoreboard.fitName("abc", 2))
        assertEquals("", Scoreboard.fitName("abc", 0))
    }

    @Test fun anEmojiAtTheLimitIsKeptWholeOrLeftOut() {
        // 11 letters and a snail: 13 code units, but 12 things a kid can see.
        val name = "Abcdefghijk$SNAIL"
        assertEquals(name, Scoreboard.cleanName(name))
        assertEquals(name, Scoreboard.cleanName("${name}z"))
        assertEquals("ab", Scoreboard.fitName("ab$SNAIL", 2))
    }

    @Test fun aFamilyEmojiIsOneCharacter() {
        assertEquals("ab", Scoreboard.fitName("ab$FAMILY", 2))
        assertEquals("ab$FAMILY", Scoreboard.fitName("ab${FAMILY}c", 3))
    }

    @Test fun aSkinToneStaysWithItsEmoji() {
        assertEquals("a$THUMBS_UP", Scoreboard.fitName("a${THUMBS_UP}b", 2))
        assertEquals("a", Scoreboard.fitName("a$THUMBS_UP", 1))
    }

    @Test fun flagsAndKeycapsAreNotSplit() {
        assertEquals(FLAG_GB, Scoreboard.fitName("$FLAG_GB$FLAG_FR", 1))
        assertEquals("$FLAG_GB$FLAG_FR", Scoreboard.fitName("$FLAG_GB$FLAG_FR", 2))
        assertEquals("x$KEYCAP_ONE", Scoreboard.fitName("x${KEYCAP_ONE}y", 2))
    }

    @Test fun aCutNameIsAlwaysWholeUtf16() {
        val name = "Mo $FAMILY$THUMBS_UP$FLAG_GB$FLAG_FR$KEYCAP_ONE${SNAIL}\u00E9!"
        for (limit in 0..name.length) {
            val cut = Scoreboard.fitName(name, limit)
            assertTrue(name.startsWith(cut))
            assertTrue(cut.isWholeUtf16(), "limit $limit left a lone surrogate")
            assertFalse(cut.endsWith(ZWJ), "limit $limit left a dangling joiner")
        }
    }

    @Test fun oneLetterPiledWithMarksCannotFillTheScreen() {
        val tower = "a" + "\u0301".repeat(Scoreboard.MAX_NAME_CHARS)
        assertEquals("Mo ", Scoreboard.fitName("Mo $tower"))
        assertEquals("Mo", Scoreboard.cleanName("Mo $tower"))
        assertTrue(Scoreboard.fitName(FAMILY.repeat(Scoreboard.MAX_NAME_LENGTH)).length <= Scoreboard.MAX_NAME_CHARS)
    }

    @Test fun typingPastTheLimitDoesNotScrambleTheName() {
        val full = "Abcdefghijkl"
        assertEquals(full, Scoreboard.editName(full, "${full}m"), "typing at the end of a full name does nothing")
        assertEquals(full, Scoreboard.editName(full, "Abcdef${SNAIL}ghijkl"), "nor does typing in the middle")
        assertEquals("Abcdefghijk$SNAIL", Scoreboard.editName("Abcdefghijk", "Abcdefghijk$SNAIL"))
        assertEquals("Abcdefghijkl", Scoreboard.editName("", "Abcdefghijklmnop"), "a pasted long name is cut")
        assertEquals("Abcdeghijkl", Scoreboard.editName(full, "Abcdeghijkl"), "deleting always works")
        assertEquals("Ab Cd", Scoreboard.editName("Ab", "Ab Cd"))
    }

    @Test fun rememberedNamesPutTheLatestFirstWithoutDuplicates() {
        var names = emptyList<String>()
        names = Scoreboard.rememberName(names, "Caleb")
        names = Scoreboard.rememberName(names, "Mya")
        names = Scoreboard.rememberName(names, "caleb ")
        assertEquals(listOf("caleb", "Mya"), names)
        repeat(20) { names = Scoreboard.rememberName(names, "Kid $it") }
        assertEquals(Scoreboard.MAX_REMEMBERED_NAMES, names.size)
        assertEquals("Kid 19", names.first())
        assertEquals(names, Scoreboard.decodeNames(Scoreboard.encodeNames(names)))
    }

    @Test fun saveFormatRoundTripsAndSurvivesGarbage() {
        val style = SnailStyle(color1 = 3, color2 = 9, pattern = Pattern.RAINBOW, eyes = Eyes.SUNGLASSES, hat = Hat.CROWN)
        val board =
            listOf(
                ScoreEntry("Caleb", 120, 3, style, 1_700_000_000_000),
                ScoreEntry("Mya", 80, 0, SnailStyle.DEFAULT, 1_700_000_000_500),
            )
        val text = Scoreboard.encode(board)
        assertEquals(board, Scoreboard.decode(text))
        val damaged = text + "\nnot a score line\n12\tx\t3\t\tBob\n"
        assertEquals(board, Scoreboard.decode(damaged))
        assertEquals(emptyList(), Scoreboard.decode(null))
    }

    @Test fun snailStyleSurvivesSavingAndBadInput() {
        val style = SnailStyle(color1 = 11, color2 = 0, pattern = Pattern.CHECKER, eyes = Eyes.SLEEPY, hat = Hat.BOW)
        assertEquals(style, SnailStyle.decode(style.encode()))
        assertEquals(SnailStyle.DEFAULT, SnailStyle.decode("garbage"))
        assertEquals(SnailStyle.DEFAULT.copy(color1 = 2), SnailStyle.decode("2,99,NOPE"))
    }

    @Test fun eyeColourIsSavedLastAndOldSnailsKeepClassicEyes() {
        val blueEyes =
            SnailStyle(color1 = 7, color2 = 2, pattern = Pattern.DOTS, eyes = Eyes.SPARKLY, hat = Hat.PIRATE, eyeColor = EyeColor.BODY)
        assertEquals("7,2,DOTS,SPARKLY,PIRATE,BODY", blueEyes.encode())
        assertEquals(blueEyes, SnailStyle.decode(blueEyes.encode()))
        assertEquals(SnailStyle.PALETTE[7], blueEyes.irisColor)
        assertEquals(SnailStyle.PALETTE[2], blueEyes.copy(eyeColor = EyeColor.SHELL).irisColor)
        // Saved before eye colours existed: five fields, and the eyes stay classic dark.
        val old = SnailStyle.decode("11,0,CHECKER,SLEEPY,BOW")
        assertEquals(SnailStyle(color1 = 11, color2 = 0, pattern = Pattern.CHECKER, eyes = Eyes.SLEEPY, hat = Hat.BOW), old)
        assertEquals(EyeColor.CLASSIC, old.eyeColor)
        assertEquals(null, old.irisColor)
        assertEquals(EyeColor.CLASSIC, SnailStyle.decode("11,0,CHECKER,SLEEPY,BOW,PURPLE").eyeColor)
    }

    /** Every surrogate is half of a pair. */
    private fun String.isWholeUtf16() =
        indices.all { i ->
            when {
                this[i].isHighSurrogate() -> getOrNull(i + 1)?.isLowSurrogate() == true
                this[i].isLowSurrogate() -> getOrNull(i - 1)?.isHighSurrogate() == true
                else -> true
            }
        }

    private companion object {
        const val SNAIL = "\uD83D\uDC0C"
        const val ZWJ = "\u200D"
        const val FAMILY = "\uD83D\uDC68$ZWJ\uD83D\uDC69$ZWJ\uD83D\uDC67$ZWJ\uD83D\uDC66"
        const val THUMBS_UP = "\uD83D\uDC4D\uD83C\uDFFD"
        const val FLAG_GB = "\uD83C\uDDEC\uD83C\uDDE7"
        const val FLAG_FR = "\uD83C\uDDEB\uD83C\uDDF7"
        const val KEYCAP_ONE = "1\uFE0F\u20E3"
    }
}
