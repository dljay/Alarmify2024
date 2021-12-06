package com.theglendales.alarm.jjmvvm.helper

import android.util.Log

private const val TAG="BadgeSortHelper"
class BadgeSortHelper {

    companion object {
        fun getBadgesListFromStr(badgeStrings: String?): List<String>? {
            Log.d(TAG, "getBadgesListFromStr: called.")
            //Ex) "I,G,H" => 이걸 받으면=> ',' 쉼표로 나눠서 String List 로 만들고=> 밑에 Activate 에서 => Intense, Gentle, Human 배지들을 visibility=visible 로 바꿔줌.
            if(!badgeStrings.isNullOrEmpty()) {
                val badgeStrList: List<String> = badgeStrings.split(",").map {badgeInitial -> badgeInitial.trim()}
                return badgeStrList
            } else {
                return null
            }
        }
    }

}