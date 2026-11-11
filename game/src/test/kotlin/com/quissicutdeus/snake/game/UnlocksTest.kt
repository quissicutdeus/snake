package com.quissicutdeus.snake.game

import com.quissicutdeus.snake.game.model.Eyes
import com.quissicutdeus.snake.game.model.Hat
import com.quissicutdeus.snake.game.model.Pattern
import com.quissicutdeus.snake.game.model.Stats
import com.quissicutdeus.snake.game.model.Unlocks
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnlocksTest {
    @Test fun everythingThatExistedBeforeIsAlwaysUnlocked() {
        val fresh = Stats()
        for (h in listOf(Hat.NONE, Hat.CROWN, Hat.PARTY, Hat.BOW)) assertTrue(Unlocks.isUnlocked(h, fresh))
        for (e in listOf(Eyes.GOOGLY, Eyes.SLEEPY, Eyes.SUNGLASSES)) assertTrue(Unlocks.isUnlocked(e, fresh))
        for (p in Pattern.entries - Pattern.STARS - Pattern.FLAG - Pattern.HEARTS) assertTrue(Unlocks.isUnlocked(p, fresh))
        val rules = (Hat.entries + Eyes.entries + Pattern.entries).mapNotNull { Unlocks.ruleFor(it) }
        assertEquals(14, rules.size)
        assertFalse(rules.any { Unlocks.isUnlocked(it.item, fresh) })
    }

    @Test fun goalsUnlockAndAreReportedOnce() {
        val before = Stats(bestScore = 90, snakesEaten = 8, gamesPlayed = 9)
        val after = before.after(score = 120, eaten = 3)
        assertEquals(Stats(120, 11, 10), after)
        assertEquals(listOf(Hat.TOP_HAT, Pattern.STARS, Eyes.HEARTS), Unlocks.newlyUnlocked(before, after).map { it.item })
        assertEquals(emptyList(), Unlocks.newlyUnlocked(after, after.after(score = 50, eaten = 0)))
        assertTrue(Unlocks.isUnlocked(Hat.TOP_HAT, after))
        assertFalse(Unlocks.isUnlocked(Hat.WIZARD, after))
        assertEquals("Reach length 200", Unlocks.ruleFor(Hat.WIZARD)?.goal)
    }

    @Test fun reachedByListsEveryUnlockedLook() {
        assertEquals(emptyList(), Unlocks.reachedBy(Stats()))
        // High scores from before unlocks existed can already reach some goals.
        val old = Stats(bestScore = 120, snakesEaten = 0, gamesPlayed = 10)
        assertEquals(listOf(Hat.TOP_HAT, Pattern.STARS), Unlocks.reachedBy(old).map { it.item })
        val after = old.after(score = 30, eaten = 12)
        assertEquals(Unlocks.reachedBy(after) - Unlocks.reachedBy(old).toSet(), Unlocks.newlyUnlocked(old, after))
    }

    @Test fun progressCountsUpToTheGoal() {
        val stats = Stats(bestScore = 150, snakesEaten = 7, gamesPlayed = 30)
        val wizard = Unlocks.ruleFor(Hat.WIZARD)!!
        assertEquals(150, wizard.progress(stats))
        assertEquals(200, wizard.target)
        assertEquals(7, Unlocks.ruleFor(Eyes.HEARTS)!!.progress(stats))
        // Past the goal it stops at the goal.
        assertEquals(10, Unlocks.ruleFor(Pattern.STARS)!!.progress(stats))
    }

    @Test fun hintsSayHowMuchIsLeft() {
        val stats = Stats(bestScore = 120, snakesEaten = 8, gamesPlayed = 7)
        val wizard = Unlocks.ruleFor(Hat.WIZARD)!!
        val hearts = Unlocks.ruleFor(Eyes.HEARTS)!!
        val stars = Unlocks.ruleFor(Pattern.STARS)!!
        assertEquals("80 more length to go!", wizard.hint(stats))
        assertEquals("1 more length to go!", wizard.hint(stats.copy(bestScore = 199)))
        assertEquals("Eat 2 more snakes!", hearts.hint(stats))
        assertEquals("Eat 1 more snake!", hearts.hint(stats.copy(snakesEaten = 9)))
        assertEquals("3 more games to go!", stars.hint(stats))
        assertEquals("1 more game to go!", stars.hint(stats.copy(gamesPlayed = 9)))
        assertEquals("10 more games to go!", stars.hint(Stats()))
        assertEquals("You did it!", stars.hint(stats.copy(gamesPlayed = 30)))
    }

    @Test fun surpriseMeOnlyPicksUnlockedLooks() {
        val fresh = Stats()
        val rng = Random(1)
        repeat(500) {
            val s = Unlocks.randomStyle(rng, fresh)
            assertTrue(Unlocks.isUnlocked(s.hat, fresh) && Unlocks.isUnlocked(s.eyes, fresh) && Unlocks.isUnlocked(s.pattern, fresh), "$s")
        }
    }

    @Test fun statsSurviveSavingAndJunkIsIgnored() {
        val s = Stats(250, 41, 33)
        assertEquals(s, Stats.decode(s.encode()))
        assertNull(Stats.decode(null))
        assertNull(Stats.decode("1,2"))
        assertNull(Stats.decode("a,b,c"))
        assertNull(Stats.decode("1,-2,3"))
    }
}
