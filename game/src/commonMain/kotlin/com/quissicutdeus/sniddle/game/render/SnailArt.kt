package com.quissicutdeus.sniddle.game.render

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.Eyes
import com.quissicutdeus.sniddle.game.model.Hat
import com.quissicutdeus.sniddle.game.model.PI_F
import com.quissicutdeus.sniddle.game.model.Pattern
import com.quissicutdeus.sniddle.game.model.Pellet
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.model.TWO_PI
import com.quissicutdeus.sniddle.game.model.TailPiece
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * How a snail looks at the player: smaller ones are scared, bigger ones angry.
 * [ASLEEP] is only for pictures (the bedtime screen), never in a game.
 */
enum class Mood { HAPPY, SCARED, ANGRY, ASLEEP }

/**
 * Draws the snails in world coordinates: each is a little snail (head, shell
 * and feelers) at the front of the slime trail it has left, and the trail is
 * its length. Everything is made of circles and paths, so there are no image
 * assets. Faces stay upright (a cartoon face looking at you) and the pupils
 * look where the snail is going.
 *
 * (In the code the slime trail is the snail's "body" or "tail".)
 */
object SnailArt {
    private val path = Path2D()

    /** Head radius relative to the body: big heads make faces easy to read. */
    const val HEAD_SCALE = 1.3f

    /** How much blinking frames are washed out towards white. */
    private const val BLINK_WASH = 0.6f

    /** Color of the body at distance [d] along it, for a snail of radius [r]. */
    fun segmentColor(
        style: SnailStyle,
        d: Float,
        r: Float,
    ): Int =
        when (style.pattern) {
            Pattern.SOLID, Pattern.DOTS, Pattern.ZIGZAG, Pattern.STARS -> style.bodyColor
            Pattern.STRIPES -> if (band(d, r * 1.4f) % 2 == 0) style.bodyColor else style.patternColor
            Pattern.CHECKER -> if (band(d, r * 1.3f) % 2 == 0) style.bodyColor else style.patternColor
            Pattern.RAINBOW -> Colors.hsv(d / (r * 9f), 0.68f, 1f)
            Pattern.FLAG -> FLAG_COLORS[band(d, r * 1.4f).mod(FLAG_COLORS.size)]
            Pattern.HEARTS -> CalendarArt.HEART_BANDS[band(d, r * 1.4f).mod(CalendarArt.HEART_BANDS.size)]
        }

    /** Red, white and blue bands, whatever colours were picked. */
    private val FLAG_COLORS = intArrayOf(Colors.FLAG_RED, Colors.WHITE, Colors.FLAG_BLUE)

    private fun band(
        d: Float,
        size: Float,
    ) = floor(d / size).toInt()

    fun outlineColor(
        style: SnailStyle,
        mood: Mood,
    ): Int =
        when {
            mood == Mood.ANGRY -> Colors.ANGRY_RED
            style.pattern == Pattern.FLAG -> Colors.darken(Colors.FLAG_BLUE, 0.45f)
            style.pattern == Pattern.HEARTS -> Colors.darken(CalendarArt.HEART_RED, 0.45f)
            else -> Colors.darken(style.bodyColor, 0.45f)
        }

    /**
     * @param blink draw washed-out (the "hurt, can't be hurt again" flashing frame)
     * @param alpha how far between the previous and current step to draw (smooth motion)
     */
    fun drawSnail(
        c: Canvas2D,
        s: Snail,
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
        drawTrail(c, s, ow, outline, wash)
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

            Pattern.HEARTS -> {
                CalendarArt.drawHearts(c, s, wash)
            }

            else -> {} // solid, stripes and rainbow are just body colors
        }
        drawGloss(c, s)
        SnailBodyArt.draw(c, s, mood, time, ow, outline, wash)
        PowerUpArt.drawShield(c, s, time)
    }

    /**
     * The slime trail: an outline (a slightly wider ribbon behind everything),
     * then the slime itself, tail first so the front overlaps the back. Each
     * is a few thick round-ended lines rather than a circle per body circle,
     * and only the parts on screen are drawn: most of a long trail usually isn't.
     */
    private fun drawTrail(
        c: Canvas2D,
        s: Snail,
        ow: Float,
        outline: Int,
        wash: Float,
    ) {
        strokeRuns(c, s, ow) { outline }
        strokeRuns(c, s, 0f) { i -> Colors.washed(slimeColor(s.style, s.segD[i], s.radius), wash) }
    }

    /**
     * Strokes the trail of [s], tail first, as runs of neighbouring circles
     * that share a colour ([colorAt]) and a width, each [extra] wider than the
     * circles on every side. Circles off the screen are left out.
     */
    private inline fun strokeRuns(
        c: Canvas2D,
        s: Snail,
        extra: Float,
        colorAt: (Int) -> Int,
    ) {
        var i = s.segCount - 1
        while (i >= 1) {
            if (!onScreen(s, i, extra)) {
                i--
                continue
            }
            val color = colorAt(i)
            val half = trailHalfWidth(s, i)
            // The run goes on towards the head while the next circle looks the same.
            var end = i
            while (end > 1) {
                val next = end - 1
                val same = onScreen(s, next, extra) && trailHalfWidth(s, next) == half && colorAt(next) == color
                if (!same) break
                end = next
            }
            if (end == i) {
                c.fillCircle(s.drawX[i], s.drawY[i], half + extra, color)
            } else {
                path.reset()
                path.moveTo(s.drawX[i], s.drawY[i])
                for (k in i - 1 downTo end) path.lineTo(s.drawX[k], s.drawY[k])
                c.strokePath(path, 2f * (half + extra), color)
            }
            i = end - 1
        }
    }

    /**
     * Half the trail's width at circle [i]: the circle's radius, except that
     * the taper towards the tip narrows in [TAPER_STEPS] small steps (so a
     * whole stretch can be one line), never narrower than the circle itself.
     */
    private fun trailHalfWidth(
        s: Snail,
        i: Int,
    ): Float {
        val r = s.radius
        val step = r * (1f - GameConfig.TAIL_TIP_SCALE) / TAPER_STEPS
        return r - floor((r - s.segR[i]) / step) * step
    }

    /** The taper of a trail is drawn in this many widths. */
    private const val TAPER_STEPS = 8

    /** Slime is the snail's own colours, paler. */
    private const val SLIME_PALE = 0.55f

    /** Colour of the slime trail at distance [d] along it, for a snail of radius [r]. */
    fun slimeColor(
        style: SnailStyle,
        d: Float,
        r: Float,
    ): Int = Colors.lighten(segmentColor(style, d, r), SLIME_PALE)

    /**
     * Whether body circle [i] of [s], grown by [extra], is in view. Every
     * pattern mark lies inside its circle, so a mark on a hidden circle is hidden too.
     */
    internal fun onScreen(
        s: Snail,
        i: Int,
        extra: Float = 0f,
    ): Boolean = SceneRenderer.View.contains(s.drawX[i], s.drawY[i], s.segR[i] + extra)

    private fun drawDots(
        c: Canvas2D,
        s: Snail,
        wash: Float,
    ) {
        val color = Colors.washed(s.style.patternColor, wash)
        for (i in s.segCount - 2 downTo 1) {
            if (i % 4 != 2 || !onScreen(s, i)) continue
            perp(s, i)
            val side = if ((i / 4) % 2 == 0) 1f else -1f
            val off = side * s.segR[i] * 0.42f
            c.fillCircle(s.drawX[i] + perpX * off, s.drawY[i] + perpY * off, s.segR[i] * 0.3f, color)
        }
    }

    private fun drawZigzag(
        c: Canvas2D,
        s: Snail,
        wash: Float,
    ) {
        if (s.segCount < 4) return
        path.reset()
        var k = 0
        var i = 2
        var pen = false
        while (i < s.segCount - 1) {
            if (onScreen(s, i)) {
                perp(s, i)
                val off = (if (k % 2 == 0) 0.5f else -0.5f) * s.segR[i]
                val x = s.drawX[i] + perpX * off
                val y = s.drawY[i] + perpY * off
                if (pen) path.lineTo(x, y) else path.moveTo(x, y)
            }
            pen = onScreen(s, i)
            k++
            i += 2
        }
        if (path.opCount > 1) c.strokePath(path, max(1.5f, s.radius * 0.28f), Colors.washed(s.style.patternColor, wash))
    }

    private fun drawChecker(
        c: Canvas2D,
        s: Snail,
        wash: Float,
    ) {
        val style = s.style
        for (i in s.segCount - 1 downTo 1) {
            if (!onScreen(s, i)) continue
            perp(s, i)
            val other = if (band(s.segD[i], s.radius * 1.3f) % 2 == 0) style.patternColor else style.bodyColor
            val off = s.segR[i] * 0.48f
            c.fillCircle(s.drawX[i] + perpX * off, s.drawY[i] + perpY * off, s.segR[i] * 0.52f, Colors.washed(other, wash))
        }
    }

    /** Little five-pointed stars along the body, every few circles, swapping sides. */
    private fun drawStars(
        c: Canvas2D,
        s: Snail,
        wash: Float,
    ) {
        val color = Colors.washed(s.style.patternColor, wash)
        for (i in s.segCount - 2 downTo 1) {
            if (i % 5 != 3 || !onScreen(s, i)) continue
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

    /** Wet shine: one soft stripe along the top-left of the slime, where it's on screen. */
    private fun drawGloss(
        c: Canvas2D,
        s: Snail,
    ) {
        val glossEnd = (s.segCount * 0.85f).toInt()
        if (glossEnd <= 2) return
        path.reset()
        var pen = false
        for (i in 1 until glossEnd) {
            val seen = onScreen(s, i)
            if (seen) {
                val x = s.drawX[i] - s.segR[i] * 0.3f
                val y = s.drawY[i] - s.segR[i] * 0.36f
                if (pen) path.lineTo(x, y) else path.moveTo(x, y)
            }
            pen = seen
        }
        if (path.opCount > 1) c.strokePath(path, s.radius * 0.32f, Colors.withAlpha(Colors.WHITE, 0.45f))
    }

    private var perpX = 0f
    private var perpY = 0f

    /** Sets [perpX]/[perpY] to the unit vector perpendicular to the body at circle [i]. */
    private fun perp(
        s: Snail,
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
}
