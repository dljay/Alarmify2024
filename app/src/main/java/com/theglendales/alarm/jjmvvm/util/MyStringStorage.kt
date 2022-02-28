package com.theglendales.alarm.jjmvvm.util

import android.content.Context

class MyStringStorage(context: Context) {

    fun getStringYo(key: String): String {
        return when(key) {
            "donationP1" -> {
                "donation_p1"
            }
            "donationP2" -> {
                "donation_p2"
            }
            else -> {
                ""
            }
        }
    }
}