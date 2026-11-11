package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.Holiday
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.model.dist
import kotlin.math.atan2
import kotlin.math.max

/**
 * Keeps a mix of AI snails around the player's size (~60% smaller, ~40%
 * bigger; little kid mode keeps more smaller, see [com.quissicutdeus.sniddle.game.Difficulty]),
 * always spawning them off-screen, and quietly removes far-away
 * ones that no longer fit the mix.
 *
 * With two kids, "off-screen" and "far away" go for both halves of the
 * screen, and the mix is made around the smaller kid, so the arena stays
 * fair for whoever is behind.
 */
class Spawner(
    private val targetCount: Int = GameConfig.AI_TARGET_COUNT,
) {
    private var timer = 0f

    fun update(
        sim: GameSim,
        dt: Float,
    ) {
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

    private fun aiCount(sim: GameSim) = sim.world.snails.count { it.alive && !it.isPlayer }

    private fun referenceLength(sim: GameSim): Float {
        val ref = if (sim.together != null) sim.players.filter { it.alive }.minOfOrNull { it.length } else sim.player?.length
        return ref ?: GameConfig.PLAYER_START_LENGTH
    }

    /** The players whose screens new snails must stay off (everyone alive). */
    private fun watchers(sim: GameSim): List<Snail> = sim.players.filter { it.alive }

    private fun offscreen(s: Snail) = GameConfig.offscreenDistance(s.radius)

    fun spawnOne(sim: GameSim): Snail? {
        val rng = sim.rng
        val ref = referenceLength(sim)
        val ais = sim.world.snails.filter { it.alive && !it.isPlayer }
        val smaller = ais.count { compareSize(it.length, ref) == SizeRelation.SMALLER }
        val wantSmaller = ais.isEmpty() || smaller.toFloat() / ais.size < sim.difficulty.smallerFraction
        val ratio =
            if (wantSmaller) {
                GameConfig.SMALLER_MIN_RATIO + rng.nextFloat() * (GameConfig.SMALLER_MAX_RATIO - GameConfig.SMALLER_MIN_RATIO)
            } else {
                GameConfig.BIGGER_MIN_RATIO + rng.nextFloat() * (GameConfig.BIGGER_MAX_RATIO - GameConfig.BIGGER_MIN_RATIO)
            }
        val length = max(GameConfig.MIN_LENGTH, ref * ratio)
        val (x, y) = findSpot(sim) ?: return null
        // Point roughly away from the middle so the straight starting body lies inside the arena.
        val outward = atan2(y - sim.world.height / 2f, x - sim.world.width / 2f)
        val heading = outward + (rng.nextFloat() - 0.5f) * 1.2f
        return sim.addSnail(
            name = sim.pickAiName(),
            style = Holiday.dressUp(SnailStyle.random(rng), rng, sim.options.season),
            isPlayer = false,
            x = x,
            y = y,
            heading = heading,
            length = length,
        )
    }

    /** A random spot well off-screen and not on top of another snail's head. */
    private fun findSpot(sim: GameSim): Pair<Float, Float>? {
        val w = sim.world
        val margin = GameConfig.SPAWN_WALL_MARGIN
        val watchers = watchers(sim)

        fun unseen(
            x: Float,
            y: Float,
        ) = watchers.all { dist(x, y, it.x, it.y) >= offscreen(it) }
        var bestX = 0f
        var bestY = 0f
        var bestScore = -1f
        repeat(SPAWN_TRIES) {
            val x = margin + sim.rng.nextFloat() * (w.width - 2 * margin)
            val y = margin + sim.rng.nextFloat() * (w.height - 2 * margin)
            val crowd = w.snails.filter { it.alive }.minOfOrNull { dist(x, y, it.x, it.y) } ?: Float.MAX_VALUE
            val clearance = GameConfig.SPAWN_CLEARANCE
            if (unseen(x, y) && crowd > clearance) return x to y
            // How far out of sight (1 = just off the nearest kid's screen).
            val hidden = watchers.minOfOrNull { dist(x, y, it.x, it.y) / max(offscreen(it), 1f) } ?: Float.MAX_VALUE
            val score = minOf(hidden, crowd / clearance)
            if (score > bestScore) {
                bestScore = score
                bestX = x
                bestY = y
            }
        }
        // Arena too crowded or the players see almost all of it: only accept off-screen spots.
        return if (unseen(bestX, bestY)) bestX to bestY else null
    }

    private fun despawnMisfits(sim: GameSim) {
        val watchers = watchers(sim)
        if (watchers.isEmpty()) return
        val ref = referenceLength(sim)

        fun isFar(s: Snail) = watchers.all { dist(s.x, s.y, it.x, it.y) > offscreen(it) + GameConfig.DESPAWN_EXTRA_DISTANCE }
        val ais = sim.world.snails.filter { it.alive && !it.isPlayer }
        for (s in ais) {
            val ratio = s.length / ref
            if ((ratio > GameConfig.DESPAWN_MAX_RATIO || ratio < GameConfig.DESPAWN_MIN_RATIO) && isFar(s)) s.alive = false
        }
        // Keep the mix around the player: if the player shrank (or AIs grew) and
        // too many are now bigger, quietly retire the biggest far-away one.
        // The next spawn will then be a smaller snail.
        val alive = ais.filter { it.alive }
        if (alive.isEmpty()) return
        val smaller = alive.count { compareSize(it.length, ref) == SizeRelation.SMALLER }
        if (smaller.toFloat() / alive.size < sim.difficulty.smallerFraction - GameConfig.MIX_TOLERANCE) {
            alive
                .filter { compareSize(it.length, ref) == SizeRelation.BIGGER && isFar(it) }
                .maxByOrNull { it.length }
                ?.alive = false
        }
    }

    private companion object {
        /** Random spots tried when looking for a free off-screen place to spawn. */
        const val SPAWN_TRIES = 30
    }
}
