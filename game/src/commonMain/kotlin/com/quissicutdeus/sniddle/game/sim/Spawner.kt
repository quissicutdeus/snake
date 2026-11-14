package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.Holiday
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.model.dist
import com.quissicutdeus.sniddle.game.model.dist2
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Keeps a mix of AI snails around the player's size (~60% smaller, ~40%
 * bigger; little kid mode keeps more smaller, see [com.quissicutdeus.sniddle.game.Difficulty]),
 * always spawning them coiled up off-screen, and quietly removes ones that
 * no longer fit the mix once they're out of sight.
 *
 * A new snail arrives coiled, not laid out straight: a straight body as long
 * as a big snail's would reach right across a kid's screen, or out through the
 * fence, from a head that is itself off-screen.
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
        val radius = GameConfig.radiusFor(length)
        // Coiled around (0, 0), then moved to the spot found for it. The head is the coil's outer end, so it is the furthest point.
        val coil = Coil.around(0f, 0f, radius, length + GameConfig.TRAIL_SPACING * 2)
        val reach = sqrt(coil.xs[0] * coil.xs[0] + coil.ys[0] * coil.ys[0]) + radius
        val (x, y) = findSpot(sim, reach) ?: return null
        val s =
            sim.addSnail(
                name = pickAiName(sim),
                style = Holiday.dressUp(SnailStyle.random(rng), rng, sim.options.season),
                isPlayer = false,
                x = x + coil.xs[0],
                y = y + coil.ys[0],
                heading = coil.heading,
                length = length,
            )
        val xs = FloatArray(coil.count) { x + coil.xs[it] }
        val ys = FloatArray(coil.count) { y + coil.ys[it] }
        s.setPose(xs, ys, coil.count, coil.heading)
        return s
    }

    /** A name for a new computer snail: one no live snail has, if any is left. */
    private fun pickAiName(sim: GameSim): String {
        val used =
            sim.world.snails
                .filter { it.alive }
                .map { it.name }
                .toSet()
        val free = AI_NAMES.filter { it !in used }
        return if (free.isNotEmpty()) free[sim.rng.nextInt(free.size)] else AI_NAMES[sim.rng.nextInt(AI_NAMES.size)]
    }

    /**
     * A random centre for a coil reaching [reach] from it: the whole coil off
     * every kid's screen and inside the fence, and not on top of another
     * snail's head. Null if there is no such spot right now.
     */
    private fun findSpot(
        sim: GameSim,
        reach: Float,
    ): Pair<Float, Float>? {
        val w = sim.world
        val margin = max(GameConfig.SPAWN_WALL_MARGIN, reach)
        if (2 * margin >= w.width || 2 * margin >= w.height) return null
        val watchers = watchers(sim)

        fun unseen(
            x: Float,
            y: Float,
        ) = watchers.all { dist(x, y, it.x, it.y) >= offscreen(it) + reach }
        var bestX = 0f
        var bestY = 0f
        var bestScore = -1f
        repeat(SPAWN_TRIES) {
            val x = margin + sim.rng.nextFloat() * (w.width - 2 * margin)
            val y = margin + sim.rng.nextFloat() * (w.height - 2 * margin)
            val crowd = w.snails.filter { it.alive }.minOfOrNull { dist(x, y, it.x, it.y) } ?: Float.MAX_VALUE
            val clearance = GameConfig.SPAWN_CLEARANCE + reach
            if (unseen(x, y) && crowd > clearance) return x to y
            // How far out of sight (1 = the coil just off the nearest kid's screen).
            val hidden = watchers.minOfOrNull { dist(x, y, it.x, it.y) / max(offscreen(it) + reach, 1f) } ?: Float.MAX_VALUE
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
        val cap = sim.aiMaxLength()

        // Far: the head a good way off every kid's screen, and none of the body on it, so nobody sees a snail vanish.
        fun isFar(s: Snail) =
            watchers.all { w ->
                val d = offscreen(w)
                dist(s.x, s.y, w.x, w.y) > d + GameConfig.DESPAWN_EXTRA_DISTANCE &&
                    (0 until s.segCount).all { i -> dist2(s.segX[i], s.segY[i], w.x, w.y) >= d * d }
            }
        val ais = sim.world.snails.filter { it.alive && !it.isPlayer }
        for (s in ais) {
            val ratio = s.length / ref
            // Over the size cap happens when the kid shrinks (their tail bitten off): the giants it left go once out of sight.
            val misfit = ratio > GameConfig.DESPAWN_MAX_RATIO || ratio < GameConfig.DESPAWN_MIN_RATIO || s.length > cap
            if (misfit && isFar(s)) s.alive = false
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

        private val AI_NAMES =
            listOf(
                "Noodle",
                "Sir Slime",
                "Wiggles",
                "Shelly",
                "Zippy",
                "Captain Shell",
                "Sheldon",
                "Slowpoke",
                "Lord Wiggleton",
                "Swirly",
                "Pickle",
                "Sprinkles",
                "Doodle",
                "Mr. Snoot",
                "Lady Glide",
                "Jellybean",
                "Pretzel",
                "Waffles",
                "Bean Sprout",
                "Ziggy",
                "Professor Scales",
                "Bubbles",
                "Sir Noodles",
                "Taco",
                "Marshmallow",
                "Escargot",
                "Gummy",
                "Princess Goo",
                "Wobble",
                "Scooter",
                "Pudding",
                "Captain Zig",
                "Snickerdoodle",
                "Lil' Turbo",
                "Mustard",
                "Tater Tot",
                "Rascal",
                "Biscuit",
            )
    }
}
