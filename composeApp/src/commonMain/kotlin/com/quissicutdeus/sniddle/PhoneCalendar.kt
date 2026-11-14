package com.quissicutdeus.sniddle

/** A date on the phone's calendar: [month] 1 to 12, [dayOfMonth] from 1. */
data class Day(
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
) {
    /**
     * Days since 1 January 1970, the same number as java.time's epoch day (the
     * bedtime timer saves it, see PlayDay). Plain arithmetic on the proleptic
     * Gregorian calendar, after Howard Hinnant's days_from_civil.
     */
    val epochDay: Long
        get() {
            // Count years from March, so a leap day falls at the end of one.
            val y = (if (month <= 2) year - 1 else year).toLong()
            val era = y.floorDiv(YEARS_PER_ERA)
            val yearOfEra = y - era * YEARS_PER_ERA
            val monthFromMarch = if (month > 2) month - MARCH else month + MONTHS_AFTER_FEBRUARY
            val dayOfYear = (DAYS_PER_FIVE_MONTHS * monthFromMarch + 2) / FIVE_MONTHS + dayOfMonth - 1
            val dayOfEra = yearOfEra * DAYS_PER_YEAR + yearOfEra / LEAP_EVERY - yearOfEra / NO_LEAP_EVERY + dayOfYear
            return era * DAYS_PER_ERA + dayOfEra - DAYS_FROM_YEAR_0_TO_1970
        }

    private companion object {
        const val YEARS_PER_ERA = 400L
        const val DAYS_PER_ERA = 146_097L
        const val DAYS_PER_YEAR = 365L
        const val LEAP_EVERY = 4L
        const val NO_LEAP_EVERY = 100L
        const val MARCH = 3
        const val MONTHS_AFTER_FEBRUARY = 9
        const val DAYS_PER_FIVE_MONTHS = 153
        const val FIVE_MONTHS = 5

        /** From 1 March of year 0 to 1 January 1970. */
        const val DAYS_FROM_YEAR_0_TO_1970 = 719_468L
    }
}

/** Today's date by the phone's own calendar and time zone. */
fun interface PhoneCalendar {
    fun today(): Day
}
