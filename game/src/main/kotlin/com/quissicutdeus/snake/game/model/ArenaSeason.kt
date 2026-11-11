package com.quissicutdeus.snake.game.model

/** What the arena floor looks like at this time of year. */
enum class ArenaSeason {
    /** Green grass and flowers: spring and summer. */
    GRASS,

    /** Golden grass with fallen leaves: October and November. */
    AUTUMN,

    /** Snow with the odd snowflake: December to February. */
    SNOW,
    ;

    companion object {
        /** The floor for [month] (1-12). */
        @Suppress("MagicNumber") // months
        fun forMonth(month: Int): ArenaSeason =
            when (month) {
                12, 1, 2 -> SNOW
                10, 11 -> AUTUMN
                else -> GRASS
            }
    }
}
