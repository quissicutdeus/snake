package com.quissicutdeus.sniddle.game.model

/**
 * A day of the year as month * 100 + day (1225 is 25 December), so a stretch
 * of the year is just a range of those: no time zones, no calendars. Used by
 * the holidays, the festive floors and the saved birthdays.
 */
@Suppress("MagicNumber") // calendar rules are made of the calendar's own numbers
object MonthDay {
    /** One month in month * 100 + day. */
    const val MONTH = 100

    /** [day] of [month] (1-12). */
    fun of(
        month: Int,
        day: Int,
    ): Int = month * MONTH + day

    fun monthOf(monthDay: Int): Int = monthDay / MONTH

    fun dayOf(monthDay: Int): Int = monthDay % MONTH

    /** All of [month] (1-12). */
    fun wholeMonth(month: Int): IntRange = of(month, 1)..of(month, Dates.daysIn(month))

    /** From [fromDay] of [fromMonth] to [toDay] of [toMonth], both included, within one year. */
    fun between(
        fromMonth: Int,
        fromDay: Int,
        toMonth: Int,
        toDay: Int,
    ): IntRange = of(fromMonth, fromDay)..of(toMonth, toDay)
}

/**
 * Date sums the game needs, worked out by hand so nothing depends on a
 * platform's calendar or time zone: the app reads today's date and hands
 * these the plain numbers.
 */
@Suppress("MagicNumber") // calendar rules are made of the calendar's own numbers
object Dates {
    /** February has 29 days this [year] (every 4th year, but only every 400th of the century years). */
    fun isLeapYear(year: Int): Boolean = if (year % 100 == 0) year % 400 == 0 else year % 4 == 0

    /** Days in [month] (1-12), counting 29 February so a birthday can be on it. */
    fun daysIn(month: Int): Int =
        when (month) {
            2 -> 29
            4, 6, 9, 11 -> 30
            else -> 31
        }

    /**
     * Easter Sunday in [year] as days after the last day of February: 22 is
     * 22 March, 32 is 1 April. Easter always falls between 22 March and
     * 25 April, so this is 22..56. The "anonymous Gregorian" sum (Meeus /
     * Jones / Butcher).
     */
    fun easterSunday(year: Int): Int {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = (h + l - 7 * m + 114) % 31 + 1
        return if (month == 3) day else MARCH + day
    }

    /** [day] of [month] in [year] is in the week before Easter Sunday, or is Easter Sunday or Monday. */
    fun isEasterWeek(
        year: Int,
        month: Int,
        day: Int,
    ): Boolean {
        val spring = springDay(month, day) ?: return false
        val sunday = easterSunday(year)
        return spring in sunday - DAYS_BEFORE_EASTER..sunday + 1
    }

    /** [month] and [day] as days after the last day of February, for March and April; null for any other month. */
    private fun springDay(
        month: Int,
        day: Int,
    ): Int? =
        when (month) {
            3 -> day
            4 -> MARCH + day
            else -> null
        }

    /** Days in March, to count April on from it. */
    private const val MARCH = 31

    private const val DAYS_BEFORE_EASTER = 7
}

/**
 * A kid's birthday: just the month (1-12) and day, never the year, so it
 * says nothing about how old anyone is.
 */
@Suppress("MagicNumber") // 29 February and 28 February are dates
data class Birthday(
    val month: Int,
    val day: Int,
) {
    init {
        require(month in 1..MONTHS && day in 1..Dates.daysIn(month)) { "no such day: $month/$day" }
    }

    /**
     * True on the birthday in [year]. A 29 February birthday is
     * celebrated on 28 February in years that don't have a 29th.
     */
    fun isOn(
        year: Int,
        month: Int,
        day: Int,
    ): Boolean {
        val leapDay = this.month == 2 && this.day == 29
        return if (leapDay && !Dates.isLeapYear(year)) month == 2 && day == 28 else month == this.month && day == this.day
    }

    /** Saved as month * 100 + day (1225 for 25 December), see [MonthDay]. */
    fun encode(): Int = MonthDay.of(month, day)

    companion object {
        private const val MONTHS = 12

        /** Reads [encode] output; null for nothing saved or a day that doesn't exist. */
        fun decode(value: Int?): Birthday? {
            if (value == null) return null
            val month = MonthDay.monthOf(value)
            val day = MonthDay.dayOf(value)
            return if (month in 1..MONTHS && day in 1..Dates.daysIn(month)) Birthday(month, day) else null
        }
    }
}
