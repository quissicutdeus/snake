package com.quissicutdeus.sniddle

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate

/** iOS's calendar: today by the phone's own calendar settings and time zone. */
object IosCalendar : PhoneCalendar {
    override fun today(): Day {
        val calendar = NSCalendar.currentCalendar
        val now = NSDate()
        return Day(
            year = calendar.component(NSCalendarUnitYear, fromDate = now).toInt(),
            month = calendar.component(NSCalendarUnitMonth, fromDate = now).toInt(),
            dayOfMonth = calendar.component(NSCalendarUnitDay, fromDate = now).toInt(),
        )
    }
}
