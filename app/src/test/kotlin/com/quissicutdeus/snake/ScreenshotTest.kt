package com.quissicutdeus.snake

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.quissicutdeus.snake.game.model.Eyes
import com.quissicutdeus.snake.game.model.Hat
import com.quissicutdeus.snake.game.model.Pattern
import com.quissicutdeus.snake.game.model.ScoreEntry
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.ui.GameOverScreen
import com.quissicutdeus.snake.ui.GameResult
import com.quissicutdeus.snake.ui.GameScreen
import com.quissicutdeus.snake.ui.HighScoresScreen
import com.quissicutdeus.snake.ui.MySnakeScreen
import com.quissicutdeus.snake.ui.Settings
import com.quissicutdeus.snake.ui.SniddleApp
import com.quissicutdeus.snake.ui.SniddleTheme
import com.quissicutdeus.snake.ui.TitleScreen
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

/**
 * Renders each screen with Robolectric's native graphics and saves PNGs to
 * app/build/screenshots, so layouts can be checked without a phone. Also a
 * smoke test: every screen must compose and run its frame loop without crashing.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w800dp-h360dp-land-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val style = SnakeStyle(color1 = 6, color2 = 2, pattern = Pattern.STRIPES, eyes = Eyes.GOOGLY, hat = Hat.CROWN)
    private lateinit var storage: Storage

    @Before fun setUp() {
        storage = Storage(RuntimeEnvironment.getApplication())
        compose.mainClock.autoAdvance = false
    }

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { SniddleTheme(content) }
    }

    private fun advance(ms: Long) {
        // Step frame by frame so the game loops really run.
        repeat((ms / 16).toInt()) { compose.mainClock.advanceTimeByFrame() }
    }

    private fun snap(name: String) = saveScreenshot(compose.onRoot().captureToImage().asAndroidBitmap(), name)

    // ---- Scenes (each rendered in several screen shapes below) ----

    private fun titleScene(
        name: String,
        settings: Settings = Settings(),
    ) {
        show { TitleScreen(style, {}, {}, {}, settings = settings, onSettingsChange = {}) }
        advance(1500)
        snap(name)
    }

    private fun playScene(name: String) {
        show { GameScreen(key = 1, style = style, onGameOver = {}, onQuit = {}) }
        advance(300)
        // Hold a finger up and to the right of the snake for a while, then down-left.
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
                onGameOver = {},
                onQuit = {},
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
        assertEquals(Settings(soundOn = false, buzzOn = true), storage.loadSettings())
        snap(name)
    }

    /**
     * Android closes the app while a game is paused in the background: on
     * return the kid is back in the game, still paused, not on the title screen.
     */
    @Test fun aPausedGameSurvivesTheAppBeingClosed() {
        val restore = StateRestorationTester(compose)
        restore.setContent { SniddleApp(storage, playSound = {}) }
        advance(100)
        compose.onNodeWithText("Play!").performClick()
        advance(1000)
        compose.onNodeWithContentDescription("Pause").performClick()
        advance(100)

        restore.emulateSavedInstanceStateRestore()
        advance(500)

        compose.onNodeWithText("Paused").assertExists()
        compose.onNodeWithText("Play!").assertDoesNotExist()
        snap("paused_after_restore")
        compose.onNodeWithText("Keep playing").performClick()
        advance(500)
        compose.onNodeWithText("Paused").assertDoesNotExist()
    }

    private fun mySnakeScene(
        name: String,
        look: SnakeStyle = style,
    ) {
        show { MySnakeScreen(look, {}, {}) }
        advance(800)
        snap(name)
    }

    private fun gameOverScene(name: String) {
        storage.saveNames(listOf("Caleb", "Mya", "Dad"))
        show { GameOverScreen(GameResult(87, 3, style, 1000L), storage, {}, {}, {}) }
        advance(500)
        snap(name)
    }

    private fun highScoresScene(name: String) {
        val styles = listOf(style) + (1..3).map { SnakeStyle.random(kotlin.random.Random(it)) }
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

    @Test fun mySnakeLandscape() = mySnakeScene("my_snake_landscape")

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
    fun mySnakePortrait() =
        mySnakeScene("my_snake_portrait", style.copy(pattern = Pattern.RAINBOW, hat = Hat.PARTY, eyes = Eyes.SUNGLASSES))

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
    fun mySnakeTablet() = mySnakeScene("my_snake_tablet")

    // ---- Oldest supported Android (minSdk), Galaxy Note 20 size ----

    @Config(sdk = [30], qualifiers = NOTE_20)
    @Test
    fun titleOldestAndroid() = titleScene("title_android11")

    @Config(sdk = [30], qualifiers = NOTE_20)
    @Test
    fun playingOldestAndroid() = playScene("game_android11")

    private companion object {
        const val PORTRAIT = "w360dp-h780dp-port-xxhdpi"
        const val TABLET = "w1280dp-h800dp-land-xhdpi"
        const val NOTE_20 = "w915dp-h412dp-land-xxhdpi"
    }
}

/** Saves [bitmap] as app/build/screenshots/<name>.png. */
internal fun saveScreenshot(
    bitmap: Bitmap,
    name: String,
) {
    val dir = File(System.getProperty("screenshots.dir") ?: "build/screenshots")
    dir.mkdirs()
    File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
