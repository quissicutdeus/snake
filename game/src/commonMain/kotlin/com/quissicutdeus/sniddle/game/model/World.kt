package com.quissicutdeus.sniddle.game.model

/** Everything that exists in the arena. Owned and mutated by [com.quissicutdeus.sniddle.game.sim.GameSim]. */
class World(
    val width: Float,
    val height: Float,
) {
    val snails = ArrayList<Snail>()
    val pellets = ArrayList<Pellet>()
    val tailPieces = ArrayList<TailPiece>()
    val powerUps = ArrayList<PowerUp>()

    fun snailById(id: Int): Snail? = snails.firstOrNull { it.id == id && it.alive }
}
