package com.theglendales.alarm.jjadapters

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.theglendales.alarm.jjmvvm.JjMainViewModel
import com.theglendales.alarm.jjmvvm.JjNetworkCheckVModel

private const val TAG ="MyNetWorkChecker"

class MyNetWorkChecker(val context: Context, val jjMainVModel: JjMainViewModel) {

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
    @RequiresApi(Build.VERSION_CODES.N) //API 24 // API 23 이하 -> 콜백 기능 못 쓰는듯.
    //
    fun setNetworkListener() {
        connectivityManager.let {
            it.registerDefaultNetworkCallback(object :ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    //Connection is gained -> Tell ViewModel!
                    Log.d(TAG,"onAvailable: Internet available: OOOOOOOOOOOOOOOOOOOOO ") //최초 앱 실행시에도 (인터넷이 되니깐) 여기 log 가 작동됨.
                    jjMainVModel.updateNTWKStatus(true)
                }
                override fun onLost(network: Network) {
                    //connection is lost // 그러나 인터넷 안되는 상태(ex.airplane mode)로 최초 실행시 일로 안 들어옴!!
                    Log.d(TAG, "onLost: Internet available: XXXXXXXXXXXXXXXXXXXXX")
                    jjMainVModel.updateNTWKStatus(false)


                }
            })
        }
    }

    fun checkContextIsAlive() {

    }



}