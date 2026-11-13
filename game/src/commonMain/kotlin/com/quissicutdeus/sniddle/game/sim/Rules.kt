package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.GameConfig
import com.quissicutdeus.sniddle.game.model.PowerType
import com.quissicutdeus.sniddle.game.model.Snail
import com.quissicutdeus.sniddle.game.model.TailPiece
import com.quissicutdeus.sniddle.game.model.dist
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * The snail-vs-snail rules. [Contact.a]'s head touched circle [Contact.seg] of
 * [Contact.b]:
 *  - roughly equal size: bounce, no damage (head-to-head: both bounce);
 *  - a is bigger: head-to-head hurts b (eaten at 0 hearts), body hit cuts b's tail off;
 *  - a is smaller: a bounces back and loses a heart (eaten at 0 hearts). Except
 *    the player running into a bigger *body* just bounces: only a bigger snail's
 *    mouth can hurt the player, so whoever eats them is right there on screen
 *    (the camera follows the player's head), never a long body whose head is
 *    somewhere off-screen.
 * A protected (blinking) snail can't be hurt or cut; contacts that would hurt it are ignored.
 */
internal fun GameSim.resolveContact(c: Contact) {
    val a = c.a
    val b = c.b
    val stale = !a.alive || !b.alive || c.seg >= b.segCount
    val headToHead = c.seg == 0
    if (stale || (headToHead && !markHeadPair(a, b))) return

    val cx = b.segX[c.seg]
    val cy = b.segY[c.seg]
    when (a.fightRelationTo(b)) {
        SizeRelation.EQUAL -> {
            bounceOffEqual(a, b, headToHead, cx, cy)
        }

        SizeRelation.BIGGER -> {
            bite(attacker = a, victim = b, seg = c.seg)
        }

        SizeRelation.SMALLER -> {
            if (a.isPlayer && !headToHead) {
                bounceOffBiggerBody(a, b, cx, cy)
            } else {
                bumpIntoBigger(a = a, bigger = b, cx = cx, cy = cy)
            }
        }
    }
}

/** Same-size snails just bounce: [a] off the point it hit, and [b] too if it was head-to-head. */
private fun GameSim.bounceOffEqual(
    a: Snail,
    b: Snail,
    headToHead: Boolean,
    cx: Float,
    cy: Float,
) {
    if (a.invulnerable > 0f || b.invulnerable > 0f) return
    if (a.knockback <= 0f) bounce(a, cx, cy, b)
    if (headToHead && b.knockback <= 0f) bounce(b, a.x, a.y, a)
}

/** A bigger head bites: head-to-head hurts the victim, a body bite cuts its tail off. */
private fun GameSim.bite(
    attacker: Snail,
    victim: Snail,
    seg: Int,
) {
    if (victim.invulnerable > 0f) return
    if (victim.hasPower(PowerType.ARMOR)) {
        // The bite bounces off the armor.
        if (attacker.knockback <= 0f) bounce(attacker, victim.segX[seg], victim.segY[seg], victim)
        emit(GameEvent.Blocked(victim.id, victim.segX[seg], victim.segY[seg]))
        return
    }
    if (seg == 0) damage(victim = victim, by = attacker) else sever(victim = victim, seg = seg, by = attacker)
}

/** The player's head ran into a bigger snail's body: a harmless bounce (*boing*). */
private fun GameSim.bounceOffBiggerBody(
    player: Snail,
    bigger: Snail,
    cx: Float,
    cy: Float,
) {
    if (player.knockback <= 0f) bounce(player, cx, cy, bigger)
}

/** [a] ran into a snail that wins the fight: it gets hurt, unless armor makes it just bounce. */
private fun GameSim.bumpIntoBigger(
    a: Snail,
    bigger: Snail,
    cx: Float,
    cy: Float,
) {
    if (!a.hasPower(PowerType.ARMOR)) {
        damage(victim = a, by = bigger, fromX = cx, fromY = cy)
        return
    }
    if (a.knockback <= 0f) {
        bounce(a, cx, cy, bigger)
        emit(GameEvent.Blocked(a.id, cx, cy))
    }
}

/** Victim loses a heart and bounces away; at 0 hearts it is eaten by [by]. */
internal fun GameSim.damage(
    victim: Snail,
    by: Snail,
    fromX: Float = by.x,
    fromY: Float = by.y,
) {
    if (victim.invulnerable > 0f || victim.hasPower(PowerType.ARMOR) || !victim.alive) return
    victim.hearts--
    if (victim.hearts <= 0) {
        eatWhole(eater = by, victim = victim)
        return
    }
    bounce(victim, fromX, fromY, by)
    victim.invulnerable = GameConfig.HURT_INVULNERABLE_SECONDS
    emit(GameEvent.Hurt(victim.id, victim.x, victim.y))
    victim.brain?.startFleeing(by)
}

/** [eater] swallows [victim] whole and grows. */
internal fun GameSim.eatWhole(
    eater: Snail,
    victim: Snail,
) {
    victim.alive = false
    victim.hearts = 0
    feed(eater, victim.length * GameConfig.EAT_WHOLE_GAIN)
    eater.snailsEaten++
    eater.sinceAte = 0f
    emit(GameEvent.AteSnail(eater.id, victim.x, victim.y, victim.style.bodyColor))
    if (victim.isPlayer) {
        // Two kids: the gobbled one comes back soon, and the game goes on.
        val t = together
        if (t != null) {
            kidGobbled(t, victim, eater)
        } else {
            gameOver = true
            eatenBy = Eater(eater.name, eater.style)
        }
        emit(GameEvent.PlayerEaten(victim.x, victim.y, victim.id))
    }
}

/**
 * Cuts [victim] at body circle [seg]: the front part survives (never below the
 * minimum length) and the rest falls off as [TailPiece]s anyone can eat.
 * Returns the length that fell off (0 if there was nothing to cut).
 */
internal fun GameSim.sever(
    victim: Snail,
    seg: Int,
    by: Snail,
): Float {
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

/** Closer than this, the bounce direction is just "backwards". */
private const val MIN_BOUNCE_DISTANCE = 0.001f

/** Knocks [s] back, heading away from the point (fromX, fromY) it hit. */
internal fun GameSim.bounce(
    s: Snail,
    fromX: Float,
    fromY: Float,
    other: Snail,
) {
    var dx = s.x - fromX
    var dy = s.y - fromY
    var d = dist(s.x, s.y, fromX, fromY)
    if (d < MIN_BOUNCE_DISTANCE) {
        dx = -cos(s.heading)
        dy = -sin(s.heading)
        d = 1f
    }
    s.heading = atan2(dy, dx)
    // Push the head out of the overlap so the same contact doesn't repeat next step.
    val push = max(2f, s.radius + other.radius - d + 2f)
    // Never push a head through the fence.
    val r = s.radius
    s.moveHeadTo((s.x + dx / d * push).coerceIn(r, world.width - r), (s.y + dy / d * push).coerceIn(r, world.height - r))
    s.updateSegments()
    s.steerLock = GameConfig.BOUNCE_SECONDS
    s.knockback = GameConfig.BOUNCE_SECONDS
    emit(GameEvent.Bounced(s.id, s.x, s.y))
}
