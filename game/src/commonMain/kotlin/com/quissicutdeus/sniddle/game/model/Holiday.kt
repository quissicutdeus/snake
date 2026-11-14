package com.quissicutdeus.sniddle.game.model

import kotlin.random.Random

/**
 * Holidays with their own looks. Playing one game while a holiday is on keeps
 * its looks for good; until then they're locked and say when to come back.
 * While it's on, computer snails dress up for it too.
 *
 * Dates are [MonthDay]s, so a season is just a range of those and nothing
 * depends on time zones or calendars. Easter moves
 * every year, so it's worked out from the year ([Dates.easterSunday]), and a
 * kid's birthday is [BIRTHDAY], on whenever their [Birthday] is.
 */
@Suppress("MagicNumber") // the seasons are dates, and read best as dates
enum class Holiday(
    /** When to play to get it, on a locked look ("Play in December"). */
    val goal: String,
    /** What a locked look says when tapped. */
    val hint: String,
    val looks: List<Enum<*>>,
    private val seasons: List<IntRange>,
    /** What computer snails wear while it's on: its own hats, unless it says otherwise. */
    crowd: List<Enum<*>>? = null,
) {
    HALLOWEEN("Play in October", "Comes out in October!", listOf(Hat.GHOST), listOf(MonthDay.wholeMonth(10))),
    THANKSGIVING("Play in November", "Gobble gobble in November!", listOf(Hat.TURKEY), listOf(MonthDay.wholeMonth(11))),
    CHRISTMAS("Play in December", "Ho ho ho! Back in December!", listOf(Hat.SANTA), listOf(MonthDay.wholeMonth(12))),

    /** Presidents' Day, Memorial Day through Flag Day and the 4th of July, and Veterans Day. */
    FLAG_DAYS(
        "Play near July 4th",
        "Comes back for July 4th!",
        listOf(Pattern.FLAG, Hat.STAR_HAT),
        listOf(MonthDay.between(2, 12, 2, 22), MonthDay.between(5, 20, 7, 10), MonthDay.between(11, 8, 11, 14)),
    ),

    /** All of February, for Valentine's Day. Overlaps Presidents' Day, and both are on then. */
    VALENTINE("Play in February", "Comes back in February!", listOf(Pattern.HEARTS), listOf(MonthDay.wholeMonth(2))),

    /** All of March, for St Patrick's Day. */
    ST_PATRICK("Play in March", "Comes back in March!", listOf(Hat.SHAMROCK), listOf(MonthDay.wholeMonth(3))),

    /**
     * All of April, and Easter week (the week before Easter Sunday through
     * Easter Monday, see [isOn]) when Easter comes in March. Then it overlaps
     * St Patrick's, and both are on.
     */
    EASTER("Play in April", "Hops back in April!", listOf(Hat.BUNNY), listOf(MonthDay.wholeMonth(4))),

    /** A kid's own birthday: never on by the date alone, only when [on] is given their [Birthday]. Party hats all round. */
    BIRTHDAY("On your birthday!", "Play on your birthday to get it!", listOf(Hat.BIRTHDAY_CAKE), emptyList(), listOf(Hat.PARTY)),
    ;

    /** What computer snails wear while it's on. */
    private val crowd: List<Enum<*>> = crowd ?: looks.filterIsInstance<Hat>()

    /** On for [monthDay] (a [MonthDay]) of [year]. [BIRTHDAY] never is: it depends on whose birthday. */
    fun isOn(
        year: Int,
        monthDay: Int,
    ): Boolean =
        when (this) {
            EASTER -> inSeason(monthDay) || Dates.isEasterWeek(year, MonthDay.monthOf(monthDay), MonthDay.dayOf(monthDay))
            else -> inSeason(monthDay)
        }

    /** [monthDay] is in one of its fixed seasons (every year the same: not Easter week or a birthday). */
    fun inSeason(monthDay: Int): Boolean = seasons.any { monthDay in it }

    companion object {
        /**
         * Every holiday that's on for [day] of [month] (1-12) in [year], plus
         * [BIRTHDAY] if it's the [birthday] of whoever is playing.
         */
        fun on(
            year: Int,
            month: Int,
            day: Int,
            birthday: Birthday? = null,
        ): Set<Holiday> {
            val on = entries.filterTo(LinkedHashSet()) { it.isOn(year, MonthDay.of(month, day)) }
            if (birthday != null && birthday.isOn(year, month, day)) on += BIRTHDAY
            return on
        }

        /** The holiday whose look [item] is, or null for an everyday look. */
        fun of(item: Enum<*>): Holiday? = entries.firstOrNull { item in it.looks }

        /** How often a computer snail wears the hat (or pattern) of a holiday that's on. */
        private const val DRESS_UP_CHANCE = 0.4f

        /**
         * A computer snail's look for [season]: holiday looks from other times
         * of year are swapped for everyday ones, and while a holiday is on
         * plenty of snails wear its hat (or its pattern, if it has one).
         */
        fun dressUp(
            style: SnailStyle,
            rng: Random,
            season: Set<Holiday>,
        ): SnailStyle {
            fun <T : Enum<*>> fit(
                current: T,
                all: List<T>,
            ): T = if (of(current).let { it == null || it in season }) current else all.filter { of(it) == null }.random(rng)

            fun <T : Enum<*>> dress(
                current: T,
                all: List<T>,
                worn: List<T>,
            ): T = if (worn.isNotEmpty() && rng.nextFloat() < DRESS_UP_CHANCE) worn.random(rng) else fit(current, all)
            val crowd = season.flatMap { it.crowd }
            val hat = dress(style.hat, Hat.entries, crowd.filterIsInstance<Hat>())
            val pattern = dress(style.pattern, Pattern.entries, season.flatMap { it.looks }.filterIsInstance<Pattern>())
            return style.copy(pattern = pattern, hat = hat)
        }
    }
}
