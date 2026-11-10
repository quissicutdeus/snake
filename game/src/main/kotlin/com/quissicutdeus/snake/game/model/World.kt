package com.quissicutdeus.snake.game.model

/** Everything that exists in the arena. Owned and mutated by [com.quissicutdeus.snake.game.sim.GameSim]. */
class World(
    val width: Float,
    val height: Float,
) {
    val snakes = ArrayList<Snake>()
    val pellets = ArrayList<Pellet>()
    val tailPieces = ArrayList<TailPiece>()

    /** Seconds of simulated time. */
    var time = 0f

    fun snakeById(id: Int): Snake? = snakes.firstOrNull { it.id == id && it.alive }
}
