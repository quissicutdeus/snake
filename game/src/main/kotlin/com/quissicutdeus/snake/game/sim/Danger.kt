package com.quissicutdeus.snake.game.sim

import com.quissicutdeus.snake.game.GameConfig
import com.quissicutdeus.snake.game.model.Snake
import com.quissicutdeus.snake.game.model.dist
import kotlin.math.cos
import kotlin.math.sin

/**
 * Snakes that could hurt the player and are coming their way: bigger (in a
 * fight, so Super Strength counts), within [GameConfig.DANGER_RANGE], and with
 * their head pointed roughly at the player. Only a bigger snake's mouth can
 * hurt the player, so it's their heads that matter. Nearest first.
 * With two kids, [me] says whose point of view (each half of the screen has its own).
 */
fun GameSim.dangers(me: Snake? = player): List<Snake> {
    val p = me?.takeIf { it.alive } ?: return emptyList()
    return world.snakes
        .filter { s ->
            if (s === p || !s.alive || p.fightRelationTo(s) != SizeRelation.SMALLER) return@filter false
            val d = dist(s.x, s.y, p.x, p.y)
            if (d > GameConfig.DANGER_RANGE || d < 1f) return@filter false
            // Cosine of the angle between where it's heading and where the player is.
            val toward = (cos(s.heading) * (p.x - s.x) + sin(s.heading) * (p.y - s.y)) / d
            toward > GameConfig.DANGER_HEADING_COS
        }.sortedBy { dist(it.x, it.y, p.x, p.y) }
}
