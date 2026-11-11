package com.quissicutdeus.sniddle.game.render

import kotlin.math.ceil

/** Extra drawing for a two-player game, on top of a kid's half of the screen. */
object TwoPlayerArt {
    /** Over a gobbled kid's half: "Oops!", who did it, and "Back in 3…" counting down. */
    fun drawComingBack(
        c: Canvas2D,
        secondsLeft: Float,
        gobbledBy: String?,
    ) {
        val ui = SceneRenderer.uiUnit(c)
        val cx = c.width / 2f
        val cy = c.height / 2f
        c.fillRect(0f, 0f, c.width, c.height, Colors.withAlpha(Colors.INK, 0.35f))
        SceneRenderer.shadowText(c, "Oops!", cx, cy - 24f * ui, 54f * ui, Colors.WHITE, TextAlign.CENTER, 3f * ui)
        if (gobbledBy != null) {
            SceneRenderer.shadowText(c, "$gobbledBy got you!", cx, cy + 12f * ui, 20f * ui, Colors.WHITE, TextAlign.CENTER, 2f * ui)
        }
        val count = ceil(secondsLeft).toInt().coerceAtLeast(1)
        SceneRenderer.shadowText(c, "Back in $count…", cx, cy + 50f * ui, 28f * ui, Colors.GOLD, TextAlign.CENTER, 2f * ui)
    }
}
