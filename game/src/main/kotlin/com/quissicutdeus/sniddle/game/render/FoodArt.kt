package com.quissicutdeus.sniddle.game.render

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.Pellet
import com.quissicutdeus.sniddle.game.model.TailPiece
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Food dots and dropped tail pieces. */
object FoodArt {
    fun drawPellet(
        c: Canvas2D,
        p: Pellet,
        time: Float,
    ) {
        val pop = min(1f, p.age * 4f)
        val r = p.radius * pop * (1f + 0.12f * sin(time * 4f + p.phase))
        if (r <= 0.1f) return
        c.fillCircle(p.x, p.y, r * 1.9f, Colors.withAlpha(p.color, 0.22f))
        c.fillCircle(p.x, p.y, r, p.color)
        c.fillCircle(p.x - r * 0.3f, p.y - r * 0.32f, r * 0.36f, Colors.withAlpha(Colors.WHITE, 0.75f))
    }

    fun drawTailPiece(
        c: Canvas2D,
        t: TailPiece,
        time: Float,
    ) {
        val left = GameConfig.TAIL_PIECE_LIFETIME - t.age
        val fade = (left / GameConfig.TAIL_PIECE_FADE).coerceIn(0f, 1f)
        val pop = min(1f, 0.6f + t.age * 3f)
        val r = t.radius * pop * (1f + 0.06f * sin(time * 3f + t.index))
        val bodyR = t.radius / GameConfig.TAIL_PIECE_RADIUS_FACTOR
        val col = SnailArt.slimeColor(t.style, t.index * bodyR * GameConfig.SEGMENT_SPACING_FACTOR, bodyR)
        c.fillCircle(t.x, t.y, r + max(1.5f, r * 0.2f), Colors.withAlpha(Colors.darken(t.style.bodyColor, 0.45f), fade))
        c.fillCircle(t.x, t.y, r, Colors.withAlpha(col, fade))
        c.fillCircle(t.x - r * 0.3f, t.y - r * 0.34f, r * 0.32f, Colors.withAlpha(Colors.lighten(col, 0.5f), 0.7f * fade))
    }
}
