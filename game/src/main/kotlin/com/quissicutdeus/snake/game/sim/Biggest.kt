package com.quissicutdeus.snake.game.sim

import com.quissicutdeus.snake.game.GameConfig
import com.quissicutdeus.snake.game.model.Snake

/** True if no other snake is as long as [s]. */
internal fun GameSim.isBiggest(s: Snake): Boolean = world.snakes.none { it !== s && it.alive && it.length >= s.length }

/**
 * Keeps [GameSim.playerOnTop] and [GameSim.biggestSeconds] up to date, and
 * cheers ([GameEvent.BecameBiggest]) when the player climbs to the top. Two
 * snakes trading places can't make it cheer over and over: after a cheer the
 * next one waits [GameConfig.BIGGEST_CHEER_GAP_SECONDS].
 */
internal fun GameSim.trackBiggest(dt: Float) {
    together?.let { return trackBiggestTogether(it, dt) }
    sinceBiggestCheer += dt
    val p = player?.takeIf { it.alive && !gameOver } ?: return
    val top = isBiggest(p)
    if (top) biggestSeconds += dt
    if (top && !playerOnTop && sinceBiggestCheer >= GameConfig.BIGGEST_CHEER_GAP_SECONDS) {
        sinceBiggestCheer = 0f
        emit(GameEvent.BecameBiggest(p.x, p.y, p.id))
    }
    playerOnTop = top
}
