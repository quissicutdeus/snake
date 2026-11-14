package com.quissicutdeus.sniddle.game

import com.quissicutdeus.sniddle.game.model.PowerType
import com.quissicutdeus.sniddle.game.sim.dangers
import kotlin.test.Test
import kotlin.test.assertEquals

class DangerTest {
    @Test fun onlyBiggerSnailsHeadingAtYouAreDangers() {
        val sim = emptySim()
        val kid = sim.player(2000f, 2000f, EAST, 200f)
        val coming = sim.ai(1000f, 2000f, EAST, 800f, name = "Coming")
        sim.ai(3000f, 2000f, EAST, 800f, name = "Going away")
        sim.ai(2000f, 1000f, SOUTH, 120f, name = "Small")
        sim.ai(2000f, 2600f, NORTH, 800f, name = "Close")
        sim.ai(2000f, 3900f, NORTH, 800f, name = "Too far")
        assertEquals(listOf("Close", "Coming"), sim.dangers().map { it.name })

        // With Super Strength nobody is a danger.
        kid.givePower(PowerType.STRENGTH)
        assertEquals(emptyList(), sim.dangers().map { it.name })
        assertEquals("Coming", coming.name)
    }

    /** A bigger snail heading for the kid's tail (not their head) gets an arrow too: a bite there cuts it off. */
    @Test fun aBiggerSnailHeadingForYourBodyIsADanger() {
        val sim = emptySim()
        // A long kid heading east: the body lies to the west, out to about x = 500.
        sim.player(2000f, 2000f, EAST, 1500f)
        // Both are well off the head's line: about 60 degrees off pointing at it.
        sim.ai(800f, 2700f, NORTH, 6000f, name = "At the tail")
        sim.ai(800f, 1300f, NORTH, 6000f, name = "Away from the tail")
        assertEquals(listOf("At the tail"), sim.dangers().map { it.name })
    }

    @Test fun noPlayerNoDangers() {
        val sim = emptySim()
        sim.ai(1000f, 2000f, EAST, 800f)
        assertEquals(emptyList(), sim.dangers())
    }
}
