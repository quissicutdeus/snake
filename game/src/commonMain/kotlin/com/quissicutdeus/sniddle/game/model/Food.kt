package com.quissicutdeus.sniddle.game.model

/** Anything a snail can eat. Position is mutable because food drifts towards nearby heads. */
interface Food {
    var x: Float
    var y: Float
    val value: Float
    val radius: Float

    /** True once eaten (lets AI notice that its target is gone) or expired. */
    val gone: Boolean
}

/** A food dot. */
class Pellet(
    override var x: Float,
    override var y: Float,
    override val value: Float,
    override val radius: Float,
    /** ARGB color. */
    val color: Int,
    /** Random phase so pellets don't all pulse together. */
    val phase: Float,
) : Food {
    /** Seconds since it appeared (it pops in). */
    var age = 0f
    override var gone = false
}

/** A chunk of a severed tail lying on the ground. Anyone can eat it. */
class TailPiece(
    override var x: Float,
    override var y: Float,
    override val value: Float,
    override val radius: Float,
    val style: SnailStyle,
    /** Index along the old body, so patterns keep alternating across pieces. */
    val index: Int,
) : Food {
    /** Seconds since it fell off (it fades away at the end). */
    var age = 0f
    override var gone = false
}
