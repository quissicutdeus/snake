package com.snakearena.game.sim

/** Things that happened during a step, for effects (particles, shake, sounds). */
sealed class GameEvent {
    abstract val x: Float
    abstract val y: Float

    data class AteFood(val snakeId: Int, override val x: Float, override val y: Float, val color: Int) : GameEvent()
    data class AteSnake(val eaterId: Int, val victimId: Int, override val x: Float, override val y: Float, val color: Int) : GameEvent()
    data class TailCut(val attackerId: Int, val victimId: Int, override val x: Float, override val y: Float, val color: Int) : GameEvent()
    data class Hurt(val snakeId: Int, val byId: Int, override val x: Float, override val y: Float, val heartsLeft: Int) : GameEvent()
    data class Bounced(val snakeId: Int, val otherId: Int, override val x: Float, override val y: Float) : GameEvent()
    data class HitWall(val snakeId: Int, override val x: Float, override val y: Float) : GameEvent()
    data class PlayerEaten(val byId: Int, override val x: Float, override val y: Float) : GameEvent()
}

/** Player steering for one step: head towards [heading] (radians) while [active]. */
class SteerInput {
    var active = false
    var heading = 0f

    fun set(heading: Float) {
        active = true
        this.heading = heading
    }

    fun release() {
        active = false
    }
}
