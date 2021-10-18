package com.theglendales.alarm.jjmvvm.helper

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.theglendales.alarm.jjmvvm.data.PlayInfoContainer
import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp
import com.theglendales.alarm.jjmvvm.util.RtWithAlbumArt
import java.lang.Exception
import java.lang.reflect.Type

private const val TAG="MySharedPrefManager"
//private const val TR_PLAY_INF_SHARED_PREF = "TrackPlayInfo"
private const val ON_DISK_RTA_ART_URI_LIST = "RtaArtPathList" //todo: 현재는 .rta 는 Uri 로 ..  .art 는 String path 로 저장
private const val URI_STORED_KEY ="RtaArt_Key"


class MySharedPrefManager(context: Context) {

    //    val prefs: SharedPreferences = context.getSharedPreferences(TR_PLAY_INF_SHARED_PREF, Context.MODE_PRIVATE) //TrackPlayInfo = xml 파일 이름!!
    val prefs: SharedPreferences = context.getSharedPreferences(ON_DISK_RTA_ART_URI_LIST, Context.MODE_PRIVATE) // OnDiskRtaArtUriList = xml 파일 이름 (디스크에 저장된 rta, art 파일 uri 저장)

    private val gson: Gson = Gson()

    inline fun <reified T> genericType() = object: TypeToken<T>() {}.type // todo: 이것이 무엇인지 inline 에 대해서 공부해봐야함.

    fun getRtaArtPathList(): MutableList<RtWithAlbumArt> {
        return try{
            val jsonStrGet = prefs.getString(URI_STORED_KEY,"No Data")

            val type = genericType<List<RtWithAlbumArt>>()
            val onDiskRtaArtPathList = gson.fromJson<List<RtWithAlbumArt>>(jsonStrGet, type)
            Log.d(TAG, "getRtaArtPathList: onDiskRtaArtPathList = $onDiskRtaArtPathList")

            onDiskRtaArtPathList.toMutableList()
        }catch (e: Exception) {
            Log.d(TAG, "getRtaArtPathList: Error retrieving from Shared Prefs..error message=$e")
            arrayListOf<RtWithAlbumArt>() // 에러 발생시 빈 깡통 List 를 리턴.
        }

    }

    fun saveRtaArtPathList(rtaArtPathList: List<RtWithAlbumArt>) {
        val jsonStrSave = gson.toJson(rtaArtPathList)
        prefs.edit().putString(URI_STORED_KEY, jsonStrSave).apply()
    }

//// 아래 셋다 현재 사용 안되는 상태
//    fun savePlayInfo(playInfoObject: PlayInfoContainer) {
//        Log.d(TAG, "savePlayInfo: savePlayInfo. trId=${playInfoObject.trackID}")
//        val jsonStrSave = gson.toJson(playInfoObject)
//        prefs.edit().putString("CurrentPlayInfo", jsonStrSave).apply()
//
//    }
//    fun getPlayInfo(): PlayInfoContainer {
//        return try{
//            val jsonStrGet = prefs.getString("CurrentPlayInfo","No Data")
//            val playInfo = gson.fromJson(jsonStrGet, PlayInfoContainer::class.java)
//            //Log.d(TAG, "getPlayInfo: trId= ${playInfo.trackID}, songStatusMp= ${playInfo.songStatusMp}")
//            playInfo
//        }catch (e: Exception) {
//            Log.d(TAG, "getPlayInfo: Possibly no saved data yet..error message=$e")
//            PlayInfoContainer(-10,-10,-10,StatusMp.IDLE) // 에러 발생시 default 값 PlayInfo 를 return
//        }
//
//    }
//    fun calledFromActivity() {
//        Log.d(TAG, "calledFromActivity: called from AlarmsListActivity.")
//    }
}