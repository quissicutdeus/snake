package com.quissicutdeus.sniddle.game.sim

import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * A floating thumbstick in screen pixels. Where the thumb first touches is the
 * centre; the snail heads the way the thumb is dragged from there. Dragging
 * further than [leash] pulls the centre along behind the thumb, so turning
 * around is always a short flick back, never a trip across the screen.
 *
 * Within [deadZone] of the centre there is no direction (keep going straight).
 */
class Thumbstick(
    private val deadZone: Float,
    private val leash: Float,
) {
    var down = false
        private set
    var centerX = 0f
        private set
    var centerY = 0f
        private set
    var thumbX = 0f
        private set
    var thumbY = 0f
        private set

    fun press(
        x: Float,
        y: Float,
    ) {
        down = true
        centerX = x
        centerY = y
        thumbX = x
        thumbY = y
    }

    fun move(
        x: Float,
        y: Float,
    ) {
        if (!down) return press(x, y)
        thumbX = x
        thumbY = y
        val dx = x - centerX
        val dy = y - centerY
        val d = sqrt(dx * dx + dy * dy)
        if (d > leash) {
            centerX = x - dx / d * leash
            centerY = y - dy / d * leash
        }
    }

    fun release() {
        down = false
    }

    /** Heading in radians (screen axes: y points down), or null for "no change". */
    fun heading(): Float? {
        if (!down) return null
        val dx = thumbX - centerX
        val dy = thumbY - centerY
        return if (dx * dx + dy * dy > deadZone * deadZone) atan2(dy, dx) else null
    }
}
