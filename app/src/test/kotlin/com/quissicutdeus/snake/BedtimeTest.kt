package com.quissicutdeus.snake

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.quissicutdeus.snake.ui.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The grown-ups' bedtime timer: the gate, the setting, the heads-up and the rest screen. */
class BedtimeTest : ScreenTest() {
    private val rest = "Time to rest, snake! 😴"

    /** A [minutes] limit, with [playedMs] already played today. */
    private fun seedBedtime(
        minutes: Int,
        playedMs: Long,
    ) {
        storage.saveSettings(Settings(bedtimeMinutes = minutes))
        storage.playDay = PlayDay(LocalDate.now().toEpochDay(), playedMs).encode()
    }

    /** The question on screen, e.g. "What is 14 × 6?". */
    private fun question(): String =
        compose
            .onNodeWithText("What is", substring = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)!!
            .first()
            .text

    private fun answer(text: String) {
        compose.onNode(hasSetTextAction()).performTextInput(text)
        advance(200)
        compose.onNodeWithText("OK").performClick()
        advance(500)
    }

    private fun rightAnswer(): String =
        Regex("""(\d+) × (\d+)""").find(question())!!.destructured.let { (a, b) -> (a.toInt() * b.toInt()).toString() }

    private fun openGrownUps() {
        compose.onNodeWithContentDescription("Grown-ups: bedtime timer").performClick()
        advance(500)
    }

    @Test fun offByDefault() {
        assertEquals(0, storage.loadSettings().bedtimeMinutes)
        storage.playDay = PlayDay(LocalDate.now().toEpochDay(), 5 * 60 * 60_000L).encode()
        launchApp()
        playFromTitle()
        compose.onNodeWithContentDescription("Pause").assertExists()
        compose.onNodeWithText(rest).assertDoesNotExist()
    }

    @Test fun aGrownUpSetsTheTimer() {
        seedOldScores()
        launchApp()
        openGrownUps()
        compose.onNodeWithText("Grown-ups only").assertExists()
        snap("bedtime_gate")
        answer(rightAnswer())
        compose.onNodeWithText("Bedtime timer").assertExists()
        compose.onNodeWithText("30 min").performClick()
        advance(300)
        assertEquals(30, storage.loadSettings().bedtimeMinutes)
        snap("bedtime_panel")
        compose.onNodeWithText("Done").performClick()
        advance(300)
        compose.onNodeWithText("Play!").assertExists()
    }

    @Config(qualifiers = PORTRAIT)
    @Test
    fun theGrownUpPanelPortrait() {
        seedBedtime(45, 12 * 60_000L)
        launchApp()
        openGrownUps()
        answer(rightAnswer())
        compose.onNodeWithText("Played today: 12 min (33 min left)").assertExists()
        snap("bedtime_panel_portrait")
    }

    @Test fun aWrongAnswerDoesntGetPast() {
        launchApp()
        openGrownUps()
        val first = question()
        answer("7")
        compose.onNodeWithText("Not quite. Try this one.").assertExists()
        compose.onNodeWithText("Bedtime timer").assertDoesNotExist()
        compose.onNodeWithText("30 min").assertDoesNotExist()
        assertNotEquals(first, question())
        snap("bedtime_gate_wrong")
        compose.onNodeWithText("Cancel").performClick()
        advance(300)
        compose.onNodeWithText("Grown-ups only").assertDoesNotExist()
        assertEquals(0, storage.loadSettings().bedtimeMinutes)
    }

    @Test fun aHeadsUpTwoMinutesBefore() {
        seedBedtime(15, 13 * 60_000L - 1_500L)
        launchApp()
        playFromTitle()
        compose.onNodeWithText("2 minutes left!").assertDoesNotExist()
        advance(1000)
        compose.onNodeWithText("2 minutes left!").assertExists()
        snap("bedtime_heads_up")
        advance(6000)
        compose.onNodeWithText("2 minutes left!").assertDoesNotExist()
    }

    /** Time runs out mid-game: the game stops at a calm moment and the rest screen shows the sleeping snake. */
    private fun restScene(name: String) {
        storage.player(0).saveSnakeName("Caleb")
        seedBedtime(15, 15 * 60_000L - 500L)
        launchApp()
        playFromTitle()
        var waited = 0
        while (waited < 70_000 && runCatching { compose.onNodeWithText(rest).assertExists() }.isFailure) {
            advance(500)
            waited += 500
        }
        compose.onNodeWithText(rest).assertExists()
        compose.onNodeWithText("What a game", substring = true).assertExists()
        assertEquals(1, storage.player(0).loadStats().gamesPlayed)
        assertTrue(storage.loadScores().any { it.name == "Caleb" })
        snap(name)
        // Play isn't there, and tapping around doesn't start a game.
        compose.onNodeWithText("Play!").assertDoesNotExist()
    }

    @Test fun restLandscape() = restScene("bedtime_rest_landscape")

    @Config(qualifiers = PORTRAIT)
    @Test
    fun restPortrait() = restScene("bedtime_rest_portrait")

    @Config(qualifiers = TABLET)
    @Test
    fun restTablet() = restScene("bedtime_rest_tablet")

    @Test fun timeUpStaysUpAfterARestartAndAGrownUpCanGiveMore() {
        seedBedtime(15, 16 * 60_000L)
        val launch = launchApp()
        compose.onNodeWithText(rest).assertExists()
        launch.intValue++
        advance(800)
        compose.onNodeWithText(rest).assertExists()
        compose.onNodeWithText("Grown-ups: 10 more minutes").performClick()
        advance(500)
        answer("1")
        compose.onNodeWithText(rest).assertExists()
        answer(rightAnswer())
        compose.onNodeWithText(rest).assertDoesNotExist()
        playFromTitle()
        compose.onNodeWithContentDescription("Pause").assertExists()
    }
}
