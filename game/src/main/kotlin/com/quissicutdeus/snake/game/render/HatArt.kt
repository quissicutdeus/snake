package com.quissicutdeus.snake.game.render

import com.quissicutdeus.snake.game.model.Hat
import com.quissicutdeus.snake.game.model.SnakeStyle
import kotlin.math.max

/** Hats drawn on top of a snake's head: crown, party hat or bow. */
object HatArt {
    private val path = Path2D()

    /** A hat whose brim is centred at (x, y), sized for a head of radius [hr]. */
    @Suppress("LongParameterList") // position, size and look of the hat
    fun drawHat(
        c: Canvas2D,
        x: Float,
        y: Float,
        hr: Float,
        hat: Hat,
        style: SnakeStyle,
        wash: Float = 0f,
    ) {
        when (hat) {
            Hat.NONE -> {}

            Hat.CROWN -> {
                drawCrown(c, x, y, hr, wash)
            }

            Hat.PARTY -> {
                drawPartyHat(c, x, y, hr, style, wash)
            }

            Hat.BOW -> {
                drawBow(c, x, y, hr, style, wash)
            }
        }
    }

    private fun drawCrown(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        wash: Float,
    ) {
        path.reset()
        path.moveTo(x - 0.55f * u, y)
        path.lineTo(x - 0.6f * u, y - 0.5f * u)
        path.lineTo(x - 0.28f * u, y - 0.24f * u)
        path.lineTo(x, y - 0.64f * u)
        path.lineTo(x + 0.28f * u, y - 0.24f * u)
        path.lineTo(x + 0.6f * u, y - 0.5f * u)
        path.lineTo(x + 0.55f * u, y)
        path.close()
        c.fillPath(path, Colors.washed(Colors.GOLD, wash))
        c.strokePath(path, max(1f, u * 0.06f), Colors.washed(0xFFB8860B.toInt(), wash))
        val tip = Colors.washed(0xFFFFF59D.toInt(), wash)
        c.fillCircle(x - 0.6f * u, y - 0.52f * u, 0.08f * u, tip)
        c.fillCircle(x, y - 0.66f * u, 0.09f * u, tip)
        c.fillCircle(x + 0.6f * u, y - 0.52f * u, 0.08f * u, tip)
        c.fillCircle(x, y - 0.14f * u, 0.09f * u, Colors.washed(0xFFE53935.toInt(), wash))
        c.fillCircle(x - 0.32f * u, y - 0.1f * u, 0.06f * u, Colors.washed(0xFF3FA9F5.toInt(), wash))
        c.fillCircle(x + 0.32f * u, y - 0.1f * u, 0.06f * u, Colors.washed(0xFF3FA9F5.toInt(), wash))
    }

    @Suppress("LongParameterList") // position, size and look of the hat
    private fun drawPartyHat(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        style: SnakeStyle,
        wash: Float,
    ) {
        c.save()
        c.translate(x + 0.12f * u, y)
        c.rotate(0.28f)
        val hw = 0.4f * u
        val h = 1.05f * u
        path.reset()
        path.moveTo(-hw, 0f)
        path.lineTo(hw, 0f)
        path.lineTo(0f, -h)
        path.close()
        c.fillPath(path, Colors.washed(style.patternColor, wash))
        c.strokePath(path, max(1f, u * 0.06f), Colors.washed(Colors.darken(style.patternColor, 0.45f), wash))
        // Two stripes across the cone.
        val stripe = Colors.washed(if (Colors.luminance(style.patternColor) > 0.75f) Colors.HEART else Colors.WHITE, wash)
        for (t in floatArrayOf(0.22f, 0.55f)) {
            val t2 = t + 0.14f
            path.reset()
            path.moveTo(-hw * (1 - t), -h * t)
            path.lineTo(hw * (1 - t), -h * t)
            path.lineTo(hw * (1 - t2), -h * t2)
            path.lineTo(-hw * (1 - t2), -h * t2)
            path.close()
            c.fillPath(path, stripe)
        }
        c.fillCircle(0f, -h, 0.15f * u, Colors.washed(Colors.GOLD, wash))
        c.restore()
    }

    @Suppress("LongParameterList") // position, size and look of the hat
    private fun drawBow(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        style: SnakeStyle,
        wash: Float,
    ) {
        c.save()
        c.translate(x + 0.42f * u, y + 0.05f * u)
        c.rotate(0.35f)
        val bowColor = Colors.washed(style.patternColor, wash)
        val dark = Colors.washed(Colors.darken(style.patternColor, 0.45f), wash)
        val lw = max(1f, u * 0.06f)
        for (side in SIDES) {
            path.reset()
            path.moveTo(0f, 0f)
            path.quadTo(side * 0.35f * u, -0.45f * u, side * 0.55f * u, -0.3f * u)
            path.lineTo(side * 0.55f * u, 0.3f * u)
            path.quadTo(side * 0.35f * u, 0.45f * u, 0f, 0f)
            path.close()
            c.fillPath(path, bowColor)
            c.strokePath(path, lw, dark)
        }
        c.fillCircle(0f, 0f, 0.15f * u, bowColor)
        c.strokeCircle(0f, 0f, 0.15f * u, lw, dark)
        c.restore()
    }
}
