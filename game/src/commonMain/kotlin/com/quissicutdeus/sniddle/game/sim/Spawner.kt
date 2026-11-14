package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.Holiday
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.SnailStyle
import com.quissicutdeus.sniddle.game.model.dist
import kotlin.math.abs
import kotlin.math.max

/**
 * Keeps a mix of AI snails around the player's size (~60% smaller, ~40%
 * bigger; little kid mode keeps more smaller, see [com.quissicutdeus.sniddle.game.Difficulty]),
 * always spawning them coiled up where no kid can see.
 *
 * A snail any kid has had on screen only ever leaves by being eaten, so one
 * a kid has been chasing is still out there when they come back for it. To
 * keep the mix right, the spawner may quietly retire snails no kid has ever
 * seen (the wrong size for the kid now, or one bigger too many), and adds
 * snails: only ones that still matter to the kid count toward [targetCount]
 * (not ones so small they are hardly more than a snack, see
 * [GameConfig.RELEVANT_MIN_RATIO]), and when the share of smaller snails runs
 * low, smaller ones are added on top, up to [GameConfig.AI_MAX_COUNT] in all.
 *
 * A new snail arrives coiled, not laid out straight: a straight body as long
 * as a big snail's would reach right across a kid's screen, or out through the
 * fence, from a head that is itself off-screen. Its head is in the middle of
 * the coil (see [Coil]).
 *
 * "Where no kid can see" is each kid's actual screen as the game reports it
 * ([GameSim.setView]), plus a margin. Before that is known it is a circle as
 * wide as the widest phone's screen, which for a big kid in the middle of the
 * arena covers nearly all of it.
 *
 * With two kids, both screens count, and the mix is made around the smaller
 * kid, so the arena stays fair for whoever is behind.
 */
class Spawner(
    private val targetCount: Int = GameConfig.AI_TARGET_COUNT,
) {
    private var timer = 0f

    fun update(
        sim: GameSim,
        dt: Float,
    ) {
        markSeen(sim)
        timer -= dt
        if (timer > 0f) return
        timer = GameConfig.SPAWN_INTERVAL
        retireUnseenMisfits(sim)
        if (wantsAnother(sim)) spawnOne(sim)
    }

    /** Fills the arena up to the target count at once (game start). */
    fun fill(sim: GameSim) {
        repeat(targetCount) { if (aiCount(sim) < targetCount) spawnOne(sim) }
    }

    private fun aiCount(sim: GameSim) = sim.world.snails.count { it.alive && !it.isPlayer }

    /** Room for another snail, and either too few that matter or too few smaller ones. */
    private fun wantsAnother(sim: GameSim): Boolean {
        val ais = sim.world.snails.filter { it.alive && !it.isPlayer }
        if (ais.size >= GameConfig.AI_MAX_COUNT) return false
        val ref = referenceLength(sim)
        val relevant = ais.filter { it.length >= ref * GameConfig.RELEVANT_MIN_RATIO }
        if (relevant.size < targetCount) return true
        val smaller = relevant.count { compareSize(it.length, ref) == SizeRelation.SMALLER }
        return smaller.toFloat() / relevant.size < sim.difficulty.smallerFraction - GameConfig.MIX_TOLERANCE
    }

    private fun referenceLength(sim: GameSim): Float {
        val ref = if (sim.together != null) sim.players.filter { it.alive }.minOfOrNull { it.length } else sim.player?.length
        return ref ?: GameConfig.PLAYER_START_LENGTH
    }

    /** The players whose screens new snails must stay off (everyone alive). */
    private fun watchers(sim: GameSim): List<Snail> = sim.players.filter { it.alive }

    private fun offscreen(s: Snail) = GameConfig.offscreenDistance(s.radius)

    /** Marks the snails some part of which is on a kid's screen right now (every few body circles are checked). */
    private fun markSeen(sim: GameSim) {
        val watchers = watchers(sim)
        for (s in sim.world.snails) {
            if (s.seenByKid || !s.alive || s.isPlayer) continue
            var i = 0
            while (i < s.segCount && !s.seenByKid) {
                val x = s.segX[i]
                val y = s.segY[i]
                if (watchers.any { !hiddenFrom(sim, it, x, y, s.segR[i]) }) s.seenByKid = true
                i += SEEN_CHECK_EVERY
            }
        }
    }

    /**
     * Retires snails no kid has ever seen that no longer fit: far the wrong
     * size for the kid now (see [GameConfig.DESPAWN_MIN_RATIO]), over the size
     * cap (left when the kid shrinks), or, while too few are smaller than the
     * kid, the biggest bigger one. The next spawn then fits better.
     */
    private fun retireUnseenMisfits(sim: GameSim) {
        // No kid (the rule tests' empty arenas): nobody to fit the mix around.
        if (watchers(sim).isEmpty()) return
        val ref = referenceLength(sim)
        val cap = sim.aiMaxLength()
        val unseen = sim.world.snails.filter { it.alive && !it.isPlayer && !it.seenByKid }
        for (s in unseen) {
            val ratio = s.length / ref
            if (ratio > GameConfig.DESPAWN_MAX_RATIO || ratio < GameConfig.DESPAWN_MIN_RATIO || s.length > cap) s.alive = false
        }
        val ais = sim.world.snails.filter { it.alive && !it.isPlayer }
        if (ais.isEmpty()) return
        val smaller = ais.count { compareSize(it.length, ref) == SizeRelation.SMALLER }
        if (smaller.toFloat() / ais.size < sim.difficulty.smallerFraction - GameConfig.MIX_TOLERANCE) {
            ais
                .filter { !it.seenByKid && compareSize(it.length, ref) == SizeRelation.BIGGER }
                .maxByOrNull { it.length }
                ?.alive = false
        }
    }

    /** Whether a coil reaching [reach] around ([x], [y]) is off [kid]'s screen, with a margin. */
    private fun hiddenFrom(
        sim: GameSim,
        kid: Snail,
        x: Float,
        y: Float,
        reach: Float,
    ): Boolean {
        val v = sim.viewOf(kid) ?: return dist(x, y, kid.x, kid.y) >= offscreen(kid) + reach
        val m = reach + GameConfig.SPAWN_VIEW_MARGIN
        return abs(x - v.x) >= v.halfWidth + m || abs(y - v.y) >= v.halfHeight + m
    }

    fun spawnOne(sim: GameSim): Snail? {
        val rng = sim.rng
        val ref = referenceLength(sim)
        val ais = sim.world.snails.filter { it.alive && !it.isPlayer && it.length >= ref * GameConfig.RELEVANT_MIN_RATIO }
        val smaller = ais.count { compareSize(it.length, ref) == SizeRelation.SMALLER }
        val wantSmaller = ais.isEmpty() || smaller.toFloat() / ais.size < sim.difficulty.smallerFraction
        val smallerRatio = GameConfig.SMALLER_MIN_RATIO + rng.nextFloat() * (GameConfig.SMALLER_MAX_RATIO - GameConfig.SMALLER_MIN_RATIO)
        val biggerRatio = GameConfig.BIGGER_MIN_RATIO + rng.nextFloat() * (GameConfig.BIGGER_MAX_RATIO - GameConfig.BIGGER_MIN_RATIO)
        // A bigger snail for a very big kid can be a coil too wide to fit anywhere out of
        // sight: then a smaller one comes instead, so the arena never just runs down.
        val ratios = if (wantSmaller) listOf(smallerRatio) else listOf(biggerRatio, smallerRatio)
        for (ratio in ratios) {
            val length = max(GameConfig.MIN_LENGTH, ref * ratio)
            val radius = GameConfig.radiusFor(length)
            // Coiled around (0, 0), head in the middle, then moved to the spot found for it.
            val coil = Coil.around(0f, 0f, radius, length + GameConfig.TRAIL_SPACING * 2, headInside = true)
            val spot = findSpot(sim, coil.reach + radius) ?: continue
            return place(sim, coil, spot.first, spot.second, length)
        }
        return null
    }

    /** A new computer snail of [length], posed as [coil] around ([x], [y]). */
    private fun place(
        sim: GameSim,
        coil: Coil,
        x: Float,
        y: Float,
        length: Float,
    ): Snail {
        val rng = sim.rng
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
        ) = watchers.all { hiddenFrom(sim, it, x, y, reach) }
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

    private companion object {
        /** Random spots tried when looking for a free off-screen place to spawn. */
        const val SPAWN_TRIES = 30

        /** Body circles checked when looking whether a snail is on a kid's screen (every 8th: they overlap a lot). */
        const val SEEN_CHECK_EVERY = 8

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
