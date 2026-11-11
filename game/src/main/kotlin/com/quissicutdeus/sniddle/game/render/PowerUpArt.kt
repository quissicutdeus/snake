package com.quissicutdeus.sniddle.game.render

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.PowerType
import com.quissicutdeus.sniddle.game.model.PowerUp
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.TWO_PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * Power-ups on the map, each glowing its own way (Strength throbs red,
 * Speed flickers yellow with spinning sparks, Armor sends out blue ripples),
 * and the glow around a snail while a power is active.
 */
object PowerUpArt {
    private val path = Path2D()

    fun color(type: PowerType): Int =
        when (type) {
            PowerType.STRENGTH -> 0xFFFF4B2B.toInt()
            PowerType.SPEED -> 0xFFFFD21F.toInt()
            PowerType.ARMOR -> 0xFF2FB8FF.toInt()
        }

    /** A power-up lying on the map, bobbing and glowing. */
    fun drawItem(
        c: Canvas2D,
        p: PowerUp,
        time: Float,
    ) {
        val left = GameConfig.POWER_UP_LIFETIME - p.age
        // Blink just before vanishing.
        if (left < GameConfig.POWER_UP_FADE && floor(time * 8f).toInt() % 2 == 0) return
        val pop = min(1f, p.age * 3f)
        val r = GameConfig.POWER_UP_RADIUS * pop
        if (r <= 0.5f) return
        val x = p.x
        val y = p.y + sin(time * 2.5f + p.x) * 3f
        val col = color(p.type)
        when (p.type) {
            PowerType.STRENGTH -> {
                val throb = 1f + 0.18f * sin(time * 7f)
                c.fillCircle(x, y, r * 2.3f * throb, Colors.withAlpha(col, 0.16f))
                c.fillCircle(x, y, r * 1.7f * throb, Colors.withAlpha(col, 0.26f))
            }

            PowerType.SPEED -> {
                c.fillCircle(x, y, r * 1.9f, Colors.withAlpha(col, 0.18f + 0.1f * sin(time * 23f)))
                for (k in 0 until 6) {
                    val a = time * 4f + k * TWO_PI / 6f
                    c.line(x + cos(a) * r * 1.35f, y + sin(a) * r * 1.35f, x + cos(a) * r * 2.1f, y + sin(a) * r * 2.1f, r * 0.14f, col)
                }
            }

            PowerType.ARMOR -> {
                for (k in 0 until 2) {
                    val phase = (time * 0.8f + k * 0.5f) % 1f
                    c.strokeCircle(x, y, r * (1.2f + phase * 1.3f), r * 0.16f, Colors.withAlpha(col, 0.7f * (1f - phase)))
                }
                c.fillCircle(x, y, r * 1.6f, Colors.withAlpha(col, 0.2f))
            }
        }
        drawOrb(c, p.type, x, y, r)
    }

    /** The coloured ball with its white icon (also used on the HUD and screen-edge arrows). */
    fun drawOrb(
        c: Canvas2D,
        type: PowerType,
        x: Float,
        y: Float,
        r: Float,
    ) {
        val col = color(type)
        c.fillCircle(x, y, r + r * 0.12f, Colors.darken(col, 0.4f))
        c.fillCircle(x, y, r, col)
        c.fillCircle(x - r * 0.35f, y - r * 0.4f, r * 0.28f, Colors.withAlpha(Colors.WHITE, 0.6f))
        drawIcon(c, type, x, y, r * 1.25f)
    }

    /** White icon centred on (x, y), [s] across: barbell, lightning bolt or shield. */
    private fun drawIcon(
        c: Canvas2D,
        type: PowerType,
        x: Float,
        y: Float,
        s: Float,
    ) {
        val ink = Colors.withAlpha(Colors.INK, 0.55f)
        val white = Colors.WHITE
        when (type) {
            PowerType.STRENGTH -> {
                c.fillRoundRect(x - s * 0.42f, y - s * 0.06f, s * 0.84f, s * 0.12f, s * 0.05f, white)
                for (side in SIDES) {
                    c.fillRoundRect(x + side * s * 0.3f - s * 0.08f, y - s * 0.26f, s * 0.16f, s * 0.52f, s * 0.06f, white)
                    c.fillRoundRect(x + side * s * 0.45f - s * 0.06f, y - s * 0.17f, s * 0.12f, s * 0.34f, s * 0.05f, white)
                }
            }

            PowerType.SPEED -> {
                path.reset()
                path.moveTo(x - s * 0.08f, y - s * 0.46f)
                path.lineTo(x + s * 0.26f, y - s * 0.46f)
                path.lineTo(x + s * 0.04f, y - s * 0.08f)
                path.lineTo(x + s * 0.24f, y - s * 0.08f)
                path.lineTo(x - s * 0.18f, y + s * 0.5f)
                path.lineTo(x - s * 0.02f, y + s * 0.06f)
                path.lineTo(x - s * 0.24f, y + s * 0.06f)
                path.close()
                c.strokePath(path, s * 0.06f, ink)
                c.fillPath(path, white)
            }

            PowerType.ARMOR -> {
                path.reset()
                path.moveTo(x, y - s * 0.46f)
                path.lineTo(x + s * 0.36f, y - s * 0.32f)
                path.lineTo(x + s * 0.33f, y + s * 0.04f)
                path.quadTo(x + s * 0.26f, y + s * 0.34f, x, y + s * 0.48f)
                path.quadTo(x - s * 0.26f, y + s * 0.34f, x - s * 0.33f, y + s * 0.04f)
                path.lineTo(x - s * 0.36f, y - s * 0.32f)
                path.close()
                c.strokePath(path, s * 0.06f, ink)
                c.fillPath(path, white)
            }
        }
    }

    /** Glow behind a snail for each active power (blinking in the last 2 seconds). */
    fun drawAuras(
        c: Canvas2D,
        s: Snail,
        time: Float,
    ) {
        for (type in PowerType.entries) {
            val left = s.powerLeft(type)
            if (left <= 0f || (left < WARNING_SECONDS && floor(time * 8f).toInt() % 2 == 0)) continue
            val col = Colors.withAlpha(color(type), 0.28f + 0.1f * sin(time * 9f))
            // Wide enough to see at any zoom; layered powers stack their glows.
            val grow = s.radius * (0.55f + 0.15f * sin(time * 6f)) + 6f
            var i = s.segCount - 1
            while (i >= 0) {
                if (SnailArt.onScreen(s, i, grow)) c.fillCircle(s.drawX[i], s.drawY[i], s.segR[i] + grow, col)
                i -= 2
            }
        }
    }

    /** Super Armor: a shiny bubble around the head. */
    fun drawShield(
        c: Canvas2D,
        s: Snail,
        time: Float,
    ) {
        val left = s.powerLeft(PowerType.ARMOR)
        if (left <= 0f || (left < WARNING_SECONDS && floor(time * 8f).toInt() % 2 == 0)) return
        val col = color(PowerType.ARMOR)
        val r = s.radius * SnailArt.HEAD_SCALE * 1.9f
        val x = s.drawX[0]
        val y = s.drawY[0]
        c.fillCircle(x, y, r, Colors.withAlpha(col, 0.16f))
        c.strokeCircle(x, y, r, r * 0.08f, Colors.withAlpha(col, 0.85f))
        path.reset()
        path.arc(x, y, r * 0.8f, 3.6f + time, 4.6f + time, moveFirst = true, steps = 8)
        c.strokePath(path, r * 0.07f, Colors.withAlpha(Colors.WHITE, 0.75f))
    }

    /** Auras start blinking when this little time is left. */
    const val WARNING_SECONDS = 2f
}
