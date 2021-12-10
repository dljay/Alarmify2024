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
import com.theglendales.alarm.presenter.AlarmDetailsFragment

// RcView 싱글 Selection 참고: https://stackoverflow.com/questions/28972049/single-selection-in-recyclerview

private const val TAG="RtPickerAdapter"
class RtPickerAdapter(var rtaArtPathList: MutableList<RtWithAlbumArt>,
                      private val receivedActivity: Activity,
                      private val rtPickerVModel: JjRtPickerVModel,
                      private val mediaPlayer: MyMediaPlayer) : RecyclerView.Adapter<RtPickerAdapter.RtPickerVHolder>()
{

    var lastUserCheckedPos = -1 // RadioBtn 으로 선택한 RT 의 Pos 기록.
    var selectedRadioBtn: RadioButton? = null // user 가 클릭하는 Holder>Linearlayout>RadioButton 을 이 변수에 저장.

// Override Methods
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RtPickerVHolder {
        val xmlToViewObject = LayoutInflater.from(receivedActivity).inflate(R.layout.jj_rtpicker_singleslot, parent, false)
        return RtPickerVHolder(xmlToViewObject)
    }

    override fun onBindViewHolder(holder: RtPickerVHolder, position: Int) {
        val currentRtItem = rtaArtPathList[position]
        val currentRtTitle = rtaArtPathList[position].rtTitle
        val currentRtFileName = rtaArtPathList[position].fileName
        val holderRadioBtn = holder.radioBtn

        holder.tvRtTitle.text = currentRtTitle
    //A. **[최초 RcView row 생성시 'DefaultFrag' 에서 설정되어있던 Ringtone 의 RadioBtn 체크 표시해주기]**
        // 기존에 User 가 설정해놓은 (DetailsFrag 에서 보여진) ringtone 였으면 자동으로 선택된 상태로 표시.. 유저가 다른 RT 를 한번이라도 클릭했다면 lastUserCheckedPos != -1
        if(lastUserCheckedPos==-1 && currentRtFileName== AlarmDetailsFragment.detailFragDisplayedRtFileName) {
            // User 가 한번이라도 다른 RT 를 클릭해서 음악 들어보고 했다면. 이제부터는 DetailsFrag 에서 기존에 설정해놓았떤 RT 의 Radio Box 표시는 무시! (왜냐면 lastUserCheckedPos !=-1 이니깐)
            Log.d(TAG, "onBindViewHolder: activate RadioBox!! currentRtFileName=$currentRtFileName")
            //ArtPathList 의 해당 포지션에 있는 rtWithAlbumArtObj 의 isRadioBtnChecked variable 을 true 로 변경.
            //rtaArtPathList[position].isRadioBtnChecked = true
            selectedRadioBtn = holderRadioBtn
            selectedRadioBtn!!.isChecked = true

        }
    //B. (앞으로 유저가 클릭하게 될) 전체를 감싸는 linear Layout 에 대한 onClickListener => (참고: 얼마든지 rcView 를 recycle 해도 클릭하지 앟는 이상 RadioBtn 의 값은 변하지 않음!)
        holder.llEntireRow.setOnClickListener {
            Log.d(TAG, "onBindViewHolder: lastuserCheckedPos=$lastUserCheckedPos")
            // <0> lastUserCheckedPos 을 업데이트 (이제는 더 이상 -1 이 아니다!!)
                lastUserCheckedPos = position
            // <1> LiveData 업데이트 - Intent 에 TrTitle, RTA/ArtFilePath 전달 용도
                val rtWithAlbumArtObj = rtaArtPathList[position]
                rtPickerVModel.updateLiveData(rtWithAlbumArtObj)
            // <2> RadioBtn 표시 관련
                //a) rtaArtPathList 의 모든 isRadioBtnChecked variable 을 'false' 로 변경
                for(i in 0 until rtaArtPathList.size) {
                    rtaArtPathList[i].isRadioBtnChecked = false
                }
                //b) (이제) User 가 클릭한 놈의 isRadioBtnChecked = true 로 변경 (클릭한 position 에 기반)
                rtaArtPathList.get(lastUserCheckedPos).isRadioBtnChecked = true
                //c) 만약 현재 클릭하는 llEntireRow 안의 'Radio Button' 이 기존에 선택해놨던 Radio Button 과 '다른 놈'이라면  (기존에 선택되어있던 Radio Btn 의 선택을 해제)
                // selectedRadioButton = 기존 선택되있던 놈으로 지정되어 있겠지..
                if(selectedRadioBtn != null && !holderRadioBtn.equals(selectedRadioBtn)) {
                    selectedRadioBtn!!.isChecked = false
                }
                //d) Replace the previous selected radio button with the current (clicked) one, and "check" it
                selectedRadioBtn = holderRadioBtn
                selectedRadioBtn!!.isChecked = true

            // <3> 음악 바로 재생 (여기서 재생 후 STATUS.ENUM 상태에 따라 LiveData 로 전달
                val rtaFilePath = rtWithAlbumArtObj.audioFilePath
                mediaPlayer.prepMusicPlayLocalSrc(rtaFilePath, true)

        }

    //C.AlbumArt 보여주기
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

    inner class RtPickerVHolder(xmlToView: View) : RecyclerView.ViewHolder(xmlToView) {
        val llEntireRow: LinearLayout = xmlToView.findViewById(R.id.ll_entireSingleSlot) // Row 전체를 감싸는 LinearLayout
        val tvRtTitle: TextView = xmlToView.findViewById(R.id.tv_singleSlot_rtPicker)
        val ivRtAlbumArt: ImageView = xmlToView.findViewById(R.id.iv_singleSlot_albumArt)
        val radioBtn: RadioButton = xmlToView.findViewById(R.id.rb_singleSlot_selector)
        // selector


    }


}