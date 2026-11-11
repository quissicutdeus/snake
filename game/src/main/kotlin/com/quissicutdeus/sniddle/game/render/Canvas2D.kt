package com.quissicutdeus.sniddle.game.render

/**
 * The tiny drawing surface the game art is written against. The Android app
 * implements it on top of the Compose/Android canvas; the preview tool
 * implements it with Java2D to write PNGs. Colors are ARGB ints, y points down.
 */
interface Canvas2D {
    val width: Float
    val height: Float

    fun save()

    fun restore()

    fun translate(
        dx: Float,
        dy: Float,
    )

    fun scale(
        sx: Float,
        sy: Float = sx,
    )

    /** Rotates by [radians] (clockwise on screen, since y points down). */
    fun rotate(radians: Float)

    fun fillCircle(
        cx: Float,
        cy: Float,
        r: Float,
        color: Int,
    )

    fun strokeCircle(
        cx: Float,
        cy: Float,
        r: Float,
        width: Float,
        color: Int,
    )

    fun fillRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        color: Int,
    )

    fun fillRoundRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        radius: Float,
        color: Int,
    )

    /** Straight line with round caps. */
    fun line(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        width: Float,
        color: Int,
    )

    fun fillPath(
        path: Path2D,
        color: Int,
    )

    /** Stroke with round caps and joins. */
    fun strokePath(
        path: Path2D,
        width: Float,
        color: Int,
    )

    /** Draws [text] with its baseline at [y]. */
    fun text(
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        align: TextAlign = TextAlign.LEFT,
        bold: Boolean = true,
    )

    /** Strokes the outline of bold [text] (round joins), [width] wide, with its baseline at [y]. */
    fun strokeText(
        text: String,
        x: Float,
        y: Float,
        size: Float,
        width: Float,
        color: Int,
        align: TextAlign = TextAlign.LEFT,
    )

    fun measureText(
        text: String,
        size: Float,
        bold: Boolean = true,
    ): Float
}

enum class TextAlign { LEFT, CENTER, RIGHT }

/** Backend-neutral path: a list of commands replayed by each [Canvas2D]. */
class Path2D {
    enum class Op { MOVE, LINE, QUAD, CUBIC, CLOSE }

    private var ops = arrayOfNulls<Op>(32)
    private var pts = FloatArray(128)
    var opCount = 0
        private set

    fun reset(): Path2D {
        opCount = 0
        ptCount = 0
        return this
    }

    private var ptCount = 0

    fun moveTo(
        x: Float,
        y: Float,
    ) = add(Op.MOVE, x, y)

    fun lineTo(
        x: Float,
        y: Float,
    ) = add(Op.LINE, x, y)

    fun quadTo(
        cx: Float,
        cy: Float,
        x: Float,
        y: Float,
    ) = add(Op.QUAD, cx, cy, x, y)

    fun cubicTo(
        c1x: Float,
        c1y: Float,
        c2x: Float,
        c2y: Float,
        x: Float,
        y: Float,
    ) = add(Op.CUBIC, c1x, c1y, c2x, c2y, x, y)

    fun close() = add(Op.CLOSE)

    /** Appends an arc of a circle as line segments (starting with lineTo unless [moveFirst]). */
    fun arc(
        cx: Float,
        cy: Float,
        r: Float,
        from: Float,
        to: Float,
        moveFirst: Boolean = false,
        steps: Int = 16,
    ) {
        for (i in 0..steps) {
            val a = from + (to - from) * i / steps
            val x = cx + kotlin.math.cos(a) * r
            val y = cy + kotlin.math.sin(a) * r
            if (i == 0 && moveFirst) moveTo(x, y) else lineTo(x, y)
        }
    }

    /** Replays the commands. Points for each op are passed in order. */
    inline fun forEach(fn: (Op, FloatArray, Int) -> Unit) {
        var p = 0
        for (i in 0 until opCount) {
            val op = opAt(i)
            fn(op, points, p)
            p += pointsFor(op)
        }
    }

    @PublishedApi internal fun opAt(i: Int) = ops[i]!!

    @PublishedApi internal val points get() = pts

    @PublishedApi internal fun pointsFor(op: Op) =
        when (op) {
            Op.MOVE, Op.LINE -> 2
            Op.QUAD -> 4
            Op.CUBIC -> 6
            Op.CLOSE -> 0
        }

    private fun add(
        op: Op,
        vararg p: Float,
    ) {
        if (opCount == ops.size) ops = ops.copyOf(ops.size * 2)
        if (ptCount + p.size > pts.size) pts = pts.copyOf(maxOf(pts.size * 2, ptCount + p.size))
        ops[opCount++] = op
        for (v in p) pts[ptCount++] = v
    }
}
