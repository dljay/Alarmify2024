package com.theglendales.alarm.jjmvvm.helper

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.util.RtOnThePhone
import java.lang.Exception

private const val TAG = "MySharedPrefManager"

//private const val TR_PLAY_INF_SHARED_PREF = "TrackPlayInfo"
//<1> RtOnThePhoneList.xml <RtOnThePhoneList>- RTA, ART PATH 저장 관련

private const val RT_ON_THE_PHONE_LIST ="RtOneThePhoneList" //현재 .rta 는 Uri 로 ..  .art 는 String path 로 저장
private const val KEY_1 = "RTOP_KEY"

//<2>
private const val RT_IN_THE_CLOUD_LIST ="RtInTheCloudList" //현재 .rta 는 Uri 로 ..  .art 는 String path 로 저장
private const val KEY_2 = "RTIC_KEY"

/*//<3> ListFrag ROW 에 사용될 ART PATH 관련
private const val ART_PATH_FOR_LIST_FRAG = "ArtPathForListFrag"
private const val KEY_2 = "Art_Key"*/

//<3> InAppPurchase 관련
private const val IAP_PREF_BOOL = "MyIAP" //MyIAP.xml
private const val IAP_PREF_URL = "MyIAPUrl" //MyIAPUrl.xml
private const val IAP_PREF_PRICE = "MyIAPPrice" //MyIAPUrl.xml


class MySharedPrefManager(val context: Context) {

    private val prefRtInTheCloud: SharedPreferences = context.getSharedPreferences(RT_IN_THE_CLOUD_LIST,Context.MODE_PRIVATE) // RtInTheCloudList.xml 파일 이름 (디스크에 저장된 rta, art 파일 uri 저장)
    private val prefRtOnThePhone: SharedPreferences = context.getSharedPreferences(RT_ON_THE_PHONE_LIST,Context.MODE_PRIVATE) // RtOneThePhoneList.xml 파일 이름 (디스크에 저장된 rta, art 파일 uri 저장)
    
    //private val prefForListFrag: SharedPreferences = context.getSharedPreferences(ART_PATH_FOR_LIST_FRAG,Context.MODE_PRIVATE) // ArtPathForListFrag.xml 파일 이름 (디스크에 저장된 rta, art 파일 uri 저장)
    //    val prefs: SharedPreferences = context.getSharedPreferences(TR_PLAY_INF_SHARED_PREF, Context.MODE_PRIVATE) //TrackPlayInfo = xml 파일 이름!!

    private val prefIapPurchaseBool: SharedPreferences = context.getSharedPreferences(IAP_PREF_BOOL, Context.MODE_PRIVATE) // MyIAP.xml
    private val prefIapUrl: SharedPreferences = context.getSharedPreferences(IAP_PREF_URL, Context.MODE_PRIVATE) // MyIAPUrl.xml
    private val prefIapPrice: SharedPreferences = context.getSharedPreferences(IAP_PREF_PRICE, Context.MODE_PRIVATE) // MyIAPPrice.xml

    //
    private val gson: Gson = Gson()

    inline fun <reified T> genericType() = object : TypeToken<T>() {}.type // todo: 이것이 무엇인지 inline 에 대해서 공부해봐야함.

    //<1> [RtOnThePhone List 저장] *.RTA 와 *.Art Path 가 저장된 Object 를 Shared Pref(RtOneThePhoneList.xml) 에 저장하기
    fun getRtOnThePhoneList(): MutableList<RtOnThePhone> {
        return try {
            val jsonStrGet = prefRtOnThePhone.getString(KEY_1, "No Data")

            val type = genericType<List<RtOnThePhone>>()
            val list = gson.fromJson<List<RtOnThePhone>>(jsonStrGet, type)
            Log.d(TAG, "getRtOnThePhoneList: list = $list")

            list.toMutableList()
        } catch (e: Exception) {
            Log.d(TAG, "getRtOnThePhoneList: Error retrieving from Shared Prefs..error message=$e")
            arrayListOf<RtOnThePhone>() // 에러 발생시 빈 깡통 List 를 리턴.
        }
    }

    fun saveRtOnThePhoneList(rtOnThePhoneList: List<RtOnThePhone>) {
        Log.d(TAG, "saveRtOnThePhoneList: begins..")
        val jsonStrSave = gson.toJson(rtOnThePhoneList)
        prefRtOnThePhone.edit().putString(KEY_1, jsonStrSave).apply()
        Log.d(TAG, "saveRtOnThePhoneList: done")
    }

    // <2> [RtInTheCloud] 저장 (JjMainViewModel 에서 Fb-Iap 정보 다 입력된 후 저장)
    fun getRtInTheCloudList(): MutableList<RtInTheCloud> {
        return try {
            val jsonStrGet = prefRtInTheCloud.getString(KEY_2, "No Data")

            val type = genericType<List<RtInTheCloud>>()
            val list = gson.fromJson<List<RtInTheCloud>>(jsonStrGet, type)
            Log.d(TAG, "getRtInTheCloudList: list = $list")
            list.toMutableList()
        } catch (e: Exception) {
            Log.d(TAG, "getRtOnThePhoneList: Error retrieving from Shared Prefs..error message=$e")
            arrayListOf<RtInTheCloud>() // 에러 발생시 빈 깡통 List 를 리턴.
        }
    }

    fun saveRtInTheCloudList(rtInTheCloudList: MutableList<RtInTheCloud>) {
        Log.d(TAG, "saveRtInTheCloudList: called.")
        val jsonStrSave = gson.toJson(rtInTheCloudList)
        prefRtInTheCloud.edit().putString(KEY_2, jsonStrSave).apply()
        Log.d(TAG, "saveRtInTheCloudList: done")

    }




// IAP 관련(IAPV2 용도. 다 삭제해버려.)
    // 1) 구매 여부 Bool - IapName / ex) (p1, true), (p2,false) ...
    fun getPurchaseBoolPerIapName(iapName: String) = prefIapPurchaseBool.getBoolean(iapName, false)

    fun savePurchaseBoolPerIapName(iapName: String, value: Boolean) {
        Log.d(TAG, "savePurchaseBoolPerIapName: called iapName=$iapName, bool=$value")
        prefIapPurchaseBool.edit().putBoolean(iapName, value).apply()
    }
    // 2) 구매하는 RT 의 URL 저장&Loading (추후 ListFrag 에서 파일 복귀시 사용)
    fun saveUrlPerIap(iapName: String, url: String) {
        prefIapUrl.edit().putString(iapName, url).apply() // todo: Firebase 에서 추후 Security 강화에서 아무나 접근 못하게..
    }
    fun getUrlByIap(iapName: String) = prefIapUrl.getString(iapName, "No Url Found")
    // 3) 물품의 가격 저장
    fun saveItemPricePerIap(iapName: String, price: String) {
        prefIapPrice.edit().putString(iapName, price).apply()
    }
    fun getItemPricePerIap(iapName: String) = prefIapPrice.getString(iapName,"No Price Found")

//Unused
//<2> List Fragment ROW 에서 보여질 ART PATH 용도: [Key,Value] = [AlarmId-Int, ArtPath-String].. AlarmDetailsFrag.kt 에서 알람 설정 후 [알람 id, artPath] 형태로 저장.
/*    fun saveArtPathForAlarm(alarmId: Int, artPath: String?) {
        Log.d(TAG, "saveArtPathForAlarm: alarmId=$alarmId, artPath=$artPath")
        prefForListFrag.edit().putString(alarmId.toString(), artPath).apply()
    }

    // 알람 id 로 art Path 받기
    fun getArtPathForAlarm(alarmId: Int): String {
        return prefForListFrag.getString(alarmId.toString(), null).toString() // 없으면 그냥 null 을 return.
    }

    // rta 제목으로 art Path 받기 (전달받은 스트링은 경로포함-> 여기서 링톤명(ex.defrt5) 추출해야함!)
    fun getArtPathFromRtaPath(fullRtaPath: String?): String {
        if (!fullRtaPath.isNullOrEmpty()) {
            val rtaFileName = fullRtaPath.substringAfter(".AlarmRingTones/").substringBefore(".rta")
            Log.d(TAG, "getArtPathFromRtaPath: rtaFileName=$rtaFileName")
            return rtaFileName
        } else {
            return "null rtaFileName..+_+"
        }

    }*/

    // 2) id / iapName / TrTitle 저장. (MyIAPHeler2.kt> refresh
//    fun getTrTitlePerTrId(trId: Int): String = prefForIAP.getString(trId.toString(), "No Value").toString()
//    fun saveTrTitlePerTrId(trId: Int, trTitle: String) { // (1,"Alaska Wind"), (2, "Elephant Cry") ..
//        prefForIAP.edit().putString(trId.toString(), trTitle).apply()
//    }
// <--- IAP 관련

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