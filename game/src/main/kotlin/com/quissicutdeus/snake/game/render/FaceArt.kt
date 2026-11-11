package com.quissicutdeus.snake.game.render

import com.quissicutdeus.snake.game.model.Eyes
import com.quissicutdeus.snake.game.model.PI_F
import kotlin.math.cos
import kotlin.math.sin

/** Cartoon faces: googly, sleepy, sunglasses or heart eyes, with eyebrows and a mouth that show the mood. */
object FaceArt {
    private val path = Path2D()
    private const val SUNGLASS = 0xFF1E1E28.toInt()
    private const val SWEAT = 0xFF7FD3FF.toInt()
    private const val HEART_PINK = 0xFFFF3B6B.toInt()

    /** Where a face sits and how it looks: centre ([fx], [fy]) for a head of radius [hr]. */
    class Face(
        val fx: Float,
        val fy: Float,
        val hr: Float,
        val mood: Mood,
        heading: Float,
        val time: Float,
        val headColor: Int,
        val wash: Float,
    ) {
        /** Eye radius and eye height. */
        val er = hr * (if (mood == Mood.SCARED) 0.37f else 0.33f)
        val ey = fy - hr * 0.12f

        /** Where the pupils look (the direction the snake is going). */
        val lookX = cos(heading)
        val lookY = sin(heading)
        val ink = Colors.washed(Colors.INK, wash)
    }

    /** Eyes, eyebrows (mood) and mouth (mood). Faces stay upright; pupils look where the snake goes. */
    fun drawFace(
        c: Canvas2D,
        face: Face,
        eyes: Eyes,
    ) {
        val mood = face.mood
        for (side in SIDES) {
            val ex = face.fx + side * face.hr * 0.42f
            when (eyes) {
                Eyes.GOOGLY -> drawGooglyEye(c, face, ex, side, mood)
                Eyes.SLEEPY -> drawSleepyEye(c, face, ex)
                Eyes.SUNGLASSES -> drawSunglassLens(c, face, ex)
                Eyes.HEARTS -> drawHeartEye(c, face, ex)
            }
            drawBrow(c, face, ex, side, mood)
        }
        if (eyes == Eyes.SUNGLASSES) {
            val er = face.er
            val y = face.ey - er * 0.2f
            c.line(
                face.fx - face.hr * 0.42f + er,
                y,
                face.fx + face.hr * 0.42f - er,
                y,
                face.hr * 0.09f,
                Colors.washed(SUNGLASS, face.wash),
            )
        }
        drawMouth(c, face, mood)
    }

    private fun drawGooglyEye(
        c: Canvas2D,
        face: Face,
        ex: Float,
        side: Int,
        mood: Mood,
    ) {
        val er = face.er
        val ey = face.ey
        c.fillCircle(ex, ey, er + face.hr * 0.05f, face.ink)
        c.fillCircle(ex, ey, er, Colors.washed(Colors.WHITE, face.wash))
        val wobble = sin(face.time * 9f + side) * er * 0.08f
        val pr = er * (if (mood == Mood.SCARED) 0.36f else 0.5f)
        val px = ex + face.lookX * er * 0.42f + wobble
        val py = ey + face.lookY * er * 0.42f
        c.fillCircle(px, py, pr, face.ink)
        c.fillCircle(px - pr * 0.35f, py - pr * 0.35f, pr * 0.32f, Colors.washed(Colors.WHITE, face.wash))
    }

    /** A white eye with a pink heart for a pupil, looking where the snake goes. */
    private fun drawHeartEye(
        c: Canvas2D,
        face: Face,
        ex: Float,
    ) {
        val er = face.er
        val ey = face.ey
        c.fillCircle(ex, ey, er + face.hr * 0.05f, face.ink)
        c.fillCircle(ex, ey, er, Colors.washed(Colors.WHITE, face.wash))
        val px = ex + face.lookX * er * 0.25f
        val py = ey + face.lookY * er * 0.25f
        c.fillPath(HudRenderer.heartPath(px, py, er * 1.25f), Colors.washed(HEART_PINK, face.wash))
    }

    private fun drawSleepyEye(
        c: Canvas2D,
        face: Face,
        ex: Float,
    ) {
        val er = face.er
        val ey = face.ey
        val hr = face.hr
        c.fillCircle(ex, ey, er + hr * 0.05f, face.ink)
        c.fillCircle(ex, ey, er, Colors.washed(Colors.WHITE, face.wash))
        c.fillCircle(ex + face.lookX * er * 0.3f, ey + er * 0.35f, er * 0.45f, face.ink)
        // Heavy eyelid over the top half.
        path.reset()
        path.arc(ex, ey, er + hr * 0.06f, PI_F, 2f * PI_F, moveFirst = true)
        path.lineTo(ex + er + hr * 0.06f, ey + er * 0.1f)
        path.lineTo(ex - er - hr * 0.06f, ey + er * 0.1f)
        path.close()
        c.fillPath(path, face.headColor)
        c.line(ex - er * 1.05f, ey + er * 0.1f, ex + er * 1.05f, ey + er * 0.1f, hr * 0.08f, face.ink)
    }

    private fun drawSunglassLens(
        c: Canvas2D,
        face: Face,
        ex: Float,
    ) {
        val er = face.er
        val ey = face.ey
        c.fillCircle(ex, ey, er * 1.15f, Colors.washed(SUNGLASS, face.wash))
        val shine = Colors.withAlpha(Colors.washed(Colors.WHITE, face.wash), 0.8f)
        c.line(ex - er * 0.55f, ey - er * 0.5f, ex - er * 0.15f, ey - er * 0.75f, face.hr * 0.06f, shine)
    }

    /** Eyebrows show the mood: raised when scared, slanted down when angry. */
    private fun drawBrow(
        c: Canvas2D,
        face: Face,
        ex: Float,
        side: Int,
        mood: Mood,
    ) {
        val er = face.er
        val ey = face.ey
        val bw = face.hr * 0.1f
        when (mood) {
            Mood.SCARED -> {
                c.line(ex + side * er * 0.7f, ey - er * 1.35f, ex - side * er * 0.55f, ey - er * 1.75f, bw, face.ink)
            }

            Mood.ANGRY -> {
                c.line(ex + side * er * 0.75f, ey - er * 1.75f, ex - side * er * 0.55f, ey - er * 1.1f, bw * 1.3f, face.ink)
            }

            Mood.HAPPY -> {}
        }
    }

    private fun drawMouth(
        c: Canvas2D,
        face: Face,
        mood: Mood,
    ) {
        val fx = face.fx
        val fy = face.fy
        val hr = face.hr
        val mw = hr * 0.08f
        when (mood) {
            Mood.HAPPY -> {
                path.reset()
                path.arc(fx, fy + hr * 0.2f, hr * 0.3f, PI_F * 0.22f, PI_F * 0.78f, moveFirst = true, steps = 10)
                c.strokePath(path, mw, face.ink)
            }

            Mood.SCARED -> {
                c.fillCircle(fx, fy + hr * 0.52f, hr * 0.14f, face.ink)
                c.fillCircle(fx, fy + hr * 0.56f, hr * 0.07f, Colors.washed(0xFFFF8A9A.toInt(), face.wash))
                drawSweatDrop(c, face, fx + hr * 0.82f, fy - hr * 0.35f, hr)
            }

            Mood.ANGRY -> {
                path.reset()
                path.arc(fx, fy + hr * 0.75f, hr * 0.28f, PI_F * 1.25f, PI_F * 1.75f, moveFirst = true, steps = 10)
                c.strokePath(path, mw, face.ink)
            }
        }
    }

    private fun drawSweatDrop(
        c: Canvas2D,
        face: Face,
        dx: Float,
        dy: Float,
        hr: Float,
    ) {
        path.reset()
        path.moveTo(dx, dy - hr * 0.28f)
        path.quadTo(dx + hr * 0.16f, dy, dx, dy + hr * 0.08f)
        path.quadTo(dx - hr * 0.16f, dy, dx, dy - hr * 0.28f)
        path.close()
        c.fillPath(path, Colors.washed(SWEAT, face.wash))
    }
}
