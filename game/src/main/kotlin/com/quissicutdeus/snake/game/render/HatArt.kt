package com.quissicutdeus.snake.game.render

import com.quissicutdeus.snake.game.model.Hat
import com.quissicutdeus.snake.game.model.PI_F
import com.quissicutdeus.snake.game.model.SnakeStyle
import kotlin.math.max

/** Hats drawn on top of a snake's head: crown, party hat, bow, top hat, wizard hat or Viking horns. */
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

            Hat.TOP_HAT -> {
                drawTopHat(c, x, y, hr, style, wash)
            }

            Hat.WIZARD -> {
                drawWizardHat(c, x, y, hr, wash)
            }

            Hat.HORNS -> {
                drawHorns(c, x, y, hr, wash)
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

    @Suppress("LongParameterList") // position, size and look of the hat
    private fun drawTopHat(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        style: SnakeStyle,
        wash: Float,
    ) {
        val black = Colors.washed(TOP_HAT_BLACK, wash)
        val edge = Colors.washed(Colors.INK, wash)
        val lw = max(1f, u * 0.06f)
        // Tall crown, then the brim over its bottom edge, then a band in the snake's pattern colour.
        path.reset()
        path.moveTo(x - 0.34f * u, y)
        path.lineTo(x - 0.38f * u, y - 0.82f * u)
        path.lineTo(x + 0.38f * u, y - 0.82f * u)
        path.lineTo(x + 0.34f * u, y)
        path.close()
        c.fillPath(path, black)
        c.strokePath(path, lw, edge)
        c.fillRoundRect(x - 0.62f * u, y - 0.1f * u, 1.24f * u, 0.18f * u, 0.09f * u, black)
        c.fillRect(x - 0.35f * u, y - 0.3f * u, 0.7f * u, 0.15f * u, Colors.washed(style.patternColor, wash))
        c.line(x - 0.24f * u, y - 0.72f * u, x - 0.26f * u, y - 0.42f * u, u * 0.07f, Colors.withAlpha(Colors.WHITE, 0.35f))
    }

    private fun drawWizardHat(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        wash: Float,
    ) {
        val blue = Colors.washed(WIZARD_BLUE, wash)
        val lw = max(1f, u * 0.06f)
        c.save()
        c.translate(x - 0.05f * u, y)
        c.rotate(-0.18f)
        // A tall cone that flops over a little at the tip.
        path.reset()
        path.moveTo(-0.42f * u, 0f)
        path.quadTo(-0.2f * u, -0.7f * u, 0.1f * u, -1.2f * u)
        path.quadTo(0.32f * u, -1.25f * u, 0.42f * u, -1.05f * u)
        path.quadTo(0.2f * u, -0.95f * u, 0.42f * u, 0f)
        path.close()
        c.fillPath(path, blue)
        c.strokePath(path, lw, Colors.washed(Colors.darken(WIZARD_BLUE, 0.45f), wash))
        c.fillRoundRect(-0.62f * u, -0.08f * u, 1.24f * u, 0.17f * u, 0.08f * u, blue)
        val gold = Colors.washed(Colors.GOLD, wash)
        c.fillCircle(-0.05f * u, -0.45f * u, 0.08f * u, gold)
        c.fillCircle(0.18f * u, -0.25f * u, 0.06f * u, gold)
        c.fillCircle(0.08f * u, -0.78f * u, 0.06f * u, gold)
        c.fillCircle(0.42f * u, -1.05f * u, 0.1f * u, gold)
        c.restore()
    }

    private fun drawHorns(
        c: Canvas2D,
        x: Float,
        y: Float,
        u: Float,
        wash: Float,
    ) {
        val horn = Colors.washed(HORN_CREAM, wash)
        val hornEdge = Colors.washed(Colors.darken(HORN_CREAM, 0.4f), wash)
        val lw = max(1f, u * 0.06f)
        for (side in SIDES) {
            // A curved horn from the side of the helmet, sweeping out and up.
            path.reset()
            path.moveTo(x + side * 0.42f * u, y - 0.22f * u)
            path.quadTo(x + side * 0.95f * u, y - 0.3f * u, x + side * 0.9f * u, y - 0.95f * u)
            path.quadTo(x + side * 0.72f * u, y - 0.5f * u, x + side * 0.38f * u, y - 0.45f * u)
            path.close()
            c.fillPath(path, horn)
            c.strokePath(path, lw, hornEdge)
        }
        // The helmet: a metal dome with a rim and rivets.
        val metal = Colors.washed(HELMET_GREY, wash)
        path.reset()
        path.arc(x, y, 0.5f * u, PI_F, 2f * PI_F, moveFirst = true)
        path.close()
        c.fillPath(path, metal)
        c.strokePath(path, lw, Colors.washed(Colors.darken(HELMET_GREY, 0.45f), wash))
        c.fillRoundRect(
            x - 0.56f * u,
            y - 0.08f * u,
            1.12f * u,
            0.16f * u,
            0.08f * u,
            Colors.washed(Colors.darken(HELMET_GREY, 0.2f), wash),
        )
        for (k in -1..1) c.fillCircle(x + k * 0.3f * u, y, 0.04f * u, Colors.washed(Colors.WHITE, wash))
    }

    private const val TOP_HAT_BLACK = 0xFF26262E.toInt()
    private const val WIZARD_BLUE = 0xFF5E35B1.toInt()
    private const val HORN_CREAM = 0xFFFFF1D0.toInt()
    private const val HELMET_GREY = 0xFFB0BEC5.toInt()
}
