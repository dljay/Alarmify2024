package com.theglendales.alarm.jjadapters

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.theglendales.alarm.R
import com.theglendales.alarm.jjdata.GlbVars
import com.theglendales.alarm.jjdata.RingtoneClass
import com.theglendales.alarm.jjmvvm.JjMpViewModel
import com.theglendales.alarm.jjmvvm.JjRecyclerViewModel
import com.theglendales.alarm.jjmvvm.data.ViewAndTrIdClass
import com.theglendales.alarm.jjmvvm.mediaplayer.MyMediaPlayer
//import com.theglendales.alarm.jjiap.MyIAPHelper
import io.gresse.hugo.vumeterlibrary.VuMeterView

//import javax.sql.DataSource

private const val TAG = "RcVAdapter"

interface MyOnItemClickListener {
    fun myOnItemClick(v: View, trackId: Int)

}

class RcViewAdapter(
    var currentRtList: MutableList<RingtoneClass>,
    private val receivedActivity: FragmentActivity,
    private val rcViewModel: JjRecyclerViewModel,
    private val mediaPlayer: MyMediaPlayer) : RecyclerView.Adapter<RcViewAdapter.MyViewHolder>() {


    companion object {
        var viewHolderMap: HashMap<Int, MyViewHolder> = HashMap()
    }

    var ringToneMap: HashMap<Int, RingtoneClass> = HashMap()
    var isRVClicked: Boolean = false // 혹시나 미리 클릭되었을 경우를 대비하여 만든 boolean value. 이거 안 쓰이나?
// 하이라이트시 background 에 적용될 색
    val highlightColor = ContextCompat.getColor(receivedActivity.applicationContext,R.color.gray_light_highlight_1)
    val plainColor = Color.WHITE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        Log.d(TAG, "(Line44)onCreateViewHolder: jj- RcV! viewType=$viewType.")
        val myXmlToViewObject =
            LayoutInflater.from(parent.context).inflate(R.layout.jj_rc_single_slot, parent, false)
        return MyViewHolder(myXmlToViewObject)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val currentItem = currentRtList[position]
        val currentTrId = currentRtList[position].id
        viewHolderMap[currentTrId] = holder

        val currentTrIapName = currentRtList[position].iapName
        holder.tv1_Title.text = currentItem.title
        holder.tv2_ShortDescription.text = currentItem.tags
        holder.holderTrId = currentTrId

        Log.d(TAG,"onBindViewHolder: jj- trId: ${holder.holderTrId}, " +
                "pos: $position // Added holder($holder) to vHoldermap[${holder.holderTrId}]. " +
                "b)vHolderMap size: ${viewHolderMap.size} c) VholderMap info: $viewHolderMap")
    //하이라이트: Bind 하면서 기존에 Click 되어있던 트랙이면 하이라이트

        if (currentTrId == GlbVars.clickedTrId) {
            enableHL(holder)
        }
        if (currentTrId != GlbVars.clickedTrId) {
            disableHL(holder)
        }
    // <--하이라이트: Bind 하면서 기존에 Click 되어있던 트랙이면 하이라이트

    //IAP 관련

//        holder.tv3_Price.text = MyIAPHelper.itemPricesMap[currentTrIapName].toString() // +",000" 단위 큰것도 잘 표시되네..

        //Purchase Stat True or False
//        when(MyIAPHelper.purchaseStatsMap[currentTrId]) {
//            true -> {// Show "Purchased" icon
//                holder.iv_PurchasedTrue.visibility = View.VISIBLE
//                holder.iv_PurchasedFalse.visibility = View.GONE
//            }
//            false -> {// Show "GET THIS" icon
//                holder.iv_PurchasedTrue.visibility = View.GONE
//                holder.iv_PurchasedFalse.visibility = View.VISIBLE
//            }
//            else -> {
//                Toast.makeText(receivedActivity, "Error Displaying Purchased Items", Toast.LENGTH_SHORT).show()}
//        }




        GlideApp.with(receivedActivity).load(currentItem.imageURL).centerCrop()
            .error(R.drawable.errordisplay)
            .placeholder(R.drawable.placeholder).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d(TAG, "onLoadFailed: Glide load failed!. Message: $e")

                    return false
                }

                // (여러 ViewHolder 를 로딩중인데) 현재 로딩한 View 에 Glide 가 이미지를 성공적으로 넣었다면.
                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d(
                        TAG,
                        "onResourceReady: Glide loading success! trId: $currentTrId, Position: $position"
                    ) // debug 결과 절대 순.차.적으로 진행되지는 않음!

                    // rcView 의 이미지 로딩전에 일찍 click 했을 때 -> 열려있는 miniPlayer의 thumbnail에 필요한 사진과 현재 glide로 로딩된 사진의 동일한지 trId로 확인 후
                    if (currentTrId == GlbVars.clickedTrId) {
                        //Log.d(TAG, "onResourceReady: Early clicked!! Setting up image for MiniPlayer's upper/lower ui ")
//                        iv_upperUi_thumbNail.setImageDrawable(resource)
//                        iv_lowerUi_bigThumbnail.setImageDrawable(resource)
                    }

                    return false
                }
            }).into(holder.iv_Thumbnail)
    }


    fun enableHL(holder: MyViewHolder) {
        Log.d(TAG, "enableHighlightOnTrId: YES")
        //holder.tv1_Title?.setTextColor(Color.MAGENTA)
        holder.ll_entire_singleSlot?.setBackgroundColor(highlightColor)
    }

    private fun disableHL(holder: MyViewHolder) {
        holder.tv1_Title.setTextColor(Color.BLACK)
        holder.ll_entire_singleSlot.setBackgroundColor(Color.WHITE)
    }
// 모든 row 의 Highlight 를 없앰. (어떤 row 를 클릭했을 때 우선적으로 실행되어 모든 하이라이트를 없앰.)
    private fun disableHLAll() {
        if(!viewHolderMap.isNullOrEmpty()) {
            viewHolderMap.forEach { (_, vHolder) -> disableHL(vHolder) }
        }

    }
// <---------- 색 highlight function. onBindViewHolder() 와 아래 onClick 두군데서 불림. <------------------------

// ---------> VuMeter 관련 ------------------------>

    private fun enblVuMeterOnTrId(trId: Int, holder: MyViewHolder) {

        // 1) rt 가 pause 가 아니고 재생중일 경우
        if (!GlbVars.isSongPaused && GlbVars.isSongPlaying) { // 그러나 곡이 재생전에는 이게 =true 로 되어있으므로 일로 안 들어감.
            Log.d(TAG, "++EnblVuMeterOn(O) trId: $trId, vuMeterView: ${holder.vuMeterView}}")
            holder.vuMeterView.visibility = VuMeterView.VISIBLE
            holder.vuMeterView.resume(true)
            holder.iv_Thumbnail.alpha = 0.3f


        } // 2) a.노래가 Pause 된 상태 or b.prepare() 대기 상태인 경우
        else if (GlbVars.isSongPaused || !GlbVars.isSongPlaying) {
            // a)pause 상태일 때
            Log.d(TAG, "△△EnblVuMeter Pause(△) on trId: $trId, vuMeterView: ${holder.vuMeterView}}")
            holder.vuMeterView.visibility = VuMeterView.VISIBLE
            holder.vuMeterView.pause()
            holder.iv_Thumbnail.alpha = 0.3f
            // b)대기 상태 -> todo: loading circle
        }
    }

    private fun disableVuMeterOnTrId(holder: MyViewHolder) {

        holder.vuMeterView.visibility = VuMeterView.INVISIBLE
        holder.iv_Thumbnail.alpha = 1.0f
        //holder.vuMeterView.stop(false)
        Log.d(TAG, "disableVuMeterOnTrId(X): trId: ${holder.holderTrId}, holder=$holder ")

    }

// <---------- VuMeter 관련 <<<------------------------

    // 스크롤 화면 떨어져나갔다 들어오는거 관련 (EQ Animation 때문에 넣었음!)---------------->
    // a) 스크롤해서 해당 view 가 화면에서 안 보일때
    override fun onViewDetachedFromWindow(holder: MyViewHolder) {
        super.onViewDetachedFromWindow(holder)
        Log.d(TAG, "!!onViewDETACHEDFromWindow: trId: ${holder.holderTrId}, holder: $holder")
        // vuMeter 때문에 여기 recyclable 설정을 할 수 밖에 없었지만. 이젠 vuMeter 를 liveData 로 실행해줄 예정여서 없어도 될듯함.
        /*if (holder == viewHolderMap[GlbVars.clickedTrId]) {
            holder.setIsRecyclable(false)
            Log.d(TAG,"onViewDetached: _X_X_X__X_X_X__X_X_X__X_X_X_ Disable setIsRecyclable(XX) for holder_TRID=${holder.holderTrId}")

        } else {
            holder.setIsRecyclable(true)
            Log.d(
                TAG,
                "onViewDetached: _O_O_O_O_O_O_O_O_O_O_O_O_O_O_O_O Enable setIsRecyclable(OO) for holder_TRID=${holder.holderTrId}"
            )

        }*/

    }

    // b) 다시 스크롤해서 해당 view 가 화면에서 보일때..//스크롤하고 화면 사라졌다 다시 오면 view 번호가 계속 바뀌는 문제.
    override fun onViewAttachedToWindow(holder: MyViewHolder) {
        super.onViewAttachedToWindow(holder)


        Log.d(TAG,"onViewAttachedToWindow: trId: ${holder.holderTrId},  holder name: $holder, vuMeter Name: ${holder.vuMeterView},")

        //현재 추가시키는 holder 가 기존 click, 재생(혹은 재생 중 pause) 중인 트랙였다. -> !!! 이거 그냥 BindView 에서 대체?
        /*    if(holder.holderTrId == GlbVars.currentPlayingTrId && holder.holderTrId != GlbVars.errorTrackId) {
                enblVuMeterOnTrId(holder.holderTrId, holder)
                enableHighlightOnTrId(holder.holderTrId)

                Log.d(TAG, "++onViewATTACHEDtoWindow: (O) enblHighlight/enblVuMeter  at trId: ${holder.holderTrId}, vuMeter(${holder.vuMeterView}")
            }
            if(holder.ll_entire_singleSlot.isSelected && holder.holderTrId!=GlbVars.currentPlayingTrId )
            // a) (가령 11번 클릭-> 1번 역시 하이라이트 됨(11번과 동일한 viewHolder 가 recycle 되었으므로), b) trId 가 클릭한 놈이 아니면 무조건 disable highlight
            {
                disableHighlightOnTrId(holder)
                disableVuMeterOnTrId(holder)
            }*/
    }

    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
        Log.d(TAG, "getItemViewType: called")
    }


// Utility -----------------------

    override fun getItemCount(): Int {
        return currentRtList.size
    }

    fun updateRecyclerView(newList: MutableList<RingtoneClass>) {
        //Log.d(TAG, "updateRecyclerView: @@@@@@@@ currentRtList.size (BEFORE): ${currentRtList.size}")
        val oldList = currentRtList

        val diffResult: DiffUtil.DiffResult =
            DiffUtil.calculateDiff(MyDiffCallbackClass(oldList, newList))
        currentRtList = newList
        Log.d(TAG, "updateRecyclerView: @@@@@@@@ currentRtList.size (AFTER): ${currentRtList.size}")
        //updateRingToneMap(receivedList)//이건 내가 추가
        diffResult.dispatchUpdatesTo(this)
        //enableHighlightOnTrId(GlbVars.clickedTrId)
    }

    fun updateRingToneMap(inputRtList: MutableList<RingtoneClass>) {

        ringToneMap.clear()

        for (i in 0 until inputRtList.size) {
            ringToneMap[inputRtList[i].id] = inputRtList[i]
            //Log.d(TAG, "updateMap: ringToneMap id= ${inputRtList[i].id} = ringToneMap: $ringToneMap")
        }
    }

    fun getDataFromMap(trackId: Int): RingtoneClass? {
        return if (ringToneMap.isNotEmpty()) ringToneMap[trackId] else null
    }

    // DiffUtil Class
    class MyDiffCallbackClass(
        var oldRingToneList: MutableList<RingtoneClass>,
        var newRingToneList: MutableList<RingtoneClass>
    ) : DiffUtil.Callback() { // Extend by DiffUtil
        override fun getOldListSize(): Int {
            return oldRingToneList.size
        }

        override fun getNewListSize(): Int {
            return newRingToneList.size
        }

        // 1차로 여기서 id 로 판별. (기존 리스트 item 과 새로 받은 리스트 item)
        override fun areItemsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ): Boolean { // check if two items represent the same item. 흠.. 다 똑같은지말고 id 만 같아도 true 라는듯..
            //Log.d(TAG, "areItemsTheSame: oldItemPos: $oldItemPosition, newItemPos: $newItemPosition, bool result: ${oldRingToneList[oldItemPosition].id == newRingToneList[newItemPosition].id}")
            return (oldRingToneList[oldItemPosition].id == newRingToneList[newItemPosition].id)
        }

        // 1차 선발된 놈들을 2차로 여기서 아예 동일한 놈인지(data 로 파악) 판명.
        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ): Boolean { // 모든 field 가 아예 똑같은건지 확인! (id/url/image 등등)
            //Log.d(TAG, "areContentsTheSame: oldItemPos: $oldItemPosition, newItemPos: $newItemPosition,  ${oldRingToneList[oldItemPosition] == newRingToneList[newItemPosition]}")
            return oldRingToneList[oldItemPosition] == newRingToneList[newItemPosition]
        }

    }

    // MyViewHolder class
    inner class MyViewHolder(myXmlToViewObject: View) : RecyclerView.ViewHolder(myXmlToViewObject),
        View.OnClickListener {

        //1) 전체 Slot 을 감싸는 Linear Layout
        val ll_entire_singleSlot: LinearLayout =
            myXmlToViewObject.findViewById(R.id.id_singleSlot_ll)// HIGHLIGHT 위해 single slot 전체를 감싸는 linear layout 추가

        //2) 왼쪽-중앙 곡 클릭 영역

        val tv1_Title: TextView = myXmlToViewObject.findViewById(R.id.id_tvTitle)
        val tv2_ShortDescription: TextView = myXmlToViewObject.findViewById(R.id.id_tvTags)
        val rl_Including_tv1_2: RelativeLayout =
            myXmlToViewObject.findViewById(R.id.id_rL_including_title_description)

        // 3) 오른쪽 FREE,GET THIS 칸
        val cl_entire_purchase: ConstraintLayout =
            myXmlToViewObject.findViewById(R.id.id_cl_entire_Purchase)
        val tv3_Price: TextView = myXmlToViewObject.findViewById(R.id.id_tvPrice)
        val iv_PurchasedFalse: ImageView = myXmlToViewObject.findViewById(R.id.id_ivPurchased_False)
        val iv_PurchasedTrue: ImageView = myXmlToViewObject.findViewById(R.id.id_ivPurchased_True)
        //var tv4_GetThis: TextView = myXmlToViewObject.findViewById(R.id.id_tvGetThis)

        val iv_Thumbnail: ImageView = myXmlToViewObject.findViewById(R.id.id_ivThumbnail)
        val vuMeterView: VuMeterView = myXmlToViewObject.findViewById(R.id.id_vumeter)
        val loadingCircle: ProgressBar = myXmlToViewObject.findViewById(R.id.id_progressCircle)

        var holderTrId: Int = -10 // 처음엔 의미없는 -10 값을 갖지만, onBindView 에서 제대로 holder.id 로 설정됨.
        // 아래 trackId 를 없애고 이걸로 사용 가능.
        // 마찬가지로 clickedPosition 도 아래에서 여기로 옮겨와서 사용 가능 (초기화에만 -10, onBindView 에서 제대로 값 설정)
        //trackId


        init {
            rl_Including_tv1_2.setOnClickListener(this)
            cl_entire_purchase.setOnClickListener(this)

            //Log.d(TAG, "MyViewHolder Init: ${myXmlToViewObject.toString()}")
        }


        override fun onClick(v: View?) {

            val clickedView = v
            val clickedPosition =
                adapterPosition // todo: 이것도. 위에 holderTrId 처럼 holderPosition 으로 설정후 onBindViewHolder 에서 제대로 position 값 입력 가능. -> smoothScrollToPos()과 연계 사용?

            isRVClicked = true // 이거 안쓰이는것 같음..  Recycle View 를 누른적이 있으면 true (혹시나 미리 누를수도 있으므로)

            GlbVars.clickedTrId = holderTrId
            Log.d(
                TAG,
                "*****************************onClick: Global.clTrId: ${GlbVars.clickedTrId}, holderTrId: $holderTrId ****************"
            )
            disableHLAll() // 모든 하이라이트를 끄고
            enableHL(this) // 선택된 viewHolder 만 하이라이트!

            if (clickedPosition != RecyclerView.NO_POSITION && clickedView != null) { // To avoid possible mistake when we delete the item but click it
                val vHolderAndTrId = ViewAndTrIdClass(v, holderTrId)
            //1)Ui Update 용 LiveData Feed
                rcViewModel.updateLiveData(vHolderAndTrId) // JJRecyclerViewModel.kt - selectedRow(MutableLiveData) 값을 업데이트!
            //2) 만약 클릭 영역이 구매쪽 제외한 전체 영역일 때(Rl_including_tv1_2 영역) => MediaPlayer 에 Play 전달!
                if(v.id == R.id.id_rL_including_title_description) {
                    mediaPlayer.prepareMusicPlay() // 여기서부터 RcVAdapter -> mediaPlayer <-> mpVuModel <-> SecondFrag (Vumeter UI업뎃)
                }
            }


        }
    }


}