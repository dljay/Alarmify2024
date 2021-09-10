package com.theglendales.alarm.jjmvvm.helper

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

private const val TAG="MySharedPrefManager"

class MySharedPrefManager(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences("SecondFrag_Info", Context.MODE_PRIVATE)

    fun saveTest() {
        prefs.edit().putString("ONE","TEST ONE").apply()
        Log.d(TAG, "saveTest: saved. I guess..")
    }
    fun getTest() {
        val result = prefs.getString("ONE","Default").toString()
        Log.d(TAG, "getTest: $result")
    }
    fun calledFromActivity() {
        Log.d(TAG, "calledFromActivity: called from AlarmsListActivity.")
    }
}