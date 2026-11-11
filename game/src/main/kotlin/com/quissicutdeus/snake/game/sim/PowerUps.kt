package com.quissicutdeus.snake.game.sim

import com.quissicutdeus.snake.game.GameConfig
import com.quissicutdeus.snake.game.model.PowerType
import com.quissicutdeus.snake.game.model.PowerUp
import com.quissicutdeus.snake.game.model.Snake
import com.quissicutdeus.snake.game.model.dist2

/**
 * Power-ups appear at random spots on the map (a few at a time), vanish
 * again after a while so they move around, and only the player picks them up.
 */
internal fun GameSim.updatePowerUps(dt: Float) {
    val ups = world.powerUps
    for (p in ups) p.age += dt
    ups.removeAll { it.age > GameConfig.POWER_UP_LIFETIME || pickedUp(it) }
    if (!options.powerUps) return
    nextPowerUpIn -= dt
    if (nextPowerUpIn <= 0f) {
        nextPowerUpIn = GameConfig.POWER_UP_INTERVAL
        if (ups.size < GameConfig.POWER_UP_MAX) ups.add(randomPowerUp())
    }
}

/** Fills the map with the starting power-ups. */
internal fun GameSim.placeStartingPowerUps() {
    if (!options.powerUps) return
    repeat(GameConfig.POWER_UP_START) { world.powerUps.add(randomPowerUp()) }
    nextPowerUpIn = GameConfig.POWER_UP_INTERVAL
}

private fun GameSim.randomPowerUp(): PowerUp {
    val m = GameConfig.POWER_UP_WALL_MARGIN
    return PowerUp(
        type = PowerType.entries[rng.nextInt(PowerType.entries.size)],
        x = m + rng.nextFloat() * (world.width - 2 * m),
        y = m + rng.nextFloat() * (world.height - 2 * m),
    )
}

/** True (and the power starts) if a player's head touches [p] (either kid's, with two). */
private fun GameSim.pickedUp(p: PowerUp): Boolean {
    val s = players.firstOrNull { it.alive && reaches(it, p) } ?: return false
    s.givePower(p.type)
    emit(GameEvent.PowerUpTaken(p.type, p.x, p.y))
    return true
}

private fun reaches(
    s: Snake,
    p: PowerUp,
): Boolean {
    val reach = s.radius + GameConfig.POWER_UP_RADIUS + GameConfig.EAT_MARGIN
    return dist2(s.x, s.y, p.x, p.y) <= reach * reach
}
