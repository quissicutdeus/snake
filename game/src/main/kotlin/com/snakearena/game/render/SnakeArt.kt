package com.snakearena.game.render

import com.snakearena.game.model.Eyes
import com.snakearena.game.model.Hat
import com.snakearena.game.model.PI_F
import com.snakearena.game.model.Pattern
import com.snakearena.game.model.Pellet
import com.snakearena.game.model.Snake
import com.snakearena.game.model.SnakeStyle
import com.snakearena.game.model.TailPiece
import com.snakearena.game.GameConfig
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

    private const val SUNGLASS = 0xFF1E1E28.toInt()
    private const val SWEAT = 0xFF7FD3FF.toInt()

    /** Color of the body at distance [d] along it, for a snake of radius [r]. */
    fun segmentColor(style: SnakeStyle, d: Float, r: Float): Int = when (style.pattern) {
        Pattern.SOLID, Pattern.DOTS, Pattern.ZIGZAG -> style.bodyColor
        Pattern.STRIPES -> if (band(d, r * 1.4f) % 2 == 0) style.bodyColor else style.patternColor
        Pattern.CHECKER -> if (band(d, r * 1.3f) % 2 == 0) style.bodyColor else style.patternColor
        Pattern.RAINBOW -> Colors.hsv(d / (r * 9f), 0.68f, 1f)
    }

    private fun band(d: Float, size: Float) = floor(d / size).toInt()

    fun outlineColor(style: SnakeStyle, mood: Mood): Int =
        if (mood == Mood.ANGRY) Colors.ANGRY_RED else Colors.darken(style.bodyColor, 0.45f)

    /**
     * @param blink draw washed-out (the "hurt, can't be hurt again" flashing frame)
     */
    fun drawSnake(c: Canvas2D, s: Snake, mood: Mood, time: Float, blink: Boolean = false) {
        val n = s.segCount
        if (n == 0) return
        val style = s.style
        val r = s.radius
        val wash = if (blink) 0.6f else 0f
        fun tint(col: Int) = if (wash > 0f) Colors.lerp(col, Colors.WHITE, wash) else col

        val ow = if (mood == Mood.ANGRY) max(2.5f, r * 0.26f) else max(1.5f, r * 0.16f)
        val outline = tint(outlineColor(style, mood))
        val sx = s.segX
        val sy = s.segY
        val sr = s.segR
        val sd = s.segD

        // 1. Outline (one pass of slightly bigger circles behind everything).
        for (i in n - 1 downTo 1) c.fillCircle(sx[i], sy[i], sr[i] + ow, outline)

        // 2. Body fill, tail first so the front overlaps the back.
        for (i in n - 1 downTo 1) c.fillCircle(sx[i], sy[i], sr[i], tint(segmentColor(style, sd[i], r)))

        // 3. Pattern details.
        when (style.pattern) {
            Pattern.DOTS -> for (i in n - 2 downTo 1) {
                if (i % 4 != 2) continue
                perp(s, i)
                val px = perpX
                val py = perpY
                val side = if ((i / 4) % 2 == 0) 1f else -1f
                c.fillCircle(sx[i] + px * side * sr[i] * 0.42f, sy[i] + py * side * sr[i] * 0.42f, sr[i] * 0.3f, tint(style.patternColor))
            }
            Pattern.ZIGZAG -> {
                path.reset()
                var first = true
                var k = 0
                var i = 2
                while (i < n - 1) {
                    perp(s, i)
                    val px = perpX
                    val py = perpY
                    val side = if (k % 2 == 0) 0.5f else -0.5f
                    val x = sx[i] + px * side * sr[i]
                    val y = sy[i] + py * side * sr[i]
                    if (first) path.moveTo(x, y) else path.lineTo(x, y)
                    first = false
                    k++
                    i += 2
                }
                if (!first) c.strokePath(path, max(1.5f, r * 0.28f), tint(style.patternColor))
            }
            Pattern.CHECKER -> for (i in n - 1 downTo 1) {
                perp(s, i)
                val px = perpX
                val py = perpY
                val other = if (band(sd[i], r * 1.3f) % 2 == 0) style.patternColor else style.bodyColor
                c.fillCircle(sx[i] + px * sr[i] * 0.48f, sy[i] + py * sr[i] * 0.48f, sr[i] * 0.52f, tint(other))
            }
            else -> Unit
        }

        // 4. Glossy highlight: one soft stripe along the top-left of the tube.
        val glossEnd = (n * 0.85f).toInt()
        if (glossEnd > 2) {
            path.reset()
            path.moveTo(sx[1] - sr[1] * 0.3f, sy[1] - sr[1] * 0.36f)
            for (i in 2 until glossEnd) path.lineTo(sx[i] - sr[i] * 0.3f, sy[i] - sr[i] * 0.36f)
            c.strokePath(path, r * 0.32f, Colors.withAlpha(Colors.WHITE, 0.32f))
        }

        drawHead(c, s, mood, time, ow, outline, wash)
    }

    private var perpX = 0f
    private var perpY = 0f

    /** Sets [perpX]/[perpY] to the unit vector perpendicular to the body at circle [i]. */
    private fun perp(s: Snake, i: Int) {
        val a = max(0, i - 1)
        val b = min(s.segCount - 1, i + 1)
        val tx = s.segX[a] - s.segX[b]
        val ty = s.segY[a] - s.segY[b]
        val len = sqrt(tx * tx + ty * ty)
        if (len < 1e-4f) {
            perpX = -sin(s.heading)
            perpY = cos(s.heading)
        } else {
            perpX = -ty / len
            perpY = tx / len
        }
    }

    private fun drawHead(c: Canvas2D, s: Snake, mood: Mood, time: Float, ow: Float, outline: Int, wash: Float) {
        fun tint(col: Int) = if (wash > 0f) Colors.lerp(col, Colors.WHITE, wash) else col
        val style = s.style
        // Squash & stretch right after eating.
        val squash = if (s.sinceAte < 0.25f) 1f + 0.22f * sin(PI_F * s.sinceAte / 0.25f) else 1f
        val hr = s.radius * HEAD_SCALE * squash
        val hx = s.segX[0]
        val hy = s.segY[0]
        val headColor = tint(segmentColor(style, 0f, s.radius))

        c.fillCircle(hx, hy, hr + ow, outline)
        c.fillCircle(hx, hy, hr, headColor)
        c.fillCircle(hx - hr * 0.32f, hy - hr * 0.38f, hr * 0.28f, Colors.withAlpha(Colors.lighten(headColor, 0.6f), 0.7f))

        // The face sits a little towards where the snake is going.
        val fx = hx + cos(s.heading) * hr * 0.15f
        val fy = hy + sin(s.heading) * hr * 0.15f
        drawFace(c, fx, fy, hr, style, mood, s.heading, time + s.id * 1.7f, headColor, wash)
        drawHat(c, fx, fy - hr * 0.74f, hr, style.hat, style, wash)
    }

    /** A face centred on (fx, fy) for a head of radius [hr]. Also used by the menus. */
    fun drawFace(
        c: Canvas2D, fx: Float, fy: Float, hr: Float, style: SnakeStyle, mood: Mood,
        heading: Float, time: Float, headColor: Int, wash: Float = 0f,
    ) {
        fun tint(col: Int) = if (wash > 0f) Colors.lerp(col, Colors.WHITE, wash) else col
        val er = hr * (if (mood == Mood.SCARED) 0.37f else 0.33f)
        val ey = fy - hr * 0.12f
        val lookX = cos(heading)
        val lookY = sin(heading)
        val ink = tint(Colors.INK)
        for (side in intArrayOf(-1, 1)) {
            val ex = fx + side * hr * 0.42f
            when (style.eyes) {
                Eyes.GOOGLY -> {
                    c.fillCircle(ex, ey, er + hr * 0.05f, ink)
                    c.fillCircle(ex, ey, er, tint(Colors.WHITE))
                    val wob = sin(time * 9f + side) * er * 0.08f
                    val pr = er * (if (mood == Mood.SCARED) 0.36f else 0.5f)
                    val px = ex + lookX * er * 0.42f + wob
                    val py = ey + lookY * er * 0.42f
                    c.fillCircle(px, py, pr, ink)
                    c.fillCircle(px - pr * 0.35f, py - pr * 0.35f, pr * 0.32f, tint(Colors.WHITE))
                }
                Eyes.SLEEPY -> {
                    c.fillCircle(ex, ey, er + hr * 0.05f, ink)
                    c.fillCircle(ex, ey, er, tint(Colors.WHITE))
                    val pr = er * 0.45f
                    c.fillCircle(ex + lookX * er * 0.3f, ey + er * 0.35f, pr, ink)
                    // Heavy eyelid over the top half.
                    path.reset()
                    path.arc(ex, ey, er + hr * 0.06f, PI_F, 2f * PI_F, moveFirst = true)
                    path.lineTo(ex + er + hr * 0.06f, ey + er * 0.1f)
                    path.lineTo(ex - er - hr * 0.06f, ey + er * 0.1f)
                    path.close()
                    c.fillPath(path, headColor)
                    c.line(ex - er * 1.05f, ey + er * 0.1f, ex + er * 1.05f, ey + er * 0.1f, hr * 0.08f, ink)
                }
                Eyes.SUNGLASSES -> {
                    c.fillCircle(ex, ey, er * 1.15f, tint(SUNGLASS))
                    c.line(ex - er * 0.55f, ey - er * 0.5f, ex - er * 0.15f, ey - er * 0.75f, hr * 0.06f, Colors.withAlpha(tint(Colors.WHITE), 0.8f))
                }
            }
            // Eyebrows show the mood.
            val bw = hr * 0.1f
            when (mood) {
                Mood.SCARED -> {
                    val outer = ex + side * er * 0.7f
                    val inner = ex - side * er * 0.55f
                    c.line(outer, ey - er * 1.35f, inner, ey - er * 1.75f, bw, ink)
                }
                Mood.ANGRY -> {
                    val outer = ex + side * er * 0.75f
                    val inner = ex - side * er * 0.55f
                    c.line(outer, ey - er * 1.75f, inner, ey - er * 1.1f, bw * 1.3f, ink)
                }
                Mood.HAPPY -> Unit
            }
        }
        if (style.eyes == Eyes.SUNGLASSES) {
            c.line(fx - hr * 0.42f + er, ey - er * 0.2f, fx + hr * 0.42f - er, ey - er * 0.2f, hr * 0.09f, tint(SUNGLASS))
        }

        // Mouth.
        val mw = hr * 0.08f
        when (mood) {
            Mood.HAPPY -> {
                path.reset()
                path.arc(fx, fy + hr * 0.2f, hr * 0.3f, PI_F * 0.22f, PI_F * 0.78f, moveFirst = true, steps = 10)
                c.strokePath(path, mw, ink)
            }
            Mood.SCARED -> {
                c.fillCircle(fx, fy + hr * 0.52f, hr * 0.14f, ink)
                c.fillCircle(fx, fy + hr * 0.56f, hr * 0.07f, tint(0xFFFF8A9A.toInt()))
                // Sweat drop.
                val dx = fx + hr * 0.82f
                val dy = fy - hr * 0.35f
                path.reset()
                path.moveTo(dx, dy - hr * 0.28f)
                path.quadTo(dx + hr * 0.16f, dy, dx, dy + hr * 0.08f)
                path.quadTo(dx - hr * 0.16f, dy, dx, dy - hr * 0.28f)
                path.close()
                c.fillPath(path, tint(SWEAT))
            }
            Mood.ANGRY -> {
                path.reset()
                path.arc(fx, fy + hr * 0.75f, hr * 0.28f, PI_F * 1.25f, PI_F * 1.75f, moveFirst = true, steps = 10)
                c.strokePath(path, mw, ink)
            }
        }
    }

    /** A hat whose brim is centred at (x, y), sized for a head of radius [hr]. */
    fun drawHat(c: Canvas2D, x: Float, y: Float, hr: Float, hat: Hat, style: SnakeStyle, wash: Float = 0f) {
        fun tint(col: Int) = if (wash > 0f) Colors.lerp(col, Colors.WHITE, wash) else col
        val u = hr
        val lw = max(1f, u * 0.06f)
        when (hat) {
            Hat.NONE -> Unit
            Hat.CROWN -> {
                path.reset()
                path.moveTo(x - 0.55f * u, y)
                path.lineTo(x - 0.6f * u, y - 0.5f * u)
                path.lineTo(x - 0.28f * u, y - 0.24f * u)
                path.lineTo(x, y - 0.64f * u)
                path.lineTo(x + 0.28f * u, y - 0.24f * u)
                path.lineTo(x + 0.6f * u, y - 0.5f * u)
                path.lineTo(x + 0.55f * u, y)
                path.close()
                c.fillPath(path, tint(Colors.GOLD))
                c.strokePath(path, lw, tint(0xFFB8860B.toInt()))
                c.fillCircle(x - 0.6f * u, y - 0.52f * u, 0.08f * u, tint(0xFFFFF59D.toInt()))
                c.fillCircle(x, y - 0.66f * u, 0.09f * u, tint(0xFFFFF59D.toInt()))
                c.fillCircle(x + 0.6f * u, y - 0.52f * u, 0.08f * u, tint(0xFFFFF59D.toInt()))
                c.fillCircle(x, y - 0.14f * u, 0.09f * u, tint(0xFFE53935.toInt()))
                c.fillCircle(x - 0.32f * u, y - 0.1f * u, 0.06f * u, tint(0xFF3FA9F5.toInt()))
                c.fillCircle(x + 0.32f * u, y - 0.1f * u, 0.06f * u, tint(0xFF3FA9F5.toInt()))
            }
            Hat.PARTY -> {
                c.save()
                c.translate(x + 0.12f * u, y)
                c.rotate(0.28f)
                val hw = 0.4f * u
                val h = 1.05f * u
                val coneColor = tint(style.patternColor)
                path.reset()
                path.moveTo(-hw, 0f)
                path.lineTo(hw, 0f)
                path.lineTo(0f, -h)
                path.close()
                c.fillPath(path, coneColor)
                // Two stripes across the cone.
                val stripe = tint(if (Colors.luminance(style.patternColor) > 0.75f) Colors.HEART else Colors.WHITE)
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
                path.reset()
                path.moveTo(-hw, 0f)
                path.lineTo(hw, 0f)
                path.lineTo(0f, -h)
                path.close()
                c.strokePath(path, lw, tint(Colors.darken(style.patternColor, 0.45f)))
                c.fillCircle(0f, -h, 0.15f * u, tint(Colors.GOLD))
                c.restore()
            }
            Hat.BOW -> {
                c.save()
                c.translate(x + 0.42f * u, y + 0.05f * u)
                c.rotate(0.35f)
                val bowColor = tint(style.patternColor)
                val dark = tint(Colors.darken(style.patternColor, 0.45f))
                for (side in intArrayOf(-1, 1)) {
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
    }

    fun drawPellet(c: Canvas2D, p: Pellet, time: Float) {
        val pop = min(1f, p.age * 4f)
        val r = p.radius * pop * (1f + 0.12f * sin(time * 4f + p.phase))
        if (r <= 0.1f) return
        c.fillCircle(p.x, p.y, r * 1.9f, Colors.withAlpha(p.color, 0.22f))
        c.fillCircle(p.x, p.y, r, p.color)
        c.fillCircle(p.x - r * 0.3f, p.y - r * 0.32f, r * 0.36f, Colors.withAlpha(Colors.WHITE, 0.75f))
    }

    fun drawTailPiece(c: Canvas2D, t: TailPiece, time: Float) {
        val left = GameConfig.TAIL_PIECE_LIFETIME - t.age
        val fade = (left / GameConfig.TAIL_PIECE_FADE).coerceIn(0f, 1f)
        val pop = min(1f, 0.6f + t.age * 3f)
        val r = t.radius * pop * (1f + 0.06f * sin(time * 3f + t.index))
        val bodyR = t.radius / GameConfig.TAIL_PIECE_RADIUS_FACTOR
        val col = segmentColor(t.style, t.index * bodyR * GameConfig.SEGMENT_SPACING_FACTOR, bodyR)
        c.fillCircle(t.x, t.y, r + max(1.5f, r * 0.2f), Colors.withAlpha(Colors.darken(t.style.bodyColor, 0.45f), fade))
        c.fillCircle(t.x, t.y, r, Colors.withAlpha(col, fade))
        c.fillCircle(t.x - r * 0.3f, t.y - r * 0.34f, r * 0.32f, Colors.withAlpha(Colors.lighten(col, 0.5f), 0.7f * fade))
    }

    /** A heart shape centred at (x, y) with total width [size]. */
    fun heartPath(x: Float, y: Float, size: Float): Path2D {
        val s = size / 2f
        path.reset()
        path.moveTo(x, y + s * 0.9f)
        path.cubicTo(x - s * 1.25f, y + s * 0.05f, x - s * 0.95f, y - s * 1.05f, x, y - s * 0.4f)
        path.cubicTo(x + s * 0.95f, y - s * 1.05f, x + s * 1.25f, y + s * 0.05f, x, y + s * 0.9f)
        path.close()
        return path
    }
}
