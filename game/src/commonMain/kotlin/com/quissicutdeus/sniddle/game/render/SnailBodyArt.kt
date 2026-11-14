package com.quissicutdeus.sniddle.game.render

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.PI_F
import com.quissicutdeus.sniddle.game.model.Pattern
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.model.TWO_PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The snail itself, at the front of its slime trail ([SnailArt] draws the
 * trail): its foot, its shell, and its head with feelers, face and hat.
 */
internal object SnailBodyArt {
    private val path = Path2D()
    private val face = FaceArt.Face()

    /** How long the head bulges after eating. */
    private const val SQUASH_SECONDS = 0.25f

    /** The whole snail, back to front: foot, shell, then the head on top so the face is never hidden. */
    @Suppress("LongParameterList") // the snail, its mood and the outline its trail was drawn with
    fun draw(
        c: Canvas2D,
        s: Snail,
        mood: Mood,
        time: Float,
        ow: Float,
        outline: Int,
        wash: Float,
    ) {
        drawFoot(c, s, ow, outline, wash)
        drawShell(c, s, ow, outline, wash)
        drawHead(c, s, mood, time, ow, outline, wash)
    }

    /** How far behind the head the snail's own body (its foot) reaches, in body radii. */
    private const val FOOT_LENGTH = 4.2f

    /**
     * The snail's foot: its body, in its full colour, from the head back to a
     * round end a little behind the shell. The pale slime carries on from there.
     */
    private fun drawFoot(
        c: Canvas2D,
        s: Snail,
        ow: Float,
        outline: Int,
        wash: Float,
    ) {
        val end = min(s.segCount - 1, (FOOT_LENGTH / GameConfig.SEGMENT_SPACING_FACTOR).toInt())
        if (end < 1) return
        path.reset()
        path.moveTo(s.drawX[end], s.drawY[end])
        for (i in end - 1 downTo 0) path.lineTo(s.drawX[i], s.drawY[i])
        val w = 2f * s.segR[end]
        c.strokePath(path, w + 2f * ow, outline)
        c.strokePath(path, w, Colors.washed(SnailArt.segmentColor(s.style, 0f, s.radius), wash))
    }

    /** How far behind the head the middle of the shell rides, and how big the shell is, in body radii. */
    private const val SHELL_BACK = 2.1f
    private const val SHELL_SCALE = 1.45f

    /** The shell's colour: the snail's second colour (the fixed-colour patterns bring their own). */
    fun shellColor(style: SnailStyle): Int =
        when (style.pattern) {
            Pattern.FLAG -> Colors.FLAG_RED
            Pattern.HEARTS -> CalendarArt.HEART_RED
            else -> style.patternColor
        }

    /**
     * The shell, riding on the trail just behind the head: a round shell with
     * a spiral that opens towards the head, and a shine.
     */
    private fun drawShell(
        c: Canvas2D,
        s: Snail,
        ow: Float,
        outline: Int,
        wash: Float,
    ) {
        val r = s.radius
        // The body circle about SHELL_BACK radii along (or straight behind a snail too short to have one).
        val at = (SHELL_BACK / GameConfig.SEGMENT_SPACING_FACTOR).toInt()
        val sx: Float
        val sy: Float
        if (at < s.segCount) {
            sx = s.drawX[at]
            sy = s.drawY[at]
        } else {
            sx = s.drawX[0] - cos(s.heading) * r * SHELL_BACK
            sy = s.drawY[0] - sin(s.heading) * r * SHELL_BACK
        }
        val sr = r * SHELL_SCALE
        val shell = Colors.washed(shellColor(s.style), wash)
        c.fillCircle(sx, sy, sr + ow, outline)
        c.fillCircle(sx, sy, sr, shell)
        // The spiral winds in from the edge nearest the head.
        val toHead = atan2(s.drawY[0] - sy, s.drawX[0] - sx)
        path.reset()
        for (k in 0..SPIRAL_STEPS) {
            val t = k.toFloat() / SPIRAL_STEPS
            val a = toHead + t * SPIRAL_TURNS * TWO_PI
            val rho = sr * (0.82f - 0.72f * t)
            val x = sx + cos(a) * rho
            val y = sy + sin(a) * rho
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val spiral = if (Colors.luminance(shell) > 0.5f) Colors.darken(shell, 0.38f) else Colors.lighten(shell, 0.5f)
        c.strokePath(path, max(1.5f, sr * 0.13f), spiral)
        c.fillCircle(sx - sr * 0.4f, sy - sr * 0.45f, sr * 0.2f, Colors.withAlpha(Colors.lighten(shell, 0.7f), 0.7f))
    }

    private const val SPIRAL_STEPS = 30
    private const val SPIRAL_TURNS = 1.9f

    /** Two feelers with round tips, reaching forwards from the head and swaying a little. */
    private fun drawFeelers(
        c: Canvas2D,
        s: Snail,
        hr: Float,
        time: Float,
        ow: Float,
        outline: Int,
        color: Int,
    ) {
        val sway = 0.08f * sin(time * 3f + s.id)
        for (side in SIDES) {
            val a = s.heading + side * 0.5f + sway
            val x0 = s.drawX[0] + cos(a) * hr * 0.8f
            val y0 = s.drawY[0] + sin(a) * hr * 0.8f
            val x1 = s.drawX[0] + cos(a) * hr * 1.65f
            val y1 = s.drawY[0] + sin(a) * hr * 1.65f
            val w = hr * 0.16f
            c.line(x0, y0, x1, y1, w + 2f * ow, outline)
            c.fillCircle(x1, y1, hr * 0.2f + ow, outline)
            c.line(x0, y0, x1, y1, w, color)
            c.fillCircle(x1, y1, hr * 0.2f, color)
        }
    }

    private fun drawHead(
        c: Canvas2D,
        s: Snail,
        mood: Mood,
        time: Float,
        ow: Float,
        outline: Int,
        wash: Float,
    ) {
        val style = s.style
        // Squash & stretch right after eating.
        val squash = if (s.sinceAte < SQUASH_SECONDS) 1f + 0.22f * sin(PI_F * s.sinceAte / SQUASH_SECONDS) else 1f
        val hr = s.radius * SnailArt.HEAD_SCALE * squash
        val hx = s.drawX[0]
        val hy = s.drawY[0]
        val headColor = Colors.washed(SnailArt.segmentColor(style, 0f, s.radius), wash)

        drawFeelers(c, s, hr, time, ow, outline, headColor)
        c.fillCircle(hx, hy, hr + ow, outline)
        c.fillCircle(hx, hy, hr, headColor)
        c.fillCircle(hx - hr * 0.32f, hy - hr * 0.38f, hr * 0.28f, Colors.withAlpha(Colors.lighten(headColor, 0.6f), 0.7f))

        // The face sits a little towards where the snail is going.
        val fx = hx + cos(s.heading) * hr * 0.15f
        val fy = hy + sin(s.heading) * hr * 0.15f
        val iris = style.irisColor ?: 0
        FaceArt.drawFace(c, face.set(fx, fy, hr, mood, s.heading, time + s.id * 1.7f, headColor, wash, iris), style.eyes)
        HatArt.drawHat(c, fx, fy - hr * 0.74f, hr, style.hat, style, wash)
    }
}
