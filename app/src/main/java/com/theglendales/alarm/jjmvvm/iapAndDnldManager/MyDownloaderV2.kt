package com.theglendales.alarm.jjmvvm.iapAndDnldManager

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.JjDNLDViewModel
import com.theglendales.alarm.jjmvvm.JjMainViewModel

import com.theglendales.alarm.jjmvvm.util.RtOnThePhone
import kotlinx.coroutines.*
import java.io.File
import java.util.*

private const val TAG="MyDownloaderV2"



//todo: 이건 예전 MyIAPHler_v1, MyDownloader_v1 등에서 쓰임. 현재는 v2 쓰므로 지워도 된다. + MyPermissionHanlder 고쳐주기.
data class DownloadableItem(val trackID: Int=0, val filePathAndName:String="") {
    override fun toString(): String
    {
        return "DownloadItemClass: trackId=$trackID, fileName=$filePathAndName"
    }
}


class MyDownloaderV2 (private val receivedActivity: Activity, val dnldViewModel: JjDNLDViewModel) : AppCompatActivity() {



//********Single File Dnld **************
    fun singleFileDNLD(rtClassObj: RtInTheCloud) {
        Log.d(TAG, "singleFileDNLD: Begins. TrId= ${rtClassObj.id}, rtTitle=${rtClassObj.title}, rtClassObj=${rtClassObj}, ")
    // 일단 Permission Check
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // API ~28 이하인 경우에는 Permission Check. API 29 이상에서는 DONWLOAD 에 특별한 Permission 필요 없음.
//            permHandler.permissionForSingleDNLD(itemToDownload) // 필요없는듯. 에러도 안남.. API25 에서 잘됨.
//        }

        val trackID = rtClassObj.id
        val fileNameWithoutExt = rtClassObj.iapName // ex) iapName= p1000, p1001 ...
        val trTitle = rtClassObj.title
        val description = rtClassObj.description
        val fileNameAndFullPath = receivedActivity.getExternalFilesDir(null)!!.absolutePath + "/.AlarmRingTones" + File.separator + fileNameWithoutExt +".rta" // rta= Ring Tone Audio 내가 만든 확장자..
        val mp3UrlToDownload= rtClassObj.mp3URL // 테스트 때 SoundHelix 16,15,14,12,11,9 링크 사용했음.

    // RtWithAlbumArtObj 으로 변환 후 일단 ViewModel 에 전달 -> SecondFrag 에서 DNLD BottomFrag UI 준비 //todo: 여기서 제대로..
        val dnldAsRtObj = RtOnThePhone(trIdStr = trackID.toString(), rtTitle = trTitle, audioFilePath = fileNameAndFullPath,
                        fileNameWithoutExt = fileNameWithoutExt, rtDescription =description, badgeStr = "", isRadioBtnChecked = false)



    // 우선 URL valid 한지 체크
        val isUrlValid: Boolean = URLUtil.isValidUrl(mp3UrlToDownload)
        if(!isUrlValid) {
            Log.d(TAG, "singleFileDNLD: DOWNLOAD ERROR. INVALID URL!")
            Toast.makeText(receivedActivity,"DOWNLOAD ERROR. INVALID URL!", Toast.LENGTH_LONG).show()
            return
        }
    //Donwload Prep
    try {
        Log.d(TAG, "singleFileDNLD: %%%%%%%%%%% WE'll FINALLY DOWNLOAD THIS trId=$trackID %%%%%%%%%%%")
        Log.d(TAG, "singleFileDNLD: %%%%%%%%%%% WE'll FINALLY DOWNLOAD THIS URL= $mp3UrlToDownload %%%%%%%%%%%")
        val dnlRequest = DownloadManager.Request(Uri.parse(mp3UrlToDownload))
        // 다운로드 방식 두가지 다 .. allowed
        dnlRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        dnlRequest.setTitle("Download")
        dnlRequest.setDescription("Downloading Your Purchased Ringtone...")
        dnlRequest.allowScanningByMediaScanner() // Starting in Q, this value is ignored. Files downloaded to directories owned by applications (e.g. Context.getExternalFilesDir(String)) will not be scanned by MediaScanner and the rest will be scanned.

        dnlRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) // 다운 중에만 표시 todo: NEXUS 5X API30 에뮬레이터에서 표시 안됨.
        // 내 app 의 Main Storage>android/data/com.xx.downloadtest1/files/안에 .AlarmRingTones라는 폴더를 만들고! 그 안에 file 저장! hidden 상태임!
        dnlRequest.setDestinationInExternalFilesDir(receivedActivity.applicationContext,".AlarmRingTones","$fileNameWithoutExt.rta") // * 경로+ File 이름 설정.
        //get download service, and enqueue file
        val dnlManager = receivedActivity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        // 다운로드 & 완료 알림(BroadCast) 관련: 1)식별 id 생성 2)Broadcast 등록.
        val downloadID = dnlManager.enqueue(dnlRequest) // 이 코드는 두가지 역할을 함.
        //1) Enqueue a new download. The download will start automatically once the download manager is ready to execute it and connectivity is available.
        // 2)추후 다운로드 완료후-> broadcast 받을것이고-> 여기서 만든 id(LONG) 로 판독 예정!
        runOnUiThread {dnldViewModel.updateDNLDRtObj(dnldAsRtObj) }



        // BackgroundThread (Dispatchers.IO) 에서 안해주면 UI 가 멈춤 (Progress 보여줄 수 없다..)
        CoroutineScope(Dispatchers.IO).launch {

            //isSingleDNLDInProcess = true
            // 아래 getResult..() 에서 다운로드 중일때는 계속 Loop 돌다가. 다운이 끝나면 False or Success Status (INT) 를 보냄.
            // **Download Progress -> ViewModel (LiveData) -> SecondFrag 로 전달은 아래 reportResultFromSingleDNLD 로도 되지만. 혹시 몰라서 이중방패로 넣어줌.
            val dnldResult = getResultFromSingleDNLD(downloadID, dnldAsRtObj)

            withContext(Dispatchers.Main) {
                when(dnldResult) {
                    DownloadManager.STATUS_FAILED -> {
                        Log.d(TAG, "singleFileDNLD: Failed to Download -_- dnldResult=$dnldResult" )
                        dnldViewModel.updateDNLDStatusLive(DownloadManager.STATUS_FAILED)
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val currentTime= Calendar.getInstance().time
                        Log.d(TAG, "singleFileDNLD: Download result success! dnldResult=$dnldResult, current Time=$currentTime ")
                        dnldViewModel.updateDNLDStatusLive(DownloadManager.STATUS_SUCCESSFUL)
                    }
                }
            }
        }
    } // try Block{} 여기까지 <--
    catch (e: Exception) {
        Log.d(TAG, "singleFileDNLD: Failed to Download. Error=$e")
        //16,  에러 발생시 SecondFrag 에 알림 (BtmSheet 없애주기라도해야지..)
        runOnUiThread { dnldViewModel.updateDNLDStatusLive(DownloadManager.STATUS_FAILED)}

        Toast.makeText(receivedActivity,"Download Failed. Please check your internet connection.",Toast.LENGTH_LONG).show()
        }

    }

    private fun getResultFromSingleDNLD(dnldID: Long, dnldAsRtObj: RtOnThePhone): Int  {
        val dnlManager = receivedActivity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        var isStillDownloading = true
        var resultCode = -44 //Default 로 -44 고 다운 성공시(8) 실패시(16) 을 return
        var prevPrgrsValue: Int = -77 // progress 가 변했을때만 ViewModel 에 전달!
        var prevStatusValue = -77 // Status 가 변했을때만 ViewModel 에 전달!

//        var myDownloadProgress: Int = 0

        while (isStillDownloading)
        {
            val query = DownloadManager.Query()
            query.setFilterById(dnldID)
            val cursor = dnlManager.query(query)

            if(cursor.moveToFirst())
            {

                val bytesDownloadedSoFar = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val totalBytesToDNLD = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val myDownloadProgress = ((bytesDownloadedSoFar * 100L)/totalBytesToDNLD).toInt() // changed from *100 to *99

                // progress 가 변했을때만 ViewModel 에 전달!
                if(prevPrgrsValue != myDownloadProgress) {
                    Log.d(TAG, "getResultFromSingleDNLD: (Before) prevPrgrsValue=$prevPrgrsValue, myDownloadProgress=$myDownloadProgress")
                    prevPrgrsValue = myDownloadProgress
                    Log.d(TAG, "getResultFromSingleDNLD: (After) prevPrgrsValue=$prevPrgrsValue, myDownloadProgress=$myDownloadProgress")
        // 여기서 한번 LiveData 때려주고!
                    runOnUiThread { dnldViewModel.updateDNLDProgressLive(prevPrgrsValue) }

                }
                // 다운로드 성공으로 완료(.STATUS_SUCCESSFUL) 후 보고까지 시간차가 나는 경우가 종종 있음. 그래서 파일 사이즈 체크를 실행하여 btmSht 없애주도록 해봄.
                // bytesWrittenOnPhone: 파일 사이즈 체크 (다운로드 중) 폰에 Write 되고 있는 실물 파일 사이즈 체크  (사실상 bytesDownloadedSoFar 과 일치해야함.)
              //  val bytesWrittenOnPhone = File(filePathAndName) // 이거 다운 시작하자마자도 다운받아야할 전체 사이즈 뜨고 그래서 그냥 안 쓰기로..
                if(myDownloadProgress >= 98) { //아래 다됐음에도 Status_Paused 되면 다운 종료하는것과 마찬가지 이중방패 (이건 위에서 제공받은 bytesDownloadedSoFar 에 의존)

                    val currentTime= Calendar.getInstance().time
                    Log.d(TAG, "getResultFromSingleDNLD: [다운로드 완료] @@@@@@myDownloadProgress=${myDownloadProgress}, time:$currentTime")
                    //isStillDownloading = false // 이제 While loop 에서 벗어나자! <- 불필요
                    resultCode = DownloadManager.STATUS_SUCCESSFUL // 8
                    runOnUiThread { dnldViewModel.updateDNLDStatusLive(DownloadManager.STATUS_SUCCESSFUL) }

                    break // escape from While Loop!
                }

                val statusInt = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) // Status.xx 가 더 있음. 더 연구해볼것..
            // Status 가 변했을때만 ViewModel 에 전달. !! 안 그러면 미친듯이 0.1 초에 한번씩 전달..
                if(prevStatusValue != statusInt) {
                    Log.d(TAG, "getResultFromSingleDNLD: (Before) prevStatusValue=$prevStatusValue, statusInt=$statusInt")
                    prevStatusValue = statusInt
                    Log.d(TAG, "getResultFromSingleDNLD: (After) prevStatusValue=$prevStatusValue, statusInt=$statusInt")
                    runOnUiThread { dnldViewModel.updateDNLDStatusLive(statusInt) }


                    //** 다운이 다 됐음에도 STATUS_PAUSED 에서 10초 이상씩 머무는 경우가 있음 -> 이럴때는 실제 폰에 '다운된 용량/다운받을 전체 파일 사이즈' 로 확인하여 98% 이상이면 무조건 SUCCESS 로 해주고 종료.
                    if(statusInt== DownloadManager.STATUS_PAUSED) {

                        if(!dnldAsRtObj.audioFilePath.isNullOrEmpty() && File(dnldAsRtObj.audioFilePath).exists()) {
                            val currentDnloadingFile = File(dnldAsRtObj.audioFilePath)
                            val prgrsBasedOnActualDNLDSize = (currentDnloadingFile.length()*100L /totalBytesToDNLD).toInt()
                            if(prgrsBasedOnActualDNLDSize > 98) {
                                Log.d(TAG, "getResultFromSingleDNLD: 다운로드 STATUS_PAUSED 지만 다운이 다 된것으로 보임. 다운로드 종료 예정 \n prgrsBasedOnActualDNLDSize=$prgrsBasedOnActualDNLDSize")
                                runOnUiThread { dnldViewModel.updateDNLDStatusLive(DownloadManager.STATUS_SUCCESSFUL) }
                                break
                            }
                        }
                    }
                }
                when(statusInt) {

                    DownloadManager.STATUS_PENDING -> {} //1, 참고로 이거 제끼고 바로 running 으로 가는 경우도 많음.
                    DownloadManager.STATUS_RUNNING -> {} //2
                    DownloadManager.STATUS_PAUSED -> {} // 4. RUNNING 으로 다운이 다 된 다음에도 PAUSED 에 한참 들어와있다가-> STATUS_SUCCESSFUL 로 이동.
                    DownloadManager.STATUS_FAILED -> { //16
                        Log.d(TAG, "getResultFromSingleDNLD: STATUS FAILED / trkId=${dnldAsRtObj.trIdStr}")
                        Toast.makeText(receivedActivity,"Download Failed. Please check your internet connection.",Toast.LENGTH_LONG).show()
                        //resultCode = DownloadManager.STATUS_FAILED // 16 <- 불필요
                        runOnUiThread { dnldViewModel.updateDNLDStatusLive(16) }

                        // isStillDownloading = false <- 불필요
                        break
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> { //8.  ** 굉장한 Delay 가 있음. 실 다운로드가 끝나고도 10~15초 이상 걸린뒤 여기에 들어옴.
                        Log.d(TAG, "getResultFromSingleDNLD: STATUS SUCCESSFUL, Progress= $myDownloadProgress, TRK ID=trkId=${dnldAsRtObj.trIdStr}")
                        runOnUiThread { dnldViewModel.updateDNLDStatusLive(8) }

                        //resultCode = DownloadManager.STATUS_SUCCESSFUL // 8  <- 불필요
                        //isStillDownloading = false <- 불필요
                        break
                    }
                }
                cursor.close()
            }
        } // whileLoop  여기까지.
        return resultCode // downloading 이 끝나면 bool 값은 false 로 되어있음.
    }
// ---------***MULTIPLE File Dnld ----------->>>>>
    fun multipleFileDNLD(multipleRtList: List<RtInTheCloud>) {
        // todo: 현재는 단순히 바로 다운로드 실행 -> Snackbar 로 "Recovering purchased items" 정도로만 뜸. 나중에 Single DNLD 처럼 Prgrs BtmSheet 쓸지 고민.."
        Log.d(TAG, "multipleFileDNLD: [멀티] 파일 복원 필요 갯수:${multipleRtList.size}, Received list=$multipleRtList")
        var isErrorOccurred = false

        for(i in multipleRtList.indices)
        {
            val trackID = multipleRtList[i].id
            val iapName = multipleRtList[i].iapName
            val trTitle = multipleRtList[i].title
            val fileNameAndFullPath = receivedActivity.getExternalFilesDir(null)!!.absolutePath + "/.AlarmRingTones" + File.separator + iapName +".rta" // rta= Ring Tone Audio 내가 만든 확장자..
            val mp3UrlToDownload= multipleRtList[i].mp3URL // 테스트 때 SoundHelix 16,15,14,12,11,9 링크 사용했음.

            // 우선 URL valid 한지 체크
            val isUrlValid: Boolean = URLUtil.isValidUrl(mp3UrlToDownload)
            if(!isUrlValid) {
                Log.d(TAG, "multipleFileDNLD: DOWNLOAD ERROR. INVALID URL!")
                isErrorOccurred = true
                // 테스트 기간에 iapName 이 p1 인 아이템을 샀지만, google play console 에 p1001 로 아이디를 바꾼 제품을 올려놓아서 더이상 찾을 수 없음. 계속 여기 걸림.
                continue // 리스트 안 객체에 잘못된 URL 이 있을 경우 아래 try/catch{} 다운로드 구간을 걸치지 않고 => 바로 다음 for loop iteration 으로 skip!
            }
            //Download Prep
            try {
                Log.d(TAG,"multipleFileDNLD: %%%%%%%%%%% WE'll FINALLY DOWNLOAD THIS trId=$trackID %%%%%%%%%%%")
                Log.d(TAG,"multipleFileDNLD: %%%%%%%%%%% WE'll FINALLY DOWNLOAD THIS URL= $mp3UrlToDownload %%%%%%%%%%%")
                val dnlRequest = DownloadManager.Request(Uri.parse(mp3UrlToDownload))
                // 다운로드 방식 두가지 다 .. allowed
                dnlRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                dnlRequest.setTitle("Download")
                dnlRequest.setDescription("Downloading Your Purchased Ringtone...")
                dnlRequest.allowScanningByMediaScanner() // Starting in Q, this value is ignored. Files downloaded to directories owned by applications (e.g. Context.getExternalFilesDir(String)) will not be scanned by MediaScanner and the rest will be scanned.

                dnlRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) // 다운 중에만 표시
                // 내 app 의 Main Storage>android/data/com.xx.downloadtest1/files/안에 .AlarmRingTones라는 폴더를 만들고! 그 안에 file 저장! hidden 상태임!
                dnlRequest.setDestinationInExternalFilesDir(receivedActivity.applicationContext,".AlarmRingTones","$iapName.rta") // * 경로+ File 이름 설정.
                //get download service, and enqueue file
                val dnlManager =receivedActivity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val downloadID = dnlManager.enqueue(dnlRequest) // 이 코드는 두가지 역할을 함.
            } catch (e: Exception) {
                isErrorOccurred = true
                Log.d(TAG, "multipleFileDNLD: error trying to retrieve previously purchased. \nError=$e")

            }
        }//end of For loop
        // 멀티 다운로드 시도 과정 report -> Snackbar 로 바로 "복원 시작" 및 에러여부 Display.
        val arrayBool: Array<Boolean> = arrayOf(true, isErrorOccurred) // true= 멀티다운로드를 가동했다!, isErrorOccurred= 다운과정에서 에러가 있냐 없냐!
        runOnUiThread { dnldViewModel.updateMultiDnldStats(arrayBool) }
    }
// <----------***MULTIPLE File Dnld <<<<<<<<<<<<----------------

}

