package com.quissicutdeus.sniddle.game.model

import com.quissicutdeus.sniddle.game.GameConfig

/** The three powers. Each lasts [seconds]; picking one up again tops it up. */
enum class PowerType(
    val label: String,
    val seconds: Float,
) {
    /** You win every fight: eat or snip snails bigger than you. */
    STRENGTH("Super Strength", GameConfig.STRENGTH_SECONDS),

    /** Much faster, and turns quicker. */
    SPEED("Super Speed", GameConfig.SPEED_SECONDS),

    /** Nothing can hurt, snip or eat you; bigger snails bounce off. */
    ARMOR("Super Armor", GameConfig.ARMOR_SECONDS),
}

/** A glowing power-up lying on the map. */
class PowerUp(
    val type: PowerType,
    val x: Float,
    val y: Float,
) {
    /** Seconds since it appeared (it pops in, and blinks before it vanishes). */
    var age = 0f
}
