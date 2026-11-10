package com.quissicutdeus.snake.game.model

import kotlin.math.PI
import kotlin.math.sqrt

// Small geometry helpers on plain floats (no allocation in the hot loops).

const val TWO_PI = (2.0 * PI).toFloat()
const val PI_F = PI.toFloat()

fun dist2(
    ax: Float,
    ay: Float,
    bx: Float,
    by: Float,
): Float {
    val dx = ax - bx
    val dy = ay - by
    return dx * dx + dy * dy
}

fun dist(
    ax: Float,
    ay: Float,
    bx: Float,
    by: Float,
): Float = sqrt(dist2(ax, ay, bx, by))

/** Wraps an angle into [-PI, PI). */
fun wrapAngle(a: Float): Float {
    var r = a % TWO_PI
    if (r < -PI_F) r += TWO_PI
    if (r >= PI_F) r -= TWO_PI
    return r
}

/** Signed smallest difference to - from, in [-PI, PI). */
fun angleDiff(
    from: Float,
    to: Float,
): Float = wrapAngle(to - from)
