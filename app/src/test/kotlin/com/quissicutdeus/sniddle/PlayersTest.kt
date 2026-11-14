package com.quissicutdeus.sniddle

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.core.content.edit
import com.quissicutdeus.sniddle.game.model.Eyes
import com.quissicutdeus.sniddle.game.model.Hat
import com.quissicutdeus.sniddle.game.model.Holiday
import com.quissicutdeus.sniddle.game.model.Pattern
import com.quissicutdeus.sniddle.game.model.ScoreEntry
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.model.Stats
import com.quissicutdeus.sniddle.game.model.Unlocks
import com.quissicutdeus.sniddle.game.sim.Eater
import com.quissicutdeus.sniddle.ui.GameOverScreen
import com.quissicutdeus.sniddle.ui.GameResult
import com.quissicutdeus.sniddle.ui.GameScreen
import com.quissicutdeus.sniddle.ui.HighScoresScreen
import com.quissicutdeus.sniddle.ui.MySnailScreen
import com.quissicutdeus.sniddle.ui.Profile
import com.quissicutdeus.sniddle.ui.Settings
import com.quissicutdeus.sniddle.ui.SniddleApp
import com.quissicutdeus.sniddle.ui.SniddleTheme
import com.quissicutdeus.sniddle.ui.TitleScreen
import com.quissicutdeus.sniddle.ui.WhatsNew
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

/** Each kid on the phone has their own snail, name and unlocks; adding, switching and removing them. */
class PlayersTest : ScreenTest() {
    /** A phone updated from before players existed opens with its snail, name and unlocks just as they were. */
    @Test fun anUpdatedPhoneKeepsItsSnail() {
        // Written the way the version before players saved them: no prefix.
        rawPrefs().edit(commit = true) {
            putString("style", sirNoodles.encode())
            putString("snail_name", "Caleb")
            putString("stats", "120,11,10")
            putStringSet("announced", setOf("Hat.TOP_HAT", "Pattern.STARS", "Eyes.HEARTS"))
        }
        storage.addScore(ScoreEntry("Caleb", 120, 4, sirNoodles, 1000L))
        val me = Profile(storage, SystemCalendar)
        assertEquals(sirNoodles, me.style)
        assertEquals("Caleb", me.snailName)
        assertEquals(Stats(120, 11, 10), me.stats)
        // Looks already cheered aren't cheered again; only the propeller cap, newer than that save, is.
        assertEquals(listOf<Any>(Hat.PROPELLER), me.alreadyEarned.map { it.item })
        launchApp()
        compose.onNodeWithText("+ Add player").assertExists()
        snap("players_upgraded")
        compose.onNodeWithText("My Snails").performClick()
        advance(500)
        compose.onNode(hasSetTextAction() and hasText("Caleb")).assertExists()
        compose.onNodeWithContentDescription("Top hat, locked", substring = true).assertDoesNotExist()
    }

    /**
     * An updated phone whose unlock totals were never saved works them out from
     * the high scores. A second kid's scores join that list, so they must not
     * count for the first kid.
     */
    @Test fun aNewKidsScoresDontUnlockLooksForTheFirst() {
        seedOldScores()
        val first = storage.player(0).loadStats()
        assertEquals(Stats(120, 0, 10), first)
        val me = Profile(storage, SystemCalendar)
        me.addPlayer(kotlin.random.Random(1))
        assertEquals(1, me.playerId)
        assertEquals(Stats(), me.stats)
        assertEquals(emptyList<Any>(), me.alreadyEarned)
        repeat(3) { storage.addScore(ScoreEntry("Mya", 250, 50, style, 5000L + it)) }
        me.recordGame(GameResult(250, 50, style, 6000L), season = emptySet())
        assertEquals(first, storage.player(0).loadStats())
        // The first kid's keys are still the old, unprefixed ones.
        assertEquals("120,0,10", rawPrefs().getString("stats", null))
        assertEquals("250,50,1", rawPrefs().getString("p1.stats", null))
    }

    /** Three kids: Caleb (playing), Mya and one who hasn't named their snail yet. */
    private fun seedThreePlayers() {
        storage.player(0).apply {
            saveStyle(style)
            saveSnailName("Caleb")
            saveStats(Stats(bestScore = 120))
            saveAnnounced(setOf("Hat.TOP_HAT"))
        }
        storage.addPlayer(sirNoodles)
        storage.player(1).saveSnailName("Mya")
        storage.addPlayer(style.copy(color1 = 0, color2 = 10, pattern = Pattern.ZIGZAG, hat = Hat.NONE, eyes = Eyes.SLEEPY))
    }

    private fun playersScene(name: String) {
        seedThreePlayers()
        launchApp()
        compose.onNodeWithContentDescription("Caleb is playing").assertExists()
        compose.onNodeWithContentDescription("Switch to Player 3").assertExists()
        compose.onNodeWithContentDescription("Add a player").assertExists()
        snap(name)
    }

    @Test fun playersLandscape() = playersScene("players_landscape")

    @Config(qualifiers = PORTRAIT)
    @Test
    fun playersPortrait() = playersScene("players_portrait")

    @Config(qualifiers = TABLET)
    @Test
    fun playersTablet() = playersScene("players_tablet")

    @Config(qualifiers = NIGHT)
    @Test
    fun playersNight() = playersScene("players_landscape")

    /** The fullest a short phone screen gets: four kids, and a cheer for looks already earned. */
    @Test fun fourPlayersAndACheerLandscape() {
        seedOldScores()
        repeat(3) { storage.addPlayer(SnailStyle.random(kotlin.random.Random(it))) }
        storage.player(2).saveSnailName("Maximilianus")
        launchApp()
        compose.onNodeWithText(alreadyEarned).assertExists()
        compose.onNodeWithContentDescription("Add a player").assertDoesNotExist()
        snap("players_full_landscape")
    }

    /** Just as full, with the "What's new" card in place of the cheer (it comes first after an update). */
    @Test fun fourPlayersAndNewsLandscape() {
        seedOldScores(newsSeen = false)
        repeat(3) { storage.addPlayer(SnailStyle.random(kotlin.random.Random(it))) }
        launchApp()
        compose.onNodeWithText("Holiday hats to win!").assertExists()
        compose.onNodeWithText("Play!").assertExists()
        snap("players_full_news_landscape")
    }

    /** "+ Add player" makes a new kid with a fresh snail, opens My Snails to name it, then shows everyone. */
    @Test fun addingAPlayer() {
        storage.player(0).saveSnailName("Caleb")
        val launch = launchApp()
        compose.onNodeWithText("+ Add player").performClick()
        advance(500)
        compose.onNodeWithText("Type a name").assertExists()
        assertEquals(listOf(0, 1), storage.loadPlayers())
        assertEquals(1, storage.loadCurrentPlayer())
        snap("players_adding")
        compose.onNodeWithText("Done").performClick()
        advance(500)
        compose.onNodeWithContentDescription("Player 2 is playing").assertExists()
        compose.onNodeWithContentDescription("Switch to Caleb").assertExists()
        snap("players_added")
        // Still the new kid after the next launch.
        launch.intValue++
        advance(300)
        compose.onNodeWithContentDescription("Player 2 is playing").assertExists()
    }

    /** Switching kids swaps the look and unlocks; each kid's game counts only for them. */
    @Test fun switchingPlayerKeepsEachKidsOwn() {
        seedThreePlayers()
        launchApp()
        compose.onNodeWithContentDescription("Switch to Mya").performClick()
        advance(300)
        compose.onNodeWithContentDescription("Mya is playing").assertExists()
        assertEquals(1, storage.loadCurrentPlayer())
        snap("players_switched")
        compose.onNodeWithText("My Snails").performClick()
        advance(500)
        compose.onNode(hasSetTextAction() and hasText("Mya")).assertExists()
        // Caleb's top hat is his: Mya hasn't reached length 100 yet.
        compose.onNodeWithContentDescription("Top hat, locked", substring = true).assertExists()
        val mya = Profile(storage, SystemCalendar)
        assertEquals(sirNoodles, mya.style)
        mya.recordGame(GameResult(40, 2, sirNoodles, 2000L), season = emptySet())
        assertEquals(Stats(40, 2, 1), storage.player(1).loadStats())
        assertEquals(Stats(bestScore = 120), storage.player(0).loadStats())
        assertEquals(style, storage.player(0).loadStyle())
    }

    /** Removing a kid who isn't playing takes away only their snail; their high scores stay. */
    @Test fun removingAPlayerWhoIsntPlaying() {
        seedThreePlayers()
        storage.addScore(ScoreEntry("Mya", 77, 1, sirNoodles, 3000L))
        Profile(storage, SystemCalendar).removePlayer(1)
        assertEquals(listOf(0, 2), storage.loadPlayers())
        assertEquals(0, storage.loadCurrentPlayer())
        assertEquals(emptyList<String>(), rawPrefs().all.keys.filter { it.startsWith("p1.") })
        assertEquals(listOf("Mya"), storage.loadScores().map { it.name })
        assertEquals(style, storage.player(0).loadStyle())
        launchApp()
        compose.onNodeWithContentDescription("Caleb is playing").assertExists()
        compose.onNodeWithContentDescription("Switch to Mya").assertDoesNotExist()
    }

    /** The kid playing removes themselves from My Snails: asked first, "Keep" backs out, then home with someone else playing. */
    @Test fun removingThePlayerWhoIsPlaying() {
        seedThreePlayers()
        launchApp()
        compose.onNodeWithContentDescription("Switch to Mya").performClick()
        advance(300)
        compose.onNodeWithText("My Snails").performClick()
        advance(500)
        scrollMySnailToBottom()
        compose.onNodeWithText("Remove player").performClick()
        advance(500)
        compose.onNodeWithText("Remove Mya's snail?").assertExists()
        snapDialog("players_remove_ask")
        compose.onNodeWithText("Keep").performClick()
        advance(500)
        compose.onNodeWithText("Remove Mya's snail?").assertDoesNotExist()
        assertEquals(listOf(0, 1, 2), storage.loadPlayers())

        compose.onNodeWithText("Remove player").performClick()
        advance(500)
        compose.onNodeWithText("Yes, remove Mya").performClick()
        advance(500)
        compose.onNodeWithText("Play!").assertExists()
        compose.onNodeWithContentDescription("Caleb is playing").assertExists()
        compose.onNodeWithContentDescription("Switch to Mya").assertDoesNotExist()
        assertEquals(listOf(0, 2), storage.loadPlayers())
        assertEquals(0, storage.loadCurrentPlayer())
        snap("players_removed")
    }

    @Config(qualifiers = NIGHT)
    @Test
    fun removingAPlayerNight() = removingThePlayerWhoIsPlaying()

    /**
     * The first kid (the old unprefixed keys) leaves: the others keep their own
     * totals, and a kid who later gets the first kid's place starts from zero,
     * not from the high-score list.
     */
    @Test fun removingTheFirstPlayerThenAddingANewOne() {
        seedOldScores()
        val me = Profile(storage, SystemCalendar)
        me.addPlayer(kotlin.random.Random(1))
        me.recordGame(GameResult(40, 2, style, 2000L), season = emptySet())
        me.removePlayer(0)
        assertEquals(listOf(1), storage.loadPlayers())
        listOf("style", "snail_name", "stats", "announced").forEach { assertEquals(false, rawPrefs().contains(it)) }
        assertEquals(Stats(40, 2, 1), storage.player(1).loadStats())
        assertEquals(10, storage.loadScores().size)

        me.addPlayer(kotlin.random.Random(2))
        assertEquals(listOf(1, 0), storage.loadPlayers())
        assertEquals(0, me.playerId)
        assertEquals(Stats(), me.stats)
        assertEquals(emptyList<Any>(), me.alreadyEarned)
        // The last kid can't be removed.
        me.removePlayer(1)
        me.removePlayer(0)
        assertEquals(listOf(0), storage.loadPlayers())
        launchApp()
        compose.onNodeWithText("+ Add player").assertExists()
        compose.onNodeWithText("already earned", substring = true).assertDoesNotExist()
        compose.onNodeWithText("My Snails").performClick()
        advance(500)
        compose.onNodeWithText("Remove player").assertDoesNotExist()
    }
}
