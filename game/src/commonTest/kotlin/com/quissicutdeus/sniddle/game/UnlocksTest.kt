package com.quissicutdeus.sniddle.game

import com.quissicutdeus.sniddle.game.model.Eyes
import com.quissicutdeus.sniddle.game.model.Hat
import com.quissicutdeus.sniddle.game.model.Holiday
import com.quissicutdeus.sniddle.game.model.Pattern
import com.quissicutdeus.sniddle.game.model.Stats
import com.quissicutdeus.sniddle.game.model.Unlocks
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
        assertEquals(18, rules.size)
        assertFalse(rules.any { Unlocks.isUnlocked(it.item, fresh) })
    }

    @Test fun someNewLooksAreFreeAndSomeAreEarned() {
        val fresh = Stats()
        for (free in listOf(Hat.COWBOY, Hat.FLOWERS, Eyes.GLASSES)) assertTrue(Unlocks.isUnlocked(free, fresh), "$free")
        assertEquals("Play 5 games", Unlocks.ruleFor(Hat.PROPELLER)?.goal)
        assertEquals("Reach length 150", Unlocks.ruleFor(Eyes.STARS)?.goal)
        assertEquals("Eat 25 snails", Unlocks.ruleFor(Hat.PIRATE)?.goal)
        assertEquals("Play 25 games", Unlocks.ruleFor(Eyes.SPARKLY)?.goal)
        val kid = Stats(bestScore = 160, snailsEaten = 30, gamesPlayed = 30)
        for (look in listOf(Hat.PROPELLER, Eyes.STARS, Hat.PIRATE, Eyes.SPARKLY)) assertTrue(Unlocks.isUnlocked(look, kid), "$look")
        assertEquals("Hat.PROPELLER", Unlocks.ruleFor(Hat.PROPELLER)?.key)
        assertEquals("Eyes.SPARKLY", Unlocks.ruleFor(Eyes.SPARKLY)?.key)
    }

    @Test fun goalsUnlockAndAreReportedOnce() {
        val before = Stats(bestScore = 90, snailsEaten = 8, gamesPlayed = 9)
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
        val old = Stats(bestScore = 120, snailsEaten = 0, gamesPlayed = 10)
        assertEquals(listOf(Hat.TOP_HAT, Pattern.STARS, Hat.PROPELLER), Unlocks.reachedBy(old).map { it.item })
        val after = old.after(score = 30, eaten = 12)
        assertEquals(Unlocks.reachedBy(after) - Unlocks.reachedBy(old).toSet(), Unlocks.newlyUnlocked(old, after))
    }

    @Test fun progressCountsUpToTheGoal() {
        val stats = Stats(bestScore = 150, snailsEaten = 7, gamesPlayed = 30)
        val wizard = Unlocks.ruleFor(Hat.WIZARD)!!
        assertEquals(150, wizard.progress(stats))
        assertEquals(200, wizard.target)
        assertEquals(7, Unlocks.ruleFor(Eyes.HEARTS)!!.progress(stats))
        // Past the goal it stops at the goal.
        assertEquals(10, Unlocks.ruleFor(Pattern.STARS)!!.progress(stats))
    }

    @Test fun hintsSayHowMuchIsLeft() {
        val stats = Stats(bestScore = 120, snailsEaten = 8, gamesPlayed = 7)
        val wizard = Unlocks.ruleFor(Hat.WIZARD)!!
        val hearts = Unlocks.ruleFor(Eyes.HEARTS)!!
        val stars = Unlocks.ruleFor(Pattern.STARS)!!
        assertEquals("80 more length to go!", wizard.hint(stats))
        assertEquals("1 more length to go!", wizard.hint(stats.copy(bestScore = 199)))
        assertEquals("Eat 2 more snails!", hearts.hint(stats))
        assertEquals("Eat 1 more snail!", hearts.hint(stats.copy(snailsEaten = 9)))
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

    @Test fun everyUnlockHasItsOwnSavedName() {
        // Building the unlock list already fails for a look type without a saved name.
        val looks = (Hat.entries + Eyes.entries + Pattern.entries + Holiday.entries.flatMap { it.looks }).distinct()
        val keys = looks.mapNotNull { Unlocks.ruleFor(it)?.key }
        assertEquals(18, keys.size)
        assertEquals(keys.size, keys.toSet().size, "$keys")
        // These get saved on phones, so they must never change.
        assertEquals("Hat.TOP_HAT", Unlocks.ruleFor(Hat.TOP_HAT)?.key)
        assertEquals("Eyes.HEARTS", Unlocks.ruleFor(Eyes.HEARTS)?.key)
        assertEquals("Pattern.STARS", Unlocks.ruleFor(Pattern.STARS)?.key)
        assertEquals("Pattern.FLAG", Unlocks.ruleFor(Pattern.FLAG)?.key)
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
