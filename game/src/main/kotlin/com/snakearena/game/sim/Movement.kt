package com.snakearena.game.sim

import com.snakearena.game.GameConfig
import com.snakearena.game.model.PI_F
import com.snakearena.game.model.Snake
import com.snakearena.game.model.World
import com.snakearena.game.model.angleDiff
import com.snakearena.game.model.wrapAngle
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Turn-rate-limited steering, speed curve, walls, and the body following the head. */
object Movement {

    /** Turns at most TURN_RATE * dt towards [desired] (NaN = keep going straight). */
    fun steer(s: Snake, desired: Float, dt: Float) {
        if (s.steerLock > 0f || desired.isNaN()) return
        val diff = angleDiff(s.heading, desired)
        val maxTurn = GameConfig.TURN_RATE * dt
        s.heading = wrapAngle(s.heading + diff.coerceIn(-maxTurn, maxTurn))
    }

    /** Moves the head forward, bounces off walls, and updates the body. Returns true if a wall was hit. */
    fun advance(s: Snake, world: World, dt: Float): Boolean {
        val boost = if (s.knockback > 0f) GameConfig.BOUNCE_SPEED_FACTOR else 1f
        val v = s.speed * boost
        var nx = s.x + cos(s.heading) * v * dt
        var ny = s.y + sin(s.heading) * v * dt

        val r = s.radius
        var hitWall = false
        if (nx < r) {
            nx = r; s.heading = wrapAngle(PI_F - s.heading); hitWall = true
        } else if (nx > world.width - r) {
            nx = world.width - r; s.heading = wrapAngle(PI_F - s.heading); hitWall = true
        }
        if (ny < r) {
            ny = r; s.heading = wrapAngle(-s.heading); hitWall = true
        } else if (ny > world.height - r) {
            ny = world.height - r; s.heading = wrapAngle(-s.heading); hitWall = true
        }
        if (hitWall) s.steerLock = max(s.steerLock, GameConfig.WALL_BOUNCE_SECONDS)

        s.moveHeadTo(nx, ny)
        s.trimTrail()
        s.updateSegments()
        return hitWall
    }

    fun tickTimers(s: Snake, dt: Float) {
        s.steerLock = max(0f, s.steerLock - dt)
        s.knockback = max(0f, s.knockback - dt)
        s.invulnerable = max(0f, s.invulnerable - dt)
        s.sinceAte += dt
        s.age += dt
    }
}
