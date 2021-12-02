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
        Log.d(TAG, "toString: called. this=$this, coded(Int)=$coded")
        when {
            coded == 0 && showNever -> return context.getText(R.string.never).toString()
            coded == 0 -> return ""
            // every day
            coded == 0x7f -> return context.getText(R.string.every_day).toString() // 매일 설정.. 현지화된 언어로 받음. Ex) "Every day" // 내 폰에서는 "매일"
            // count selected days
            else -> {
                val dayCount = (0..6).count { it.isSet() }
                // short or long form?
                val dayStrings = when {
                    dayCount > 1 -> DateFormatSymbols().shortWeekdays
                    else -> DateFormatSymbols().weekdays
                }
                val returningStr = (0..6).filter { it.isSet() } // 여기서는 [1, 3, 5, 6] 이런값..
                    .map { dayIndex: Int -> DAY_MAP[dayIndex] }
                    .map { calDay -> dayStrings[calDay] }
                    .joinToString(context.getText(R.string.day_concat)) // 여기서는 [화, 목, 토, 일] 여기!! .getText 가 local 언어로 받음!!
                Log.d(TAG, "toString: returningStr=$returningStr")
                return returningStr // 내가 추가
            }
        }

    }
    fun toIntListJj(context: Context, showNever: Boolean): List<Int> {
        Log.d(TAG, "toIntListJj: called. this=$this, coded(Int)=$coded")

        when {
            coded == 0 && showNever -> return arrayListOf<Int>(-2) // repeat 없음
            coded == 0 -> return arrayListOf<Int>(-1) // todo: 이게 요일을 어떻게 설정했을때 뜨는건지 모르겠네. (아직까지 한번도 안 뜨네..)
            // every day
            coded == 0x7f -> return arrayListOf<Int>(8) // 127? 매일 설정.. 현지화된 언어로 받음. Ex) "Every day" // 내 폰에서는 "매일"
            // count selected days
            else -> {
                val dayCount = (0..6).count { it.isSet() }
                // short or long form?
                val dayStrings = when {
                    dayCount > 1 -> DateFormatSymbols().shortWeekdays
                    else -> DateFormatSymbols().weekdays
                }
                val returningIntList = (0..6).filter { it.isSet() } // 여기서는 [1, 3, 5, 6] 이런값.. //일 = 1 월 = 2 화 = 3 수 = 4  목 = 5 금 = 6 토 = 7
                    .map { dayIndex: Int -> DAY_MAP[dayIndex] }
                    //.map { calDay -> dayStrings[calDay] } // 여기서는 [화, 목, 토, 일] 여기!! .getText 가 local 언어로 받음!!
                    //.joinToString(context.getText(R.string.day_concat))
                Log.d(TAG, "toIntListJj: returningList[INT]=$returningIntList")
                return returningIntList // 내가 추가
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