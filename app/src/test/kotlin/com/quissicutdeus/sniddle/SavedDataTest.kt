package com.quissicutdeus.sniddle

import androidx.core.content.edit
import com.quissicutdeus.sniddle.game.model.Birthday
import com.quissicutdeus.sniddle.game.model.Eyes
import com.quissicutdeus.sniddle.game.model.Hat
import com.quissicutdeus.sniddle.game.model.Pattern
import com.quissicutdeus.sniddle.game.model.ScoreEntry
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.model.Stats
import com.quissicutdeus.sniddle.ui.Settings
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What's already saved on the kids' phones keeps loading, and is written back
 * the same way: the "sniddle" SharedPreferences file, with the same keys and
 * value types, reached through [phoneStorage] like the app does.
 */
class SavedDataTest : ScreenTest() {
    private val green = SnailStyle(color1 = 3, color2 = 5, pattern = Pattern.ZIGZAG, eyes = Eyes.SLEEPY, hat = Hat.BOW)

    /** Every key, written by hand the way the app before KeyValueStore wrote it. */
    @Test fun everythingSavedBeforeStillLoads() {
        rawPrefs().edit(commit = true) {
            putString("scores", "120\t11\t1000\t8,2,DOTS,SUNGLASSES,PARTY\tCaleb\n40\t2\t2000\t3,5,ZIGZAG,SLEEPY,BOW\tBea\tlittle")
            putString("names", "Caleb\nBea")
            putBoolean("sound", false)
            putBoolean("buzz", false)
            putBoolean("music", true)
            putInt("bedtime_minutes", 45)
            putString("players", "0,1")
            putInt("player", 1)
            putInt("news_seen", 7)
            putString("play_day", "20404,600000,0")
            // Player 0: the keys from before there were players.
            putString("style", "8,2,DOTS,SUNGLASSES,PARTY")
            putString("snail_name", "Caleb")
            putString("stats", "120,11,10")
            putStringSet("announced", setOf("Hat.TOP_HAT", "Pattern.STARS"))
            putBoolean("little_kid", false)
            putInt("birthday", 912)
            // Player 1.
            putString("p1.style", "3,5,ZIGZAG,SLEEPY,BOW")
            putString("p1.snail_name", "Bea")
            putString("p1.stats", "40,2,1")
            putStringSet("p1.announced", emptySet())
            putBoolean("p1.little_kid", true)
            putInt("p1.birthday", 229)
        }
        val s = phoneStorage(org.robolectric.RuntimeEnvironment.getApplication())

        assertEquals(
            listOf(ScoreEntry("Caleb", 120, 11, sirNoodles, 1000L), ScoreEntry("Bea", 40, 2, green, 2000L, littleKid = true)),
            s.loadScores(),
        )
        assertEquals(listOf("Caleb", "Bea"), s.loadNames())
        assertEquals(Settings(soundOn = false, buzzOn = false, musicOn = true, bedtimeMinutes = 45), s.loadSettings())
        assertEquals(listOf(0, 1), s.loadPlayers())
        assertEquals(1, s.loadCurrentPlayer())
        assertEquals(7, s.newsSeen)
        assertEquals("20404,600000,0", s.playDay)
        with(s.player(0)) {
            assertEquals(sirNoodles, loadStyle())
            assertEquals("Caleb", loadSnailName())
            assertEquals(Stats(120, 11, 10), loadStats())
            assertEquals(setOf("Hat.TOP_HAT", "Pattern.STARS"), loadAnnounced())
            assertEquals(false, loadLittleKid())
            assertEquals(Birthday(9, 12), loadBirthday())
        }
        with(s.player(1)) {
            assertEquals(green, loadStyle())
            assertEquals("Bea", loadSnailName())
            assertEquals(Stats(40, 2, 1), loadStats())
            assertEquals(emptySet<String>(), loadAnnounced())
            assertEquals(true, loadLittleKid())
            assertEquals(Birthday(2, 29), loadBirthday())
        }
    }

    /** Saving writes the same keys, with the same value types, an older version (or a downgrade) reads. */
    @Test fun savingWritesTheSameKeysAndTypes() {
        storage.addScore(ScoreEntry("Caleb", 120, 11, sirNoodles, 1000L))
        storage.saveSettings(Settings(soundOn = false, buzzOn = true, musicOn = false, bedtimeMinutes = 30))
        storage.newsSeen = 3
        storage.playDay = "20404,1,2"
        storage.player(0).apply {
            saveStyle(sirNoodles)
            saveSnailName("Caleb")
            saveStats(Stats(120, 11, 10))
            saveAnnounced(setOf("Hat.TOP_HAT"))
            saveLittleKid(true)
            saveBirthday(Birthday(9, 12))
        }
        val id = checkNotNull(storage.addPlayer(green))
        storage.saveCurrentPlayer(id)

        val saved = rawPrefs().all
        assertEquals(
            mapOf<String, Any>(
                "scores" to "120\t11\t1000\t8,2,DOTS,SUNGLASSES,PARTY,CLASSIC\tCaleb",
                "names" to "Caleb",
                "sound" to false,
                "buzz" to true,
                "music" to false,
                "bedtime_minutes" to 30,
                "news_seen" to 3,
                "play_day" to "20404,1,2",
                "style" to "8,2,DOTS,SUNGLASSES,PARTY,CLASSIC",
                "snail_name" to "Caleb",
                "stats" to "120,11,10",
                "announced" to setOf("Hat.TOP_HAT"),
                "little_kid" to true,
                "birthday" to 912,
                "players" to "0,1",
                "player" to 1,
                "p1.style" to "3,5,ZIGZAG,SLEEPY,BOW,CLASSIC",
                "p1.snail_name" to "",
                "p1.stats" to Stats().encode(),
                "p1.announced" to emptySet<String>(),
                "p1.little_kid" to false,
            ),
            saved,
        )
    }
}
