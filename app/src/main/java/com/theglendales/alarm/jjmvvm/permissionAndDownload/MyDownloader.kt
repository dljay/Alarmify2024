package com.theglendales.alarm.jjmvvm.permissionAndDownload

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.theglendales.alarm.jjmvvm.iap.MyIAPHelper
import com.theglendales.alarm.jjmvvm.mediaplayer.MyMediaPlayer
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

private const val TAG="MyDownloader"

data class DownloadableItem(val trackID: Int=0, val filePathAndName:String="") {
    override fun toString(): String
    {
        return "DownloadItemClass: trackId=$trackID, fileName=$filePathAndName"
    }
}

class MyDownloader(private val receivedActivity: Activity) : AppCompatActivity() {

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

    val permHandler = MyPermissionHandler(receivedActivity)
    val btmShtMultiObj = BtmSht_Sync




    private val listToBeJudged= mutableListOf<DownloadableItem>()


    // <1-a> MyIapHelper.kt 에서 호출!로 시작-> 여기서 B)정상 구매인지 C) 디스크에 파일이 있는지 확인, 2)없으면 Perm Check-> Download!
    fun multiDownloadOrNot(downloadableItem: DownloadableItem, keepTheFile: Boolean) {

        //A) MyIAPHelper.kt>downloadHandler() 에서 전달 받은 DownloadableItem 은 "심판 대상이므로" 일단 리스트에 add.
        listToBeJudged.add(downloadableItem)
        Log.d(TAG, "MultiDownloadOrNot: listToBeJudged.size=${listToBeJudged.size}, itemTobeJudged[TrkId]=${downloadableItem.trackID}")

        //B) 근데 구매건수(myQueryPurchases.size) 에는 포함되어 있지만 (어떤 이유로) "불량 구매"여서 디스크에서 삭제되어야 하는 경우. 삭제 실행.
        if(!keepTheFile) {
            Log.d(TAG, "MultiDownloadOrNot: XX trackId=${downloadableItem.trackID} (는)  구매한적이 없거나(어떤 연유로 불량 구매). 디스크에 있으면 삭제..")
            deleteFromDisk(downloadableItem)
        }
        //C) 계속 진행되다 여기서 전달받은 갯수가 드디어 myQryPurchListSize 와 같으면 (=queryPurchases.size) 다운받을지 말지 심판 시작!
        //C-1) 최초 실행시 Sync 작업: 기존 구매내역 확인 후 파일 다운받을지 말지 정하기.
        if(listToBeJudged.size == MyIAPHelper.myQryPurchListSize) // listTobeJudged(처리 전달 받은 아이템 숫자) = 구입 내역 목록 아이템 갯수
        //todo: 만약 이 size 가 일치하지 않는 경우 반드시 대비 필요!!할까..?
        {
            val finalList = mutableListOf<DownloadableItem>()
            for (i in 0 until listToBeJudged.size) // until 은 마지막 숫자 제외!! => i in 0 until 3 => 0,1,2 까지.
            {
                //Log.d(TAG, "downloadOrNotJudge: inside 'for loop!!'")
                val filePath =  listToBeJudged[i].filePathAndName
                val fileName = File(filePath).name

                if(checkDuplicatedFileOnDisk(filePath)) { // 파일이 이미 디스크에 있는 상태
                    //ultimateListToPass.remove(downloadableItem) // true if the element has been successfully removed; false if it was not present in the collection.
                    Log.d(TAG, "MultiDownloadOrNot: [i]=$i, listToBeJudged.size=${listToBeJudged.size} // OO: $fileName 가(이) 이미 디스크에 있어서 다운 필요 없음. 경로: '$filePath")
                }
                else if(!checkDuplicatedFileOnDisk(filePath)) {

                    finalList.add(listToBeJudged[i])
                    Log.d(TAG, "MultiDownloadOrNot: @@ finalList.size=${finalList.size} // @@ XX: $fileName (은) 디스크에 없어서 다음 파일을 다운받아야함=$filePath")
                }
            }
            //D) 최종으로 for loop 이 끝나고 정리된 리스트를 전달!
            // 이제 기존/현재의 구매건(들)에 대한 정리가 끝났을테니.초기화.
            MyIAPHelper.myQryPurchListSize = 0
            listToBeJudged.clear()
            // Permission 요청: Write Permission 체크 진행 ->그리고 여기서 바로 다운로드로 진행..
            Log.d(TAG, "MultiDownloadOrNot: ##for loop 종료! finalList.size=${finalList.size}")
            permHandler.permissionToWrite(finalList) //

        }
        //C-2) 최초 Sync 작업 수행할게 없거나(끝났으면) myQryPurchListSize=0 (클릭해서 한개 구매할 때 일로 옴)

    }
    fun singleDownloadOrNot(downloadableItem: DownloadableItem, keepTheFile: Boolean) {

        Log.d(TAG, "SingleDownloadOrNot: listToBeJudged.size=${listToBeJudged.size}, itemTobeJudged[TrkId]=${downloadableItem.trackID}")

        if(!keepTheFile) {
            Log.d(TAG, "SingleDownloadOrNot: XX trackId=${downloadableItem.trackID} (는)  구매한적이 없거나(어떤 연유로 불량 구매). 디스크에 있으면 삭제..")
            deleteFromDisk(downloadableItem)
            // todo : add return?
        }
        val finalList = mutableListOf<DownloadableItem>()
        val filePath =  downloadableItem.filePathAndName
        val fileName = File(filePath).name

        if(checkDuplicatedFileOnDisk(filePath)) { // 파일이 이미 디스크에 있는 상태
            Log.d(TAG, "SingleDownloadOrNot:  $fileName 가(이) 이미 디스크에 있어서 다운 필요 없음 XX. 경로: '$filePath")
        }
        else if(!checkDuplicatedFileOnDisk(filePath)) {
            finalList.add(downloadableItem)
            Log.d(TAG, "SingleDownloadOrNot:  $fileName (은) 디스크에 없어서 다음 파일을 다운받아야함 OO filepath=$filePath")
        }

        // Permission 요청: Write Permission 체크 진행 ->그리고 여기서 바로 다운로드로 진행..
        permHandler.permissionToWrite(finalList)
    }

    // <1-b> MyIapHelper.kt 에서 호출!로 시작-> Delete!
    fun deleteFromDisk(downloadableItem: DownloadableItem) {
        val trId=  downloadableItem.trackID
        val fileNameFull = downloadableItem.filePathAndName
        val fileToDelete = File(fileNameFull)

        if(fileToDelete.exists()) {
            fileToDelete.delete()
            Log.d(TAG, "deleteFromDisk: *****Deleting trId=$trId, fileToDelete=$fileToDelete")
        }

    }

    // <2>
    fun checkDuplicatedFileOnDisk (filePathAndName: String): Boolean { //다운로드 받을 File 이 있는지 체크. todo: 불량/끊긴 다운로드인지도 체크 필요.?
        //val destPath : String= receivedActivitiy.getExternalFilesDir(null)!!.absolutePath + "/.AlarmRingTones"
        //Toast.makeText(this,"${destPath.toString()}",Toast.LENGTH_LONG).show()
        //Log.d(TAG, "checkDuplicatedFileOnDisk: begins!!")
        val fileToCheck = File(filePathAndName)
        return if(fileToCheck.isFile) { //true if and only if the file denoted by this abstract pathname exists and is a normal file; false otherwise

            Log.d(TAG, "checkDuplicatedFileOnDisk: \"File ${fileToCheck.name} exists\"")
            //Toast.makeText(receivedActivity,"File ${fileToCheck.name} exists", Toast.LENGTH_LONG).show()
            true
        }else{
            Log.d(TAG, "checkDuplicatedFileOnDisk: \"File ${fileToCheck.name} DOES NOT!!XX exist!!\"")
            //Toast.makeText(receivedActivity,"File ${fileToCheck.name} does not exist!!", Toast.LENGTH_SHORT).show()
            false

        }
    }

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
    fun singleFileDNLD(itemToDownloadable: DownloadableItem) {

        val fileNameAndFullPath = itemToDownloadable.filePathAndName
        val trackID = itemToDownloadable.trackID
        val fileNametoSaveOnDisk = File(fileNameAndFullPath).name

        //download request
        val mp3UrlToDownload = MyMediaPlayer.mp3UrlMap[trackID]
        //val mp3UrlToDownload = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
        // 우선 URL valid 한지 체크
        val isUrlValid: Boolean = URLUtil.isValidUrl(mp3UrlToDownload)
        if(!isUrlValid) {
            Log.d(TAG, "singleFileDNLD: DOWNLOAD ERROR. INVALID URL!")
            Toast.makeText(receivedActivity,"DOWNLOAD ERROR. INVALID URL!", Toast.LENGTH_LONG).show()
            return
        }

        Log.d(TAG, "singleFileDNLD: %%%%%%%%%%% WE'll FINALLY DOWNLOAD THIS trId=$trackID %%%%%%%%%%%")
        Log.d(TAG, "singleFileDNLD: %%%%%%%%%%% WE'll FINALLY DOWNLOAD THIS URL= $mp3UrlToDownload %%%%%%%%%%%")
        val dnlRequest = DownloadManager.Request(Uri.parse(mp3UrlToDownload))
        // 다운로드 방식 두가지 다 .. allowed
        dnlRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        dnlRequest.setTitle("Download")
        dnlRequest.setDescription("Downloading Your Purchased Ringtone...")
        dnlRequest.allowScanningByMediaScanner() // Starting in Q, this value is ignored. Files downloaded to directories owned
        // by applications (e.g. Context.getExternalFilesDir(String)) will not be scanned by MediaScanner and the rest will be scanned.
        //dnlRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION) // 다운 완료되면 표시
        dnlRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) // 다운 중에만 표시
        // 다운로드 경로 설정
        // 1) File Explorer 등을 켰을때 Downloads 라고 뜨는 public directory 에 등록
        //dnlRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "${System.currentTimeMillis()}")
        // 2) Main Storage 에 폴더를 생성 (ex.MyTest0413) 후 그곳에 파일 저장.
//        dnlRequest.setDestinationInExternalPublicDir("/myTest0413", "${System.currentTimeMillis()}")
        // 3) 내 app 의 Main Storage>android/data/com.xx.downloadtest1/files/안에 .AlarmRingTones라는 폴더를 만들고! 그 안에 file 저장! hidden 상태임!
        dnlRequest.setDestinationInExternalFilesDir(receivedActivity.applicationContext,".AlarmRingTones",fileNametoSaveOnDisk)

        //get download service, and enqueue file
        val dnlManager = receivedActivity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        // 다운로드 & 완료 알림(BroadCast) 관련: 1)식별 id 생성 2)Broadcast 등록.
        val downloadID = dnlManager.enqueue(dnlRequest) // 이 코드는 두가지 역할을 함.
        //1) Enqueue a new download. The download will start automatically once the download manager is ready to execute it and connectivity is available.
        // 2)추후 다운로드 완료후-> broadcast 받을것이고-> 여기서 만든 id(LONG) 로 판독 예정!

        // Download Progress 위해.
        //downloadAttemptCount+=1 //추후 filesToDNLDCount 와 동일하게
        //showDNLDProgress(downloadID,trackID)
        CoroutineScope(Dispatchers.IO).launch {

            openBtmShtSingleDNLD()

            isSingleDNLDInProcess = true
            val isStillDNLDING = getResultFromSingleDNLD(downloadID, trackID, fileNameAndFullPath) // (1) showDNLDCoroutine 시작->
            withContext(Dispatchers.Main) {
                if(!isStillDNLDING) {
                    Log.d(TAG, "^^^^^^^^^^^singleFileDNLD: INSIDE COROUTINE. isStillDNLDING=$isStillDNLDING")
                    runOnUiThread {
                        btmShtSingleDNLDInstance.animateLPI(100,1) //  그래프 만땅!
                    }
                    delay(1000)
                    btmShtSingleDNLDInstance.removeSingleDNLDBtmSheet() // 혹시 몰라서 또 넣어줬음. 이중장치.
                    isSingleDNLDInProcess = false
                }
            }
        }
        Log.d(TAG, "singleFileDNLD: DLND ID= $downloadID, trackId=$trackID")

    }

    private fun getResultFromSingleDNLD(dnldID: Long, trId: Int, filePathAndName: String):Boolean {
        val dnlManager = receivedActivity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        var isStillDownloading = true
        var animInitialBool = false // 시작하자마자 20~40 % 까지(random) 는 무조건 animation 실행.
        var animFortyNSixtyBool = false // progress 가 40~60 사이일 때 animation
        var animSixtyEightyBool = false // 60~80 구간
        var animEightyOrHigher = false // 80 이상

        while (isStillDownloading)
        {
            val query = DownloadManager.Query()
            query.setFilterById(dnldID)
            val cursor = dnlManager.query(query)

            if(cursor.moveToFirst()){
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                when(status) {
                    DownloadManager.STATUS_PENDING -> {
                        Log.d(TAG, "showDNLDProgress: STATUS PENDING / trkId=$trId")}
                    DownloadManager.STATUS_PAUSED -> {
                        //Log.d(TAG, "showDNLDProgress: STATUS PAUSED / trkId=$trId")
                    }
                    DownloadManager.STATUS_RUNNING -> {
                        // Log.d(TAG, "showDNLDProgress: STATUS RUNNING / trkId=$trId")
                    }
                    DownloadManager.STATUS_FAILED -> {
                        Log.d(TAG, "showDNLDProgress: STATUS FAILED / trkId=$trId")
                        Toast.makeText(receivedActivity,"Download Failed. Please check your internet connection.",
                            Toast.LENGTH_LONG).show()
                        btmShtSingleDNLDInstance.removeSingleDNLDBtmSheet() // 아래 while loop 밖에도 있지만. 혹시 모르니 여기서도 btm Sheet 닫기 넣어줌.
                        isSingleDNLDInProcess = false
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        Log.d(TAG, "showDNLDProgress: STATUS SUCCESSFUL, TRK ID=$trId")
                        //btmShtSingleDNLDInstance.removeSingleDNLDBtmSheet() // 아래 while loop 밖에도 있지만. 혹시 모르니 여기서도 btm Sheet 닫기 넣어줌.
                        isStillDownloading = false
                    }
                }
                val bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val myDownloadProgress = ((bytesDownloaded * 100L)/bytesTotal).toInt() // changed from *100 to *99

                // 다운로드 완료 후 보고까지 시간차가 나는 경우가 종종 있음. 그래서 파일 사이즈 체크를 실행하여 btmSht 없애주도록 해봄.
                //파일 사이즈 체크 // 나름 10 초 안팎으로 들어오는 듯. 나름 reliable .. receiver 와 함께 download complete 으로 인지해도 reliable 할지.. 모니터링 필요.
                val fileSizeCheck = File(filePathAndName)
                if(fileSizeCheck.length().toInt() == bytesTotal) { // 다운받은 파일크기 == 전체 파일 크기 <- 이건 다운 완료 후 10초내로 응답하는 듯..나름 reliable..?
                    val currentTime= Calendar.getInstance().time
                    Log.d(TAG, "getResultFromSingleDNLD: @@@@@@fileSizeCheck.length=${fileSizeCheck.length()}, time:$currentTime")
                    isStillDownloading = false
                }

                runOnUiThread {
                    //Log.d(TAG, "sumTotalDNLDSize: TRK ID=$trId, dnldID=$dnldID, bytes_soFar=$bytesDownloaded, bytes_total= $bytesTotal, progress:$myDownloadProgress")
                    // 다운이 (최초로) STATUS_RUNNING 이 되면 animation 을 20~40 % 중 랜덤 value 로 올리는 animation 실행.
                    if(!animInitialBool && status == DownloadManager.STATUS_RUNNING) { //status running = 2
                        val randomPrgrsValue = (20..40).random() // 20~40 중 random value.
                        btmShtSingleDNLDInstance.animateLPI(randomPrgrsValue,5000) // 애니메이션이 차는 속도 5초.
                        animInitialBool = true

                        Log.d(TAG, "getResultFromSingleDNLD: initialAnimation! 'Actual Prgrs'=$myDownloadProgress, randomValue=$randomPrgrsValue, status=$status")
                    }

                    if(!animFortyNSixtyBool && myDownloadProgress > 40 && myDownloadProgress <60) { // 40~60 구간 animation
                        if(btmShtSingleDNLDInstance.isAnimationRunning()) { // 그 전 혹은 현재의 animation 이 작동중이면..그냥 return
                            //Log.d(TAG, "getResultFromDNLD: (40-60) Whatever the Animation Is Running Already!! Current Progress=$myDownloadProgress")
                            return@runOnUiThread
                        }
                        val randomDuration = (2000L..5000L).random()
                        btmShtSingleDNLDInstance.animateLPI(myDownloadProgress, randomDuration)
                        animFortyNSixtyBool = true
                        Log.d(TAG, "getResultFromSingleDNLD: animFortyNSixtyBool=$animFortyNSixtyBool, 'Actual Prgrs'=$myDownloadProgress, randomDur=$randomDuration")
                    }
                    if(!animSixtyEightyBool && myDownloadProgress >= 60 && myDownloadProgress <80) { // 40~60 구간 animation
                        if(btmShtSingleDNLDInstance.isAnimationRunning()) {
                            //Log.d(TAG, "getResultFromDNLD: (60-80) Whatever the Animation Is Running Already!! Current Progress=$myDownloadProgress")
                            return@runOnUiThread
                        }
                        val randomDuration = (2000L..5000L).random()
                        btmShtSingleDNLDInstance.animateLPI(myDownloadProgress, randomDuration)
                        animSixtyEightyBool = true
                        Log.d(TAG, "getResultFromSingleDNLD: animSixtyEightyBool=$animSixtyEightyBool, 'Actual Prgrs'=$myDownloadProgress, randomDur=$randomDuration")
                    }
                    if(!animEightyOrHigher && myDownloadProgress >= 80) { // 80 구간 animation
                        if(btmShtSingleDNLDInstance.isAnimationRunning()) {
                            //    Log.d(TAG, "getResultFromDNLD: (80-) Whatever the Animation Is Running Already!! Current Progress=$myDownloadProgress")
                            return@runOnUiThread
                        }
                        val randomDuration = (3000L..6000L).random()
                        btmShtSingleDNLDInstance.animateLPI(myDownloadProgress, randomDuration)
                        animEightyOrHigher = true
                        Log.d(TAG, "getResultFromSingleDNLD: animEightyOrHigherBool=$animEightyOrHigher, 'Actual Prgrs'=$myDownloadProgress, randomDur=$randomDuration")
                    }

                }

                cursor.close()
            }

        }
        return isStillDownloading // downloading 이 끝나면 bool 값은 false 로 되어있음.
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

