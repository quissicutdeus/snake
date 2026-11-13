package com.quissicutdeus.sniddle

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
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
import org.junit.Assert.assertTrue
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

/**
 * Renders each screen with Robolectric's native graphics and saves PNGs to
 * app/build/screenshots, so layouts can be checked without a phone. Also a
 * smoke test: every screen must compose and run its frame loop without crashing.
 */
class ScreenshotTest : ScreenTest() {
    // ---- Scenes (each rendered in several screen shapes below) ----

    private fun titleScene(
        name: String,
        settings: Settings = Settings(),
    ) {
        show { TitleScreen(style, {}, {}, {}, settings, {}, today.arena, today.season) }
        advance(1500)
        snap(name)
    }

    private fun playScene(name: String) {
        show { GameScreen(key = 1, style = style, playerName = "Caleb", onGameOver = {}, onQuit = {}, options = today) }
        advance(300)
        // Hold a finger up and to the right of the snail for a while, then down-left.
        compose.onRoot().performTouchInput { down(Offset(width * 0.8f, height * 0.25f)) }
        advance(2500)
        compose.onRoot().performTouchInput { moveTo(Offset(width * 0.2f, height * 0.7f)) }
        advance(1500)
        snap(name)
        compose.onRoot().performTouchInput { up() }
    }

    /** Pauses a game and flips the sound switch there; the change must stick and be saved. */
    private fun pausedScene(name: String) {
        show {
            var settings by remember { mutableStateOf(storage.loadSettings()) }
            GameScreen(
                key = 1,
                style = style,
                playerName = "You",
                onGameOver = {},
                onQuit = {},
                options = today,
                settings = settings,
                onSettingsChange = {
                    settings = it
                    storage.saveSettings(it)
                },
            )
        }
        advance(300)
        compose.onNodeWithContentDescription("Pause").performClick()
        advance(100)
        compose.onNodeWithContentDescription("Sound on. Tap to turn it off.").performClick()
        advance(100)
        compose.onNodeWithContentDescription("Sound off. Tap to turn it on.").assertExists()
        // Sound off silences the music, so the music button shows off too.
        compose.onNodeWithContentDescription("Music off. Tap to turn it on.").assertExists()
        assertEquals(Settings(soundOn = false, buzzOn = true), storage.loadSettings())
        snap(name)
        // Turning the music back on brings the sound back with it.
        compose.onNodeWithContentDescription("Music off. Tap to turn it on.").performClick()
        advance(100)
        compose.onNodeWithContentDescription("Sound on. Tap to turn it off.").assertExists()
        assertEquals(Settings(), storage.loadSettings())
    }

    /**
     * Android closes the app while a game is paused in the background: on
     * return the kid is back in the game, still paused, not on the title screen.
     */
    @Test fun aPausedGameSurvivesTheAppBeingClosed() {
        val app = SavedAndRestored()
        compose.setContent { Restorable(app) { SniddleApp(storage, SystemCalendar, NoSound, NoBuzz) } }
        advance(100)
        compose.onNodeWithText("Play!").performClick()
        advance(1000)
        compose.onNodeWithContentDescription("Pause").performClick()
        advance(100)

        app.closeAndReopen { advance(250) }
        assertTrue("the screen really was saved", app.saved.orEmpty().isNotEmpty())

        compose.onNodeWithText("Paused").assertExists()
        compose.onNodeWithText("Play!").assertDoesNotExist()
        snap("paused_after_restore")
        compose.onNodeWithText("Keep playing").performClick()
        advance(500)
        compose.onNodeWithText("Paused").assertDoesNotExist()
    }

    // ---- Looks earned before unlocks existed: cheered once on the title screen ----

    private fun alreadyEarnedScene(name: String) {
        seedOldScores()
        val launch = launchApp()
        compose.onNodeWithText(alreadyEarned).assertExists()
        snap(name)
        assertEquals(setOf("Hat.TOP_HAT", "Pattern.STARS"), storage.player(0).loadAnnounced())
        compose.onNodeWithText(alreadyEarned).performClick()
        advance(100)
        compose.onNodeWithText(alreadyEarned).assertDoesNotExist()
        launch.intValue++
        advance(300)
        compose.onNodeWithText("Play!").assertExists()
        compose.onNodeWithText(alreadyEarned).assertDoesNotExist()
    }

    @Test fun alreadyEarnedLandscape() = alreadyEarnedScene("already_earned_landscape")

    @Config(qualifiers = PORTRAIT)
    @Test
    fun alreadyEarnedPortrait() = alreadyEarnedScene("already_earned_portrait")

    @Test fun alreadyEarnedGoesAwayOnPlayAndStaysAway() {
        seedOldScores()
        val launch = launchApp()
        compose.onNodeWithText(alreadyEarned).assertExists()
        compose.onNodeWithText("Play!").performClick()
        advance(300)
        launch.intValue++
        advance(300)
        compose.onNodeWithText("Play!").assertExists()
        compose.onNodeWithText(alreadyEarned).assertDoesNotExist()
    }

    @Test fun aFreshInstallHasNothingToAnnounce() {
        launchApp()
        compose.onNodeWithText("Play!").assertExists()
        compose.onNodeWithText("already earned", substring = true).assertDoesNotExist()
        assertEquals(emptySet<String>(), storage.player(0).loadAnnounced())
    }

    @Test fun looksCheeredAtGameOverAreNotAnnouncedAgain() {
        storage.addScore(ScoreEntry("Caleb", 90, 0, style, 1000L))
        val unlocked = Profile(storage, SystemCalendar).recordGame(GameResult(120, 0, style, 2000L), season = emptySet())
        assertEquals(listOf<Any>(Hat.TOP_HAT), unlocked.map { it.item })
        assertEquals(emptyList<Any>(), Profile(storage, SystemCalendar).alreadyEarned.map { it.item })
        launchApp()
        compose.onNodeWithText("already earned", substring = true).assertDoesNotExist()
    }

    @Test fun musicPlaysOnlyWhileTheGameRuns() {
        val calls = mutableListOf<Boolean>()
        show { GameScreen(key = 1, style = style, playerName = "You", onGameOver = {}, onQuit = {}, today, onMusic = { calls += it }) }
        advance(300)
        assertEquals(listOf(true), calls)
        compose.onNodeWithContentDescription("Pause").performClick()
        advance(100)
        assertEquals(listOf(true, false), calls)
        compose.onNodeWithText("Keep playing").performClick()
        advance(100)
        assertEquals(listOf(true, false, true), calls)
    }

    private fun mySnailScene(
        name: String,
        look: SnailStyle = style,
    ) {
        show {
            MySnailScreen(
                look,
                {},
                {},
                name = "Caleb",
                onNameChange = {},
                knownNames = listOf("Caleb", "Mya", "Dad"),
                stats = Stats(bestScore = 120),
            )
        }
        advance(800)
        snap(name)
    }

    /** Tapping a locked look wiggles it and says what's left, but never picks it. */
    @Test fun tappingALockedLookSaysWhatsLeft() {
        val picked = mutableListOf<SnailStyle>()
        show {
            MySnailScreen(
                style,
                { picked += it },
                {},
                name = "Caleb",
                onNameChange = {},
                knownNames = listOf("Caleb"),
                stats = Stats(bestScore = 120, snailsEaten = 8, gamesPlayed = 7),
            )
        }
        advance(800)
        val hearts = compose.onNodeWithContentDescription("Heart eyes, locked: Eat 10 snails. Eat 2 more snails!")
        hearts.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        compose.onNodeWithText("Eat 2 more snails!").assertDoesNotExist()

        // Drag the choices panel up to the eyes by hand: performScrollTo waits for a
        // scroll animation that never ends while the test drives the clock.
        compose.onRoot().performTouchInput { swipe(Offset(width * 0.75f, bottom - 20f), Offset(width * 0.75f, top + 20f), 300) }
        advance(1500)
        hearts.performClick()
        advance(500)
        compose.onNodeWithText("Eat 2 more snails!").assertExists()
        compose.onNodeWithText("Eat 10 snails").assertDoesNotExist()
        assertEquals(emptyList<SnailStyle>(), picked)
        snap("my_snail_locked_hint")

        advance(2500)
        compose.onNodeWithText("Eat 2 more snails!").assertDoesNotExist()
        compose.onNodeWithText("Eat 10 snails").assertExists()
        assertEquals(emptyList<SnailStyle>(), picked)
    }

    /** A game in November earns the turkey hat for good; Santa waits for December, with no progress bar. */
    @Test fun holidayLooksComeFromPlayingOnTheDay() {
        val profile = Profile(storage, SystemCalendar)
        val unlocked = profile.recordGame(GameResult(20, 0, style, 2000L), season = setOf(Holiday.THANKSGIVING))
        assertEquals(listOf<Any>(Hat.TURKEY), unlocked.map { it.item })
        show {
            MySnailScreen(
                style,
                {},
                {},
                name = "Caleb",
                onNameChange = {},
                knownNames = listOf("Caleb"),
                stats = profile.stats,
            )
        }
        advance(800)
        compose.onNodeWithText("Turkey hat").assertExists()
        compose.onNodeWithContentDescription("Santa hat, locked: Play in December. Ho ho ho! Back in December!").assertExists()
        compose.onNodeWithText("0/1").assertDoesNotExist()
        repeat(2) {
            compose.onRoot().performTouchInput { swipe(Offset(width * 0.75f, bottom - 20f), Offset(width * 0.75f, top + 20f), 300) }
            advance(1500)
        }
        snap("my_snail_holidays")
    }

    private fun gameOverScene(name: String) {
        storage.saveNames(listOf("Caleb", "Mya", "Dad"))
        show {
            GameOverScreen(
                GameResult(87, 3, style, 1000L, Eater("Sir Noodles", sirNoodles), biggestSeconds = 42),
                listOfNotNull(Unlocks.ruleFor(Hat.TOP_HAT)),
                storage,
                "Caleb",
                {},
                {},
                {},
            )
        }
        advance(500)
        snap(name)
    }

    private fun highScoresScene(name: String) {
        val styles = listOf(style) + (1..3).map { SnailStyle.random(kotlin.random.Random(it)) }
        listOf("Caleb" to 212, "Mya" to 180, "Dad" to 150, "Caleb" to 99, "Mya" to 64).forEachIndexed { i, (n, s) ->
            storage.addScore(ScoreEntry(n, s, i, styles[i % styles.size], 1000L + i))
        }
        show { HighScoresScreen(storage, highlight = 1001L, onPlay = {}, onBack = {}) }
        advance(800)
        snap(name)
    }

    // ---- Phone, landscape (the class default) ----

    @Test fun titleLandscape() = titleScene("title_landscape")

    @Test fun playingLandscape() = playScene("game_landscape")

    @Test fun pausedLandscape() = pausedScene("paused_landscape")

    @Test fun mySnailLandscape() = mySnailScene("my_snail_landscape")

    @Test fun gameOverLandscape() = gameOverScene("game_over_landscape")

    @Test fun highScoresLandscape() = highScoresScene("high_scores_landscape")

    // ---- Phone, portrait ----

    @Config(qualifiers = PORTRAIT)
    @Test
    fun titlePortrait() = titleScene("title_portrait", Settings(soundOn = false, buzzOn = false))

    @Config(qualifiers = PORTRAIT)
    @Test
    fun playingPortrait() = playScene("game_portrait")

    @Config(qualifiers = PORTRAIT)
    @Test
    fun pausedPortrait() = pausedScene("paused_portrait")

    @Config(qualifiers = PORTRAIT)
    @Test
    fun mySnailPortrait() =
        mySnailScene("my_snail_portrait", style.copy(pattern = Pattern.RAINBOW, hat = Hat.PARTY, eyes = Eyes.SUNGLASSES))

    @Config(qualifiers = PORTRAIT)
    @Test
    fun gameOverPortrait() = gameOverScene("game_over_portrait")

    @Config(qualifiers = PORTRAIT)
    @Test
    fun highScoresPortrait() = highScoresScene("high_scores_portrait")

    // ---- Tablet (Pixel Tablet size), landscape ----

    @Config(qualifiers = TABLET)
    @Test
    fun titleTablet() = titleScene("title_tablet")

    @Config(qualifiers = TABLET)
    @Test
    fun playingTablet() = playScene("game_tablet")

    @Config(qualifiers = TABLET)
    @Test
    fun mySnailTablet() = mySnailScene("my_snail_tablet")

    // ---- Oldest supported Android (minSdk), Galaxy Note 20 size ----

    @Config(sdk = [30], qualifiers = NOTE_20)
    @Test
    fun titleOldestAndroid() = titleScene("title_android11")

    @Config(sdk = [30], qualifiers = NOTE_20)
    @Test
    fun playingOldestAndroid() = playScene("game_android11")
}
