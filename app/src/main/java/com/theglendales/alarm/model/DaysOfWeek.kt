package com.theglendales.alarm.model

import android.content.Context
import android.util.Log
import com.theglendales.alarm.R
import java.text.DateFormatSymbols
import java.util.Calendar

/*
 * Days of week code as a single int. 0x00: no day, 0x01: Monday,
 * 0x02: Tuesday, 0x04: Wednesday, 0x08: Thursday, 0x10: Friday, 0x20: Saturday, 0x40:  Sunday
 */
private const val TAG="DaysOfWeek"
data class DaysOfWeek(val coded: Int) { // coded = mutableDays 콘스트럭터. ex. DaysOfWeek(mutableDays)
    // Returns days of week encoded in an array of booleans.
    val booleanArray = BooleanArray(7) { index -> index.isSet() }
    val isRepeatSet = coded != 0

    fun toString(context: Context, showNever: Boolean): String {
        //Log.d(TAG, "toString: called. this=$this, coded=$coded")
        return when {
            coded == 0 && showNever -> context.getText(R.string.never).toString()
            coded == 0 -> ""
            // every day
            coded == 0x7f -> return context.getText(R.string.every_day).toString()
            // count selected days
            else -> {
                val dayCount = (0..6).count { it.isSet() }
                // short or long form?
                val dayStrings = when {
                    dayCount > 1 -> DateFormatSymbols().shortWeekdays
                    else -> DateFormatSymbols().weekdays
                }

                (0..6).filter { it.isSet() }
                        .map { dayIndex -> DAY_MAP[dayIndex] }
                        .map { calDay -> dayStrings[calDay] }
                        .joinToString(context.getText(R.string.day_concat))
            }
        }

    }

    private fun Int.isSet(): Boolean {
        //Log.d(TAG, "isSet: called. coded=$coded")
        return coded and (1 shl this) > 0
    }

    /**
     * returns number of days from today until next alarm
     */
    fun getNextAlarm(today: Calendar): Int {
        val todayIndex = (today.get(Calendar.DAY_OF_WEEK) + 5) % 7
        //Log.d(TAG, "getNextAlarm: called. todayIndex=$todayIndex")

        return (0..6).firstOrNull { dayCount ->
            val day = (todayIndex + dayCount) % 7
            day.isSet()
        } ?: -1
    }

    override fun toString(): String {
        return (if (0.isSet()) "m" else "_") +
                (if (1.isSet()) 't' else '_') +
                (if (2.isSet()) 'w' else '_') +
                (if (3.isSet()) 't' else '_') +
                (if (4.isSet()) 'f' else '_') +
                (if (5.isSet()) 's' else '_') +
                if (6.isSet()) 's' else '_'
    }

    companion object {
        private val DAY_MAP = intArrayOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)
    }
}