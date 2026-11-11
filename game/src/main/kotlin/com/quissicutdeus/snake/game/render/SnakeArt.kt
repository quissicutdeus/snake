package com.quissicutdeus.snake.game.render

import com.quissicutdeus.snake.game.GameConfig
import com.quissicutdeus.snake.game.model.Eyes
import com.quissicutdeus.snake.game.model.Hat
import com.quissicutdeus.snake.game.model.PI_F
import com.quissicutdeus.snake.game.model.Pattern
import com.quissicutdeus.snake.game.model.Pellet
import com.quissicutdeus.snake.game.model.Snake
import com.quissicutdeus.snake.game.model.SnakeStyle
import com.quissicutdeus.snake.game.model.TailPiece
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** How a snake looks at the player: smaller ones are scared, bigger ones angry. */
enum class Mood { HAPPY, SCARED, ANGRY }

/**
 * Draws snakes, food and tail pieces in world coordinates. Everything is made
 * of circles and paths, so there are no image assets. Faces stay upright (a
 * cartoon face looking at you) and the pupils look where the snake is going.
 */
object SnakeArt {
    private val path = Path2D()

    /** Head radius relative to the body: big heads make faces easy to read. */
    const val HEAD_SCALE = 1.3f

    /** How much blinking frames are washed out towards white. */
    private const val BLINK_WASH = 0.6f

    /** How long the head bulges after eating. */
    private const val SQUASH_SECONDS = 0.25f

    /** Color of the body at distance [d] along it, for a snake of radius [r]. */
    fun segmentColor(
        style: SnakeStyle,
        d: Float,
        r: Float,
    ): Int =
        when (style.pattern) {
            Pattern.SOLID, Pattern.DOTS, Pattern.ZIGZAG, Pattern.STARS -> style.bodyColor
            Pattern.STRIPES -> if (band(d, r * 1.4f) % 2 == 0) style.bodyColor else style.patternColor
            Pattern.CHECKER -> if (band(d, r * 1.3f) % 2 == 0) style.bodyColor else style.patternColor
            Pattern.RAINBOW -> Colors.hsv(d / (r * 9f), 0.68f, 1f)
            Pattern.FLAG -> FLAG_COLORS[band(d, r * 1.4f).mod(FLAG_COLORS.size)]
        }

    /** Red, white and blue bands, whatever colours were picked. */
    private val FLAG_COLORS = intArrayOf(Colors.FLAG_RED, Colors.WHITE, Colors.FLAG_BLUE)

    private fun band(
        d: Float,
        size: Float,
    ) = floor(d / size).toInt()

    fun outlineColor(
        style: SnakeStyle,
        mood: Mood,
    ): Int =
        when {
            mood == Mood.ANGRY -> Colors.ANGRY_RED
            style.pattern == Pattern.FLAG -> Colors.darken(Colors.FLAG_BLUE, 0.45f)
            else -> Colors.darken(style.bodyColor, 0.45f)
        }

    /**
     * @param blink draw washed-out (the "hurt, can't be hurt again" flashing frame)
     * @param alpha how far between the previous and current step to draw (smooth motion)
     */
    fun drawSnake(
        c: Canvas2D,
        s: Snake,
        mood: Mood,
        time: Float,
        blink: Boolean = false,
        alpha: Float = 1f,
    ) {
        if (s.segCount == 0) return
        s.prepareDraw(alpha)
        val r = s.radius
        val wash = if (blink) BLINK_WASH else 0f
        val ow = if (mood == Mood.ANGRY) max(2.5f, r * 0.26f) else max(1.5f, r * 0.16f)
        val outline = Colors.washed(outlineColor(s.style, mood), wash)
        PowerUpArt.drawAuras(c, s, time)
        drawBody(c, s, ow, outline, wash)
        when (s.style.pattern) {
            Pattern.DOTS -> {
                drawDots(c, s, wash)
            }

            Pattern.ZIGZAG -> {
                drawZigzag(c, s, wash)
            }

            Pattern.CHECKER -> {
                drawChecker(c, s, wash)
            }

            Pattern.STARS -> {
                drawStars(c, s, wash)
            }

            else -> {} // solid, stripes and rainbow are just body colors
        }
        drawGloss(c, s)
        drawHead(c, s, mood, time, ow, outline, wash)
        PowerUpArt.drawShield(c, s, time)
    }

    /** Outline (slightly bigger circles behind everything), then the body, tail first so the front overlaps the back. */
    private fun drawBody(
        c: Canvas2D,
        s: Snake,
        ow: Float,
        outline: Int,
        wash: Float,
    ) {
        val n = s.segCount
        for (i in n - 1 downTo 1) c.fillCircle(s.drawX[i], s.drawY[i], s.segR[i] + ow, outline)
        for (i in n - 1 downTo 1) {
            c.fillCircle(s.drawX[i], s.drawY[i], s.segR[i], Colors.washed(segmentColor(s.style, s.segD[i], s.radius), wash))
        }
    }

    private fun drawDots(
        c: Canvas2D,
        s: Snake,
        wash: Float,
    ) {
        val color = Colors.washed(s.style.patternColor, wash)
        for (i in s.segCount - 2 downTo 1) {
            if (i % 4 != 2) continue
            perp(s, i)
            val side = if ((i / 4) % 2 == 0) 1f else -1f
            val off = side * s.segR[i] * 0.42f
            c.fillCircle(s.drawX[i] + perpX * off, s.drawY[i] + perpY * off, s.segR[i] * 0.3f, color)
        }
    }

    private fun drawZigzag(
        c: Canvas2D,
        s: Snake,
        wash: Float,
    ) {
        if (s.segCount < 4) return
        path.reset()
        var k = 0
        var i = 2
        while (i < s.segCount - 1) {
            perp(s, i)
            val off = (if (k % 2 == 0) 0.5f else -0.5f) * s.segR[i]
            val x = s.drawX[i] + perpX * off
            val y = s.drawY[i] + perpY * off
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
            k++
            i += 2
        }
        c.strokePath(path, max(1.5f, s.radius * 0.28f), Colors.washed(s.style.patternColor, wash))
    }

    private fun drawChecker(
        c: Canvas2D,
        s: Snake,
        wash: Float,
    ) {
        val style = s.style
        for (i in s.segCount - 1 downTo 1) {
            perp(s, i)
            val other = if (band(s.segD[i], s.radius * 1.3f) % 2 == 0) style.patternColor else style.bodyColor
            val off = s.segR[i] * 0.48f
            c.fillCircle(s.drawX[i] + perpX * off, s.drawY[i] + perpY * off, s.segR[i] * 0.52f, Colors.washed(other, wash))
        }
    }

    /** Little five-pointed stars along the body, every few circles, swapping sides. */
    private fun drawStars(
        c: Canvas2D,
        s: Snake,
        wash: Float,
    ) {
        val color = Colors.washed(s.style.patternColor, wash)
        for (i in s.segCount - 2 downTo 1) {
            if (i % 5 != 3) continue
            perp(s, i)
            val off = (if ((i / 5) % 2 == 0) 1f else -1f) * s.segR[i] * 0.3f
            val r = s.segR[i] * 0.62f
            val cx = s.drawX[i] + perpX * off
            val cy = s.drawY[i] + perpY * off
            path.reset()
            for (k in 0 until 10) {
                val a = -PI_F / 2f + k * PI_F / 5f
                val rr = if (k % 2 == 0) r else r * 0.45f
                if (k == 0) path.moveTo(cx + cos(a) * rr, cy + sin(a) * rr) else path.lineTo(cx + cos(a) * rr, cy + sin(a) * rr)
            }
            path.close()
            c.fillPath(path, color)
        }
    }

    /** Glossy highlight: one soft stripe along the top-left of the tube. */
    private fun drawGloss(
        c: Canvas2D,
        s: Snake,
    ) {
        val glossEnd = (s.segCount * 0.85f).toInt()
        if (glossEnd <= 2) return
        path.reset()
        path.moveTo(s.drawX[1] - s.segR[1] * 0.3f, s.drawY[1] - s.segR[1] * 0.36f)
        for (i in 2 until glossEnd) path.lineTo(s.drawX[i] - s.segR[i] * 0.3f, s.drawY[i] - s.segR[i] * 0.36f)
        c.strokePath(path, s.radius * 0.32f, Colors.withAlpha(Colors.WHITE, 0.32f))
    }

    private var perpX = 0f
    private var perpY = 0f

    /** Sets [perpX]/[perpY] to the unit vector perpendicular to the body at circle [i]. */
    private fun perp(
        s: Snake,
        i: Int,
    ) {
        val a = max(0, i - 1)
        val b = min(s.segCount - 1, i + 1)
        val tx = s.drawX[a] - s.drawX[b]
        val ty = s.drawY[a] - s.drawY[b]
        val len = sqrt(tx * tx + ty * ty)
        if (len < 1e-4f) {
            perpX = -sin(s.heading)
            perpY = cos(s.heading)
        } else {
            perpX = -ty / len
            perpY = tx / len
        }
    }

    private fun drawHead(
        c: Canvas2D,
        s: Snake,
        mood: Mood,
        time: Float,
        ow: Float,
        outline: Int,
        wash: Float,
    ) {
        val style = s.style
        // Squash & stretch right after eating.
        val squash = if (s.sinceAte < SQUASH_SECONDS) 1f + 0.22f * sin(PI_F * s.sinceAte / SQUASH_SECONDS) else 1f
        val hr = s.radius * HEAD_SCALE * squash
        val hx = s.drawX[0]
        val hy = s.drawY[0]
        val headColor = Colors.washed(segmentColor(style, 0f, s.radius), wash)

        c.fillCircle(hx, hy, hr + ow, outline)
        c.fillCircle(hx, hy, hr, headColor)
        c.fillCircle(hx - hr * 0.32f, hy - hr * 0.38f, hr * 0.28f, Colors.withAlpha(Colors.lighten(headColor, 0.6f), 0.7f))

        // The face sits a little towards where the snake is going.
        val fx = hx + cos(s.heading) * hr * 0.15f
        val fy = hy + sin(s.heading) * hr * 0.15f
        FaceArt.drawFace(c, FaceArt.Face(fx, fy, hr, mood, s.heading, time + s.id * 1.7f, headColor, wash), style.eyes)
        HatArt.drawHat(c, fx, fy - hr * 0.74f, hr, style.hat, style, wash)
    }
}
