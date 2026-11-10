package com.snakearena.game.sim

import com.snakearena.game.GameConfig
import com.snakearena.game.model.Snake
import com.snakearena.game.model.SnakeStyle
import com.snakearena.game.model.dist
import kotlin.math.atan2
import kotlin.math.max

/**
 * Keeps a mix of AI snakes around the player's size (~60% smaller, ~40%
 * bigger), always spawning them off-screen, and quietly removes far-away
 * ones that no longer fit the mix.
 */
class Spawner(private val targetCount: Int = GameConfig.AI_TARGET_COUNT) {
    private var timer = 0f

    fun update(sim: GameSim, dt: Float) {
        timer -= dt
        if (timer > 0f) return
        timer = GameConfig.SPAWN_INTERVAL
        despawnMisfits(sim)
        if (aiCount(sim) < targetCount) spawnOne(sim)
    }

    /** Fills the arena up to the target count at once (game start). */
    fun fill(sim: GameSim) {
        repeat(targetCount) { if (aiCount(sim) < targetCount) spawnOne(sim) }
    }

    private fun aiCount(sim: GameSim) = sim.world.snakes.count { it.alive && !it.isPlayer }

    private fun referenceLength(sim: GameSim): Float = sim.player?.length ?: GameConfig.PLAYER_START_LENGTH

    fun spawnOne(sim: GameSim): Snake? {
        val rng = sim.rng
        val ref = referenceLength(sim)
        val ais = sim.world.snakes.filter { it.alive && !it.isPlayer }
        val smaller = ais.count { compareSize(it.length, ref) == SizeRelation.SMALLER }
        val wantSmaller = ais.isEmpty() || smaller.toFloat() / ais.size < GameConfig.SMALLER_FRACTION
        val ratio = if (wantSmaller) {
            GameConfig.SMALLER_MIN_RATIO + rng.nextFloat() * (GameConfig.SMALLER_MAX_RATIO - GameConfig.SMALLER_MIN_RATIO)
        } else {
            GameConfig.BIGGER_MIN_RATIO + rng.nextFloat() * (GameConfig.BIGGER_MAX_RATIO - GameConfig.BIGGER_MIN_RATIO)
        }
        val length = max(GameConfig.MIN_LENGTH, ref * ratio)
        val (x, y) = findSpot(sim) ?: return null
        // Point roughly away from the middle so the straight starting body lies inside the arena.
        val outward = atan2(y - sim.world.height / 2f, x - sim.world.width / 2f)
        val heading = outward + (rng.nextFloat() - 0.5f) * 1.2f
        return sim.addSnake(
            name = sim.pickAiName(),
            style = SnakeStyle.random(rng),
            isPlayer = false,
            x = x,
            y = y,
            heading = heading,
            length = length,
        )
    }

    /** A random spot well off-screen and not on top of another snake's head. */
    private fun findSpot(sim: GameSim): Pair<Float, Float>? {
        val w = sim.world
        val margin = 160f
        val player = sim.player
        val minDist = if (player != null && player.alive) GameConfig.offscreenDistance(player.radius) else 0f
        var bestX = 0f
        var bestY = 0f
        var bestScore = -1f
        repeat(30) {
            val x = margin + sim.rng.nextFloat() * (w.width - 2 * margin)
            val y = margin + sim.rng.nextFloat() * (w.height - 2 * margin)
            val toPlayer = if (player != null && player.alive) dist(x, y, player.x, player.y) else Float.MAX_VALUE
            val crowd = w.snakes.filter { it.alive }.minOfOrNull { dist(x, y, it.x, it.y) } ?: Float.MAX_VALUE
            if (toPlayer >= minDist && crowd > 200f) return x to y
            val score = minOf(toPlayer / max(minDist, 1f), crowd / 200f)
            if (score > bestScore) {
                bestScore = score
                bestX = x
                bestY = y
            }
        }
        // Arena too crowded or the player sees almost all of it: only accept off-screen spots.
        return if (player == null || !player.alive || dist(bestX, bestY, player.x, player.y) >= minDist) bestX to bestY else null
    }

    private fun despawnMisfits(sim: GameSim) {
        val player = sim.player ?: return
        if (!player.alive) return
        val far = GameConfig.offscreenDistance(player.radius) + 200f
        for (s in sim.world.snakes) {
            if (!s.alive || s.isPlayer) continue
            val ratio = s.length / player.length
            if ((ratio > GameConfig.DESPAWN_MAX_RATIO || ratio < GameConfig.DESPAWN_MIN_RATIO) &&
                dist(s.x, s.y, player.x, player.y) > far
            ) {
                s.alive = false
            }
        }
    }
}
