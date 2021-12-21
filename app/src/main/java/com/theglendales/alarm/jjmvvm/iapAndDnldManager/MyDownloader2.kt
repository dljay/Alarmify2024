package com.theglendales.alarm.jjmvvm.iapAndDnldManager

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjmvvm.JjDNLDViewModel
import com.theglendales.alarm.jjmvvm.mediaplayer.MyMediaPlayer

import com.theglendales.alarm.jjmvvm.permissionAndDownload.MyPermissionHandler
import com.theglendales.alarm.jjmvvm.unused.BtmSht_SingleDNLD
import com.theglendales.alarm.jjmvvm.unused.BtmSht_Sync
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjmvvm.util.RtWithAlbumArt
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

private const val TAG="MyDownloader2"




data class DownloadableItem(val trackID: Int=0, val filePathAndName:String="") {
    override fun toString(): String
    {
        return "DownloadItemClass: trackId=$trackID, fileName=$filePathAndName"
    }
}


class MyDownloader2 (private val receivedActivity: Activity, val dnldViewModel: JjDNLDViewModel) : AppCompatActivity() {

    companion object {
        var isSyncInProcess: Boolean = false // onResume()/onPause() 등에서 현재도 다운중인지 확인 위해 사용.
        var isSingleDNLDInProcess: Boolean = false

        var totalFilesToDNLD : Int = 0
        val btmShtSingleDNLDInstance= BtmSht_SingleDNLD() // 이놈 만큼은 object 가 아니고 class 임!
    }

    private val WRITE_PERMISSION_CODE: Int = 812 //Download 관련
    private val READ_PERMISSION_CODE: Int = 2480 // Read. 위에 download 가 되면(당연히 write 가 되므로) read permission 은 그냥 pass.
    private var downloadUrl: String =""
    private var receivedTrackId: Int = -10
    private var fileNameGlbVar =""

    private val myDiskSearcher: DiskSearcher by globalInject()

    val permHandler = MyPermissionHandler(receivedActivity)
    val btmShtMultiObj = BtmSht_Sync


    private val listToBeJudged= mutableListOf<DownloadableItem>()


    // <1-b> MyIapHelper.kt 에서 호출!로 시작-> Delete!

    // <2>
//********MULTIPLE File Dnld ---------------->>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    class DownloadInfoContainer(val dnldID: Long=0, val trkId: Int=0, val fileNameAndPath: String ="")

    fun multipleFileDNLD(needToSyncListReceived: MutableList<DownloadableItem>) {
        // 다수 파일에 대한 다운로드. 앱 최초 실행 시 그전 구입건이 2개 이상 있으면 무조건 일로 들어옴.
        Log.d(TAG, "multipleFileDNLD: starts")

        val dnldInfoContainerList = ArrayList<DownloadInfoContainer>() // 여기에 downloadId, trkId, fileNamePath 저장.

        openBtmShtMultiDNLD()
        //1) Download Request for multiple files
        for (i in 0 until needToSyncListReceived.size)
        {
            val fileNameAndFullPath = needToSyncListReceived[i].filePathAndName
            val trackID = needToSyncListReceived[i].trackID
            val fileNametoSaveOnDisk = File(fileNameAndFullPath).name
            val mp3UrlToDownload = MyMediaPlayer.mp3UrlMap[trackID]
            //URL Check
            val isUrlValid: Boolean = URLUtil.isValidUrl(mp3UrlToDownload)
            if(!isUrlValid) {
                Log.d(TAG, "multipleFileDNLD: DOWNLOAD ERROR. INVALID URL!")
                Toast.makeText(receivedActivity,"DOWNLOAD ERROR. INVALID URL!", Toast.LENGTH_LONG).show()
                return
            }
            Log.d(TAG, "multipleFileDNLD: %%%%%%%%%%% WE'll FINALLY DOWNLOAD THIS trId=$trackID %%%%%%%%%%%")
            Log.d(TAG, "multipleFileDNLD: %%%%%%%%%%% WE'll FINALLY DOWNLOAD THIS URL= $mp3UrlToDownload %%%%%%%%%%%")
            val dnlRequest = DownloadManager.Request(Uri.parse(mp3UrlToDownload))
            // 다운로드 방식 두가지 다 .. allowed
            dnlRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            dnlRequest.setTitle("Download")
            dnlRequest.setDescription("Downloading Your Purchased Ringtone...")
            dnlRequest.allowScanningByMediaScanner() // Starting in Q, this value is ignored. Files downloaded to directories owned
            dnlRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) // 다운 중에만 표시
            dnlRequest.setDestinationInExternalFilesDir(receivedActivity.applicationContext,".AlarmRingTones",fileNametoSaveOnDisk)

            //get download service, and enqueue file
            val dnlManager = receivedActivity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadID = dnlManager.enqueue(dnlRequest)
            val infoContainerInstance = DownloadInfoContainer(downloadID, trackID, fileNameAndFullPath)
            dnldInfoContainerList.add(infoContainerInstance)
            totalFilesToDNLD+= 1 // 이놈은 companion obj 으로 BCReceiver 에서 사용. (BcReceiver - btmSht 없애는 이중장치)

        } // end of for loop
        //2) Get Response using Coroutine
        var dnldCounter: Int = 0
        CoroutineScope(Dispatchers.IO).launch {
            dnldInfoContainerList.forEach { infoContainerObj ->
                launch {
                    isSyncInProcess = true // onResume/onFail 에서 여전히 다운로드 진행중인지 체크 위해..
                    val downloadResult = getDnldResultMulti(infoContainerObj) // 여기서 대기!! 다운로드 하나가 끝나면 이쪽으로 결과를 보고.
                    Log.d(TAG, "multipleFileDNLD: Coroutine-forEach. COMPLETED DNLDID=${infoContainerObj.dnldID}")
                    if(downloadResult) { // true 를 받으면=> 즉 하나가 끝났으면.
                        dnldCounter++
                    }
                    var totalProgress: Float = dnldCounter.toFloat()/dnldInfoContainerList.size.toFloat() * 100F

                    runOnUiThread { // 여기서 여러 파일의 다운로드 prgrs animation 진행!!

                        if(dnldCounter>0) { //하나라도 다운로드 완료되면-> Progress animation 시작
                            Log.d(TAG, "multipleFileDNLD: totalProgress=$totalProgress. dnldCounter.tofloat=${dnldCounter.toFloat()}, dnldInfoContList.size=${dnldInfoContainerList.size}")
                            btmShtMultiObj.animateLPI(totalProgress.toInt(),4000) // 애니메이션이 차는 속도 4초.
                        }
                        if(dnldCounter == dnldInfoContainerList.size) { // 100 퍼 만땅 animation 위해 그냥 넣음..
                            Log.d(TAG, "multipleFileDNLD: 100F Anim run!")
                            btmShtMultiObj.animateLPI(100,10) // 애니메이션이 차는 속도 0.01초.

                            btmShtMultiObj.lpiMulti.progress = 0 // 초기화. 이거 안하면 나중에 프로그레스바 anim 그래프 가운데서 시작하고 막 그러네
                        }

                    }
                    if(dnldCounter == dnldInfoContainerList.size) { // 전체 다운로드 완료!!
                        Log.d(TAG, "multipleFileDNLD: !!ALL DOWNLOADS/SYNC COMPLETED. dnldCounter=$dnldCounter")
                        delay(1000) // 그래프 만땅 보여주기 위해 1초 delay.
                        btmShtMultiObj.removeMultiDNLDBtmSheet()
                        // progress 값 초기화. 앱 진행중에 새로고침했을때 대비.
                        totalProgress = 0F
                        isSyncInProcess = false

                    }
                }
            }//
        }

    }
    private fun getDnldResultMulti(infoContainer: DownloadInfoContainer): Boolean { // 한개가 끝날때마다 bool return.
        val dnlManager = receivedActivity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var isStillDownloading = true
        //multi- 다운로드 Animation 관련
        var animInitialBool = false // 시작하자마자 20~40 % 까지(random) 는 무조건 animation 실행.
        var animFortyNSixtyBool = false // progress 가 40~60 사이일 때 animation
        var animSixtyEightyBool = false // 60~80 구간
        var animEightyOrHigher = false // 80 이상

        var statusRunningCount =  0 // logd 미친듯이 뜨는것 막기 위해..
        var statusPauseCount = 0

        while (isStillDownloading)
        {
            val query = DownloadManager.Query()
            query.setFilterById(infoContainer.dnldID)
            val cursor = dnlManager.query(query)



            //1) status 로 다운 완료 여부 파악
            if(cursor.moveToFirst()){
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))

                when(status) {
                    DownloadManager.STATUS_PENDING -> {
                        Log.d(TAG, "showDNLDProgress: STATUS PENDING  DNLDId=${infoContainer.dnldID}")
                    }
                    DownloadManager.STATUS_PAUSED -> {
                        statusPauseCount++
                        if(statusPauseCount==1) {
                            Log.d(TAG, "showDNLDProgress: STATUS PAUSED  <1st!/ trkId=${infoContainer.trkId}")
                        }
                        if(statusPauseCount == 400) {
                            Log.d(TAG, "showDNLDProgress: STATUS PAUSED  <400th>/ trkId=${infoContainer.trkId}")
                            statusPauseCount = 0
                        }

                    }
                    DownloadManager.STATUS_RUNNING -> {
                        statusRunningCount++
                        if(statusRunningCount == 1) {
                            Log.d(TAG, "showDNLDProgress: STATUS RUNNING <1st!>/ trkId=${infoContainer.trkId}")
                        }
                        if(statusRunningCount == 400) {
                            Log.d(TAG, "showDNLDProgress: STATUS RUNNING <400th>/ trkId=${infoContainer.trkId}")
                            statusRunningCount = 0
                        }

                    }
                    DownloadManager.STATUS_FAILED -> {
                        Log.d(TAG, "showDNLDProgress: STATUS FAILED / DNLDId=${infoContainer.dnldID}")
                        Toast.makeText(receivedActivity,"Failed to Download. Please Check your Internet connection.",
                            Toast.LENGTH_LONG).show()
                        btmShtMultiObj.removeMultiDNLDBtmSheet()
                        isSyncInProcess = false
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        Log.d(TAG, "showDNLDProgress: STATUS SUCCESSFUL, trkId= ${infoContainer.trkId}, DNLDId=${infoContainer.dnldID}")
                        //btmShtSingleDNLDInstance.removeSingleDNLDBtmSheet() // 아래 while loop 밖에도 있지만. 혹시 모르니 여기서도 btm Sheet 닫기 넣어줌.
                        isStillDownloading = false
                    }
                }
                //2) 파일 사이즈 크기로 다운 완료 여부 파악
                val bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val myDownloadProgress = ((bytesDownloaded * 100L)/bytesTotal).toInt() // changed from *100 to *99

                val fileForSizeCheck = File(infoContainer.fileNameAndPath)
                if(fileForSizeCheck.length().toInt() == bytesTotal) { // STATUS SUCCESSFUL 이 늦게 뜰때는 여기에 의존!
                    // 다운받은 파일크기 == 전체 파일 크기 <- 이건 다운 완료 후 10초내로 응답하는 듯..나름 reliable. BcReceiver 로 전달되는것보다 30초~1분정도 빠름.
                    val currentTime= Calendar.getInstance().time
                    Log.d(TAG, "getResultFromMULTIDNLD: @@@@@@fileSizeCheck.length=${fileForSizeCheck.length()}, name=${fileForSizeCheck.name}, time:$currentTime")
                    isStillDownloading = false
                }
                //runOnUiThread {} // 여기서 anim 하고 isAnimRunning 했을 때 objAnim 이 lateInit 후 초기 화 안된 문제 발생.
                cursor.close()
            }
        }
        return true
    }

// <----------***MULTIPLE File Dnld <<<<<<<<<<<<----------------

//********Single File Dnld **************
    fun singleFileDNLD(itemToDownload: DownloadableItem) {
        Log.d(TAG, "singleFileDNLD: Begins. ItemToDownload=$itemToDownload")
    // 일단 Permission Check
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // API ~28 이하인 경우에는 Permission Check. API 29 이상에서는 DONWLOAD 에 특별한 Permission 필요 없음.
//            permHandler.permissionForSingleDNLD(itemToDownload) // 필요없는듯. 에러도 안남.. API25 에서 잘됨.
//        }


        val fileNameAndFullPath = itemToDownload.filePathAndName
        val trackID = itemToDownload.trackID
        val fileName = File(fileNameAndFullPath).name
        val testXXTitle = "TestTitle"

    // RtWithAlbumArtObj 으로 변환 후 일단 ViewModel 에 전달 -> SecondFrag 에서 DNLD BottomFrag UI 준비 //todo: 여기서 제대로..
        val dnldAsRtObj = RtWithAlbumArt(trIdStr = trackID.toString(), rtTitle = testXXTitle, audioFilePath = fileNameAndFullPath,
                        fileName = fileName, rtDescription ="", badgeStr = "", isRadioBtnChecked = false)
        dnldViewModel.updateDNLDRtObj(dnldAsRtObj)


    //download URL // todo: URL 변경

        val mp3UrlToDownload="https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3" // 16,15,14,12,11 링크 사용했음.
    // 우선 URL valid 한지 체크
        val isUrlValid: Boolean = URLUtil.isValidUrl(mp3UrlToDownload)
        if(!isUrlValid) {
            Log.d(TAG, "singleFileDNLD: DOWNLOAD ERROR. INVALID URL!")
            Toast.makeText(receivedActivity,"DOWNLOAD ERROR. INVALID URL!", Toast.LENGTH_LONG).show()
            return
        }
    //Donwload Prep
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
        dnlRequest.setDestinationInExternalFilesDir(receivedActivity.applicationContext,".AlarmRingTones",fileName) // * 경로설정.
        //get download service, and enqueue file
        val dnlManager = receivedActivity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        // 다운로드 & 완료 알림(BroadCast) 관련: 1)식별 id 생성 2)Broadcast 등록.
        val downloadID = dnlManager.enqueue(dnlRequest) // 이 코드는 두가지 역할을 함.
        //1) Enqueue a new download. The download will start automatically once the download manager is ready to execute it and connectivity is available.
        // 2)추후 다운로드 완료후-> broadcast 받을것이고-> 여기서 만든 id(LONG) 로 판독 예정!

    // Download Progress -> ViewModel (LiveData) -> SecondFrag 로 전달됨.
        CoroutineScope(Dispatchers.IO).launch {
            isSingleDNLDInProcess = true

            // 아래 getResult..() 에서 다운로드 중일때는 계속 Loop 돌다가. 다운이 끝나면 False or Success Status (INT) 를 보냄.
            val dnldResult = getResultFromSingleDNLD(downloadID, dnldAsRtObj)
            // 아래 getResult..() 여기서 다 LiveData->SecondFrag 로 콜백해주지만. 혹시 몰라서 넣어놓음.
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
        Log.d(TAG, "singleFileDNLD: DLND ID= $downloadID, trackId=$trackID")

    }

    private fun getResultFromSingleDNLD(dnldID: Long, dnldAsRtObj: RtWithAlbumArt): Int  {
        val dnlManager = receivedActivity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        var isStillDownloading = true
        var resultCode = -44 //Default 로 -44 고 다운 성공시(8) 실패시(16) 을 return
        /*var animInitialBool = false // 시작하자마자 20~40 % 까지(random) 는 무조건 animation 실행.
        var animFortyNSixtyBool = false // progress 가 40~60 사이일 때 animation
        var animSixtyEightyBool = false // 60~80 구간
        var animEightyOrHigher = false // 80 이상*/
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
                    Log.d(TAG, "getResultFromSingleDNLD: (Before) prevValue=$prevPrgrsValue, myDownloadProgress=$myDownloadProgress")
                    prevPrgrsValue = myDownloadProgress
                    Log.d(TAG, "getResultFromSingleDNLD: (After) prevValue=$prevPrgrsValue, myDownloadProgress=$myDownloadProgress")

                    dnldViewModel.updateDNLDProgressLive(prevPrgrsValue)
                }


                // 다운로드 성공으로 완료(.STATUS_SUCCESSFUL) 후 보고까지 시간차가 나는 경우가 종종 있음. 그래서 파일 사이즈 체크를 실행하여 btmSht 없애주도록 해봄.

                // bytesWrittenOnPhone: 파일 사이즈 체크 (다운로드 중) 폰에 Write 되고 있는 실물 파일 사이즈 체크  (사실상 bytesDownloadedSoFar 과 일치해야함.)
              //  val bytesWrittenOnPhone = File(filePathAndName) // 이거 다운 시작하자마자도 다운받아야할 전체 사이즈 뜨고 그래서 그냥 안 쓰기로..

                if(myDownloadProgress >= 98) { //todo: 간혹 다운 다 됐음에도 Status.Pause 에 한참 머무는 경우가 있어서. 미리 이걸로 체크. 다운 안됐는데 뜰수도 있음. Firebase 링크에서는 안될수도..
                    //todo: Double Check! 다운 진짜 완료됐는지!! 디스크에 있는 파일사이즈 체크. 이거 예전에 넣어놓은 이유가 다 있겠징.
                    val currentTime= Calendar.getInstance().time
                    Log.d(TAG, "getResultFromSingleDNLD: [다운로드 완료] @@@@@@myDownloadProgress=${myDownloadProgress}, time:$currentTime")
                    isStillDownloading = false // 이제 While loop 에서 벗어나자!
                    resultCode = DownloadManager.STATUS_SUCCESSFUL // 8
                    dnldViewModel.updateDNLDStatusLive(DownloadManager.STATUS_SUCCESSFUL)
                    break // escape from While Loop!
                }

                val statusInt = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) // Status.xx 가 더 있음. 더 연구해볼것..
            // Status 가 변했을때만 ViewModel 에 전달.
                if(prevStatusValue != statusInt) {
                    Log.d(TAG, "getResultFromSingleDNLD: (Before) prevStatusValue=$prevStatusValue, statusInt=$statusInt")
                    prevStatusValue = statusInt
                    Log.d(TAG, "getResultFromSingleDNLD: (After) prevStatusValue=$prevStatusValue, statusInt=$statusInt")

                    dnldViewModel.updateDNLDStatusLive(statusInt)
                }


                when(statusInt) {

                    DownloadManager.STATUS_PENDING -> {} //1, 참고로 이거 제끼고 바로 running 으로 가는 경우도 많음.
                    DownloadManager.STATUS_RUNNING -> {} //2
                    DownloadManager.STATUS_PAUSED -> {} // 4. RUNNING 으로 다운이 다 된 다음에도 PAUSED 에 한참 들어와있다가-> STATUS_SUCCESSFUL 로 이동.
                    DownloadManager.STATUS_FAILED -> { //16
                        Log.d(TAG, "getResultFromSingleDNLD: STATUS FAILED / trkId=${dnldAsRtObj.trIdStr}")
                        Toast.makeText(receivedActivity,"Download Failed. Please check your internet connection.",Toast.LENGTH_LONG).show()
                        //resultCode = DownloadManager.STATUS_FAILED // 16 <- 불필요
                        dnldViewModel.updateDNLDStatusLive(16)
                        isStillDownloading = false
                        break
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> { //8.  ** 굉장한 Delay 가 있음. 실 다운로드가 끝나고도 10~15초 이상 걸린뒤 여기에 들어옴.
                        Log.d(TAG, "getResultFromSingleDNLD: STATUS SUCCESSFUL, Progress= $myDownloadProgress, TRK ID=trkId=${dnldAsRtObj.trIdStr}")
                        dnldViewModel.updateDNLDStatusLive(8)
                        //resultCode = DownloadManager.STATUS_SUCCESSFUL // 8  <- 불필요
                        isStillDownloading = false
                        break
                    }
                }
                cursor.close()
            }

        } // whileLoop  여기까지.
        return resultCode // downloading 이 끝나면 bool 값은 false 로 되어있음.
    }

    //*********Bottom Sheets ***********
    fun openBtmShtMultiDNLD() { // 다운로드와 동시에 BtmSheet_Sync 호출.

        val fm = btmShtMultiObj.fragmentManager
        if (fm == null) {
            Log.d(TAG, "openBtmShtMultiDNLD: fm is null==bottomSheet NOT(XX) displayed or prepared!!!!")
        }
        if (fm != null) { // BottomSheet 이 display 된 상태.
            Log.d(TAG, "openBtmShtMultiDNLD: fm not null..something is(OO) displayed already")

            fm.beginTransaction()
            fm.executePendingTransactions()
        }
        if (!btmShtMultiObj.isAdded) {//아무것도 display 안된 상태.
            btmShtMultiObj.showBtmSyncDialog(receivedActivity)
            Log.d(TAG, "openBtmShtMultiDNLD: ***DISPLAY BOTTOM SHEET_MULTI! NOW!! .isAdded= FALSE!!..")
        }
    }

    fun openBtmShtSingleDNLD() { // 다운로드와 동시에 BtmSheet_Sync 호출.

        val fm = btmShtSingleDNLDInstance.fragmentManager
        if(fm==null) {
            Log.d(TAG, "openBtmShtSingleDNLD: fm is null==bottomSheet NOT(XX) displayed or prepared!!!!")
        }
        if(fm!=null) { // BottomSheet 이 display 된 상태.
            Log.d(TAG, "openBtmShtSingleDNLD: fm not null..something is(OO) displayed already")

            fm.beginTransaction()
            fm.executePendingTransactions()
            //fm 에서의 onCreateView/Dialog() 작용은 Asynchronous 기 때문에. <-요기 executePending() 을 통해서 다 실행(?)한 후.에.야 밑에 .isAdded 에 걸림.
        }

        if(!btmShtSingleDNLDInstance.isAdded) {//아무것도 display 안된 상태.
            btmShtSingleDNLDInstance.showBtmSingleDNLDSheet(receivedActivity)
            Log.d(TAG, "openBtmShtSync: ***DISPLAY BOTTOM SHEET NOW!! .isAdded= FALSE!!..")
        }
    }

}

