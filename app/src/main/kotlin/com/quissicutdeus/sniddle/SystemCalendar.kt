package com.quissicutdeus.sniddle

import java.time.LocalDate

/** Android's (and the JVM's) calendar: today in the phone's time zone. */
object SystemCalendar : PhoneCalendar {
    override fun today(): Day = LocalDate.now().let { Day(it.year, it.monthValue, it.dayOfMonth) }
}
