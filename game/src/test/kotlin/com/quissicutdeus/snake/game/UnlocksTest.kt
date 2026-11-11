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
        for (p in Pattern.entries - Pattern.STARS) assertTrue(Unlocks.isUnlocked(p, fresh))
        val rules = (Hat.entries + Eyes.entries + Pattern.entries).mapNotNull { Unlocks.ruleFor(it) }
        assertEquals(5, rules.size)
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

    @Test fun progressCountsUpToTheGoal() {
        val stats = Stats(bestScore = 150, snakesEaten = 7, gamesPlayed = 30)
        val wizard = Unlocks.ruleFor(Hat.WIZARD)!!
        assertEquals(150, wizard.progress(stats))
        assertEquals(200, wizard.target)
        assertEquals(7, Unlocks.ruleFor(Eyes.HEARTS)!!.progress(stats))
        // Past the goal it stops at the goal.
        assertEquals(10, Unlocks.ruleFor(Pattern.STARS)!!.progress(stats))
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
