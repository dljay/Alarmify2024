package com.theglendales.alarm.jjmvvm.iapAndDnldManager

import android.content.Context
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.theglendales.alarm.jjmvvm.JjMainViewModel

private const val TAG="MyDownloaderV3"
class MyDownloaderV3(val context: Context) {
    // 여기서 위에 : AppCompatActivity() 받게 하면 -> val jjMViewModel by viewmodels() -> 활성화는 된다. 하지만  :AppCompatAcitivty() 는 fragment 나 activity 에서 쓰는거니깐..
    // Your activity is not yet attached to the Application instance. You can't request ViewModel before onCreate call.  <- 이런 문제가.

    fun callme() {
        Log.d(TAG, "callme: called")
    }
    fun callYou() {
        Log.d(TAG, "callYou: called")
        //jjMainViewModel.delshit()
    }
}