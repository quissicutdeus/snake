package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.GameConfig

/**
 * Which computer snails a kid has seen ([com.quissicutdeus.sniddle.game.model.Snail.seenByKid]),
 * and quietly retiring those no kid ever has. A snail that has been on a
 * kid's screen only ever leaves by being eaten.
 */
internal object Sightings {
    /** Body circles checked when looking whether a snail is on a kid's screen (every 8th: they overlap a lot). */
    private const val CHECK_EVERY = 8

    /** Marks the snails some part of which is on a kid's screen right now (every few body circles are checked). */
    fun markSeen(sim: GameSim) {
        val watchers = sim.watchers()
        for (s in sim.world.snails) {
            if (s.seenByKid || !s.alive || s.isPlayer) continue
            var i = 0
            while (i < s.segCount && !s.seenByKid) {
                val x = s.segX[i]
                val y = s.segY[i]
                if (watchers.any { !sim.hiddenFrom(it, x, y, s.segR[i]) }) s.seenByKid = true
                i += CHECK_EVERY
            }
        }
    }

    /**
     * Retires snails no kid has ever seen that no longer fit the mix around
     * [ref]: far the wrong size (see [GameConfig.DESPAWN_MIN_RATIO]), over the
     * size cap (left when the kid shrinks), or, while too few are smaller
     * than [ref], the biggest bigger one. The next spawn then fits better.
     */
    fun retireUnseenMisfits(
        sim: GameSim,
        ref: Float,
    ) {
        // No kid (the rule tests' empty arenas): nobody to fit the mix around.
        if (sim.watchers().isEmpty()) return
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
}
