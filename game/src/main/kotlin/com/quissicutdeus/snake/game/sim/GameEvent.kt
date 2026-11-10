package com.quissicutdeus.snake.game.sim

/** Things that happened during a step, for effects (particles, shake, sounds). */
sealed class GameEvent {
    abstract val x: Float
    abstract val y: Float

    data class AteFood(
        override val x: Float,
        override val y: Float,
        val color: Int,
    ) : GameEvent()

    data class AteSnake(
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
        val snakeId: Int,
        override val x: Float,
        override val y: Float,
    ) : GameEvent()

    data class Bounced(
        val snakeId: Int,
        override val x: Float,
        override val y: Float,
    ) : GameEvent()

    data class HitWall(
        val snakeId: Int,
        override val x: Float,
        override val y: Float,
    ) : GameEvent()

    data class PlayerEaten(
        override val x: Float,
        override val y: Float,
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
