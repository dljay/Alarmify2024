package com.theglendales.alarm.jjmvvm.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.File

private const val TAG="DiskSearcher"

class DiskSearcher(val context: Context)
{
    val emptyList = mutableListOf<RtWithAlbumArt>()
    val onDiskRtList = mutableListOf<RtWithAlbumArt>()


    fun rtAndArtSearcher(): MutableList<RtWithAlbumArt>
    {
        onDiskRtList.clear() // DetailsFrag 다시 들어왔을 때 먼저 클리어하고 시작.
        val emptyUriList = listOf<Uri>()

        val folder = context.getExternalFilesDir(null)!!.absolutePath
        val myDir = File(folder,"/.AlarmRingTones")
        // 만약 폴더가 없을때는 폴더를 생성
        if(!myDir.exists()) {
            Log.d(TAG, "rtAndArtSearcher: Folder $myDir doesn't exist. We'll create one")
            myDir.mkdir()
        }
        // 폴더는 있는데 파일이 없을때.. 그냥 return
        if(myDir.listFiles().isNullOrEmpty()) {
            Log.d(TAG, "rtAndArtSearcher: NO FILES INSIDE THE FOLDER!")
            return emptyList
        }
        if(myDir.listFiles() != null)
        {
            for(f in myDir.listFiles())
            {
                val mmr =  MediaMetadataRetriever()

                val actualFileForMmr = folder+"/.AlarmRingTones"+ File.separator + f.name

                try { // 미디어 파일이 아니면(즉 Pxx.rta 가 아닌 파일은) setDataSource 하면 crash 남! 따라서 try/catch 로 확인함.
                    mmr.setDataSource(actualFileForMmr)
                }catch (er:Exception) {
                    Log.d(TAG, "rtAndArtSearcher: unable to run mmr.setDataSource for the file=${f.name}. WE'LL DELETE THIS PIECE OF SHIT!")
                    f.delete()
                    return emptyList // todo: 여기서 에러났다고 무조건 이거 return 하면 안될듯..
                }

                //1) 파일이 제대로 된 mp3 인지 곡 길이(duration) return 하는것으로 확인. (Ex. p1=10초=10042(ms) 리턴)  옹.
                val fileDuration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                Log.d(TAG, "rtAndArtSearcher: fileName= ${f.name}, fileDuration=$fileDuration")
                if(fileDuration==null) {
                    Log.d(TAG, "rtAndArtSearcher: Possible Corrupted file. Filename=${f.name}")
                    f.delete()
                }

                //2) hyphen(-) 포함이거나/'p' 가 없거나!/사이즈가=0 이면 => 삭제
                if(f.name.contains('-')||!f.name.contains('p')||f.length()==0L) {
                    Log.d(TAG, "!!! rtSearcher: ${f.name}")
                    if(f.length()==0L) {
                        Log.d(TAG, "rtAndArtSearcher: filesize prob 0? Filesize=${f.length()}")
                    }
                    f.delete()


                }
                //3) Album MetaData (제목, TrId) 찾기. 앨범 아트는 AlarDetailsFrag 에서 찾아줌. 4) 번에서 이걸 RingtoneClass 로 만들어줌.

                    // 3-b) 제목
                    val rtTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)

                    //3-c) TrId 찾기
                    val trIDString = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)


                    // 3-d) mp3 File uri(경로) - **  ** -> 이걸 추후에 앨범아트 찾는 용도로 사용.
                        // a)File Path 를 uri 로 변환
                    val fileUri = Uri.parse(f.path.toString())
                        // b)RtWithAlbumArt 로 만들어서 list 에 저장.

                // 4) Ringtone Class 로 만들어주기
                val onDiskRingtone = RtWithAlbumArt(trIDString, rtTitle= rtTitle, uri = fileUri, fileName = f.name) // 못 찾을 경우 default 로 일단 trid 는 모두 -20 으로 설정
                onDiskRtList.add(onDiskRingtone)
                Log.d(TAG, "rtSearcher: [ADDING TO THE LIST] \n *** Title= $rtTitle, trId=$trIDString, \n *** file.name=${f.name} // file.path= ${f.path} // uri=$fileUri")
            }
            //Log.d(TAG, "searchFile: file Numbers= $numberOfFiles")
        }
        return onDiskRtList
    }

    fun metaInfoChooChool() {

    }

}