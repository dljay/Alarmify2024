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
import com.theglendales.alarm.jjfirebaserepo.FirebaseRepoClass
import com.theglendales.alarm.jjmvvm.JjMpViewModel
import com.theglendales.alarm.jjmvvm.JjRecyclerViewModel
import com.theglendales.alarm.jjmvvm.JjViewModel
import com.theglendales.alarm.jjmvvm.data.ViewAndTrIdClass
import com.theglendales.alarm.jjmvvm.helper.VHolderUiHandler
import com.theglendales.alarm.jjmvvm.mediaplayer.MyCacher
import com.theglendales.alarm.jjmvvm.mediaplayer.MyMediaPlayer
import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp

//Coroutines

/**
 * A simple [Fragment] subclass.
 * Use the [SecondFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

private const val TAG = "SecondFragment"

class SecondFragment : androidx.fragment.app.Fragment() {


//    var iapInstance = MyIAPHelper(this,null, ArrayList())

    //SharedPreference 저장 관련 (Koin  으로 대체!) ==> 일단 사용 안함.
    //val mySharedPrefManager: MySharedPrefManager by globalInject()
    //private val playInfo: PlayInfoContainer = PlayInfoContainer(-10,-10,-10, StatusMp.IDLE)

    //RcView Related
    lateinit var rcvAdapterInstance: RcViewAdapter
    lateinit var rcView: RecyclerView
    lateinit var layoutManager: LinearLayoutManager

    //Swipe Refresh
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    //Chip related
    lateinit var chipGroup: ChipGroup
    var myIsChipChecked = false

    private val myNetworkCheckerInstance: MyNetWorkChecker by globalInject() // Koin 으로 대체!! 성공!
    // VumeterHandler
    private val VHolderUiHandler: VHolderUiHandler by globalInject() // Koin Inject

    //Lottie Animation(Loading & Internet Error)
    lateinit var lottieAnimationView: LottieAnimationView

    //Media Player & MiniPlayer Related
    lateinit var mpClassInstance: MyMediaPlayer


    //Sliding Panel Related
    var shouldPanelBeVisible = false
    lateinit var slidingUpPanelLayout: SlidingUpPanelLayout    //findViewById(R.id.id_slidingUpPanel)  }

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

    // 다른 frag 나 밖에 갔다왔을때 -> 기존 rcV 의 클릭해놓은 트랙을 기억하기 위해.
    var isEverythingReady = false // a) 최초 rcV 열어서 모든게 준비되면 =true, b) 다른 frag 로 나갔다왔을 때 reconstructXX() 다 끝나면 true.

    //Firebase 관련
    private val firebaseRepoInstance: FirebaseRepoClass by globalInject()
    lateinit var jjFirebaseVModel: JjViewModel
    var fullRtClassList: MutableList<RingtoneClass> = ArrayList()

    // Basic overridden functions -- >
    override fun onCreate(savedInstanceState: Bundle?) {
        //Log.d(TAG, "onCreate: jj-called..")
        super.onCreate(savedInstanceState)

    }

//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//        Log.d(TAG, "onActivityCreated: jj-2ndFrag Activity!!Created!!")
//    }

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View {
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
        layoutManager = LinearLayoutManager(context)
        rcView.layoutManager = layoutManager
    //  LIVEDATA ->
        //1) ViewModel 2종 생성(RcvVModel/MediaPlayerVModel)
            //1-A)  *** JjRcvViewModel 이것은 오롯이 RcView 에서 받은 Data-> MiniPlayer(BtmSlide) Ui 업뎃에 사용됨! ***
            val jjRcvViewModel = ViewModelProvider(requireActivity()).get(JjRecyclerViewModel::class.java)
            //1-B) jjMpViewModel 생성
            val jjMpViewModel = ViewModelProvider(requireActivity()).get(JjMpViewModel::class.java)

        //2) LiveData Observe
            //2-A) rcV 에서 클릭-> rcvViewModel -> 여기로 전달.
            jjRcvViewModel.selectedRow.observe(viewLifecycleOwner, { viewAndTrIdClassInstance ->
                Log.d(TAG,"onViewCreated: !!! 'RcvViewModel' 옵저버!! 트랙ID= ${viewAndTrIdClassInstance.trId}")
                if(isEverythingReady) {
                    myOnLiveDataFromRCV(viewAndTrIdClassInstance)

                }
            //**SHARED PREF 저장용 **
                //playInfo.trackID = viewAndTrIdClassInstance.trId
            })
            //2-B) MediaPlayer 에서의 Play 상태(loading/play/pause) 업뎃을 observe
            jjMpViewModel.mpStatus.observe(viewLifecycleOwner, { StatusEnum ->
                Log.d(TAG, "onViewCreated: !!! 'MpViewModel' 옵저버! Current Music Play Status: $StatusEnum")
                // a) MiniPlayer Play() Pause UI 업데이트 (현재 SecondFragment.kt 에서 해결)
                when(StatusEnum) {
                    StatusMp.PLAY -> {showMiniPlayerPauseBtn()} // 최초의 ▶,⏸ 아이콘 변경을 위하여 사용. 그후에는 해당버튼 Click -> showMiniPlayerPause/Play 실행됨.
                    StatusMp.BUFFERING -> {showMiniPlayerPlayBtn()}
                    StatusMp.ERROR -> {showMiniPlayerPlayBtn()}
                    StatusMp.PAUSED -> {showMiniPlayerPlayBtn()}
                    }
                // b) VuMeter/Loading Circle 등 UI 컨트롤
                VHolderUiHandler.LcVmIvController(StatusEnum)
                // c) **SHARED PREF 저장용 **
                //playInfo.songStatusMp = StatusEnum
                })

            //2-C) seekbar 업뎃을 위한 현재 곡의 길이(.duration) observe. (MyMediaPlayer -> JjMpViewModel-> 여기로)
            jjMpViewModel.songDuration.observe(viewLifecycleOwner, { dur ->
                Log.d(TAG, "onViewCreated: duration received = ${dur.toInt()}")
                seekBar.max = dur.toInt()
                // c) **GlbVar 저장용 **
                //GlbVars.seekBarMax = dur.toInt()
            })
            //2-D) seekbar 업뎃을 위한 현재 곡의 길이(.duration) observe. (MyMediaPlayer -> JjMpViewModel-> 여기로)
            jjMpViewModel.currentPosition.observe(viewLifecycleOwner, { playbackPos ->
                //Log.d(TAG, "onViewCreated: playback Pos=${playbackPos.toInt()} ")
                    seekBar.progress = playbackPos.toInt() +200
                // c) **GlbVars 저장용 ** 현재 재생중인 seekbar 위치
                //GlbVars.seekbarProgress = playbackPos.toInt() +200
                //GlbVars.playbackPos = playbackPos
                })
        //3) Firebase ViewModel Initialize
        jjFirebaseVModel = ViewModelProvider(requireActivity()).get(JjViewModel::class.java)


        //4) RcvAdapter & MediaPlayer & MiniPlayer Instance 생성.
            mpClassInstance = activity?.let {MyMediaPlayer(it, jjMpViewModel)}!!
            rcvAdapterInstance = activity?.let {RcViewAdapter(ArrayList(),it,jjRcvViewModel,mpClassInstance)}!! // it = activity. 공갈리스트 넣어서 instance 만듬

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

    // MyCacher Init()
        val myCacherInstance = context?.let { MyCacher(it, it.cacheDir, mpClassInstance) }
        if (myCacherInstance != null) {
            myCacherInstance.initCacheVariables()
        }

    }
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: 2nd Frag!")
        // 아래 onPause() 에서 save 한 '기존 재생 정보' 는 observeAndLoadFirebase() 에서 로딩하기로.

    }
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: 2nd Frag!")
        collapseSlidingPanel()
        //1) 현재 음악이 재생중이든 아니든 (재생중이 아니었으면 어차피 pauseMusic() 은 의미가 없음)
            mpClassInstance.pauseMusic() // a)일단 PAUSE 때리고
            mpClassInstance.removeHandler() // b)handler 없애기
        Log.d(TAG, "onPause: GlbVars 정보: CurrentTrId=${GlbVars.clickedTrId}")

        //2) 최종적으로 선택해놓은 트랙 아이디

        isEverythingReady = false // 다시 돌아왔을 때 이게 true 여야 jjRcViewModel 이 반응함.

        //3) 다시 돌아왔을 때 Slide 의 upperUi 에서 빨간색 앨범커버가 보였다 다른 앨범으로 교체되는 현상을 막기 위해.
         if(iv_upperUi_thumbNail.visibility == View.VISIBLE) {
             iv_upperUi_thumbNail.visibility == View.INVISIBLE
         }


        //3) 그리고 나서 save current play data to SharedPref using gson.
        //mySharedPrefManager.savePlayInfo(playInfo)

    }
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: 2nd Frag!")
         mpClassInstance.releaseExoPlayer() //? 여기 아니면 AlarmsListActivity 에다가?

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
            if(MyMediaPlayer.currentPlayStatus == StatusMp.PAUSED) {
                mpClassInstance.continueMusic()
                showMiniPlayerPauseBtn()
                }
            }
        //  Play 상태에서 ⏸ 클릭 했을 때 -> 음악 Pause 해야함.
        private fun onMiniPlayerPauseClicked() {
            if(MyMediaPlayer.currentPlayStatus == StatusMp.PLAY) {
                mpClassInstance.pauseMusic()
                showMiniPlayerPlayBtn()
                }
            }

    //위에 onCreatedView 에서 observe 하고 있는 LiveData 가 갱신되었을때 다음을 실행
    // 여기서 우리가 받는 view 는 다음 둘중 하나:  rl_Including_tv1_2.setOnClickListener(this) OR! cl_entire_purchase.setOnClickListener(this)
    // Takes in 'Click Events' and a)Update Mini Player b)Trigger MediaPlayer

    private fun myOnLiveDataFromRCV(viewAndTrId: ViewAndTrIdClass) {


        val ringtoneClassFromtheList = rcvAdapterInstance.getDataFromMap(viewAndTrId.trId)
        Log.d(TAG, "myOnLiveDataReceived: called. rtClassFromtheList= $ringtoneClassFromtheList")
        val ivInside_Rc = viewAndTrId.view.findViewById<ImageView>(R.id.id_ivThumbnail) // Recycler View 의 현재 row 에 있는 사진을 variable 로 생성
        // 추후 다른 Frag 갔다 들어왔을 때 화면에 재생시키기 위해. 아래 currentThumbNail 에 임시저장.

        //Sliding Panel - Upper UI
        // 글자 크기 고려해서 공백 추가 (흐르는 효과 Marquee FX 위해)
        var spaceFifteen="               " // 15칸
        var spaceTwenty="                    " // 20칸
        var spaceFifty="                                                 " //50칸 (기존 사용)
        var spaceSixty="                                                           " //60칸
        tv_upperUi_title.text = spaceFifteen+ ringtoneClassFromtheList?.title // miniPlayer(=Upper Ui) 의 Ringtone Title 변경 [제목 앞에 15칸 공백 더하기-흐르는 효과 위해]
        if(ringtoneClassFromtheList?.title!!.length <6) {tv_upperUi_title.append(spaceSixty) } // [제목이 너무 짧으면 6글자 이하] -> [뒤에 공백 50칸 추가] // todo: null safety check?
        else {tv_upperUi_title.append(spaceTwenty) // [뒤에 20칸 공백 추가] 흐르는 text 위해서. -> 좀 더 좋은 공백 채우는 방법이 있을지 고민..
        }

        //Sliding Panel -  Lower UI
        tv_lowerUi_about.text = ringtoneClassFromtheList?.description
        //
        when (viewAndTrId.view.id) {
            //1) RcView > 왼쪽 큰 영역(album/title) 클릭했을때 처리.
            R.id.id_rL_including_title_description -> {

                //1) Mini Player 사진 변경 (RcView 에 있는 사진 그대로 옮기기)
                if (ivInside_Rc != null) { // 사실 RcView 가 제대로 setup 되어있으면 무조건 null 이 아님! RcView 클릭한 부분에 View 가 로딩된 상태 (사진 로딩 상태 x)
                    if(iv_upperUi_thumbNail.visibility==View.INVISIBLE) {
                        iv_upperUi_thumbNail.visibility = View.VISIBLE
                    }
                    iv_upperUi_thumbNail.setImageDrawable(ivInside_Rc.drawable) //RcV 현재 row 에 있는 사진으로 설정
                    iv_lowerUi_bigThumbnail.setImageDrawable(ivInside_Rc.drawable) //RcV 현재 row 에 있는 사진으로 설정
                }

                // 최초 SlidingPanel 이 HIDDEN  일때만 열어주기. 이미 EXPAND 상태로 보고 있다면 Panel 은 그냥 둠
                if (slidingUpPanelLayout.panelState == SlidingUpPanelLayout.PanelState.HIDDEN) {
                    slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED // Show Panel! 아리러니하게도 .COLLAPSED 가 (위만) 보이는 상태임!
                }
            }
            // 2) 우측 FREE, GET THIS 클릭했을 때 처리.
            R.id.id_cl_entire_Purchase -> {
                Log.d(TAG, "myOnItemClick: You probably clicked FREE or GET This")
                Toast.makeText(this.context, "Clicked Purchase Button for ${viewAndTrId.trId}",Toast.LENGTH_SHORT).show()
                // tvGetThis.text = "Clicked!" <-- 이거 에러남. 잘 됐었는데. 희한..
                //                iapInstance.myOnPurchaseClicked(trackId)
            }

        }
    }




    private fun setNetworkAvailabilityListener() {
        //1-b) API 24 이상이면 콜백까지 등록
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            myNetworkCheckerInstance.connectivityManager.let {
                it.registerDefaultNetworkCallback(object :
                    ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        //Connection is gained.
                        Log.d(
                            TAG,
                            "onAvailable: Internet available: OOOOOOOOOOOOOOOOOOOOO "
                        ) //최초 앱 실행시에도 (인터넷이 되니깐) 여기 log 가 작동됨.

                        Handler(Looper.getMainLooper()).post { observeAndLoadFireBase() } // MainThread 에서만 실행해야함. 이거 없으면 크래쉬! (Cannot invoke observe on a backgroudn thread)
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
        for (i in 0 until chipGroup.childCount) {
            val chip: Chip = chipGroup.getChildAt(i) as Chip
            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                createStringListFromChips()
                when (isChecked) {
                    true -> {
                        chip.isChipIconVisible = false

                    }
                    false -> {
                        chip.isChipIconVisible = true
                        backToFullRtList()
                    }
                }
            }
        }
    }
    // Chip Related #2 (Listener Setup & Sending a Request to FbRepoClass.)
    private fun createStringListFromChips() {
        val tagsList = mutableListOf<String>()
        val natureTag ="Nature"
        val cityTag ="City"
        val intenseTag ="Intense"
        val vintageTag ="Vintage"
        val weaponsTag ="Weapons"
        val peacefulTag="Calm"

        chipGroup.checkedChipIds.forEach {

            when(it) {
                R.id.id_chip1_nature -> tagsList.add(natureTag)
                R.id.id_chip2_city -> tagsList.add(cityTag)
                R.id.id_chip3_intense -> tagsList.add(intenseTag)
                R.id.id_chip4_vintage -> tagsList.add(vintageTag)
                R.id.id_chip5_weapons -> tagsList.add(weaponsTag)
                R.id.id_chip6_calm -> tagsList.add(peacefulTag)
            }
        }
        Log.d(TAG, "createStringListFromChips: tagsList= $tagsList")

        if(tagsList.isNotEmpty()) {
            myIsChipChecked= true // pull to refresh  했을 때 이 값을 근거로..
            sendRequestToFbRepo(tagsList)
        }else if(tagsList.isEmpty()) { // 체크 된 chip 이 하나도 없음!
            myIsChipChecked= false
        }
    }
     //위에 Chip 이 선택된 항목(string list)을 여기로 전달.
    private fun sendRequestToFbRepo(tagsList: MutableList<String>) {
         var tagOnlyRtClassList: MutableList<RingtoneClass> = ArrayList()

         firebaseRepoInstance.sortSingleOrMultipleTags(tagsList).addOnCompleteListener {
             if (it.isSuccessful) {

                 tagOnlyRtClassList = it.result!!.toObjects(RingtoneClass::class.java)
                 rcvAdapterInstance.updateRecyclerView(tagOnlyRtClassList)

                 //Log.d(TAG, "sendRequestToFbRepo: Success. Showing items including 'A.N.Y' of the tags: $tagsList")
                 Log.d(TAG, "sendRequestToFbRepo: tagOnlyRtClassList.size=${tagOnlyRtClassList.size}, 내용: $tagOnlyRtClassList")

             } else {
                 Log.d(TAG, "sendRequestToFbRepo: ERROR!!- Exception message: ${it.exception!!.message}")
             }
         }

    }
    private fun backToFullRtList() {
        rcvAdapterInstance.updateRecyclerView(fullRtClassList)
        mySmoothScroll()

    }

    //lottieAnimation Controller = 로딩:0 번, 인터넷에러:1번, 정상:2번(lottie 를 감춰!)
    private fun lottieAnimController(status: Int) {
        when (status) {
            0 -> {
                lottieAnimationView.setAnimation(R.raw.lottie_loading1)
            } //최초 app launch->read.. auto play 기 때문에
            1 -> {
                activity?.runOnUiThread(Runnable
                {
                    Log.d(TAG, "lottieAnimController: NO INTERNET ERROR!!")
                    lottieAnimationView.visibility = LottieAnimationView.VISIBLE
                    lottieAnimationView.setAnimation(R.raw.lottie_error1)
                    Snackbar.make(lottieAnimationView,"Please kindly check your network connection status",Snackbar.LENGTH_LONG).show()
                    //todo: 여기 SnackBar 에서 View 가 불안정할수 있음=>try this? -> Snackbar.make(requireActivity().findViewById(android.R.id.content), "..", Snackbar.LENGTH_LONG).show()

                })
                // 만약 sync(multiple file downloads)/single file download 중였다면 btmSheet 없애기.
                //1) 싱글 다운로드 instance & Multi(obj)
//                MyDownloader.btmShtSingleDNLDInstance.removeSingleDNLDBtmSheet()
//                BtmSht_Sync.removeMultiDNLDBtmSheet()
            }
            2 -> {
                activity?.runOnUiThread(Runnable
                {
                    lottieAnimationView.cancelAnimation()
                    lottieAnimationView.visibility = LottieAnimationView.GONE
                })
            }

        }
    }
//MediaPlayerViewModel 을 Observe

    //Firebase ViewModel 을 Observe
    private fun observeAndLoadFireBase() {
        //1. 인터넷 가능한지 체크
        //인터넷되는지 체크
        val isInternetAvailable: Boolean = myNetworkCheckerInstance.isNetWorkAvailable()
        if (!isInternetAvailable) { // 인터넷 사용 불가!
            Log.d(TAG, "loadFromFireBase: isInternetAvailable= $isInternetAvailable")
            lottieAnimController(1)
            return //더이상 firebase 로딩이고 나발이고 진행 안함!!
        }

        //2. If we have internet connectivity, then call FireStore!
        //val jjViewModel = ViewModelProvider(requireActivity()).get(JjViewModel::class.java)
        //Log.d(TAG, "onViewCreated: jj LIVEDATA- (Before Loading) jjViewModel.liveRtList: ${jjViewModel.liveRtList.value}")
        jjFirebaseVModel.getRtLiveDataObserver().observe(requireActivity(), Observer {
            //Log.d(TAG, "onViewCreated: jj LIVEDATA- (After Loading) jjViewModel.liveRtList: ${jjViewModel.liveRtList.value}")
            it.addOnCompleteListener {
                if (it.isSuccessful) { // Task<QuerySnapshot> is successful 일 때
                    Log.d(TAG, "onViewCreated: <<<<<<<<<loadPostData: successful")


                    // IAP related: Initialize IAP and send instance <- 이게 시간이 젤 오래걸리는듯.
//                    iapInstance = MyIAPHelper(this, rcvAdapterInstance, fullRtClassList) //reInitialize
//                    iapInstance.refreshItemIdsAndMp3UrlMap() // !!!!!!!!!!!!!!여기서 일련의 과정을 거쳐서 rcView 화면 onBindView 까지 해줌!!

                    // SwipeRefresh 멈춰 (aka 빙글빙글 animation 멈춰..)
                    if (swipeRefreshLayout.isRefreshing) {
                        Log.d(TAG, "loadPostData: swipeRefresh.isRefreshing = true")
                        swipeRefreshLayout.isRefreshing = false
                    }
                    // 우선 lottie Loading animation-stop!!
                    lottieAnimController(2) //stop!

                    fullRtClassList = it.result!!.toObjects(RingtoneClass::class.java)
                    // Update Recycler View
                    updateResultOnRcView(fullRtClassList)
                    // Update MediaPlayer.kt
                    mpClassInstance.createMp3UrlMap(fullRtClassList)

                // 아무 트랙도 클릭 안한 상태
                    if(GlbVars.clickedTrId == -1) {
                        isEverythingReady = true // 이제는 rcV 를 클릭하면 그에 대해 대응할 준비가 되어있음.
                    }

                // 다른 frag 갔다가 돌아왔을 때 (or 새로고침) 했을 때- 다음의 reConstructXX() 가 다 완료되면 isEverythingReady = true 가 된다.
                    if (GlbVars.clickedTrId > 0) { // todo: 이중장치 추가? onSavedInstance 등?
                        Log.d(TAG, "observeAndLoadFireBase: GlbVars.clickedTrId= ${GlbVars.clickedTrId}")
                        // 1)만약 기존에 선택해놓은 row 가 있으면 그쪽으로 이동.
                        mySmoothScroll()
                        // 2) Highlight the Track -> 이건 rcView> onBindView 에서 해줌.
                        val prevSelectedVHolder = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]
                        // 3) Fill in the previous selected track info to MINIPlayer!!!
                        reConstructSLPanelTextOnReturn(prevSelectedVHolder, GlbVars.clickedTrId)
                         //4) Update RcV UI! (VuMeter 등)
                        reConstructTrUisOnReturn(GlbVars.clickedTrId)
                    }
                    //  mySharedPrefManager.getPlayInfo() 사용했을 때 (사용 안 함)
//                    val prevPlayInfo = mySharedPrefManager.getPlayInfo()
//                    // A) 재생중인 트랙이 있었음
//                    if(prevPlayInfo.trackID >0) {
//                        // 1)만약 기존에 선택해놓은 row 가 있으면 그쪽으로 이동.
//                        mySmoothScroll()
//                        // 2) Highlight the Track -> 이건 rcView> onBindView 에서 해줌.
//                        val prevSelectedVHolder = RcViewAdapter.viewHolderMap[prevPlayInfo.trackID]
//                        // 3) Fill in the previous selected track info to MINIPlayer!!!
//                        reConstructSLPanelTextOnReturn(prevSelectedVHolder, prevPlayInfo.trackID)
//                        // 4) Update RcV UI! (VuMeter 등)
//                        reConstructTrUisOnReturn(prevPlayInfo)
//                    }
                } else { // 에러났을 때
                    lottieAnimController(1)
                    Toast.makeText(
                        this.context,
                        "Error Loading Data from Firebase. Error: ${it.exception.toString()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        })
    }

    private fun mySmoothScroll() {
        layoutManager.scrollToPositionWithOffset(GlbVars.clickedTrId - 1, 60)
    }

    private fun registerSwipeRefreshListener() {
        swipeRefreshLayout.setOnRefreshListener { //setOnRefreshListener 는  function! (SwipeRefreshLayout.OnRefreshListener 인터페이스를 받는) .. 결국 아래는 이름없는 function..?
            Log.d(TAG, "+++++++++++++ inside setOnRefreshListener+++++++++")
            swipeRefreshLayout.isRefreshing = true

            // Chip check 여부에 따라
            if (myIsChipChecked) { //하나라도 체크되어있으면
                // Do nothing. Just stop the spinner
                if (swipeRefreshLayout.isRefreshing) {
                    Log.d(TAG, "Chip checked. Doing nothing but stopping the spinner.")
                    swipeRefreshLayout.isRefreshing = false

                }
            } else if (!myIsChipChecked) {
                Handler(Looper.getMainLooper()).post { observeAndLoadFireBase() }
            }
        }
    }
    //SeekBarListener (유저가 seekbar 를 만졌을 때 반응하는것.)
    private fun seekbarListenerSetUp(){
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean)
            {
                mpClassInstance.removeHandler() // 새로 추가함.
                var progressLong = progress.toLong()
                if(fromUser) mpClassInstance.onSeekBarTouchedYo(progressLong)

            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    private fun setUpLateInitUis(v: View) {
    //Lottie
        lottieAnimationView = v.findViewById(R.id.id_lottie_secondFrag)

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

        //b) lower Ui
        constLayout_entire = v.findViewById<ConstraintLayout>(R.id.id_lowerUI_entireConsLayout)
        iv_lowerUi_bigThumbnail = v.findViewById<ImageView>(R.id.id_lowerUi_iv_bigThumbnail)
        //iv_lowerUi_bigThumbnail.visibility = View.INVISIBLE // Frag 전환시 placeHolder (빨갱이사진) 보이는 것 방지 위해.
        tv_lowerUi_about = v.findViewById<TextView>(R.id.id_lowerUi_tv_Description)

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
        slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        iv_upperUi_ClickArrow.setImageResource(R.drawable.clickarrow)// ↑ arrow 전환 visibility }
        slidingUpPanelLayout.isOverlayed = false //

    }

// 1)SharedPref 에 저장된 재생중 Tr 정보를 바탕으로 UI 를 재구성하는 반면,
    private fun reConstructTrUisOnReturn(prevTrId: Int) {

        mpClassInstance.prepMusicPlayOnlineSrc(prevTrId, false) // 다른  frag 가는 순간 음악은 pause -> 따라서 다시 돌아와도 자동재생하면 안됨!
        isEverythingReady = true

    }
// 2)SharedPref 에 저장된 재생중 Tr 정보를 바탕으로 SlidingPanel UI 를 재구성.
    private fun reConstructSLPanelTextOnReturn(vHolder: RcViewAdapter.MyViewHolder?, trackId: Int) { // observeAndLoadFireBase() 여기서 불림
        if (vHolder != null) {
            Log.d(TAG, "setSlidingPanelOnReturn: called. vHolder !=null. TrackId= $trackId")


            val ringtoneClassFromtheList = rcvAdapterInstance.getDataFromMap(trackId)
            val ivInside_Rc = vHolder.iv_Thumbnail
            Log.d(TAG,"setSlidingPanelOnReturn: title= ${ringtoneClassFromtheList?.title}, description = ${ringtoneClassFromtheList?.description}")
        //Sliding Panel - Upper UI
            var spaceFifteen="               " // 15칸
            var spaceTwenty="                    " // 20칸
            var spaceFifty="                                                 " //50칸 (기존 사용)
            var spaceSixty="                                                           " //60칸
            tv_upperUi_title.text = spaceFifteen+ ringtoneClassFromtheList?.title // miniPlayer(=Upper Ui) 의 Ringtone Title 변경 [제목 앞에 15칸 공백 더하기-흐르는 효과 위해]
            if(ringtoneClassFromtheList?.title!!.length <6) {tv_upperUi_title.append(spaceSixty) } // [제목이 너무 짧으면 6글자 이하] -> [뒤에 공백 50칸 추가] // todo: null safety check?
            else {tv_upperUi_title.append(spaceTwenty) // [뒤에 20칸 공백 추가] 흐르는 text 위해서. -> 좀 더 좋은 공백 채우는 방법이 있을지 고민..
            }



        //Sliding Panel -  Lower UI
            tv_lowerUi_about.text = ringtoneClassFromtheList?.description
            iv_lowerUi_bigThumbnail.setImageDrawable(ivInside_Rc.drawable)
        //Sliding Panel - Upper UI
            if(iv_upperUi_thumbNail.visibility == View.INVISIBLE) {
                iv_upperUi_thumbNail.visibility = View.VISIBLE
            }
            iv_upperUi_thumbNail.setImageDrawable(ivInside_Rc.drawable)

            setUpSlidingPanel()

        }

    }

    private fun setUpSlidingPanel() {

        Log.d(TAG,"setUpSlidingPanel: slidingUpPanelLayout.isActivated=${slidingUpPanelLayout.isActivated}")
        slidingUpPanelLayout.setDragView(cl_upperUi_entireWindow) //setDragView = 펼치는 Drag 가능 영역 지정

        // A. 기존에 클릭 후 다른 Frag 갔다 돌아온 경우. (Panel 은 Collapsed 아니면 Expanded 상태 유지중임.)
        if (shouldPanelBeVisible) {
            Log.d(TAG, "setUpSlidingPanel: isInitialPanelSetup=$shouldPanelBeVisible")

            // 만약 확장된 상태였다면 초기화가 안되어있어서 모퉁이 허옇고 & arrow(↑)가 위로 가있음. 아래에서 해결.
            if (slidingUpPanelLayout.panelState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                /*//모퉁이 흰색 없애주고 & 불투명으로
                    slidingUpPanelLayout.isOverlayed =true // 모퉁이 edge 없애기 위해. Default 는 안 겹치게 false 값.
                    upperUiHolder.alpha = 0.5f // +0.3 은 살짝~ 보이게끔

                //↓ arrow 전환 visibility
                    iv_upperUi_ClickArrow.setImageResource(R.drawable.clickarrow_down)*/
                // 다 필요없고 그냥 Collapse 시켜버리려할때는 위에 지우고 이걸로 사용.
                collapseSlidingPanel() // onPause() 에서도 해주는데 안 먹히네?

            }
        }
        // B. 최초 로딩- 기존 클릭이 없어서 Panel 이 접혀있지도(COLLAPSED) 확장되지도(EXPANDED) 않은 경우에는 감춰놓기.
        else if (!shouldPanelBeVisible) {
            slidingUpPanelLayout.panelState =
                SlidingUpPanelLayout.PanelState.HIDDEN // 일단 클릭전에는 감춰놓기!
        }


        //slidingUpPanelLayout.anchorPoint = 0.6f //화면의 60% 만 올라오게.  그러나 2nd child 의 height 을 match_parent -> 300dp 로 설정해서 이걸 쓸 필요가 없어짐!
        //slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.ANCHORED // 위치를 60%로 초기 시작
        slidingUpPanelLayout.addPanelSlideListener(object :
            SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                // Panel 이 열리고 닫힐때의 callback
                shouldPanelBeVisible = true // 이제 Panel 이 열렸으니깐. todo: 이거 bool 값에 의존하는게 괜찮을지..

                upperUiHolder.alpha =
                    1 - slideOffset + 0.5f // +0.5 은 어느정도 보이게끔 // todo: 나중에는 그냥 invisible 하는게 더 좋을수도. 너무 주렁주렁

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
                    slidingUpPanelLayout.isOverlayed =
                        true // 모퉁이 edge 없애기 위해. Default 는 안 겹치게 false 값.
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
                        iv_upperUi_ClickArrow.setImageResource(R.drawable.clickarrow_down)// ↓ arrow 전환 visibility }

                        // 계속 click 이 투과되는 문제(뒤에 recyclerView 의 버튼 클릭을 함)를 다음과같이 해결. 위에 나온 lowerUi 의 constraint layout 에 touch를 허용.
                        constLayout_entire.setOnTouchListener { _, _ -> true }

                    }
                    SlidingUpPanelLayout.PanelState.COLLAPSED -> {
                        //Log.d(TAG, "onPanelStateChanged: Sliding Panel Collapsed")
                        iv_upperUi_ClickArrow.setImageResource(R.drawable.clickarrow)// ↑ arrow 전환 visibility }
                        slidingUpPanelLayout.isOverlayed =
                            false // 이렇게해야 rcView contents 와 안겹침 = (마지막 칸)이 자동으로 panel 위로 올라가서 보임.
                    }
                }
            }
        })


    }


    fun updateResultOnRcView(fullRtClassList: MutableList<RingtoneClass>) {
        Log.d(TAG, "showResult: 5) called..Finally! ")


        // IAP related: Initialize IAP and send instance <- 이게 시간이 젤 오래걸리는듯.

//                iapInstance = MyIAPHelper(this, rcvAdapterInstance, fullRtClassList) //reInitialize
//                iapInstance.refreshItemIdsAndMp3UrlMap() // !!!!!!!!!!!!!!여기서 일련의 과정을 거쳐서 rcView 화면 onBindView 까지 해줌!!

        // Update MediaPlayer.kt
//                mpClassInstance.createMp3UrlMap(fullRtClassList)
        // Update Recycler View
        rcvAdapterInstance.updateRecyclerView(fullRtClassList) // todo: 추후 // comment 시킬것. MyIAPHelper.kt 에서 해주기로 함!
        rcvAdapterInstance.updateRingToneMap(fullRtClassList)// todo: 이 map 안 쓰이는것 같은데 흐음.. (우리는 Map 기반이므로 list 정보를 -> 모두 Map 으로 업데이트!)


    }


}
