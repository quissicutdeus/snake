package com.snakearena.game.render

import com.snakearena.game.model.Snake
import com.snakearena.game.model.SnakeStyle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A snake that isn't in a game: laid along a wave that travels with time, so
 * it slithers in place. Used by the My Snake preview, the title screen and
 * the little pictures on the high-score list.
 */
class PosedSnake(style: SnakeStyle, val length: Float = 260f) {
    val snake = Snake(0, "", style, isPlayer = true, x = 0f, y = 0f, heading = 0f, length = length)
    private val count = (length / 2.5f).toInt() + 8
    private val xs = FloatArray(count)
    private val ys = FloatArray(count)

    var style: SnakeStyle
        get() = snake.style
        set(value) { snake.style = value }

    /**
     * Lays the snake with its head at (headX, headY) and the body trailing to
     * the left in a wave of [amplitude] and [wavelength] world units.
     */
    fun wave(headX: Float, headY: Float, time: Float, amplitude: Float = 26f, wavelength: Float = 150f, speed: Float = 2.2f) {
        val k = 2f * Math.PI.toFloat() / wavelength
        // Walk along the curve in small steps so the body length is measured along it.
        var x = headX
        var y = headY + amplitude * 0.35f * sin(-time * speed)
        xs[0] = x
        ys[0] = y
        var u = 0f
        for (i in 1 until count) {
            // Step in x such that the arc step is ~2.5 units.
            val slope = amplitude * k * cos(u * k - time * speed)
            val dx = 2.5f / kotlin.math.sqrt(1f + slope * slope)
            u += dx
            x -= dx
            val env = min(1f, 0.35f + u / (wavelength * 0.8f))
            y = headY + amplitude * env * sin(u * k - time * speed)
            xs[i] = x
            ys[i] = y
        }
        val heading = atan2(ys[0] - ys[3], xs[0] - xs[3])
        snake.setPose(xs, ys, count, heading)
    }

    /**
     * Lays the snake in a coil around (cx, cy): the tail tucked in the middle,
     * the head at the outer end. Returns the coil's diameter in world units
     * (scale it to fit your picture).
     */
    fun curl(cx: Float, cy: Float): Float {
        val r = snake.radius
        val outer = r * 4.6f
        val gap = r * 2.5f / (2f * Math.PI.toFloat())
        var phi = 0f
        for (i in 0 until count) {
            val rho = kotlin.math.max(r * 1.3f, outer - gap * phi)
            // Head at the bottom-right; the body winds inwards.
            val a = 0.5f - phi
            xs[i] = cx + cos(a) * rho
            ys[i] = cy + sin(a) * rho
            phi += 2.5f / rho
        }
        snake.setPose(xs, ys, count, atan2(ys[0] - ys[2], xs[0] - xs[2]))
        return 2f * (outer + r * SnakeArt.HEAD_SCALE * 1.4f)
    }

    fun draw(c: Canvas2D, time: Float, mood: Mood = Mood.HAPPY) {
        SnakeArt.drawSnake(c, snake, mood, time)
    }
}
