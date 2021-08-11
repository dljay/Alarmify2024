package com.theglendales.alarm.jjadapters

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log

private const val TAG ="MyNetWorkChecker"

class MyNetWorkChecker(val context: Context) {

    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isNetWorkAvailable(): Boolean {
        Log.d(TAG, "isNetWorkAvailable: starts..") // API 23 이상
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = connectivityManager.activeNetwork ?: return false //.activeNetwork 는 API 23이상에서만 사용 가능. @RequiresApi 로 대체.
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                //for other device how are able to connect with Ethernet
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                //for check internet over Bluetooth
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        }else { //API 23 미만
            val nwInfo = connectivityManager.activeNetworkInfo ?: return false //if null return false
            return nwInfo.isConnected //이건 true 겠지 그럼?
        }

    }



}