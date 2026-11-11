package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.Snail

/** True if no other snail is as long as [s]. */
internal fun GameSim.isBiggest(s: Snail): Boolean = world.snails.none { it !== s && it.alive && it.length >= s.length }

/**
 * Keeps [GameSim.playerOnTop] and [GameSim.biggestSeconds] up to date, and
 * cheers ([GameEvent.BecameBiggest]) when the player climbs to the top. Two
 * snails trading places can't make it cheer over and over: after a cheer the
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
