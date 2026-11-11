package com.quissicutdeus.sniddle.game.render

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.dist2
import com.quissicutdeus.sniddle.game.sim.GameSim
import com.quissicutdeus.sniddle.game.sim.isDangerTo
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Markers at the screen edge for things just off-screen: glowing arrows to
 * power-ups, and red warning arrows with an angry little face for bigger
 * snails heading the player's way.
 */
internal object EdgeMarkers {
    private val arrow = Path2D()

    // Where the current marker goes (set by place()), in screen pixels.
    private var mx = 0f
    private var my = 0f
    private var angle = 0f

    /**
     * Finds where the line from the screen centre towards a point ([dx], [dy]
     * from the centre) leaves the (inset) screen. Along the top it stays below
     * the pause button. Returns false if the point is on screen.
     */
    fun place(
        c: Canvas2D,
        dx: Float,
        dy: Float,
        ui: Float,
    ): Boolean {
        val cx = c.width / 2f
        val cy = c.height / 2f
        if (abs(dx) < cx && abs(dy) < cy) return false
        val inset = 26f * ui
        val limitY = if (dy < 0f) cy - 76f * ui else cy - inset
        val k = min((cx - inset) / max(abs(dx), 1e-3f), limitY / max(abs(dy), 1e-3f))
        mx = cx + dx * k
        my = cy + dy * k
        angle = atan2(dy, dx)
        return true
    }

    /** An arrow at the placed spot, pointing outwards. */
    fun drawArrow(
        c: Canvas2D,
        color: Int,
        size: Float,
    ) {
        val tip = 25f * size
        val wing = 9f * size * 1.4f
        arrow.reset()
        arrow.moveTo(mx + cos(angle) * tip, my + sin(angle) * tip)
        arrow.lineTo(mx + cos(angle + 1.2f) * wing, my + sin(angle + 1.2f) * wing)
        arrow.lineTo(mx + cos(angle - 1.2f) * wing, my + sin(angle - 1.2f) * wing)
        arrow.close()
        c.fillPath(arrow, color)
    }

    val x get() = mx
    val y get() = my

    /** Warning arrows (nearest few) for bigger snails coming at the player from off-screen. */
    fun drawDangers(
        c: Canvas2D,
        sim: GameSim,
        me: Snail?,
        cam: Camera,
        ui: Float,
        time: Float,
    ) {
        val p = me?.takeIf { it.alive } ?: return
        val pulse = 1f + 0.12f * sin(time * 9f)
        // The nearest few of sim.dangers(me) that are off-screen, picked without
        // making a list (this runs every frame).
        var found = 0
        val snails = sim.world.snails
        for (i in snails.indices) {
            val s = snails[i]
            if (isDangerTo(s, p) && offScreen(c, s, cam)) found = keepIfNear(s, dist2(s.x, s.y, p.x, p.y), found)
        }
        for (i in 0 until found) {
            val s = nearest[i] ?: continue
            nearest[i] = null
            val dx = cam.worldToScreenX(s.x, c.width) - c.width / 2f
            val dy = cam.worldToScreenY(s.y, c.height) - c.height / 2f
            if (place(c, dx, dy, ui)) {
                drawArrow(c, Colors.ANGRY_RED, ui * pulse)
                drawAngryHead(c, s, 11f * ui * pulse)
            }
        }
    }

    /**
     * Slides [s], [d2] from the player (squared), into place among the [found]
     * snails in [nearest], nearest first (ties keep the order they came in);
     * the furthest drops off a full list. Returns how many there are now.
     */
    private fun keepIfNear(
        s: Snail,
        d2: Float,
        found: Int,
    ): Int {
        val full = found == nearest.size
        var at = if (full) found - 1 else found
        if (full && d2 >= nearestD2[at]) return found
        while (at > 0 && d2 < nearestD2[at - 1]) {
            nearest[at] = nearest[at - 1]
            nearestD2[at] = nearestD2[at - 1]
            at--
        }
        nearest[at] = s
        nearestD2[at] = d2
        return if (full) found else found + 1
    }

    // The snails getting an arrow this frame and how far (squared) each is from the player.
    private val nearest = arrayOfNulls<Snail>(GameConfig.MAX_DANGER_ARROWS)
    private val nearestD2 = FloatArray(GameConfig.MAX_DANGER_ARROWS)

    /** The head must be fully off-screen (its radius counts) to need an arrow. */
    private fun offScreen(
        c: Canvas2D,
        s: Snail,
        cam: Camera,
    ): Boolean {
        val r = s.radius * cam.scale
        return abs(cam.worldToScreenX(s.x, c.width) - c.width / 2f) - r >= c.width / 2f ||
            abs(cam.worldToScreenY(s.y, c.height) - c.height / 2f) - r >= c.height / 2f
    }

    /** A tiny head in the snail's colour with the angry red outline and frown. */
    private fun drawAngryHead(
        c: Canvas2D,
        s: Snail,
        r: Float,
    ) {
        c.fillCircle(mx, my, r, s.style.bodyColor)
        c.strokeCircle(mx, my, r, r * 0.22f, Colors.ANGRY_RED)
        for (side in SIDES) {
            val ex = mx + side * r * 0.38f
            val ey = my + r * 0.05f
            c.fillCircle(ex, ey, r * 0.24f, Colors.WHITE)
            c.fillCircle(ex, ey + r * 0.04f, r * 0.12f, Colors.INK)
            // Brows slant down towards the middle (outer end high, inner end low).
            c.line(ex + side * r * 0.3f, ey - r * 0.5f, ex - side * r * 0.14f, ey - r * 0.24f, r * 0.13f, Colors.INK)
        }
    }
}
