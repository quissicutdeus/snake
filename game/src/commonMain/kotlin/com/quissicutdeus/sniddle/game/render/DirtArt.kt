package com.quissicutdeus.sniddle.game.render

/**
 * The garden-soil floor's decorations: little pebbles, sprouts and worm
 * holes (with the odd worm peeking out). Soft, earthy colours so none of it
 * looks like food, and none costs more than a flower.
 */
internal object DirtArt {
    fun drawDecoration(
        c: Canvas2D,
        x: Float,
        y: Float,
        hash: Int,
    ) {
        when (hash.ushr(5).mod(KINDS)) {
            0 -> drawPebbles(c, x, y, hash)
            1, 2 -> drawSprout(c, x, y, hash)
            else -> drawWormHole(c, x, y, hash)
        }
    }

    private const val KINDS = 5

    private fun drawPebbles(
        c: Canvas2D,
        x: Float,
        y: Float,
        hash: Int,
    ) {
        val flip = if (hash.ushr(9) and 1 == 0) 1f else -1f
        c.fillCircle(x - 5f * flip, y + 2f, 8f, PEBBLE_DARK)
        c.fillCircle(x - 6f * flip, y + 0.5f, 6.4f, PEBBLE)
        c.fillCircle(x + 8f * flip, y - 5f, 5.5f, PEBBLE_DARK)
        c.fillCircle(x + 7.5f * flip, y - 6f, 4.2f, PEBBLE)
    }

    private val path = Path2D()

    /** Two round leaves on a short stem, coming up out of a little mound. */
    private fun drawSprout(
        c: Canvas2D,
        x: Float,
        y: Float,
        hash: Int,
    ) {
        val lean = (hash.ushr(9).mod(5) - 2) * 1.2f
        c.fillCircle(x, y + 8f, 7f, MOUND)
        c.line(x, y + 7f, x + lean, y - 4f, 2.6f, STEM)
        val tx = x + lean
        val ty = y - 4f
        path.reset()
        // Left leaf, then right leaf: pointed ovals out from the top of the stem.
        path.moveTo(tx, ty)
        path.quadTo(tx - 6f, ty - 10f, tx - 13f, ty - 6f)
        path.quadTo(tx - 7f, ty + 1f, tx, ty)
        path.moveTo(tx, ty)
        path.quadTo(tx + 6f, ty - 11f, tx + 13f, ty - 8f)
        path.quadTo(tx + 8f, ty, tx, ty)
        path.close()
        c.fillPath(path, LEAF)
    }

    /** A dark hole with a soft rim; now and then a worm peeks out and says hello. */
    private fun drawWormHole(
        c: Canvas2D,
        x: Float,
        y: Float,
        hash: Int,
    ) {
        c.fillCircle(x, y + 1.5f, 10f, HOLE_RIM)
        c.fillCircle(x, y, 7.5f, HOLE)
        if (hash.ushr(11).mod(2) != 0) return
        path.reset()
        path.moveTo(x - 1f, y + 1f)
        path.quadTo(x - 2f, y - 17f, x + 8f, y - 17f)
        c.strokePath(path, 7f, WORM)
        c.fillCircle(x + 7f, y - 19f, 1.6f, Colors.INK)
    }

    // Muted, so they read as part of the ground, not as food.
    private val PEBBLE = 0xFFB7AEA2.toInt()
    private val PEBBLE_DARK = 0xFF978C80.toInt()
    private val MOUND = 0xFFB28A63.toInt()
    private val STEM = 0xFF6E9A45.toInt()
    private val LEAF = 0xFF84B852.toInt()
    private val HOLE_RIM = 0xFFAF8760.toInt()
    private val HOLE = 0xFF6F4D35.toInt()
    private val WORM = 0xFFEC9DA0.toInt()
}
