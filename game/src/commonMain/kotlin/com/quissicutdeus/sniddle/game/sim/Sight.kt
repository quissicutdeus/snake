package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.dist
import kotlin.math.abs

/** The kids whose screens count: every player still alive. */
internal fun GameSim.watchers(): List<Snail> = players.filter { it.alive }

/**
 * Whether a coil reaching [reach] around ([x], [y]) is off [kid]'s screen:
 * the screen the game reports ([GameSim.setView]) plus
 * [GameConfig.SPAWN_VIEW_MARGIN]. Before that is known, it is a circle as wide
 * as the widest phone's screen ([GameConfig.offscreenDistance]), which for a
 * big kid in the middle of the arena covers nearly all of it.
 */
internal fun GameSim.hiddenFrom(
    kid: Snail,
    x: Float,
    y: Float,
    reach: Float,
): Boolean {
    val v = viewOf(kid) ?: return dist(x, y, kid.x, kid.y) >= GameConfig.offscreenDistance(kid.radius) + reach
    val m = reach + GameConfig.SPAWN_VIEW_MARGIN
    return abs(x - v.x) >= v.halfWidth + m || abs(y - v.y) >= v.halfHeight + m
}
