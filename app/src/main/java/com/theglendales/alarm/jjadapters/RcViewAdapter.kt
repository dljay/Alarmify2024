package com.theglendales.alarm.jjadapters

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.GlbVars
import com.theglendales.alarm.jjdata.RtInTheCloud

import com.theglendales.alarm.jjmvvm.mediaplayer.ExoForUrl
import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp
//import com.theglendales.alarm.jjiap.MyIAPHelper_v1
import io.gresse.hugo.vumeterlibrary.VuMeterView

//import javax.sql.DataSource

private const val TAG = "RcVAdapter"

interface RcCommInterface {
    fun onRcvClick(rtObj: RtInTheCloud, isPurchaseClicked: Boolean)
}

class RcViewAdapter(
    private var rtPlusIapInfoList: List<RtInTheCloud>,
    private val receivedActivity: FragmentActivity,
    private val secondFragListener: RcCommInterface) : RecyclerView.Adapter<RcViewAdapter.MyViewHolder>() {


// 하이라이트 전후로 쓸 색
    val primaryTextColor = ContextCompat.getColor(receivedActivity.applicationContext,R.color.primaryTextColor)
    val secondaryTextColor = ContextCompat.getColor(receivedActivity.applicationContext,R.color.secondaryTextColor)
    val tertiaryTextColor =  ContextCompat.getColor(receivedActivity.applicationContext,R.color.tertiaryTextColor)
    val accentTextColor = ContextCompat.getColor(receivedActivity.applicationContext,R.color.jj_accentColor_1)
// 현재 click 된 ViewHolder 를 여기에 저장.
    var prevClickedHolder: MyViewHolder? = null
    var clickedHolder: MyViewHolder? = null

// MediaPlayer
    private val exoForUrlPlay: ExoForUrl by globalInject()

    var isRVClicked: Boolean = false // 혹시나 미리 클릭되었을 경우를 대비하여 만든 boolean value. 이거 안 쓰이나?
// 하이라이트시 background 에 적용될 색

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        Log.d(TAG, "(Line44)onCreateViewHolder: jj- RcV! viewType=$viewType.")
        val myXmlToViewObject = LayoutInflater.from(parent.context).inflate(R.layout.jj_rc_single_slot, parent, false)
        return MyViewHolder(myXmlToViewObject)

    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val currentRt = rtPlusIapInfoList[position]
        val currentTrId = currentRt.id
        val currentIapName = currentRt.iapName
        val currentIntensity = currentRt.intensity

        holder.tv1_Title.text = currentRt.title
        holder.tv2_ShortDescription.text = currentRt.tags
        holder.holderTrId = currentTrId

        Log.d(TAG, "onBindViewHolder: holder.hashCode= ${holder.hashCode()}, holder TrId= ${holder.holderTrId}, currentTrIapName= $currentIapName")
        Log.d(TAG, "onBindViewHolder: Purchased Stats=${currentRt.purchaseBool}")

//        Log.d(TAG,"onBindViewHolder: jj- trId: ${holder.holderTrId}, pos: $position) " + "Added holder($holder) to vHoldermap[${holder.holderTrId}]. " + "b)vHolderMap size: ${viewHolderMap.size} c) VholderMap info: $viewHolderMap")


        hideFreeIcon(holder) // 우선 View 가 Recycle 되면서 기존 [FREE] 표시가 그대로 나올 수 있으므로 -> Bind 과정에서 Hide 시켜주기

    //스크롤 하면서 트랙 재활용시 하이라이트&VuMeter & Purchased(V) 표시 관련--->
        // A) Bind 하면서 기존에 Click 되어있던 트랙이면 a) 글자 색 변경 (O) b) VuMeter (O) c) Purchased Check(X) d) FREE(X)
        if (currentTrId == GlbVars.clickedTrId) {
            clickedHolder = holder // a) ListFrag 복귀 후 clickedHolder 는 Null 상태이므로 '기존에 선택했던 TrId 가 배정된 '현재의 Holder' 로 설정' b) 단순 위아래 Scroll 은 어차피 동일한 clickedHolder 값이 배정됨.
            enableHL(holder)
            enableVM(exoForUrlPlay.currentPlayStatus, holder)
            disablePurchasedCheck(holder)
            hideFreeIcon(holder)
        }
        // B) Bind 하면서 'Select' 된 트랙이 아닐경우 a) 글자 색 변경 (X) b) VuMeter (X) c) Purchased Check (O) (필요할 경우)
        if (currentTrId != GlbVars.clickedTrId) {
            disableHL(holder)
            disableVMnLC(holder)
            when(currentRt.purchaseBool) {
                true -> enablePurchasedCheck(holder)
                false -> {
                    disablePurchasedCheck(holder)
                    if(currentRt.iapName.contains("f")) { // FREE ITEM 이고 "아직 구입이 안된 경우"-> FREE ICON 보여주기.
                        showFreeIcon(holder)
                        Log.d(TAG, "onBindViewHolder: this-trID=$currentTrId must have FREE marked!!")
                    }
                }
            }
        }
    // <-- 트랙 재활용시 하이라이트&VuMeter 이슈 관련--->

        GlideApp.with(receivedActivity).load(currentRt.imageURL).centerCrop()
            .error(R.drawable.errordisplay)
            .placeholder(R.drawable.placeholder).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?,model: Any?,target: Target<Drawable>?,isFirstResource: Boolean): Boolean {
                    Log.d(TAG, "onLoadFailed: Glide load failed!. Message: $e")
                    return false}

                // (여러 ViewHolder 를 로딩중인데) 현재 로딩한 View 에 Glide 가 이미지를 성공적으로 넣었다면.
                override fun onResourceReady(resource: Drawable?,model: Any?,target: Target<Drawable>?
                    ,dataSource: DataSource?,isFirstResource: Boolean): Boolean {
                    //Log.d(TAG,"onResourceReady: Glide loading success! trId: $currentTrId, Position= ${holder.adapterPosition}") // debug 결과 절대 순.차.적으로 진행되지는 않음!

                    // //Glide 가 로딩되기전 클릭하는 상황에 대응하기 위해 -> 열려있는 miniPlayer의 thumbnail에 필요한 사진과 현재 glide로 로딩된 사진의 동일한지 trId로 확인 후
                    /*if (currentTrId == GlbVars.clickedTrId)
                    {
                        Log.d(TAG, "onResourceReady: (Possible) Early Click!! Setting up image for MiniPlayer's upper/lower ui ")
                        val iv_upperUi_thumbNail = receivedActivity.findViewById<ImageView>(R.id.id_upperUi_iv_coverImage)
                        val iv_lowerUi_bigThumbnail = receivedActivity.findViewById<ImageView>(R.id.id_lowerUi_iv_bigThumbnail)
                        iv_upperUi_thumbNail.setImageDrawable(resource)
                        iv_lowerUi_bigThumbnail.setImageDrawable(resource)
                    }*/

                    return false
                }
            }).into(holder.iv_Thumbnail)
    }



    // Highlight & LoadingCircle & VuMeter 작동 관련    --------->
    //0) LcVmIvController : LoadingCircle, VuMeter, ImageView: 썸네일 밝기 원복
        fun lcVmIvController(playStatus: StatusMp) {
            if(clickedHolder!=null) {
                when(playStatus) {
                    StatusMp.BUFFERING -> {
                        // 1)기존에 UI 가 작동된 Holder 를 다 '원복' 해주기
                        if(prevClickedHolder!=null) {
                            prevClickedHolder!!.loadingCircle.visibility = View.INVISIBLE // loading Circle 안보이게. (a)
                            prevClickedHolder!!.vuMeterView.visibility = VuMeterView.GONE // VuMeter 도 안보이게 (b)

                        }
                        // 2) 새로 Click 된 Holder 의 UI 업데이트
                        clickedHolder!!.loadingCircle.visibility = View.VISIBLE
                        clickedHolder!!.ivPurchaseCheck.visibility = VuMeterView.GONE // PurchasedCheck(v) 표시 없애주기.
                        clickedHolder!!.ivFreeIcon.visibility = VuMeterView.GONE // PurchasedCheck(v) 표시 없애주기.
                        clickedHolder!!.vuMeterView.visibility = VuMeterView.GONE
                    }
                    StatusMp.READY -> {
                        clickedHolder!!.loadingCircle.visibility = View.INVISIBLE
                        clickedHolder!!.vuMeterView.visibility = VuMeterView.VISIBLE
                        clickedHolder!!.vuMeterView.pause()
                    }
                    StatusMp.PAUSED -> {
                        clickedHolder!!.vuMeterView.pause()
                    }
                    StatusMp.PLAY -> {

                        clickedHolder!!.vuMeterView.visibility = VuMeterView.VISIBLE
                        clickedHolder!!.vuMeterView.resume(true)
                    }

                    else -> {}
                }
            }
        }
    //1)Highlight - 클릭 순간 작동
        private fun enableHL(selectedHolder: MyViewHolder?) {
            Log.d(TAG, "enableHL: called for selectedHolder = $selectedHolder")
            if (selectedHolder != null) {

                selectedHolder.tv1_Title.setTextColor(accentTextColor)
                selectedHolder.tv2_ShortDescription.setTextColor(accentTextColor)
            }
        }

        private fun disableHL(unselectedHolder: MyViewHolder?) {
            Log.d(TAG, "disableHL: called for unselectedHolder=$unselectedHolder")
            if (unselectedHolder != null) {

                unselectedHolder.tv1_Title.setTextColor(primaryTextColor)
                unselectedHolder.tv2_ShortDescription.setTextColor(tertiaryTextColor)

            }

        }

    // 2) BindView 할때만 작동 (a) (b) 위로 쓱싹 스크롤) VuMeter and Loading Circle =>
        private fun enableVM(currentPlayStatus: StatusMp, selectedHolder: MyViewHolder) {

            when(currentPlayStatus) {
                StatusMp.PLAY -> {
                    //selectedHolder.iv_Thumbnail.alpha = 0.6f // 어둡게
                    selectedHolder.vuMeterView.visibility = VuMeterView.VISIBLE
                }
                StatusMp.PAUSED -> {
                    Log.d(TAG, "enableVM: .PAUSED called for holder.hashCode= ${selectedHolder.hashCode()}")
                    selectedHolder.vuMeterView.visibility = VuMeterView.VISIBLE
                    //selectedHolder.iv_Thumbnail.alpha = 0.6f // 어둡게
                    Handler(Looper.getMainLooper()).postDelayed({
                        selectedHolder.vuMeterView.pause() // EQ 막대기를 보여줘야하는데 바로 vuMeterView.pause() 때리면 아무것도 안 보임. 따라서 0.1 초 Delay 후 Pause 때림.
                    }, 100)

                    //selectedHolder.vuMeterView.stop(true) // 이것도 얼추 먹히긴 하는데 scroll 하면서 EQ Bar 가 없어짐.

                }
                else -> {} // 이 외 IDLE, ERROR 등일때는 암것도 안함~
            }
        }
        private fun disableVMnLC(unselectedHolder: MyViewHolder) {
            unselectedHolder.loadingCircle.visibility = View.INVISIBLE // 일단 loadingCircle 없애기.
            //unselectedHolder.iv_Thumbnail.alpha = 1.0f // 밝기 원복
            unselectedHolder.vuMeterView.visibility = VuMeterView.GONE // VuMeter 감추기
        }
    // 3) Purchased Check(v) 표시
    private fun enablePurchasedCheck(holder: MyViewHolder) {
        Log.d(TAG, "enablePurchasedCheck: (O) called")
        holder.ivPurchaseCheck.visibility = View.VISIBLE

    }
    private fun disablePurchasedCheck(holder: MyViewHolder) {
        Log.d(TAG, "disablePurchasedCheck: (X) called")
        holder.ivPurchaseCheck.visibility = View.INVISIBLE
    }
    // 3) 무료 아이템 (iapName 이 f 로 시작 ex.f5001) FREE ICON 보여주기.
    private fun showFreeIcon(holder: MyViewHolder) {
        holder.ivFreeIcon.visibility = View.VISIBLE
    }
    private fun hideFreeIcon(holder: MyViewHolder) {
        holder.ivFreeIcon.visibility = View.GONE
    }
// <---------- // Highlight & VuMeter 작동 관련    --------->

    // 스크롤 화면 떨어져나갔다 들어오는거 관련 (EQ Animation 때문에 넣었음!)---------------->
    // a) 스크롤해서 해당 view 가 화면에서 안 보일때
    override fun onViewDetachedFromWindow(holder: MyViewHolder) {
        super.onViewDetachedFromWindow(holder)
        //Log.d(TAG, "!!onViewDETACHEDFromWindow: trId: ${holder.holderTrId}, holder: $holder")
    }

    // b) 다시 스크롤해서 해당 view 가 화면에서 보일때..//스크롤하고 화면 사라졌다 다시 오면 view 번호가 계속 바뀌는 문제.
    override fun onViewAttachedToWindow(holder: MyViewHolder) {
        super.onViewAttachedToWindow(holder)
        //Log.d(TAG,"onViewAttachedToWindow: trId: ${holder.holderTrId},  holder name: $holder, vuMeter Name: ${holder.vuMeterView},")
    }

    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
        Log.d(TAG, "getItemViewType: called")
    }


// Utility -----------------------

    override fun getItemCount(): Int {
        return rtPlusIapInfoList.size
    }


    fun refreshRecyclerView(newList: List<RtInTheCloud>) {
        //Log.d(TAG, "refreshRecyclerView: @@@@@@@@ currentRtList.size (BEFORE): ${currentRtList.size}")

        val oldList = rtPlusIapInfoList //.map { it.copy() }.toList() // 현재 메모리에 떠있던 rtList 내용물을 받아서 새로운 리스트로 만들어줌.

        //Log.d(TAG, "refreshRecyclerView: oldList.hashcode= ${oldList.hashCode()}, newlist.hashcode=${newList.hashCode()}")
        Log.d(TAG, "refreshRecyclerView: oldList=$oldList, \n\n newList=$newList") //어찌하여 둘이 같은가?!?!
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(MyRtDiffCallbackClass(oldList, newList))
        rtPlusIapInfoList = newList
        Log.d(TAG, "refreshRecyclerView: @@@@@@@@ currentRtList.size (AFTER): ${rtPlusIapInfoList.size}")

        diffResult.dispatchUpdatesTo(this)

    }

    // DiffUtil Class
    class MyRtDiffCallbackClass(var oldRingToneList: List<RtInTheCloud>, var newRingToneList: List<RtInTheCloud>) : DiffUtil.Callback() { // Extend by DiffUtil
        override fun getOldListSize(): Int {
            return oldRingToneList.size
        }

        override fun getNewListSize(): Int {
            return newRingToneList.size
        }

        // 1차로 여기서 id 로 판별. (기존 리스트 item 과 새로 받은 리스트 item)
        override fun areItemsTheSame(oldItemPosition: Int,newItemPosition: Int): Boolean { // check if two items represent the same item. 흠.. 다 똑같은지말고 id 만 같아도 true 라는듯..
            //Log.d(TAG, "areItemsTheSame: oldItemPos: $oldItemPosition, newItemPos: $newItemPosition, bool result: ${oldRingToneList[oldItemPosition].id == newRingToneList[newItemPosition].id}")
            return (oldRingToneList[oldItemPosition].id == newRingToneList[newItemPosition].id) // id 의존 왜냐면 id is unique and unchangeable.
        }

        // 1차 결과가 true 일때만 불림-> 1차 선발된 놈들을 2차로 여기서 아예 동일한 놈인지(data 로 파악) 판명.
        override fun areContentsTheSame(oldItemPosition: Int,newItemPosition: Int): Boolean { // 모든 field 가 아예 똑같은건지 확인! (id/url/image 등등) //todo: 신규 구매후 purchaseBool 변경이 감지되서 rcV 업뎃된느지 확인 필요.
            //Log.d(TAG, "areContentsTheSame: oldItemPos: $oldItemPosition, newItemPos: $newItemPosition,  ${oldRingToneList[oldItemPosition] == newRingToneList[newItemPosition]}")
            return (oldRingToneList[oldItemPosition] == newRingToneList[newItemPosition])
        }

    }

    // MyViewHolder class
    inner class MyViewHolder(myXmlToViewObject: View) : RecyclerView.ViewHolder(myXmlToViewObject),View.OnClickListener
    {

        //1) 전체 Slot 을 감싸는 Linear Layout
        val ll_entire_singleSlot: LinearLayout = myXmlToViewObject.findViewById(R.id.id_singleSlot_ll)// HIGHLIGHT 위해 single slot 전체를 감싸는 linear layout 추가

        //2) 왼쪽-중앙 곡 클릭 영역

        val tv1_Title: TextView = myXmlToViewObject.findViewById(R.id.id_tvTitle)
        val tv2_ShortDescription: TextView = myXmlToViewObject.findViewById(R.id.id_tvTags)
        val rl_Including_tv1_2: RelativeLayout = myXmlToViewObject.findViewById(R.id.id_rL_including_title_description)

        // 3) 왼쪽 - 제일 왼쪽 AlbumArt 및 vuMeter/LoadingCircle 영역
        val iv_Thumbnail: ImageView = myXmlToViewObject.findViewById(R.id.id_ivThumbnail)

        // 4) 오른쪽 vumeter /loading circle / Purchased(V) / FREE ICON 표시 영역
        val ivPurchaseCheck: ImageView = myXmlToViewObject.findViewById(R.id.iv_singleSlot_purchased_check)
        val ivFreeIcon: ImageView = myXmlToViewObject.findViewById(R.id.iv_singleSlot_freeicon_container)

        val vuMeterView: VuMeterView = myXmlToViewObject.findViewById(R.id.id_vumeter)
        val loadingCircle: ProgressBar = myXmlToViewObject.findViewById(R.id.id_progressCircle)
        var holderTrId: Int = -10 // 처음엔 의미없는 -10 값을 갖지만, onBindView 에서 제대로 holder.id 로 설정됨.

        // 5) 구매상태
        var isPurchased=false

        init {
            ll_entire_singleSlot.setOnClickListener(this)
            //Log.d(TAG, "MyViewHolder Init: ${myXmlToViewObject.toString()}")
        }


        override fun onClick(v: View?) {

            val clickedView = v
            val clickedPosition = adapterPosition // todo: 이것도. 위에 holderTrId 처럼 holderPosition 으로 설정후 onBindViewHolder 에서 제대로 position 값 입력 가능. -> smoothScrollToPos()과 연계 사용?
            val selectedRt = rtPlusIapInfoList[adapterPosition] // todo: 이거 좀 급하게 바꿨는데 잘되는것 같음 일단은. 면밀한 확인 필요.

            isRVClicked = true // 이거 안쓰이는것 같음..  Recycle View 를 누른적이 있으면 true (혹시나 미리 누를수도 있으므로)

            if (clickedPosition != RecyclerView.NO_POSITION && clickedView != null)
            { // To avoid possible mistake when we delete the item but click it

                        //1-a) 만약 이 RtClass 의 purchase 값이 true 면 일단 holder 에 기록해놓기
                            if(selectedRt.purchaseBool) {
                                this.isPurchased = true
                            }

                        //todo: exoForUrl 에 clickedTrId 기억해놓기.
                        prevClickedHolder = clickedHolder // 이전에 선택되어있던 holder 값을 prevClickedHolder 로 복사. (첫 Click 이라면 prevClick 이 null 이 되겠지 당연히..)
                        clickedHolder = this // clickedHolder = holder

                        GlbVars.clickedTrId = holderTrId // onBindViewHolder 에서 적합한 trID 를 이미 부여받은 상태.
                        Log.d(TAG,"*****************************onClick-To Play MUSIC: Global.clTrId: ${GlbVars.clickedTrId}, holderTrId: $holderTrId ****************")

                        //1-b) 하이라이트 작동  <<그 외 IvThumbNail 어둡게 하기, Loading Circle, Vumeter 등은 Music Play Status 에 따라서 LcVmController() 로 조절>>
                          disableHL(prevClickedHolder)
                          enableHL(clickedHolder)

                        //1-c) 이전 holder [previousHolder] 의 .isPurchased 값이 true 면
                        if(prevClickedHolder!=null && prevClickedHolder!!.isPurchased) {
                            enablePurchasedCheck(prevClickedHolder!!)
                        }

                        //1-d) 음악 플레이 //재생중일때 또 클릭하면 그냥 무시하기?
                        exoForUrlPlay.prepMusicPlayOnlineSrc(holderTrId, true) // 여기서부터 RcVAdapter -> mediaPlayer <-> mpVuModel <-> SecondFrag (Vumeter UI업뎃)

//[음악 재생 대신 Diffutil Test 용 코드] - 구매 후 즉각 RcV 아이콘 변경되는지 확인하기 위한 간접 테스트=> 클릭한 아이템 purchaseBool 값을 인위적으로 true 로 바꿔줌 => 바로 RcV 에 반영되야함!
//jjMainVModel.testDiffutil()

                        // miniPlayer UI 를 업뎃.
                        secondFragListener.onRcvClick(selectedRt,isPurchaseClicked = false) // JjMainViewModel.kt - selectedRt(StateFlow) 값을 업데이트!
                    //}
                //}
            }


        }
    }


}