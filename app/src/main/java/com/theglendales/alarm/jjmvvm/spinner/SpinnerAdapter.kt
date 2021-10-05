package com.theglendales.alarm.jjmvvm.spinner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjadapters.GlideApp
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjmvvm.util.RtWithAlbumArt

private const val TAG="SpinnerAdapter"
class SpinnerAdapter(val context: Context) : BaseAdapter() {

    companion object{
        val rtOnDiskList= mutableListOf<RtWithAlbumArt>()

    }
    private val myDiskSearcher: DiskSearcher by globalInject()

    fun updateList(rtOnDiskListReceived: MutableList<RtWithAlbumArt>) {
        //Log.d(TAG, "updateList: called. rtOnDiskListReceived=$rtOnDiskListReceived")
        rtOnDiskList.clear()
        for(i in 0 until rtOnDiskListReceived.size) {
            rtOnDiskList.add(rtOnDiskListReceived[i])
        }
        //Log.d(TAG, "updateList: done..!! rtOnDiskList=$rtOnDiskList")
        
    }

    override fun getCount(): Int {
        //Log.d(TAG, "getCount: ${rtOnDiskList.size}")
        return rtOnDiskList.size
    }

    override fun getItem(position: Int): Any {
        //Log.d(TAG, "getItem: position=$position")
        return position
    }

    override fun getItemId(position: Int): Long {
        //Log.d(TAG, "getItemId: position.toLong= ${position.toLong()}")
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        //Log.d(TAG, "getView: called.")
         // ** !! 자원을 재사용할때는 convertView 가 null 이 아닌 값으로 들어옴!!! !! **
        val view: View
        val spinnerVH: SpnViewHolder

        if(convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_rt_on_disk, parent, false)
            spinnerVH = SpnViewHolder()
            spinnerVH.tvName = view.findViewById<TextView>(R.id.item_name)
            spinnerVH.ivArtSmall = view.findViewById<ImageView>(R.id.item_image_small) // 우리가 앨범아트 넣을 imageView .. mp3 메타데이터에서 찾아서 넣음.

            view.tag = spinnerVH // view 의 tag 를 viewHolder 로 설정
        } else { // 이미 만들어진 view 가 있으므로, tag 를 통해 불러와서 대체한다.
            spinnerVH = convertView.tag as SpnViewHolder
            view = convertView
        }


        spinnerVH.tvName!!.text = rtOnDiskList[position].rtTitle // 제목
        val trackId= rtOnDiskList[position].trIdStr // 아쉽게도 스트링임.
        val mp3FileUri = rtOnDiskList[position].audioFileuri

        Log.d(TAG, "getView: rtTitle= ${rtOnDiskList[position].rtTitle}, trId= $trackId")



        //(이미 변환 완료된 bmp 가 MAP 에 등록되어 있다면 -> Glide 로 rt 앨범아트 보여주기

        GlideApp.with(context).load(albumArtLoader(fileUri = mp3FileUri)).centerCrop()
            .error(R.drawable.errordisplay)
            .placeholder(R.drawable.placeholder).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    Log.d(TAG, "onLoadFailed: Glide load failed!. Message: $e")
                    return false
                }

                // (여러 ViewHolder 를 로딩중인데) 현재 로딩한 View 에 Glide 가 이미지를 성공적으로 넣었다면.
                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?
                                             , dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    Log.d(TAG,"onResourceReady: Glide loading success! trId: $trackId, Position: $position") // debug 결과 절대 순.차.적으로 진행되지는 않음!

                    return false
                }
            }).into(spinnerVH.ivArtSmall!!)



        return view
    }


    // 디스크에 있는 ringtone File (mp3) 의 위치(uri) 를 통해 AlbumArt 를 추출 !
    // todo: 여기서 다루는 BitMap 들이 제법 memory 를 차지할 수 있을텐데..TEST 필요..? details Frag 나갈때 Glide 제거?
    fun albumArtLoader(fileUri: Uri): Bitmap? {
        val mmr =  MediaMetadataRetriever()

        try { // 미디어 파일이 아니면(즉 Pxx.rta 가 아닌 파일은) setDataSource 하면 crash 남! 따라서 try/catch 로 확인함.
            mmr.setDataSource(context,fileUri)
        }catch (er:Exception) {
            Log.d(TAG, "error mmr.setDataSource")
        }
        // Album Art
        val artBytes: ByteArray? = mmr.embeddedPicture // returns null if no such graphic is found.
        var albumArt: Bitmap? = null


        if(artBytes!=null)
        {
            try {
                // Sol1) AlarmsListActivity 가 시작되었을때 진작에 여기 bitMap 들을 로딩->메모리에 띄워놓기.
                // Sol2) 만약 [trkId,Boolean] map 에서 이미 해당 trkId = true 로 등록되어있으면 -> decode 하지말고 그냥 여기서 멈춰!! => Memory 에 허튼 BMP decode 된 놈들 떠다니는걸 막기 위해.
                albumArt = BitmapFactory.decodeByteArray(artBytes,0, artBytes.size)
                Log.d(TAG, "albumArtLoader: successfully added bitmap. albumArt=$albumArt")
                return albumArt
                // todo : Clean from memory

            }catch (e: Exception) {
                Log.d(TAG, "albumArtLoader: error trying to retrieve Image. Error=$e")
            }
        }
        return null
    }

    private class SpnViewHolder {
        var tvName: TextView? = null
        var ivArtSmall: ImageView? = null

    }
}