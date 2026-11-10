package com.quissicutdeus.snake.game.render

import kotlin.math.abs
import kotlin.math.roundToInt

/** ARGB color helpers. */
object Colors {
    fun argb(
        a: Int,
        r: Int,
        g: Int,
        b: Int,
    ): Int = (a shl 24) or (r shl 16) or (g shl 8) or b

    fun alpha(c: Int) = (c ushr 24) and 0xFF

    fun red(c: Int) = (c shr 16) and 0xFF

    fun green(c: Int) = (c shr 8) and 0xFF

    fun blue(c: Int) = c and 0xFF

    fun withAlpha(
        c: Int,
        a: Float,
    ): Int = (c and 0x00FFFFFF) or ((a.coerceIn(0f, 1f) * alpha(c)).roundToInt() shl 24)

    fun lerp(
        c1: Int,
        c2: Int,
        t: Float,
    ): Int {
        val k = t.coerceIn(0f, 1f)

        fun ch(
            a: Int,
            b: Int,
        ) = (a + (b - a) * k).roundToInt()
        return argb(ch(alpha(c1), alpha(c2)), ch(red(c1), red(c2)), ch(green(c1), green(c2)), ch(blue(c1), blue(c2)))
    }

    /** Washed-out version of [c] for blinking frames ([wash] 0 = unchanged, 1 = white). */
    fun washed(
        c: Int,
        wash: Float,
    ): Int = if (wash > 0f) lerp(c, WHITE, wash) else c

    fun lighten(
        c: Int,
        t: Float,
    ) = lerp(c, (c and 0xFF000000.toInt()) or 0xFFFFFF, t)

    fun darken(
        c: Int,
        t: Float,
    ) = lerp(c, c and 0xFF000000.toInt(), t)

    /** h in [0,1), s and v in [0,1]. */
    fun hsv(
        h: Float,
        s: Float,
        v: Float,
    ): Int {
        val hh = ((h % 1f) + 1f) % 1f * 6f
        val c = v * s
        val x = c * (1f - abs(hh % 2f - 1f))
        val (r, g, b) =
            when (hh.toInt()) {
                0 -> Triple(c, x, 0f)
                1 -> Triple(x, c, 0f)
                2 -> Triple(0f, c, x)
                3 -> Triple(0f, x, c)
                4 -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
        val m = v - c
        return argb(255, ((r + m) * 255).roundToInt(), ((g + m) * 255).roundToInt(), ((b + m) * 255).roundToInt())
    }

    /** Perceived brightness 0..1 (to pick dark or light details). */
    fun luminance(c: Int): Float = (0.299f * red(c) + 0.587f * green(c) + 0.114f * blue(c)) / 255f

    const val WHITE = 0xFFFFFFFF.toInt()
    const val BLACK = 0xFF000000.toInt()
    const val INK = 0xFF2B2B3A.toInt()
    const val ANGRY_RED = 0xFFE53935.toInt()
    const val GRASS = 0xFF9ED96B.toInt()
    const val GRASS_DOT = 0xFF8CCB5A.toInt()
    const val OUTSIDE = 0xFF5E9E3A.toInt()
    const val FENCE = 0xFFB5793E.toInt()
    const val FENCE_DARK = 0xFF7F522A.toInt()
    const val HEART = 0xFFFF3D5A.toInt()
    const val GOLD = 0xFFFFC928.toInt()
}

/** Left and right: -1 and +1 (pairs of eyes, bow loops). */
internal val SIDES = intArrayOf(-1, 1)
