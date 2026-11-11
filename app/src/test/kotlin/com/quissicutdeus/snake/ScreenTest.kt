package com.quissicutdeus.snake

import android.content.Context
import android.content.SharedPreferences
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

/** Screen shapes: a phone held upright, a tablet, and a Galaxy Note 20 sized phone. */
internal const val PORTRAIT = "w360dp-h780dp-port-xxhdpi"
internal const val TABLET = "w1280dp-h800dp-land-xhdpi"
internal const val NOTE_20 = "w915dp-h412dp-land-xxhdpi"

/**
 * What every screenshot test shares: Robolectric's native graphics, a phone in
 * landscape by default, the app's storage, and helpers to show screens, run
 * the clock frame by frame and save PNGs to app/build/screenshots.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w800dp-h360dp-land-xxhdpi")
abstract class ScreenTest {
    @get:Rule val compose = createComposeRule()

    protected val style = SnakeStyle(color1 = 6, color2 = 2, pattern = Pattern.STRIPES, eyes = Eyes.GOOGLY, hat = Hat.CROWN)
    protected val sirNoodles = SnakeStyle(color1 = 8, color2 = 2, pattern = Pattern.DOTS, eyes = Eyes.SUNGLASSES, hat = Hat.PARTY)
    protected lateinit var storage: Storage

    @Before fun setUp() {
        storage = Storage(RuntimeEnvironment.getApplication())
        compose.mainClock.autoAdvance = false
    }

    protected fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { SniddleTheme(content) }
    }

    protected fun advance(ms: Long) {
        // Step frame by frame so the game loops really run.
        repeat((ms / 16).toInt()) { compose.mainClock.advanceTimeByFrame() }
    }

    protected fun snap(name: String) = saveScreenshot(compose.onRoot().captureToImage().asAndroidBitmap(), name)

    protected val alreadyEarned = "You've already earned: Top hat and Stars!"

    /**
     * Ten old high scores, one past length 100: enough for the top hat and stars, never cheered.
     * With [newsSeen], the device has also already seen all the "What's new" lines.
     */
    protected fun seedOldScores(newsSeen: Boolean = true) {
        if (newsSeen) storage.newsSeen = WhatsNew.last().number
        (1..10).forEach { i -> storage.addScore(ScoreEntry("Caleb", if (i == 1) 120 else 20 + i, 0, style, 1000L + i)) }
    }

    /** The whole app; bumping the returned counter starts it afresh (like the next launch). */
    protected fun launchApp(): androidx.compose.runtime.MutableIntState {
        val launch = androidx.compose.runtime.mutableIntStateOf(0)
        compose.setContent { androidx.compose.runtime.key(launch.intValue) { SniddleApp(storage, playSound = {}) } }
        advance(800)
        return launch
    }

    protected fun playFromTitle() {
        compose.onNodeWithText("Play!").performClick()
        advance(1000)
    }

    /** The saved data straight from the phone, as the version before players wrote it. */
    protected fun rawPrefs(): SharedPreferences =
        RuntimeEnvironment.getApplication().getSharedPreferences("snake_arena", Context.MODE_PRIVATE)

    /** Scrolls My Snake's choices to the bottom by hand (see [tappingALockedLookSaysWhatsLeft]). */
    protected fun scrollMySnakeToBottom() {
        repeat(4) {
            compose.onRoot().performTouchInput { swipe(Offset(width * 0.75f, bottom - 20f), Offset(width * 0.75f, top + 20f), 300) }
            advance(1500)
        }
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
