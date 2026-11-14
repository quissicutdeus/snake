package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.dist
import kotlin.math.cos
import kotlin.math.sin

/**
 * Snails that could hurt the player and are coming their way: bigger (in a
 * fight, so Super Strength counts), with their head pointed roughly at the
 * player's head (within [GameConfig.DANGER_RANGE]) or at some part of the
 * player's body (within [GameConfig.DANGER_BODY_RANGE]), where a bite cuts the
 * tail off. Only a bigger snail's mouth can hurt the player, so it's their
 * heads that matter. Nearest first.
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
    if (headingAt(s, p.x, p.y, GameConfig.DANGER_RANGE)) return true
    // The body, every few circles: close enough together for a 50-degree cone, and this runs every frame.
    for (i in BODY_STRIDE until p.segCount step BODY_STRIDE) {
        if (headingAt(s, p.segX[i], p.segY[i], GameConfig.DANGER_BODY_RANGE)) return true
    }
    return false
}

/** Whether [s]'s head is within [range] of (x, y) and pointed roughly at it. */
private fun headingAt(
    s: Snail,
    x: Float,
    y: Float,
    range: Float,
): Boolean {
    val d = dist(s.x, s.y, x, y)
    if (d > range || d < 1f) return false
    // Cosine of the angle between where it's heading and where the point is.
    val toward = (cos(s.heading) * (x - s.x) + sin(s.heading) * (y - s.y)) / d
    return toward > GameConfig.DANGER_HEADING_COS
}

/** Body circles skipped between the ones checked (they are at most half a radius apart). */
private const val BODY_STRIDE = 4
