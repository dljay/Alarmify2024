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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjadapters.GlideApp
import com.theglendales.alarm.jjadapters.MyNetWorkChecker
import com.theglendales.alarm.jjadapters.RcCommIntf
import com.theglendales.alarm.jjadapters.RcViewAdapter
import com.theglendales.alarm.jjdata.GlbVars
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjfirebaserepo.FirebaseRepoClass
import com.theglendales.alarm.jjmvvm.*
import com.theglendales.alarm.jjmvvm.helper.VHolderUiHandler
import com.theglendales.alarm.jjmvvm.iapAndDnldManager.BtmShtSingleDNLDV2

import com.theglendales.alarm.jjmvvm.iapAndDnldManager.MyDownloaderV2
import com.theglendales.alarm.jjmvvm.iapAndDnldManager.MyIAPHelperV2
import com.theglendales.alarm.jjmvvm.iapAndDnldManager.MyIAPHelperV3
import com.theglendales.alarm.jjmvvm.mediaplayer.MyCacher
import com.theglendales.alarm.jjmvvm.mediaplayer.MyMediaPlayer
import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp
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




    //IAP
    lateinit var iapInstanceV2: MyIAPHelperV2
    private val iapInstanceV3: MyIAPHelperV3 by globalInject()
    //Download 관련
    lateinit var myDownloaderV2: MyDownloaderV2
    lateinit var btmSht_SingleDNLDV: BtmShtSingleDNLDV2
    //Network Checker
    lateinit var myNetworkCheckerInstance: MyNetWorkChecker
    //ViewModel 5종 생성
    private val jjMainVModel: JjMainViewModel by viewModels() // [LiveData] + [Flow]
    //Toast Messenger
    private val toastMessenger: ToastMessenger by globalInject() //ToastMessenger


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
    lateinit var mPlayer_bdg1_intense: ImageView
    lateinit var mPlayer_bdg2_gentle: ImageView
    lateinit var mPlayer_bdg3_nature: ImageView
    lateinit var mPlayer_bdg4_location: ImageView
    lateinit var mPlayer_bdg5_popular: ImageView
    lateinit var mPlayer_bdg6_misc: ImageView


    // listfrag 가거나 나갔다왔을 때 관련.
    var isFireBaseFetchDone = false // a) 최초 rcV 열어서 모든게 준비되면 =true, b) 다른 frag 로 나갔다왔을 때 reconstructXX() 다 끝나면 true.
    var currentClickedTrId = -1

    //Firebase 관련
    private val firebaseRepoInstance: FirebaseRepoClass by globalInject()
    lateinit var jjFbVModel: JjFirebaseViewModel
    var fullRtClassList: MutableList<RtInTheCloud> = ArrayList()

    // Basic overridden functions -- >
    override fun onCreate(savedInstanceState: Bundle?) {

        isFireBaseFetchDone=false // ListFrag 갔을 때 이 값이 계속 true 로 있길래. 여기서 false 로 해줌. -> fb 로딩 끝나면 true 로 변함.
        Log.d(TAG, "onCreate: jj-called..isEverythingReady=$isFireBaseFetchDone, currentClickedTrId=$currentClickedTrId")
        super.onCreate(savedInstanceState)
    }

//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//        Log.d(TAG, "onActivityCreated: jj-2ndFrag Activity!!Created!!")
//    }

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
    //BtmSht_SingleDnld init (싱글톤으로)
        btmSht_SingleDNLDV = BtmShtSingleDNLDV2.newInstance()


    //  LIVEDATA ->

        //1) ViewModel 5종 생성(RcvVModel/MediaPlayerVModel)
            //1-B) jjMpViewModel 생성
            val jjMpViewModel = ViewModelProvider(requireActivity()).get(JjMpViewModel::class.java)
            //1-C) jjMyDownloaderViewModel 생성
            val jjDNLDViewModel = ViewModelProvider(requireActivity()).get(JjDNLDViewModel::class.java)
            //1-D) jjFirebaseVModel Init
            jjFbVModel = ViewModelProvider(requireActivity()).get(JjFirebaseViewModel::class.java)

        //2) LiveData Observe
            //2-A) rcV 에서 클릭-> rcvViewModel -> 여기로 전달. [!! 기존 클릭해놓은 트랙이 있으면 ListFrag 갔다왔을때 자동으로 그전 track 값을 (fb 로딩전에) 호출하는 문제있음!!] -> isEverythingReady 로 해결함.
        //Media Player ViewMODEL Observe
            //2-B-가) MP: MediaPlayer 에서의 Play 상태(loading/play/pause) 업뎃을 observe
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

            //2-B-나) MP: seekbar 업뎃을 위한 현재 곡의 길이(.duration) observe. (MyMediaPlayer -> JjMpViewModel-> 여기로)
            jjMpViewModel.songDuration.observe(viewLifecycleOwner, { dur ->
                Log.d(TAG, "onViewCreated: duration received = ${dur.toInt()}")
                seekBar.max = dur.toInt()
                // c) **GlbVar 저장용 **
                //GlbVars.seekBarMax = dur.toInt()
            })
            //2-B-다) MP: seekbar 업뎃을 위한 현재 곡의 길이(.duration) observe. (MyMediaPlayer -> JjMpViewModel-> 여기로)
            jjMpViewModel.currentPosition.observe(viewLifecycleOwner, { playbackPos ->
                //Log.d(TAG, "onViewCreated: playback Pos=${playbackPos.toInt()} ")
                    seekBar.progress = playbackPos.toInt() +200
                // c) **GlbVars 저장용 ** 현재 재생중인 seekbar 위치
                //GlbVars.seekbarProgress = playbackPos.toInt() +200
                //GlbVars.playbackPos = playbackPos
                })

        //DNLD ViewMODEL Observe //한번 btmSht_SingleDNLDV Frag 를 보여준뒤-> ListFrag -> SecondFrag 복귀 했을 때 아래 LiveData 가 후루루룩 다 불리는 문제 => onDestroy() 에서 강제 VModel Clear 해줬음!
            //2-C-가) DNLD: RtOnThePhone Obj 받기 (UI 갱신: DNLD Dialogue 열어주고 곡 제목 표시)
            jjDNLDViewModel.dnldRtObj.observe(viewLifecycleOwner, {rtWithAlbumArtObj ->
                Log.d(TAG, "onViewCreated: trId= ${rtWithAlbumArtObj.trIdStr}, received rtObj = $rtWithAlbumArtObj")
                // Show BtmSht_SingleDNLD Frag
                btmSht_SingleDNLDV.show(requireActivity().supportFragmentManager, btmSht_SingleDNLDV.tag) // <-- listFrag 갔다 복귀했을 때 다시 DNLDFrag 열어주는 문제때문에 없앰.
                //todo: viewmodel 에 getCurrentRtObj() 만들고 -> 다운로드중인 RT 제목 + 그래픽 보여주기?
                
            })

            //2-C-나) DNLD: Status Observe. (UI 갱신: 종료[성공 or Fail])
            jjDNLDViewModel.dnldStatus.observe(viewLifecycleOwner, {dnldStatusInt ->

                Log.d(TAG, "onViewCreated: current DNLD Status is=$dnldStatusInt")
                when(dnldStatusInt) {
                    DownloadManager.STATUS_PENDING -> {} //1
                    DownloadManager.STATUS_RUNNING -> {}//2
                    DownloadManager.STATUS_PAUSED -> {}//4
                    DownloadManager.STATUS_FAILED -> {//16
                        Log.d(TAG, "onViewCreated: Observer: !!!! DNLD FAILED (XX) !!!!! ")
                        //remove BTMSHEET & Show Warning Snackbar
                        btmSht_SingleDNLDV.removeBtmSheetAfterOneSec()
                        snackBarDeliverer(requireActivity().findViewById(android.R.id.content), "Download Failed. Please check your network connectivity", false)

                    }
                    DownloadManager.STATUS_SUCCESSFUL-> {//8 <- 다시 secondFrag 들어왔을 때 뜰 수 있음.
                        Log.d(TAG, "onViewCreated: Observer: DNLD SUCCESS (O)  ")
                        // Prgrs Bar 만빵으로 채워주고 -> BtmSheet 없애주기 (만빵 안 차면 약간 허탈..)
                        btmSht_SingleDNLDV.animateLPI(100,1) //  그래프 만땅!
                        btmSht_SingleDNLDV.removeBtmSheetAfterOneSec() //1 초 Delay 후 btmSheet 없애주기.
                        snackBarDeliverer(requireActivity().findViewById(android.R.id.content), "DOWNLOAD COMPLETED.", false)
                    }
                    else -> {btmSht_SingleDNLDV.removeBtmSheetAfterOneSec()
                        snackBarDeliverer(requireActivity().findViewById(android.R.id.content), "Unknown Download Status received. Status Code=$dnldStatusInt", false)
                        }

                }
            })
            //2-C-다 DNLD: (UI 갱신: Prgrs 애니메이션 보여주기)
            jjDNLDViewModel.dnldPrgrs.observe(viewLifecycleOwner, {dnldPrgrs ->
                Log.d(TAG, "onViewCreated: current DNLD Progress is=$dnldPrgrs")
                btmSht_SingleDNLDV.prepAndAnimateLPI(dnldPrgrs) // 여기서 prgrs 확인 및 기존 Animation 작동중인지 확인 후 Progress Bar Animation 작동.
            })
            //2-C-라 MultiDNLD 진행되었을때 SnackBar 로 알림 (다운로드 결과까지 포함) //todo: 과연 boolArray 가 최선일지..
            jjDNLDViewModel.isMultiDnldRunning.observe(viewLifecycleOwner, {arrayBool ->
                if(arrayBool.size == 2) { // 정상이라면 arrayBool 은 값을 두개만 포함해야한다. ex.) true, true = 작동ok, 에러없음.
                    Log.d(TAG, "onViewCreated: **[멀티] 다운로드 가동됨=${arrayBool[0]} 에러여부=${arrayBool[1]}")
                    when(arrayBool[1]) {
                        true -> { snackBarDeliverer(requireActivity().findViewById(android.R.id.content),"UNABLE TO RECOVER SOME OF THE PURCHASED ITEMS.", false)}
                        false -> {snackBarDeliverer(requireActivity().findViewById(android.R.id.content),"RECOVERING PREVIOUSLY OWNED ITEMS ..", false)}
                    }
                }
            })
            //2-D-가 NetworkCheck [FLOW] StateFlow 사용!(O)
            // viewlifecycleowner: 현재 생성되는 view 의 lifecycle. 그리고 그것에 종속된 coroutineScope= lifecyclescope?

            //Fragments should always use the viewLifecycleOwner to trigger UI updates.
             viewLifecycleOwner.lifecycleScope.launch {
                //repeatOnLifeCycle() : 이 블록 안은 이 lifecycle 의 onStart() 에서 실행- onStop() 에서 cancel. lifecycle 시작하면 자동 re-launch!
                lifecycle.repeatOnLifecycle(State.RESUMED) {
                    launch {
                        jjMainVModel.selectedRow.collect { rtInTheCloudObj -> currentClickedTrId = rtInTheCloudObj.id
                            Log.d(TAG,"onViewCreated: !!! [MainVModel] 옵저버!! 트랙ID= ${rtInTheCloudObj.id}, \n currentClickedTrId=$currentClickedTrId")
                                updateMiniPlayerUiOnClick(rtInTheCloudObj) // 동시에 ListFrag 갔다왔을때도 이걸 통해서 [복원]
                         }
                    }
                }
            }

        //3) Firebase ViewModel Initialize


        //4) IAP ViewModel


        //5)이제 ViewModel 들을 넘김: RcvAdapter & MediaPlayer & MiniPlayer Instance 생성.
            mpClassInstance = activity?.let {MyMediaPlayer(it, jjMpViewModel)}!!
            rcvAdapterInstance = activity?.let {RcViewAdapter(ArrayList(),it,jjMainVModel,mpClassInstance)}!! // it = activity. 공갈리스트 넣어서 instance 만듬 //todo: okay to pass VModel to Adapter?
            myDownloaderV2 = activity?.let {MyDownloaderV2(it,jjDNLDViewModel)}!!
            iapInstanceV2 = MyIAPHelperV2(requireActivity(), rcvAdapterInstance, myDownloaderV2)
            myNetworkCheckerInstance = context?.let { MyNetWorkChecker(it, jjMainVModel) }!!

        //0) 2021.1.6 MainViewModel //todo: 이거 flow 로 바꾸고 lottieAnim("loading") 과 타이밍 비교. 여기 저~~기 위에 써주기 (어차피 onStart() 에서 불릴테니깐)
            //[MainVModel-1] Firebase 에서 새로운 리스트를 받을 떄 (or 단순 listFrag<->SecondFrag 복귀 후 livedata 기존 값 복기)
            jjMainVModel.rtInTheCloudList.observe(viewLifecycleOwner) {rtListPlusIAPInfo->
                Log.d(TAG, "---------------------- [MainVModel-RTLIST] rtListFromFb via ViewModel= $rtListPlusIAPInfo")
                fullRtClassList = rtListPlusIAPInfo // 추후 Chip Sorting 때 사용
                mpClassInstance.createMp3UrlMap(rtListPlusIAPInfo)
                rcvAdapterInstance.refreshRecyclerView(rtListPlusIAPInfo)
                lottieAnimController("stop")
            }
            //[MainVModel-2] Network Availability 관련 (listFrag->SecondFrag 오면 두번 들어옴. 1) livedata 기존 값 복기 2)SecondFrag 시작하면서 setNetworkListener()
            jjMainVModel.isNetworkWorking.observe(viewLifecycleOwner) { isNetworkWorking ->
                Log.d(TAG, "[MainVModel-NT] Network Availability detected, isNetworkWorking=[$isNetworkWorking] ")

                //A-1) true && false (기존에 O 지금은 X) or A-2) false && false (기존도 X 지금도 X) - 혹시 몰라서 넣음.
                if(jjMainVModel.prevNT && !isNetworkWorking || !jjMainVModel.prevNT && !isNetworkWorking  ) {
                    Log.d(TAG, "[MainVModel-NT] Network Error! Launch Lottie!")
                    lottieAnimController("error")
                    //Toast.makeText(this.context,"Error: Unable to connect",Toast.LENGTH_SHORT).show()
                    toastMessenger.showMyToast("Error: Unable to connect",isShort = true)
                }
                //B) false && true (기존에 X 지금은 O)
                else if(!jjMainVModel.prevNT && isNetworkWorking) {
                    Log.d(TAG, "[MainVModel-NT] Network Working Again! Remove Error Lottie and Relaunch FB!!")
                    lottieAnimController("stop")
                    jjMainVModel.refreshAndUpdateLiveData() // Relaunch FB! -> 사실 lottie 가 사라지면서 기존 RcView 가 보여서 상관없긴 하지만.애초 Network 불가상태로 접속해 있을 수 있음.
                }
                jjMainVModel.prevNT = isNetworkWorking // 여기서 ViewModel 안의 값을 바꿔줌에 따라 위에서처럼 Bool 값 prev&now 변화를 감지 할 수 있음.

            }
            //[MainVModel-3] (구매 후) DNLD 상태 업뎃 -> UI 반영 (DnldPanel 보여주기 등)
            jjMainVModel.getLiveDataFromDownloaderV3().observe(viewLifecycleOwner) { dnldInfo->
                Log.d(TAG, "[MainVModel-DNLD-A] Title=${dnldInfo.dnldTrTitle}, Status=${dnldInfo.status}, Prgrs=${dnldInfo.prgrs} ")

                if(dnldInfo.prgrs==-1 && dnldInfo.status==-1) { // ListFrag 복귀 후 용도면 해당값은 false  -> 아무것도 안하고 끝!
                    Log.d(TAG, "onViewCreated: 아마도 ListFrag 다녀오신듯..? 암것도 안하고 여기서 quit!")
                    return@observe
                }
                //A) Prgrs 를 받는순간 isPreparingToDNLD -> false -> Lottie Loading Circle (GONE), ProgressBar(VISIBLE)
                when(dnldInfo.isBufferingToDNLD) { // isBufferingToDNLD
                    false -> {btmSht_SingleDNLDV.showLPIAndHideLottieCircle(isPreparingToDNLD = false)}
                }

                //B) STATUS 에 따라서 BtmSheet 열기 & 닫기 (모든 Status 는 한번씩만 받는다)
                when(dnldInfo.status) { // 참고** Pending=1 , Running=2, Paused=4, Successful=8, Failed=16
                    0 -> { // 내가 지정한 숫자. '0' 이면 (다운로드 attempt 시작하자마자) -> BtmSheet 을 열어줘!
                        Log.d(TAG, "[MainVModel-DNLD-B] STATUS=0 ")
                            btmSht_SingleDNLDV.show(requireActivity().supportFragmentManager, btmSht_SingleDNLDV.tag)}

                    DownloadManager.STATUS_FAILED -> { //16
                        Log.d(TAG, "[MainVModel-DNLD-B] STATUS=FAILED(16) Observer: !!!! DNLD FAILED (XX) !!!!! ")
                        //remove BTMSHEET & Show Warning Snackbar
                        btmSht_SingleDNLDV.removeBtmSheetAfterOneSec()
                        snackBarDeliverer(requireActivity().findViewById(android.R.id.content), "Download Failed. Please check your network connectivity", false)
                        return@observe
                    }
                    DownloadManager.STATUS_SUCCESSFUL-> { //8 <- 다시 secondFrag 들어왔을 때 뜰 수 있음.
                        Log.d(TAG, "[MainVModel-DNLD-B] STATUS=SUCCESSFUL(8) Observer: DNLD SUCCESS (O)  ")
                        // Prgrs Bar 만빵으로 채워주고 -> BtmSheet 없애주기 (만빵 안 차면 약간 허탈..)
                        btmSht_SingleDNLDV.animateLPI(100,1) //  그래프 만땅= 120 으로 설정해줬음.
                        btmSht_SingleDNLDV.removeBtmSheetAfterOneSec() //1 초 Delay 후 btmSheet 없애주기.
                        snackBarDeliverer(requireActivity().findViewById(android.R.id.content), "DOWNLOAD COMPLETED.", false)
                        return@observe
                    }
                    -444 -> { // VModel> Coroutine > .invokeOnCompletion 에서 handler 가 에러 감지 (내가 임의로 넣은 숫자 -444)
                        Log.d(TAG, "[MainVModel-DNLD-B] STATUS=-444")
                        btmSht_SingleDNLDV.removeBtmSheetImmediately() // 에러메시지는 ViewModel 에서 Toast 로 전파. //
                        toastMessenger.showMyToast("Download Failed..",isShort = false)
                        return@observe
                    }
                   /* else -> {btmSht_SingleDNLDV.removeBtmSheetImmediately() // 다운로드 실패- 내가 만든 -444 코드나 그 외 Status 를 받으면 -> 바로 BtmSht 없애고 + Toast 메시지
                        toastMessenger.showMyToast("Download Failed..Status Code=${dnldInfo.status}",isShort = false)
                        return@observe
                        //snackBarDeliverer(requireActivity().findViewById(android.R.id.content), "Unknown Download Status received. Status Code=${dnldInfo.status}", false)
                    }*/
                }
                //C) Progress Animation
                if(dnldInfo.prgrs >0 ) {
                    Log.d(TAG, "[MainVModel-DNLD-C] Prgrs Animation! (prgrs=${dnldInfo.prgrs})")
                    btmSht_SingleDNLDV.prepAndAnimateLPI(dnldInfo.prgrs) // 그래프 만땅= 100 .
                    btmSht_SingleDNLDV.updateTitleTextView(dnldInfo.dnldTrTitle) // Tr Title 보여주기 (첫 Prgrs 받는 순간 반영. 이후 prgrs 받을 때마다 setText 되지만. 상관 없을듯..)
                }

            }

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
        initChip(view)
        //setNetworkAvailabilityListener() // 처음 SecondFrag 를 열면 여기서 network 확인 -> 이후 connectivity yes/no 상황에 따라 -> lottie anim 보여주기 + re-connect.
        registerSwipeRefreshListener()

    // MyCacher Init()
        val myCacherInstance = context?.let { MyCacher(it, it.cacheDir, mpClassInstance) }
        if (myCacherInstance != null) {
            myCacherInstance.initCacheVariables()
        }

    }

    override fun onStart() {
        super.onStart()
        //Log.d(TAG, "onStart: 2nd Frag // viewLifecycleOwner.lifecycle.currentState=${viewLifecycleOwner.lifecycle.currentState}")
        Log.d(TAG, "onStart: 2nd Frag // lifecycle.currentState=${lifecycle.currentState}")
    }
    override fun onResume() {
        super.onResume()
        //Log.d(TAG, "onResume: 2nd Frag // viewLifecycleOwner.lifecycle.currentState=${viewLifecycleOwner.lifecycle.currentState} ")
        Log.d(TAG, "onResume: 2nd Frag! // lifecycle.currentState=${lifecycle.currentState}")
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

        Log.d(TAG, "onPause: 2nd Frag! // viewLifecycleOwner.lifecycle.currentState=${viewLifecycleOwner.lifecycle.currentState}")
        Log.d(TAG, "onPause: 2nd Frag! // lifecycle.currentState=${lifecycle.currentState}")
        //collapseSlidingPanel()
        //1) 현재 음악이 재생중이든 아니든 (재생중이 아니었으면 어차피 pauseMusic() 은 의미가 없음)
            mpClassInstance.pauseMusic() // a)일단 PAUSE 때리고
            mpClassInstance.removeHandler() // b)handler 없애기
        Log.d(TAG, "onPause: GlbVars 정보: CurrentTrId=${GlbVars.clickedTrId}")

        //2) 최종적으로 선택해놓은 트랙 아이디
        //3) 다시 돌아왔을 때 Slide 의 upperUi 에서 빨간색 앨범커버가 보였다 다른 앨범으로 교체되는 현상을 막기 위해.

        //3) 그리고 나서 save current play data to SharedPref using gson.
        //mySharedPrefManager.savePlayInfo(playInfo)

    }
    override fun onDestroy() {
        super.onDestroy()

        Log.d(TAG, "onDestroy: 2nd Frag! // lifecycle.currentState=${lifecycle.currentState}") //DESTROYED 로 뜬다.
         mpClassInstance.releaseExoPlayer() //? 여기 아니면 AlarmsListActivity 에다가?

         //requireActivity().viewModelStore.clear()// ListFrag 로 갈때는 그냥 ViewModel Clear 해줌 -> 다시 복귀했을 때 생쑈 없애기 위해..
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
        var spaceFifty="                                                 " //50칸 (기존 사용)
        var spaceSixty="                                                           " //60칸
        tv_upperUi_title.text = spaceFifteen+ rtObj.title // miniPlayer(=Upper Ui) 의 Ringtone Title 변경 [제목 앞에 15칸 공백 더하기-흐르는 효과 위해]
        if(rtObj.title.length <6) {tv_upperUi_title.append(spaceSixty) } // [제목이 너무 짧으면 6글자 이하] -> [뒤에 공백 50칸 추가] // todo: null safety check?
        else {tv_upperUi_title.append(spaceTwenty) // [뒤에 20칸 공백 추가] 흐르는 text 위해서. -> 좀 더 좋은 공백 채우는 방법이 있을지 고민..
        }

    //Sliding Panel -  Lower UI
        tv_lowerUi_about.text = rtObj.description // Description 채워주기
        val badgeStrList = rtObj.bdgStrArray// Badge Sort
        showOrHideBadgesOnMiniPlayer(badgeStrList) // Badge 켜고끄기- MiniPlayer 에 반영
        //
        //1) Mini Player 사진 변경 (RcView 에 있는 사진 그대로 옮기기)
        GlideApp.with(requireContext()).load(rtObj.imageURL).centerCrop().error(R.drawable.errordisplay)
            .placeholder(R.drawable.placeholder).listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?,model: Any?,target: Target<Drawable>?,isFirstResource: Boolean): Boolean {
                    Log.d(TAG, "onLoadFailed: failed ... ")
                    return false
                }

                override fun onResourceReady(resource: Drawable?,model: Any?,target: Target<Drawable>?,dataSource: DataSource?,
                                             isFirstResource: Boolean): Boolean {
                    iv_upperUi_thumbNail.setImageDrawable(resource) //RcV 현재 row 에 있는 사진으로 설정
                    iv_lowerUi_bigThumbnail.setImageDrawable(resource) //RcV 현재 row 에 있는 사진으로 설정
                    //
                    return false
                }

            }).into(iv_upperUi_thumbNail)
        //iv_upperUi_thumbNail.setImageDrawable(ivInside_Rc.drawable) //RcV 현재 row 에 있는 사진으로 설정
        //iv_lowerUi_bigThumbnail.setImageDrawable(ivInside_Rc.drawable) //RcV 현재 row 에 있는 사진으로 설정


        // 최초 SlidingPanel 이 HIDDEN  일때만 열어주기. 이미 EXPAND 상태로 보고 있다면 Panel 은 그냥 둠
        if (slidingUpPanelLayout.panelState == SlidingUpPanelLayout.PanelState.HIDDEN) {
            slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED // Show Panel! 아리러니하게도 .COLLAPSED 가 (위만) 보이는 상태임!
        }

            //다운로드 Test 용도 - IAP  검증 걸치지 않고 해당 번호에 넣은 RT 다운로드 URL 로 이동. [원복]
//                val testRtHelixObj = RtInTheCloud(title = "SoundHelix8.mp3","moreshit","desc","imgUrl",
//                mp3URL = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",id=1, iapName = "shitbagHelix")
//                myDownloaderV2.singleFileDNLD(testRtHelixObj)

    }

    private fun initChip(v: View) {
        //Chip Related#1 (Init)
        chipGroup = v.findViewById(R.id.id_chipGroup)
        for (i in 0 until chipGroup.childCount) {
            val chip: Chip = chipGroup.getChildAt(i) as Chip
            chip.setOnCheckedChangeListener { _, isChecked ->
                createStringListFromChips()
                when (isChecked) {
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
    // Chip Related #2 (Listener Setup & Sending a Request to FbRepoClass.)
    private fun createStringListFromChips() {
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
        Log.d(TAG, "createStringListFromChips: tagsList= $tagsList")

        if(tagsList.isNotEmpty()) {
            myIsChipChecked= true // pull to refresh  했을 때 이 값을 근거로..
            filterListByTags(tagsList)
        }else if(tagsList.isEmpty()) { // 체크 된 chip 이 하나도 없음!
            myIsChipChecked= false
            filterListByTags(tagsList)
        }
    }
     //위에 Chip 이 선택된 항목(string list)을 여기로 전달.
    private fun filterListByTags(tagsList: MutableList<String>) {

    // ** String List 두개 비교하기 ** rtObject.bdgStrArray & tagsList. ex) [INT, NATURE] .. //
         // tag 2개 설정 -> 2개 해제  -> 아무것도 없다 ! => 그냥 원복: fullRtList 전체 보여주기.
         if(tagsList.isEmpty()) {
             Log.d(TAG, "filterListByTags: tagsList is Empty..Showing fullRtClassList")
             rcvAdapterInstance.refreshRecyclerView(fullRtClassList)
             return
         } else { // 그렇지 않을때는 tagsList 로 들어온 STR 에 의거- filtering 된 리스트를 rcV 에 전달. 
             val sortedList = fullRtClassList.filter { rtObject -> rtObject.bdgStrArray.containsAll(tagsList) }
             Log.d(TAG, "filterListByTags: sortedList.size=${sortedList.size}, sortedList=$sortedList")

             rcvAdapterInstance.refreshRecyclerView(sortedList.toMutableList())
         }
     //test <--
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


    //lottieAnimation Controller = 로딩:0 번, 인터넷에러:1번, 정상:2번(lottie 를 감춰!)
    private fun lottieAnimController(status: String) {
        when (status) {
            "loading" -> {
                lottieAnimationView.setAnimation(R.raw.lottie_loading1)
            } //최초 app launch->read.. auto play 기 때문에
            "error" -> {
                activity?.runOnUiThread(Runnable
                {
                    Log.d(TAG, "lottieAnimController: NO INTERNET ERROR!!")
                    lottieAnimationView.visibility = LottieAnimationView.VISIBLE
                    lottieAnimationView.setAnimation(R.raw.lottie_error1)

                    snackBarDeliverer(lottieAnimationView,"Please kindly check your network connection status",false)

                    //todo: 여기 SnackBar 에서 View 가 불안정할수 있음=>try this? -> Snackbar.make(requireActivity().findViewById(android.R.id.content), "..", Snackbar.LENGTH_LONG).show()

                })
            }
            "stop" -> {
                activity?.runOnUiThread(Runnable
                {
                    Log.d(TAG, "lottieAnimController: STOP (any) Animation!!")
                    lottieAnimationView.cancelAnimation()
                    lottieAnimationView.visibility = LottieAnimationView.GONE
                })
            }

        }
    }
//MediaPlayerViewModel 을 Observe

    //Firebase ViewModel 을 Observe
    private fun observeAndLoadFireBase() {
        // 현재도 lottie 는 나오는 상황 (setUpLateIniUis() 에서 벌써 loading 실행해놨음)
        //1. 인터넷 가능한지 체크
        //인터넷되는지 체크
      /*  val isInternetAvailable: Boolean = myNetworkCheckerInstance.isNetWorkAvailable()
        if (!isInternetAvailable) { // 인터넷 사용 불가!
            Log.d(TAG, "loadFromFireBase: isInternetAvailable= $isInternetAvailable")
            lottieAnimController("error")
            return // 더이상 firebase 로딩이고 나발이고 진행 안함!!
        }*/

        //2. If we have internet connectivity, then call FireStore!

        //Log.d(TAG, "onViewCreated: jj LIVEDATA- (Before Loading) jjFirebaseVModel.liveRtList: ${jjFirebaseVModel.fullRtClassList}")
        jjFbVModel.getRtLiveDataObserver().observe(requireActivity(), Observer {
            //Log.d(TAG, "onViewCreated: jj LIVEDATA- (After Loading) jjFirebaseVModel.liveRtList: ${jjFirebaseVModel.fullRtClassList}")
            it.addOnCompleteListener {
                if (it.isSuccessful) { // Task<QuerySnapshot> is successful 일 때
            /*        Log.d(TAG, "onViewCreated: <<<<<<<<<loadPostData: successful")

                    // SwipeRefresh 돌고 있었으면.. 멈춰 (aka 빙글빙글 animation 멈춰..)
                    if (swipeRefreshLayout.isRefreshing) {
                        Log.d(TAG, "loadPostData: swipeRefresh.isRefreshing = true")
                        swipeRefreshLayout.isRefreshing = false
                    }
                    // 우선 lottie Loading animation-stop!!
                    lottieAnimController("stop") //모든게 로딩 완료되었으니 애니메이션 stop!

                    fullRtClassList = it.result!!.toObjects(RtInTheCloud::class.java)
                    //Log.d(TAG, "observeAndLoadFireBase: fullRtClassList.hashCode() = ${fullRtClassList.hashCode()}")
                // IAP
                    iapInstanceV2.refreshItemIdIapNameTitle(fullRtClassList) // 여기서 Price 정보 MAP 완성후 -> ** rcV 업데이트!(fullRtClassList 전달) **
                // Update MediaPlayer.kt 의 URL
                    mpClassInstance.createMp3UrlMap(fullRtClassList)
                // Update RcV's RT MAP
                    rcvAdapterInstance.updateRingToneMap(fullRtClassList) //updateRcV 와는 별개로 추후 ListFrag 갔다왔을 때 UI 업뎃을 위해 사용.
                    //rcvAdapterInstance.refreshRecyclerView(fullRtClassList)*/

                // *******
                // 아무 트랙도 클릭 안한 상태
                    if(GlbVars.clickedTrId == -1 || currentClickedTrId == -1) {
                        isFireBaseFetchDone = true // 이제는 rcV 를 클릭하면 그에 대해 대응할 준비가 되어있음.
                    }
                // 다른 frag 갔다가 돌아왔을 때 (or 새로고침) 했을 때- 다음의 reConstructXX() 가 다 완료되면 isEverythingReady = true 가 된다.
                    else if (GlbVars.clickedTrId > 0|| currentClickedTrId >0) { // 이중장치로 currentClickedTrId 추가함. 꼭 필요는 없긴 해..사실..
                        Log.d(TAG, "observeAndLoadFireBase: GlbVars.clickedTrId= ${GlbVars.clickedTrId}, currentClickedTrId=$currentClickedTrId")
                        // 1)만약 기존에 선택해놓은 row 가 있으면 그쪽으로 이동.
                        mySmoothScroll()
                        // 2) Highlight the Track -> 이건 rcView> onBindView 에서 해줌.
                        val prevSelectedVHolder = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]
                        // 3) Fill in the previous selected track info to MINIPlayer!!!
                        reConstructSLPanelTextOnReturn(prevSelectedVHolder, GlbVars.clickedTrId)
                         //4) Update RcV UI! (VuMeter 등)
                        reConstructTrUisOnReturn(GlbVars.clickedTrId)
                    }
                }/* else { // 에러났을 때
                    lottieAnimController("error")
                    Toast.makeText(
                        this.context,
                        "Error Loading Data from Firebase. Error: ${it.exception.toString()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }*/
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
                //Handler(Looper.getMainLooper()).post { observeAndLoadFireBase() } //todo: jjMainVModel.getRtLisFromFb() 로 바꾸기!
                jjMainVModel.refreshAndUpdateLiveData()
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
        //일단 lottieAnim - Loading 애니메이션 틀어주기
        lottieAnimController("loading")

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
        //b-2) lower ui Badge
        // Badge 관련
        mPlayer_bdg1_intense = v.findViewById(R.id.mPlayer_badge1_Intense)
        mPlayer_bdg2_gentle = v.findViewById(R.id.mPlayer_badge2_Gentle)
        mPlayer_bdg3_nature = v.findViewById(R.id.mPlayer_badge3_Nature)
        mPlayer_bdg4_location = v.findViewById(R.id.mPlayer_badge_4_Location)
        mPlayer_bdg5_popular = v.findViewById(R.id.mPlayer_badge_5_Popular)
        mPlayer_bdg6_misc = v.findViewById(R.id.mPlayer_badge_6_Misc)

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
        isFireBaseFetchDone = true

    }
// 2)SharedPref 에 저장된 재생중 Tr 정보를 바탕으로 SlidingPanel UI 를 재구성.
    private fun reConstructSLPanelTextOnReturn(vHolder: RcViewAdapter.MyViewHolder?, trackId: Int) { // observeAndLoadFireBase() 여기서 불림
        if (vHolder != null) {
            Log.d(TAG, "setSlidingPanelOnReturn: called. vHolder !=null. TrackId= $trackId")


            val rtObjFromList = fullRtClassList.single { rtObj -> rtObj.id == trackId }
            val ivInside_Rc = vHolder.iv_Thumbnail
            Log.d(TAG,"setSlidingPanelOnReturn: title= ${rtObjFromList?.title}, description = ${rtObjFromList?.description}")
        //Sliding Panel - Upper UI
            var spaceFifteen="               " // 15칸
            var spaceTwenty="                    " // 20칸
            var spaceFifty="                                                 " //50칸 (기존 사용)
            var spaceSixty="                                                           " //60칸
            tv_upperUi_title.text = spaceFifteen+ rtObjFromList?.title // miniPlayer(=Upper Ui) 의 Ringtone Title 변경 [제목 앞에 15칸 공백 더하기-흐르는 효과 위해]
            if(rtObjFromList?.title!!.length <6) {tv_upperUi_title.append(spaceSixty) } // [제목이 너무 짧으면 6글자 이하] -> [뒤에 공백 50칸 추가] // todo: null safety check?
            else {tv_upperUi_title.append(spaceTwenty) // [뒤에 20칸 공백 추가] 흐르는 text 위해서. -> 좀 더 좋은 공백 채우는 방법이 있을지 고민..
            }

        //Sliding Panel -  Lower UI
            tv_lowerUi_about.text = rtObjFromList?.description
            iv_lowerUi_bigThumbnail.setImageDrawable(ivInside_Rc.drawable)
        //Sliding Panel - Upper UI
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
    
    fun testRcComm(){ object : RcCommIntf {
        override fun someFuncion() {
            Log.d(TAG, "somexxFunction: called")
            }
        }
    }


}
