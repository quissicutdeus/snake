package com.quissicutdeus.sniddle.game.model

import com.quissicutdeus.sniddle.game.GameConfig
import kotlin.random.Random

/**
 * What the arena floor looks like. The three seasons follow the month; dirt
 * and space turn up in some games all year; wrapping paper is for all of
 * December. [key] is how a saved game remembers its floor: never change one.
 */
enum class ArenaSeason(
    val key: String,
) {
    /** Green grass and flowers: spring and summer. */
    GRASS("grass"),

    /** Golden grass with fallen leaves: October and November. */
    AUTUMN("autumn"),

    /** Snow with the odd snowflake: December to February. */
    SNOW("snow"),

    /** Brown garden soil with pebbles, sprouts and worm holes: some games, all year. */
    DIRT("dirt"),

    /** A dark sky full of stars, with planets and a comet: some games, all year. */
    SPACE("space"),

    /** Christmas wrapping paper with ribbons: every game and the title screen, all of December. */
    WRAPPING_PAPER("wrapping"),
    ;

    companion object {
        /** The season's floor for [month] (1-12). */
        @Suppress("MagicNumber") // months
        fun forMonth(month: Int): ArenaSeason =
            when (month) {
                12, 1, 2 -> SNOW
                10, 11 -> AUTUMN
                else -> GRASS
            }

        /** The festive floor on [day] of [month], if there is one then (it beats every other floor). */
        fun festive(
            month: Int,
            day: Int,
        ): ArenaSeason? = WRAPPING_PAPER.takeIf { month * MONTH + day in CHRISTMAS }

        /** The title screen's floor on [day] of [month]: the festive one, else the season's. */
        fun forTitle(
            month: Int,
            day: Int,
        ): ArenaSeason = festive(month, day) ?: forMonth(month)

        /**
         * A new game's floor on [day] of [month]: the festive one, else the
         * season's, dirt or space, picked with [rng] (see [GameConfig.ARENA_SEASON_WEIGHT]).
         */
        fun forGame(
            month: Int,
            day: Int,
            rng: Random,
        ): ArenaSeason {
            festive(month, day)?.let { return it }
            val roll = rng.nextInt(GameConfig.ARENA_SEASON_WEIGHT + GameConfig.ARENA_DIRT_WEIGHT + GameConfig.ARENA_SPACE_WEIGHT)
            return when {
                roll < GameConfig.ARENA_SEASON_WEIGHT -> forMonth(month)
                roll < GameConfig.ARENA_SEASON_WEIGHT + GameConfig.ARENA_DIRT_WEIGHT -> DIRT
                else -> SPACE
            }
        }

        /** The floor saved as [key]; null for one this version doesn't know. */
        fun byKey(key: String): ArenaSeason? = entries.firstOrNull { it.key == key }

        private const val MONTH = 100

        /** All of December, as month * 100 + day (like [Holiday]'s dates). */
        private val CHRISTMAS = 1201..1231
    }
}
