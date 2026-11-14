package com.quissicutdeus.sniddle

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import com.quissicutdeus.sniddle.game.render.Canvas2D
import com.quissicutdeus.sniddle.game.render.Path2D
import com.quissicutdeus.sniddle.game.render.TextAlign
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Paint
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathBuilder
import org.jetbrains.skia.RRect
import org.jetbrains.skia.Rect
import org.jetbrains.skia.TextLine
import org.jetbrains.skia.Typeface
import kotlin.math.PI

actual fun ComposeCanvas2D(): ComposeCanvas2D = SkiaCanvas2D()

/**
 * The game's [Canvas2D] on iOS, drawn with Skia (Compose's own renderer there),
 * the counterpart of Android's `android.graphics` adapter: same paints, same
 * round caps and joins, same text alignment. Paints and the path builder are
 * reused; each drawn path is a small native object, freed as soon as it is drawn.
 *
 * Text goes through Skia's shaper ([TextLine]), not a bare [Font], so emoji in
 * a kid's name fall back to Apple's emoji font the way Android's Paint falls
 * back by itself. Each string is shaped once, at one reference size, and drawn
 * scaled to the size asked for: name tags change size smoothly as the camera
 * zooms, so shaping per size would reshape them every frame.
 */
private class SkiaCanvas2D : ComposeCanvas2D {
    private var canvas: Canvas? = null

    override var width = 0f
        private set
    override var height = 0f
        private set

    private val fill = Paint().apply { mode = PaintMode.FILL }
    private val stroke =
        Paint().apply {
            mode = PaintMode.STROKE
            strokeCap = PaintStrokeCap.ROUND
            strokeJoin = PaintStrokeJoin.ROUND
        }
    private val plainStroke = Paint().apply { mode = PaintMode.STROKE }
    private val textPaint = Paint()
    private val textStroke =
        Paint().apply {
            mode = PaintMode.STROKE
            strokeJoin = PaintStrokeJoin.ROUND
        }
    private val builder = PathBuilder()

    override fun draw(
        scope: DrawScope,
        block: (Canvas2D) -> Unit,
    ) {
        scope.drawIntoCanvas { c ->
            canvas = c.skiaCanvas
            width = scope.size.width
            height = scope.size.height
            block(this)
            canvas = null
        }
    }

    private val c: Canvas get() = canvas ?: error("ComposeCanvas2D used outside draw()")

    override fun save() {
        c.save()
    }

    override fun restore() {
        c.restore()
    }

    override fun translate(
        dx: Float,
        dy: Float,
    ) {
        c.translate(dx, dy)
    }

    override fun scale(
        sx: Float,
        sy: Float,
    ) {
        c.scale(sx, sy)
    }

    override fun rotate(radians: Float) {
        c.rotate((radians * 180.0 / PI).toFloat())
    }

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
        c.drawRect(Rect.makeLTRB(x, y, x + w, y + h), fill)
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
        c.drawRRect(RRect.makeLTRB(x, y, x + w, y + h, radius), fill)
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

    /** Draws [p] with [paint]. */
    private fun drawPath(
        p: Path2D,
        paint: Paint,
    ) {
        builder.reset()
        p.forEach { op, pts, i ->
            when (op) {
                Path2D.Op.MOVE -> builder.moveTo(pts[i], pts[i + 1])
                Path2D.Op.LINE -> builder.lineTo(pts[i], pts[i + 1])
                Path2D.Op.QUAD -> builder.quadTo(pts[i], pts[i + 1], pts[i + 2], pts[i + 3])
                Path2D.Op.CUBIC -> builder.cubicTo(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], pts[i + 4], pts[i + 5])
                Path2D.Op.CLOSE -> builder.closePath()
            }
        }
        val path: Path = builder.detach()
        c.drawPath(path, paint)
        path.close()
    }

    override fun fillPath(
        path: Path2D,
        color: Int,
    ) {
        fill.color = color
        drawPath(path, fill)
    }

    override fun strokePath(
        path: Path2D,
        width: Float,
        color: Int,
    ) {
        stroke.color = color
        stroke.strokeWidth = width
        drawPath(path, stroke)
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
        textPaint.color = color
        drawLine(Lines.of(text, bold), x, y, size, align, textPaint)
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
        // Android strokes outlines in the bold face. The line is drawn scaled, so the width is too.
        textStroke.strokeWidth = width * Lines.REFERENCE_SIZE / size
        textStroke.color = color
        drawLine(Lines.of(text, bold = true), x, y, size, align, textStroke)
    }

    override fun measureText(
        text: String,
        size: Float,
        bold: Boolean,
    ): Float = Lines.of(text, bold).width * size / Lines.REFERENCE_SIZE

    /** Draws [line] (shaped at the reference size) at [size], its baseline at [y]. */
    private fun drawLine(
        line: TextLine,
        x: Float,
        y: Float,
        size: Float,
        align: TextAlign,
        paint: Paint,
    ) {
        val k = size / Lines.REFERENCE_SIZE
        c.save()
        c.translate(x - offset(line.width * k, align), y)
        c.scale(k, k)
        c.drawTextLine(line, 0f, 0f, paint)
        c.restore()
    }

    /** How far left of x the text starts, as Android's Paint.Align does it. */
    private fun offset(
        width: Float,
        align: TextAlign,
    ) = when (align) {
        TextAlign.LEFT -> 0f
        TextAlign.CENTER -> width / 2f
        TextAlign.RIGHT -> width
    }
}

/**
 * Text lines shaped at [REFERENCE_SIZE], kept between frames. Every frame draws
 * the same handful of strings, so after the first frame drawing text shapes
 * nothing. The cache is emptied when it fills up (a changing score adds a line
 * now and then). Two fonts, made once.
 */
private object Lines {
    /** Large enough that scaled glyph outlines stay crisp at every size the game draws. */
    const val REFERENCE_SIZE = 64f

    private const val MAX = 256

    private val plain = Font(typeface(FontStyle.NORMAL), REFERENCE_SIZE)
    private val heavy = Font(typeface(FontStyle.BOLD), REFERENCE_SIZE)
    private val lines = HashMap<Pair<String, Boolean>, TextLine>()

    fun of(
        text: String,
        bold: Boolean,
    ): TextLine {
        val key = text to bold
        lines[key]?.let { return it }
        if (lines.size >= MAX) {
            lines.values.forEach { it.close() }
            lines.clear()
        }
        return TextLine.make(text, if (bold) heavy else plain).also { lines[key] = it }
    }

    /** The system font (San Francisco), as Android uses its own system font. */
    private fun typeface(style: FontStyle): Typeface =
        FontMgr.default.matchFamilyStyle(null, style)
            ?: FontMgr.default.legacyMakeTypeface("", style)
            ?: error("no system font")
}
