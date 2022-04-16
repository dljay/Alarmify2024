package com.theglendales.alarm.jjadapters

import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.theglendales.alarm.R
import com.theglendales.alarm.jjmvvm.JjRtPickerVModel
import com.theglendales.alarm.jjmvvm.mediaplayer.ExoForLocal
import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp
import com.theglendales.alarm.jjmvvm.util.RtOnThePhone
import com.theglendales.alarm.presenter.AlarmDetailsFragment
import io.gresse.hugo.vumeterlibrary.VuMeterView

// RcView 싱글 Selection 참고: https://stackoverflow.com/questions/28972049/single-selection-in-recyclerview

private const val TAG="RtPickerAdapter"
class RtPickerAdapter(var rtOnThePhoneList: MutableList<RtOnThePhone>,
                      private val receivedActivity: Activity,
                      private val rtPickerVModel: JjRtPickerVModel,
                      private val exoForLocal: ExoForLocal) : RecyclerView.Adapter<RtPickerAdapter.RtPickerVHolder>()
{

    var lastUserCheckedPos = -1 // RadioBtn 으로 선택한 RT 의 Pos 기록.
    var selectedRadioBtn: RadioButton? = null // user 가 클릭하는 Holder>Linearlayout>RadioButton 을 이 변수에 저장.

    // 현재 click 된 ViewHolder 를 여기에 저장.
    var prevClickedHolder: RtPickerAdapter.RtPickerVHolder? = null
    var clickedHolder: RtPickerAdapter.RtPickerVHolder? = null


// Override Methods
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RtPickerVHolder {
        val xmlToViewObject = LayoutInflater.from(receivedActivity).inflate(R.layout.jj_rtpicker_singleslot, parent, false)
        return RtPickerVHolder(xmlToViewObject)
    }

    override fun onBindViewHolder(holder: RtPickerVHolder, position: Int) {
        val currentRtItem = rtOnThePhoneList[position]
        val currentRtTitle = rtOnThePhoneList[position].rtTitle
        val currentRtFileName = rtOnThePhoneList[position].fileNameWithExt
        val currentRtDescription = rtOnThePhoneList[position].rtDescription
        val currentHolderTrId = rtOnThePhoneList[position].trIdStr
        val currentHolderRadioBtn = holder.radioBtn

        //Log.d(TAG, "onBindViewHolder: currentHolderTrId= $currentHolderTrId,  pos=$position, rtFileName=$currentRtFileName, rtTitle=$currentRtTitle,")

        holder.tvRtTitle.text = currentRtTitle
    //A. **[최초 RcView row 생성시 'DefaultFrag' 에서 설정되어있던 Ringtone 의 RadioBtn 체크 표시해주기]**
        // 기존에 User 가 설정해놓은 (DetailsFrag 에서 보여진) ringtone 였으면 자동으로 선택된 상태로 표시.. 유저가 다른 RT 를 한번이라도 클릭했다면 lastUserCheckedPos != -1
        if(lastUserCheckedPos==-1 && currentRtFileName== AlarmDetailsFragment.detailFragDisplayedRtFileName) {
            // User 가 한번이라도 다른 RT 를 클릭해서 음악 들어보고 했다면. 이제부터는 DetailsFrag 에서 기존에 설정해놓았떤 RT 의 Radio Box 표시는 무시! (왜냐면 lastUserCheckedPos !=-1 이니깐)
            Log.d(TAG, "onBindViewHolder: activate RadioBox!! currentRtFileName=$currentRtFileName")

            selectedRadioBtn = currentHolderRadioBtn
            selectedRadioBtn!!.isChecked = true
            lastUserCheckedPos = position

        }
    //B. (앞으로 유저가 클릭하게 될) 전체를 감싸는 linear Layout 에 대한 onClickListener => (참고: 얼마든지 rcView 를 recycle 해도 클릭하지 앟는 이상 RadioBtn 의 값은 변하지 않음!)
        holder.clEntireRow.setOnClickListener {
            Log.d(TAG, "onBindViewHolder: [BEFORE] lastuserCheckedPos=$lastUserCheckedPos")
            // <0> lastUserCheckedPos 을 업데이트 (이제는 더 이상 -1 이 아니다!!)
                lastUserCheckedPos = position
            Log.d(TAG, "onBindViewHolder: [AFTER] lastuserCheckedPos=$lastUserCheckedPos")
            // <1> LiveData 업데이트 - Intent 에 TrTitle, RTA/ArtFilePath 전달 용도
                val rtWithAlbumArtObj = rtOnThePhoneList[position]
                rtPickerVModel.updateLiveData(rtWithAlbumArtObj)
            // <2> RadioBtn 표시 관련
                //a) rtOnThePhoneList 의 모든 isRadioBtnChecked variable 을 'false' 로 변경
                for(i in 0 until rtOnThePhoneList.size) {
                    rtOnThePhoneList[i].isRadioBtnChecked = false
                }
                //b) (이제) User 가 클릭한 놈의 isRadioBtnChecked = true 로 변경 (클릭한 position 에 기반)
                rtOnThePhoneList[lastUserCheckedPos].isRadioBtnChecked = true

                //c) 만약 현재 클릭하는 llEntireRow 안의 'Radio Button' 이 기존에 선택해놨던 Radio Button 과 '다른 놈'이라면  (기존에 선택되어있던 Radio Btn 의 선택을 해제) // a) 가 있는데 이거 필요한건가..?
                // selectedRadioButton = 기존 선택되있던 놈으로 지정되어 있겠지..
                if(selectedRadioBtn != null && currentHolderRadioBtn != selectedRadioBtn) {
                    selectedRadioBtn!!.isChecked = false
                }
                //d) Replace the previous selected radio button with the current (clicked) one, and "check" it
                selectedRadioBtn = currentHolderRadioBtn
                selectedRadioBtn!!.isChecked = true

            // <3> 음악 바로 재생 (여기서 재생 후 STATUS.ENUM 상태에 따라 LiveData 로 전달
                val rtaFilePath = rtWithAlbumArtObj.audioFilePath
                exoForLocal.prepMusicPlayLocalSrc(rtaFilePath, true)

            // <4> 현재 선택된 Holder 값을 variable 에 전달
                prevClickedHolder = clickedHolder
                clickedHolder = holder
        }
    //C. 스크롤 쓱싹 위아래 하면서 Bind 할 때  (기존 선택된 트랙이면 -> vuMeter, RadioBtn Enable, 아니면 vuMeter/RadioBtn 재탕되는것 방지하기!)
        when(position) {
            lastUserCheckedPos -> { // 기존 선택된 트랙 -> enable vuMeter& RadioBtn (O)
                //Log.d(TAG, "onBindViewHolder: 이전에 선택해놓은 트랙(O)!! currentHolderTrId= $currentHolderTrId,  pos=$position, lastusercheckedpos=$lastUserCheckedPos")
                enableVm(ExoForLocal.currentPlayStatus, holder)
                holder.radioBtn.isChecked = true
            }
            else -> { // 이전에 선택해놓은 트랙이 아님!  (그러니깐 viewHolder 가 재활용되면서 vuMeter 나 RadioBtn 재활성화 시키는것 막자!) (X)
                //Log.d(TAG, "onBindViewHolder: 이전에 선택해놓은 트랙이 아님(X)!! currentHolderTrId= $currentHolderTrId,  pos=$position, lastusercheckedpos=$lastUserCheckedPos")
                holder.vuMeter.visibility = View.GONE
                holder.radioBtn.isChecked= false // Disable RadioBtn
            }
        }

    //D. Description (Intense, Gentle.. 등) 채워주기
        holder.tvRtDescription.text = currentRtDescription
    //E.AlbumArt 보여주기
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
        return rtOnThePhoneList.size
    }

// My Methods
    fun updateRcV(newList: MutableList<RtOnThePhone>) {
        val oldList= rtOnThePhoneList // Constructor 로 받은 리스트
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(JjDiffCallback(oldList, newList))
        rtOnThePhoneList = newList
        Log.d(TAG, "updateRcV: !!!!!!! rtOnThePhoneList.size (AFTER DIFF UTIL) = ${rtOnThePhoneList.size}")
        diffResult.dispatchUpdatesTo(this)
    }
// DiffUtil Class
    class JjDiffCallback(var oldList: MutableList<RtOnThePhone>, var newList: MutableList<RtOnThePhone>) : DiffUtil.Callback() {
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

// Vumeter Control -1 [클릭시 vuMeter 재생]
    fun vumeterControl(playStatus: StatusMp) {
    // 1)기존에 UI 가 작동되던 Holder 를 다 '원복' 해주기
        if(prevClickedHolder!=null) {
            prevClickedHolder!!.vuMeter.visibility = VuMeterView.GONE // VuMeter 도 안보이게 (b)
        }
    // 2) 현재 클릭된 Holder 의 UI 업데이트
        if(clickedHolder!=null) {
            when(playStatus) {
                StatusMp.BUFFERING -> {
                    clickedHolder!!.vuMeter.visibility = VuMeterView.VISIBLE
                    clickedHolder!!.vuMeter.pause()
                }
                StatusMp.READY -> {
                    //clickedHolder!!.loadingCircle.visibility = View.INVISIBLE
                    clickedHolder!!.vuMeter.visibility = VuMeterView.VISIBLE
                    clickedHolder!!.vuMeter.pause()
                }
                StatusMp.PAUSED -> {
                    clickedHolder!!.vuMeter.pause()
                }
                StatusMp.PLAY -> {
                    //clickedHolder!!.iv_Thumbnail.alpha = 0.6f
                    clickedHolder!!.vuMeter.visibility = VuMeterView.VISIBLE
                    clickedHolder!!.vuMeter.resume(true)
                }
                StatusMp.IDLE -> {}
                StatusMp.ERROR -> {}
            }
        }
    }
// Vumeter Control -2 [BindView 할 때 Holder 재활용으로 VuMeter 뜨는것 방지위해]
    private fun enableVm(currentPlayStatus: StatusMp, selectedHolder: RtPickerVHolder) {
        when(currentPlayStatus) {
            StatusMp.PLAY -> {
                //selectedHolder.iv_Thumbnail.alpha = 0.6f // 어둡게
                selectedHolder.vuMeter.visibility = VuMeterView.VISIBLE
            }
            StatusMp.PAUSED -> {
                Log.d(TAG, "enableVM: .PAUSED called for holder.hashCode= ${selectedHolder.hashCode()}")
                selectedHolder.vuMeter.visibility = VuMeterView.VISIBLE
                //selectedHolder.iv_Thumbnail.alpha = 0.6f // 어둡게
                Handler(Looper.getMainLooper()).postDelayed({
                    selectedHolder.vuMeter.pause() // EQ 막대기를 보여줘야하는데 바로 vuMeterView.pause() 때리면 아무것도 안 보임. 따라서 0.1 초 Delay 후 Pause 때림.
                }, 100)
                //selectedHolder.vuMeterView.stop(true) // 이것도 얼추 먹히긴 하는데 scroll 하면서 EQ Bar 가 없어짐.
            }
            else -> {} // 이 외 IDLE, ERROR 등일때는 암것도 안함~
        }
    }
    // RtPickerActivity 가 종료될 때 모든 값을 초기화 (안 그러면 다시 RtPicker 눌렀을 때 기존에 Play 되던 EQ/RadioBtn 그대로 보여진다.)
    fun initVariables() {
        lastUserCheckedPos = -1
        selectedRadioBtn = null
        prevClickedHolder = null
        clickedHolder = null
    }

// ViewHolder

    inner class RtPickerVHolder(xmlToView: View) : RecyclerView.ViewHolder(xmlToView) {
        val clEntireRow: ConstraintLayout = xmlToView.findViewById(R.id.cl_entireSingleSlot) // Row 전체를 감싸는 LinearLayout
        val tvRtTitle: TextView = xmlToView.findViewById(R.id.tv_rtPicker_title)
        val tvRtDescription: TextView = xmlToView.findViewById(R.id.tv_rtPicker_description)
        val ivRtAlbumArt: ImageView = xmlToView.findViewById(R.id.iv_singleSlot_albumArt)
        val radioBtn: RadioButton = xmlToView.findViewById(R.id.rb_singleSlot_selector)
        val vuMeter: VuMeterView = xmlToView.findViewById(R.id.rtPicker_vumeter)// vumeter


    }


}