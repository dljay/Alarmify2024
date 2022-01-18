package com.theglendales.alarm.jjmvvm.iapAndDnldManager

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.util.RtOnThePhone
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import junit.framework.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

private const val TAG="SingleDownloaderV3"

data class DNLDInfoContainer(var dnldTrTitle:String ="", var prgrs:Int=0, var status:Int= -1, var isBufferingToDNLD: Boolean = true) //Pending=1

class SingleDownloaderV3(val context: Context) {
    private val toastMessenger: ToastMessenger by globalInject() //ToastMessenger

    private val _dnldInfoLiveData = MutableLiveData<DNLDInfoContainer>() // Private& Mutable LiveData
    val dnldInfoLiveData: LiveData<DNLDInfoContainer> = _dnldInfoLiveData

    val dnldInfoObj= DNLDInfoContainer("", 0,-1, isBufferingToDNLD = true) //-> isRunning = true (SecondFrag 에서 observe 중) -> 바로 Dnld BtmSheet 보여줌!

//<1> 다운로드 시도
    suspend fun launchDNLD(rtInTheCloud: RtInTheCloud): Long {
        Log.d(TAG, "launchDNLD: %%%%%%%%%%% <1> DNLD Attempt Begins!! TrId= ${rtInTheCloud.id}, rtTitle=${rtInTheCloud.title}, rtClassObj=${rtInTheCloud}, ")
        //** 테스트 다운로드 URL 시도>>

        //A) LiveData 업뎃으로 다운로드 attempt 시작과 동시에-> [DNLD BTMSHEET 바로 열어주기]
        dnldInfoObj.dnldTrTitle = rtInTheCloud.title
        dnldInfoObj.prgrs = 0
        dnldInfoObj.status = 0 // status -> 0 -> SecondFrag -> BtmShtDNLDV2 show!
        dnldInfoObj.isBufferingToDNLD = true // -> 이게 true 인 동안은 Lottie 빙글빙글 애니메이션 + PrgrsBar(의 View= Gone 상태)
        updateLiveDataOnMainThread(dnldInfoObj)


        //B) 필요한 정보 추출.
        val fileNameWithoutExt = rtInTheCloud.iapName // ex) iapName= p1000, p1001 ...
        val mp3UrlToDownload= rtInTheCloud.mp3URL // 테스트 때 SoundHelix 16,15,14,12,11,9 링크 사용했음.

        //C) 실제 다운로드 실행 (추가로 위에있던 invalid Url 체크는 지워줬음. 아래 dnlManager.enqueue 에서 이미 다 해줘서 특별히 필요없는듯)
        //여기서 try{} catch{} 쓰면 이미 Exception 이 여기서 잡혀서 Parent 코루틴 Handler 로 전달 안됨(dnldParentJob) -> 모든 오류는 Coroutine Handler 에서 잡아주기

        val dnlRequest = DownloadManager.Request(Uri.parse(mp3UrlToDownload))
        // 다운로드 방식 두가지 다 .. allowed
        dnlRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        dnlRequest.setTitle("Download")
        dnlRequest.setDescription("Downloading Your Purchased Ringtone...")
        dnlRequest.allowScanningByMediaScanner() // Starting in Q, this value is ignored. Files downloaded to directories owned by applications (e.g. Context.getExternalFilesDir(String)) will not be scanned by MediaScanner and the rest will be scanned.
        dnlRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) // 다운 중에만 표시 todo: NEXUS 5X API30 에뮬레이터에서 표시 안됨.
        // 내 app 의 Main Storage>android/data/com.xx.downloadtest1/files/안에 .AlarmRingTones라는 폴더를 만들고! 그 안에 file 저장! hidden 상태임!
        dnlRequest.setDestinationInExternalFilesDir(context.applicationContext,".AlarmRingTones","$fileNameWithoutExt.rta") // * 경로+ File 이름 설정.
        //get download service, and enqueue file
        val dnlManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        // 다운로드 & 완료 알림(BroadCast) 관련: 1)식별 id 생성 2)Broadcast 등록.
        val downloadID = dnlManager.enqueue(dnlRequest) // 이 코드는 두가지 역할을 함. a)다운로드를 실행하고 b) Dnld Task Id 부여. enqueue - 다운로드 시작

        return downloadID
    }
//<2> <1> 에서 다운로드 진행 시도가 성공 -> 진행중인 다운로드 상태를 LiveData 에 업데이트.
    suspend fun watchDnldProgress(dnldId: Long, rtInTheCloud: RtInTheCloud){ // 현재 진행중인 DNLD 를 관찰하며 -> LiveData 에 상황을 전달 -> SecondFrag 에서 BTMSheet 업데이트.
        Log.d(TAG, "watchDnldProgress: %%%%%%%%% called. TrTitle=${rtInTheCloud.title}, dnldId=$dnldId")

    //A) *** rtInTheCloud -> rtOnThePhone 으로 변경 *** :
        val trackID = rtInTheCloud.id
        val fileNameWithoutExt = rtInTheCloud.iapName // ex) iapName= p1000, p1001 ...
        val trTitle = rtInTheCloud.title
        val description = rtInTheCloud.description
        val fileNameAndFullPath = context.getExternalFilesDir(null)!!.absolutePath + "/.AlarmRingTones" + File.separator + fileNameWithoutExt +".rta" // rta= Ring Tone Audio 내가 만든 확장자..
        val rtOnThePhone = RtOnThePhone(trIdStr = trackID.toString(), rtTitle = trTitle, audioFilePath = fileNameAndFullPath,
        fileNameWithoutExt = fileNameWithoutExt, rtDescription =description, badgeStr = "", isRadioBtnChecked = false)

    //B) 실제 다운로드 Prgrs/Status Watch & Report to LiveData
        val dnlManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var isStillDownloading=true
        var prevPrgrs: Int = 0 // progress 가 변했을때만 ViewModel 에 전달!
        var prevStatus = -77 // Status 가 변했을때만 ViewModel 에 전달!

        while(isStillDownloading)
        {
            val query = DownloadManager.Query()
            query.setFilterById(dnldId)
            val cursor = dnlManager.query(query)

            if(cursor.moveToFirst())
            {
                //throw Exception("SSSIBAL") //Error Test

                val bytesDownloadedSoFar = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val totalBytesToDNLD = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val currentPrgrs = ((bytesDownloadedSoFar * 100L)/totalBytesToDNLD).toInt() // changed from *100 to *99
                val currentStatus = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) // Status.xx 가 더 있음. 더 연구해볼것..

                //A) Download 가 98% 이상 진행되었다고 'DownloadManager' 가 전달했을 떄:
                if(currentPrgrs >= 98) { //위에서 제공받은 bytesDownloadedSoFar 에 의존
                    val currentTime= Calendar.getInstance().time
                    Log.d(TAG, "watchDnldProgress: (A) [다운로드 완료] @@@@@@myDownloadProgress=${currentPrgrs}, time:$currentTime")

                    dnldInfoObj.status = DownloadManager.STATUS_SUCCESSFUL // 8
                    dnldInfoObj.prgrs = currentPrgrs
                    updateLiveDataOnMainThread(dnldInfoObj)
                    break // Finish! escape from While Loop!
                }
                //B) Progress 가 변했을때만 ViewModel 에 전달
                if(prevPrgrs != currentPrgrs) {
                    dnldInfoObj.isBufferingToDNLD = false // 첫 Prgrs 를 받는 순간 Lottie 빙글빙글 Animation (View=Gone) + Linear Prgrs Bar(Visibility=Show)
                    prevPrgrs = currentPrgrs
                    Log.d(TAG, "watchDnldProgress: (B) (After) prevPrgrs=$prevPrgrs, currentPrgrs=$currentPrgrs")

                    dnldInfoObj.prgrs = currentPrgrs
                    updateLiveDataOnMainThread(dnldInfoObj)
                }
                //C) Status 가 처음 변했을 때만 ViewModel 에 전달 (While loop 에 의해 계속 currentStatus 를 체크하겠지만 값이 변했을 때만 VModel 에 반영 (ex. PENDING-> RUNNING ->SUCCESSFUL )
                if(prevStatus != currentStatus) {
                    prevStatus = currentStatus
                    Log.d(TAG, "watchDnldProgress: (C) (After) prevStatus=$prevStatus, currentStatus=$currentStatus")

                    when(currentStatus) {
                        DownloadManager.STATUS_PENDING -> {//1, 참고로 이거 제끼고 바로 running 으로 가는 경우도 많음. //todo: Timeout? https://stackoverflow.com/questions/28782311/timeout-for-android-downloadmanager
                            Log.d(TAG, "watchDnldProgress: (C-1) CurrentStatus=PENDING")
                            dnldInfoObj.status = currentStatus
                            updateLiveDataOnMainThread(dnldInfoObj) }

                        DownloadManager.STATUS_RUNNING -> {Log.d(TAG, "watchDnldProgress: (C-2) CurrentStatus=RUNNING")

                            dnldInfoObj.status = currentStatus
                            updateLiveDataOnMainThread(dnldInfoObj) } //2

                        DownloadManager.STATUS_PAUSED -> { //4
                            //B-1)** 다운이 다 됐음에도 STATUS_PAUSED 에서 10초 이상씩 머무는 경우->
                            val prgrsBasedOnActualDNLDSize = actualDnldSizeCheck(rtOnThePhone, totalBytesToDNLD) //-> 실제 폰에 '다운된 용량/다운받을 전체 파일 사이즈' 로 확인하여
                            if(prgrsBasedOnActualDNLDSize > 98) { // -> 98% 이상이면 무조건 SUCCESS 로 해주고 종료.
                                Log.d(TAG, "watchDnldProgress: (C-4) CurrentStatus=PAUSED 지만 다운이 다 된것으로 보임. Status->SUCCESSFUL 로 변경. \n prgrsBasedOnActualDNLDSize=$prgrsBasedOnActualDNLDSize")

                                dnldInfoObj.status = DownloadManager.STATUS_SUCCESSFUL
                                updateLiveDataOnMainThread(dnldInfoObj)
                                break // while loop 종료
                            }
                        }
                        // C-2) ** 다운 성공. 그런데 굉장한 Delay 가 있음. 실 다운로드가 끝나고도 10~15초 이상 걸린뒤 여기에 들어옴.
                        DownloadManager.STATUS_SUCCESSFUL -> { //8
                            dnldInfoObj.isBufferingToDNLD = false // 혹시나 (파일이 너무 작아서) progress 안 받고 바로 Success 되는 경우 대비.
                            Log.d(TAG, "watchDnldProgress: (C-8) CurrentStatus=SUCCESSFUL, Progress= $currentPrgrs, TRK ID=trkId=${rtOnThePhone.trIdStr}")

                            dnldInfoObj.status = DownloadManager.STATUS_SUCCESSFUL
                            updateLiveDataOnMainThread(dnldInfoObj)
                            break // while loop 종료
                        }
                        DownloadManager.STATUS_FAILED -> { //16
                            Log.d(TAG, "watchDnldProgress: (C-16) CurrentStatus=FAILED!XX ")

                            dnldInfoObj.status = DownloadManager.STATUS_FAILED
                            updateLiveDataOnMainThread(dnldInfoObj)
                            break // while loop 종료
                        }
                    }
                }
                cursor.close()
            }
        }
    }
//<3> <1> & <2> 실행 중 필요할때마다 라이브데이터 업데이트 -> SecondFrag 에서 반영 (진즉에 Observe 중)
    suspend fun updateLiveDataOnMainThread(dnldInfoObj: DNLDInfoContainer) {
        withContext(Dispatchers.Main) { // 잠시 Thread 를 IO -> Main 으로 변경 (UI 업뎃되기 때문에)
            _dnldInfoLiveData.value = dnldInfoObj
        }
    }
//<4> 위의 <1> & 2> 과정에서 에러가 발생했을 시 -> Coroutine Scope 에서 .invokeOnCompletion 에서 확인 후 아래를 실행 -> LiveDATA 업데이트
    fun errorWhileDownloading() {
    Log.d(TAG, "errorWhileDownloading: called")
        dnldInfoObj.status = -444 // 임의 숫자
        _dnldInfoLiveData.postValue(dnldInfoObj) // 진행중인 MainThread(DNLD 진행 상황 report) 가 다 끝나면 이 값을 실행 (근데 어차피 이게 불리는 곳이 .invokeOnCompletion 이니 상관없을듯.)

    }
//<5> DNLDInfo to Initial State -> 이건 다운로드  종료(혹은 error) 일때 설정 (.invokeOnCompletion) ==> ListFrag 갔다오거나 했을때 LiveData 자동 복구 되어도 SecondFrag 에서 확인 후 거를 수 있게끔!
    fun resetLiveDataToInitialState(){
        Log.d(TAG, "resetLiveDataToInitialState: called")
        dnldInfoObj.apply {
            dnldTrTitle=""
            prgrs = 0
            status = -1
        }
        _dnldInfoLiveData.postValue(dnldInfoObj)
    }

//** Utility Methods
//<6> LiveData 를 ViewModel 에서 받아갈 때:
    fun getMyDnldLiveData(): LiveData<DNLDInfoContainer> = dnldInfoLiveData
//<7> 현재 다운로드 받고 있는 파일이 실제 디스크에 얼만큼 다운받았는지 확인 ->  받아야할 사이즈로 나눠서 ->  prgrsBasedOnActualDNLDSize Return..
    fun actualDnldSizeCheck(rtOnThePhone: RtOnThePhone, totalBytesToDNLD: Int): Int {
        Log.d(TAG, "actualDnldSizeCheck: calledf")
        return if(!rtOnThePhone.audioFilePath.isNullOrEmpty() && File(rtOnThePhone.audioFilePath).exists()) {
            val currentDnloadingFile = File(rtOnThePhone.audioFilePath)
            val prgrsBasedOnActualDNLDSize = (currentDnloadingFile.length()*100L /totalBytesToDNLD).toInt()
            prgrsBasedOnActualDNLDSize

        } else {
            0
         }
    }



}