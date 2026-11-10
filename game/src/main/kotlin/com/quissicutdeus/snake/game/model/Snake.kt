package com.quissicutdeus.snake.game.model

import com.quissicutdeus.snake.game.GameConfig
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * One snake. The head moves; the body is the trail the head left behind, cut
 * off at [length]. After eating, [length] grows at once but the visible body
 * catches up as the head moves forward (the tail simply waits), which looks
 * like natural growth.
 */
class Snake(
    val id: Int,
    val name: String,
    val style: SnakeStyle,
    val isPlayer: Boolean,
    x: Float,
    y: Float,
    heading: Float,
    length: Float,
) {
    var x = x
        private set
    var y = y
        private set
    var heading = heading
    var length = length
        private set(value) {
            field = value
            radius = GameConfig.radiusFor(value)
            baseSpeed = baseSpeedFor(value)
        }

    // Derived from [length] and kept up to date by its setter: both curves use
    // pow(), and radius is read for every body circle and every food item.
    var radius = GameConfig.radiusFor(length)
        private set
    private var baseSpeed = baseSpeedFor(length)

    private fun baseSpeedFor(length: Float) = GameConfig.speedFor(length) * (if (isPlayer) 1f else GameConfig.AI_SPEED_FACTOR)

    /** Longest length ever reached (the player's score). */
    private var bestLength = length

    var hearts = if (isPlayer) GameConfig.PLAYER_HEARTS else GameConfig.AI_HEARTS
    var alive = true
    var snakesEaten = 0

    /** Seconds of protection left (blinking after being hurt, or after losing the tail). */
    var invulnerable = 0f

    /** Seconds left during which steering is ignored (after a bounce or a wall). */
    var steerLock = 0f

    /** Seconds left of extra knock-back speed after a bounce. */
    var knockback = 0f

    /** Seconds since the snake last ate something (used for the "squash" effect). */
    var sinceAte = 10f

    /** Seconds left of each power (indexed by [PowerType.ordinal]). */
    private val powerTime = FloatArray(PowerType.entries.size)

    /** Seconds since spawned. */
    var age = 0f

    private val trail = Trail()

    /** AI state; null for the player. */
    var brain: com.quissicutdeus.snake.game.sim.AiBrain? = null

    // Body circles sampled from the head (index 0) towards the tail.
    var segCount = 0
        private set
    var segX = FloatArray(32)
        private set
    var segY = FloatArray(32)
        private set
    var segR = FloatArray(32)
        private set

    /** Distance along the body from the head. */
    var segD = FloatArray(32)
        private set

    // Body circles as they were before the latest step, and the blended
    // positions actually drawn. Drawing between two 60 Hz steps keeps motion
    // smooth on 90/120 Hz screens.
    private var prevX = FloatArray(32)
    private var prevY = FloatArray(32)
    private var prevCount = 0
    var drawX = FloatArray(32)
        private set
    var drawY = FloatArray(32)
        private set

    val speed: Float
        get() = if (hasPower(PowerType.SPEED)) baseSpeed * GameConfig.SPEED_POWER_FACTOR else baseSpeed

    fun hasPower(type: PowerType): Boolean = powerTime[type.ordinal] > 0f

    /** Seconds left of [type] (0 when not active). */
    fun powerLeft(type: PowerType): Float = powerTime[type.ordinal]

    /** Starts (or tops up) a power. */
    fun givePower(type: PowerType) {
        powerTime[type.ordinal] = type.seconds
    }

    fun tickPowers(dt: Float) {
        for (i in powerTime.indices) powerTime[i] = max(0f, powerTime[i] - dt)
    }

    val score: Int get() = GameConfig.scoreFor(bestLength)

    init {
        trail.pushFront(x, y)
    }

    /** Remembers the current body before a step (see [prepareDraw]). */
    fun rememberPrevious() {
        if (prevX.size < segCount) {
            prevX = FloatArray(segX.size)
            prevY = FloatArray(segX.size)
        }
        segX.copyInto(prevX, 0, 0, segCount)
        segY.copyInto(prevY, 0, 0, segCount)
        prevCount = segCount
    }

    /**
     * Fills [drawX]/[drawY] with the body blended [alpha] (0..1) of the way
     * from the previous step to the current one.
     */
    fun prepareDraw(alpha: Float) {
        if (drawX.size < segCount) {
            drawX = FloatArray(segX.size)
            drawY = FloatArray(segX.size)
        }
        val a = alpha.coerceIn(0f, 1f)
        for (i in 0 until segCount) {
            if (i < prevCount) {
                drawX[i] = prevX[i] + (segX[i] - prevX[i]) * a
                drawY[i] = prevY[i] + (segY[i] - prevY[i]) * a
            } else {
                drawX[i] = segX[i]
                drawY[i] = segY[i]
            }
        }
    }

    /** Lays a straight body behind the head, pointing opposite to [heading]. */
    fun layStraightBody() {
        trail.clear()
        val bx = -cos(heading)
        val by = -sin(heading)
        var d = 0f
        while (d <= length + GameConfig.TRAIL_SPACING) {
            trail.pushBack(x + bx * d, y + by * d)
            d += GameConfig.TRAIL_SPACING
        }
        updateSegments()
        prevCount = 0
    }

    fun moveHeadTo(
        nx: Float,
        ny: Float,
    ) {
        x = nx
        y = ny
        if (trail.size == 0 || dist2(nx, ny, trail.x(0), trail.y(0)) >= GameConfig.TRAIL_SPACING * GameConfig.TRAIL_SPACING) {
            trail.pushFront(nx, ny)
        }
    }

    /**
     * Places the snake along given points (head first) — used for menu
     * previews and pictures, not during play.
     */
    fun setPose(
        xs: FloatArray,
        ys: FloatArray,
        count: Int,
        heading: Float,
    ) {
        x = xs[0]
        y = ys[0]
        this.heading = heading
        trail.clear()
        for (i in 0 until count) trail.pushBack(xs[i], ys[i])
        updateSegments()
        prevCount = 0
    }

    fun grow(amount: Float) {
        length += amount
        bestLength = max(bestLength, length)
    }

    /** Shortens the snake to [newLength] (never below the minimum). */
    fun shrinkTo(newLength: Float) {
        length = max(GameConfig.MIN_LENGTH, min(length, newLength))
        trimTrail()
    }

    /** Drops trail points that lie beyond the body length (keeps one extra for interpolation). */
    private fun trimTrail() {
        var acc = dist(x, y, trail.x(0), trail.y(0))
        for (i in 1 until trail.size) {
            acc += dist(trail.x(i - 1), trail.y(i - 1), trail.x(i), trail.y(i))
            if (acc >= length) {
                trail.truncate(i + 1)
                return
            }
        }
    }

    /** Length of the body that is actually laid out on the ground right now. */
    fun visibleLength(): Float {
        var acc = dist(x, y, trail.x(0), trail.y(0))
        for (i in 1 until trail.size) acc += dist(trail.x(i - 1), trail.y(i - 1), trail.x(i), trail.y(i))
        return min(acc, length)
    }

    /** Tapered radius at distance [d] along the body. */
    fun radiusAt(d: Float): Float {
        val r = radius
        val t = d / length
        if (t <= GameConfig.TAIL_TAPER_START) return r
        val k = ((t - GameConfig.TAIL_TAPER_START) / (1f - GameConfig.TAIL_TAPER_START)).coerceIn(0f, 1f)
        return r * (1f - k * (1f - GameConfig.TAIL_TIP_SCALE))
    }

    /**
     * Re-samples the body circles (head first) every radius * spacing factor
     * along the polyline head -> trail[0] -> trail[1] -> ... up to [length].
     */
    fun updateSegments() {
        resample(trim = false)
    }

    /**
     * After the head has moved: [trimTrail] and [updateSegments] in a single
     * walk along the trail. The walk adds up exactly the same distances in the
     * same order as [trimTrail], so it drops exactly the same points.
     */
    fun trimTrailAndUpdateSegments() {
        resample(trim = true)
    }

    private fun resample(trim: Boolean) {
        val spacing = max(2f, radius * GameConfig.SEGMENT_SPACING_FACTOR)
        var count = 0
        addSegment(count++, x, y, 0f)
        var nextD = spacing
        var walked = 0f
        var px = x
        var py = y
        for (i in 0 until trail.size) {
            val qx = trail.x(i)
            val qy = trail.y(i)
            val segLen = dist(px, py, qx, qy)
            if (segLen > 0f) {
                while (nextD <= walked + segLen && nextD <= length) {
                    val t = (nextD - walked) / segLen
                    addSegment(count++, px + (qx - px) * t, py + (qy - py) * t, nextD)
                    nextD += spacing
                }
            }
            walked += segLen
            if (walked >= length) {
                // trimTrail() only looks from point 1 on, so it always keeps at least two.
                if (trim) trail.truncate(max(i, 1) + 1)
                break
            }
            px = qx
            py = qy
        }
        segCount = count
    }

    private fun addSegment(
        i: Int,
        sx: Float,
        sy: Float,
        d: Float,
    ) {
        if (i >= segX.size) {
            val n = segX.size * 2
            segX = segX.copyOf(n)
            segY = segY.copyOf(n)
            segR = segR.copyOf(n)
            segD = segD.copyOf(n)
        }
        segX[i] = sx
        segY[i] = sy
        segD[i] = d
        segR[i] = radiusAt(d)
    }

    override fun toString(): String = "Snake#$id($name, len=${length.toInt()})"
}
