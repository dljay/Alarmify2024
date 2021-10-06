package com.theglendales.alarm.jjmvvm.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.File

private const val TAG="DiskSearcher"
private const val RT_FOLDER="/.AlarmRingTones"
private const val ART_FOLDER="/.AlbumArt"


class DiskSearcher(val context: Context)
{
    val emptyList = mutableListOf<RtWithAlbumArt>()
    val onDiskRingtoneList = mutableListOf<RtWithAlbumArt>()
    val onDiskArtMap: HashMap<String?, String?> = HashMap() // <trkId, 앨범아트 경로>

    val folder = context.getExternalFilesDir(null)!!.absolutePath
    val alarmRtDir = File(folder, RT_FOLDER)
    val artDir = File(folder, ART_FOLDER)

    fun rtOnDiskSearcher(): MutableList<RtWithAlbumArt>
    {
        onDiskRingtoneList.clear() // DetailsFrag 다시 들어왔을 때 먼저 클리어하고 시작.
        val emptyUriList = listOf<Uri>()


        // 만약 폴더가 없을때는 폴더를 생성
        if(!alarmRtDir.exists()) {
            Log.d(TAG, "rtAndArtSearcher: Folder $alarmRtDir doesn't exist. We'll create one")
            alarmRtDir.mkdir()
        }
        // 폴더는 있는데 파일이 없을때.. 그냥 return
        if(alarmRtDir.listFiles().isNullOrEmpty()) {
            Log.d(TAG, "rtAndArtSearcher: NO FILES INSIDE THE FOLDER!")
            return emptyList
        }
        // 폴더에 파일이 있을때..
        if(alarmRtDir.listFiles() != null)
        {
            for(f in alarmRtDir.listFiles())
            {
                val mmr =  MediaMetadataRetriever()

                val actualFileForMmr = folder+"/.AlarmRingTones"+ File.separator + f.name

                try { // 미디어 파일이 아니면(즉 Pxx.rta 가 아닌 파일은) setDataSource 하면 crash 남! 따라서 try/catch 로 확인함.
                    mmr.setDataSource(actualFileForMmr)
                }catch (er:Exception) {
                    Log.d(TAG, "rtAndArtSearcher: unable to run mmr.setDataSource for the file=${f.name}. WE'LL DELETE THIS PIECE OF SHIT!")
                    f.delete()
                    //return emptyList //
                }

                //1) 파일이 제대로 된 mp3 인지 곡 길이(duration) return 하는것으로 확인. (Ex. p1=10초=10042(ms) 리턴)  옹.
                val fileDuration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                Log.d(TAG, "rtAndArtSearcher: fileName= ${f.name}, fileDuration=$fileDuration")
                if(fileDuration==null) {
                    Log.d(TAG, "rtAndArtSearcher: Possible Corrupted file. Filename=${f.name}")
                    f.delete()
                }

                //2) hyphen(-) 포함이거나/'p' 가 없거나!/사이즈가=0 이면 => 삭제 // todo: 확장자명이 .rta 가 아녀도 삭제! (현재는 확장자 mp3 등 상관 없이 허용)
                if(f.name.contains('-')||!f.name.contains('p')||f.length()==0L) {
                    Log.d(TAG, "!!! rtSearcher: ${f.name}")
                    if(f.length()==0L) {
                        Log.d(TAG, "rtAndArtSearcher: filesize prob 0? Filesize=${f.length()}")
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
                    val artFilePath = onDiskArtMap[trIDString]
                    // 해당 trID의 artFilePath 가 MAP 에 등록되어있지 않은 경우. (User 가 지웠거나 기타 등등..)
                    if(artFilePath.isNullOrEmpty()) {
                        // todo: 새로 BitMap 변환하여 artFile 생성
                    }

                // 4) RtWithAlbumArt Class 로 만들어서 리스트(onDiskRtList)에 저장
                val onDiskRingtone = RtWithAlbumArt(trIDString, rtTitle= rtTitle, audioFileUri = fileUri, fileName = f.name, artFilePathStr = artFilePath) // 못 찾을 경우 default 로 일단 trid 는 모두 -20 으로 설정
                onDiskRingtoneList.add(onDiskRingtone)
                Log.d(TAG, "rtSearcher: [ADDING TO THE LIST]  *** Title= $rtTitle, trId=$trIDString, \n *** file.name=${f.name} // file.path= ${f.path} //\n artFilePath=$artFilePath,  uri=$fileUri")
            }// for loop 끝.
            //Log.d(TAG, "searchFile: file Numbers= $numberOfFiles")
        }
        return onDiskRingtoneList
    }

    // 위의 rtOnDiskSearcher() 에서 받음 리스트로 a) album art 가 있는지 체크 -> 있는 놈 경로는 xx Uri List 에 저장 b) albumArt 가 없으면 -> 생성!-> 디스크에 저장.
    fun readAlbumArtOnDisk() {
    // A) Disk 에 있는 ringTone 의 Album Art 가 디스크에 저장되어 있는지 체크 -> 있으면 xx Uri List 에 저장 (추후 Glide 로 그래픽 로딩 예정)
        // A-1) /.AlbumArt 폴더의 리스트 확인

        // A-1-a)만약 /.AlbumArt 폴더가 없을때는 폴더를 생성
        if(!artDir.exists()) {
            Log.d(TAG, "artCheckOrCreate: Hey! Folder $artDir doesn't exist. We'll create One!@~")
            artDir.mkdir()
        }
        // A-1-b)폴더는 있는데 그 안에 아무 파일이 없을때..
        if(artDir.listFiles().isNullOrEmpty()) {
            Log.d(TAG, "artCheckOrCreate: NO Album Art graphic FILES INSIDE THE FOLDER!")
            // todo: 오디오 파일에서 추출하여 .art 파일 생성
        }
        // A-1-c)폴더에 파일이 있을때..
        if(artDir.listFiles() != null)
        {
            // ./AlbumArt 폴더에 있는 xxx.art 파일 for loop
            for(artFile in artDir.listFiles())
            {
                val fullPathOfArtFile: String = folder+ ART_FOLDER+ File.separator + artFile.name
                //val artUri = Uri.parse(artFile.path.toString())
                val trkId = artFile.nameWithoutExtension // 모든 앨범아트는 RT 의 TrkId 값.art 로 설정해야함! 파일명의 앞글자만 딴것. ex) 01

                onDiskArtMap[trkId] = artFile.path // MAP 에 저장! <trkId, Uri>
                Log.d(TAG, "artOnDiskSearch: added artFilePath(${artFile.path}) to onDiskArtMap=> $onDiskArtMap")
            }// for loop 끝.

        }
        // A-2) onDiskRingToneList 와 onDiskArtList 를 대조
//        for(i in 0 until rtWAlbumArtList.size) {
//            rtWAlbumArtList[i].artFilePathStr =
//        }
        //rtWAlbumArtList.filter { rtObj -> rtObj.rtTitle  }

        // todo: 쓸데없는 파일 있으면 삭제..
    }

    // AlarmDetailsFragment> Line 385 에서 호출 (Details Frag 열었을 때 동그란 Frame 안에 있는 Album Art 사진)
    fun getArtFilePath(trkId: String?): String? = onDiskArtMap[trkId]

}