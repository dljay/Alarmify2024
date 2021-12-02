/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theglendales.alarm.view

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.theglendales.alarm.model.DaysOfWeek
import io.reactivex.Single
import java.text.DateFormatSymbols
import java.util.Calendar

private const val TAG="RepeatPreference"
fun DaysOfWeek.summary(context: Context): String {
    return toString(context, true)
}

// 기존 .summary 는 요일을 local string 으로 받아서 (ex. 월,화. Mon, Tue..) Int 로 받기 테스트.
// 이게 listFrag 에서 요일 표시하는데 영향이 있을지 확인 필요..
fun DaysOfWeek.summaryInNumber(context: Context): List<Int> {
    return toIntListJj(context, true)
}

//Extension Function: ClassName.xxx()


fun DaysOfWeek.onChipDayClicked(which: Int, isChecked:Boolean): Single<DaysOfWeek> {
    return Single.create { emitter->
        var mutableDays: Int = coded
        mutableDays = when { // 아래에서 or 는 연산이지 '또는' 이 아님..
            isChecked -> {mutableDays or (1 shl which)} // 기존 mutableDays + 선택된 which 의 bitwise 연산으로 새로운 mutableDays 값을 얻음!
                //Log.d(TAG, "onChipDayClicked: isChecked=[$isChecked] which=$which, 1 shl which= ${1 shl which},mutD or 1shlWhich=${mutableDays or (1 shl which)},  ")}
            else -> {mutableDays and (1 shl which).inv()}
                //Log.d(TAG, "onChipDayClicked: isChecked=[$isChecked], which=$which, 1 shl which= ${1 shl which}, else")}
        }
        Log.d(TAG, "onChipDayClicked: isChecked=[$isChecked], mutableDays=$mutableDays")
        emitter.onSuccess(DaysOfWeek(mutableDays))

    }
}

fun DaysOfWeek.showDialog(context: Context): Single<DaysOfWeek> {
    return Single.create { emitter ->
        val weekdays = DateFormatSymbols().weekdays
        val entries = arrayOf(weekdays[Calendar.MONDAY], weekdays[Calendar.TUESDAY], weekdays[Calendar.WEDNESDAY], weekdays[Calendar.THURSDAY], weekdays[Calendar.FRIDAY], weekdays[Calendar.SATURDAY], weekdays[Calendar.SUNDAY])
        var mutableDays: Int = coded
        Log.d(TAG, "showDialog: mutableDays=$mutableDays, DaysOfWeek(mutableDays)=${DaysOfWeek(mutableDays)}")

        AlertDialog.Builder(context)
                .setMultiChoiceItems(entries, booleanArray) { _, which, isChecked ->
                    mutableDays = when {
                        isChecked -> {mutableDays or (1 shl which)
                            Log.d(TAG, "showDialog: which=$which, 1 shl which= ${1 shl which}, mutD or 1shlWhich=${mutableDays or (1 shl which)} isChecked=$isChecked")}
                        else -> {mutableDays and (1 shl which).inv()
                            Log.d(TAG, "showDialog: which=$which, 1 shl which= ${1 shl which}, else")}
                        
                    }
                    Log.d(TAG, "showDialog: .setMultiChoiceItems 체크박스 누를때마다 여기로 들어옴= mutableDays=$mutableDays")
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    emitter.onSuccess(DaysOfWeek(mutableDays))
                    Log.d(TAG, "showDialog: .setPositiveButton(ok). mutableDays=$mutableDays")
                }
                .setOnCancelListener {
                    emitter.onSuccess(DaysOfWeek(mutableDays))
                    Log.d(TAG, "showDialog: .setCancelListener. mutableDays=$mutableDays")
                }
                .create()
                .show()
    }
}
