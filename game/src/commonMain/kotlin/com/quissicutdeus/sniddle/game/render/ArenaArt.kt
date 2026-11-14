package com.quissicutdeus.sniddle.game.render

import com.quissicutdeus.sniddle.game.model.ArenaSeason
import com.quissicutdeus.sniddle.game.model.PI_F
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * The arena floor for each [ArenaSeason]: grass with flowers, golden grass
 * with fallen leaves, snow with snowflakes, garden soil ([DirtArt]), a starry
 * sky ([SpaceArt]) or Christmas wrapping paper ([WrappingPaperArt]), inside
 * the wooden fence. Every floor draws the same grid of dots and swaps only
 * the colours and the odd decoration (space nudges its dots into stars, and
 * wrapping paper prints a pattern on every other row); the few big things
 * (planets, ribbons) are drawn only when on screen. The ground stays plain
 * enough that food dots and snails stand out on it; space is the one dark
 * floor, and everything drawn on it was checked to read there.
 */
internal object ArenaArt {
    /** The colours of one season's floor, and how often a grid spot gets a decoration instead of a dot. */
    private class Palette(
        val ground: Int,
        val dot: Int,
        val outside: Int,
        val every: Int,
    )

    private val PALETTES =
        mapOf(
            ArenaSeason.GRASS to Palette(Colors.GRASS, Colors.GRASS_DOT, Colors.OUTSIDE, every = 29),
            ArenaSeason.AUTUMN to Palette(0xFFC4D47A.toInt(), 0xFFB3C266.toInt(), 0xFF8C8F3E.toInt(), every = 13),
            ArenaSeason.SNOW to Palette(0xFFC6D8E7.toInt(), 0xFFB2C8DB.toInt(), 0xFF8BA6BC.toInt(), every = 23),
            // A garden bed in the lawn: the lawn shows behind the fence.
            ArenaSeason.DIRT to Palette(0xFFC79E74.toInt(), 0xFFB68C64.toInt(), Colors.OUTSIDE, every = 7),
            ArenaSeason.SPACE to Palette(0xFF262C5E.toInt(), 0, 0xFF161A3E.toInt(), every = 19),
            // Its pattern is printed in rows (see WrappingPaperArt.drawMotif), not scattered.
            ArenaSeason.WRAPPING_PAPER to Palette(0xFFB8DDB0.toInt(), 0xFFE9D27E.toInt(), 0xFF9E3B36.toInt(), every = 0),
        )

    /** Behind the fence, filling the screen. */
    fun outside(season: ArenaSeason): Int = PALETTES.getValue(season).outside

    private const val FENCE_WIDTH = 26f

    fun drawFloor(
        c: Canvas2D,
        season: ArenaSeason,
        aw: Float,
        ah: Float,
    ) {
        // Fence first (it sits just outside the ground).
        val fw = FENCE_WIDTH
        val palette = PALETTES.getValue(season)
        c.fillRoundRect(-fw, -fw, aw + 2 * fw, ah + 2 * fw, 34f, Colors.FENCE_DARK)
        c.fillRoundRect(-fw * 0.6f, -fw * 0.6f, aw + 1.2f * fw, ah + 1.2f * fw, 26f, Colors.FENCE)
        c.fillRect(0f, 0f, aw, ah, palette.ground)
        if (season == ArenaSeason.WRAPPING_PAPER) WrappingPaperArt.drawStripes(c, aw, ah)
        drawGround(c, season, palette, aw, ah)
        when (season) {
            ArenaSeason.SPACE -> SpaceArt.drawSkyObjects(c, aw, ah)
            ArenaSeason.WRAPPING_PAPER -> WrappingPaperArt.drawRibbons(c, aw, ah)
            else -> Unit
        }
        drawFencePosts(c, aw, ah)
    }

    /** Dots and the odd decoration, on a fixed grid so they don't move. */
    private fun drawGround(
        c: Canvas2D,
        season: ArenaSeason,
        palette: Palette,
        aw: Float,
        ah: Float,
    ) {
        val view = SceneRenderer.View
        val span = max(view.right - view.left, view.bottom - view.top)
        val step = if (span > 2600f) 180f else 90f
        val gx0 = max(0, floor(view.left / step).toInt())
        val gx1 = min((aw / step).toInt(), ceil(view.right / step).toInt())
        val gy0 = max(0, floor(view.top / step).toInt())
        val gy1 = min((ah / step).toInt(), ceil(view.bottom / step).toInt())
        val inner = 10f
        // Decorations only when zoomed in, where they're big enough to see.
        val decorate = step == 90f
        for (gy in gy0..gy1) {
            for (gx in gx0..gx1) {
                val x = gx * step + (if (gy % 2 == 0) 0f else step / 2f)
                val y = gy * step
                if (x !in inner..(aw - inner) || y !in inner..(ah - inner)) continue
                drawSpot(c, season, palette, x, y, gx, gy, decorate)
            }
        }
    }

    /** One grid spot: a dot, a star, a decoration or a bit of the printed pattern. */
    @Suppress("LongParameterList") // where the spot is, and what goes there
    private fun drawSpot(
        c: Canvas2D,
        season: ArenaSeason,
        palette: Palette,
        x: Float,
        y: Float,
        gx: Int,
        gy: Int,
        decorate: Boolean,
    ) {
        val hash = (gx * 73856093) xor (gy * 19349663)
        when {
            season == ArenaSeason.WRAPPING_PAPER -> {
                if (decorate && gx % 2 == 0 && gy % 2 == 0) {
                    WrappingPaperArt.drawMotif(c, x, y, gx, gy)
                } else {
                    c.fillCircle(x, y, 4.5f, palette.dot)
                }
            }

            decorate && hash.mod(palette.every) == 0 -> {
                drawDecoration(c, season, x, y, hash)
            }

            season == ArenaSeason.SPACE -> {
                SpaceArt.drawStar(c, x, y, hash)
            }

            else -> {
                c.fillCircle(x, y, 6f, palette.dot)
            }
        }
    }

    private fun drawDecoration(
        c: Canvas2D,
        season: ArenaSeason,
        x: Float,
        y: Float,
        hash: Int,
    ) {
        when (season) {
            ArenaSeason.GRASS -> drawFlower(c, x, y, FLOWER_COLORS[hash.mod(FLOWER_COLORS.size)])
            ArenaSeason.AUTUMN -> drawLeaf(c, x, y, hash)
            ArenaSeason.SNOW -> drawSnowflake(c, x, y)
            ArenaSeason.DIRT -> DirtArt.drawDecoration(c, x, y, hash)
            ArenaSeason.SPACE -> SpaceArt.drawDecoration(c, x, y, hash)
            ArenaSeason.WRAPPING_PAPER -> Unit // printed in rows instead, see drawGround
        }
    }

    private fun drawFencePosts(
        c: Canvas2D,
        aw: Float,
        ah: Float,
    ) {
        val view = SceneRenderer.View
        val post = 250f
        val r = 15f
        val off = FENCE_WIDTH * 0.5f
        var p = 0f
        while (p <= aw) {
            if (view.contains(p, -off, r)) c.fillCircle(p, -off, r, Colors.FENCE_DARK)
            if (view.contains(p, ah + off, r)) c.fillCircle(p, ah + off, r, Colors.FENCE_DARK)
            p += post
        }
        p = 0f
        while (p <= ah) {
            if (view.contains(-off, p, r)) c.fillCircle(-off, p, r, Colors.FENCE_DARK)
            if (view.contains(aw + off, p, r)) c.fillCircle(aw + off, p, r, Colors.FENCE_DARK)
            p += post
        }
    }

    private val FLOWER_COLORS = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFF176.toInt(), 0xFFFFB3D9.toInt(), 0xFFB3E5FC.toInt())

    private fun drawFlower(
        c: Canvas2D,
        x: Float,
        y: Float,
        petal: Int,
    ) {
        val r = 6f
        c.fillCircle(x - r, y, r, petal)
        c.fillCircle(x + r, y, r, petal)
        c.fillCircle(x, y - r, r, petal)
        c.fillCircle(x, y + r, r, petal)
        c.fillCircle(x, y, r * 0.8f, 0xFFFFB300.toInt())
    }

    private val path = Path2D()

    /** A fallen leaf: a pointed oval in a soft autumn colour, lying at one of a few angles. */
    private fun drawLeaf(
        c: Canvas2D,
        x: Float,
        y: Float,
        hash: Int,
    ) {
        val k = hash.ushr(4).mod(LEAF_ANGLES)
        val dx = LEAF_DX[k] * LEAF_HALF_LENGTH
        val dy = LEAF_DY[k] * LEAF_HALF_LENGTH
        // Sideways from the leaf's middle line, for its two curved edges.
        val sx = -LEAF_DY[k] * LEAF_HALF_WIDTH
        val sy = LEAF_DX[k] * LEAF_HALF_WIDTH
        path.reset()
        path.moveTo(x - dx, y - dy)
        path.quadTo(x + sx, y + sy, x + dx, y + dy)
        path.quadTo(x - sx, y - sy, x - dx, y - dy)
        path.close()
        c.fillPath(path, LEAF_COLORS[hash.ushr(8).mod(LEAF_COLORS.size)])
    }

    /** A soft six-armed snowflake: three crossing lines in one stroke. */
    private fun drawSnowflake(
        c: Canvas2D,
        x: Float,
        y: Float,
    ) {
        val r = 9f
        path.reset()
        for (k in SNOW_DX.indices) {
            path.moveTo(x - SNOW_DX[k] * r, y - SNOW_DY[k] * r)
            path.lineTo(x + SNOW_DX[k] * r, y + SNOW_DY[k] * r)
        }
        c.strokePath(path, 2.4f, SNOWFLAKE)
    }

    private const val LEAF_ANGLES = 6
    private const val LEAF_HALF_LENGTH = 11f
    private const val LEAF_HALF_WIDTH = 10f

    /** Directions a leaf can lie along, 30 degrees apart, worked out once. */
    private val LEAF_DX = FloatArray(LEAF_ANGLES) { cos(it * PI_F / LEAF_ANGLES) }
    private val LEAF_DY = FloatArray(LEAF_ANGLES) { sin(it * PI_F / LEAF_ANGLES) }

    /** A snowflake's three lines, 60 degrees apart and turned a little off square. */
    private val SNOW_DX = FloatArray(3) { cos(it * PI_F / 3f + 0.26f) }
    private val SNOW_DY = FloatArray(3) { sin(it * PI_F / 3f + 0.26f) }

    /** Muted so they read as part of the ground, not as food. */
    private val LEAF_COLORS = intArrayOf(0xB3E07B39.toInt(), 0xB3C8553A.toInt(), 0xB3D9A441.toInt(), 0xB3A0703F.toInt())
    private val SNOWFLAKE = 0xE6FFFFFF.toInt()
}
