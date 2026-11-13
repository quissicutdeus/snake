package com.quissicutdeus.sniddle

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** [LocalDate] as the app's own [Day]. */
internal fun LocalDate.toDay() = Day(year, monthValue, dayOfMonth)

/** The app's own calendar arithmetic agrees with java.time, which counted the bedtime timer's days before. */
class DayTest {
    @Test fun epochDaysMatchJavaTime() {
        var date = LocalDate.of(1900, 1, 1)
        val end = LocalDate.of(2200, 12, 31)
        while (!date.isAfter(end)) {
            assertEquals(date.toString(), date.toEpochDay(), date.toDay().epochDay)
            date = date.plusDays(1)
        }
    }

    @Test fun theSystemCalendarIsTodayOnThisPhone() {
        assertEquals(LocalDate.now().toDay(), SystemCalendar.today())
    }
}
