package com.quissicutdeus.sniddle.game.render

import com.quissicutdeus.sniddle.game.model.Eyes
import com.quissicutdeus.sniddle.game.model.PI_F
import kotlin.math.cos
import kotlin.math.sin

/** Cartoon faces (googly, sleepy, sunglasses, hearts, glasses, stars or sparkly eyes), with eyebrows and a mouth that show the mood. */
object FaceArt {
    private val path = Path2D()
    private const val SUNGLASS = 0xFF1E1E28.toInt()
    private const val SWEAT = 0xFF7FD3FF.toInt()
    private const val HEART_PINK = 0xFFFF3B6B.toInt()
    private const val GLASSES_FRAME = 0xFF7B2FBE.toInt()
    private const val STAR_EDGE = 0xFFB8860B.toInt()
    private const val IRIS = 0xFF2E86DE.toInt()
    private const val IRIS_EDGE_DARKEN = 0.45f

    /**
     * Where a face sits and how it looks: centre ([fx], [fy]) for a head of
     * radius [hr]. One is filled in again with [set] for each snail drawn, so
     * a frame makes no new ones.
     */
    class Face {
        var fx = 0f
            private set
        var fy = 0f
            private set
        var hr = 0f
            private set
        var mood = Mood.HAPPY
            private set
        var time = 0f
            private set
        var headColor = 0
            private set
        var wash = 0f
            private set

        /** Eye radius and eye height. */
        var er = 0f
            private set
        var ey = 0f
            private set

        /** Where the pupils look (the direction the snail is going). */
        var lookX = 0f
            private set
        var lookY = 0f
            private set
        var ink = 0
            private set

        /** The coloured part of the eyes and its darker edge; [iris] is 0 for classic dark eyes. */
        var iris = 0
            private set
        var irisEdge = 0
            private set

        @Suppress("LongParameterList") // everything a face needs, in one go
        fun set(
            fx: Float,
            fy: Float,
            hr: Float,
            mood: Mood,
            heading: Float,
            time: Float,
            headColor: Int,
            wash: Float,
            iris: Int = 0,
        ): Face {
            this.fx = fx
            this.fy = fy
            this.hr = hr
            this.mood = mood
            this.time = time
            this.headColor = headColor
            this.wash = wash
            er = hr * (if (mood == Mood.SCARED) 0.37f else 0.33f)
            ey = fy - hr * 0.12f
            lookX = cos(heading)
            lookY = sin(heading)
            ink = Colors.washed(Colors.INK, wash)
            this.iris = if (iris == 0) 0 else Colors.washed(iris, wash)
            irisEdge = if (iris == 0) 0 else Colors.washed(Colors.darken(iris, IRIS_EDGE_DARKEN), wash)
            return this
        }
    }

    /** Eyes, eyebrows (mood) and mouth (mood). Faces stay upright; pupils look where the snail goes. */
    fun drawFace(
        c: Canvas2D,
        face: Face,
        eyes: Eyes,
    ) {
        val mood = face.mood
        if (mood == Mood.ASLEEP) {
            drawAsleep(c, face)
            return
        }
        for (side in SIDES) {
            val ex = face.fx + side * face.hr * 0.42f
            when (eyes) {
                Eyes.GOOGLY -> drawGooglyEye(c, face, ex, side, mood)
                Eyes.SLEEPY -> drawSleepyEye(c, face, ex)
                Eyes.SUNGLASSES -> drawSunglassLens(c, face, ex)
                Eyes.HEARTS -> drawHeartEye(c, face, ex)
                Eyes.GLASSES -> drawGlassesEye(c, face, ex, side, mood)
                Eyes.STARS -> drawStarEye(c, face, ex, mood)
                Eyes.SPARKLY -> drawSparklyEye(c, face, ex, side, mood)
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
        drawPupil(c, face, px, py, pr)
        c.fillCircle(px - pr * 0.35f, py - pr * 0.35f, pr * 0.32f, Colors.washed(Colors.WHITE, face.wash))
    }

    /**
     * A pupil of radius [r]: plain dark for classic eyes, or a coloured iris
     * (a bit bigger, with a darker edge so white or yellow still shows on the
     * eye white) around a small dark pupil.
     */
    private fun drawPupil(
        c: Canvas2D,
        face: Face,
        x: Float,
        y: Float,
        r: Float,
    ) {
        if (face.iris == 0) {
            c.fillCircle(x, y, r, face.ink)
            return
        }
        val ir = r * 1.3f
        c.fillCircle(x, y, ir, face.iris)
        c.strokeCircle(x, y, ir, face.hr * 0.045f, face.irisEdge)
        c.fillCircle(x, y, r * 0.6f, face.ink)
    }

    /** A googly eye behind a round frame, the two frames joined by a bridge over the nose. */
    private fun drawGlassesEye(
        c: Canvas2D,
        face: Face,
        ex: Float,
        side: Int,
        mood: Mood,
    ) {
        drawGooglyEye(c, face, ex, side, mood)
        val fr = face.er + face.hr * 0.12f
        val frame = Colors.washed(GLASSES_FRAME, face.wash)
        val lw = face.hr * 0.1f
        c.strokeCircle(ex, face.ey, fr, lw, frame)
        if (side < 0) c.line(ex + fr, face.ey - fr * 0.2f, 2f * face.fx - ex - fr, face.ey - fr * 0.2f, lw, frame)
        // The arm, back toward the ear.
        c.line(ex + side * fr, face.ey - fr * 0.2f, ex + side * (fr + face.hr * 0.16f), face.ey - fr * 0.35f, lw, frame)
    }

    /** A white eye with a star for a pupil (gold, or the eye colour); the star shrinks when scared. */
    private fun drawStarEye(
        c: Canvas2D,
        face: Face,
        ex: Float,
        mood: Mood,
    ) {
        val er = face.er
        val ey = face.ey
        c.fillCircle(ex, ey, er + face.hr * 0.05f, face.ink)
        c.fillCircle(ex, ey, er, Colors.washed(Colors.WHITE, face.wash))
        val sr = er * (if (mood == Mood.SCARED) 0.62f else 0.85f)
        starPath(ex + face.lookX * er * 0.18f, ey + face.lookY * er * 0.18f, sr)
        if (face.iris == 0) {
            c.fillPath(path, Colors.washed(Colors.GOLD, face.wash))
            c.strokePath(path, face.hr * 0.05f, Colors.washed(STAR_EDGE, face.wash))
        } else {
            c.fillPath(path, face.iris)
            c.strokePath(path, face.hr * 0.05f, face.irisEdge)
        }
    }

    /** Puts a five-pointed star of radius [r] at (cx, cy) in [path]. */
    private fun starPath(
        cx: Float,
        cy: Float,
        r: Float,
    ) {
        path.reset()
        for (k in 0 until 10) {
            val a = -PI_F / 2f + k * PI_F / 5f
            val rr = if (k % 2 == 0) r else r * 0.48f
            if (k == 0) path.moveTo(cx + cos(a) * rr, cy + sin(a) * rr) else path.lineTo(cx + cos(a) * rr, cy + sin(a) * rr)
        }
        path.close()
    }

    /**
     * Big cartoon eyes: a large coloured iris with a dark pupil, two bright
     * sparkles and lashes on the outside. Scared, the iris shrinks to a dot.
     */
    private fun drawSparklyEye(
        c: Canvas2D,
        face: Face,
        ex: Float,
        side: Int,
        mood: Mood,
    ) {
        val er = face.er * 1.1f
        val ey = face.ey
        val white = Colors.washed(Colors.WHITE, face.wash)
        // Lashes first, so the eye's outline tidies their roots.
        for (k in 0..2) {
            val a = -PI_F / 2f + side * (0.45f + k * 0.32f)
            val x0 = ex + cos(a) * er
            val y0 = ey + sin(a) * er
            c.line(x0, y0, x0 + cos(a) * er * 0.5f, y0 + sin(a) * er * 0.5f, face.hr * 0.08f, face.ink)
        }
        c.fillCircle(ex, ey, er + face.hr * 0.05f, face.ink)
        c.fillCircle(ex, ey, er, white)
        val scared = mood == Mood.SCARED
        val ir = er * (if (scared) 0.42f else 0.72f)
        val ix = ex + face.lookX * (er - ir) * 0.6f
        val iy = ey + face.lookY * (er - ir) * 0.6f
        c.fillCircle(ix, iy, ir, if (face.iris == 0) Colors.washed(IRIS, face.wash) else face.iris)
        if (face.iris != 0) c.strokeCircle(ix, iy, ir, face.hr * 0.045f, face.irisEdge)
        c.fillCircle(ix, iy, ir * 0.55f, face.ink)
        c.fillCircle(ix - ir * 0.38f, iy - ir * 0.38f, ir * 0.3f, white)
        c.fillCircle(ix + ir * 0.35f, iy + ir * 0.35f, ir * 0.14f, white)
    }

    /** A white eye with a heart for a pupil (pink, or the eye colour), looking where the snail goes. */
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
        val heart = HudRenderer.heartPath(px, py, er * 1.25f)
        if (face.iris == 0) {
            c.fillPath(heart, Colors.washed(HEART_PINK, face.wash))
        } else {
            c.fillPath(heart, face.iris)
            c.strokePath(heart, face.hr * 0.045f, face.irisEdge)
        }
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
        drawPupil(c, face, ex + face.lookX * er * 0.3f, ey + er * 0.35f, er * 0.45f)
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

            Mood.HAPPY, Mood.ASLEEP -> {}
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

            Mood.ASLEEP -> {
                // A little round "o", breathing slowly.
                c.fillCircle(fx, fy + hr * 0.45f, hr * 0.1f, face.ink)
            }
        }
    }

    /**
     * Fast asleep, whatever eyes the snail wears: each eye is a closed lid
     * (a smiley curve with three lashes) and the face is calm, no eyebrows.
     */
    private fun drawAsleep(
        c: Canvas2D,
        face: Face,
    ) {
        val hr = face.hr
        val er = face.er
        val lw = hr * 0.08f
        for (side in SIDES) {
            val ex = face.fx + side * hr * 0.42f
            val ey = face.ey + er * 0.1f
            path.reset()
            path.arc(ex, ey - er * 0.35f, er * 0.85f, PI_F * 0.2f, PI_F * 0.8f, moveFirst = true, steps = 10)
            c.strokePath(path, lw, face.ink)
            for (k in -1..1) {
                val a = PI_F * (0.5f + k * 0.22f)
                val x0 = ex + cos(a) * er * 0.85f
                val y0 = ey - er * 0.35f + sin(a) * er * 0.85f
                c.line(x0, y0, x0 + cos(a) * er * 0.35f, y0 + sin(a) * er * 0.35f, lw * 0.7f, face.ink)
            }
        }
        drawMouth(c, face, Mood.ASLEEP)
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
