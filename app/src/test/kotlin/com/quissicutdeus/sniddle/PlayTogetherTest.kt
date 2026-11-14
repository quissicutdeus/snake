package com.quissicutdeus.sniddle

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
import com.quissicutdeus.sniddle.game.Difficulty
import com.quissicutdeus.sniddle.game.model.Birthday
import com.quissicutdeus.sniddle.game.model.Eyes
import com.quissicutdeus.sniddle.game.model.Hat
import com.quissicutdeus.sniddle.game.model.Holiday
import com.quissicutdeus.sniddle.game.model.Pattern
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.model.Stats
import com.quissicutdeus.sniddle.game.model.Unlocks
import com.quissicutdeus.sniddle.game.sim.GameSim
import com.quissicutdeus.sniddle.game.sim.SimOptions
import com.quissicutdeus.sniddle.ui.GUEST_ID
import com.quissicutdeus.sniddle.ui.KidResult
import com.quissicutdeus.sniddle.ui.PlayerCard
import com.quissicutdeus.sniddle.ui.Profile
import com.quissicutdeus.sniddle.ui.Settings
import com.quissicutdeus.sniddle.ui.SniddleApp
import com.quissicutdeus.sniddle.ui.TogetherOverScreen
import com.quissicutdeus.sniddle.ui.TogetherResult
import com.quissicutdeus.sniddle.ui.TwoPlayerScreen
import com.quissicutdeus.sniddle.ui.WhatsNew
import com.quissicutdeus.sniddle.ui.recordTogether
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.annotation.Config
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Two kids on one screen: the button, picking who plays, both halves steering, comebacks and the end screen. */
@Config(qualifiers = TABLET)
class PlayTogetherTest : ScreenTest() {
    private val mya = SnailStyle(color1 = 10, color2 = 4, pattern = Pattern.DOTS, eyes = Eyes.HEARTS, hat = Hat.BOW)
    private val caleb = PlayerCard(0, "Caleb", style)
    private val myaCard = PlayerCard(1, "Mya", mya)

    /** Caleb (playing), Mya and a third kid who hasn't named their snail. */
    private fun seedKids(three: Boolean = true) {
        storage.newsSeen = WhatsNew.last().number
        storage.player(0).apply {
            saveStyle(style)
            saveSnailName("Caleb")
        }
        storage.addPlayer(mya)
        storage.player(1).saveSnailName("Mya")
        if (three) storage.addPlayer(sirNoodles)
        storage.saveCurrentPlayer(0)
    }

    @Test fun titleOffersPlayTogetherOnATablet() {
        seedKids()
        launchApp()
        compose.onNodeWithText("Play together").assertExists()
        snap("together_title_tablet")
    }

    @Config(qualifiers = WIDE_PHONE)
    @Test
    fun titleOffersPlayTogetherOnABigPhoneSideways() {
        seedKids()
        launchApp()
        compose.onNodeWithText("Play together").assertExists()
        snap("together_title_wide_phone")
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

    @Config(qualifiers = NIGHT)
    @Test
    fun pickingTheSecondKidNight() = pickingTheSecondKid()

    /** With only one kid on the phone: a "Player 2" with a random look, or add a new kid and come back here. */
    @Test fun withOneKidPlayer2OrAddAKid() {
        storage.newsSeen = WhatsNew.last().number
        storage.player(0).saveSnailName("Caleb")
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
            SniddleApp(storage, SystemCalendar, NoSound, NoBuzz)
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

    /** Two fingers, one in each half, each steering its own kid's snail. */
    private fun bothHalvesSteer(name: String) {
        var sim: GameSim? = null
        // No computer snails to bump into, so each kid goes where their finger says: none at the start,
        // and none spawned later (the world is random, so a new one could turn up in a kid's path).
        val quietArena: (GameSim) -> Unit = { s ->
            sim = s
            s.world.snails.removeAll { !it.isPlayer }
        }
        show {
            TwoPlayerScreen(
                key = 1,
                kids = listOf(caleb, myaCard),
                onGameOver = {},
                onQuit = {},
                options = SimOptions(aiCount = 0, powerUps = false),
                prepare = quietArena,
            )
        }
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

    @Config(qualifiers = WIDE_PHONE)
    @Test
    fun bothHalvesSteerWidePhone() = bothHalvesSteer("together_playing_wide_phone")

    /** A big snail gobbles Mya: "Oops!" in her half, and three seconds later she's back, small and blinking. */
    @Test fun aGobbledKidComesBack() {
        var sim: GameSim? = null
        show {
            TwoPlayerScreen(
                key = 1,
                kids = listOf(caleb, myaCard),
                onGameOver = {},
                onQuit = {},
                // One heart each, so the first bite gobbles her.
                options = SimOptions(difficulty = Difficulty.NORMAL.copy(hearts = 1)),
                prepare = { s ->
                    sim = s
                    val kid = s.players[1]
                    // Only Noodle nearby, so it is Noodle who does it.
                    s.world.snails.removeAll { !it.isPlayer }
                    // Nose to nose with her: the first step is the bite.
                    s.addSnail(
                        "Noodle",
                        sirNoodles,
                        isPlayer = false,
                        x = kid.x,
                        y = kid.y - 45f,
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
        val me = Profile(storage, SystemCalendar)
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

    /** Gentle only when both kids have little kid mode on (a guest isn't little); both kids' holidays; a floor like a one-player game's. */
    @Test fun twoPlayerOptionsFollowBothKids() {
        seedKids(three = false)
        val june2 = Day(2026, 6, 2)
        storage.player(1).saveBirthday(Birthday(6, 2))
        val me = Profile(storage) { june2 }
        assertEquals(Difficulty.NORMAL, me.togetherOptions(1).difficulty)
        me.changeLittleKid(true)
        assertEquals("only Caleb is little", Difficulty.NORMAL, me.togetherOptions(1).difficulty)
        assertEquals("a guest isn't little", Difficulty.NORMAL, me.togetherOptions(GUEST_ID).difficulty)
        storage.player(1).saveLittleKid(true)
        assertEquals(Difficulty.LITTLE, me.togetherOptions(1).difficulty)
        // Mya's birthday dresses up the computer snails, like a one-player game on her birthday.
        assertTrue(Holiday.BIRTHDAY in me.togetherOptions(1).season)
        assertFalse(Holiday.BIRTHDAY in me.togetherOptions(GUEST_ID).season)
        // The floor is picked like a one-player game's: the same pick for the same throw of the dice.
        assertEquals(me.gameArena(Random(7)), me.togetherOptions(1, Random(7)).arena)
    }

    /** Bedtime counts a two-player game too, stops it at a calm moment, counts it for both kids and shows the rest screen. */
    @Test fun bedtimeStopsATwoPlayerGame() {
        seedKids(three = false)
        storage.saveSettings(Settings(bedtimeMinutes = 15))
        storage.playDay = PlayDay(LocalDate.now().toEpochDay(), 15 * 60_000L - 500L).encode()
        launchApp()
        compose.onNodeWithText("Play together").performClick()
        advance(500)
        compose.onNodeWithText("Go!").performClick()
        advance(500)
        compose.onNodeWithContentDescription("Caleb's half", substring = true).assertExists()
        val rest = "Time to rest, snail! 😴"
        var waited = 0
        while (waited < 70_000 && runCatching { compose.onNodeWithText(rest).assertExists() }.isFailure) {
            advance(500)
            waited += 500
        }
        compose.onNodeWithText(rest).assertExists()
        assertEquals(1, storage.player(0).loadStats().gamesPlayed)
        assertEquals(1, storage.player(1).loadStats().gamesPlayed)
        compose.onNodeWithText("Play together").assertDoesNotExist()
        snap("together_bedtime_rest_tablet")
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

    @Config(qualifiers = WIDE_PHONE)
    @Test
    fun endScreenWidePhone() = endScreen("together_over_wide_phone")

    @Config(qualifiers = NIGHT)
    @Test
    fun endScreenNight() = endScreen("together_over_tablet")
}
