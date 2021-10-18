package com.theglendales.alarm.jjmvvm.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

private const val TAG="DiskSearcher"
private const val RT_FOLDER="/.AlarmRingTones"
private const val ART_FOLDER="/.AlbumArt"
//private const val SH_PREF_FOLDER= "shared_prefs" // is this folder name reliable? check..


class DiskSearcher(val context: Context)
{
    companion object {
        val finalRtArtPathList = mutableListOf<RtWithAlbumArt>() // AlarmListActivity> DiskSearcher.rtArtPath SharedPref 에서 받은걸 여기에 저장.
    }
    val mySharedPrefManager: MySharedPrefManager by globalInject() // Shared Pref by Koin!!

    val emptyList = mutableListOf<RtWithAlbumArt>()

    val onDiskArtMap: HashMap<String?, String?> = HashMap() // <trkId, 앨범아트 경로> <- 이것이 먼저 업데이트되어
    val onDiskRingtoneList = mutableListOf<RtWithAlbumArt>() // 요 리스트를 갱신하는데 도움을 줌..


    val topFolder = context.getExternalFilesDir(null)!!.absolutePath
    val alarmRtDir = File(topFolder, RT_FOLDER)
    val artDir = File(topFolder, ART_FOLDER)
    //val xmlFile = File(topFolder+SH_PREF_FOLDER+ "RtaArtPathList.xml") // RtaArtPathList.xml

    // 앱 최초실행인지 확인하는 기능
    fun initialLaunchCheck()  {
        // <A> /.AlarmRingTones 폴더가 존재하지 않는다. -> 폴더 생성
        if(!alarmRtDir.exists()) {alarmRtDir.mkdir()}
        // <B> /.AlbumArt 폴더가 존재하지 않는다.
        if(!artDir.exists()) {artDir.mkdir()}
//        // <C> 폴더는 있는데 파일이 없다
//        if(alarmRtDir.listFiles().isNullOrEmpty()) {}
//        // <D> 폴더는 있는데 파일이 없다
//        if(artDir.listFiles().isNullOrEmpty()) {        }

        else {Log.d(TAG, "isInitialLaunch: This is not an INITIAL LAUNCH OF this app")}
    }

    // rta & art 파일이 매칭하는지 보완이 필요없는지 확인하는 기능 isRescanNeeded isRtListRebuildNeeded
    fun isDiskScanNeeded(): Boolean {
        var isDiskRescanNeeded = false

        // SharedPref 에서 돌려받는 list 는 Disk 에 저장되어있는 RtWithAlbumArt object 들의 정보를 담고 있음.

        val listFromSharedPref = mySharedPrefManager.getRtaArtPathList()
    // 1)SharedPref 파일 자체가 없을때 (깡통 List 를 받음.)
        if(listFromSharedPref.isNullOrEmpty()) {

            Log.d(TAG, "isDiskScanNeeded: We couldn't retrieve sharedPref!")
            isDiskRescanNeeded=true
            return isDiskRescanNeeded
        }
    //  2) 받은 list 안을 검색했을 때 'artPath' field 가 null // todo: 혹은 'audioFilePath' 가 null 값인게 있는 놈이 있는지
        val artPathEmptyList = listFromSharedPref.filter { rtWithAlbumArtObj -> rtWithAlbumArtObj.artFilePathStr.isNullOrEmpty()}

        if(artPathEmptyList.isNotEmpty()) {

                for(i in 0 until artPathEmptyList.size) {
                    Log.d(TAG, "isDiskScanNeeded: 다음 파일의 artFilePathStr 은 비어있음!! = ${artPathEmptyList[i].fileName}")
                }
            isDiskRescanNeeded=true
            return isDiskRescanNeeded
        }
        return isDiskRescanNeeded
    }

    fun updateList(rtOnDiskListReceived: MutableList<RtWithAlbumArt>) {
        Log.d(TAG, "updateList: called. rtOnDiskListReceived=$rtOnDiskListReceived")

        DiskSearcher.finalRtArtPathList.clear()
        for(i in 0 until rtOnDiskListReceived.size) {
            DiskSearcher.finalRtArtPathList.add(rtOnDiskListReceived[i])
        }
        Log.d(TAG, "updateList: done..!! fianlRtArtPathList = ${DiskSearcher.finalRtArtPathList}")
    }

    fun downloadedRtSearcher(): MutableList<RtWithAlbumArt>
    {
        onDiskRingtoneList.clear() // DetailsFrag 다시 들어왔을 때 먼저 클리어하고 시작.
        //todo: Raw 폴더에 있는 default Ringtone 들을 먼저 리스트에 업데이트!

        val emptyUriList = listOf<Uri>()


        // 만약 폴더가 없을때는 폴더를 생성
        if(!alarmRtDir.exists()) {
            Log.d(TAG, "downloadedRtSearcher: Folder $alarmRtDir doesn't exist. We'll create one")
            alarmRtDir.mkdir()
        }
        // 폴더는 있는데 파일이 없을때.. 그냥 return
        if(alarmRtDir.listFiles().isNullOrEmpty()) {
            Log.d(TAG, "downloadedRtSearcher: NO FILES INSIDE THE FOLDER!")
            return emptyList
        }
        // 폴더에 파일이 있을때..
        if(alarmRtDir.listFiles() != null)
        {
            for(f in alarmRtDir.listFiles())
            {
                val mmr =  MediaMetadataRetriever()

                val actualFileForMmr = topFolder+"/.AlarmRingTones"+ File.separator + f.name

                try { // 미디어 파일이 아니면(즉 Pxx.rta 가 아닌 파일은) setDataSource 하면 crash 남! 따라서 try/catch 로 확인함.
                    mmr.setDataSource(actualFileForMmr)
                }catch (er:Exception) {
                    Log.d(TAG, "downloadedRtSearcher: unable to run mmr.setDataSource for the file=${f.name}. WE'LL DELETE THIS PIECE OF SHIT!")
                    f.delete()
                    //return emptyList //
                }

                //1) 파일이 제대로 된 mp3 인지 곡 길이(duration) return 하는것으로 확인. (Ex. p1=10초=10042(ms) 리턴)  옹.
                val fileDuration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                Log.d(TAG, "downloadedRtSearcher: fileName= ${f.name}, fileDuration=$fileDuration")
                if(fileDuration==null) {
                    Log.d(TAG, "downloadedRtSearcher: Possible Corrupted file. Filename=${f.name}")
                    f.delete()
                }

                //2) hyphen(-) 포함이거나/'p' 가 없거나!/사이즈가=0 이면 => 삭제 // todo: 확장자명이 .rta 가 아녀도 삭제! (현재는 확장자 mp3 등 상관 없이 허용)
                if(f.name.contains('-')||!f.name.contains('p')||f.length()==0L) {
                    Log.d(TAG, "!!! downloadedRtSearcher: ${f.name}")
                    if(f.length()==0L) {
                        Log.d(TAG, "downloadedRtSearcher: file size prob 0? Filesize=${f.length()}")
                    }
                    f.delete()
                }
                //3) Album MetaData (제목, TrId) 찾기. 앨범 아트는 AlarDetailsFrag 에서 찾아줌. 4) 번에서 이걸 RingtoneClass 로 만들어줌.
                    // 3-a) 제목
                    val rtTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)

                    //3-b) TrId 찾기
                    val trIDString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)

                    //3-c) mp3 File uri(경로)
                        // a)File Path 를 uri 로 변환
                    val fileUri = Uri.parse(f.path.toString())
                    //3-d) artFile Path(String) ** 아래 readArtOnDisk() 가 앱 시작과 동시에 실행됨. 다 되었다는 가정하에 여기서 찾지만. Path 가 아직 없는경우에 보완책
                    val artFilePath = onDiskArtMap[trIDString] //trIDString & artFilePath 둘 다 nullable String


                // 4) RtWithAlbumArt Class 로 만들어서 리스트(onDiskRtList)에 저장
                val onDiskRingtone = RtWithAlbumArt(trIDString, rtTitle= rtTitle, audioFilePath = f.path, fileName = f.name, artFilePathStr = artFilePath) // 못 찾을 경우 default 로 일단 trid 는 모두 -20 으로 설정
                onDiskRingtoneList.add(onDiskRingtone)
                Log.d(TAG, " downloadedRtSearcher: \n[ADDING TO THE LIST]  *** Title= $rtTitle, trId=$trIDString, \n *** file.name=${f.name} // file.path= ${f.path.toString()} //\n artFilePath=$artFilePath,  uri=$fileUri")

                // 해당 trID의 artFilePath 가 MAP 에 등록되어있지 않은 경우. (User 가 지웠거나 기타 등등..)
                if(artFilePath.isNullOrEmpty()) {
                    extractArtFromSingleRta(trIDString, fileUri)

                }
            }// for loop 끝.
            //Log.d(TAG, "searchFile: file Numbers= $numberOfFiles")
        }
        Log.d(TAG, "downloadedRtSearcher: returning RT List!!")
        return onDiskRingtoneList

    }
    fun mergeList() {}

    // 위의 rtOnDiskSearcher() 에서 받음 리스트로 a) album art 가 있는지 체크 -> 있는 놈 경로는 xx Uri List 에 저장 b) albumArt 가 없으면 -> 생성!-> 디스크에 저장.
    fun readAlbumArtOnDisk() {
    // A) Disk 에 있는 ringTone 의 Album Art 가 디스크에 저장되어 있는지 체크 -> 있으면 xx Uri List 에 저장 (추후 Glide 로 그래픽 로딩 예정)
        // A-1) /.AlbumArt 폴더의 리스트 확인

        // A-1-a)만약 /.AlbumArt 폴더가 없을때는 폴더를 생성
        if(!artDir.exists()) {
            Log.d(TAG, "readAlbumArtOnDisk: Hey! Folder $artDir doesn't exist. We'll create One!@~")
            artDir.mkdir()
        }

        // A-1-b)폴더는 있는데 그 안에 아무 파일이 없을때..(** 앱 신규 설치시**)
        if(artDir.listFiles().isNullOrEmpty()) {
            Log.d(TAG, "readAlbumArtOnDisk: NO Album Art graphic FILES INSIDE THE FOLDER!. Probably the first time opening this app?")
            downloadedRtSearcher()
        }
        // todo: 쓸데없는 파일 있으면 삭제..
        // A-1-c)폴더에 파일이 있을때..
        if(artDir.listFiles() != null)
        {
            // ./AlbumArt 폴더에 있는 xxx.art 파일 for loop
            for(artFile in artDir.listFiles())
            {
                val fullPathOfArtFile: String = topFolder+ ART_FOLDER+ File.separator + artFile.name
                //val artUri = Uri.parse(artFile.path.toString())
                val trkId = artFile.nameWithoutExtension // 모든 앨범아트는 RT 의 TrkId 값.art 로 설정해야함! 파일명의 앞글자만 딴것. ex) 01

                onDiskArtMap[trkId] = artFile.path // MAP 에 저장! <trkId, Uri>
                Log.d(TAG, "readAlbumArtOnDisk: added artFilePath(${artFile.path}) to onDiskArtMap=> $onDiskArtMap")
            }// for loop 끝.

        }


    }

    // AlarmDetailsFragment> Line 385 에서 호출 (Details Frag 열었을 때 동그란 Frame 안에 있는 Album Art 사진에 현재 RT 의 경로를 전달)
    fun getArtFilePath(trkId: String?): String? = onDiskArtMap[trkId]

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
            Log.d(TAG, "saveBmpToJpgOnDisk: trkId & bitmap are EMPTY! NULL!! ZILCH")
            return
        }


        // Initialize a new file instance to save bitmap object
        val dir = artDir
        if(!dir.exists()) {
            dir.mkdirs()
        }
        val savedToDiskFile = File(dir,"${trkId}.art") // JPG 파일 포맷인데 우리는 그냥 .art 로 임의로 사용 (모바일에서 자동 검색되서 뜨는것 막는 용도도 있음..)

        try{
            // Compress the bitmap and save in jpg format
            val stream: OutputStream = FileOutputStream(savedToDiskFile)
            bitmap?.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
            // 이제는 파일 추출했으니
            onDiskArtMap[trkId] = savedToDiskFile.path // a) onDiskArtMap 에 artPath 를 기록
            // b)onDiskRtList 에서 RtWithAlbumArt object 를 찾아서 albumArtPath 를 정리해줌 -> 그래야 Spinner 에 뜨지!!
            val index: Int = onDiskRingtoneList.indexOfFirst { rt -> rt.trIdStr == trkId } // 동일한 rt.trId 를 갖는 놈의 인덱스를 onDiskRtList 에서 찾기
            Log.d(TAG, "saveBmpToJpgOnDisk: index of missing artPath ins onDiskRtList=$index")
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
}