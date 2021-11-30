package com.theglendales.alarm.jjadapters

import android.app.Activity
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.theglendales.alarm.R
import com.theglendales.alarm.jjmvvm.JjRtPickerVModel
import com.theglendales.alarm.jjmvvm.util.RtWithAlbumArt
import kotlin.math.sign

private const val TAG="RtPickerAdapter"
class RtPickerAdapter(var rtaArtPathList: MutableList<RtWithAlbumArt>,
                      private val receivedActivity: Activity,
                      private val rcVModelVModel: JjRtPickerVModel) : RecyclerView.Adapter<RtPickerAdapter.RtPickerVHolder>()
{
// Override Methods
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RtPickerVHolder {
        val xmlToViewObject = LayoutInflater.from(receivedActivity).inflate(R.layout.jj_rtpicker_singleslot, parent, false)
        return RtPickerVHolder(xmlToViewObject)
    }

    override fun onBindViewHolder(holder: RtPickerVHolder, position: Int) {
        val currentRtItem = rtaArtPathList[position]
        val rowRtTitle = rtaArtPathList[position].rtTitle
        holder.tvRtTitle.text = rowRtTitle

        //AlbumArt 보여주기
        GlideApp.with(receivedActivity).load(currentRtItem.artFilePathStr).centerCrop().error(R.drawable.errordisplay)
            .placeholder(R.drawable.placeholder).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?,model: Any?,target: Target<Drawable>?,isFirstResource: Boolean): Boolean {
                    Log.d(TAG, "onLoadFailed: [RtPicker] Failed to show AlbumArt")
                    return false
                }

                override fun onResourceReady(resource: Drawable?,model: Any?,target: Target<Drawable>?,dataSource: DataSource?,isFirstResource: Boolean): Boolean {
                    return false
                }
            }).into(holder.ivRtAlbumArt)
    }

    override fun getItemCount(): Int {
        return rtaArtPathList.size
    }
// My Methods
    fun updateRcV(newList: MutableList<RtWithAlbumArt>) {
        val oldList= rtaArtPathList // Constructor 로 받은 리스트
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(JjDiffCallback(oldList, newList))
        rtaArtPathList = newList
        Log.d(TAG, "updateRcV: !!!!!!! rtaArtPathList.size (AFTER DIFF UTIL) = ${rtaArtPathList.size}")
        diffResult.dispatchUpdatesTo(this)
    }
// DiffUtil Class
    class JjDiffCallback(var oldList: MutableList<RtWithAlbumArt>, var newList: MutableList<RtWithAlbumArt>) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // check if two items represent the same item. 흠.. 다 똑같은지말고 id 만 같아도 true 라는듯..
        return (oldList[oldItemPosition].rtTitle == newList[newItemPosition].rtTitle)
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // 1차 선발된 놈들을 2차로 여기서 아예 동일한 놈인지(data 로 파악) 판명. // 모든 Field 가 아예 똑같은건지 확인 (rtTitle, rtaFilePath 등..)
        return (oldList[oldItemPosition] == newList[newItemPosition])
    }

}
// ViewHolder

    inner class RtPickerVHolder(xmlToView: View) : RecyclerView.ViewHolder(xmlToView), View.OnClickListener {
        val tvRtTitle: TextView = xmlToView.findViewById(R.id.tv_singleSlot_rtPicker)
        val ivRtAlbumArt: ImageView = xmlToView.findViewById(R.id.iv_singleSlot_albumArt)
        // selector

        override fun onClick(v: View?) {
            //TODO("Not yet implemented")
        }

    }


}