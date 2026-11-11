package com.quissicutdeus.sniddle.game.sim

import com.quissicutdeus.sniddle.game.model.PowerType

/** Things that happened during a step, for effects (particles, shake, sounds). */
sealed class GameEvent {
    abstract val x: Float
    abstract val y: Float

    data class AteFood(
        override val x: Float,
        override val y: Float,
        val color: Int,
    ) : GameEvent()

    data class AteSnail(
        val eaterId: Int,
        override val x: Float,
        override val y: Float,
        val color: Int,
    ) : GameEvent()

    data class TailCut(
        val attackerId: Int,
        val victimId: Int,
        override val x: Float,
        override val y: Float,
        val color: Int,
    ) : GameEvent()

    data class Hurt(
        val snailId: Int,
        override val x: Float,
        override val y: Float,
    ) : GameEvent()

    data class Bounced(
        val snailId: Int,
        override val x: Float,
        override val y: Float,
    ) : GameEvent()

    data class HitWall(
        val snailId: Int,
        override val x: Float,
        override val y: Float,
    ) : GameEvent()

    /** The player picked up a power-up. */
    data class PowerUpTaken(
        val type: PowerType,
        override val x: Float,
        override val y: Float,
    ) : GameEvent()

    /** Super Armor stopped a hit on [snailId]. */
    data class Blocked(
        val snailId: Int,
        override val x: Float,
        override val y: Float,
    ) : GameEvent()

    /** The player just became the biggest snail in the arena. */
    data class BecameBiggest(
        override val x: Float,
        override val y: Float,
        /** Which player (with two kids playing, it matters whose half shakes). */
        val snailId: Int = -1,
    ) : GameEvent()

    data class PlayerEaten(
        override val x: Float,
        override val y: Float,
        /** Which player was gobbled (with two kids playing, it matters whose half shakes). */
        val snailId: Int = -1,
    ) : GameEvent()
}

/** Player steering for one step: head towards [heading] (radians) while [active]. */
class SteerInput {
    var active = false
        private set
    var heading = 0f
        private set

    fun set(heading: Float) {
        active = true
        this.heading = heading
    }

    fun release() {
        active = false
    }
}
