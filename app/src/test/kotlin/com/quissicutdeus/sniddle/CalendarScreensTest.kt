package com.quissicutdeus.sniddle

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.quissicutdeus.sniddle.game.model.ArenaSeason
import com.quissicutdeus.sniddle.game.model.Birthday
import com.quissicutdeus.sniddle.game.model.Hat
import com.quissicutdeus.sniddle.game.model.Holiday
import com.quissicutdeus.sniddle.ui.GameResult
import com.quissicutdeus.sniddle.ui.Profile
import com.quissicutdeus.sniddle.ui.SniddleApp
import com.quissicutdeus.sniddle.ui.WhatsNew
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Birthdays (setting one, the cheer on the day, the cake hat) and the arena's seasons, on a fixed date. */
class CalendarScreensTest : ScreenTest() {
    private fun launchOn(date: LocalDate) {
        storage.newsSeen = WhatsNew.last().number
        // With a version, so the seasonal title screenshots show it on grass, leaves and snow.
        compose.setContent { SniddleApp(storage, { date.toDay() }, NoSound, NoBuzz, version = "0.2.8") }
        advance(800)
    }

    @Test fun aKidSetsTheirBirthdayInMySnail() {
        launchOn(LocalDate.of(2026, 9, 1))
        compose.onNodeWithText("My Snails").performClick()
        advance(500)
        scrollMySnailToBottom()
        compose.onNodeWithText("🎂 Add my birthday").performClick()
        advance(500)
        compose.onNodeWithText("When's your birthday?").assertExists()
        saveScreenshot(compose.onNode(isDialog()).captureToImage().asAndroidBitmap(), "birthday_months")
        compose.onNodeWithText("February").performClick()
        advance(300)
        compose.onNodeWithText("29").assertExists() // the leap day is there to pick
        compose.onNodeWithText("30").assertDoesNotExist()
        saveScreenshot(compose.onNode(isDialog()).captureToImage().asAndroidBitmap(), "birthday_days")
        compose.onNodeWithText("29").performClick()
        advance(500)
        assertEquals(Birthday(2, 29), storage.player(0).loadBirthday())
        compose.onNodeWithText("🎂 February 29").assertExists()
        snap("birthday_my_snail")
        // Changeable, and optional.
        compose.onNodeWithText("🎂 February 29").performClick()
        advance(500)
        compose.onNodeWithText("No birthday").performClick()
        advance(500)
        assertNull(storage.player(0).loadBirthday())
        compose.onNodeWithText("🎂 Add my birthday").assertExists()
    }

    @Test fun onTheirBirthdayTheTitleCheersAndConfettiFlies() {
        storage.player(0).saveSnailName("Caleb")
        storage.player(0).saveBirthday(Birthday(9, 12))
        storage.newsSeen = WhatsNew.last().number
        compose.setContent { SniddleApp(storage, { Day(2026, 9, 12) }, NoSound, NoBuzz) }
        advance(300)
        compose.onNodeWithText("🎂 Happy birthday, Caleb! 🎉").assertExists()
        compose.onNodeWithText("Play a game to get your birthday cake hat!").assertExists()
        snap("birthday_title")
        advance(4000)
        snap("birthday_title_after")
        compose.onNodeWithText("My Snails").performClick()
        advance(500)
        scrollMySnailToBottom()
        compose.onNodeWithContentDescription("Birthday cake, locked: On your birthday!", substring = true).assertExists()
    }

    @Test
    @Config(qualifiers = PORTRAIT)
    fun pickingTheBirthdayKidSetsOffTheConfetti() {
        storage.player(0).saveSnailName("Caleb")
        val mya = storage.addPlayer(sirNoodles)!!
        storage.player(mya).saveSnailName("Mya")
        storage.player(mya).saveBirthday(Birthday(9, 12))
        launchOn(LocalDate.of(2026, 9, 12))
        compose.onNodeWithText("Happy birthday", substring = true).assertDoesNotExist() // not Caleb's
        compose.onNodeWithContentDescription("Switch to Mya").performClick()
        advance(700)
        compose.onNodeWithText("🎂 Happy birthday, Mya! 🎉").assertExists()
        snap("birthday_title_portrait")
    }

    @Test fun theDayBeforeAndAfterIsJustAnotherDay() {
        storage.player(0).saveBirthday(Birthday(9, 12))
        assertFalse(Profile(storage) { Day(2026, 9, 11) }.isBirthday)
        assertFalse(Profile(storage) { Day(2026, 9, 13) }.isBirthday)
        assertTrue(Profile(storage) { Day(2031, 9, 12) }.isBirthday)
    }

    @Test fun aLeapDayBirthdayIsOnThe28thInOtherYears() {
        storage.player(0).saveBirthday(Birthday(2, 29))
        assertTrue(Profile(storage) { Day(2027, 2, 28) }.isBirthday)
        assertFalse(Profile(storage) { Day(2027, 3, 1) }.isBirthday)
        assertFalse(Profile(storage) { Day(2028, 2, 28) }.isBirthday)
        assertTrue(Profile(storage) { Day(2028, 2, 29) }.isBirthday)
    }

    @Test fun aGameOnTheirBirthdayEarnsTheCakeHatForGood() {
        storage.player(0).saveBirthday(Birthday(9, 12))
        val me = Profile(storage) { Day(2026, 9, 12) }
        val options = me.todaysOptions()
        assertTrue(Holiday.BIRTHDAY in options.season) // party hats for the computer snails
        val unlocked = me.recordGame(GameResult(30, 0, style, 1L))
        assertEquals(listOf(Hat.BIRTHDAY_CAKE), unlocked.map { it.item })
        val nextWeek = Profile(storage) { Day(2026, 9, 19) }
        assertFalse(nextWeek.isBirthday)
        assertTrue(Holiday.BIRTHDAY !in nextWeek.todaysOptions().season)
        assertTrue(Holiday.BIRTHDAY in nextWeek.stats.holidays)
    }

    @Test fun eachKidHasTheirOwnBirthdayAndRemovingForgetsIt() {
        storage.player(0).saveBirthday(Birthday(9, 12))
        val me = Profile(storage) { Day(2026, 9, 12) }
        me.addPlayer(kotlin.random.Random(1))
        assertNull(me.birthday)
        assertFalse(me.isBirthday)
        me.changeBirthday(Birthday(1, 2))
        val id = me.playerId
        me.switchTo(0)
        assertEquals(Birthday(9, 12), me.birthday)
        me.removePlayer(id)
        assertFalse(rawPrefs().contains("p$id.birthday"))
        // A new kid in the same spot starts with no birthday.
        me.addPlayer(kotlin.random.Random(2))
        assertEquals(id, me.playerId)
        assertNull(me.birthday)
    }

    /** The title shows the season's floor (wrapping paper in Christmas week); a game sometimes gets dirt or space instead. */
    @Test fun theArenaFollowsTheSeasons() {
        assertEquals(ArenaSeason.SNOW, Profile(storage) { Day(2026, 12, 10) }.todaysOptions().arena)
        assertEquals(ArenaSeason.WRAPPING_PAPER, Profile(storage) { Day(2026, 12, 20) }.todaysOptions().arena)
        assertEquals(ArenaSeason.AUTUMN, Profile(storage) { Day(2026, 10, 20) }.todaysOptions().arena)
        val may = Profile(storage) { Day(2026, 5, 20) }
        assertEquals(ArenaSeason.GRASS, may.todaysOptions().arena)
        val rng = kotlin.random.Random(4)
        val games = List(100) { may.gameArena(rng) }.toSet()
        assertEquals(setOf(ArenaSeason.GRASS, ArenaSeason.DIRT, ArenaSeason.SPACE), games)
        val christmas = Profile(storage) { Day(2026, 12, 25) }
        assertEquals(setOf(ArenaSeason.WRAPPING_PAPER), List(20) { christmas.gameArena(rng) }.toSet())
    }

    @Test fun theTitleAndTheGameShowTheSnow() {
        launchOn(LocalDate.of(2027, 1, 20))
        snap("title_snow")
        playFromTitle()
        advance(1500)
        snap("playing_snow")
    }

    @Test fun theTitleShowsAutumnLeaves() {
        launchOn(LocalDate.of(2026, 10, 20))
        snap("title_autumn")
    }
}
