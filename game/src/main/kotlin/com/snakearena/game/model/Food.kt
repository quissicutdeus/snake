package com.snakearena.game.model

/** A food dot. [x]/[y] are mutable because food drifts towards nearby heads. */
class Pellet(
    var x: Float,
    var y: Float,
    val value: Float,
    val radius: Float,
    /** ARGB color. */
    val color: Int,
    /** Random phase so pellets don't all pulse together. */
    val phase: Float,
) {
    /** Seconds since it appeared (it pops in). */
    var age = 0f

    /** True once eaten (lets AI notice that its target is gone). */
    var gone = false
}

/** A chunk of a severed tail lying on the ground. Anyone can eat it. */
class TailPiece(
    var x: Float,
    var y: Float,
    val value: Float,
    val radius: Float,
    val style: SnakeStyle,
    /** Index along the old body, so patterns keep alternating across pieces. */
    val index: Int,
) {
    /** Seconds since it fell off. */
    var age = 0f

    /** True once eaten or expired. */
    var gone = false
}
