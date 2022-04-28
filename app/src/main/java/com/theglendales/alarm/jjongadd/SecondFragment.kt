package com.theglendales.alarm.jjongadd

//import android.app.Fragment
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment // todo: Keep an eye on this guy..

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle.State


import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjadapters.GlideApp
import com.theglendales.alarm.jjadapters.MyNetWorkChecker
import com.theglendales.alarm.jjadapters.RcCommInterface
import com.theglendales.alarm.jjadapters.RcViewAdapter
import com.theglendales.alarm.jjdata.GlbVars
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.*
import com.theglendales.alarm.jjmvvm.iapAndDnldManager.*

import com.theglendales.alarm.jjmvvm.mediaplayer.ExoForUrl
import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp
import com.theglendales.alarm.jjmvvm.util.LottieAnimHandler
import com.theglendales.alarm.jjmvvm.util.LottieENUM
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import kotlinx.coroutines.launch


//Coroutines

/**
 * A simple [Fragment] subclass.
 * Use the [SecondFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

private const val TAG = "SecondFragment"

class SecondFragment : androidx.fragment.app.Fragment() {

    //Download 관련
    lateinit var btmSht_SingleDNLDV: BtmShtSingleDNLDV2
    //Network Checker
    lateinit var myNetworkCheckerInstance: MyNetWorkChecker
    //Main ViewModel  생성
    lateinit var jjMainVModel: JjMainViewModel // [LiveData] + [Flow]
    //private val jjMainVModel: JjMainViewModel by viewModels()  // <- 이렇게 등록했을 때 listFrag 갔다와서 구입 클릭하면 -> coroutines.JobCancellationException: Job was cancelled 에러뜸..
    //Toast Messenger
    private val toastMessenger: ToastMessenger by globalInject() //ToastMessenger
    //BtmSheet - GooglePlay Store 못 갈 때 에러 보여주기.
    private val myBtmSheetPSError = BtmSheetPlayStoreError // OBJECT 로 만들었음! BottomSheet 하나만 뜨게하기 위해!


    //RcView Related
    lateinit var rcvAdapterInstance: RcViewAdapter
    lateinit var rcView: RecyclerView
    lateinit var layoutManager: LinearLayoutManager
    lateinit var flRcView: FrameLayout // RcView 를 감싸고 있는 FrameLayout -- 마지막 SLOT 보이게 하기 위해서 runTime Padding 조절 (흰색 칸 없앨때는 Padding 없애고)


    //Swipe Refresh
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    //Chip related
    lateinit var chipGroup: ChipGroup
    var myIsChipChecked = false

    //Lottie Animation(Loading & Internet Error) + LoadingCircle(로티 X) 관련
    lateinit var lottieAnimationView: LottieAnimationView
    lateinit var lottieAnimHandler: LottieAnimHandler
    lateinit var centerLoadingCircle: CircularProgressIndicator
    lateinit var frameLayoutForCircle: FrameLayout

    //Media Player & MiniPlayer Related
    private val exoForUrlPlay: ExoForUrl by globalInject()


    //Sliding Panel Related
    var shouldPanelBeVisible = false
    lateinit var slidingUpPanelLayout: SlidingUpPanelLayout    //findViewById(R.id.id_slidingUpPanel)  }
    lateinit var btmNavViewFromActivity: BottomNavigationView
    lateinit var btmAppBarFromActivity: BottomAppBar

    //a) Sliding Panel: Upper Ui

    lateinit var upperUiHolder: LinearLayout // { this.view?.findViewById(R.id.id_upperUi_ll) }  // 추후 이 부분이 fade out
    lateinit var tv_upperUi_title: TextView // { findViewById<TextView>(R.id.id_upperUi_tv_title) }
    lateinit var iv_upperUi_thumbNail: ImageView //  { findViewById<ImageView>(R.id.id_upperUi_iv_coverImage)  }
    lateinit var iv_upperUi_ClickArrow: ImageView //  { findViewById<ImageView>(R.id.id_upperUi_iv_clickarrowUp) }
    lateinit var cl_upperUi_entireWindow: ConstraintLayout //  {findViewById<ConstraintLayout>(R.id.id_upperUi_ConsLayout)}
    lateinit var imgbtn_Play: ImageButton
    lateinit var imgbtn_Pause: ImageButton
    lateinit var seekBar: SeekBar


    //b) lower Ui
    lateinit var constLayout_entire: ConstraintLayout // {findViewById<ConstraintLayout>(R.id.id_lowerUI_entireConsLayout)}
    lateinit var iv_lowerUi_bigThumbnail: ImageView // {findViewById<ImageView>(R.id.id_lowerUi_iv_bigThumbnail)}
    lateinit var tv_lowerUi_about: TextView // { findViewById<TextView>(R.id.id_lowerUi_tv_Description) }
    lateinit var mPlayer_bdg1_intense: ImageView
    lateinit var mPlayer_bdg2_gentle: ImageView
    lateinit var mPlayer_bdg3_nature: ImageView
    lateinit var mPlayer_bdg4_location: ImageView
    lateinit var mPlayer_bdg5_popular: ImageView
    lateinit var mPlayer_bdg6_misc: ImageView
    //lateinit var tv_price: TextView
    lateinit var btn_buyThis: Button
    lateinit var purchased_check_icon: TextView


    // listfrag 가거나 나갔다왔을 때 관련.
    //var isFireBaseFetchDone = false // a) 최초 rcV 열어서 모든게 준비되면 =true, b) 다른 frag 로 나갔다왔을 때 reconstructXX() 다 끝나면 true.
    var currentClickedTrId = -1

    //Firebase 관련
    //var fullRtClassList: List<RtInTheCloud> = ArrayList()

    // Basic overridden functions -- >
    override fun onCreate(savedInstanceState: Bundle?) {

        //isFireBaseFetchDone=false // ListFrag 갔을 때 이 값이 계속 true 로 있길래. 여기서 false 로 해줌. -> fb 로딩 끝나면 true 로 변함.
        //Log.d(TAG, "onCreate: jj-called..isEverythingReady=$isFireBaseFetchDone, currentClickedTrId=$currentClickedTrId")
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        Log.d(TAG, "onCreateView: jj-called.")
        val view: View = inflater.inflate(R.layout.fragment_second, container, false)
        return view
    }

    @RequiresApi(Build.VERSION_CODES.N) // API 24
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        Log.d(TAG, "onViewCreated: jj- begins..")
        super.onViewCreated(view, savedInstanceState)

    //RcView-->
        rcView = view.findViewById<RecyclerView>(R.id.id_rcV_2ndFrag)
        layoutManager = LinearLayoutManager(context)
        rcView.layoutManager = layoutManager
        rcView.isNestedScrollingEnabled =false // // !! 중요!! 이걸 설정해놓아야 ListActivity>collapsingToolBarLayout 이 현재 RcV 의 Scroll 에 반응해서 열리거나 Collapse 되지 않는다!
    // 다음과 같이 설정도 가능 -> ViewCompat.setNestedScrollingEnabled(rcView, false)

        //rcV 현재 보이는 마지막 칸 Listener
        /*rcView.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val totalItemCount = layoutManager.itemCount
                val lastVisible= layoutManager.findLastCompletelyVisibleItemPosition()
                Log.d(TAG, "onScrolled: lastVisible=$lastVisible")
                if(lastVisible >= totalItemCount -1) {
                    Log.d(TAG, "onScrolled: JjongHyuk Last Visible. ")
                }
            }
        })*/

    //BtmSht_SingleDnld init (싱글톤으로)
        btmSht_SingleDNLDV = BtmShtSingleDNLDV2.newInstance()

    //  LIVEDATA ->

        //1) ViewModel 생성
        jjMainVModel = ViewModelProvider(requireActivity()).get(JjMainViewModel::class.java)

        //2) LiveData Observe
        //Media Player ViewMODEL Observe
            //2-B-가) MP: MediaPlayer 에서의 Play 상태(loading/play/pause) 업뎃을 observe
        jjMainVModel.getMpStatusLiveData().observe(viewLifecycleOwner) { StatusEnum ->
            Log.d(TAG, "MpViewModel-mpStatus) 옵저버! Current Music Play Status: $StatusEnum")
            // a) MiniPlayer Play() Pause UI 업데이트 (현재 SecondFragment.kt 에서 해결)
            when (StatusEnum) {
                StatusMp.PLAY -> {
                    showMiniPlayerPauseBtn()
                } // 최초의 ▶,⏸ 아이콘 변경을 위하여 사용. 그후에는 해당버튼 Click -> showMiniPlayerPause/Play 실행됨.
                StatusMp.BUFFERING -> {
                    showMiniPlayerPlayBtn()
                }
                StatusMp.ERROR -> {
                    showMiniPlayerPlayBtn()
                }
                StatusMp.PAUSED -> {
                    showMiniPlayerPlayBtn()
                }
            }
            // b) VuMeter/MP Loading Circle 등 UI 컨트롤
            rcvAdapterInstance.lcVmIvController(StatusEnum) // 원복후 불러도 Prev/CurrentHolder 는 어차피 null 이기에 상관없음.

        }

        //2-B-나) MP: seekbar 업뎃을 위한 현재 곡의 길이(.duration) observe. (ExoForLocal -> JjMpViewModel-> 여기로)
        jjMainVModel.getSongDurationLiveData().observe(viewLifecycleOwner) { dur ->
            Log.d(TAG, "MpViewModel-songDuration duration received = ${dur.toInt()}")
            seekBar.max = dur.toInt()
            // c) **GlbVar 저장용 **
            //GlbVars.seekBarMax = dur.toInt()
        }
        //2-B-다) MP: seekbar 업뎃을 위한 현재 곡의 길이(.duration) observe. (ExoForLocal -> JjMpViewModel-> 여기로)
        jjMainVModel.getCurrentPosLiveData().observe(viewLifecycleOwner) { playbackPos ->
            //Log.d(TAG, "onViewCreated: playback Pos=${playbackPos.toInt()} ")
            seekBar.progress = playbackPos.toInt() + 200
            // c) **GlbVars 저장용 ** 현재 재생중인 seekbar 위치
            //GlbVars.seekbarProgress = playbackPos.toInt() +200
            //GlbVars.playbackPos = playbackPos
        }


        //Fragments should always use the viewLifecycleOwner to trigger UI updates.
             viewLifecycleOwner.lifecycleScope.launch {
                //repeatOnLifeCycle() : 이 블록 안은 이 lifecycle 의 onStart() 에서 실행- onStop() 에서 cancel. lifecycle 시작하면 자동 re-launch!
                viewLifecycleOwner.repeatOnLifecycle(State.RESUMED) {
                    launch {
                        jjMainVModel.selectedRow.collect { rtInTheCloudObj -> currentClickedTrId = rtInTheCloudObj.id
                            Log.d(TAG,"[MainVModel <0> - Selected Row] !!!  옵저버!! 트랙ID= ${rtInTheCloudObj.id}, \n currentClickedTrId=$currentClickedTrId")
                                updateMiniPlayerUiOnClick(rtInTheCloudObj) // 동시에 ListFrag 갔다왔을때도 이걸 통해서 [복원]
                         }
                    }
                    // BillingService Disconnect 됐을 때 refreshFbIAP() 해주는 로직인데 의미 없어서 뺐음.
                    /*launch {
                        jjMainVModel.getBillingDisconnectedAlert().collect {
                            Log.d(TAG, "[MainVModel <0.5> - Billing Disconnected] called it=$it") // 뜨긴 떴다!!
                            when(it) {
                                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> { //-1 값
                                    Log.d(TAG, "[MainVModel <0.5> - Billing Disconnected] Billing Service got Disconnected 된 듯!!")
                                    //jjMainVModel.refreshFbAndIAPInfo()
                                }
                            }
                        }
                    }*/
                }
            }
        //5)이제 ViewModel 들을 넘김: RcvAdapter & MediaPlayer & MiniPlayer Instance 생성.
            //mpClassInstance_V1 = activity?.let {ExoForLocal(it, jjMpViewModel)}!!
            rcvAdapterInstance = activity?.let {RcViewAdapter(ArrayList(),it,secondFragListener)}!! // it = activity. 공갈리스트 넣어서 instance 만듬 //todo: okay to pass VModel to Adapter?
            myNetworkCheckerInstance = context?.let { MyNetWorkChecker(it, jjMainVModel) }!!

        //0) 2021.1.6 MainViewModel //todo: 이거 flow 로 바꾸고 lottieAnim("loading") 과 타이밍 비교. 여기 저~~기 위에 써주기 (어차피 onStart() 에서 불릴테니깐)

        //[MainVModel-1] 1) [네트워크 사용O] Fb 에서 새로운 리스트를 받음/새로고침 2) [네트워크 사용X] a)신규 구매 후 리스트 변화. b) 단순 listFrag<->SecondFrag 복귀 후 livedata 기존 값 복기
            jjMainVModel.rtInTheCloudList.observe(viewLifecycleOwner) {rtListPlusIAPInfo->
                Log.d(TAG, "---------------------- [MainVModel <1> - RTLIST] rtListFromFb received. Size= ${rtListPlusIAPInfo.size}")
                // A) 빈깡통 리스트 받았을 때: 모든 에러 상황에서 빈깡통 리스트를 받음.
                if(rtListPlusIAPInfo.isNullOrEmpty()) {
                    lottieAnimHandler.animController(LottieENUM.STOP_ALL) // 일단 최초 Loading Animation 이 돌고있었다면 Stop
                    swipeRefreshLayout.isRefreshing = false // 새로고침 빙글빙글 있었다면 = false
                    lottieAnimHandler.animController(LottieENUM.ERROR_GENERAL) // 1) Error Lottie 띄워주기
                }

                // B) 제대로 된 리스트 받았을 때 (인터넷 안되면 SharedPref 에서라도 예전에 저장해놓은 리스트를 받음)
                else {
                    if(BtmSheetPlayStoreError.isAdded) {BtmSheetPlayStoreError.removePlayErrorBtmSheet()} // PlayStore 로긴 안되있어 뜬 상태로 -> 로긴 후 복귀했을 때 -> BTMSheet 없애주기.

                    exoForUrlPlay.createMp3UrlMap(rtListPlusIAPInfo)
                    if(myIsChipChecked) { //Chip 이 하나 이상 선택된 경우
                        val tagsList = getTagsList()
                        val filteredList = getFilteredList(rtListPlusIAPInfo, tagsList) // Filtered 된 List 를 받고
                        rcvAdapterInstance.refreshRecyclerView(filteredList)
                    } else { // Chip 이 선택 안된 경우.
                        rcvAdapterInstance.refreshRecyclerView(rtListPlusIAPInfo)
                    }
                    lottieAnimHandler.animController(LottieENUM.STOP_ALL) // 어떤 Animation 이 있었던 Stop!
                    swipeRefreshLayout.isRefreshing = false // 새로고침 빙글빙글 있었다면 = false
                }
            }
        //[MainVModel- <1>-ERROR]
            jjMainVModel.errorIntLiveData.observe(viewLifecycleOwner) { errorIntCode ->
                when(errorIntCode) { // 0 = JjPlayStoreUnAvailableException
                    0 -> {myBtmSheetPSError.showBtmSheetPlayStoreError(requireActivity()) //Alert 창 보여주고-> 클릭시 -> PlayStore 로 이동
                    }
                    else -> {} //현재 다른 코드 없음.
                }

            }
        //[MainVModel-2] Network Availability 관련 (listFrag->SecondFrag 오면 두번 들어옴. 1) livedata 기존 값 복기 2)SecondFrag 시작하면서 setNetworkListener()
            jjMainVModel.isNetworkWorking.observe(viewLifecycleOwner) { isNetworkWorking ->
                Log.d(TAG, "[MainVModel <2> - NT] Network Availability detected, isNetworkWorking=[$isNetworkWorking] ")

                //A-1) true && false (기존에 O 지금은 X) or A-2) false && false (기존도 X 지금도 X) - 혹시 몰라서 넣음.
                if(jjMainVModel.prevNT && !isNetworkWorking || !jjMainVModel.prevNT && !isNetworkWorking  ) {
                    Log.d(TAG, "[MainVModel <2> - NT] Network Error! Launch Lottie!")
                    lottieAnimHandler.animController(LottieENUM.ERROR_GENERAL)

                    snackBarDeliverer(requireActivity().findViewById(android.R.id.content),"Please kindly check your network connection status",false)
                    //toastMessenger.showMyToast("Error: Unable to connect",isShort = true)
                }
                //B) false && true (기존에 X 지금은 O)
                else if(!jjMainVModel.prevNT && isNetworkWorking) {
                    Log.d(TAG, "[MainVModel <2> - NT] Network Working Again! Remove Error Lottie and Relaunch FB!!")
                    lottieAnimHandler.animController(LottieENUM.STOP_ALL)
                    jjMainVModel.refreshFbAndIAPInfo() // Relaunch FB! -> 사실 lottie 가 사라지면서 기존 RcView 가 보여서 상관없긴 하지만.애초 Network 불가상태로 접속해 있을 수 있음.
                }
                jjMainVModel.prevNT = isNetworkWorking // 여기서 ViewModel 안의 값을 바꿔줌에 따라 위에서처럼 Bool 값 prev&now 변화를 감지 할 수 있음.

            }
        //[MainVModel-3] (구매 전) 클릭 -> Purchase 창 뜨기전까지 Loading Circle 보여주고 없애기
            jjMainVModel.purchaseLoadingCircleSwitch.observe(viewLifecycleOwner) { onOffNumber ->
                Log.d(TAG, "[MainVModel <3> - centerLoadingCircleSwitch] Valued Received=$onOffNumber ") // 0: 보여주기, 1: 끄기.
                when(onOffNumber){
                    0 -> {frameLayoutForCircle.visibility = View.VISIBLE
                        centerLoadingCircle.visibility = View.VISIBLE} // 보여주기(O)
                    1 -> {frameLayoutForCircle.visibility = View.GONE} // 끄기(X)
                    2 -> {centerLoadingCircle.visibility = View.GONE}// 2 -> circle 만 없애주기 ()
                }
            }
        //[MainVModel-4] (구매 후) Single DNLD -> UI 반영 (DnldPanel 보여주기 등)
            jjMainVModel.getLiveDataSingleDownloader().observe(viewLifecycleOwner) { dnldInfo->
                Log.d(TAG, "[MainVModel <4> -DNLD-A] Title=${dnldInfo.dnldTrTitle}, Status=${dnldInfo.status}, Prgrs=${dnldInfo.prgrs} ")

                //A) Prgrs 를 받는순간 isPreparingToDNLD -> false -> Lottie Loading Circle (X), ProgressBar(O)
                when(dnldInfo.isBufferingToDNLD) { // isBufferingToDNLD(X)
                    false -> {btmSht_SingleDNLDV.showLPIAndHideLottieCircle()} // 계속 불리게 되지만 showLPIAndHideLottieCircle() 안에서 자체적으로 중복 call 확인 후 return.
                }
                //B) STATUS 에 따라서 BtmSheet 열기 & 닫기 (모든 Status 는 한번씩만 받는다)
                when(dnldInfo.status) { // 참고** -1= IDLE 암것도 안한 상태, 0= Download 준비 시작!, Pending=1 , Running=2, Paused=4, Successful=8, Failed=16
                    -1 -> { // 내가 지정- IDLE 상태
                        //btmSht_SingleDNLDV.removeBtmSheetImmediately() // 없어도 됨. 그리고 이거 있으면 -> Successful 해서 1초간 만땅 Prgrs Bar 보여주던게 바로 꺼짐. very unsatisfying..
                    }
                    0 -> { // 내가 지정- 다운로드 attempt 시작하자마자. (0 은 그냥 내가 지정한 숫자) -> BtmSheet 을 열어줘! +
                        Log.d(TAG, "[MainVModel <4> -DNLD-B] STATUS=0 ")
                            btmSht_SingleDNLDV.show(requireActivity().supportFragmentManager, btmSht_SingleDNLDV.tag)}

                    DownloadManager.STATUS_FAILED -> { //16
                        Log.d(TAG, "[MainVModel <4> -DNLD-B] STATUS=FAILED(16) Observer: !!!! DNLD FAILED (XX) !!!!! ")
                        //remove BTMSHEET & Show Warning Snackbar
                        btmSht_SingleDNLDV.removeBtmSheetAfterOneSec()
                        snackBarDeliverer(requireActivity().findViewById(android.R.id.content), "Download Failed. Please check your network connectivity", false)
                        return@observe
                    }
                    DownloadManager.STATUS_SUCCESSFUL-> { //8 <- 다시 secondFrag 들어왔을 때 뜰 수 있음.
                        Log.d(TAG, "[MainVModel <4> -DNLD-B] STATUS=SUCCESSFUL(8) Observer: DNLD SUCCESS (O)  ")
                        // Prgrs Bar 만빵으로 채워주고 -> BtmSheet 없애주기 (만빵 안 차면 약간 허탈..)
                        btmSht_SingleDNLDV.animateLPI(100,1) //  그래프 만땅= 100 으로 설정해줬음.
                        btmSht_SingleDNLDV.removeBtmSheetAfterOneSec() //1 초 Delay 후 btmSheet 없애주기.
                        snackBarDeliverer(requireActivity().findViewById(android.R.id.content), "DOWNLOAD COMPLETED.", false)
                        return@observe
                    }
                    -444 -> { // VModel> Coroutine > .invokeOnCompletion 에서 handler 가 에러 감지 (내가 임의로 넣은 숫자 -444)
                        Log.d(TAG, "[MainVModel <4> -DNLD-B] STATUS=-444")
                        btmSht_SingleDNLDV.removeBtmSheetImmediately() // 에러메시지는 ViewModel 에서 Toast 로 전파. //
                        toastMessenger.showMyToast("Download Failed..", isShort = false)
                        return@observe
                    }
                   /* else -> {btmSht_SingleDNLDV.removeBtmSheetImmediately() // 다운로드 실패- 내가 만든 -444 코드나 그 외 Status 를 받으면 -> 바로 BtmSht 없애고 + Toast 메시지
                        toastMessenger.showMyToast("Download Failed..Status Code=${dnldInfo.status}",isShort = false)
                        return@observe
                        //snackBarDeliverer(requireActivity().findViewById(android.R.id.content), "Unknown Download Status received. Status Code=${dnldInfo.status}", false)
                    }*/
                }
                //C) Progress Animation
                if(dnldInfo.prgrs > 0 ) {
                    Log.d(TAG, "[MainVModel <4> -DNLD-C] Prgrs Animation! (prgrs=${dnldInfo.prgrs})")
                    btmSht_SingleDNLDV.prepAndAnimateLPI(dnldInfo.prgrs) // 그래프 만땅= 100 .
                    btmSht_SingleDNLDV.updateTitleTextView(dnldInfo.dnldTrTitle) // Tr Title 보여주기 (첫 Prgrs 받는 순간 반영. 이후 prgrs 받을 때마다 setText 되지만. 상관 없을듯..)
                }


            }
        //[MainVModel-5] [멀티 다운로드]
            jjMainVModel.getLiveDataMultiDownloader().observe(viewLifecycleOwner, {stateEnum ->
                Log.d(TAG, "onViewCreated:[MainVModel <5> - 멀티다운로드] StateEnum=$stateEnum , Thread=${Thread.currentThread().name}")
                when(stateEnum) {
                    MultiDnldState.IDLE -> {Log.d(TAG, "onViewCreated: received idle, do nothing..")}
                    MultiDnldState.ERROR -> {snackBarDeliverer(requireActivity().findViewById(android.R.id.content),"UNABLE TO RECOVER PURCHASED ITEMS.", false)}
                    MultiDnldState.SUCCESSFUL -> {snackBarDeliverer(requireActivity().findViewById(android.R.id.content),"RECOVERING PREVIOUSLY OWNED ITEMS ..", false)}
                }
            })
      /*  //[MainVModel-5] [PurchaseState] // 유저가 구매창 입력한 결과 Observe
        jjMainVModel.getPurchaseState().observe(viewLifecycleOwner, {purchaseStateEnum ->
            Log.d(TAG, "onViewCreated:[MainVModel-PURCHASE STATE] PurchaseState=$purchaseStateEnum , Thread=${Thread.currentThread().name}")
            when(purchaseStateEnum) {
                PurchaseStateENUM.IDLE -> {Log.d(TAG, "onViewCreated: [purchase state] received idle, do nothing..")}
                PurchaseStateENUM.PURCHASED -> {jjMainVModel.downloadPurchased()}
                PurchaseStateENUM.ERROR -> {snackBarDeliverer(requireActivity().findViewById(android.R.id.content),"UNABLE TO RECOVER PURCHASED ITEMS.", false)}
                PurchaseStateENUM.CANCELED -> {snackBarDeliverer(requireActivity().findViewById(android.R.id.content),"RECOVERING PREVIOUSLY OWNED ITEMS ..", false)}

            }
        })*/

    //  < -- LIVEDATA
        rcView.adapter = rcvAdapterInstance
        rcView.setHasFixedSize(true)
        //RcView <--
    // 네트워크 체크-> MyNetworkChecker -> ViewModel -> SecondFrag 의 Lottie 로 연결
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //Nougat=API 24. todo: MINIMUM SDK 변경?
            Log.d(TAG, "onViewCreated: network call back- registered ")
            myNetworkCheckerInstance.setNetworkListener() //annotated element should only be called on the given API level or higher
        }
        setUpLateInitUis(view) // -> 이 안에서 setUpSlindingPanel() 도 해줌. todo: Coroutine 으로 착착. chain 하지 말고..
        //Chip
        setChipListener(view)
        //setNetworkAvailabilityListener() // 처음 SecondFrag 를 열면 여기서 network 확인 -> 이후 connectivity yes/no 상황에 따라 -> lottie anim 보여주기 + re-connect.
        registerSwipeRefreshListener()



    }

    override fun onStart() {
        super.onStart()
        //Log.d(TAG, "onStart: 2nd Frag // viewLifecycleOwner.lifecycle.currentState=${viewLifecycleOwner.lifecycle.currentState}")
        Log.d(TAG, "onStart: 2nd Frag // lifecycle.currentState=${lifecycle.currentState}")
    }
    override fun onResume() {
        super.onResume()
        //todo: 어떤 사유로든 Fb+IAP 로딩이 실패해서 돌아왔을때 자동으로 Refresh 하는 로직  (ex.PlayStore Sign-in 하고 돌아왔을때..)
        Log.d(TAG, "onResume: 2nd Frag! // lifecycle.currentState=${lifecycle.currentState} // PanelState= ${slidingUpPanelLayout.panelState}")


    //A) 돌아왔을 때 SlidingUpPanel 상태 복원 - 여기 onResume 에서 해주는게 맞음.
        when(slidingUpPanelLayout.panelState) {
            SlidingUpPanelLayout.PanelState.HIDDEN -> {flRcView.setPadding(0,0,0,140)}
            SlidingUpPanelLayout.PanelState.COLLAPSED -> {collapseSlidingPanel()
                flRcView.setPadding(0,0,0,0)
            }
            SlidingUpPanelLayout.PanelState.EXPANDED -> {expandSlidingPanel()
                flRcView.setPadding(0,0,0,0)
            }
        }

    // B) <1> 어떤 이유로 에러가 나서 RtIAPList 받지 못한 상태에서 나갔다 다시 복귀
        // <2> GooglePlayStore 로그인 하라는 btmSheet 을 보여준뒤 복귀했을때! -> refreshFbIAp() 해줌 -> 완료되면 -> 여기서 observe 중이던곳에서 new 리스트 확인 후 -> BtmSheet 삭제됨!
        if(BtmSheetPlayStoreError.isAdded) { //todo: .isAdded | 혹은 다른 에러로 나갔따 왔을 때 refreshFB() 필요한 경우..

        lottieAnimHandler.animController(LottieENUM.INIT_LOADING) //우선  빙글빙글 Init Loading 작동 -> (추후) Fb 에서 리스트 받는대로 없애줌.
        jjMainVModel.refreshFbAndIAPInfo() // refreshFbIAP 해서 GooglePlay 에 이제는 로긴 되어있다면) -> Fb 에서 공갈 아닌 리스트 받아서 lottieAnim 과 BtmSheetPsError 둘 다 없애줄것임.
        }
    // C) CollapsingToolbarLayout 이 혹시라도 확장되서 보여지는것을 방지위해 (위에 onViewCreated 에도 있으나 대책없이 일단 여기 넣어놨음..)
        if(rcView.isActivated) {
            rcView.isNestedScrollingEnabled =false //
        }
    // DNLD BTM SHEET 보여주기 관련 - 이것은 Permission과도 관련되어 있어서?  신중한 접근 필요. (Update: permission 상관없는듯..)
    // 현재 기본 WRITE_EXTERNAL Permission 은 AlarmsListActivity 에서 이뤄지는 중.
//        //B) 현재 Sync = Multi 다운로드가 진행중 && 인터넷이 되는 상태면 btmSheet_Multi 다시 보여주기!
//        if(MyDownloader_v1.isSyncInProcess && myNetworkCheckerInstance.isNetWorkAvailable())
//        {
//            BtmSht_Sync.showBtmSyncDialog(this)
//
//        }
//        //C) 현재 Single 다운로드가 진행중 && 인터넷이 되는상태면 btmSheet_Single 다시 보여주기!
//        else if(MyDownloader_v1.isSingleDNLDInProcess && myNetworkCheckerInstance.isNetWorkAvailable()) {
//            MyDownloader_v1.btmShtSingleDNLDInstance.showBtmSingleDNLDSheet(this)
//        }

    }
    override fun onPause() {
        super.onPause()
        jjMainVModel.triggerPurchaseLoadingCircle(1) // Purchase 로딩 중 나갔다 들어오면 내가 심은 LoadingCircle 때문에 화면이 어두컴컴)
        Log.d(TAG, "onPause: 2nd Frag! // viewLifecycleOwner.lifecycle.currentState=${viewLifecycleOwner.lifecycle.currentState}")
        Log.d(TAG, "onPause: 2nd Frag! // lifecycle.currentState=${lifecycle.currentState}")
        //collapseSlidingPanel()
        //1) 현재 음악이 재생중이든 아니든 (재생중이 아니었으면 어차피 pauseMusic() 은 의미가 없음)
        exoForUrlPlay.pauseMusic() // a)일단 PAUSE 때리고
        exoForUrlPlay.removeHandler() // b)handler 없애기
        Log.d(TAG, "onPause: GlbVars 정보: CurrentTrId=${GlbVars.clickedTrId}")

    }
    override fun onDestroy() {
        super.onDestroy()

        Log.d(TAG, "onDestroy: 2nd Frag! // lifecycle.currentState=${lifecycle.currentState}") //DESTROYED 로 뜬다.

        //mpClassInstance.releaseExoPlayer() //? 여기 아니면 AlarmsListActivity 에다가?
    }
// ===================================== My Functions ==== >

    //MiniPlayer Play/Pause btn UI Update
        // Show Pause Btn
        private fun showMiniPlayerPauseBtn() {
            imgbtn_Play.visibility = View.GONE
            imgbtn_Pause.visibility = View.VISIBLE
        }
        // Show Play btn
        private fun showMiniPlayerPlayBtn() {
            imgbtn_Play.visibility = View.VISIBLE
            imgbtn_Pause.visibility = View.GONE
        }
        // Pause 상태에서 ▶  클릭했을 때
        private fun onMiniPlayerPlayClicked()  {
            if(exoForUrlPlay.currentPlayStatus == StatusMp.PAUSED) { // replace as if(jjMainVModel.getMpStatusLiveData == ... )
                exoForUrlPlay.continueMusic()
                showMiniPlayerPauseBtn()
                }
            }
        //  Play 상태에서 ⏸ 클릭 했을 때 -> 음악 Pause 해야함.
        private fun onMiniPlayerPauseClicked() {
            if(exoForUrlPlay.currentPlayStatus == StatusMp.PLAY) { // replace as if(jjMainVModel.getMpStatusLiveData == ... )
                exoForUrlPlay.pauseMusic()
                showMiniPlayerPlayBtn()
            }
        }

    //위에 onCreatedView 에서 observe 하고 있는 LiveData 가 갱신되었을때 다음을 실행
    // 여기서 우리가 받는 view 는 다음 둘중 하나:  rl_Including_tv1_2.setOnClickListener(this) OR! cl_entire_purchase.setOnClickListener(this)
    // Takes in 'Click Events' and a)Update Mini Player b)Trigger MediaPlayer

    private fun updateMiniPlayerUiOnClick(rtObj: RtInTheCloud) {
        Log.d(TAG, "updateMiniPlayerUiOnClick: called. .. rtObj = $rtObj")

        if(rtObj.id < 0) { // 만약 id 가 0 미만 (즉 깡통) 그냥 return..
            Log.d(TAG, "updateMiniPlayerUiOnClick: [FLOW] 최초 오픈시 Default 값 예상. Invalid Rt Obj. Return")
            return
        }
        // 추후 다른 Frag 갔다 들어왔을 때 화면에 재생시키기 위해. 아래 currentThumbNail 에 임시저장.
    //Sliding Panel - Upper UI
        // 글자 크기 고려해서 공백 추가 (흐르는 효과 Marquee FX 위해)
        var spaceFifteen="               " // 15칸
        var spaceTwenty="                    " // 20칸
        var spaceSixty="                                                           " //60칸
        tv_upperUi_title.text = spaceFifteen+ rtObj.title // miniPlayer(=Upper Ui) 의 Ringtone Title 변경 [제목 앞에 15칸 공백 더하기-흐르는 효과 위해]
        if(rtObj.title.length <6) {tv_upperUi_title.append(spaceSixty) } // [제목이 너무 짧으면 6글자 이하] -> [뒤에 공백 50칸 추가] // todo: null safety check?
        else {tv_upperUi_title.append(spaceTwenty) // [뒤에 20칸 공백 추가] 흐르는 text 위해서. -> 좀 더 좋은 공백 채우는 방법이 있을지 고민..
        }

    //Sliding Panel -  Lower UI
        tv_lowerUi_about.text = rtObj.description // Description 채워주기
        val badgeStrList = rtObj.bdgStrArray// Badge Sort
        showOrHideBadgesOnMiniPlayer(badgeStrList) // Badge 켜고끄기- MiniPlayer 에 반영
        //1) Rt 가격 표시 + Download (Purchase) 버튼 onClickListener 설정 (Purchase 상태면 (v) 활성화)
        btn_buyThis.text = rtObj.itemPrice
        when(rtObj.purchaseBool) {
            true -> {
                btn_buyThis.visibility = View.GONE
                purchased_check_icon.visibility= View.VISIBLE
            }
            false -> {
                btn_buyThis.visibility = View.VISIBLE
                purchased_check_icon.visibility= View.GONE
            }
        }

        btn_buyThis.setOnClickListener {
            onMiniPlayerPauseClicked() // 음악 재생 멈춤.
            jjMainVModel.onTrackClicked(rtObj, isPurchaseClicked = true, requireActivity())
        }

        //2) Mini Player 사진 변경 (RcView 에 있는 사진 그대로 옮기기)
        GlideApp.with(requireContext()).load(rtObj.imageURL).centerCrop().error(R.drawable.errordisplay)
            .placeholder(R.drawable.placeholder).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?,model: Any?,target: Target<Drawable>?,isFirstResource: Boolean): Boolean {
                    Log.d(TAG, "onLoadFailed: failed ... ")
                    return false
                }

                override fun onResourceReady(resource: Drawable?,model: Any?,target: Target<Drawable>?,dataSource: DataSource?,
                                             isFirstResource: Boolean): Boolean {
                    iv_upperUi_thumbNail.setImageDrawable(resource) // Upper Ui -> 이거 삭제했음.
                    iv_lowerUi_bigThumbnail.setImageDrawable(resource) // Lower ui.
                    //
                    return false
                }
            }).into(iv_upperUi_thumbNail)

        // 최초 SlidingPanel 이 HIDDEN  일때만 열어주기. 이미 EXPAND 상태로 보고 있다면 Panel 은 그냥 둠
        if (slidingUpPanelLayout.panelState == SlidingUpPanelLayout.PanelState.HIDDEN) {
            slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED // Show Panel! 아리러니하게도 .COLLAPSED 가 (위만) 보이는 상태임!
        }
        // 클릭시 구매창 이동..
    }

    //Chip Related#1 (Listener 등록)
    private fun setChipListener(v: View) { //setChip Listener.
        
        chipGroup = v.findViewById(R.id.id_chipGroup)
        for (i in 0 until chipGroup.childCount) {
            val chip: Chip = chipGroup.getChildAt(i) as Chip

            // Chip 이 체크/해제될때마다
            chip.setOnCheckedChangeListener { _, isChecked ->
                //a) 아이콘 hide/show
                when (isChecked) {
                    true -> {chip.isChipIconVisible = false}
                    false -> {chip.isChipIconVisible = true}
                }
                //b) 선택된 칩 배경으로 tagsList 를 받아서 -> rcv 를 Refresh!
                filterListThenRefreshRcv(getTagsList())
            }
        }
    }
    // Chip Related #2 (Create TagsList) - 체크된 Chip 을 바탕으로 tagsList 를 만듬
    private fun getTagsList(): List<String> {

        val tagsList = mutableListOf<String>()

        val intenseTag ="INT"
        val gentleTag="GEN"
        val natureTag ="NAT"
        val locationsTag ="LOC"
        val popularTag ="POP"
        val miscTag ="MIS"

        chipGroup.checkedChipIds.forEach {

            when(it) {
                R.id.id_chip1_intense -> tagsList.add(intenseTag)
                R.id.id_chip2_gentle -> tagsList.add(gentleTag)
                R.id.id_chip3_nature -> tagsList.add(natureTag)
                R.id.id_chip4_location -> tagsList.add(locationsTag)
                R.id.id_chip5_popular -> tagsList.add(popularTag)
                R.id.id_chip6_misc -> tagsList.add(miscTag)
            }
        }
        Log.d(TAG, "getTagsList: tagsList= $tagsList")
        return tagsList.toList()
    }
    // Chip Related #3 (Chip 선택 및 해제시 이쪽으로 옴 -> RefreshRcv)
    private fun filterListThenRefreshRcv(tagsList: List<String>) {
        //A-1) Chip 이 하나 이상 체크되었다.
        if(tagsList.isNotEmpty()) {
            myIsChipChecked= true // VModel 에서 이 값을 근거로 움직임.
            val filteredRtList = jjMainVModel.getUnfilteredList().filter { rtObject -> rtObject.bdgStrArray.containsAll(tagsList) }

            if(filteredRtList == jjMainVModel.rtInTheCloudList.value){ // 이미 FilteredList 로 refreshRcV 된 경우 (Ex. ListFrag 갔다와서 VModel 이 먼저 자동 복원)
                Log.d(TAG, "filterListThenRefreshRcv: 이미 FilteredList 로 refreshRcV 된 경우")
                return
            } else {
                rcvAdapterInstance.refreshRecyclerView(filteredRtList)
            }
        //A-2) Chip 이 선택되었다가 다 해제되었을 때
        }else if(tagsList.isEmpty()) {
            myIsChipChecked= false
            jjMainVModel.showUnfilteredList() // -> JjVModel [라이브데이터 원래 list 로 갱신] -> SecondFrag -> rcvRefresh!
        }
    }
    // Chip Related #4
    private fun getFilteredList(unfilteredRtList: List<RtInTheCloud>, tagsList: List<String>): List<RtInTheCloud> {
        if(tagsList.isEmpty()) return ArrayList() // tagsList 가 깡통 (즉 Chip 선택된게 X) -> 깡통 List 반환
        val filteredRtList = unfilteredRtList.filter { rtObject -> rtObject.bdgStrArray.containsAll(tagsList) }
        return filteredRtList
    }

    // MiniPlayer Lower Part - Chip(badge) 을 sort 하여 보여줄건 보여주고 가릴건 가리기.
    private fun showOrHideBadgesOnMiniPlayer(badgeStrList: List<String>?) {
        // 일단 다 gone 으로 꺼주고 시작 (안 그러면 RtPicker 갔다왔을 떄 기존에 켜진놈이 안 꺼지니께..)
        // 혹시 이렇게 꺼지는게 눈에 안 좋아보이면 위에서 RtPicker Activity 갈때 꺼줘도 됨..
        mPlayer_bdg1_intense.visibility = View.GONE
        mPlayer_bdg2_gentle.visibility = View.GONE
        mPlayer_bdg3_nature.visibility = View.GONE
        mPlayer_bdg4_location.visibility = View.GONE
        mPlayer_bdg5_popular.visibility = View.GONE
        mPlayer_bdg6_misc.visibility = View.GONE
        // String List 에서 이제 글자따라 다시 visible 시켜주기!
        Log.d(TAG, "showOrHideBadges: badgeStrList=$badgeStrList")
        if (badgeStrList != null) {
            for(i in badgeStrList.indices) {
                when(badgeStrList[i]) {
                    "INT" -> mPlayer_bdg1_intense.visibility = View.VISIBLE
                    "GEN" -> mPlayer_bdg2_gentle.visibility = View.VISIBLE
                    "NAT" -> mPlayer_bdg3_nature.visibility = View.VISIBLE
                    "LOC" -> mPlayer_bdg4_location.visibility = View.VISIBLE
                    "POP" -> mPlayer_bdg5_popular.visibility = View.VISIBLE
                    "MIS" -> mPlayer_bdg6_misc.visibility = View.VISIBLE
                }
            }
        }
        Log.d(TAG, "showOrHideBadges: done..")
    }



    private fun mySmoothScroll() {
        layoutManager.scrollToPositionWithOffset(GlbVars.clickedTrId - 1, 60)
    }

    private fun registerSwipeRefreshListener() {
        swipeRefreshLayout.setOnRefreshListener { //setOnRefreshListener 는  function! (SwipeRefreshLayout.OnRefreshListener 인터페이스를 받는) .. 결국 아래는 이름없는 function..?
            Log.d(TAG, "+++++++++++++ inside setOnRefreshListener+++++++++")
            swipeRefreshLayout.isRefreshing = true
            jjMainVModel.refreshFbAndIAPInfo()

//            // Chip check 여부에 따라
//            if (myIsChipChecked) { //하나라도 체크되어있으면
//                // Do nothing. Just stop the spinner
//                if (swipeRefreshLayout.isRefreshing) {
//                    Log.d(TAG, "Chip checked. Doing nothing but stopping the spinner.")
//                    swipeRefreshLayout.isRefreshing = false
//                }
//            } else if (!myIsChipChecked) {
//                jjMainVModel.refreshAndUpdateLiveData()
//            }
        }
    }
    //SeekBarListener (유저가 seekbar 를 만졌을 때 반응하는것.)
    private fun seekbarListenerSetUp(){
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean)
            {
                exoForUrlPlay.removeHandler() // 새로 추가함.
                var progressLong = progress.toLong()
                if(fromUser) exoForUrlPlay.onSeekBarTouchedYo(progressLong)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    private fun setUpLateInitUis(v: View) {
        Log.d(TAG, "setUpLateInitUis: called")
    //Lottie & LoadingCircle
        frameLayoutForCircle = v.findViewById(R.id.fl_loadingCircle)
        centerLoadingCircle = v.findViewById(R.id.loadingCircle_itself)
        frameLayoutForCircle.visibility = View.GONE// 일단 LoadingCircle 안보이게 하기.

        lottieAnimationView = v.findViewById(R.id.id_lottie_secondFrag)
        lottieAnimHandler = LottieAnimHandler(requireActivity(), lottieAnimationView)
        //일단 lottieAnim - Loading 애니메이션 틀어주기
        lottieAnimHandler.animController(LottieENUM.INIT_LOADING)

    //Swipe Refresh Layout Related
        swipeRefreshLayout = v.findViewById(R.id.id_swipeRefreshLayout)


    // SlidingUpPanel
        slidingUpPanelLayout = v.findViewById(R.id.id_slidingUpPanel)

        //a) Sliding Panel: Upper Ui

        upperUiHolder = v.findViewById(R.id.id_upperUi_ll)   // 추후 이 부분이 fade out
        tv_upperUi_title = v.findViewById<TextView>(R.id.id_upperUi_tv_title)
        iv_upperUi_thumbNail = v.findViewById<ImageView>(R.id.id_upperUi_iv_coverImage)
        iv_upperUi_ClickArrow = v.findViewById<ImageView>(R.id.id_upperUi_iv_clickarrowUp)
        cl_upperUi_entireWindow = v.findViewById<ConstraintLayout>(R.id.id_upperUi_ConsLayout)

        // mini player 에 장착된 play/pause 버튼 찾기 및 listener 등록
            imgbtn_Play = v.findViewById(R.id.id_imgbtn_upperUi_play)
            imgbtn_Pause = v.findViewById(R.id.id_imgbtn_upperUi_pause)

                imgbtn_Play.setOnClickListener {
                    onMiniPlayerPlayClicked()
                }
                imgbtn_Pause.setOnClickListener {
                    onMiniPlayerPauseClicked()
                }
        //Seekbar Related
            seekBar = v.findViewById(R.id.id_upperUi_Seekbar)
            seekbarListenerSetUp()

        //b-1) lower Ui 기본
        constLayout_entire = v.findViewById<ConstraintLayout>(R.id.id_lowerUI_entireConsLayout)
        iv_lowerUi_bigThumbnail = v.findViewById<ImageView>(R.id.id_lowerUi_iv_bigThumbnail)
        //iv_lowerUi_bigThumbnail.visibility = View.INVISIBLE // Frag 전환시 placeHolder (빨갱이사진) 보이는 것 방지 위해.
        tv_lowerUi_about = v.findViewById<TextView>(R.id.id_lowerUi_tv_Description)
        //b-2) Lower Ui 가격/Purchase Button
        //tv_price = v.findViewById<TextView>(R.id.tv_price_btm_player)
        btn_buyThis = v.findViewById(R.id.btn_buyThis)

        purchased_check_icon = v.findViewById(R.id.iv_purchased_check_icon)
        //b-3) lower ui Badge
        // Badge 관련
        mPlayer_bdg1_intense = v.findViewById(R.id.mPlayer_badge1_Intense)
        mPlayer_bdg2_gentle = v.findViewById(R.id.mPlayer_badge2_Gentle)
        mPlayer_bdg3_nature = v.findViewById(R.id.mPlayer_badge3_Nature)
        mPlayer_bdg4_location = v.findViewById(R.id.mPlayer_badge_4_Location)
        mPlayer_bdg5_popular = v.findViewById(R.id.mPlayer_badge_5_Popular)
        mPlayer_bdg6_misc = v.findViewById(R.id.mPlayer_badge_6_Misc)

        //Activity 에서 받은 BottomNavView (추후 SlidingPanel 이 EXPAND/COLLAPSE 될 때 VISIBLE/INVISIBLE 해준다.)
        btmNavViewFromActivity = requireActivity().findViewById<BottomNavigationView>(R.id.id_bottomNavigationView) // todo: check
        btmAppBarFromActivity = requireActivity().findViewById(R.id.bottomAppBar2)

        //btmAppBarFromActivity.setBackgroundColor(ContextCompat.getColor(requireContext(),R.color.blue_var_1)) // 테스트. Spotify 처럼 배경색 일치.


    // RcV 를 감싸는 FrameLayout [RcV 마지막 칸이 짤리는 문제가 있어서 PaddingBottom 으로 해결중. 최초 SecondFrag 열었을때는 ]
        flRcView =v.findViewById(R.id.frameLayout_RcView)
        Log.d(TAG, "setUpLateInitUis: PanelState=${slidingUpPanelLayout.panelState}")



    //Title Scroll horizontally. 흐르는 텍스트
        tv_upperUi_title.apply {
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.MARQUEE
            isSelected = true
            //text ="Song Title                                           "
            // text 제목이 일정 수준 이하면 여백을 추가, 추후 title.length < xx => 정확한 카운트 알고리즘.
        }

        setUpSlidingPanel()
    }

    // Sliding Panel
    private fun collapseSlidingPanel() {
        Log.d(TAG, "collapseSlidingPanel: called")
        slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        iv_upperUi_ClickArrow.setImageResource(R.drawable.arrow_up_white)// ↑ arrow 전환 visibility }
        slidingUpPanelLayout.isOverlayed = false //
    }
    private fun expandSlidingPanel() {
        Log.d(TAG, "expandSlidingPanel: called..")
        slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.EXPANDED
        //모퉁이 흰색 없애주고 & 불투명으로
          slidingUpPanelLayout.isOverlayed =true // 모퉁이 edge 없애기 위해. Default 는 안 겹치게 false 값.
          //upperUiHolder.alpha = 0.5f // +0.3 은 살짝~ 보이게끔
          //↓ arrow 전환 visibility
          iv_upperUi_ClickArrow.setImageResource(R.drawable.arrow_down_white)
    }

    private fun setUpSlidingPanel() {
        Log.d(TAG,"setUpSlidingPanel: slidingUpPanelLayout: 1)PanelState=${slidingUpPanelLayout.panelState}, 2)isActivated=${slidingUpPanelLayout.isActivated}")
        slidingUpPanelLayout.setDragView(cl_upperUi_entireWindow) //setDragView = 펼치는 Drag 가능 영역 지정


        // A. 기존에 클릭 후 다른 Frag 갔다 돌아온 경우. (Panel 은 Collapsed 아니면 Expanded 상태 유지중임.)
        if (shouldPanelBeVisible) {
            Log.d(TAG, "setUpSlidingPanel: HEY isInitialPanelSetup=$shouldPanelBeVisible")
            btmAppBarFromActivity.setBackgroundResource(R.drawable.btm_nav_bg_rectangle_corner)
            //collapseSlidingPanel()

            // 만약 확장된 상태였다면 초기화가 안되어있어서 모퉁이 허옇고 & arrow(↑)가 위로 가있음. 아래에서 해결.
            /*if (slidingUpPanelLayout.panelState == SlidingUpPanelLayout.PanelState.EXPANDED) { // EXPANDED=0
                Log.d(TAG, "setUpSlidingPanel: called.")
                //collapseSlidingPanel() // onPause() 에서도 해주는데 안 먹히네?
            }*/
        }
        // B. 최초 로딩- 기존 클릭이 없어서 Panel 이 접혀있지도(COLLAPSED) 확장되지도(EXPANDED) 않은 경우에는 감춰놓기.
        else if (!shouldPanelBeVisible) {
            Log.d(TAG, "setUpSlidingPanel: shouldPanelBeVisible (x)")
            slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.HIDDEN // 3. 일단 클릭전에는 감춰놓기!
        }
        //slidingUpPanelLayout.anchorPoint = 0.6f //화면의 60% 만 올라오게.  그러나 2nd child 의 height 을 match_parent -> 300dp 로 설정해서 이걸 쓸 필요가 없어짐!
        //slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.ANCHORED // 위치를 60%로 초기 시작
        slidingUpPanelLayout.addPanelSlideListener(object :SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                //Log.d(TAG, "onPanelSlide: Panel State=${slidingUpPanelLayout.panelState}")
                shouldPanelBeVisible = true // 이제 Panel 이 열렸으니깐. todo: 이거 bool 값에 의존하는게 괜찮을지..
                //upperUiHolder.alpha = 1 - slideOffset + 0.5f // +0.5 은 어느정도 보이게끔 //

                // 트랙 클릭-> 미니플레이어가 등장! (그 이전에는 offset = -xxx 값임.)
                //Log.d(TAG, "onPanelSlide: slideOffset= $slideOffset, rcvAdapterInstance.itemCount=${rcvAdapterInstance.itemCount}")
                val entireListCount = rcvAdapterInstance.itemCount

                if (slideOffset == 0.0f && GlbVars.clickedTrId == entireListCount) { //마지막 트랙 클릭.
                    rcView.post { // 메인 ui 스레드에서는 다른 업무 처리로 바뻐서 다른 thread (워커스레드?) 를 만들어줌.
                        rcView.smoothScrollBy(0, 300) //RcV 전체를 약300dp 위로 밀어줌.
                        // todo: 추후 rcView 사이즈 변경될 때 고려 ->정확한 calculation 필요  https://greedy0110.tistory.com/41
                        Log.d(TAG, "myOnItemClick: 살짝 슬라이드! 마지막 트랙 보이게!")
                        //Log.d(TAG, "onPanelSlide: entirelistcount: $entireListCount")
                    }
                }

                // 완전히 펼쳐질 때
                if (!slidingUpPanelLayout.isOverlayed && slideOffset > 0.2f) { //안겹치게 설정된 상태에서 panel 이 열리는 중 (20%만 열리면 바로 모퉁이 감추기!)
                    //Log.d(TAG, "onPanelSlide: Hiding 모퉁이! yo! ")
                    slidingUpPanelLayout.isOverlayed = true // 모퉁이 edge 없애기 위해. Default 는 안 겹치게 false 값.
                }

            }

            @SuppressLint("ClickableViewAccessibility") // 아래 constLayout_entire.setxx... 이거 장애인 warning 없애기
            override fun onPanelStateChanged(panel: View?,previousState: SlidingUpPanelLayout.PanelState?,newState: SlidingUpPanelLayout.PanelState?) {
                //Log.d(TAG, "onPanelStateChanged: previousState= $previousState -> newState=$newState, isActivated= ${slidingUpPanelLayout.isActivated}")
                Log.d(TAG, "onPanelStateChanged: btmNavView.height=${btmNavViewFromActivity.height}, umanoHeight?인듯=${panel!!.height}")

            //1) 접힌상태-> 완전히 열리는 상태로 전환중(COLLAPSED -> DRAGGING) // 추후 DRAGGING -> EXPANDED 로 진행 (대략 0.4 초 소요)
            /*if(previousState== SlidingUpPanelLayout.PanelState.COLLAPSED && newState == SlidingUpPanelLayout.PanelState.DRAGGING) {
                btmAppBarFromActivity.animate().translationY(btmAppBarFromActivity.height.toFloat()).alpha(0.0f)
            //btmNavViewFromActivity.animate().translationY(btmNavViewFromActivity.height.toFloat()).alpha(0.0f) // 어차피 BtmAppBar 가 BtmNavView 의 Parent 여서 이것까지 해줄 필요 없음.
            }*/

            //1) Bottom App Bar 코너 Round -> Rectangle 로. (최초 트랙 클릭 으로 Hidden -> Dragging 상태) 이후 Collapsed 가 되는것임
            if(previousState== SlidingUpPanelLayout.PanelState.HIDDEN && newState == SlidingUpPanelLayout.PanelState.DRAGGING) {
                btmAppBarFromActivity.setBackgroundResource(R.drawable.btm_nav_bg_rectangle_corner)

            }

            //2) Drag 해서 -> 완전히 열렸을 때(Dragging -> Expanded)
            if(previousState== SlidingUpPanelLayout.PanelState.DRAGGING && newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                btmAppBarFromActivity.animate().translationY(btmAppBarFromActivity.height.toFloat()).alpha(0.0f)
                //btmNavViewFromActivity.animate().translationY(btmNavViewFromActivity.height.toFloat()).alpha(0.0f) // 어차피 BtmAppBar 가 BtmNavView 의 Parent 여서 이것까지 해줄 필요 없음.
            }

            //3) 완전히 열린 상태 -> 접히는 상태로 전환 // // DRAGGING -> EXPANDED -> DRAGGING 으로 진행 (대략 0.4 초 소요)
            else if(previousState== SlidingUpPanelLayout.PanelState.EXPANDED && newState == SlidingUpPanelLayout.PanelState.DRAGGING){
                btmAppBarFromActivity.animate().translationY(0F).alpha(1.0f) // '0F 까지!' View 를 올리는 것.
            //  btmNavViewFromActivity.animate().translationY(0F).alpha(1.0f) // '0F 까지!' View 를 올리는 것.

            }


            //3) 완전 열린상태(EXPAND) or MiniPlayer 만 보여주는 상태 (COLLAPSED) 설정 ( RcView 짤리지 않게 paddingBottom 까지 설정)
                when (newState) {
                    SlidingUpPanelLayout.PanelState.EXPANDED -> {
                        Log.d(TAG, "onPanelStateChanged: Sliding Panel= EXPANDED")
                        iv_upperUi_ClickArrow.setImageResource(R.drawable.arrow_down_white)// ↓ arrow 전환 visibility }

                        // 계속 click 이 투과되는 문제(뒤에 recyclerView 의 버튼 클릭을 함)를 다음과같이 해결. 위에 나온 lowerUi 의 constraint layout 에 touch를 허용.
                        constLayout_entire.setOnTouchListener { _, _ -> true }
                        flRcView.setPadding(0,0,0,0)
                    }
                    SlidingUpPanelLayout.PanelState.COLLAPSED -> {
                        Log.d(TAG, "onPanelStateChanged: Sliding Panel= COLLAPSED")
                        iv_upperUi_ClickArrow.setImageResource(R.drawable.arrow_up_white)// ↑ arrow 전환 visibility }
                        slidingUpPanelLayout.isOverlayed = false // 이렇게해야 rcView contents 와 안겹침 = (마지막 칸)이 자동으로 panel 위로 올라가서 보임.
                        flRcView.setPadding(0,0,0,0)

                    }
                    SlidingUpPanelLayout.PanelState.HIDDEN -> {
                        Log.d(TAG, "onPanelStateChanged: Sliding Panel = HIDDEN")
                        flRcView.setPadding(0,0,0,140)
                    }
                }
            }
        })



    }
    private fun snackBarDeliverer(view: View, msg: String, isShort: Boolean) {
        if(activity!=null && isAdded) { // activity 가 존재하며, 현재 Fragment 가 attached 되있으면 Snackbar 를 표시.
            Log.d(TAG, "snackBarMessenger: Show Snackbar. Fragment isAdded=$isAdded, Message=[$msg], Activity=$activity")
            if(isShort) {
                Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
            }else {
                Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show()
            }
        } else {
            Log.d(TAG, "snackBarDeliverer: Unable to Deliver Snackbar message!!")
        }

    }
//RecyclerView Click Communication
    val secondFragListener = object : RcCommInterface {
        override fun onRcvClick(rtObj: RtInTheCloud, isPurchaseClicked: Boolean) {
            jjMainVModel.onTrackClicked(rtObj, isPurchaseClicked, requireActivity())

        }
    }




}
