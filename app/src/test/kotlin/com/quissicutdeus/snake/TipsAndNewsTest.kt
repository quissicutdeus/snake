package com.quissicutdeus.snake

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
import com.quissicutdeus.snake.game.model.Eyes
import com.quissicutdeus.snake.game.model.Hat
import com.quissicutdeus.snake.game.model.Holiday
import com.quissicutdeus.snake.game.model.Pattern
import com.quissicutdeus.snake.game.model.ScoreEntry
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.model.Stats
import com.quissicutdeus.snake.game.model.Unlocks
import com.quissicutdeus.snake.game.sim.Eater
import com.quissicutdeus.snake.ui.GameOverScreen
import com.quissicutdeus.snake.ui.GameResult
import com.quissicutdeus.snake.ui.GameScreen
import com.quissicutdeus.snake.ui.HighScoresScreen
import com.quissicutdeus.snake.ui.MySnakeScreen
import com.quissicutdeus.snake.ui.Profile
import com.quissicutdeus.snake.ui.Settings
import com.quissicutdeus.snake.ui.SniddleApp
import com.quissicutdeus.snake.ui.SniddleTheme
import com.quissicutdeus.snake.ui.TitleScreen
import com.quissicutdeus.snake.ui.WhatsNew
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

/** The "What's new" card after an update, and the tips on a kid's first game. */
class TipsAndNewsTest : ScreenTest() {
    // "What's new": once after an update, never on a fresh install.

    private fun whatsNewScene(name: String) {
        seedOldScores(newsSeen = false)
        val launch = launchApp()
        compose.onNodeWithText("New!").assertExists()
        WhatsNew.forEach { compose.onNodeWithText(it.line).assertExists() }
        // One card at a time: the earned looks wait until the news is tapped away.
        compose.onNodeWithText(alreadyEarned).assertDoesNotExist()
        snap(name)
        assertEquals(WhatsNew.last().number, storage.newsSeen)
        compose.onNodeWithText("New!").performClick()
        advance(300)
        compose.onNodeWithText("New!").assertDoesNotExist()
        compose.onNodeWithText(alreadyEarned).assertExists()
        snap("${name}_then_earned")
        launch.intValue++
        advance(300)
        compose.onNodeWithText("Play!").assertExists()
        compose.onNodeWithText("New!").assertDoesNotExist()
    }

    @Test fun whatsNewLandscape() = whatsNewScene("whats_new_landscape")

    @Config(qualifiers = PORTRAIT)
    @Test
    fun whatsNewPortrait() = whatsNewScene("whats_new_portrait")

    @Test fun whatsNewGoesAwayOnPlay() {
        seedOldScores(newsSeen = false)
        val launch = launchApp()
        compose.onNodeWithText("New!").assertExists()
        compose.onNodeWithText("Play!").performClick()
        advance(300)
        launch.intValue++
        advance(300)
        compose.onNodeWithText("New!").assertDoesNotExist()
    }

    @Test fun aFreshInstallNeverSeesOldNews() {
        val launch = launchApp()
        compose.onNodeWithText("Play!").assertExists()
        compose.onNodeWithText("New!").assertDoesNotExist()
        assertEquals(WhatsNew.last().number, storage.newsSeen)
        // After the first game the device has history, but the news was already "seen".
        storage.addScore(ScoreEntry("Caleb", 30, 0, style, 1000L))
        launch.intValue++
        advance(300)
        compose.onNodeWithText("New!").assertDoesNotExist()
    }

    // ---- First-game tips ----

    private fun shows(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    /** Steers round in a slow circle until [text] shows (or [maxMs] passes). */
    private fun steerUntil(
        text: String,
        maxMs: Long,
    ) {
        compose.onRoot().performTouchInput { down(center) }
        var ms = 0L
        while (!shows(text) && ms < maxMs) {
            val angle = ms / 1500.0
            compose.onRoot().performTouchInput {
                moveTo(center + Offset(cos(angle).toFloat() * 150f, sin(angle).toFloat() * 150f))
            }
            advance(160)
            ms += 160
        }
        compose.onRoot().performTouchInput { up() }
    }

    private fun tipsScene(name: String) {
        var done = 0
        show {
            GameScreen(
                key = 1,
                style = style,
                playerName = "You",
                onGameOver = {},
                onQuit = {},
                showTips = true,
                onTipsDone = { done++ },
            )
        }
        advance(600)
        compose.onNodeWithText("Drag anywhere to steer").assertExists()
        snap("${name}_steer")
        // Waiting doesn't move it on: the kid has to steer.
        advance(3000)
        compose.onNodeWithText("Drag anywhere to steer").assertExists()
        steerUntil("Eat dots to grow", maxMs = 4000)
        advance(600)
        compose.onNodeWithText("Drag anywhere to steer").assertDoesNotExist()
        compose.onNodeWithText("Eat dots to grow").assertExists()
        snap("${name}_eat")
        steerUntil("Bigger ones bite!", maxMs = 30_000)
        advance(600)
        compose.onNodeWithText("Eat dots to grow").assertDoesNotExist()
        compose.onNodeWithText("Gobble smaller ones!").assertExists()
        snap("${name}_danger")
        assertEquals(0, done)
        advance(6500)
        compose.onNodeWithText("Bigger ones bite!").assertDoesNotExist()
        assertEquals(1, done)
    }

    @Test fun firstGameTipsLandscape() = tipsScene("tips_landscape")

    @Config(qualifiers = PORTRAIT)
    @Test
    fun firstGameTipsPortrait() = tipsScene("tips_portrait")

    /** A finger landing on the tip card still steers: the card lets touches through. */
    @Test fun theTipCardDoesNotBlockSteering() {
        show { GameScreen(key = 1, style = style, playerName = "You", onGameOver = {}, onQuit = {}, showTips = true) }
        advance(2500)
        val card = compose.onNodeWithText("Drag anywhere to steer")
        card.performTouchInput { down(center) }
        advance(100)
        card.performTouchInput { moveBy(Offset(-150f, 0f)) }
        advance(2500)
        // Steering moved it on (it may even be past the eating tip already, if dots were close by).
        compose.onNodeWithText("Drag anywhere to steer").assertDoesNotExist()
    }

    @Test fun aFreshInstallGetsTipsUntilTheFirstGameIsDone() {
        val launch = launchApp()
        playFromTitle()
        compose.onNodeWithText("Drag anywhere to steer").assertExists()
        Profile(storage).recordGame(GameResult(20, 0, style, 1000L), season = emptySet())
        launch.intValue++
        advance(300)
        playFromTitle()
        compose.onNodeWithText("Drag anywhere to steer").assertDoesNotExist()
    }

    @Test fun kidsWhoHavePlayedBeforeGetNoTips() {
        seedOldScores()
        launchApp()
        playFromTitle()
        compose.onNodeWithText("Drag anywhere to steer").assertDoesNotExist()
    }

    /** A new kid on a well-played phone still gets the tips on their first game. */
    @Test fun aNewPlayerGetsTipsToo() {
        seedOldScores()
        launchApp()
        compose.onNodeWithText("Add player", substring = true).performClick()
        advance(800)
        compose.onNodeWithText("Done").performClick()
        advance(800)
        playFromTitle()
        compose.onNodeWithText("Drag anywhere to steer").assertExists()
    }
}
