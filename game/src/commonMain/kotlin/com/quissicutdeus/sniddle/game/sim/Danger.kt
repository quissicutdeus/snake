package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.dist
import kotlin.math.cos
import kotlin.math.sin

/**
 * Snails that could hurt the player and are coming their way: bigger (in a
 * fight, so Super Strength counts), within [GameConfig.DANGER_RANGE], and with
 * their head pointed roughly at the player. Only a bigger snail's mouth can
 * hurt the player, so it's their heads that matter. Nearest first.
 * With two kids, [me] says whose point of view (each half of the screen has its own).
 */
fun GameSim.dangers(me: Snail? = player): List<Snail> {
    val p = me?.takeIf { it.alive } ?: return emptyList()
    return world.snails
        .filter { isDangerTo(it, p) }
        .sortedBy { dist(it.x, it.y, p.x, p.y) }
}

/** Whether [s] is one of [p]'s [dangers] ([p] must be alive). */
internal fun isDangerTo(
    s: Snail,
    p: Snail,
): Boolean {
    if (s === p || !s.alive || p.fightRelationTo(s) != SizeRelation.SMALLER) return false
    val d = dist(s.x, s.y, p.x, p.y)
    if (d > GameConfig.DANGER_RANGE || d < 1f) return false
    // Cosine of the angle between where it's heading and where the player is.
    val toward = (cos(s.heading) * (p.x - s.x) + sin(s.heading) * (p.y - s.y)) / d
    return toward > GameConfig.DANGER_HEADING_COS
}
