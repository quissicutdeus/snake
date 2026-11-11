package com.quissicutdeus.snake

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.quissicutdeus.snake.game.model.ScoreEntry
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.ui.GameOverScreen
import com.quissicutdeus.snake.ui.GameResult
import com.quissicutdeus.snake.ui.GameScreen
import com.quissicutdeus.snake.ui.HighScoresScreen
import com.quissicutdeus.snake.ui.Profile
import com.quissicutdeus.snake.ui.SniddleApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.annotation.Config

/** Little kid mode: switched on per kid in My Snake, gentler games, and a 🐣 by their high scores. */
class LittleKidModeTest : ScreenTest() {
    private val switch = "Little kid mode"

    /** Caleb (playing) and Bea, the youngest. */
    private fun seedTwoKids() {
        storage.newsSeen =
            com.quissicutdeus.snake.ui.WhatsNew
                .last()
                .number
        storage.player(0).apply {
            saveStyle(style)
            saveSnakeName("Caleb")
        }
        storage.addPlayer(sirNoodles)
        storage.player(1).saveSnakeName("Bea")
    }

    private fun switchScene(name: String) {
        seedTwoKids()
        storage.saveCurrentPlayer(1)
        launchApp()
        compose.onNodeWithText("My Snake").performClick()
        advance(500)
        compose.onNodeWithText(switch).assertIsOff()
        compose.onNodeWithText(switch).performClick()
        advance(300)
        compose.onNodeWithText(switch).assertIsOn()
        compose.onNodeWithText("Gentler snakes and more food").assertExists()
        snap(name)
        // Only Bea's: Caleb's games stay as they were.
        assertTrue(storage.player(1).loadLittleKid())
        assertFalse(storage.player(0).loadLittleKid())
    }

    @Test fun switchLandscape() = switchScene("little_kid_switch_landscape")

    @Config(qualifiers = PORTRAIT)
    @Test
    fun switchPortrait() = switchScene("little_kid_switch_portrait")

    @Test fun eachKidHasTheirOwnSwitchAndANewKidStartsWithItOff() {
        seedTwoKids()
        storage.player(1).saveLittleKid(true)
        val me = Profile(storage)
        assertFalse(me.littleKid)
        me.switchTo(1)
        assertTrue(me.littleKid)
        me.changeLittleKid(false)
        assertFalse(storage.player(1).loadLittleKid())
        me.changeLittleKid(true)
        // Removing Bea forgets it; whoever gets her place next starts with it off.
        me.removePlayer(1)
        assertFalse(rawPrefs().contains("p1.little_kid"))
        storage.addPlayer(style)
        assertFalse(storage.player(1).loadLittleKid())
    }

    /** Four hearts in the corner, and plenty of food. */
    @Test fun aLittleKidGame() {
        show { GameScreen(key = 1, style = sirNoodles, playerName = "Bea", onGameOver = {}, onQuit = {}, littleKid = true) }
        advance(1500)
        snap("little_kid_game")
    }

    /**
     * Android closes the app during Bea's paused little-kid game, and the
     * switch was turned off meanwhile: the game still comes back gentle
     * (four hearts), since it carries on as it was.
     */
    @Test fun aSavedLittleKidGameComesBackGentle() {
        seedTwoKids()
        storage.saveCurrentPlayer(1)
        storage.player(1).saveLittleKid(true)
        val restore = StateRestorationTester(compose)
        restore.setContent { SniddleApp(storage, playSound = {}) }
        advance(100)
        compose.onNodeWithText("Play!").performClick()
        advance(1000)
        compose.onNodeWithContentDescription("Pause").performClick()
        advance(100)
        storage.player(1).saveLittleKid(false)

        restore.emulateSavedInstanceStateRestore()
        advance(500)
        compose.onNodeWithText("Keep playing").performClick()
        advance(500)
        snap("little_kid_game_after_restore")
    }

    @Test fun aLittleKidHighScoreIsSavedWithItsChick() {
        show { GameOverScreen(GameResult(64, 1, sirNoodles, 3000L, littleKid = true), emptyList(), storage, "Bea", {}, {}, {}) }
        advance(500)
        compose.onNodeWithText("Bea").performClick()
        advance(100)
        assertEquals(listOf(true), storage.loadScores().map { it.littleKid })
    }

    @Test fun highScoresShowWhichGamesWereLittleKidMode() {
        val styles = listOf(style, sirNoodles) + (1..2).map { SnakeStyle.random(kotlin.random.Random(it)) }
        listOf(Triple("Caleb", 212, false), Triple("Bea", 180, true), Triple("Dad", 150, false), Triple("Bea", 99, true))
            .forEachIndexed { i, (n, s, little) -> storage.addScore(ScoreEntry(n, s, i, styles[i], 1000L + i, little)) }
        show { HighScoresScreen(storage, highlight = null, onPlay = {}, onBack = {}) }
        advance(800)
        compose.onAllNodesWithContentDescription("Little kid mode").assertCountEquals(2)
        snap("high_scores_little_kid")
    }
}
