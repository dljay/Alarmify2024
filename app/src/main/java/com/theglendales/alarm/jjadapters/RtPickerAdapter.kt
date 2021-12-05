package com.theglendales.alarm.jjadapters

import android.app.Activity
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.theglendales.alarm.R
import com.theglendales.alarm.jjmvvm.JjRtPickerVModel
import com.theglendales.alarm.jjmvvm.mediaplayer.MyMediaPlayer
import com.theglendales.alarm.jjmvvm.util.RtWithAlbumArt

// RcView 싱글 Selection 참고: https://stackoverflow.com/questions/28972049/single-selection-in-recyclerview

private const val TAG="RtPickerAdapter"
class RtPickerAdapter(var rtaArtPathList: MutableList<RtWithAlbumArt>,
                      private val receivedActivity: Activity,
                      private val rtPickerVModel: JjRtPickerVModel,
                      private val mediaPlayer: MyMediaPlayer) : RecyclerView.Adapter<RtPickerAdapter.RtPickerVHolder>()
{
    var lastCheckedPos = -1 // CheckBox 로 선택한 RT 의 Pos 기록.

// Override Methods
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RtPickerVHolder {
        val xmlToViewObject = LayoutInflater.from(receivedActivity).inflate(R.layout.jj_rtpicker_singleslot, parent, false)
        return RtPickerVHolder(xmlToViewObject)
    }

    override fun onBindViewHolder(holder: RtPickerVHolder, position: Int) {
        val currentRtItem = rtaArtPathList[position]
        val rowRtTitle = rtaArtPathList[position].rtTitle

        holder.tvRtTitle.text = rowRtTitle
        holder.radioBtn.isChecked = (position == lastCheckedPos) // ex) radioBtn.isChecked= true (만약 현재 lastCheckedPos 값이 설정하는 row 의 BindViewHolder 의 값과 같으면)

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
        val llEntireRow: LinearLayout = xmlToView.findViewById(R.id.ll_entireSingleSlot) // Row 전체를 감싸는 LinearLayout
        val tvRtTitle: TextView = xmlToView.findViewById(R.id.tv_singleSlot_rtPicker)
        val ivRtAlbumArt: ImageView = xmlToView.findViewById(R.id.iv_singleSlot_albumArt)
        val radioBtn: RadioButton = xmlToView.findViewById(R.id.rb_singleSlot_selector)
        // selector

        init {
            //radioBtn.setOnClickListener(this)
            llEntireRow.setOnClickListener(this)

        }

        override fun onClick(v: View?) {
        // RadioBtn 표시 관련
            var copyOfLastCheckedPos = lastCheckedPos
            lastCheckedPos = adapterPosition // 클릭하는 순간 Adapter 포지션을 lastCheckedPos 으로 남김.
            notifyItemChanged(copyOfLastCheckedPos) // 즉 이전에 선택되었던 row 의 RadioBtn 은 모두 False 로
            notifyItemChanged(lastCheckedPos) // 그리고 지금 선택된 row 의 RadioBtn 만 활성화.

        // LiveData 업데이트 - Intent 에 TrTitle, RTA/ArtFilePath 전달 용도
            if(rtaArtPathList.size > 0 && lastCheckedPos < rtaArtPathList.size) {
                val rtWithAlbumArtObj = rtaArtPathList[lastCheckedPos]
                rtPickerVModel.updateLiveData(rtWithAlbumArtObj)
        // 음악 바로 재생 (여기서 재생 후 STATUS.ENUM 상태에 따라 LiveData 로 전달
                val rtaFilePath = rtWithAlbumArtObj.audioFilePath
                mediaPlayer.prepMusicPlayLocal(rtaFilePath, true)

            }


        }


    }


}