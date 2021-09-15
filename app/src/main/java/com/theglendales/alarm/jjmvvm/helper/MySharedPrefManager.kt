package com.theglendales.alarm.jjmvvm.helper

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.theglendales.alarm.jjmvvm.data.PlayInfoContainer
import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp
import java.lang.Exception

private const val TAG="MySharedPrefManager"

class MySharedPrefManager(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences("TrackPlayInfo", Context.MODE_PRIVATE) //TrackPlayInfo = xml 파일 이름!!
    private val gson: Gson = Gson()

    fun savePlayInfo(playInfoObject: PlayInfoContainer) {
        Log.d(TAG, "savePlayInfo: savePlayInfo. trId=${playInfoObject.trackID}")
        val jsonStrSave = gson.toJson(playInfoObject)
        prefs.edit().putString("CurrentPlayInfo", jsonStrSave).apply()

    }
    fun getPlayInfo(): PlayInfoContainer {
        return try{
            val jsonStrGet = prefs.getString("CurrentPlayInfo","No Data")
            val playInfo = gson.fromJson(jsonStrGet, PlayInfoContainer::class.java)
            //Log.d(TAG, "getPlayInfo: trId= ${playInfo.trackID}, songStatusMp= ${playInfo.songStatusMp}")
            playInfo
        }catch (e: Exception) {
            Log.d(TAG, "getPlayInfo: Possibly no saved data yet..error message=$e")
            PlayInfoContainer(-10,-10,-10,StatusMp.IDLE) // 에러 발생시 default 값 PlayInfo 를 return
        }

    }
    fun calledFromActivity() {
        Log.d(TAG, "calledFromActivity: called from AlarmsListActivity.")
    }
}