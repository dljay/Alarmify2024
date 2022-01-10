package com.theglendales.alarm.jjmvvm.util

import android.content.Context
import android.widget.Toast

class ToastMessenger(val context: Context) {

    fun showMyToast(msg: String, isShort: Boolean) {
        if(isShort) {
            Toast.makeText(context,msg,Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context,msg,Toast.LENGTH_LONG).show()
        }
    }
}