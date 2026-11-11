package com.quissicutdeus.snake.game.model

import kotlin.random.Random

/**
 * Holidays with their own looks. Playing one game while a holiday is on keeps
 * its looks for good; until then they're locked and say when to come back.
 * While it's on, computer snakes dress up for it too.
 *
 * Dates are month * 100 + day (1225 is 25 December), so a season is just a
 * range of those and nothing depends on time zones or calendars.
 */
@Suppress("MagicNumber") // the seasons are dates, and read best as dates
enum class Holiday(
    /** When to play to get it, on a locked look ("Play in December"). */
    val goal: String,
    /** What a locked look says when tapped. */
    val hint: String,
    val looks: List<Enum<*>>,
    private val seasons: List<IntRange>,
) {
    HALLOWEEN("Play in October", "Comes out in October!", listOf(Hat.GHOST), listOf(1001..1031)),
    THANKSGIVING("Play in November", "Gobble gobble in November!", listOf(Hat.TURKEY), listOf(1101..1130)),
    CHRISTMAS("Play in December", "Ho ho ho! Back in December!", listOf(Hat.SANTA), listOf(1201..1231)),

    /** Presidents' Day, Memorial Day through Flag Day and the 4th of July, and Veterans Day. */
    FLAG_DAYS(
        "Play near July 4th",
        "Comes back for July 4th!",
        listOf(Pattern.FLAG, Hat.STAR_HAT),
        listOf(212..222, 520..710, 1108..1114),
    ),
    ;

    fun isOn(monthDay: Int): Boolean = seasons.any { monthDay in it }

    companion object {
        /** Every holiday that's on for [month] (1-12) and [day]. */
        fun on(
            month: Int,
            day: Int,
        ): Set<Holiday> = entries.filterTo(LinkedHashSet()) { it.isOn(month * MONTH + day) }

        /** The holiday whose look [item] is, or null for an everyday look. */
        fun of(item: Enum<*>): Holiday? = entries.firstOrNull { item in it.looks }

        /** How often a computer snake wears the hat of a holiday that's on. */
        private const val DRESS_UP_CHANCE = 0.4f

        private const val MONTH = 100

        /**
         * A computer snake's look for [season]: holiday looks from other times
         * of year are swapped for everyday ones, and while a holiday is on
         * plenty of snakes wear its hat.
         */
        fun dressUp(
            style: SnakeStyle,
            rng: Random,
            season: Set<Holiday>,
        ): SnakeStyle {
            fun <T : Enum<*>> fit(
                current: T,
                all: List<T>,
            ): T = if (of(current).let { it == null || it in season }) current else all.filter { of(it) == null }.random(rng)
            val hats = season.flatMap { h -> h.looks.filterIsInstance<Hat>() }
            val hat = if (hats.isNotEmpty() && rng.nextFloat() < DRESS_UP_CHANCE) hats.random(rng) else fit(style.hat, Hat.entries)
            return style.copy(pattern = fit(style.pattern, Pattern.entries), hat = hat)
        }
    }
}
