package com.snakearena.game.model

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Immutable 2D point/vector in world units. */
data class Vec2(val x: Float, val y: Float) {
    operator fun plus(o: Vec2) = Vec2(x + o.x, y + o.y)
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    operator fun times(s: Float) = Vec2(x * s, y * s)
    fun length(): Float = sqrt(x * x + y * y)
    fun distanceTo(o: Vec2): Float = dist(x, y, o.x, o.y)
    fun angle(): Float = atan2(y, x)

    companion object {
        val ZERO = Vec2(0f, 0f)
        fun fromAngle(angle: Float, length: Float = 1f) = Vec2(cos(angle) * length, sin(angle) * length)
    }
}

const val TWO_PI = (2.0 * PI).toFloat()
const val PI_F = PI.toFloat()

fun dist2(ax: Float, ay: Float, bx: Float, by: Float): Float {
    val dx = ax - bx
    val dy = ay - by
    return dx * dx + dy * dy
}

fun dist(ax: Float, ay: Float, bx: Float, by: Float): Float = sqrt(dist2(ax, ay, bx, by))

/** Wraps an angle into [-PI, PI). */
fun wrapAngle(a: Float): Float {
    var r = a % TWO_PI
    if (r < -PI_F) r += TWO_PI
    if (r >= PI_F) r -= TWO_PI
    return r
}

/** Signed smallest difference to - from, in [-PI, PI). */
fun angleDiff(from: Float, to: Float): Float = wrapAngle(to - from)
