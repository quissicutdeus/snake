package com.snakearena.game.model

/** Everything that exists in the arena. Owned and mutated by [com.snakearena.game.sim.GameSim]. */
class World(val width: Float, val height: Float) {
    val snakes = ArrayList<Snake>()
    val pellets = ArrayList<Pellet>()
    val tailPieces = ArrayList<TailPiece>()

    /** Seconds of simulated time. */
    var time = 0f

    fun snakeById(id: Int): Snake? = snakes.firstOrNull { it.id == id && it.alive }
}
