package com.theglendales.alarm.jjmvvm.iapAndDnldManager

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.theglendales.alarm.jjdata.RtInTheCloud
import java.io.File


/**
 * 기존 구입했으나 유실된 건 복원 역할 (Recover Previously Purchased & Downloaded)
 */

private const val TAG="MultiDownloaderV3"
enum class MultiDnldState { IDLE, SUCCESSFUL, ERROR}

class MultiDownloaderV3(val context: Context) {

    private val _multiDnldLiveData = MutableLiveData<MultiDnldState>() // Private& Mutable LiveData
    val multiDnldLiveData: LiveData<MultiDnldState> = _multiDnldLiveData // Public & Immutable LiveData

    var currentStateObj = MultiDnldState.IDLE // IDLE 로 시작.


    suspend fun launchMultipleFileDNLD(multipleRtList: List<RtInTheCloud>): MultiDnldState {
        // 현재는 단순히 바로 다운로드 실행 -> Snackbar 로 "Recovering purchased items" 정도로만 뜸. 나중에 Single DNLD 처럼 Prgrs BtmSheet 쓸지 고민.."
        Log.d(TAG, "launchMultipleFileDNLD: [멀티] 파일 복원 필요 갯수:${multipleRtList.size}, Received list=$multipleRtList")
        var isErrorOccurred = false

        for(i in multipleRtList.indices)
        {
            val trackID = multipleRtList[i].id
            val iapName = multipleRtList[i].iapName
            val trTitle = multipleRtList[i].title
            val fileNameAndFullPath = context.getExternalFilesDir(null)!!.absolutePath + "/.AlarmRingTones" + File.separator + iapName +".rta" // rta= Ring Tone Audio 내가 만든 확장자..
            val mp3UrlToDownload= multipleRtList[i].mp3URL // 테스트 때 SoundHelix 16,15,14,12,11,9 링크 사용했음.

            //Download Prep
            try {
                //throw Exception("sssiii multi") -> 밑에서 catch 잘하고 SecondFrag 에서도 snackbar 메시지 출력 잘됨.
                Log.d(TAG,"launchMultipleFileDNLD: %%%%%%%%%%% WE'll FINALLY DOWNLOAD THIS trId=$trackID %%%%%%%%%%%")
                Log.d(TAG,"launchMultipleFileDNLD: %%%%%%%%%%% WE'll FINALLY DOWNLOAD THIS URL= $mp3UrlToDownload %%%%%%%%%%%")
                val dnlRequest = DownloadManager.Request(Uri.parse(mp3UrlToDownload))
                // 다운로드 방식 두가지 다 .. allowed
                dnlRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                dnlRequest.setTitle("Download")
                dnlRequest.setDescription("Downloading Your Purchased Ringtone...")
                dnlRequest.allowScanningByMediaScanner() // Starting in Q, this value is ignored. Files downloaded to directories owned by applications (e.g. Context.getExternalFilesDir(String)) will not be scanned by MediaScanner and the rest will be scanned.
                dnlRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) // 다운 중에만 표시
                dnlRequest.setDestinationInExternalFilesDir(context.applicationContext,".AlarmRingTones","$iapName.rta") // * 경로+ File 이름 설정.

                val dnlManager =context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val downloadID = dnlManager.enqueue(dnlRequest) // 이 코드는 두가지 역할을 함.
            } catch (e: Exception) {
                isErrorOccurred = true
                Log.d(TAG, "launchMultipleFileDNLD: error trying to retrieve previously purchased. \nError=$e")
            }
        }//end of For loop
        // 멀티 다운로드 시도 과정 report -> Snackbar 로 바로 "복원 시작" 및 에러여부 Display.
        return when(isErrorOccurred) {
            // 이 method 자체가 JjMainViewModel 에서 Background Thread 에서 불렸으나 -> SnackBar 결과 표시는 SecondFrag - Main Thread 에서 해줘야됨.
            true -> {
                currentStateObj = MultiDnldState.ERROR
                currentStateObj
            }
            false -> {
                currentStateObj = MultiDnldState.SUCCESSFUL
                currentStateObj
            }
        }
       /* when(isErrorOccurred) {
            // 이 method 자체가 JjMainViewModel 에서 Background Thread 에서 불렸으나 -> SnackBar 결과 표시는 SecondFrag - Main Thread 에서 해줘야됨.
            true -> {
                currentStateObj = MultiDnldState.ERROR
                updateLiveDataOnMainThread(currentStateObj)}
            false -> {
                currentStateObj = MultiDnldState.SUCCESSFUL
                updateLiveDataOnMainThread(currentStateObj)}
        }*/

         // 결과 라이브데이터에 입력-> postValue: 현재 우리는 IO 쓰레드에 있지만 (다 끝나고?) MainThread 로 전달 -> 자동으로 SecondFrag 에서 확인 (observe 중였으니깐)
    }
    fun getMultiDnldState(): LiveData<MultiDnldState> = multiDnldLiveData
    fun resetCurrentStateToIdle() { // 요걸 해줘야 listFrag 갔다왔을때 지랄 안 남.
        currentStateObj = MultiDnldState.IDLE
        updateLiveDataEnum(currentStateObj)
    }
// ** Utility Method **
    fun updateLiveDataEnum(currentStateObj: MultiDnldState) {
        _multiDnldLiveData.value = currentStateObj // 즉각 Main Thread 에서 반영!
    }

}
