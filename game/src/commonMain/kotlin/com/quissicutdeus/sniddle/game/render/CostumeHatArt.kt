package com.quissicutdeus.sniddle.game.render

import com.quissicutdeus.sniddle.game.model.Hat
import com.quissicutdeus.sniddle.game.model.PI_F
import com.quissicutdeus.sniddle.game.model.SnailStyle
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Dressing-up hats: cowboy hat, flower crown, propeller cap and pirate hat.
 * Like [HatArt], each is centred on its brim at (x, y) and sized by the head
 * radius u.
 */
internal object CostumeHatArt {
    private val path = Path2D()

    /** [Hat.COWBOY], [Hat.FLOWERS], [Hat.PROPELLER] or [Hat.PIRATE] at (x, y) for a head of radius [u]. */
    @Suppress("LongParameterList") // position, size and look of the hat
    fun drawHat(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        hat: Hat,
        style: SnailStyle,
        wash: Float,
    ) {
        when (hat) {
            Hat.COWBOY -> {
                drawCowboy(c, x, y, u, style, wash)
            }

            Hat.FLOWERS -> {
                drawFlowers(c, x, y, u, wash)
            }

            Hat.PROPELLER -> {
                drawPropeller(c, x, y, u, wash)
            }

            Hat.PIRATE -> {
                drawPirate(c, x, y, u, wash)
            }

            else -> {}
        }
    }

    /** A brown hat with a dented top, a brim curling up at both ends and a band in the snail's pattern colour. */
    @Suppress("LongParameterList") // position, size and look of the hat
    private fun drawCowboy(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        style: SnailStyle,
        wash: Float,
    ) {
        val tan = Colors.washed(COWBOY_BROWN, wash)
        val edge = Colors.washed(Colors.darken(COWBOY_BROWN, 0.5f), wash)
        val lw = max(1f, u * 0.06f)
        path.reset()
        path.moveTo(x - 0.36f * u, y - 0.05f * u)
        path.lineTo(x - 0.42f * u, y - 0.58f * u)
        path.quadTo(x - 0.22f * u, y - 0.8f * u, x, y - 0.62f * u)
        path.quadTo(x + 0.22f * u, y - 0.8f * u, x + 0.42f * u, y - 0.58f * u)
        path.lineTo(x + 0.36f * u, y - 0.05f * u)
        path.close()
        c.fillPath(path, tan)
        c.strokePath(path, lw, edge)
        c.fillRect(x - 0.38f * u, y - 0.26f * u, 0.76f * u, 0.14f * u, Colors.washed(style.patternColor, wash))
        // The brim: wide, dipping in the middle and curling up at the ends.
        path.reset()
        path.moveTo(x - 0.86f * u, y - 0.36f * u)
        path.quadTo(x - 0.62f * u, y + 0.1f * u, x, y + 0.08f * u)
        path.quadTo(x + 0.62f * u, y + 0.1f * u, x + 0.86f * u, y - 0.36f * u)
        path.quadTo(x + 0.6f * u, y - 0.08f * u, x, y - 0.1f * u)
        path.quadTo(x - 0.6f * u, y - 0.08f * u, x - 0.86f * u, y - 0.36f * u)
        path.close()
        c.fillPath(path, tan)
        c.strokePath(path, lw, edge)
    }

    /** Five bright flowers in a row around the top of the head, on a leafy green band. */
    private fun drawFlowers(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        wash: Float,
    ) {
        val green = Colors.washed(LEAF_GREEN, wash)
        val lw = max(1f, u * 0.06f)
        path.reset()
        path.arc(x, y + 0.45f * u, 0.7f * u, PI_F * 1.15f, PI_F * 1.85f, moveFirst = true, steps = 12)
        c.strokePath(path, 0.12f * u, green)
        for (k in -1..1 step 2) {
            leaf(x + k * 0.42f * u, y - 0.16f * u, 0.2f * u, k * 0.5f)
            c.fillPath(path, green)
        }
        for (k in -2..2) {
            val fx = x + k * 0.32f * u
            val fy = y - 0.12f * u - (2 - abs(k)) * 0.07f * u
            val petal = Colors.washed(FLOWER_COLORS[k + 2], wash)
            val dark = Colors.washed(Colors.darken(FLOWER_COLORS[k + 2], 0.4f), wash)
            val r = 0.13f * u
            for (p in 0 until PETALS) {
                val a = -PI_F / 2f + p * 2f * PI_F / PETALS
                c.fillCircle(fx + cos(a) * r, fy + sin(a) * r, r + lw * 0.8f, dark)
            }
            for (p in 0 until PETALS) {
                val a = -PI_F / 2f + p * 2f * PI_F / PETALS
                c.fillCircle(fx + cos(a) * r, fy + sin(a) * r, r, petal)
            }
            c.fillCircle(fx, fy, 0.08f * u, Colors.washed(FLOWER_MIDDLE, wash))
        }
    }

    /** Puts a pointed leaf of length [len] at (cx, cy), tilted by [tilt], in [path]. */
    private fun leaf(
        cx: Float,
        cy: Float,
        len: Float,
        tilt: Float,
    ) {
        val dx = cos(tilt) * len
        val dy = sin(tilt) * len
        path.reset()
        path.moveTo(cx - dx, cy - dy)
        path.quadTo(cx - dy * 0.6f, cy + dx * 0.6f, cx + dx, cy + dy)
        path.quadTo(cx + dy * 0.6f, cy - dx * 0.6f, cx - dx, cy - dy)
        path.close()
    }

    /** A round cap in red, yellow and blue panels with a little peak, and a propeller on top. */
    private fun drawPropeller(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        wash: Float,
    ) {
        val lw = max(1f, u * 0.06f)
        val edge = Colors.washed(Colors.INK, wash)
        // The peak sticks out to the front (right), under the cap.
        c.fillRoundRect(x + 0.1f * u, y - 0.1f * u, 0.62f * u, 0.16f * u, 0.08f * u, Colors.washed(CAP_BLUE, wash))
        val r = 0.5f * u
        path.reset()
        path.arc(x, y, r, PI_F, 2f * PI_F, moveFirst = true)
        path.close()
        c.fillPath(path, Colors.washed(CAP_RED, wash))
        // The middle panel, a lens from the top button down to the brim.
        path.reset()
        path.moveTo(x, y - r)
        path.quadTo(x + 0.3f * u, y - 0.3f * u, x + 0.2f * u, y)
        path.lineTo(x - 0.2f * u, y)
        path.quadTo(x - 0.3f * u, y - 0.3f * u, x, y - r)
        path.close()
        c.fillPath(path, Colors.washed(CAP_YELLOW, wash))
        path.reset()
        path.arc(x, y, r, PI_F, 2f * PI_F, moveFirst = true)
        path.close()
        c.strokePath(path, lw, edge)
        // The stalk and the two blades, tilted a little as if spinning.
        val top = y - 0.78f * u
        c.line(x, y - r, x, top, 0.08f * u, edge)
        c.save()
        c.translate(x, top)
        c.rotate(-0.15f)
        for (side in SIDES) {
            path.reset()
            path.moveTo(0f, 0f)
            path.quadTo(side * 0.25f * u, -0.17f * u, side * 0.52f * u, -0.02f * u)
            path.quadTo(side * 0.25f * u, 0.11f * u, 0f, 0f)
            path.close()
            val blade = if (side < 0) CAP_RED else CAP_BLUE
            c.fillPath(path, Colors.washed(blade, wash))
            c.strokePath(path, lw * 0.8f, edge)
        }
        c.restore()
        c.fillCircle(x, top, 0.08f * u, Colors.washed(CAP_YELLOW, wash))
        c.strokeCircle(x, top, 0.08f * u, lw * 0.8f, edge)
    }

    /** A black pirate hat curving up at both ends, with a gold edge and a white skull and crossbones. */
    private fun drawPirate(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        wash: Float,
    ) {
        val black = Colors.washed(PIRATE_BLACK, wash)
        val gold = Colors.washed(Colors.GOLD, wash)
        val white = Colors.washed(Colors.WHITE, wash)
        val lw = max(1f, u * 0.06f)
        path.reset()
        path.moveTo(x - 0.85f * u, y - 0.12f * u)
        path.quadTo(x - 0.6f * u, y - 0.95f * u, x, y - 0.86f * u)
        path.quadTo(x + 0.6f * u, y - 0.95f * u, x + 0.85f * u, y - 0.12f * u)
        path.quadTo(x, y - 0.2f * u, x - 0.85f * u, y - 0.12f * u)
        path.close()
        c.fillPath(path, black)
        c.strokePath(path, lw, Colors.washed(Colors.INK, wash))
        // Gold trim along the bottom edge.
        path.reset()
        path.moveTo(x - 0.82f * u, y - 0.16f * u)
        path.quadTo(x, y - 0.24f * u, x + 0.82f * u, y - 0.16f * u)
        c.strokePath(path, 0.1f * u, gold)
        // Crossbones behind the skull, then the skull with two dark eyes.
        val sy = y - 0.56f * u
        val bw = 0.08f * u
        c.line(x - 0.28f * u, sy + 0.18f * u, x + 0.28f * u, sy - 0.02f * u, bw, white)
        c.line(x - 0.28f * u, sy - 0.02f * u, x + 0.28f * u, sy + 0.18f * u, bw, white)
        c.fillCircle(x, sy, 0.17f * u, white)
        c.fillRoundRect(x - 0.1f * u, sy + 0.06f * u, 0.2f * u, 0.14f * u, 0.05f * u, white)
        c.fillCircle(x - 0.065f * u, sy - 0.01f * u, 0.05f * u, black)
        c.fillCircle(x + 0.065f * u, sy - 0.01f * u, 0.05f * u, black)
    }

    private const val PETALS = 5
    private const val COWBOY_BROWN = 0xFFB07A45.toInt()
    private const val LEAF_GREEN = 0xFF3E9B3A.toInt()
    private const val FLOWER_MIDDLE = 0xFFFFC928.toInt()
    private const val CAP_RED = 0xFFE53935.toInt()
    private const val CAP_YELLOW = 0xFFFFD93D.toInt()
    private const val CAP_BLUE = 0xFF3D7BF5.toInt()
    private const val PIRATE_BLACK = 0xFF24242C.toInt()
    private val FLOWER_COLORS =
        intArrayOf(
            0xFFFF6FB5.toInt(), // pink
            0xFFFFFFFF.toInt(), // white
            0xFFB06BE8.toInt(), // lilac
            0xFFFF9F1C.toInt(), // orange
            0xFF7FD3FF.toInt(), // sky blue
        )
}
