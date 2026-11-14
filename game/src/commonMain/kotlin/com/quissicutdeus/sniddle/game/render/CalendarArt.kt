package com.quissicutdeus.sniddle.game.render

import com.quissicutdeus.sniddle.game.model.Snail
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Looks for days on the calendar: the shamrock hat (St Patrick's), bunny
 * ears (Easter), the birthday cake (drawn for [HatArt.drawHat]) and the
 * Valentine's hearts pattern. Like [HatArt]'s own, each hat is centred on its
 * brim at (x, y) and sized by the head radius u.
 */
internal object CalendarArt {
    private val path = Path2D()

    /** A green leprechaun hat with a gold buckle, and a shamrock tucked in the band. */
    fun drawShamrock(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        wash: Float,
    ) {
        val green = Colors.washed(SHAMROCK_GREEN, wash)
        val lw = max(1f, u * 0.06f)
        path.reset()
        path.moveTo(x - 0.4f * u, y)
        path.lineTo(x - 0.3f * u, y - 0.78f * u)
        path.lineTo(x + 0.3f * u, y - 0.78f * u)
        path.lineTo(x + 0.4f * u, y)
        path.close()
        c.fillPath(path, green)
        c.strokePath(path, lw, Colors.washed(Colors.darken(SHAMROCK_GREEN, 0.5f), wash))
        c.fillRect(x - 0.37f * u, y - 0.3f * u, 0.74f * u, 0.16f * u, Colors.washed(Colors.INK, wash))
        // The buckle: a gold square with a hole in it.
        c.fillRect(x - 0.11f * u, y - 0.33f * u, 0.22f * u, 0.22f * u, Colors.washed(Colors.GOLD, wash))
        c.fillRect(x - 0.05f * u, y - 0.27f * u, 0.1f * u, 0.1f * u, Colors.washed(Colors.INK, wash))
        c.fillRoundRect(x - 0.66f * u, y - 0.08f * u, 1.32f * u, 0.17f * u, 0.08f * u, green)
        // Three round leaves and a stem, on the band at the side.
        val sx = x + 0.3f * u
        val sy = y - 0.5f * u
        val leaf = Colors.washed(LEAF_GREEN, wash)
        c.line(sx, sy, sx + 0.08f * u, sy + 0.22f * u, max(1f, u * 0.05f), leaf)
        c.fillCircle(sx, sy - 0.1f * u, 0.1f * u, leaf)
        c.fillCircle(sx - 0.1f * u, sy + 0.03f * u, 0.1f * u, leaf)
        c.fillCircle(sx + 0.1f * u, sy + 0.03f * u, 0.1f * u, leaf)
    }

    /** Two tall bunny ears, pink inside, leaning out a little. */
    fun drawBunny(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        wash: Float,
    ) {
        val edge = Colors.washed(BUNNY_EDGE, wash)
        val fur = Colors.washed(Colors.WHITE, wash)
        val pink = Colors.washed(BUNNY_PINK, wash)
        val lw = max(1f, u * 0.06f)
        for (side in SIDES) {
            c.save()
            c.translate(x + side * 0.24f * u, y + 0.05f * u)
            c.rotate(side * 0.22f)
            c.fillRoundRect(-0.17f * u - lw, -1.2f * u - lw, 0.34f * u + 2 * lw, 1.2f * u + 2 * lw, 0.17f * u + lw, edge)
            c.fillRoundRect(-0.17f * u, -1.2f * u, 0.34f * u, 1.2f * u, 0.17f * u, fur)
            c.fillRoundRect(-0.08f * u, -1.02f * u, 0.16f * u, 0.82f * u, 0.08f * u, pink)
            c.restore()
        }
    }

    /** A little pink-iced cake with three lit candles: the hat for a kid's own birthday. */
    fun drawBirthdayCake(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        wash: Float,
    ) {
        val lw = max(1f, u * 0.06f)
        val ink = Colors.washed(Colors.INK, wash)
        // Candles first, so the cake covers their bottoms.
        for (k in -1..1) {
            val cx = x + k * 0.28f * u
            val top = y - 0.95f * u + (if (k == 0) -0.08f * u else 0f)
            c.fillRect(cx - 0.05f * u, top, 0.1f * u, y - 0.5f * u - top, Colors.washed(CANDLES[k + 1], wash))
            c.fillCircle(cx, top - 0.09f * u, 0.09f * u, Colors.washed(FLAME_ORANGE, wash))
            c.fillCircle(cx, top - 0.07f * u, 0.045f * u, Colors.washed(FLAME_YELLOW, wash))
        }
        path.reset()
        path.moveTo(x - 0.5f * u, y)
        path.lineTo(x - 0.5f * u, y - 0.55f * u)
        path.lineTo(x + 0.5f * u, y - 0.55f * u)
        path.lineTo(x + 0.5f * u, y)
        path.close()
        c.fillPath(path, Colors.washed(SPONGE, wash))
        c.strokePath(path, lw, ink)
        // Icing on top, dripping down.
        val icing = Colors.washed(ICING_PINK, wash)
        c.fillRect(x - 0.5f * u, y - 0.55f * u, u, 0.17f * u, icing)
        for (k in 0..3) c.fillCircle(x - 0.36f * u + k * 0.24f * u, y - 0.37f * u, 0.08f * u, icing)
        for (k in 0..2) c.fillCircle(x - 0.24f * u + k * 0.24f * u, y - 0.16f * u, 0.045f * u, Colors.washed(SPRINKLES[k], wash))
        c.fillRoundRect(x - 0.64f * u, y - 0.04f * u, 1.28f * u, 0.13f * u, 0.06f * u, Colors.washed(PLATE, wash))
    }

    /** The hearts pattern: little red hearts along the body, every few circles, swapping sides. */
    fun drawHearts(
        c: Canvas2D,
        s: Snail,
        wash: Float,
    ) {
        val color = Colors.washed(HEART_RED, wash)
        for (i in s.segCount - 2 downTo 1) {
            if (i % 5 != 3 || !SnailArt.onScreen(s, i)) continue
            // Across the body here, from the circles either side.
            val tx = s.drawX[i - 1] - s.drawX[i + 1]
            val ty = s.drawY[i - 1] - s.drawY[i + 1]
            val len = max(1e-4f, sqrt(tx * tx + ty * ty))
            val off = (if ((i / 5) % 2 == 0) 1f else -1f) * s.segR[i] * 0.28f
            heart(s.drawX[i] - ty / len * off, s.drawY[i] + tx / len * off, s.segR[i] * 0.55f)
            c.fillPath(path, color)
        }
    }

    /** Puts an upright heart of about radius [r] centred at (cx, cy) in [path]. */
    private fun heart(
        cx: Float,
        cy: Float,
        r: Float,
    ) {
        path.reset()
        path.moveTo(cx, cy + 0.9f * r)
        path.cubicTo(cx - 1.3f * r, cy + 0.1f * r, cx - 0.7f * r, cy - 0.95f * r, cx, cy - 0.35f * r)
        path.cubicTo(cx + 0.7f * r, cy - 0.95f * r, cx + 1.3f * r, cy + 0.1f * r, cx, cy + 0.9f * r)
        path.close()
    }

    /** The hearts pattern's two body colours: pink, and a pale pink-white. */
    val HEART_BANDS = intArrayOf(0xFFFF8FC0.toInt(), 0xFFFFF0F6.toInt())
    const val HEART_RED = 0xFFE5304F.toInt()

    private const val SHAMROCK_GREEN = 0xFF2E9E4F.toInt()
    private const val LEAF_GREEN = 0xFF7EE07E.toInt()
    private const val BUNNY_EDGE = 0xFF9E9EB0.toInt()
    private const val BUNNY_PINK = 0xFFFFB3CF.toInt()
    private const val SPONGE = 0xFFFFF1D6.toInt()
    private const val ICING_PINK = 0xFFFF6FB5.toInt()
    private const val PLATE = 0xFFE3E7F0.toInt()
    private const val FLAME_ORANGE = 0xFFFF9F1C.toInt()
    private const val FLAME_YELLOW = 0xFFFFF59D.toInt()
    private val CANDLES = intArrayOf(0xFF3FA9F5.toInt(), 0xFFFFD93D.toInt(), 0xFF2ECC71.toInt())
    private val SPRINKLES = intArrayOf(0xFF3FA9F5.toInt(), 0xFFFFD93D.toInt(), 0xFF9B59B6.toInt())
}
