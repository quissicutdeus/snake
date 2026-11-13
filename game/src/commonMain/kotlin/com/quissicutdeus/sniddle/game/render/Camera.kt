package com.quissicutdeus.sniddle.game.render

import com.quissicutdeus.sniddle.game.GameConfig
import kotlin.math.exp
import kotlin.math.min

/** Follows the player smoothly and zooms out a little as the player grows. */
class Camera {
    var x = GameConfig.ARENA_WIDTH / 2f
        private set
    var y = GameConfig.ARENA_HEIGHT / 2f
        private set

    /** Screen pixels per world unit. */
    var scale = 1f
        private set
    private var initialized = false

    fun follow(
        tx: Float,
        ty: Float,
        radius: Float,
        screenW: Float,
        screenH: Float,
        dt: Float,
    ) {
        val target = min(screenW, screenH) / GameConfig.viewSpanFor(radius)
        if (!initialized) {
            x = tx
            y = ty
            scale = target
            initialized = true
            return
        }
        val k = 1f - exp(-dt * 9f)
        x += (tx - x) * k
        y += (ty - y) * k
        scale += (target - scale) * (1f - exp(-dt * 2f))
    }

    /** Follows [s]'s head as it is drawn this frame (blended between steps, see Snail.prepareDraw). */
    fun followSnail(
        s: com.quissicutdeus.sniddle.game.model.Snail,
        alpha: Float,
        screenW: Float,
        screenH: Float,
        dt: Float,
    ) {
        s.prepareDraw(alpha)
        follow(s.drawX[0], s.drawY[0], s.radius, screenW, screenH, dt)
    }

    fun snapTo(
        tx: Float,
        ty: Float,
        radius: Float,
        screenW: Float,
        screenH: Float,
    ) {
        initialized = false
        follow(tx, ty, radius, screenW, screenH, 0f)
    }

    fun worldToScreenX(
        wx: Float,
        screenW: Float,
    ) = (wx - x) * scale + screenW / 2f

    fun worldToScreenY(
        wy: Float,
        screenH: Float,
    ) = (wy - y) * scale + screenH / 2f
}
