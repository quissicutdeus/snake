package com.quissicutdeus.sniddle.game.render

import com.quissicutdeus.sniddle.game.model.PI_F

/**
 * The space floor: a dark blue sky with little stars where the other floors
 * have dots, the odd twinkle or tiny moon, and a few big things at fixed
 * places (a ringed planet, a blue-green planet, a cratered moon and a comet).
 * The stars are small and soft so they never look like food.
 */
internal object SpaceArt {
    /** A little star in place of a dot, nudged off the grid so the sky doesn't look like a pattern. */
    fun drawStar(
        c: Canvas2D,
        x: Float,
        y: Float,
        hash: Int,
    ) {
        val sx = x + (hash.ushr(3).mod(JITTER) - JITTER / 2)
        val sy = y + (hash.ushr(9).mod(JITTER) - JITTER / 2)
        val r = 1.6f + hash.ushr(15).mod(4) * 0.6f
        c.fillCircle(sx, sy, r, STAR_COLORS[hash.ushr(19).mod(STAR_COLORS.size)])
    }

    private const val JITTER = 41

    fun drawDecoration(
        c: Canvas2D,
        x: Float,
        y: Float,
        hash: Int,
    ) {
        if (hash.ushr(5).mod(4) == 0) drawTinyMoon(c, x, y) else drawTwinkle(c, x, y, 7f + hash.ushr(7).mod(3) * 2.5f)
    }

    private val path = Path2D()

    /** A four-pointed twinkle, its sides curved in. */
    private fun drawTwinkle(
        c: Canvas2D,
        x: Float,
        y: Float,
        r: Float,
    ) {
        c.fillCircle(x, y, r * 0.5f, TWINKLE_GLOW)
        path.reset()
        path.moveTo(x, y - r)
        path.quadTo(x, y, x + r, y)
        path.quadTo(x, y, x, y + r)
        path.quadTo(x, y, x - r, y)
        path.quadTo(x, y, x, y - r)
        path.close()
        c.fillPath(path, TWINKLE)
    }

    private fun drawTinyMoon(
        c: Canvas2D,
        x: Float,
        y: Float,
    ) {
        c.fillCircle(x, y, 8f, MOON_DARK)
        c.fillCircle(x - 1f, y - 1f, 6.8f, MOON)
        c.fillCircle(x + 2f, y + 1.5f, 1.8f, MOON_DARK)
    }

    /** The big things, at fixed places in the arena, drawn over the stars. */
    fun drawSkyObjects(
        c: Canvas2D,
        aw: Float,
        ah: Float,
    ) {
        drawRingedPlanet(c, aw * 0.28f, ah * 0.3f, 120f)
        drawPlanet(c, aw * 0.74f, ah * 0.7f, 95f)
        drawMoon(c, aw * 0.18f, ah * 0.8f, 55f)
        drawComet(c, aw * 0.66f, ah * 0.24f)
        drawMoon(c, aw * 0.88f, ah * 0.12f, 38f)
        drawPlanet(c, aw * 0.45f, ah * 0.93f, 60f)
    }

    private fun drawRingedPlanet(
        c: Canvas2D,
        cx: Float,
        cy: Float,
        r: Float,
    ) {
        if (!SceneRenderer.View.contains(cx, cy, r * 2f)) return
        // The ring's far side, the planet, then the near side over it.
        ring(c, cx, cy, r, near = false)
        c.fillCircle(cx, cy, r, RINGED_DARK)
        c.fillCircle(cx - r * 0.08f, cy - r * 0.08f, r * 0.9f, RINGED)
        c.fillCircle(cx - r * 0.35f, cy - r * 0.38f, r * 0.28f, RINGED_SHINE)
        ring(c, cx, cy, r, near = true)
    }

    private fun ring(
        c: Canvas2D,
        cx: Float,
        cy: Float,
        r: Float,
        near: Boolean,
    ) {
        c.save()
        c.translate(cx, cy)
        c.rotate(-0.32f)
        c.scale(1f, RING_TILT)
        path.reset()
        // Down the screen is the near side of a ring tipped towards us.
        if (near) path.arc(0f, 0f, r * 1.6f, 0f, PI_F, moveFirst = true) else path.arc(0f, 0f, r * 1.6f, PI_F, 2f * PI_F, moveFirst = true)
        c.strokePath(path, r * 0.34f, RING)
        c.restore()
    }

    private const val RING_TILT = 0.32f

    private fun drawPlanet(
        c: Canvas2D,
        cx: Float,
        cy: Float,
        r: Float,
    ) {
        if (!SceneRenderer.View.contains(cx, cy, r)) return
        c.fillCircle(cx, cy, r, PLANET_DARK)
        c.fillCircle(cx - r * 0.08f, cy - r * 0.08f, r * 0.9f, PLANET)
        // A couple of green islands and a shine.
        c.fillCircle(cx + r * 0.25f, cy + r * 0.2f, r * 0.3f, PLANET_LAND)
        c.fillCircle(cx - r * 0.35f, cy + r * 0.3f, r * 0.18f, PLANET_LAND)
        c.fillCircle(cx - r * 0.35f, cy - r * 0.38f, r * 0.24f, PLANET_SHINE)
    }

    private fun drawMoon(
        c: Canvas2D,
        cx: Float,
        cy: Float,
        r: Float,
    ) {
        if (!SceneRenderer.View.contains(cx, cy, r)) return
        c.fillCircle(cx, cy, r, MOON_DARK)
        c.fillCircle(cx - r * 0.08f, cy - r * 0.08f, r * 0.9f, MOON)
        c.fillCircle(cx + r * 0.3f, cy + r * 0.15f, r * 0.22f, MOON_DARK)
        c.fillCircle(cx - r * 0.3f, cy + r * 0.35f, r * 0.14f, MOON_DARK)
        c.fillCircle(cx - r * 0.1f, cy - r * 0.4f, r * 0.12f, MOON_DARK)
    }

    /** A comet heading down and to the left, its tail streaming up behind it. */
    private fun drawComet(
        c: Canvas2D,
        hx: Float,
        hy: Float,
    ) {
        val len = 460f
        if (!SceneRenderer.View.contains(hx + len * 0.4f, hy - len * 0.3f, len * 0.6f)) return
        // Tail: three tapering layers, wide and faint to narrow and bright. Along the tail is
        // (TAIL_DX, TAIL_DY); across it is (-TAIL_DY, TAIL_DX).
        for (k in TAIL_WIDTHS.indices) {
            val w = TAIL_WIDTHS[k]
            val l = len * TAIL_LENGTHS[k]
            val nx = -TAIL_DY * w
            val ny = TAIL_DX * w
            val mx = hx + TAIL_DX * l * 0.45f
            val my = hy + TAIL_DY * l * 0.45f
            path.reset()
            path.moveTo(hx + nx, hy + ny)
            path.quadTo(mx + nx * 0.8f, my + ny * 0.8f, hx + TAIL_DX * l, hy + TAIL_DY * l)
            path.quadTo(mx - nx * 0.8f, my - ny * 0.8f, hx - nx, hy - ny)
            path.quadTo(hx - TAIL_DX * w * 1.3f, hy - TAIL_DY * w * 1.3f, hx + nx, hy + ny)
            path.close()
            c.fillPath(path, TAIL_COLORS[k])
        }
        c.fillCircle(hx, hy, 22f, COMET_HEAD)
        c.fillCircle(hx - 6f, hy - 6f, 8f, Colors.WHITE)
    }

    // The tail points up and to the right (y points down).
    private const val TAIL_DX = 0.8f
    private const val TAIL_DY = -0.6f
    private val TAIL_WIDTHS = floatArrayOf(48f, 32f, 18f)
    private val TAIL_LENGTHS = floatArrayOf(1f, 0.82f, 0.6f)
    private val TAIL_COLORS = intArrayOf(0x3384C8FF, 0x55A8DCFF, 0x99E6F6FF.toInt())

    private val STAR_COLORS = intArrayOf(0xB3FFFFFF.toInt(), 0xB3FFF1B8.toInt(), 0xB3C4DAFF.toInt(), 0x80FFFFFF.toInt())
    private val TWINKLE = 0xE6FFF4C4.toInt()
    private const val TWINKLE_GLOW = 0x33FFF4C4
    private val MOON = 0xFFC9CCD8.toInt()
    private val MOON_DARK = 0xFFA4A8B9.toInt()
    private val RINGED = 0xFFE9B577.toInt()
    private val RINGED_DARK = 0xFFC98F55.toInt()
    private const val RINGED_SHINE = 0x40FFFFFF
    private val RING = 0xB3F4DDA8.toInt()
    private val PLANET = 0xFF5FA8D8.toInt()
    private val PLANET_DARK = 0xFF4683B3.toInt()
    private val PLANET_LAND = 0xFF7CC08A.toInt()
    private const val PLANET_SHINE = 0x40FFFFFF
    private val COMET_HEAD = 0xFFCFEFFF.toInt()
}
