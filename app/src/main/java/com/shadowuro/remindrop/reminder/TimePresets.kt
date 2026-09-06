package com.shadowuro.remindrop.reminder

import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimePresets {
    fun inOneHour(now: Long = System.currentTimeMillis()): Long = now + 60L * 60L * 1000L

    fun inThirtyMinutes(now: Long = System.currentTimeMillis()): Long = now + 30L * 60L * 1000L

    fun thisEvening(now: Long = System.currentTimeMillis()): Long {
        val target = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 19)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now + 15L * 60L * 1000L) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    fun tomorrow(now: Long = System.currentTimeMillis()): Long = Calendar.getInstance().run {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    fun thisWeekend(now: Long = System.currentTimeMillis()): Long {
        val target = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentDay = target.get(Calendar.DAY_OF_WEEK)
        var daysUntilSaturday = (Calendar.SATURDAY - currentDay + 7) % 7
        if (daysUntilSaturday == 0 && target.timeInMillis <= now) daysUntilSaturday = 7
        target.add(Calendar.DAY_OF_YEAR, daysUntilSaturday)
        return target.timeInMillis
    }

    fun format(timeMillis: Long, locale: Locale = Locale.getDefault()): String {
        val date = Date(timeMillis)
        val datePart = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(date)
        val timePart = DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(date)
        return "$datePart · $timePart"
    }
}
