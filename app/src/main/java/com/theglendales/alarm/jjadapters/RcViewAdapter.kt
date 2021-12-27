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
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.JjRecyclerViewModel
import com.theglendales.alarm.jjmvvm.data.ViewAndTrIdClass

import com.theglendales.alarm.jjmvvm.iapAndDnldManager.MyIAPHelperV2
import com.theglendales.alarm.jjmvvm.mediaplayer.MyMediaPlayer
import com.theglendales.alarm.model.mySharedPrefManager
//import com.theglendales.alarm.jjiap.MyIAPHelper_v1
import io.gresse.hugo.vumeterlibrary.VuMeterView

//import javax.sql.DataSource

private const val TAG = "RcVAdapter"

interface MyOnItemClickListener {
    fun myOnItemClick(v: View, trackId: Int)

}

class RcViewAdapter(
    var currentRtList: MutableList<RtInTheCloud>,
    private val receivedActivity: FragmentActivity,
    private val rcViewModel: JjRecyclerViewModel,
    private val mediaPlayer: MyMediaPlayer) : RecyclerView.Adapter<RcViewAdapter.MyViewHolder>() {


    companion object {
        var viewHolderMap: HashMap<Int, MyViewHolder> = HashMap()
    }

    var ringToneMap: HashMap<Int, RtInTheCloud> = HashMap()
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
        val currentIapName = currentRtList[position].iapName

        viewHolderMap[currentTrId] = holder


        holder.tv1_Title.text = currentItem.title
        holder.tv2_ShortDescription.text = currentItem.hashtags
        holder.holderTrId = currentTrId


        Log.d(TAG, "onBindViewHolder: holder TrId= ${holder.holderTrId}, currentTrIapName= $currentIapName")
        Log.d(TAG, "onBindViewHolder: Purchased Statsp=${mySharedPrefManager.getPurchaseBoolPerIapName(currentIapName)} ")

//        Log.d(TAG,"onBindViewHolder: jj- trId: ${holder.holderTrId}, pos: $position) " +
//                "Added holder($holder) to vHoldermap[${holder.holderTrId}]. " +
//                "b)vHolderMap size: ${viewHolderMap.size} c) VholderMap info: $viewHolderMap")
    //트랙 재활용시 하이라이트&VuMeter 이슈 관련--->
        // 1) Bind 하면서 기존에 Click 되어있던 트랙이면 하이라이트&VuMeter 생성
        if (currentTrId == GlbVars.clickedTrId) {
            enableHL(holder)
            enableVM(holder)
        }
        if (currentTrId != GlbVars.clickedTrId) {
            disableHL(holder)
            disableVMnLC(holder)
        }
    // <-- 트랙 재활용시 하이라이트&VuMeter 이슈 관련--->

        //IAP 관련
        holder.tv3_Price.text = MyIAPHelperV2.itemPricesMap[currentIapName].toString() // +",000" 단위 큰것도 잘 표시되네..

        //Purchase Stat True or False
        when(mySharedPrefManager.getPurchaseBoolPerIapName(currentIapName)) {
            true -> {// Show "Purchased" icon
                holder.iv_PurchasedTrue.visibility = View.VISIBLE
                holder.iv_PurchasedFalse.visibility = View.GONE
            }
            false -> {// Show "GET THIS" icon
                holder.iv_PurchasedTrue.visibility = View.GONE
                holder.iv_PurchasedFalse.visibility = View.VISIBLE
            }
            else -> {
                Toast.makeText(receivedActivity, "Error Displaying Purchased Item for trId=$currentTrId", Toast.LENGTH_SHORT).show()}
        }
        GlideApp.with(receivedActivity).load(currentItem.imageURL).centerCrop()
            .error(R.drawable.errordisplay)
            .placeholder(R.drawable.placeholder).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?,model: Any?,target: Target<Drawable>?,isFirstResource: Boolean): Boolean {
                    Log.d(TAG, "onLoadFailed: Glide load failed!. Message: $e")
                    return false}

                // (여러 ViewHolder 를 로딩중인데) 현재 로딩한 View 에 Glide 가 이미지를 성공적으로 넣었다면.
                override fun onResourceReady(resource: Drawable?,model: Any?,target: Target<Drawable>?
                    ,dataSource: DataSource?,isFirstResource: Boolean): Boolean {
                    Log.d(TAG,"onResourceReady: Glide loading success! trId: $currentTrId, Position= ${holder.adapterPosition}") // debug 결과 절대 순.차.적으로 진행되지는 않음!

                    // //Glide 가 로딩되기전 클릭하는 상황에 대응하기 위해 -> 열려있는 miniPlayer의 thumbnail에 필요한 사진과 현재 glide로 로딩된 사진의 동일한지 trId로 확인 후
                    if (currentTrId == GlbVars.clickedTrId) {
                        Log.d(TAG, "onResourceReady: (Possible) Early Click!! Setting up image for MiniPlayer's upper/lower ui ")

                    //1안 - 예전 방법
                        val iv_upperUi_thumbNail = receivedActivity.findViewById<ImageView>(R.id.id_upperUi_iv_coverImage)
                        val iv_lowerUi_bigThumbnail = receivedActivity.findViewById<ImageView>(R.id.id_lowerUi_iv_bigThumbnail)
                        iv_upperUi_thumbNail.setImageDrawable(resource)
                        iv_lowerUi_bigThumbnail.setImageDrawable(resource)

                    //2안 - 라이브 데이터 사용하여 한번 더 전달) -> 아래 iv_ThumbNail 에 들어가는건 아래 .into 에서 이뤄지므로.. 이건 안된다! (X) <- 그냥 지워버려..
//                        val vHolderAndTrId = ViewAndTrIdClass(holder.rl_Including_tv1_2, currentTrId)
//                        rcViewModel.updateLiveData(vHolderAndTrId) 지워라!!


                    }

                    return false
                }
            }).into(holder.iv_Thumbnail)
    }

// Highlight & VuMeter 작동 관련    --------->
    //1)Highlight
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
    // 2) VuMeter and Loading Circle => todo: VHolderUiHandler 가 현재 Koin 덕분에 SingleTon 이니까. 그쪽으로 전달하자!!!!
        private fun enableVM(holder: MyViewHolder) {
        // 여기에다 if(VHolderUiHandler.currentStatusMp == StatusMp.PLAYING) ..... {}
        holder.iv_Thumbnail.alpha = 0.3f // 어둡게
        holder.vuMeterView.visibility = VuMeterView.VISIBLE
        }
        private fun disableVMnLC(holder: MyViewHolder) {
            holder.loadingCircle.visibility = View.INVISIBLE // 일단 loadingCircle 없애기.
            holder.iv_Thumbnail.alpha = 1.0f // 밝기 원복
            holder.vuMeterView.visibility = VuMeterView.GONE // VuMeter 감추기
        }
// <---------- // Highlight & VuMeter 작동 관련    --------->



    // 스크롤 화면 떨어져나갔다 들어오는거 관련 (EQ Animation 때문에 넣었음!)---------------->
    // a) 스크롤해서 해당 view 가 화면에서 안 보일때
    override fun onViewDetachedFromWindow(holder: MyViewHolder) {
        super.onViewDetachedFromWindow(holder)
        Log.d(TAG, "!!onViewDETACHEDFromWindow: trId: ${holder.holderTrId}, holder: $holder")
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

    fun refreshRecyclerView(newList: MutableList<RtInTheCloud>) {
        //Log.d(TAG, "refreshRecyclerView: @@@@@@@@ currentRtList.size (BEFORE): ${currentRtList.size}")
        val oldList = currentRtList

        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(MyDiffCallbackClass(oldList, newList))
        currentRtList = newList
        Log.d(TAG, "refreshRecyclerView: @@@@@@@@ currentRtList.size (AFTER): ${currentRtList.size}")
        //updateRingToneMap(receivedList)//이건 내가 추가
        diffResult.dispatchUpdatesTo(this)
        //enableHighlightOnTrId(GlbVars.clickedTrId)
    }

    fun updateRingToneMap(inputRtList: MutableList<RtInTheCloud>) {
        ringToneMap.clear()
        for (i in 0 until inputRtList.size) {
            ringToneMap[inputRtList[i].id] = inputRtList[i]
            //Log.d(TAG, "updateMap: ringToneMap id= ${inputRtList[i].id} = ringToneMap: $ringToneMap")
        }
    }

    fun getDataFromMap(trackId: Int): RtInTheCloud? {
        return if (ringToneMap.isNotEmpty()) ringToneMap[trackId] else null
    }

    // DiffUtil Class
    class MyDiffCallbackClass(var oldRingToneList: MutableList<RtInTheCloud>, var newRingToneList: MutableList<RtInTheCloud>) : DiffUtil.Callback() { // Extend by DiffUtil
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
        val rl_Including_tv1_2: RelativeLayout = myXmlToViewObject.findViewById(R.id.id_rL_including_title_description)

        // 3) 왼쪽 - 제일 왼쪽 AlbumArt 및 vuMeter/LoadingCircle 영역
        val iv_Thumbnail: ImageView = myXmlToViewObject.findViewById(R.id.id_ivThumbnail)
        val vuMeterView: VuMeterView = myXmlToViewObject.findViewById(R.id.id_vumeter)
        val loadingCircle: ProgressBar = myXmlToViewObject.findViewById(R.id.id_progressCircle)

        // 4) 오른쪽 FREE,GET THIS 칸
        val cl_entire_purchase: ConstraintLayout =myXmlToViewObject.findViewById(R.id.id_cl_entire_Purchase)
        val tv3_Price: TextView = myXmlToViewObject.findViewById(R.id.id_tvPrice)
        val iv_PurchasedFalse: ImageView = myXmlToViewObject.findViewById(R.id.id_ivPurchased_False)
        val iv_PurchasedTrue: ImageView = myXmlToViewObject.findViewById(R.id.id_ivPurchased_True)
        //var tv4_GetThis: TextView = myXmlToViewObject.findViewById(R.id.id_tvGetThis)



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
            val clickedPosition =adapterPosition // todo: 이것도. 위에 holderTrId 처럼 holderPosition 으로 설정후 onBindViewHolder 에서 제대로 position 값 입력 가능. -> smoothScrollToPos()과 연계 사용?

            isRVClicked = true // 이거 안쓰이는것 같음..  Recycle View 를 누른적이 있으면 true (혹시나 미리 누를수도 있으므로)




            if (clickedPosition != RecyclerView.NO_POSITION && clickedView != null)
            { // To avoid possible mistake when we delete the item but click it
                val vHolderAndTrId = ViewAndTrIdClass(v, holderTrId)

            //1) 하이라이트, 음악 재생은 "클릭 영역이 구매쪽 제외한 전체 영역일 때만!!" (Rl_including_tv1_2 영역)
                if(v.id == R.id.id_rL_including_title_description) {
                    //1-a)
                    GlbVars.clickedTrId = holderTrId
                    Log.d(TAG,"*****************************onClick-To Play MUSIC: Global.clTrId: ${GlbVars.clickedTrId}, holderTrId: $holderTrId ****************")

                    //1-b) 하이라이트 작동
                    disableHLAll() // 모든 하이라이트를 끄고
                    enableHL(this) // 선택된 viewHolder 만 하이라이트!

                    //1-c) 음악 플레이
                    mediaPlayer.prepMusicPlayOnlineSrc(holderTrId, true) // 여기서부터 RcVAdapter -> mediaPlayer <-> mpVuModel <-> SecondFrag (Vumeter UI업뎃)
                }
            //2) 음악쪽 클릭이든 구매쪽 클릭이든 일단 SecondFrag.kt 에 전달-> 거기서 알아서 판단.
                //LiveData Feed
                rcViewModel.updateLiveData(vHolderAndTrId) // JJRecyclerViewModel.kt - selectedRow(MutableLiveData) 값을 업데이트!

            }


        }
    }


}