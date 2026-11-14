package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.Food
import com.quissicutdeus.sniddle.game.model.Pellet
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.TWO_PI
import com.quissicutdeus.sniddle.game.model.TailPiece
import com.quissicutdeus.sniddle.game.model.dist2
import kotlin.math.min
import kotlin.math.sqrt

internal fun GameSim.randomPellet(): Pellet {
    val big = rng.nextFloat() < GameConfig.BIG_PELLET_CHANCE
    val margin = 40f
    return Pellet(
        x = margin + rng.nextFloat() * (world.width - 2 * margin),
        y = margin + rng.nextFloat() * (world.height - 2 * margin),
        value = if (big) GameConfig.BIG_PELLET_VALUE else GameConfig.PELLET_VALUE,
        radius = if (big) GameConfig.BIG_PELLET_RADIUS else GameConfig.PELLET_RADIUS,
        color = PELLET_COLORS[rng.nextInt(PELLET_COLORS.size)],
        phase = rng.nextFloat() * TWO_PI,
    )
}

internal fun GameSim.eatFood(dt: Float) {
    heads.build(world.snails, foodReach(dt))
    val pellets = world.pellets
    for (i in pellets.indices) {
        val p = pellets[i]
        p.age += dt
        if (!eatenNow(p, dt)) continue
        p.gone = true
        emit(GameEvent.AteFood(p.x, p.y, p.color))
        pellets[i] = randomPellet()
    }
    world.tailPieces.removeAll { updateTailPiece(it, dt) }
}

/** Ages a dropped tail piece and lets snails eat it. Returns true when it should go away. */
private fun GameSim.updateTailPiece(
    t: TailPiece,
    dt: Float,
): Boolean {
    t.age += dt
    val expired = t.age > GameConfig.TAIL_PIECE_LIFETIME
    val eaten = !expired && eatenNow(t, dt)
    if (eaten) emit(GameEvent.AteFood(t.x, t.y, t.style.bodyColor))
    t.gone = expired || eaten
    return t.gone
}

/**
 * True if a snail's head reaches [food] and eats it; snails close by pull it
 * nearer. Snails are tried in list order (the first one to reach it eats
 * it), skipping only those too far away for [tryEat] to do anything.
 */
private fun GameSim.eatenNow(
    food: Food,
    dt: Float,
): Boolean {
    val cell = heads.cellAt(food.x, food.y)
    for (e in heads.startOf(cell) until heads.startOf(cell + 1)) {
        val s = world.snails[heads.snailIndexAt(e)]
        if (s.alive && tryEat(s, food, dt)) return true
    }
    return false
}

/**
 * Beyond this distance from a head, [tryEat] can neither eat nor pull any
 * food this step: the furthest any snail reaches for the biggest food,
 * plus how far food may already have been pulled by the snails tried
 * before it (each pull moves it at most SUCTION_SPEED * dt).
 */
private fun GameSim.foodReach(dt: Float): Float {
    var foodR = 0f
    for (p in world.pellets) foodR = maxOf(foodR, p.radius)
    for (t in world.tailPieces) foodR = maxOf(foodR, t.radius)
    val grab = maxOf(GameConfig.MAX_RADIUS + GameConfig.EAT_MARGIN, 2f * GameConfig.MAX_RADIUS + GameConfig.SUCTION_RANGE)
    val drift = world.snails.size * GameConfig.SUCTION_SPEED * dt
    return grab + foodR + drift + 1f // + 1: slack for float rounding
}

/** Eats the food if the head touches it; otherwise pulls nearby food a little closer. */
private fun GameSim.tryEat(
    s: Snail,
    food: Food,
    dt: Float,
): Boolean {
    val r = s.radius
    val d2 = dist2(s.x, s.y, food.x, food.y)
    val eatR = r + food.radius + GameConfig.EAT_MARGIN
    if (d2 < eatR * eatR) {
        feed(s, food.value)
        s.sinceAte = 0f
        return true
    }
    val suck = r * 2f + GameConfig.SUCTION_RANGE + food.radius
    if (d2 < suck * suck) {
        val d = sqrt(d2)
        val step = min(d, GameConfig.SUCTION_SPEED * dt)
        food.x += (s.x - food.x) / d * step
        food.y += (s.y - food.y) / d * step
    }
    return false
}

/** Bright candy colors for food dots (ARGB). */
private val PELLET_COLORS =
    intArrayOf(
        0xFFFF5252.toInt(),
        0xFFFFAB40.toInt(),
        0xFFFFEB3B.toInt(),
        0xFF69F0AE.toInt(),
        0xFF40C4FF.toInt(),
        0xFF7C4DFF.toInt(),
        0xFFFF4081.toInt(),
        0xFFFFFFFF.toInt(),
    )
