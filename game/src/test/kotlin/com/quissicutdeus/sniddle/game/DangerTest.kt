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

    @Test fun noPlayerNoDangers() {
        val sim = emptySim()
        sim.ai(1000f, 2000f, EAST, 800f)
        assertEquals(emptyList(), sim.dangers())
    }
}
