package com.quissicutdeus.sniddle.game

import com.quissicutdeus.sniddle.game.model.PowerType
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.sim.GameSim
import com.quissicutdeus.sniddle.game.sim.SavedRun
import com.quissicutdeus.sniddle.game.sim.SimOptions
import kotlin.math.PI

/** An empty arena (no food, no spawner refills, AI off) for rule tests. */
fun emptySim(
    aiEnabled: Boolean = false,
    pellets: Int = 0,
    seed: Long = 42,
) = GameSim(
    seed = seed,
    options = SimOptions(pelletCount = pellets, aiCount = 0, spawnPlayer = false, aiEnabled = aiEnabled, powerUps = false),
)

/** A whole game whose kid starts [length] long (as if carried on from a saved game), with the arena filled around them. */
fun gameWithKid(
    length: Float,
    seed: Long,
) = GameSim(seed = seed, resume = SavedRun("Kid", SnailStyle.DEFAULT, length, length, 3, 0, List(PowerType.entries.size) { 0f }))

fun GameSim.player(
    x: Float,
    y: Float,
    heading: Float,
    length: Float,
): Snail = addSnail("Kid", SnailStyle.DEFAULT, isPlayer = true, x = x, y = y, heading = heading, length = length)

fun GameSim.ai(
    x: Float,
    y: Float,
    heading: Float,
    length: Float,
    name: String = "Bot",
): Snail = addSnail(name, SnailStyle.DEFAULT, isPlayer = false, x = x, y = y, heading = heading, length = length)

fun GameSim.steps(n: Int) = repeat(n) { step() }

/** Steps until [cond] holds (or fails after [max] steps). */
fun GameSim.stepUntil(
    max: Int = 600,
    cond: () -> Boolean,
): Int {
    repeat(max) { i ->
        if (cond()) return i
        step()
    }
    if (cond()) return max
    error("condition not reached in $max steps")
}

const val EAST = 0f
const val WEST = PI.toFloat()
const val SOUTH = (PI / 2).toFloat()
const val NORTH = (-PI / 2).toFloat()
