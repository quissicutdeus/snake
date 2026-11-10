package com.snakearena.game.preview

import com.snakearena.game.render.Canvas2D
import com.snakearena.game.render.Path2D
import com.snakearena.game.render.TextAlign
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** [Canvas2D] on top of Java2D, for rendering the real game art to PNG files. */
class Java2DCanvas(w: Int, h: Int) : Canvas2D {
    val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    private val g: Graphics2D = image.createGraphics().apply {
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    }
    private val stack = ArrayDeque<AffineTransform>()

    override val width = w.toFloat()
    override val height = h.toFloat()

    private fun color(c: Int) = Color(c, true)

    override fun save() = stack.addLast(g.transform)
    override fun restore() { g.transform = stack.removeLast() }
    override fun translate(dx: Float, dy: Float) = g.translate(dx.toDouble(), dy.toDouble())
    override fun scale(sx: Float, sy: Float) = g.scale(sx.toDouble(), sy.toDouble())
    override fun rotate(radians: Float) = g.rotate(radians.toDouble())

    override fun fillCircle(cx: Float, cy: Float, r: Float, color: Int) {
        g.color = color(color)
        g.fill(Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r))
    }

    override fun strokeCircle(cx: Float, cy: Float, r: Float, width: Float, color: Int) {
        g.color = color(color)
        g.stroke = BasicStroke(width)
        g.draw(Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r))
    }

    override fun fillRect(x: Float, y: Float, w: Float, h: Float, color: Int) {
        g.color = color(color)
        g.fill(Rectangle2D.Float(x, y, w, h))
    }

    override fun fillRoundRect(x: Float, y: Float, w: Float, h: Float, radius: Float, color: Int) {
        g.color = color(color)
        g.fill(RoundRectangle2D.Float(x, y, w, h, radius * 2, radius * 2))
    }

    override fun strokeRoundRect(x: Float, y: Float, w: Float, h: Float, radius: Float, width: Float, color: Int) {
        g.color = color(color)
        g.stroke = BasicStroke(width)
        g.draw(RoundRectangle2D.Float(x, y, w, h, radius * 2, radius * 2))
    }

    override fun line(x0: Float, y0: Float, x1: Float, y1: Float, width: Float, color: Int) {
        g.color = color(color)
        g.stroke = BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(Line2D.Float(x0, y0, x1, y1))
    }

    private fun awtPath(p: Path2D): java.awt.geom.Path2D.Float {
        val out = java.awt.geom.Path2D.Float()
        p.forEach { op, pts, i ->
            when (op) {
                Path2D.Op.MOVE -> out.moveTo(pts[i], pts[i + 1])
                Path2D.Op.LINE -> out.lineTo(pts[i], pts[i + 1])
                Path2D.Op.QUAD -> out.quadTo(pts[i], pts[i + 1], pts[i + 2], pts[i + 3])
                Path2D.Op.CUBIC -> out.curveTo(pts[i], pts[i + 1], pts[i + 2], pts[i + 3], pts[i + 4], pts[i + 5])
                Path2D.Op.CLOSE -> out.closePath()
            }
        }
        return out
    }

    override fun fillPath(path: Path2D, color: Int) {
        g.color = color(color)
        g.fill(awtPath(path))
    }

    override fun strokePath(path: Path2D, width: Float, color: Int) {
        g.color = color(color)
        g.stroke = BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        g.draw(awtPath(path))
    }

    private fun font(size: Float, bold: Boolean) = Font("SansSerif", if (bold) Font.BOLD else Font.PLAIN, 1).deriveFont(size)

    override fun text(text: String, x: Float, y: Float, size: Float, color: Int, align: TextAlign, bold: Boolean) {
        g.font = font(size, bold)
        g.color = color(color)
        val w = g.fontMetrics.stringWidth(text)
        val dx = when (align) {
            TextAlign.LEFT -> 0f
            TextAlign.CENTER -> -w / 2f
            TextAlign.RIGHT -> -w.toFloat()
        }
        g.drawString(text, x + dx, y)
    }

    override fun measureText(text: String, size: Float, bold: Boolean): Float {
        g.font = font(size, bold)
        return g.fontMetrics.stringWidth(text).toFloat()
    }

    fun writePng(file: File) {
        file.parentFile.mkdirs()
        ImageIO.write(image, "png", file)
    }
}
