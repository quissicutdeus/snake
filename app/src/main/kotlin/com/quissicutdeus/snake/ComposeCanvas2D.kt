package com.quissicutdeus.snake

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.quissicutdeus.snake.game.render.Canvas2D
import com.quissicutdeus.snake.game.render.Path2D
import com.quissicutdeus.snake.game.render.TextAlign

/**
 * The game's [Canvas2D] drawn onto a Compose [DrawScope] (through its Android
 * canvas). Paints and the path are reused, so drawing a frame allocates nothing.
 */
class ComposeCanvas2D : Canvas2D {
    private var canvas: android.graphics.Canvas? = null

    override var width = 0f
        private set
    override var height = 0f
        private set

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    private val plainStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textStroke =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            typeface = Typeface.DEFAULT_BOLD
        }
    private val path = Path()
    private val rect = RectF()

    /** Runs [block] with this canvas bound to the scope's drawing surface. */
    fun draw(
        scope: DrawScope,
        block: (Canvas2D) -> Unit,
    ) {
        scope.drawIntoCanvas { c ->
            canvas = c.nativeCanvas
            width = scope.size.width
            height = scope.size.height
            block(this)
            canvas = null
        }
    }

    private val c: android.graphics.Canvas get() = canvas ?: error("ComposeCanvas2D used outside draw()")

    override fun save() {
        c.save()
    }

    override fun restore() = c.restore()

    override fun translate(
        dx: Float,
        dy: Float,
    ) = c.translate(dx, dy)

    override fun scale(
        sx: Float,
        sy: Float,
    ) = c.scale(sx, sy)

    override fun rotate(radians: Float) = c.rotate(Math.toDegrees(radians.toDouble()).toFloat())

    override fun fillCircle(
        cx: Float,
        cy: Float,
        r: Float,
        color: Int,
    ) {
        fill.color = color
        c.drawCircle(cx, cy, r, fill)
    }

    override fun strokeCircle(
        cx: Float,
        cy: Float,
        r: Float,
        width: Float,
        color: Int,
    ) {
        plainStroke.color = color
        plainStroke.strokeWidth = width
        c.drawCircle(cx, cy, r, plainStroke)
    }

    override fun fillRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        color: Int,
    ) {
        fill.color = color
        c.drawRect(x, y, x + w, y + h, fill)
    }

    override fun fillRoundRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        radius: Float,
        color: Int,
    ) {
        fill.color = color
        rect.set(x, y, x + w, y + h)
        c.drawRoundRect(rect, radius, radius, fill)
    }

    override fun line(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        width: Float,
        color: Int,
    ) {
        stroke.color = color
        stroke.strokeWidth = width
        c.drawLine(x0, y0, x1, y1, stroke)
    }

    private fun toAndroid(p: Path2D): Path {
        path.rewind()
        p.forEach { op, pts, i ->
            when (op) {
                Path2D.Op.MOVE -> path.moveTo(pts[i], pts[i + 1])
                Path2D.Op.LINE -> path.lineTo(pts[i], pts[i + 1])
                Path2D.Op.QUAD -> path.quadTo(pts[i], pts[i + 1], pts[i + 2], pts[i + 3])
                Path2D.Op.CUBIC -> path.cubicTo(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], pts[i + 4], pts[i + 5])
                Path2D.Op.CLOSE -> path.close()
            }
        }
        return path
    }

    override fun fillPath(
        path: Path2D,
        color: Int,
    ) {
        fill.color = color
        c.drawPath(toAndroid(path), fill)
    }

    override fun strokePath(
        path: Path2D,
        width: Float,
        color: Int,
    ) {
        stroke.color = color
        stroke.strokeWidth = width
        c.drawPath(toAndroid(path), stroke)
    }

    private fun setupText(
        size: Float,
        bold: Boolean,
    ) {
        textPaint.textSize = size
        textPaint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    override fun text(
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        align: TextAlign,
        bold: Boolean,
    ) {
        setupText(size, bold)
        textPaint.color = color
        textPaint.textAlign = androidAlign(align)
        c.drawText(text, x, y, textPaint)
    }

    override fun strokeText(
        text: String,
        x: Float,
        y: Float,
        size: Float,
        width: Float,
        color: Int,
        align: TextAlign,
    ) {
        textStroke.textSize = size
        textStroke.strokeWidth = width
        textStroke.color = color
        textStroke.textAlign = androidAlign(align)
        c.drawText(text, x, y, textStroke)
    }

    private fun androidAlign(align: TextAlign) =
        when (align) {
            TextAlign.LEFT -> Paint.Align.LEFT
            TextAlign.CENTER -> Paint.Align.CENTER
            TextAlign.RIGHT -> Paint.Align.RIGHT
        }

    override fun measureText(
        text: String,
        size: Float,
        bold: Boolean,
    ): Float {
        setupText(size, bold)
        return textPaint.measureText(text)
    }
}
