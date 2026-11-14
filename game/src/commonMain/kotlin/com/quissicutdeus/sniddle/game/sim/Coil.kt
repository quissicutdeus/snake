package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.GameConfig
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A snail's body coiled up in a spiral around a point. Even a very long snail
 * fits in a small space this way, where a straight body could poke through the
 * fence.
 *
 * The head is either on the outside, pointing onward along the curve, or in
 * the middle with the tail end wrapped round it. A new computer snail gets
 * the head in the middle: the rings a kid bumps into first are then the tail
 * end, so a bite snips off a little, not the whole snail behind its head.
 */
class Coil private constructor(
    val xs: FloatArray,
    val ys: FloatArray,
    val count: Int,
    val heading: Float,
    /** How far the coil reaches from its centre (to the middle of the outer ring). */
    val reach: Float,
) {
    companion object {
        /** Gap between rings of the coil, in body radii (so the rings don't touch). */
        private const val RING_GAP_RADII = 2.4f

        /** The innermost ring, in body radii from the centre. */
        private const val INNER_RADII = 2f

        /** A spiral of total length at least [length] around ([cx], [cy]), head first, the head inside if [headInside]. */
        fun around(
            cx: Float,
            cy: Float,
            radius: Float,
            length: Float,
            headInside: Boolean = false,
        ): Coil {
            val b = RING_GAP_RADII * radius / (2f * PI.toFloat()) // ring gap per radian
            var r = INNER_RADII * radius
            var angle = 0f
            var walked = 0f
            val xs = ArrayList<Float>()
            val ys = ArrayList<Float>()
            xs += cx + r
            ys += cy
            while (walked < length) {
                // Step one trail spacing along the curve.
                angle += GameConfig.TRAIL_SPACING / sqrt(r * r + b * b)
                r = INNER_RADII * radius + b * angle
                xs += cx + r * cos(angle)
                ys += cy + r * sin(angle)
                walked += GameConfig.TRAIL_SPACING
            }
            // Built from the centre out: the outer end is the head unless it stays inside.
            if (!headInside) {
                xs.reverse()
                ys.reverse()
            }
            val heading = atan2(ys[0] - ys[1], xs[0] - xs[1])
            return Coil(xs.toFloatArray(), ys.toFloatArray(), xs.size, heading, r)
        }
    }
}
