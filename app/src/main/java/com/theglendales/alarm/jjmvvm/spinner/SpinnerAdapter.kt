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
import com.theglendales.alarm.jjdata.GlbVars
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjmvvm.util.RtWithAlbumArt
import java.io.File

private const val TAG="SpinnerAdapter"
class SpinnerAdapter(val context: Context) : BaseAdapter() {

    private val myDiskSearcher: DiskSearcher by globalInject()
    var rtOnDiskList= listOf<RtWithAlbumArt>()


    fun updateList(rtOnDiskListReceived: List<RtWithAlbumArt>) {
        rtOnDiskList = rtOnDiskListReceived
    }

    override fun getCount(): Int {
        Log.d(TAG, "getCount: ")
        return rtOnDiskList.size
    }

    override fun getItem(position: Int): Any {
        Log.d(TAG, "getItem: position=$position")
        return position
    }

    override fun getItemId(position: Int): Long {
        Log.d(TAG, "getItemId: position.toLong= ${position.toLong()}")
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        //Log.d(TAG, "getView: called.")
        val rootView: View = LayoutInflater.from(context).inflate(R.layout.item_rt_on_disk, parent, false)

        val tvName = rootView.findViewById<TextView>(R.id.item_name)
        tvName.text = rtOnDiskList[position].rtTitle
        val trackId= rtOnDiskList[position].trId
        val mp3FileUri = rtOnDiskList[position].uri

        Log.d(TAG, "getView: rtTitle= ${rtOnDiskList[position].rtTitle}, trId= $trackId")

        val ivImage = rootView.findViewById<ImageView>(R.id.item_image) // 우리가 앨범아트 넣을 imageView .. mp3 메타데이터에서 찾아서 넣음.

        //Glide 로 rt 앨범아트 보여주기
        GlideApp.with(context).load(albumArtLoader(mp3FileUri)).centerCrop()
            .error(R.drawable.errordisplay)
            .placeholder(R.drawable.placeholder).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?,model: Any?,target: Target<Drawable>?,isFirstResource: Boolean): Boolean {
                    Log.d(TAG, "onLoadFailed: Glide load failed!. Message: $e")
                    return false
                }

                // (여러 ViewHolder 를 로딩중인데) 현재 로딩한 View 에 Glide 가 이미지를 성공적으로 넣었다면.
                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?
                                             , dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    Log.d(TAG,"onResourceReady: Glide loading success! trId: $trackId, Position: $position") // debug 결과 절대 순.차.적으로 진행되지는 않음!

                    return false
                }
            }).into(ivImage)

//        tvName.setText(rtOnDiskList.get(position).name)
//        ivImage.setImageResource(rtOnDiskList.get(position).imageInt)

        return rootView
    }

    // 디스크에 있는 ringtone File (mp3) 의 위치(uri) 를 통해 AlbumArt 를 추출 !
    // todo: 여기서 다루는 BitMap 들이 제법 memory 를 차지할 수 있을텐데..
    fun albumArtLoader(fileUri: Uri): Bitmap? {
        val mmr =  MediaMetadataRetriever()

        try { // 미디어 파일이 아니면(즉 Pxx.rta 가 아닌 파일은) setDataSource 하면 crash 남! 따라서 try/catch 로 확인함.
            mmr.setDataSource(context,fileUri)
        }catch (er:Exception) {
            Log.d(TAG, "error mmr.setDataSource")
        }
        // Album Art
        val artBytes: ByteArray? = mmr.embeddedPicture
        var albumArt: Bitmap? = null

        if(artBytes!=null)
        {
            try {
                albumArt = BitmapFactory.decodeByteArray(artBytes,0, artBytes.size)
                Log.d(TAG, "rtAndArtSearcher: successfully added bitmap. albumArt=$albumArt")
                return albumArt

            }catch (e: Exception) {
                Log.d(TAG, "rtAndArtSearcher: error trying to retrieve Image. Error=$e")
            }
        }
        return null
    }
}