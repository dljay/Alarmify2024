package com.theglendales.alarm.jjongadd

//import android.app.Fragment
import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment // todo: Keep an eye on this guy..

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjadapters.MyNetWorkChecker
import com.theglendales.alarm.jjadapters.RcViewAdapter
import com.theglendales.alarm.jjdata.GlbVars
import com.theglendales.alarm.jjdata.RingtoneClass
import com.theglendales.alarm.jjmvvm.JjRecyclerViewModel
import com.theglendales.alarm.jjmvvm.JjViewModel
import com.theglendales.alarm.jjmvvm.data.ViewAndTrackIdClass

//Coroutines

/**
 * A simple [Fragment] subclass.
 * Use the [SecondFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

private const val TAG="SecondFragment"
class SecondFragment : androidx.fragment.app.Fragment()  {

    //var fullRtClassList: MutableList<RingtoneClass> = ArrayList()
//    var iapInstance = MyIAPHelper(this,null, ArrayList())




//RcView Related
    lateinit var rcvAdapterInstance: RcViewAdapter
    lateinit var rcView: RecyclerView


//Swipe Refresh
    lateinit var swipeRefreshLayout : SwipeRefreshLayout


//Chip related
    lateinit var chipGroup: ChipGroup
    var myIsChipChecked = false

    private val myNetworkCheckerInstance: MyNetWorkChecker by globalInject() // Koin 으로 아래 줄 대체!! 성공!
    //(DEL) private val myNetworkCheckerInstance by lazy { context?.let { MyNetWorkChecker(it) } }
    //private val firebaseRepoInstance: FirebaseRepoClass by globalInject()

    //Lottie Animation(Loading & Internet Error)
    lateinit var lottieAnimationView: LottieAnimationView

//Sliding Panel Related

    lateinit var slidingUpPanelLayout: SlidingUpPanelLayout    //findViewById(R.id.id_slidingUpPanel)  }

    //a) Sliding Panel: Upper Ui

    lateinit var upperUiHolder: LinearLayout // { this.view?.findViewById(R.id.id_upperUi_ll) }  // 추후 이 부분이 fade out
    lateinit var tv_upperUi_title: TextView // { findViewById<TextView>(R.id.id_upperUi_tv_title) }
    lateinit var iv_upperUi_thumbNail: ImageView //  { findViewById<ImageView>(R.id.id_upperUi_iv_coverImage)  }
    lateinit var iv_upperUi_ClickArrow: ImageView //  { findViewById<ImageView>(R.id.id_upperUi_iv_clickarrowUp) }
    lateinit var cl_upperUi_entireWindow: ConstraintLayout //  {findViewById<ConstraintLayout>(R.id.id_upperUi_ConsLayout)}

    //b) lower Ui
    lateinit var constLayout_entire: ConstraintLayout // {findViewById<ConstraintLayout>(R.id.id_lowerUI_entireConsLayout)}
    lateinit var iv_lowerUi_bigThumbnail: ImageView // {findViewById<ImageView>(R.id.id_lowerUi_iv_bigThumbnail)}
    lateinit var tv_lowerUi_about: TextView // { findViewById<TextView>(R.id.id_lowerUi_tv_Description) }

// Basic overridden functions -- >
    override fun onCreate(savedInstanceState: Bundle?) {
    //Log.d(TAG, "onCreate: jj-called..")
        super.onCreate(savedInstanceState)

    }

//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//        Log.d(TAG, "onActivityCreated: jj-2ndFrag Activity!!Created!!")
//    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment

        Log.d(TAG, "onCreateView: jj- lineNumberTest.. ")
        val view: View = inflater.inflate(R.layout.fragment_second, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated: jj- begins..")
        super.onViewCreated(view, savedInstanceState)
    //RcView-->
            rcView = view.findViewById<RecyclerView>(R.id.id_rcV_2ndFrag)
            rcView.layoutManager = LinearLayoutManager(context)
        //  LIVEDATA ->
        //1)  *** JjRcvViewModel 을 RcView 에 주입. 이것은 오롯이 RcView 에서 받은 Data-> MiniPlayer(BtmSlide) Ui 업뎃에 사용됨! ***
            val jjRcvViewModel = ViewModelProvider(requireActivity()).get(JjRecyclerViewModel::class.java)
            rcvAdapterInstance = activity?.let { RcViewAdapter(ArrayList(), it,jjRcvViewModel) }!! // it = activity. 공갈리스트 넣어서 instance 만듬
        //2) *** 옵저버: RcView 에서 -> 여기로 View 와 TrackId 값을 전달-> UI 갱신
            jjRcvViewModel.selectedRow.observe(viewLifecycleOwner, { viewAndTrIdClassInstance ->
                Log.d(TAG, "onViewCreated: !!! 옵저버!! 트랙ID= ${viewAndTrIdClassInstance.trId}")
                myOnLiveDataReceived(viewAndTrIdClassInstance)
            })
        //  < -- LIVEDATA
            rcView.adapter = rcvAdapterInstance
            rcView.setHasFixedSize(true)
    //RcView <--
        setUpLateInitUis(view) // -> 이 안에서 setUpSlindingPanel() 도 해줌. todo: Coroutine 으로 착착. chain 하지 말고..
    //Chip
        initChip(view)
    // 네트워크 체크-> Lottie 로 연결
        setNetworkAvailabilityListener()
    //MVVM - Livedata Observe Firebase ..
        observeAndLoadFireBase()
    //SwipeRefresh Listener 등록
        registerSwipeRefreshListener()

    //기존 선택해놓은 track 있으면->
        // 3) Activity 에서 onPause 로 갈때 (한번 나갔다오는 경우) onSaveInstanceState 활용해서 사용해도 괜춘할듯. 4) or  Local 에 저장? (ROOM.. 등)

    }


//    override fun onSaveInstanceState(outState: Bundle) { // Activity 에서 call 해야 실행된다하네..
//        super.onSaveInstanceState(outState)
//        Log.d(TAG, "onSaveInstanceState: jj-called.")
//        // 폰에서 나갈 때 (가운데 버튼) 불림 (AlarmsListActivity 의 onPause() 도 불리지..)
//        if(GlbVars.clickedTrId > 0) {
//            outState.putInt("trIdYo",GlbVars.clickedTrId) // <- 이건 onCreateView 에서 쓰임.
//        }
//
//    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: 2nd Frag!")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: 2nd Frag!")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: 2nd Frag!")
    }

// ===================================== My Functions ==== >
    //위에 onCreatedView 에서 observe 하고 있는 LiveData 가 갱신되었을때 다음을 실행
    // 여기서 우리가 받는 view 는 다음 둘중 하나:  rl_Including_tv1_2.setOnClickListener(this) OR! cl_entire_purchase.setOnClickListener(this)
    private fun myOnLiveDataReceived(viewAndTrId: ViewAndTrackIdClass)
    {

        Log.d(TAG, "myOnLiveDataReceived: called")
        val ringtoneClassFromtheList = rcvAdapterInstance.getDataFromMap(viewAndTrId.trId)
        val ivInside_Rc = viewAndTrId.view.findViewById<ImageView>(R.id.id_ivThumbnail) // Recycler View 의 현재 row 에 있는 사진을 variable 로 생성
        // 추후 다른 Frag 갔다 들어왔을 때 화면에 재생시키기 위해. 아래 currentThumbNail 에 임시저장.

        //Sliding Panel - Upper UI
        tv_upperUi_title.text = ringtoneClassFromtheList?.title // miniPlayer(=Upper Ui) 의 Ringtone Title 변경
        tv_upperUi_title.append("                                                 ") // 흐르는 text 위해서. todo: 추후에는 글자 크기 계산-> 정확히 공백 더하기

        //Sliding Panel -  Lower UI
        tv_lowerUi_about.text = ringtoneClassFromtheList?.description

        when(viewAndTrId.view.id) {
            //1) RcView > 왼쪽 큰 영역(album/title) 클릭했을때 처리.
            R.id.id_rL_including_title_description -> {
                //1) Mini Player 사진 변경 (RcView 에 있는 사진 그대로 옮기기)
                if (ivInside_Rc != null) { // 사실 RcView 가 제대로 setup 되어있으면 무조건 null 이 아님! RcView 클릭한 부분에 View 가 로딩된 상태 (사진 로딩 상태 x)

                    iv_upperUi_thumbNail.setImageDrawable(ivInside_Rc.drawable)
                    iv_lowerUi_bigThumbnail.setImageDrawable(ivInside_Rc.drawable)
                }
                //2-1) Mp!! Show mini player & play music right away! + EQ meter fx

    //                mpClassInstance.playMusic(this, trackId, v)//*************************************************** Media Player Related *************************
    //                Log.d(TAG, "myOnItemClick: temp list !!@#!@#!$@@!$!$!@$ templist = $tempList")

                // 최초 SlidingPanel 이 HIDDEN  일때만 열어주기. 이미 EXPAND 상태로 보고 있다면 Panel 은 그냥 둠
                if (slidingUpPanelLayout.panelState == SlidingUpPanelLayout.PanelState.HIDDEN) {
                    slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED // Show Panel! 아리러니하게도 .COLLAPSED 가 (위만) 보이는 상태임!
                }
            }
            // 2) 우측 FREE, GET THIS 클릭했을 때 처리.
            R.id.id_cl_entire_Purchase ->
            {
                Log.d(TAG, "myOnItemClick: You probably clicked FREE or GET This")
                // tvGetThis.text = "Clicked!" <-- 이거 에러남. 잘 됐었는데. 희한..
    //                iapInstance.myOnPurchaseClicked(trackId)
            }

        }
    }
    private fun setSlidingPanelTextOnReturn(vHolder: RcViewAdapter.MyViewHolder?, trackId: Int) { // observeAndLoadFireBase() 여기서 불림. 지금은  comment 처리
        if(vHolder!=null) {
            Log.d(TAG, "setSlidingPanelOnReturn: called. vHolder !=null. TrackId= $trackId")

            val ringtoneClassFromtheList = rcvAdapterInstance.getDataFromMap(trackId)
            //val ivInside_Rc = vHolder.iv_Thumbnail
            Log.d(TAG, "setSlidingPanelOnReturn: title= ${ringtoneClassFromtheList?.title}, description = ${ringtoneClassFromtheList?.description} ")
            //Sliding Panel - Upper UI
            tv_upperUi_title.text = ringtoneClassFromtheList?.title // miniPlayer(=Upper Ui) 의 Ringtone Title 변경
            tv_upperUi_title.append("                                                 ") // 흐르는 text 위해서. todo: 추후에는 글자 크기 계산-> 정확히 공백 더하기

            //Sliding Panel -  Lower UI
            tv_lowerUi_about.text = ringtoneClassFromtheList?.description

        //ImageView 에 들어갈 사진은 LiveData 가 해결해주니. 상관없음.
            //iv_upperUi_thumbNail.setImageDrawable(ivInside_Rc.drawable)
            //iv_lowerUi_bigThumbnail.setImageDrawable(ivInside_Rc.drawable)

            setUpSlidingPanel()

        }

    }
    private fun setNetworkAvailabilityListener() {
    //1-b) API 24 이상이면 콜백까지 등록
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
    {
        myNetworkCheckerInstance.connectivityManager.let {
            it.registerDefaultNetworkCallback(object :
                ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    //Connection is gained.
                    Log.d(TAG, "onAvailable: Internet available: OOOOOOOOOOOOOOOOOOOOO ") //최초 앱 실행시에도 (인터넷이 되니깐) 여기 log 가 작동됨.

                    Handler(Looper.getMainLooper()).post {observeAndLoadFireBase()} // MainThread 에서만 실행해야함. 이거 없으면 크래쉬! (Cannot invoke observe on a backgroudn thread)
                    // 참고: Normally observe(..) and observeForever(..) should be called from the main thread because their
                    // callbacks (Observer<T>.onChanged(T t)) often change the UI which is only possible in the main thread.
                }

                override fun onLost(network: Network) {
                    //connection is lost // 그러나 인터넷 안되는 상태(ex.airplane mode)로 최초 실행시 일로 안 들어옴!!
                    Log.d(TAG, "onLost: Internet available: XXXXXXXXXXXXXXXXXXXXX")
                    lottieAnimController(1)
                }
            })
        }

    }
    //그 외 API 23 이하거나 && 인터넷이 안되는 상태로 app 을 켜면 loadFromFireBase() 실행 (<- 여기서 현재 돌고있는 loading animation 을 인터넷 불가 animation 으로 바꿔줌)
    //return
}
    private fun initChip(v: View) {
    //Chip Related#1 (Init)
    chipGroup = v.findViewById(R.id.id_chipGroup)
    for(i in 0 until chipGroup.childCount) {
        val chip: Chip = chipGroup.getChildAt(i) as Chip
        chip.setOnCheckedChangeListener { buttonView, isChecked ->
            //createStringListFromChips()
            when(isChecked)
            {
                true -> {
                    chip.isChipIconVisible = false

                }
                false -> {
                    chip.isChipIconVisible = true
                    //backToFullRtList()
                    }
                }
            }
        }
    }
    //lottieAnimation Controller = 로딩:0 번, 인터넷에러:1번, 정상:2번(lottie 를 감춰!)
    private fun lottieAnimController(status: Int) {
        when(status) {
            0 ->  { lottieAnimationView.setAnimation(R.raw.lottie_loading1)} //최초 app launch->read.. auto play 기 때문에
            1 -> { activity?.runOnUiThread(Runnable
            {
                Log.d(TAG, "lottieAnimController: NO INTERNET ERROR!!")
                lottieAnimationView.visibility = LottieAnimationView.VISIBLE
                lottieAnimationView.setAnimation(R.raw.lottie_error1)
                Snackbar.make(lottieAnimationView,"Please kindly check your network connection status",
                    Snackbar.LENGTH_LONG).show()


            })
                // 만약 sync(multiple file downloads)/single file download 중였다면 btmSheet 없애기.
                //1) 싱글 다운로드 instance & Multi(obj)
//                MyDownloader.btmShtSingleDNLDInstance.removeSingleDNLDBtmSheet()
//                BtmSht_Sync.removeMultiDNLDBtmSheet()
            }
            2 -> {  activity?.runOnUiThread(Runnable
            {
                lottieAnimationView.cancelAnimation()
                lottieAnimationView.visibility = LottieAnimationView.GONE
            })
            }

        }
    }

    private fun observeAndLoadFireBase() {
    //1. 인터넷 가능한지 체크
    //인터넷되는지 체크

        val isInternetAvailable: Boolean = myNetworkCheckerInstance.isNetWorkAvailable()
        if(!isInternetAvailable) { // 인터넷 사용 불가!
            Log.d(TAG, "loadFromFireBase: isInternetAvailable= $isInternetAvailable")
            lottieAnimController(1)
            return //더이상 firebase 로딩이고 나발이고 진행 안함!!
        }

    //2. If we have internet connectivity, then call FireStore!

        val jjViewModel = ViewModelProvider(requireActivity()).get(JjViewModel::class.java)
        //Log.d(TAG, "onViewCreated: jj LIVEDATA- (Before Loading) jjViewModel.liveRtList: ${jjViewModel.liveRtList.value}")
        jjViewModel.getRtLiveDataObserver().observe(requireActivity(), Observer {
            //Log.d(TAG, "onViewCreated: jj LIVEDATA- (After Loading) jjViewModel.liveRtList: ${jjViewModel.liveRtList.value}")
            it.addOnCompleteListener {
                if(it.isSuccessful) { // Task<QuerySnapshot> is successful 일 때
                    Log.d(TAG, "onViewCreated: <<<<<<<<<loadPostData: successful")

                // 만약 기존에 선택해놓은 row 가 있으면 그쪽으로 이동.
//                    mySmoothScroll()

                // IAP related: Initialize IAP and send instance <- 이게 시간이 젤 오래걸리는듯.
//                    iapInstance = MyIAPHelper(this, rcvAdapterInstance, fullRtClassList) //reInitialize
//                    iapInstance.refreshItemIdsAndMp3UrlMap() // !!!!!!!!!!!!!!여기서 일련의 과정을 거쳐서 rcView 화면 onBindView 까지 해줌!!

                // Update MediaPlayer.kt
//                    mpClassInstance.createMp3UrlMap(fullRtClassList)

                // SwipeRefresh 멈춰 (aka 빙글빙글 animation 멈춰..)
                    if(swipeRefreshLayout.isRefreshing) {
                        Log.d(TAG, "loadPostData: swipeRefresh.isRefreshing = true")
                        swipeRefreshLayout.isRefreshing = false
                    }
                // 우선 lottie Loading animation-stop!!
                    lottieAnimController(2) //stop!

                    val fullRtClassList = it.result!!.toObjects(RingtoneClass::class.java)
                // Update Recycler View
                    showResultAndMore(fullRtClassList)

                // 다른 frag 갔다가 돌아왔을 때 return 했을 때 slidingPanel(miniPlayer) 채워주기.
                    if(GlbVars.clickedTrId>0)
                    {
                        // 1) Highlight the Track
                        Log.d(TAG, "observeAndLoadFireBase: highlight the track")
                        //rcvAdapterInstance.enableHighlightOnTrId(GlbVars.clickedTrId) // default 값은 -1. 즉 -1 이 아니면 뭔가 선택된 상황..
                        val prevSelectedVHolder = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]
                        // 2) Fill in the rest of info
                        setSlidingPanelTextOnReturn(prevSelectedVHolder,GlbVars.clickedTrId)
                    }
                } else { // 에러났을 때
                    lottieAnimController(1)
                    Toast.makeText(this.context,"Error Loading Data from Firebase. Error: ${it.exception.toString()}",Toast.LENGTH_SHORT).show()
                }
            }

        })
    }

    private fun registerSwipeRefreshListener() {
        swipeRefreshLayout.setOnRefreshListener { //setOnRefreshListener 는  function! (SwipeRefreshLayout.OnRefreshListener 인터페이스를 받는) .. 결국 아래는 이름없는 function..?
            Log.d(TAG, "+++++++++++++ inside setOnRefreshListener+++++++++")
            swipeRefreshLayout.isRefreshing = true

            // Chip check 여부에 따라
            if(myIsChipChecked)
            { //하나라도 체크되어있으면
                // Do nothing. Just stop the spinner
                if(swipeRefreshLayout.isRefreshing) {
                    Log.d(TAG, "Chip checked. Doing nothing but stopping the spinner.")
                    swipeRefreshLayout.isRefreshing = false

                }
            }else if(!myIsChipChecked) {
                Handler(Looper.getMainLooper()).post {observeAndLoadFireBase()}
            }

        }
    }

    private fun setUpLateInitUis(v: View) {
    //Lottie
        lottieAnimationView = v.findViewById(R.id.id_lottie_animView)

    //Swipe Refresh Layout Related
        swipeRefreshLayout = v.findViewById(R.id.id_swipeRefreshLayout)

    // SlidingUpPanel
        slidingUpPanelLayout = v.findViewById(R.id.id_slidingUpPanel)
        //a) Sliding Panel: Upper Ui

        upperUiHolder = v.findViewById(R.id.id_upperUi_ll)   // 추후 이 부분이 fade out
        tv_upperUi_title= v.findViewById<TextView>(R.id.id_upperUi_tv_title)
        iv_upperUi_thumbNail= v.findViewById<ImageView>(R.id.id_upperUi_iv_coverImage)
            //iv_upperUi_thumbNail.visibility = View.INVISIBLE // Frag 전환시 placeHolder (빨갱이사진) 보이는 것 방지 위해.
        iv_upperUi_ClickArrow= v.findViewById<ImageView>(R.id.id_upperUi_iv_clickarrowUp)
        cl_upperUi_entireWindow= v.findViewById<ConstraintLayout>(R.id.id_upperUi_ConsLayout)

        //b) lower Ui
        constLayout_entire= v.findViewById<ConstraintLayout>(R.id.id_lowerUI_entireConsLayout)
        iv_lowerUi_bigThumbnail= v.findViewById<ImageView>(R.id.id_lowerUi_iv_bigThumbnail)
            //iv_lowerUi_bigThumbnail.visibility = View.INVISIBLE // Frag 전환시 placeHolder (빨갱이사진) 보이는 것 방지 위해.
        tv_lowerUi_about= v.findViewById<TextView>(R.id.id_lowerUi_tv_Description)

        setUpSlidingPanel()
    }

    private fun setUpSlidingPanel()
    {
        //slidingUpPanelLayout.setDragView(iv_upperUi_ClickArrow) // 클릭 가능 영역을 화살표(^) 로 제한
        slidingUpPanelLayout.setDragView(cl_upperUi_entireWindow)

        // 기존 클릭이 없어서 Panel 이 접혀있지도(COLLAPSED) 확장되지도(EXPANDED) 않은 경우에는 감춰놓기.
        slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.HIDDEN // 일단 클릭전에는 감춰놓기!


        //slidingUpPanelLayout.anchorPoint = 0.6f //화면의 60% 만 올라오게.  그러나 2nd child 의 height 을 match_parent -> 300dp 로 설정해서 이걸 쓸 필요가 없어짐!
        //slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.ANCHORED // 위치를 60%로 초기 시작
        slidingUpPanelLayout.addPanelSlideListener(object :
            SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                // Panel 이 열리고 닫힐때의 callback

                upperUiHolder.alpha = 1 - slideOffset + 0.3f // +0.3 은 살짝~ 보이게끔

                // 트랙 클릭-> 미니플레이어가 등장! (그 이전에는 offset = -xxx 값임.)
                //Log.d(TAG, "onPanelSlide: slideOffset= $slideOffset, rcvAdapterInstance.itemCount=${rcvAdapterInstance.itemCount}")
                val entireListCount = rcvAdapterInstance.itemCount

                if (slideOffset == 0.0f && GlbVars.clickedTrId == entireListCount) { //마지막 트랙 클릭.
                    rcView.post { // 메인 ui 스레드에서는 다른 업무 처리로 바뻐서 다른 thread (워커스레드?) 를 만들어줌.
                        rcView.smoothScrollBy(0, 300) //제일 밑 트랙을 300dp 위로 밀어줌.
                        // todo: 추후 rcView 사이즈 변경될 때 고려 ->정확한 calculation 필요  https://greedy0110.tistory.com/41
                        Log.d(TAG, "myOnItemClick: 살짝 슬라이드! 마지막 트랙 보이게!")
                        //Log.d(TAG, "onPanelSlide: entirelistcount: $entireListCount")
                    }
                }

                // 완전히 펼쳐질 때
                if (!slidingUpPanelLayout.isOverlayed && slideOffset > 0.2f) { //안겹치게 설정된 상태에서 panel 이 열리는 중 (20%만 열리면 바로 모퉁이 감추기!)
                    //Log.d(TAG, "onPanelSlide: Hiding 모퉁이! yo! ")
                    slidingUpPanelLayout.isOverlayed =true // 모퉁이 edge 없애기 위해. Default 는 안 겹치게 false 값.
                }

            }

            @SuppressLint("ClickableViewAccessibility") // 아래 constLayout_entire.setxx... 이거 장애인 warning 없애기
            override fun onPanelStateChanged(
                panel: View?,
                previousState: SlidingUpPanelLayout.PanelState?,
                newState: SlidingUpPanelLayout.PanelState?
            ) {

                when (newState) {
                    SlidingUpPanelLayout.PanelState.EXPANDED -> {
                        //Log.d(TAG, "onPanelStateChanged: Sliding Panel Expanded")
                        iv_upperUi_ClickArrow.setImageResource(R.drawable.clickarrow_down)// ^ arrow 전환 visibility }

                        // 계속 click 이 투과되는 문제(뒤에 recyclerView 의 버튼 클릭을 함)를 다음과같이 해결. 위에 나온 lowerUi 의 constraint layout 에 touch를 허용.
                        constLayout_entire.setOnTouchListener { v, event -> true }

                    }
                    SlidingUpPanelLayout.PanelState.COLLAPSED -> {
                        //Log.d(TAG, "onPanelStateChanged: Sliding Panel Collapsed")
                        iv_upperUi_ClickArrow.setImageResource(R.drawable.clickarrow)// ^ arrow 전환 visibility }
                        slidingUpPanelLayout.isOverlayed =
                            false // 이렇게해야 rcView contents 와 안겹침 = (마지막 칸)이 자동으로 panel 위로 올라가서 보임.
                    }
                }
            }
        })

        //Title Scroll horizontally. 흐르는 텍스트
        tv_upperUi_title.apply {
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.MARQUEE
            isSelected = true
            //text ="Song Title                                           "
            // text 제목이 일정 수준 이하면 여백을 추가, 추후 title.length < xx => 정확한 카운트 알고리즘.
        }
    }


    fun showResultAndMore(fullRtClassList: MutableList<RingtoneClass>) {
        Log.d(TAG, "showResult: 5) called..Finally! ")
        // 만약 기존에 선택해놓은 row 가 있으면 그쪽으로 이동.
//                mySmoothScroll()


        // IAP related: Initialize IAP and send instance <- 이게 시간이 젤 오래걸리는듯.

//                iapInstance = MyIAPHelper(this, rcvAdapterInstance, fullRtClassList) //reInitialize
//                iapInstance.refreshItemIdsAndMp3UrlMap() // !!!!!!!!!!!!!!여기서 일련의 과정을 거쳐서 rcView 화면 onBindView 까지 해줌!!

        // Update MediaPlayer.kt
//                mpClassInstance.createMp3UrlMap(fullRtClassList)

        // Update Recycler View
        rcvAdapterInstance.updateRecyclerView(fullRtClassList) // todo: 추후 // comment 시킬것. MyIAPHelper.kt 에서 해주기로 함!
        rcvAdapterInstance.updateRingToneMap(fullRtClassList)// todo: 이 map 안 쓰이는것 같은데 흐음.. (우리는 Map 기반이므로 list 정보를 -> 모두 Map 으로 업데이트!)

        // SwipeRefresh 멈춰 (aka 빙글빙글 animation 멈춰..)
//                if(swipeRefreshLayout.isRefreshing) {
//                    Log.d(TAG, "loadPostData: swipeRefresh.isRefreshing = true")
//                    swipeRefreshLayout.isRefreshing = false
//                }
        // 우선 lottie Loading animation-stop!!
//                lottieAnimController(2) //stop!
    }


}
