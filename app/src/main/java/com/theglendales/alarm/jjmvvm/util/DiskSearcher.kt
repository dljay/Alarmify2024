package com.theglendales.alarm.jjmvvm.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import java.io.*

private const val TAG="DiskSearcher"
private const val RTA_FOLDER="/.AlarmRingTones"
private const val ART_FOLDER="/.AlbumArt"
//private const val SH_PREF_FOLDER= "shared_prefs" // is this folder name reliable? check..


class DiskSearcher(val context: Context)
{
    companion object {
        val finalRtArtPathList = mutableListOf<RtOnThePhone>() // AlarmListActivity> DiskSearcher.rtArtPath SharedPref 에서 받은걸 여기에 저장.
    }
    val mySharedPrefManager: MySharedPrefManager by globalInject() // Shared Pref by Koin!!

    val emptyList = mutableListOf<RtOnThePhone>()

    val onDiskArtMap: HashMap<String?, String?> = HashMap() // <trkId, 앨범아트 경로> <- 이것이 먼저 업데이트되어
    val onDiskRingtoneList = mutableListOf<RtOnThePhone>() // 요 리스트를 갱신하는데 도움을 줌..


    val topFolder = context.getExternalFilesDir(null)!!.absolutePath
    val alarmRtDir = File(topFolder, RTA_FOLDER)
    val artDir = File(topFolder, ART_FOLDER)
    //val xmlFile = File(topFolder+SH_PREF_FOLDER+ "RtOnThePhoneList.xml") // RtOnThePhoneList.xml


    // rta & art 파일이 매칭하는지 보완이 필요없는지 확인하는 기능 isRescanNeeded isRtListRebuildNeeded
    fun isDiskScanNeeded(): Boolean {
        var isDiskRescanNeeded = false

    // 1) 우선 폴더 존재하는지 확인.
        if(!alarmRtDir.exists()) {alarmRtDir.mkdir()} // <A> /.AlarmRingTones 폴더가 존재하지 않는다. -> 폴더 생성
        if(!artDir.exists()) {artDir.mkdir()} // <B> /.AlbumArt 폴더가 존재하지 않는다.

        // SharedPref (RtOnThePhoneList.xml)에서 돌려받는 list 는 Disk 에 저장되어있는 RtOnThePhone object 들의 정보를 담고 있음.
        val listFromSharedPref = mySharedPrefManager.getRtOnThePhoneList()


    // 2-A)SharedPref (RtOnThePhoneList.xml) 파일 자체가 없을때 (즉 최초 실행하여 xml 파일이 없을때) 깡통 List 를 받음.
        if(listFromSharedPref.isNullOrEmpty()) {

            Log.d(TAG, "isDiskScanNeeded: 2-A) We couldn't retrieve sharedPref!")
            isDiskRescanNeeded=true
            return isDiskRescanNeeded
        }
    // 2-B) /.AlarmRingtonesFolder 파일 갯수가 0 혹은 <10 미만
        if(alarmRtDir.listFiles().isNullOrEmpty()||alarmRtDir.listFiles().size < 10) { //todo: defaultRt 갯수 바뀌면. 반영. 혹은 구입한 px.rta 갯수가 많을때도 문제. 숫자에 의존해서는 안된다.
            Log.d(TAG, "isDiskScanNeeded: 2-B)  /.AlarmRingtonesFolder 파일 갯수가 0 혹은 <10 미만")
            isDiskRescanNeeded=true
            return isDiskRescanNeeded
        }
    // 2-C) /.AlbumArtFolder 파일 갯수 <-> /.AlarmRtFolder 파일 갯수 비교 (추가 구매건 혹은 삭제된 RT가 있으면 불일치할것임.)
        if (alarmRtDir.listFiles().size != artDir.listFiles().size) {
            Log.d(TAG, "isDiskScanNeeded: 2-C) 파일 갯수 불일치 /.AlarmRtFolder=${alarmRtDir.listFiles().size} <-> /.AlbumArtFolder=${artDir.listFiles().size} ")
            isDiskRescanNeeded=true
            return isDiskRescanNeeded
        }
    // 2-D) SharedPref (RtOnThePhoneList.xml) 리스트 <-> /.AlarmRtFolder 파일 갯수 비교 (추가 구매건 혹은 삭제된 RT가 있으면 불일치할것임.)
        if(alarmRtDir.listFiles().size != listFromSharedPref.size) {
            Log.d(TAG, "isDiskScanNeeded: 2-D) SharedPref 리스트 <-> /.AlarmRtFolder 파일 갯수 불일치.")
            isDiskRescanNeeded=true
            return isDiskRescanNeeded
        }
    //2-E) SharedPref (RtOnThePhoneList.xml) 에서 받은 리스트 안 obj 검색->'artPath' (혹은 audio path)가 null 임.//
        val artPathEmptyList = listFromSharedPref.filter { rtWithAlbumArtObj -> rtWithAlbumArtObj.artFilePathStr.isNullOrEmpty()}

        if(artPathEmptyList.isNotEmpty()) { //(즉 artPathEmptyList 안 갯수가 > 0)
                for(i in 0 until artPathEmptyList.size) { //todo: 여기 for loop 은 단순 모니터링 용. 없애도 됨.
                    Log.d(TAG, "isDiskScanNeeded: 다음 파일의 artFilePathStr 은 비어있음!! = ${artPathEmptyList[i].fileNameWithoutExt}")
                }
            isDiskRescanNeeded=true
            return isDiskRescanNeeded
        }
        return isDiskRescanNeeded
    }



    fun onDiskRtSearcher(): MutableList<RtOnThePhone>
    {
        onDiskRingtoneList.clear() // DetailsFrag 다시 들어왔을 때 먼저 클리어하고 시작.
        // todo: 현재 /.AlarmRingTones 폴더 안 rta 갯수를 파악해서 필요할 경우 copy 하는것에서 -> 무조건 다 copy 하는것으로 변경할지? (defrt mp3 데이터 변경되었을때나 업데이트 되었을때 대처 위해..)
    //(1)-b  /.AlarmRingTones 에 Defrt 파일이 없거나, 10개 미만으로 있을때 (즉 최초 실행 혹은 어떤 연유로 defrta 파일 갯수가 부족) => Raw 폴더에 있는 DefaultRt 들을 폰에 복사
            val listOfDefrtFiles = alarmRtDir.listFiles { _, name -> name.contains("defrt")  } // 파일 이름에 "defrt" 를 포함하는 놈들을 List 로 받아서. 그 갯수 확인.
                if(listOfDefrtFiles.isNullOrEmpty() || listOfDefrtFiles.size <10) { // 아예 폴더가 비어있거나 or Defrtxx.rta 파일이 10개 미만일 떄.
                    Log.d(TAG, "onDiskRtSearcher: Possible Missing Defrt Files. numberOfDefRtFiles=${listOfDefrtFiles.size}")

                    copyDefaultRtsToPhone(R.raw.defrt01, "defrt01.rta")
                    copyDefaultRtsToPhone(R.raw.defrt02, "defrt02.rta")
                    copyDefaultRtsToPhone(R.raw.defrt03, "defrt03.rta")
                    copyDefaultRtsToPhone(R.raw.defrt04, "defrt04.rta")
                    copyDefaultRtsToPhone(R.raw.defrt05, "defrt05.rta")
                    copyDefaultRtsToPhone(R.raw.defrt06, "defrt06.rta")
                    copyDefaultRtsToPhone(R.raw.defrt07, "defrt07.rta")
                    copyDefaultRtsToPhone(R.raw.defrt08, "defrt08.rta")
                    copyDefaultRtsToPhone(R.raw.defrt09, "defrt09.rta")
                    copyDefaultRtsToPhone(R.raw.defrt10, "defrt10.rta")
                    // raw 파일명은 .mp3 로, 폰에는 .rta 로 (.mp3 로 raw 에 넣지 않으면 인스톨 후 생성되는 두 Default 알람의 벨소리가 Notification 에서 소리 안남!)

                }

    //(1)-c: 구입한 파일이 현 폴더에 있는지 한번 더 확인? ...구축해줄곳임. flowchart 참고.

    // (2) 이제 폴더에 파일이 있을테니 이것으로 updateList() 로 전달할 ringtone 리스트를 만듬.
        if(alarmRtDir.listFiles() != null)
        {
            for(f in alarmRtDir.listFiles())
            {
            // (2)-a /.AlarmRingtones 폴더에 있는 파일들에서 trkId, Title, artFilePath 등을 추출!!
                val rtOnDisk = extractMetaDataFromRta(f)
                onDiskRingtoneList.add(rtOnDisk)
                Log.d(TAG, " onDiskRtSearcher: \n[ADDING TO THE LIST]  *** Title= ${rtOnDisk.rtTitle}, trId=${rtOnDisk.trIdStr}, " +
                        "\n *** file.name=${f.name} // file.path= ${f.path.toString()} //\n artFilePath=${rtOnDisk.artFilePathStr}")

            // (2)-b **해당 trID의 artFilePath 가 MAP 에 등록되어있지 않은 경우** null 상태. (User 가 지웠거나, onDiskRtSearcher() 가 가동 안되었을때 등등..)
                // 신규 구매의 경우 px.rta 파일은 있으나 rta 파일은 없을것. 그러나 정상적이라면 .rta<->.art 파일은 각각 있어야 함.
                // (그래서 ListFrag 에서 .readAlbumArtOnDisk() 를 먼저 실행하여 이 extractArtFromRta() 과정을 생략 해주는것.)
                if(rtOnDisk.artFilePathStr.isNullOrEmpty()) {
                    // ** 전체 file path rebuilding 이 'artFileList <-> audioFileList 대조 후 rebuilding 보다 효율적일듯. ** 생각보다 별로 안 걸림.

                    extractArtFromSingleRta(rtOnDisk.trIdStr, Uri.parse(rtOnDisk.audioFilePath)) } // 안될때만 extractArtFromSingleRta?
            }// for loop 끝.
            //Log.d(TAG, "searchFile: file Numbers= $numberOfFiles")
        }
        Log.d(TAG, "onDiskRtSearcher: returning 'onDiskRingtoneList' List!! \n onDiskRingtoneList= $onDiskRingtoneList")
        return onDiskRingtoneList

    }

    fun getArtPathViaRtaFileName(rtaFileName: String): String {
        // .listFiles filter 쓰는것보다 이게 더 효과적. filter 는 결국 모든 파일이 filter 조건문에 맞는지 확인함..
        var artFilePath= ""
        here@for(artFile in artDir.listFiles()) {
            Log.d(TAG, "getArtPathViaRtaFileName: artFile.nameWithouExtension=${artFile.nameWithoutExtension}")
            if(artFile.nameWithoutExtension == rtaFileName) {
                Log.d(TAG, "getArtPathViaRtaFileName: We found matching art file! name=${artFile.name}")
                artFilePath = artFile.path
                break@here // 찾는 순간 here@ 로 가고-> '루프 바로 뒤의 실행문으로 점프!!'
            }
        }
        return artFilePath
    }

    // 위의 rtOnDiskSearcher() 에서 받음 리스트로 a) album art 가 있는지 체크 -> 있는 놈 경로는 xx Uri List 에 저장 b) albumArt 가 없으면 -> 생성!-> 디스크에 저장.
    fun readAlbumArtOnDisk() {
    // A) Disk 에 있는 ringTone 의 Album Art 가 디스크에 저장되어 있는지 체크 -> 있으면 xx Uri List 에 저장 (추후 Glide 로 그래픽 로딩 예정)
        // A-1) /.AlbumArt 폴더의 리스트 확인

        // A-1-a)만약 /.AlbumArt 폴더가 없을때는 폴더를 생성
        if(!artDir.exists()) {
            Log.d(TAG, "readAlbumArtOnDisk: Hey! Folder $artDir doesn't exist. We'll create One!@~")
            artDir.mkdir()
        }

        // A-1-b)폴더는 있는데 그 안에 아무 파일이 없을때..(아무것도 다운 받은게 없는 상태.)
        if(artDir.listFiles().isNullOrEmpty()) {
            Log.d(TAG, "readAlbumArtOnDisk: NO Album Art graphic FILES INSIDE THE FOLDER!. Probably the first time opening this app?")
            onDiskRtSearcher() // todo? Hmm..?
        }


        // A-1-c)폴더에 파일이 있을때 => 각 파일의 경로를 onDiskArtMap 에 저장 ex) [01, 경로1], [02, 경로2] ...
        if(artDir.listFiles() != null)
        {
            // ./AlbumArt 폴더에 있는 xxx.art 파일 for loop
            for(artFile in artDir.listFiles())
            {
                //1) 쓸데없는 파일 있으면 삭제..
                if(artFile.name.contains('-')||!artFile.name.contains(".art")||artFile.length()==0L) {
                    Log.d(TAG, "!!! Deleting following .art file= ${artFile.name}")
                    artFile.delete()

                }
                //2) Map 에 띄워줌.
                val fullPathOfArtFile: String = topFolder+ ART_FOLDER+ File.separator + artFile.name
                //val artUri = Uri.parse(artFile.path.toString())
                val trkId = artFile.nameWithoutExtension // 모든 앨범아트는 RT 의 TrkId 값.art 로 설정해야함! (**파일명과 TrkId 가 일치해야함! ) -> ex) defrt01.art

                onDiskArtMap[trkId] = artFile.path // MAP 에 저장! <trkId, PathString>
                Log.d(TAG, "readAlbumArtOnDisk: added artFilePath(${artFile.path}) to onDiskArtMap => $onDiskArtMap")

            }// for loop 끝.

        }


    }

    // AlarmDetailsFragment> Line 385 에서 호출 (Details Frag 열었을 때 동그란 Frame 안에 있는 Album Art 사진에 현재 RT 의 경로를 전달)
    fun getArtFilePath(trkId: String?): String? = onDiskArtMap[trkId]
    fun updateList(rtOnDiskListReceived: MutableList<RtOnThePhone>) {
        Log.d(TAG, "updateList: called. rtOnDiskListReceived=$rtOnDiskListReceived")

        DiskSearcher.finalRtArtPathList.clear()
        for(i in 0 until rtOnDiskListReceived.size) {
            DiskSearcher.finalRtArtPathList.add(rtOnDiskListReceived[i])
        }
        Log.d(TAG, "updateList: done..!! fianlRtArtPathList = ${DiskSearcher.finalRtArtPathList}")
    }

//************ Private Utility Functions ====================>>>>
    private fun copyDefaultRtsToPhone(defaultRtRaw: Int, defRtName: String) {
        Log.d(TAG, "copyDefaultRtsToPhone: started..")
        //Method #1
        val inputStr: InputStream = context.resources.openRawResource(defaultRtRaw)
        val outStr: FileOutputStream = FileOutputStream(topFolder + RTA_FOLDER +File.separator + defRtName)
        val buff: ByteArray = ByteArray(1024)
        var length: Int = inputStr.read(buff)
        try {
            while (length > 0) {
                if (length != -1) {
                    outStr.write(buff, 0, length)
                    length = inputStr.read(buff)
                }
            }
            Log.d(TAG, "copyDefaultRtsToPhone: copying default rt=[$defaultRtRaw] completed..")
        } catch (e: Exception) {
            Log.d(TAG, "copyDefaultRtsToPhone: !! something went wrong! error=$e")
        } 
        finally {
            inputStr.close()
            outStr.close()
        }

     //Method #2 <- 현재는 안되는 중 (X)
//    val defaultRtUri= Uri.parse("android.resource://" + context.packageName + "/" + R.raw.defrt1)
//    val defaultRtFile = File(defaultRtUri.toString())
//    defaultRtFile.copyTo(File(topFolder+ RT_FOLDER+File.separator + "defrt1.rta"))

    }

    private fun extractMetaDataFromRta(fileInRtaFolder: File): RtOnThePhone {
        val mmr =  MediaMetadataRetriever()

        val actualFileForMmr = topFolder+ RTA_FOLDER+ File.separator + fileInRtaFolder.name

        try { // 미디어 파일이 아니면(즉 Pxx.rta 가 아닌 파일은) setDataSource 하면 crash 남! 따라서 try/catch 로 확인함.
            mmr.setDataSource(actualFileForMmr)
        }catch (er:Exception) {
            Log.d(TAG, "extractMetaDataFromRta: unable to run mmr.setDataSource for the file=${fileInRtaFolder.name}. WE'LL DELETE THIS PIECE OF SHIT!")
            fileInRtaFolder.delete()
        }

        //1) 파일이 제대로 된 rta 인지 곡 길이(duration) return 하는것으로 확인. (Ex. p1=10초=10042(ms) 리턴)  옹.
        val fileDuration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

        Log.d(TAG, "extractMetaDataFromRta: fileName= ${fileInRtaFolder.name}, fileDuration=$fileDuration")
        if(fileDuration==null) {
            Log.d(TAG, "extractMetaDataFromRta: Possible Corrupted file. Filename=${fileInRtaFolder.name}")
            fileInRtaFolder.delete()
        }
        //2) 파일명에 hyphen(-) 포함되어있거나(중복 다운로드)/'rt' 가 없거나!/사이즈가=0 이면 => 삭제
        //
        if(fileInRtaFolder.name.contains('-')||!fileInRtaFolder.name.contains(".rta")||fileInRtaFolder.length()==0L) {
            Log.d(TAG, "!!! extractMetaDataFromRta: ${fileInRtaFolder.name}")
            if(fileInRtaFolder.length()==0L) {
                Log.d(TAG, "extractMetaDataFromRta: file size prob 0? Filesize=${fileInRtaFolder.length()}")
            }
            fileInRtaFolder.delete()
            Log.d(TAG, "downloadedRtSearcher: deleted file: [ ${fileInRtaFolder.name} ] from the disk")

        }
        //3) Album MetaData (제목, TrId) 찾기. 앨범 아트는 AlarmDetailsFrag 에서 찾아줌. 4) 번에서 이걸 RtInTheCloud 로 만들어줌.
            // 3-a) "제목" // METADATA_KEY_TITLE 사용!
            val rtTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)

            // 3-b) "TrId 찾기" // METADATA_KEY_COMPOSER 사용!
            //val trIDString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
            val trIDString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)

            // 3-c) rta File Path
            val audioFilePath = fileInRtaFolder.path.toString()

            // 3-d) "앨범설명글" METADATA_KEY_ALBUMARTIST 사용
            val rtDescription = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST) // todo: check.
            //Log.d(TAG, "extractMetaDataFromRta: rtDescription= $rtDescription")

            // 3-e) "Badge" 관련 String 받기 (EX. Intense, Nature 배지 표시 필요한 놈은 "I,N" 이렇게 입력되어잇음. // METADATA_KEY_ARTIST 사용
            val badgeString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            Log.d(TAG, "extractMetaDataFromRta: BadgeString= $badgeString")
        

            // 3-f) artFile Path(String) 를 Map 에서 받음.
            //todo:  ** 아래 readArtOnDisk() 가 앱 시작과 동시에 실행됨. 다 되었다는 가정하에 여기서 찾지만. Path 가 아직 없는경우에 보완책
            val artFilePath = onDiskArtMap[trIDString] //<trkId, 앨범아트 경로> trIDString & artFilePath 둘 다 nullable String

        //4) RtOnThePhone Class 로 만들어서 리스트(onDiskRtList)에 저장
        val onDiskRingtone = RtOnThePhone(trIDString, rtTitle= rtTitle, audioFilePath = audioFilePath, fileNameWithoutExt = fileInRtaFolder.name,
            artFilePathStr = artFilePath, rtDescription = rtDescription, badgeStr = badgeString) // 못 찾을 경우 default 로 일단 trid 는 모두 -20 으로 설정
        Log.d(TAG, "extractMetaDataFromRta: Extracted [onDiskRingtone]=$onDiskRingtone")
        return onDiskRingtone
    }
    // 모든 링톤 파일(rta)은 albumArt 를 MetaData 로 갖고 있어야 하는데 어떤 이유에서든(User 삭제 등) 없을때
    private fun extractArtFromSingleRta(trkId: String?, rtaUri: Uri) {
        Log.d(TAG, "extractArtFromSingleRta: called for trkId=$trkId, rtaUri=$rtaUri")
        val mmr =  MediaMetadataRetriever()

        try { // 미디어 파일이 아니면(즉 Pxx.rta 가 아닌 파일은) setDataSource 하면 crash 남! 따라서 try/catch 로 확인함.
            mmr.setDataSource(context,rtaUri)
        }catch (er:Exception) {
            Log.d(TAG, "error mmr.setDataSource")
        }
        // Album Art
        val artBytes: ByteArray? = mmr.embeddedPicture // returns null if no such graphic is found.

        var albumArtBMP: Bitmap? = null
        if(artBytes==null) {Log.d(TAG, "extractArtFromSingleRta: No embedded Image Resource found!!") }
        if(artBytes!=null) // embed 된 image 를 추출 가능하면=>
        {
            try {
                // 1)ByteArryay 를 BitMap 으로 변환
                albumArtBMP = BitmapFactory.decodeByteArray(artBytes,0, artBytes.size)
                // 2) Disk 에 Save. 파일명은 trId.art ****
                saveBmpToJpgOnDisk(trkId, albumArtBMP)
                Log.d(TAG, "extractArtFromSingleRta: try done..")


            }catch (e: Exception) {
                Log.d(TAG, "extractArtFromSingleRta: error trying to add bitmap to albumArtMap.. Error=$e")
            }
        }
    }

    //Save to Disk
    private fun saveBmpToJpgOnDisk(trkId: String?, bitmap: Bitmap?) {
        if(trkId.isNullOrEmpty() || bitmap==null) {
            Log.d(TAG, "saveBmpToJpgOnDisk: trkId=$trkId & bitmap are EMPTY! NULL!! ZILCH")
            return
        }

        // Initialize a new file instance to save bitmap object
        val dir = artDir
        if(!dir.exists()) {
            dir.mkdirs()
        }
        // ex) .rta 파일의 METADATA 중 '작곡가' 에 TrId 가 저장됨: Ex) defrt01, p1, p2 ...
        val savedToDiskFile = File(dir,"${trkId}.art") // JPG 파일 포맷인데 우리는 그냥 .art 로 임의로 사용 (모바일에서 자동 검색되서 뜨는것 막는 용도도 있음..)

        try{
            // Compress the bitmap and save in jpg format
            val stream: OutputStream = FileOutputStream(savedToDiskFile)
            bitmap?.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
            // 이제는 파일 추출했으니
            onDiskArtMap[trkId] = savedToDiskFile.path // a) onDiskArtMap 에 artPath 를 기록
            // b)onDiskRtList 에서 RtOnThePhone object 를 찾아서 albumArtPath 를 정리해줌 -> 그래야 Spinner 에 뜨지!!
            val index: Int = onDiskRingtoneList.indexOfFirst { rt -> rt.trIdStr == trkId } // 동일한 rt.trId 를 갖는 놈의 인덱스를 onDiskRtList 에서 찾기
            Log.d(TAG, "saveBmpToJpgOnDisk: index of missing artPath in onDiskRtList=$index")
            onDiskRingtoneList[index].artFilePathStr = savedToDiskFile.path


            Log.d(TAG, "saveBmpToJpgOnDisk: Saved to disk. File Name= ${savedToDiskFile.name}, path=${savedToDiskFile.path}")
        }catch (e: IOException){
            Log.d(TAG, "saveBmpToJpgOnDisk: unable to save to disk because -> error=$e")
        }

//        // Return the saved bitmap uri
//        return Uri.parse(file.absolutePath)
    }

//    /**
//     * 프로그램 최초 설치시 아래 extractArtFromAllRta()
//     */
//    private fun extractAllArtsFromAllRta() {
//
//    }

//*************************** DOWNLOAD 관련해서 필요한 기능들 (기존에 MyDownloader_v1.kt 에 있던 기능들 옮겨옴.)
    // Same File Exists ? + And that Same File need to be replaced?
    fun isSameFileOnThePhone (filePathAndName: String): Boolean { //다운로드 받기 전 이미 Disk 에 File 이 있는지 체크.
        val fileToCheck = File(filePathAndName)
        return if(fileToCheck.isFile && fileToCheck.exists()) { //true if and only if the file denoted by this abstract pathname exists and is a normal file; false otherwise
            Log.d(TAG, "isSameFileOnThePhone: \"File ${fileToCheck.name} exists\"")
            //Toast.makeText(receivedActivity,"File ${fileToCheck.name} exists", Toast.LENGTH_LONG).show()
            // todo: 파일은 있지만 혹시나 불량 파일인지도 확인? (중간에 다운로드 끊기는 등)
            true
        }else{
            Log.d(TAG, "doesSameFileExistOnThePhone: \"File ${fileToCheck.name} DOES NOT!!XX exist!!\"")
            //Toast.makeText(receivedActivity,"File ${fileToCheck.name} does not exist!!", Toast.LENGTH_SHORT).show()
            false
        }
    }
    fun isSameFileOnThePhone_RtObj(rtObj: RtInTheCloud): Boolean {
        val fileSupposedToBeAt =topFolder + RTA_FOLDER+ File.separator + rtObj.iapName + ".rta" // 구매해서 다운로드 했다면 저장되있을 위치
        val fileToCheck = File(fileSupposedToBeAt)
        return if(fileToCheck.isFile && fileToCheck.exists()) { //true if and only if the file denoted by this abstract pathname exists and is a normal file; false otherwise
            Log.d(TAG, "isSameFileOnThePhone_RtObj: \"File ${fileToCheck.name} exists\"")
            //Toast.makeText(receivedActivity,"File ${fileToCheck.name} exists", Toast.LENGTH_LONG).show()
            // todo: 파일은 있지만 혹시나 불량 파일인지도 확인? (중간에 다운로드 끊기는 등)
            true
        }else{
            Log.d(TAG, "isSameFileOnThePhone_RtObj: \"File ${fileToCheck.name} DOES NOT!!XX exist!!\"")
            //Toast.makeText(receivedActivity,"File ${fileToCheck.name} does not exist!!", Toast.LENGTH_SHORT).show()
            false
        }
    }
    fun deleteFileByIAPName(iapName: String) {
        val rtaSupposedToBeAt =topFolder + RTA_FOLDER+ File.separator + iapName + ".rta" // 구매해서 다운로드 했다면 저장되있을 위치
        val artSupposedToBeAt =topFolder + ART_FOLDER+ File.separator + iapName + ".art" // 구매해서 다운로드 했다면 저장되있을 위치

        try {
            val rtaToDelete = File(rtaSupposedToBeAt)
            val artToDelete = File(artSupposedToBeAt)
            if(rtaToDelete.exists()) {
                rtaToDelete.delete()
                Log.d(TAG, "deleteFileByIAPName: *****Deleting .rat file Name=${rtaToDelete.name}")
            } else if(!rtaToDelete.exists()) {
                Log.d(TAG, "deleteFileByIAPName: Such File doesn't exist on the drive. 1)rta FileName= $rtaToDelete")
            }
            if(artToDelete.exists()) {
                artToDelete.delete()
                Log.d(TAG, "deleteFileByIAPName: *****Deleting .art file Name=${artToDelete.name}")
            } else if(!artToDelete.exists()) {
                Log.d(TAG, "deleteFileByIAPName: Such File doesn't exist on the drive. 2)art FileName= $artToDelete")
            }
        }catch (e: Exception) {
            Log.d(TAG, "deleteFileByIAPName: Maybe fileNameFull variable is null? rtaSupposedToBeAt=$rtaSupposedToBeAt, artSupposedToBeAt= $artSupposedToBeAt ")
        }
    }
    fun deleteFileByPath(fileNameAndFullPath: String) { // IAP_V2 에서 사용되서 남겨놓은것.
            try {
                val fileToDelete = File(fileNameAndFullPath)
                if(fileToDelete.exists()) {
                    fileToDelete.delete()
                    Log.d(TAG, "deleteFromDisk: *****Deleting file Name=${fileToDelete.name}")
                } else if(!fileToDelete.exists()) {
                    Log.d(TAG, "deleteFromDisk: Such File doesn't exist on the drive. FileName= $fileToDelete")
                }
            }catch (e: Exception) {
                Log.d(TAG, "deleteFromDisk: Maybe fileNameFull variable is null? fileNameFull=$fileNameAndFullPath ")
            }

        }
}