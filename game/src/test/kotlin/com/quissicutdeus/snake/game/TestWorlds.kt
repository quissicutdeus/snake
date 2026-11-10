package com.quissicutdeus.snake.game

import com.quissicutdeus.snake.game.model.Snake
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.sim.GameSim
import com.quissicutdeus.snake.game.sim.SimOptions

/** An empty arena (no food, no spawner refills, AI off) for rule tests. */
fun emptySim(
    aiEnabled: Boolean = false,
    pellets: Int = 0,
) = GameSim(
    seed = 42,
    options = SimOptions(pelletCount = pellets, aiCount = 0, spawnPlayer = false, aiEnabled = aiEnabled, powerUps = false),
)

fun GameSim.player(
    x: Float,
    y: Float,
    heading: Float,
    length: Float,
): Snake = addSnake("Kid", SnakeStyle.DEFAULT, isPlayer = true, x = x, y = y, heading = heading, length = length)

fun GameSim.ai(
    x: Float,
    y: Float,
    heading: Float,
    length: Float,
    name: String = "Bot",
): Snake = addSnake(name, SnakeStyle.DEFAULT, isPlayer = false, x = x, y = y, heading = heading, length = length)

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
const val WEST = Math.PI.toFloat()
const val SOUTH = (Math.PI / 2).toFloat()
const val NORTH = (-Math.PI / 2).toFloat()
