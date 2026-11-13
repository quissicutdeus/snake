package com.quissicutdeus.sniddle.game.render

import com.quissicutdeus.sniddle.game.model.PI_F
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * The holiday hats: Santa hat, turkey, ghost and a star-spangled top hat.
 * Like [HatArt], each is centred on its brim at (x, y) and sized by the head
 * radius u.
 */
internal object HolidayHatArt {
    private val path = Path2D()

    /** A red hat flopping over to one side, with a fluffy white brim and bobble. */
    fun drawSanta(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        wash: Float,
    ) {
        val red = Colors.washed(SANTA_RED, wash)
        val white = Colors.washed(Colors.WHITE, wash)
        path.reset()
        path.moveTo(x - 0.5f * u, y)
        path.quadTo(x - 0.3f * u, y - 1.0f * u, x + 0.25f * u, y - 0.95f * u)
        path.quadTo(x + 0.6f * u, y - 0.9f * u, x + 0.75f * u, y - 0.45f * u)
        path.quadTo(x + 0.45f * u, y - 0.6f * u, x + 0.5f * u, y)
        path.close()
        c.fillPath(path, red)
        c.strokePath(path, max(1f, u * 0.06f), Colors.washed(Colors.darken(SANTA_RED, 0.45f), wash))
        // Fluffy brim: a row of overlapping puffs.
        for (k in -3..3) c.fillCircle(x + k * 0.17f * u, y - 0.02f * u, 0.15f * u, white)
        c.fillCircle(x + 0.75f * u, y - 0.42f * u, 0.17f * u, white)
    }

    /** A turkey sitting on the head: a fan of tail feathers behind a round brown body and a little face. */
    fun drawTurkey(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        wash: Float,
    ) {
        val cx = x
        val cy = y - 0.25f * u
        for (k in FEATHERS.indices) {
            val a = PI_F + (k + 0.5f) * PI_F / FEATHERS.size
            val fx = cx + cos(a) * 0.62f * u
            val fy = cy + sin(a) * 0.62f * u
            c.save()
            c.translate(fx, fy)
            c.rotate(a + PI_F / 2f)
            c.fillRoundRect(-0.16f * u, -0.3f * u, 0.32f * u, 0.6f * u, 0.16f * u, Colors.washed(FEATHERS[k], wash))
            c.restore()
        }
        val brown = Colors.washed(TURKEY_BROWN, wash)
        c.fillCircle(cx, cy, 0.42f * u, brown)
        c.strokeCircle(cx, cy, 0.42f * u, max(1f, u * 0.06f), Colors.washed(Colors.darken(TURKEY_BROWN, 0.45f), wash))
        // Head, beak and the dangly red wattle.
        val hx = cx
        val hy = cy - 0.32f * u
        c.fillCircle(hx, hy, 0.2f * u, Colors.washed(TURKEY_HEAD, wash))
        c.fillCircle(hx - 0.07f * u, hy - 0.05f * u, 0.045f * u, Colors.washed(Colors.INK, wash))
        c.fillCircle(hx + 0.07f * u, hy - 0.05f * u, 0.045f * u, Colors.washed(Colors.INK, wash))
        path.reset()
        path.moveTo(hx - 0.06f * u, hy + 0.03f * u)
        path.lineTo(hx + 0.06f * u, hy + 0.03f * u)
        path.lineTo(hx, hy + 0.14f * u)
        path.close()
        c.fillPath(path, Colors.washed(Colors.GOLD, wash))
        c.fillCircle(hx + 0.05f * u, hy + 0.18f * u, 0.06f * u, Colors.washed(SANTA_RED, wash))
    }

    /** A friendly little sheet ghost sitting on the head, with a wavy hem. */
    fun drawGhost(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        wash: Float,
    ) {
        val hw = 0.42f * u
        val top = y - 1.05f * u
        path.reset()
        path.moveTo(x - hw, y)
        path.lineTo(x - hw, top + hw)
        path.arc(x, top + hw, hw, PI_F, 2f * PI_F)
        path.lineTo(x + hw, y)
        // Three scallops along the bottom.
        val w = 2f * hw / 3f
        for (k in 0 until 3) {
            val x0 = x + hw - k * w
            path.quadTo(x0 - w / 2f, y - 0.22f * u, x0 - w, y)
        }
        path.close()
        c.fillPath(path, Colors.washed(GHOST_WHITE, wash))
        c.strokePath(path, max(1f, u * 0.06f), Colors.washed(GHOST_EDGE, wash))
        val ink = Colors.washed(Colors.INK, wash)
        val ey = top + 0.42f * u
        c.fillCircle(x - 0.14f * u, ey, 0.08f * u, ink)
        c.fillCircle(x + 0.14f * u, ey, 0.08f * u, ink)
        c.fillCircle(x, ey + 0.2f * u, 0.07f * u, ink) // "Boo!"
    }

    /** A top hat in red and white stripes with a blue band and a white star. */
    fun drawStarHat(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        wash: Float,
    ) {
        val lw = max(1f, u * 0.06f)
        val red = Colors.washed(Colors.FLAG_RED, wash)
        val white = Colors.washed(Colors.WHITE, wash)
        val blue = Colors.washed(Colors.FLAG_BLUE, wash)
        val hw = 0.34f * u
        val h = 0.86f * u
        c.fillRect(x - hw, y - h, 2f * hw, h, white)
        val sw = 2f * hw / 5f
        for (k in 0 until 5 step 2) c.fillRect(x - hw + k * sw, y - h, sw, h, red)
        c.fillRect(x - hw, y - 0.34f * u, 2f * hw, 0.2f * u, blue)
        star(x, y - 0.24f * u, 0.11f * u)
        c.fillPath(path, white)
        path.reset()
        path.moveTo(x - hw, y)
        path.lineTo(x - hw, y - h)
        path.lineTo(x + hw, y - h)
        path.lineTo(x + hw, y)
        path.close()
        c.strokePath(path, lw, Colors.washed(Colors.INK, wash))
        c.fillRoundRect(x - 0.62f * u, y - 0.08f * u, 1.24f * u, 0.17f * u, 0.08f * u, blue)
    }

    /** Puts a five-pointed star of radius [r] at (cx, cy) in [path]. */
    private fun star(
        cx: Float,
        cy: Float,
        r: Float,
    ) {
        path.reset()
        for (k in 0 until 10) {
            val a = -PI_F / 2f + k * PI_F / 5f
            val rr = if (k % 2 == 0) r else r * 0.45f
            if (k == 0) path.moveTo(cx + cos(a) * rr, cy + sin(a) * rr) else path.lineTo(cx + cos(a) * rr, cy + sin(a) * rr)
        }
        path.close()
    }

    private const val SANTA_RED = 0xFFD62828.toInt()
    private const val TURKEY_BROWN = 0xFF8D5A3B.toInt()
    private const val TURKEY_HEAD = 0xFFC9A27E.toInt()
    private const val GHOST_WHITE = 0xFFF7F7FF.toInt()
    private const val GHOST_EDGE = 0xFF9FA3C7.toInt()
    private val FEATHERS =
        intArrayOf(
            0xFFE53935.toInt(),
            0xFFFF9F1C.toInt(),
            0xFFFFD93D.toInt(),
            0xFFFF9F1C.toInt(),
            0xFFE53935.toInt(),
        )
}
