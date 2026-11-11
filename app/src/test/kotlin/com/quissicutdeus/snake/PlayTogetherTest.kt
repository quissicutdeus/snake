package com.quissicutdeus.snake

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.quissicutdeus.snake.game.model.Eyes
import com.quissicutdeus.snake.game.model.Hat
import com.quissicutdeus.snake.game.model.Pattern
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.model.Stats
import com.quissicutdeus.snake.game.model.Unlocks
import com.quissicutdeus.snake.game.sim.GameSim
import com.quissicutdeus.snake.ui.GUEST_ID
import com.quissicutdeus.snake.ui.KidResult
import com.quissicutdeus.snake.ui.PlayerCard
import com.quissicutdeus.snake.ui.Profile
import com.quissicutdeus.snake.ui.SniddleApp
import com.quissicutdeus.snake.ui.TogetherOverScreen
import com.quissicutdeus.snake.ui.TogetherResult
import com.quissicutdeus.snake.ui.TwoPlayerScreen
import com.quissicutdeus.snake.ui.WhatsNew
import com.quissicutdeus.snake.ui.recordTogether
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.annotation.Config
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Two kids on one screen: the button, picking who plays, both halves steering, comebacks and the end screen. */
@Config(qualifiers = TABLET)
class PlayTogetherTest : ScreenTest() {
    private val mya = SnakeStyle(color1 = 10, color2 = 4, pattern = Pattern.DOTS, eyes = Eyes.HEARTS, hat = Hat.BOW)
    private val caleb = PlayerCard(0, "Caleb", style)
    private val myaCard = PlayerCard(1, "Mya", mya)

    /** Caleb (playing), Mya and a third kid who hasn't named their snake. */
    private fun seedKids(three: Boolean = true) {
        storage.newsSeen = WhatsNew.last().number
        storage.player(0).apply {
            saveStyle(style)
            saveSnakeName("Caleb")
        }
        storage.addPlayer(mya)
        storage.player(1).saveSnakeName("Mya")
        if (three) storage.addPlayer(sirNoodles)
        storage.saveCurrentPlayer(0)
    }

    @Test fun titleOffersPlayTogetherOnATablet() {
        seedKids()
        launchApp()
        compose.onNodeWithText("Play together").assertExists()
        snap("together_title_tablet")
    }

    @Config(qualifiers = NOTE_20)
    @Test
    fun titleOffersPlayTogetherOnABigPhoneSideways() {
        seedKids()
        launchApp()
        compose.onNodeWithText("Play together").assertExists()
        snap("together_title_note20")
    }

    @Config(qualifiers = "w800dp-h360dp-land-xxhdpi")
    @Test
    fun noPlayTogetherOnASmallPhone() {
        seedKids()
        launchApp()
        compose.onNodeWithText("Play!").assertExists()
        compose.onNodeWithText("Play together").assertDoesNotExist()
    }

    @Config(qualifiers = PORTRAIT)
    @Test
    fun noPlayTogetherOnAPhoneHeldUpright() {
        seedKids()
        launchApp()
        compose.onNodeWithText("Play together").assertDoesNotExist()
    }

    /** The next kid is picked to start with; tapping another picks them, and Go! starts with both. */
    @Test fun pickingTheSecondKid() {
        seedKids()
        launchApp()
        compose.onNodeWithText("Play together").performClick()
        advance(500)
        compose.onNodeWithText("Who's playing with Caleb?").assertExists()
        compose.onNodeWithContentDescription("Play with Mya").assert(isSelected(true))
        compose.onNodeWithContentDescription("Play with Player 3").assert(isSelected(false))
        snap("together_pick_tablet")
        compose.onNodeWithContentDescription("Play with Player 3").performClick()
        advance(300)
        compose.onNodeWithText("Caleb on the left, Player 3 on the right").assertExists()
        compose.onNodeWithText("Go!").performClick()
        advance(1000)
        compose.onNodeWithContentDescription("Caleb's half", substring = true).assertExists()
        compose.onNodeWithContentDescription("Player 3's half", substring = true).assertExists()
        compose.onNodeWithContentDescription("Time left", substring = true).assertExists()
    }

    /** With only one kid on the phone: a "Player 2" with a random look, or add a new kid and come back here. */
    @Test fun withOneKidPlayer2OrAddAKid() {
        storage.newsSeen = WhatsNew.last().number
        storage.player(0).saveSnakeName("Caleb")
        launchApp()
        compose.onNodeWithText("Play together").performClick()
        advance(500)
        compose.onNodeWithContentDescription("Play with Player 2").assert(isSelected(true))
        snap("together_pick_one_kid")
        compose.onNodeWithContentDescription("Add a player").performClick()
        advance(500)
        compose.onNodeWithText("Type a name").assertExists()
        compose.onNodeWithText("Done").performClick()
        advance(500)
        // The new kid is the one picked on the title screen now, and Caleb plays with them.
        compose.onNodeWithText("Who's playing with Player 2?").assertExists()
        compose.onNodeWithContentDescription("Play with Caleb").assert(isSelected(true))
    }

    /** Back pauses a two-player game (both halves), like one-player; it doesn't drop the kids on the title screen. */
    @Test fun backPausesBothHalves() {
        seedKids()
        var back: OnBackPressedDispatcher? = null
        compose.setContent {
            back = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            SniddleApp(storage, playSound = {})
        }
        advance(800)
        compose.onNodeWithText("Play together").performClick()
        advance(500)
        compose.onNodeWithText("Go!").performClick()
        advance(500)
        compose.runOnUiThread { back!!.onBackPressed() }
        advance(300)
        compose.onNodeWithText("Paused").assertExists()
        compose.onNodeWithContentDescription("Caleb's half", substring = true).assertExists()
        compose.onNodeWithText("Keep playing").performClick()
        advance(300)
        compose.onNodeWithText("Paused").assertDoesNotExist()
    }

    private fun isSelected(selected: Boolean) = SemanticsMatcher.expectValue(SemanticsProperties.Selected, selected)

    /** Two fingers, one in each half, each steering its own kid's snake. */
    private fun bothHalvesSteer(name: String) {
        var sim: GameSim? = null
        // No computer snakes nearby to bump into, so each kid goes where their finger says.
        val quietArena: (GameSim) -> Unit = { s ->
            sim = s
            s.world.snakes.removeAll { !it.isPlayer }
        }
        show { TwoPlayerScreen(key = 1, kids = listOf(caleb, myaCard), onGameOver = {}, onQuit = {}, prepare = quietArena) }
        advance(300)
        val game = sim!!
        // Caleb drags left (west) in the left half; Mya drags up (north) in the right half.
        compose.onRoot().performTouchInput {
            down(0, Offset(width * 0.25f, height * 0.6f))
            down(1, Offset(width * 0.75f, height * 0.6f))
        }
        advance(100)
        compose.onRoot().performTouchInput {
            moveTo(0, Offset(width * 0.25f - 150f, height * 0.6f))
            moveTo(1, Offset(width * 0.75f, height * 0.6f - 150f))
        }
        advance(1500)
        assertHeading(PI.toFloat(), game.players[0].heading, "Caleb heads west")
        assertHeading((-PI / 2).toFloat(), game.players[1].heading, "Mya heads north")
        snap(name)

        // Caleb's finger drifts across into Mya's half, heading down: it still steers Caleb, and Mya keeps going north.
        compose.onRoot().performTouchInput { moveTo(0, Offset(width * 0.55f, height * 0.95f)) }
        advance(1500)
        assertTrue("Caleb turned south-east", sin(game.players[0].heading) > 0.3f && cos(game.players[0].heading) > 0f)
        assertHeading((-PI / 2).toFloat(), game.players[1].heading, "Mya still heads north")
        compose.onRoot().performTouchInput {
            up(0)
            up(1)
        }
    }

    private fun assertHeading(
        expected: Float,
        actual: Float,
        what: String,
    ) {
        val diff = abs(atan2(sin(actual - expected), cos(actual - expected)))
        assertTrue("$what (heading $actual)", diff < 0.3f)
    }

    @Test fun bothHalvesSteerTablet() = bothHalvesSteer("together_playing_tablet")

    @Config(qualifiers = NOTE_20)
    @Test
    fun bothHalvesSteerNote20() = bothHalvesSteer("together_playing_note20")

    /** A big snake gobbles Mya: "Oops!" in her half, and three seconds later she's back, small and blinking. */
    @Test fun aGobbledKidComesBack() {
        var sim: GameSim? = null
        show {
            TwoPlayerScreen(
                key = 1,
                kids = listOf(caleb, myaCard),
                onGameOver = {},
                onQuit = {},
                prepare = { s ->
                    sim = s
                    val kid = s.players[1]
                    kid.hearts = 1
                    // Only Noodle nearby, so it is Noodle who does it.
                    s.world.snakes.removeAll { !it.isPlayer }
                    // Right in front of her, coming her way.
                    s.addSnake(
                        "Noodle",
                        sirNoodles,
                        isPlayer = false,
                        x = kid.x,
                        y = kid.y - 140f,
                        heading = (PI / 2).toFloat(),
                        length = 700f,
                    )
                },
            )
        }
        val game = sim!!
        val kid = game.players[1]
        var steps = 0
        while (kid.alive && steps++ < 120) advance(16)
        assertFalse("Mya was gobbled", kid.alive)
        advance(1300)
        compose.onRoot().assertExists()
        snap("together_oops_tablet")
        assertEquals(
            "Noodle",
            game.together!!
                .kids[1]
                .gobbledBy
                ?.name,
        )
        advance(3000)
        assertTrue("Mya is back", kid.alive)
        assertTrue(kid.invulnerable > 0f)
        advance(300)
        snap("together_back_tablet")
    }

    /** When the clock runs out: "Time's up!", then the result for both kids. */
    @Test fun theGameEndsWhenTheClockRunsOut() {
        var result: TogetherResult? = null
        show { TwoPlayerScreen(key = 1, kids = listOf(caleb, myaCard), onGameOver = { result = it }, onQuit = {}, seconds = 3f) }
        advance(3300)
        compose.onNodeWithText("Time's up!").assertExists()
        compose.onNodeWithContentDescription("Pause").assertDoesNotExist()
        snap("together_times_up_tablet")
        assertEquals(null, result)
        advance(2500)
        val r = result!!
        assertEquals(listOf("Caleb", "Mya"), r.kids.map { it.name })
        assertEquals(listOf(0, 1), r.kids.map { it.playerId })
        assertTrue(r.kids.all { it.score > 0 })
    }

    /** Each kid's game counts for that kid, both go on the high scores, and a guest unlocks nothing. */
    @Test fun resultsCountForEachKid() {
        seedKids(three = false)
        val me = Profile(storage)
        val result =
            TogetherResult(
                listOf(
                    KidResult(0, "Caleb", style, 90, 1, 0, 0),
                    KidResult(1, "Mya", mya, 130, 3, 20, 1),
                ),
                timestamp = 5000L,
            )
        val unlocked = recordTogether(me, storage, result, season = emptySet())
        assertEquals(listOf(emptyList(), Unlocks.newlyUnlocked(Stats(), Stats(130, 3, 1))), unlocked)
        assertEquals(Stats(90, 1, 1), me.stats)
        assertEquals(Stats(130, 3, 1), storage.player(1).loadStats())
        assertEquals("Caleb is still the one picked", 0, me.playerId)
        val scores = storage.loadScores()
        assertEquals(listOf("Mya" to 5001L, "Caleb" to 5000L), scores.map { it.name to it.timestamp })

        val guest = TogetherResult(listOf(result.kids[0], KidResult(GUEST_ID, "Player 2", mya, 400, 9, 0, 0)), 6000L)
        assertEquals(listOf(emptyList(), emptyList<Any>()), recordTogether(me, storage, guest, season = emptySet()))
    }

    private fun endScreen(name: String) {
        val topHat = Unlocks.newlyUnlocked(Stats(), Stats(bestScore = 120))
        val result =
            TogetherResult(
                listOf(
                    KidResult(0, "Caleb", style, 212, 4, 75, 1),
                    KidResult(1, "Mya", mya, 120, 2, 0, 2),
                ),
                timestamp = 5000L,
            )
        show { TogetherOverScreen(result, listOf(emptyList(), topHat), onPlayAgain = {}, onHome = {}) }
        advance(500)
        compose.onNodeWithText("Caleb is the biggest!").assertExists()
        compose.onNodeWithText("Great teamwork, Caleb and Mya!").assertExists()
        snap(name)
    }

    @Test fun endScreenTablet() = endScreen("together_over_tablet")

    @Config(qualifiers = NOTE_20)
    @Test
    fun endScreenNote20() = endScreen("together_over_note20")
}
