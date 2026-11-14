package com.quissicutdeus.sniddle.game.render

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * The Christmas floor: green wrapping paper with soft stripes, a
 * printed pattern of little presents, holly and candy canes, and a red
 * ribbon each way with a bow where they cross. Everything is drawn only
 * where the camera can see it.
 */
internal object WrappingPaperArt {
    /** Soft stripes down the paper, under everything else. */
    fun drawStripes(
        c: Canvas2D,
        aw: Float,
        ah: Float,
    ) {
        val view = SceneRenderer.View
        val first = max(0, floor(view.left / STRIPE_EVERY).toInt())
        val last = min((aw / STRIPE_EVERY).toInt(), ceil(view.right / STRIPE_EVERY).toInt())
        for (k in first..last) {
            band(c, k * STRIPE_EVERY + STRIPE_EVERY / 4f, 0f, STRIPE_WIDTH, ah, STRIPE, aw, ah)
        }
    }

    private const val STRIPE_EVERY = 90f
    private const val STRIPE_WIDTH = 24f

    /** The printed pattern at grid spot ([gx], [gy]): presents, holly and candy canes in turn. */
    fun drawMotif(
        c: Canvas2D,
        x: Float,
        y: Float,
        gx: Int,
        gy: Int,
    ) {
        when ((gx / 2 + gy / 2).mod(3)) {
            0 -> drawPresent(c, x, y, PRESENT_COLORS[(gy / 2).mod(PRESENT_COLORS.size)])
            1 -> drawHolly(c, x, y)
            else -> drawCandyCane(c, x, y)
        }
    }

    private fun drawPresent(
        c: Canvas2D,
        x: Float,
        y: Float,
        box: Int,
    ) {
        val h = 13f
        c.fillRoundRect(x - h, y - h + 3f, 2f * h, 2f * h - 3f, 3f, box)
        c.fillRect(x - 2.5f, y - h + 3f, 5f, 2f * h - 3f, GOLD)
        c.fillRect(x - h, y - 1f, 2f * h, 5f, GOLD)
        c.fillCircle(x - 4.5f, y - h + 1f, 4f, GOLD)
        c.fillCircle(x + 4.5f, y - h + 1f, 4f, GOLD)
    }

    private val path = Path2D()

    /** Two holly leaves and three berries. */
    private fun drawHolly(
        c: Canvas2D,
        x: Float,
        y: Float,
    ) {
        path.reset()
        path.moveTo(x, y)
        path.quadTo(x - 8f, y - 11f, x - 18f, y - 5f)
        path.quadTo(x - 10f, y + 5f, x, y)
        path.moveTo(x, y)
        path.quadTo(x + 8f, y - 11f, x + 18f, y - 5f)
        path.quadTo(x + 10f, y + 5f, x, y)
        path.close()
        c.fillPath(path, HOLLY)
        c.fillCircle(x - 3.5f, y + 1f, 3.8f, BERRY)
        c.fillCircle(x + 3.5f, y + 1f, 3.8f, BERRY)
        c.fillCircle(x, y - 3.5f, 3.8f, BERRY)
    }

    /** A red candy cane, its white stripes painted across the stick. */
    private fun drawCandyCane(
        c: Canvas2D,
        x: Float,
        y: Float,
    ) {
        path.reset()
        path.moveTo(x + 3f, y + 14f)
        path.lineTo(x + 3f, y - 6f)
        path.quadTo(x + 3f, y - 14f, x - 3f, y - 14f)
        path.quadTo(x - 9f, y - 14f, x - 9f, y - 7f)
        c.strokePath(path, 6f, CANE)
        c.line(x + 0.5f, y + 9f, x + 5.5f, y + 6f, 2.2f, Colors.WHITE)
        c.line(x + 0.5f, y + 2f, x + 5.5f, y - 1f, 2.2f, Colors.WHITE)
        c.line(x + 0.5f, y - 5f, x + 5.5f, y - 8f, 2.2f, Colors.WHITE)
    }

    /** A red ribbon across the arena each way, and a bow where they cross. */
    fun drawRibbons(
        c: Canvas2D,
        aw: Float,
        ah: Float,
    ) {
        val rx = aw * RIBBON_X
        val ry = ah * RIBBON_Y
        val half = RIBBON_WIDTH / 2f
        band(c, rx - half, 0f, RIBBON_WIDTH, ah, RIBBON_DARK, aw, ah)
        band(c, 0f, ry - half, aw, RIBBON_WIDTH, RIBBON_DARK, aw, ah)
        band(c, rx - half + 4f, 0f, RIBBON_WIDTH - 8f, ah, RIBBON, aw, ah)
        band(c, 0f, ry - half + 4f, aw, RIBBON_WIDTH - 8f, RIBBON, aw, ah)
        band(c, rx - half * 0.3f, 0f, half * 0.6f, ah, RIBBON_SHINE, aw, ah)
        band(c, 0f, ry - half * 0.3f, aw, half * 0.6f, RIBBON_SHINE, aw, ah)
        drawBow(c, rx, ry)
    }

    private const val RIBBON_X = 0.38f
    private const val RIBBON_Y = 0.58f
    private const val RIBBON_WIDTH = 64f

    private fun drawBow(
        c: Canvas2D,
        x: Float,
        y: Float,
    ) {
        if (!SceneRenderer.View.contains(x, y, BOW)) return
        // Two tails hanging down, two loops out to the sides, a knot in the middle.
        path.reset()
        path.moveTo(x - 8f, y)
        path.lineTo(x - 52f, y + 110f)
        path.lineTo(x - 30f, y + 100f)
        path.lineTo(x - 20f, y + 118f)
        path.lineTo(x + 6f, y)
        path.moveTo(x + 8f, y)
        path.lineTo(x + 46f, y + 118f)
        path.lineTo(x + 58f, y + 98f)
        path.lineTo(x + 78f, y + 106f)
        path.lineTo(x - 2f, y)
        path.close()
        c.fillPath(path, RIBBON_DARK)
        for (side in SIDES) {
            path.reset()
            path.moveTo(x, y)
            path.cubicTo(x + side * 40f, y - 95f, x + side * 120f, y - 60f, x + side * 95f, y - 8f)
            path.cubicTo(x + side * 80f, y + 25f, x + side * 35f, y + 20f, x, y)
            path.close()
            c.fillPath(path, RIBBON_DARK)
            path.reset()
            path.moveTo(x + side * 6f, y - 4f)
            path.cubicTo(x + side * 40f, y - 78f, x + side * 104f, y - 50f, x + side * 84f, y - 10f)
            path.cubicTo(x + side * 70f, y + 14f, x + side * 36f, y + 10f, x + side * 6f, y - 4f)
            path.close()
            c.fillPath(path, RIBBON)
        }
        c.fillCircle(x, y, 22f, RIBBON_DARK)
        c.fillCircle(x - 2f, y - 2f, 17f, RIBBON)
        c.fillCircle(x - 7f, y - 8f, 6f, RIBBON_SHINE)
    }

    private const val BOW = 140f

    /** A rectangle cut to the arena and to what the camera sees (the canvas has no clipping). */
    private fun band(
        c: Canvas2D,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        color: Int,
        aw: Float,
        ah: Float,
    ) {
        val view = SceneRenderer.View
        val l = max(max(x, 0f), view.left)
        val r = min(min(x + w, aw), view.right)
        val t = max(max(y, 0f), view.top)
        val b = min(min(y + h, ah), view.bottom)
        if (r > l && b > t) c.fillRect(l, t, r - l, b - t, color)
    }

    private val STRIPE = 0xFFC8E6C1.toInt()
    private val GOLD = 0xFFE9C25A.toInt()
    private val PRESENT_COLORS = intArrayOf(0xFFD9625C.toInt(), 0xFFF2F2EE.toInt(), 0xFF6C8FD1.toInt())
    private val HOLLY = 0xFF2F7A43.toInt()
    private val BERRY = 0xFFD7473F.toInt()
    private val CANE = 0xFFD9534F.toInt()
    private val RIBBON = 0xFFD2463F.toInt()
    private val RIBBON_DARK = 0xFFA8322D.toInt()
    private const val RIBBON_SHINE = 0x40FFFFFF
}
