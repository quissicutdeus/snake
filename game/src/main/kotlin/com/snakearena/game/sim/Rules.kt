package com.snakearena.game.sim

import com.snakearena.game.GameConfig
import com.snakearena.game.model.Snake
import com.snakearena.game.model.TailPiece
import com.snakearena.game.model.dist
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class SizeRelation { SMALLER, EQUAL, BIGGER }

/** How a snake of length [a] compares to one of length [b]. Within ~10% counts as equal. */
fun compareSize(a: Float, b: Float): SizeRelation = when {
    a > b * GameConfig.EQUAL_SIZE_RATIO -> SizeRelation.BIGGER
    b > a * GameConfig.EQUAL_SIZE_RATIO -> SizeRelation.SMALLER
    else -> SizeRelation.EQUAL
}

fun Snake.relationTo(other: Snake): SizeRelation = compareSize(length, other.length)

/**
 * The snake-vs-snake rules. [Contact.a]'s head touched circle [Contact.seg] of
 * [Contact.b]:
 *  - roughly equal size: bounce, no damage (head-to-head: both bounce);
 *  - a is bigger: head-to-head hurts b (eaten at 0 hearts), body hit cuts b's tail off;
 *  - a is smaller: a bounces back and loses a heart (eaten at 0 hearts).
 * A protected (blinking) snake can't be hurt or cut; contacts that would hurt it are ignored.
 */
internal fun GameSim.resolveContact(c: Contact) {
    val a = c.a
    val b = c.b
    if (!a.alive || !b.alive || c.seg >= b.segCount) return
    val headToHead = c.seg == 0
    if (headToHead && !markHeadPair(a, b)) return

    val cx = b.segX[c.seg]
    val cy = b.segY[c.seg]
    when (a.relationTo(b)) {
        SizeRelation.EQUAL -> {
            if (a.invulnerable > 0f || b.invulnerable > 0f) return
            if (a.knockback <= 0f) bounce(a, cx, cy, b)
            if (headToHead && b.knockback <= 0f) bounce(b, a.x, a.y, a)
        }
        SizeRelation.BIGGER -> {
            if (b.invulnerable > 0f) return
            if (headToHead) damage(victim = b, by = a) else sever(victim = b, seg = c.seg, by = a)
        }
        SizeRelation.SMALLER -> {
            if (a.invulnerable > 0f) return
            damage(victim = a, by = b, fromX = cx, fromY = cy)
        }
    }
}

/** Victim loses a heart and bounces away; at 0 hearts it is eaten by [by]. */
internal fun GameSim.damage(victim: Snake, by: Snake, fromX: Float = by.x, fromY: Float = by.y) {
    if (victim.invulnerable > 0f || !victim.alive) return
    victim.hearts--
    if (victim.hearts <= 0) {
        eatWhole(eater = by, victim = victim)
        return
    }
    bounce(victim, fromX, fromY, by)
    victim.invulnerable = GameConfig.HURT_INVULNERABLE_SECONDS
    emit(GameEvent.Hurt(victim.id, by.id, victim.x, victim.y, victim.hearts))
    victim.brain?.startFleeing(by)
}

/** [eater] swallows [victim] whole and grows. */
internal fun GameSim.eatWhole(eater: Snake, victim: Snake) {
    victim.alive = false
    victim.hearts = 0
    eater.grow(victim.length * GameConfig.EAT_WHOLE_GAIN)
    eater.snakesEaten++
    eater.sinceAte = 0f
    emit(GameEvent.AteSnake(eater.id, victim.id, victim.x, victim.y, victim.style.bodyColor))
    if (victim.isPlayer) {
        gameOver = true
        emit(GameEvent.PlayerEaten(eater.id, victim.x, victim.y))
    }
}

/**
 * Cuts [victim] at body circle [seg]: the front part survives (never below the
 * minimum length) and the rest falls off as [TailPiece]s anyone can eat.
 * Returns the length that fell off (0 if there was nothing to cut).
 */
internal fun GameSim.sever(victim: Snake, seg: Int, by: Snake): Float {
    val cutAt = victim.segD[seg]
    val keep = max(GameConfig.MIN_LENGTH, cutAt)
    val visible = victim.visibleLength()
    if (keep >= visible - 1f) return 0f
    val dropped = victim.length - keep

    // Lay pieces along the part of the body that falls off.
    val spacing = max(12f, victim.radius * GameConfig.TAIL_PIECE_SPACING_FACTOR)
    val count = max(1, ceil((visible - keep) / spacing).toInt())
    val value = dropped / count
    val pieceR = max(6f, victim.radius * GameConfig.TAIL_PIECE_RADIUS_FACTOR)
    var placed = 0
    var nextD = keep + spacing / 2f
    for (i in 0 until victim.segCount) {
        if (placed >= count) break
        if (victim.segD[i] >= nextD) {
            addTailPiece(TailPiece(victim.segX[i], victim.segY[i], value, pieceR, victim.style, i))
            placed++
            nextD += spacing
        }
    }
    // Any remainder (rounding) goes on the last circle.
    while (placed < count) {
        val i = victim.segCount - 1
        addTailPiece(TailPiece(victim.segX[i], victim.segY[i], value, pieceR, victim.style, i))
        placed++
    }

    victim.shrinkTo(keep)
    victim.updateSegments()
    victim.invulnerable = max(victim.invulnerable, GameConfig.CUT_PROTECT_SECONDS)
    emit(GameEvent.TailCut(by.id, victim.id, victim.segX[victim.segCount - 1], victim.segY[victim.segCount - 1], victim.style.bodyColor))
    victim.brain?.startFleeing(by)
    return dropped
}

/** Knocks [s] back, heading away from the point (fromX, fromY) it hit. */
internal fun GameSim.bounce(s: Snake, fromX: Float, fromY: Float, other: Snake) {
    var dx = s.x - fromX
    var dy = s.y - fromY
    var d = dist(s.x, s.y, fromX, fromY)
    if (d < 0.001f) {
        dx = -cos(s.heading)
        dy = -sin(s.heading)
        d = 1f
    }
    s.heading = atan2(dy, dx)
    // Push the head out of the overlap so the same contact doesn't repeat next step.
    val push = max(2f, s.radius + other.radius - d + 2f)
    s.moveHeadTo(s.x + dx / d * push, s.y + dy / d * push)
    s.updateSegments()
    s.steerLock = GameConfig.BOUNCE_SECONDS
    s.knockback = GameConfig.BOUNCE_SECONDS
    emit(GameEvent.Bounced(s.id, other.id, s.x, s.y))
}
