package com.theglendales.alarm.presenter

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjadapters.GlideApp
import com.theglendales.alarm.jjadapters.RtPickerAdapter

import com.theglendales.alarm.jjmvvm.JjRtPickerVModel
import com.theglendales.alarm.jjmvvm.helper.BadgeSortHelper
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import com.theglendales.alarm.jjmvvm.mediaplayer.ExoForLocal
import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjmvvm.util.RtOnThePhone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList

// startActivityForResult 참고: https://youtu.be/AD5qt7xoUU8


private const val TAG="RtPickerActivity"

private const val PICKER_RESULT_RT_TITLE="RtTitle"
private const val PICKER_RESULT_AUDIO_PATH="AudioPath"
private const val PICKER_RESULT_ART_PATH="ArtPath"
private const val CURRENT_RT_FILENAME_KEY= "currentRtFileName_Key"


class RtPickerActivity : AppCompatActivity() {
    // ViewModel
    private val rtPickerVModel: JjRtPickerVModel by viewModels()
    //ToolBar (ActionBar 대신하여 모든 Activity 에 만들어주는 중.)
    private lateinit var toolBar: Toolbar
    // DiskSearcher
    private val myDiskSearcher: DiskSearcher by globalInject()
    private val mySharedPrefManager: MySharedPrefManager by globalInject()
    //RcView Related
    lateinit var rtPickerRcvAdapter: RtPickerAdapter
    lateinit var rcView: RecyclerView
    lateinit var layoutManager: LinearLayoutManager

    //Media Player
    private val exoForLocal: ExoForLocal by globalInject()
    //SlidingUp Panel (AKA mini Player) UIs
    lateinit var slidingUpPanelLayout: SlidingUpPanelLayout // SlideUpPanel 전체
    lateinit var allBtmSlideLayout: RelativeLayout // SlideUpPanel 중 상단(돌출부) 전체

    //SlidingUp Panel [음악재생 mini player]
        // a) 상단 Uis

        private val ll_play_pause_container by lazy {allBtmSlideLayout.findViewById(R.id.ll_playPause_btn_container) as LinearLayout}
        private val imgbtn_Play by lazy { allBtmSlideLayout.findViewById(R.id.id_imgbtn_upperUi_play) as ImageButton }
        private val imgbtn_Pause by lazy { allBtmSlideLayout.findViewById(R.id.id_imgbtn_upperUi_pause) as ImageButton }
        private val seekBar by lazy { allBtmSlideLayout.findViewById(R.id.id_upperUi_Seekbar) as SeekBar }

        private val upperUiHolder by lazy { allBtmSlideLayout.findViewById(R.id.id_upperUi_ll) as LinearLayout }    // 추후 이 부분이 fade out
        //private val tv_upperUi_title by lazy { allBtmSlideLayout.findViewById(R.id.id_upperUi_tv_title) as TextView }
        lateinit var tv_upperUi_title: TextView // 이놈만 흐르는 Text (Marquee) Fx 위해 lateinit 으로 대체.

        private val iv_upperUi_thumbNail by lazy { allBtmSlideLayout.findViewById(R.id.id_upperUi_iv_coverImage) as ImageView}
        private val iv_upperUi_ClickArrow by lazy { allBtmSlideLayout.findViewById(R.id.id_upperUi_iv_clickarrowUp) as ImageView }
        private val cl_upperUi_entireWindow by lazy { allBtmSlideLayout.findViewById(R.id.id_upperUi_ConsLayout) as ConstraintLayout }
        // b) 하단 Uis
        private val constLayout_entire by lazy {allBtmSlideLayout.findViewById(R.id.id_lowerUI_entireConsLayout) as ConstraintLayout}
        private val iv_lowerUi_bigThumbnail by lazy { allBtmSlideLayout.findViewById(R.id.id_lowerUi_iv_bigThumbnail) as ImageView }
        private val tv_lowerUi_about by lazy { allBtmSlideLayout.findViewById(R.id.id_lowerUi_tv_Description) as TextView }
        private val btn_buyThis by lazy { allBtmSlideLayout.findViewById(R.id.btn_buyThis) as TextView } // 가격+ DNDL 표시 있는 버튼.

        // b-2) Badges
        private val iv_badge1_Intense by lazy {allBtmSlideLayout.findViewById(R.id.mPlayer_badge1_Intense) as ImageView}
        private val iv_badge2_Gentle by lazy {allBtmSlideLayout.findViewById(R.id.mPlayer_badge2_Gentle) as ImageView}
        private val iv_badge3_Nature by lazy {allBtmSlideLayout.findViewById(R.id.mPlayer_badge3_Nature) as ImageView}
        private val iv_badge4_Location by lazy {allBtmSlideLayout.findViewById(R.id.mPlayer_badge_4_Location) as ImageView}
        private val iv_badge5_Popular by lazy {allBtmSlideLayout.findViewById(R.id.mPlayer_badge_5_Popular) as ImageView}
        private val iv_badge6_Misc by lazy {allBtmSlideLayout.findViewById(R.id.mPlayer_badge_6_Misc) as ImageView}
        // b-3) Intensity (Speaker) 아이콘
        private val mp_iv_lightning_1 by lazy { allBtmSlideLayout.findViewById(R.id.mp_iv_lightning_1) as ImageView }
        private val mp_iv_lightning_2 by lazy { allBtmSlideLayout.findViewById(R.id.mp_iv_lightning_2) as ImageView }
        private val mp_iv_lightning_3 by lazy { allBtmSlideLayout.findViewById(R.id.mp_iv_lightning_3) as ImageView }
        private val mp_iv_lightning_4 by lazy { allBtmSlideLayout.findViewById(R.id.mp_iv_lightning_4) as ImageView }




    override fun onCreate(savedInstanceState: Bundle?) {

        val resultIntent = Intent() // 여기서 만들어서 DetailsFrag (AlarmListActivity) 로 되돌릴 intent

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rt_picker)
        window.navigationBarColor = ContextCompat.getColor(applicationContext, R.color.jj_bg_color_2)//System NAV BAR (최하단 뒤로가기/Home 버튼 등 구성되어있는) 배경색 설정

    //1) Activity 화면 Initialize (ActionBar 등..)
        // 화면 위에 뜨는 타이틀
        toolBar = findViewById(R.id.id_toolBar_RtPicker)
        setSupportActionBar(toolBar)
        //toolBar.title = "Ringtone Picker" // 이미 activity_rt_picker.xml 에서 해줌.
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기(<-) 표시. null check?

    // 2) SlidingUpPanel (AKA MiniPlayer) UI Initialize 및 onClickListener 장착

        //a) 전체 SlidingUpPanel
        slidingUpPanelLayout = findViewById(R.id.id_sldUpPnlRtPickerActivity) // 전체 SlidingUpPanel
        allBtmSlideLayout = findViewById(R.id.ir_rl_entireSlider) // SlidingUpPanel 중 상단(돌출부) 전체
        btn_buyThis.visibility=View.GONE

        setUpSlidingPanel()

        //b) 상단 돌출부 제목 TextView 흐르는 텍스트 (Marquee) 효과
        tv_upperUi_title= allBtmSlideLayout.findViewById(R.id.id_upperUi_tv_title)

        tv_upperUi_title.apply {
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.MARQUEE
            isSelected = true
            //text ="Song Title                                           "
            // text 제목이 일정 수준 이하면 여백을 추가, 추후 title.length < xx => 정확한 카운트 알고리즘.
        }

        //c) ListenerSetup: Play & Pause Button onClickListener (mini player 상단 돌출부)
        ll_play_pause_container.setOnClickListener {
            when(imgbtn_Play.visibility) {
                View.VISIBLE -> {onMiniPlayerPlayClicked()}
                View.GONE -> {onMiniPlayerPauseClicked()}
            }
        }
        // 아래 Play(>)/Pause(||) 실제로 누르기는 너무 작아서. 감싸고 있는 ll_play_pause_container 로 대체했음.
//        imgbtn_Play.setOnClickListener {onMiniPlayerPlayClicked()}
//        imgbtn_Pause.setOnClickListener {onMiniPlayerPauseClicked()}
        seekbarListenerSetUp()


    //3) RcView 셋업-->
        rcView = findViewById<RecyclerView>(R.id.rcV_RtPicker)
        layoutManager = LinearLayoutManager(this)
        rcView.layoutManager = layoutManager


        
    //4)  LIVEDATA -> // 참고로 별도로 Release 해줄 필요 없음. if you are using observe method, LiveData will be automatically cleared in onDestroy state.
        //(가) RtPicker ViewModel
            // A)생성(RcvVModel)
            //val rtPickerVModel = ViewModelProvider(this).get(JjRtPickerVModel::class.java)

            // B) Observe - RtPicker 로 User 가 RingTone 을 골랐을 때
            rtPickerVModel.selectedRow.observe(this) { rtOnThePhone ->
                Log.d(TAG,"onCreate: rtPickerVModel 옵저버!! rtTitle=${rtOnThePhone.rtTitle}, \n rtaPath= ${rtOnThePhone.audioFilePath}, artPath= ${rtOnThePhone.artFilePathStr}")
                //B-1) Intent 에 현재 선택된 RT 의 정보담기  (AlarDetailsFrag.kt 로 연결됨) .. RT 계속 바꿀때마다 Intent.putExtra 하는데 overWrite 되는듯.
                resultIntent.putExtra(PICKER_RESULT_RT_TITLE, rtOnThePhone.rtTitle)
                resultIntent.putExtra(PICKER_RESULT_AUDIO_PATH, rtOnThePhone.audioFilePath)
                resultIntent.putExtra(PICKER_RESULT_ART_PATH, rtOnThePhone.artFilePathStr)
                setResult(RESULT_OK, resultIntent)
                //B-2) 음악 Player 에 UI 업데이트
                //B-2-a) Sliding Panel 전체
                // 최초 SlidingPanel 이 HIDDEN(안보이는 상태)면 열어주기.
                if (slidingUpPanelLayout.panelState == SlidingUpPanelLayout.PanelState.HIDDEN) {
                    slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
                } // 이미 EXPAND/Collapsed 상태로 보이면 Panel 은 그냥 둠 [.COLLAPSED = (위만) 보이는 상태임!]

        //B-2-b) Sliding Panel - Upper UI
                //제목
                // 글자 크기 고려해서 공백 추가 (흐르는 효과 Marquee FX 위해)
                var spaceFifteen = "               " // 15칸
                var spaceTwenty = "                    " // 20칸
                var spaceFifty = "                                                 " //50칸 (기존 사용)
                var spaceSixty = "                                                           " //60칸
                tv_upperUi_title.text =
                    spaceFifteen + rtOnThePhone.rtTitle // miniPlayer(=Upper Ui) 의 Ringtone Title 변경 [제목 앞에 15칸 공백 더하기-흐르는 효과 위해]
                if (rtOnThePhone.rtTitle!!.length < 6) {
                    tv_upperUi_title.append(spaceSixty)
                } // [제목이 너무 짧으면 6글자 이하] -> [뒤에 공백 50칸 추가]
                else {
                    tv_upperUi_title.append(spaceTwenty) // [뒤에 20칸 공백 추가] 흐르는 text 위해서. -> 좀 더 좋은 공백 채우는 방법이 있을지 고민..
                }

            //AlbumCover
                val glideBuilder =
                    GlideApp.with(this).load(rtOnThePhone.artFilePathStr).centerCrop()
                        .error(R.drawable.errordisplay)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .placeholder(R.drawable.placeholder)

                glideBuilder.into(iv_upperUi_thumbNail)
                glideBuilder.into(iv_lowerUi_bigThumbnail)

        //B-2-c) Sliding Panel -  Lower UI
                tv_lowerUi_about.text = rtOnThePhone.rtDescription // Rt 설명
                //iv_lowerUi_bigThumbnail.setImageDrawable(iv_upperUi_thumbNail.drawable) // AlbumArt 현재 상단 UI 앨범아트 고대로 갖고와서 설정.
            //Badges
                val badgeStrRaw = rtOnThePhone.badgeStr // ex. "INT,NAT,POP" -> Intense, Nature, Popular 뭔 이런식.
                val badgeStrList = BadgeSortHelper.getBadgesListFromStr(badgeStrRaw)
                showOrHideBadges(badgeStrList)
            //Intensity
                val intensity = rtOnThePhone.intensity
                when(intensity) {
                    "1" -> { mp_iv_lightning_1.setImageResource(R.drawable.ic_speaker_1_yellow)
                        mp_iv_lightning_2.setImageResource(R.drawable.ic_speaker_1_grayedout)
                        mp_iv_lightning_3.setImageResource(R.drawable.ic_speaker_1_grayedout)
                        mp_iv_lightning_4.setImageResource(R.drawable.ic_speaker_1_grayedout)
                    }
                    "2" -> { mp_iv_lightning_1.setImageResource(R.drawable.ic_speaker_1_yellow)
                        mp_iv_lightning_2.setImageResource(R.drawable.ic_speaker_1_yellow)
                        mp_iv_lightning_3.setImageResource(R.drawable.ic_speaker_1_grayedout)
                        mp_iv_lightning_4.setImageResource(R.drawable.ic_speaker_1_grayedout)
                    }
                    "3" -> { mp_iv_lightning_1.setImageResource(R.drawable.ic_speaker_1_yellow)
                        mp_iv_lightning_2.setImageResource(R.drawable.ic_speaker_1_yellow)
                        mp_iv_lightning_3.setImageResource(R.drawable.ic_speaker_1_yellow)
                        mp_iv_lightning_4.setImageResource(R.drawable.ic_speaker_1_grayedout)
                    }
                    "4" -> { mp_iv_lightning_1.setImageResource(R.drawable.ic_speaker_1_yellow)
                        mp_iv_lightning_2.setImageResource(R.drawable.ic_speaker_1_yellow)
                        mp_iv_lightning_3.setImageResource(R.drawable.ic_speaker_1_yellow)
                        mp_iv_lightning_4.setImageResource(R.drawable.ic_speaker_1_yellow)
                    }
                }
            }
        //(나) MediaPlayer ViewModel - 기존 SecondFrag 에서 사용했던 'JjMpViewModel' & ExoForLocal 그대로 사용 예정. [** 현재 SecondFrag 에서는 MpVModel X MainVModel 로 통합되었음**]
        // (음악 재생 상태에 따른 플레이어 UI 업데이트) (RT 선택시 음악 재생은 RtPickerAdapter 에서 바로함.)
            //A) 생성


        //todo: 아래 getMpxx() 등 exoForLocal 안에 있는 LiveData 들은 RtPickerActivitiy 이 종료(destroy) 됨에도 계속 살아있다. Memory Leak 일수 있음.
        // 해결책중 하나는 RtPickerActivity 에서 observable 을 따로 변수로 만들어주고 removeObserver() .. 이거 하는건데 복잡해서 일단은 생략. 추후 확인 필요.
        // https://www.tabnine.com/code/java/methods/androidx.lifecycle.LiveData/removeObserver
            //B) Observe
                //B-1) MediaPlayer 에서의 Play 상태(loading/play/pause) 업뎃을 observe
        rtPickerVModel.getMpStatusLiveData().observe(this) { statusEnum ->
            Log.d(TAG,"onCreate: !!! 'MpViewModel' 옵저버! Current Music Play Status: $statusEnum")
            // a) MiniPlayer Play() Pause UI 업데이트 (현재 SecondFragment.kt 에서 해결)
            when (statusEnum) {
                StatusMp.PLAY -> {
                    showMiniPlayerPauseBtn()
                } // 최초의 ▶,⏸ 아이콘 변경을 위하여 사용. 그후에는 해당버튼 Click -> showMiniPlayerPause/Play 실행됨.
                StatusMp.BUFFERING -> {
                    showMiniPlayerPlayBtn()
                }
                StatusMp.ERROR -> {
                    showMiniPlayerPlayBtn()
                }//
                StatusMp.PAUSED -> {
                    showMiniPlayerPlayBtn()
                }

                else -> {}
            }
            // b) VuMeter 컨트롤? 여기서는 필요없을듯..
            rtPickerRcvAdapter.vumeterControl(statusEnum)
        }

        //VHolderUiHandler.LcVmIvController(StatusEnum)

                //B-2) Seekbar 관련
                    //2-C) seekbar 업뎃을 위한 현재 곡의 길이(.duration) observe. (ExoForLocal -> JjMpViewModel-> 여기로)
        rtPickerVModel.getSongDurationLiveData().observe(this, { dur ->
                        Log.d(TAG, "onCreate: duration received = ${dur.toInt()}")
                        seekBar.max = dur.toInt()

                    })
                    //2-D) seekbar 업뎃을 위한 현재 곡의 길이(.duration) observe. (ExoForLocal -> JjMpViewModel-> 여기로)
        rtPickerVModel.getCurrentPosLiveData().observe(this, { playbackPos ->
                        seekBar.progress = playbackPos.toInt() +200
                        Log.d(TAG, "onCreate: playbackPos=$playbackPos")
                    })

    // 5) Exo for Local Init
        exoForLocal.initExoForLocalPlay()
    //6) RcVAdapter Init
        rtPickerRcvAdapter = RtPickerAdapter(ArrayList(), this, rtPickerVModel, exoForLocal)
        rcView.adapter = rtPickerRcvAdapter
        rcView.setHasFixedSize(true)


    //7-a) RcVAdapter 에 보여줄 List<RtOnThePhone> 를 제공 (이미 AlarmsListFrag 실행 -> DiskSearcher 에 로딩되어있으니 특별히 기다릴 필요 없지..)
    /**
     * ListFrag 에서 해결됐어야 하지만. 늦은 다운로드 도착 혹은 중복 다운로드된 파일 (p100x-1.rta, p100x-2.rta.. etc.) 이 혹시나 있을 경우를 대비하여 DiskScan >>
     */
        if(myDiskSearcher.isDiskScanNeeded()) { // 만약 새로 스캔 후 리스트업 & Shared Pref 저장할 필요가 있다면
            Log.d(TAG, "onCreate: $$$ [RtPicker] Alright let's scan the disk!")
            //1-a) /.AlbumArt 폴더 검색 -> art 파일 list up -> 경로를 onDiskArtMap 에 저장 <trkId, ArtPath>
            myDiskSearcher.readAlbumArtOnDisk()
            //1-b-1) onDiskRtSearcher 를 시작-> search 끝나면 Default Rt(raw 폴더) 와 List Merge!
            val resultList = myDiskSearcher.onDiskRtSearcher() // rtArtPathList Rebuilding 프로세스. resultList 는 RtWAlbumArt object 리스트고 각 Obj 에는 .trkId, .artPath, .audioFileUri 등의 정보가 있음.
            //** 1-b-2) 1-b-1) 과정에서 rtOnDisk object 의 "artFilePathStr" 이 비어잇으면-> extractArtFromSingleRta() & save image(.rta) on Disk

            // 1-c) Merge 된 리스트(rtWithAlbumArt obj 로 구성)를 얼른 Shared Pref 에다 저장! (즉 SharedPref 에는 art, rta 의 경로가 적혀있음)
            mySharedPrefManager.saveRtOnThePhoneList(resultList)

            // 1-d) DiskSearcher.kt>finalRtArtPathList [Companion obj 메모리] 에 띄워놓음(갱신)
            myDiskSearcher.updateList(resultList)
            Log.d(TAG, "onCreate: --------------------------- DiskScan DONE..(Hopefully..)---------- \n\n resultList = $resultList!")
            //} // ** diskScan 종료 <--

        }
        val rtOnDiskList:  MutableList<RtOnThePhone> = DiskSearcher.finalRtArtPathList
        if(!rtOnDiskList.isNullOrEmpty()) {
            rtPickerRcvAdapter.updateRcV(rtOnDiskList)
            

        //7-b) 현재  DetailsFrag 에 설정되어있던 Rt 가, rcView 로 전달하는 리스트(rtOnDiskList) 에서 몇번째 포지션에 있는지 'FileName' 으로 검색 후
            // => 해당 위치로 smooth Scroll..
            CoroutineScope(IO).launch {
                val positionInTheList = getPositionOfCurrentRt(rtOnDiskList)
                withContext(Main) {
                    Log.d(TAG, "onCreate: smoothScroll to Pos=$positionInTheList")
                    layoutManager.scrollToPositionWithOffset(positionInTheList, 60)
                }
            }
        }else {
            Toast.makeText(this, "Error locating ringtone paths.",Toast.LENGTH_SHORT).show()
        }

    }
// <0> My Utility Methods
    private suspend fun getPositionOfCurrentRt(rtOnDiskList: MutableList<RtOnThePhone>): Int {
        // 현재 DetailsFrag 에서 설정되어있는 RT 의 '파일 이름'
        val rtFileName = AlarmDetailsFragment.detailFragDisplayedRtFileName
        val index  =  rtOnDiskList.indexOfFirst { rt -> rt.fileNameWithExt == rtFileName } // 동일한 'FileName'을 갖는 놈의 인덱스를 리스트 에서 찾기
        Log.d(TAG, "getPositionOfCurrentRt: returning index=$index")
        return index
    }


// <1> SlidingPanel 세팅 (펼치기 접기) 관련
private fun setUpSlidingPanel() {

    slidingUpPanelLayout.setDragView(cl_upperUi_entireWindow) //setDragView = 펼치는 Drag 가능 영역 지정
    //감춰놓기.
    slidingUpPanelLayout.panelState =SlidingUpPanelLayout.PanelState.HIDDEN // 일단 클릭전에는 감춰놓기!

    slidingUpPanelLayout.addPanelSlideListener(object :
        SlidingUpPanelLayout.PanelSlideListener {
        override fun onPanelSlide(panel: View?, slideOffset: Float) {

            //upperUiHolder.alpha =1 - slideOffset + 0.5f // +0.5 은 어느정도 보이게끔 // todo: 나중에는 그냥 invisible 하는게 더 좋을수도. 너무 주렁주렁

            // 트랙 클릭-> 미니플레이어가 등장! (그 이전에는 offset = -xxx 값임.)
            //Log.d(TAG, "onPanelSlide: slideOffset= $slideOffset, rcvAdapterInstance.itemCount=${rcvAdapterInstance.itemCount}")
            val entireListCount = rtPickerRcvAdapter.itemCount

            //rcView 살짝 위로 밀어주기 .. Mini Player 가 열리지 않은 상태에서 마지막 rt 를 선택했을 때
            if (slideOffset == 0.0f && rtPickerRcvAdapter.lastUserCheckedPos == rtPickerRcvAdapter.itemCount-1) {
                rcView.post { // 메인 ui 스레드에서는 다른 업무 처리로 바뻐서 다른 thread (워커스레드?) 를 만들어줌.
                    rcView.smoothScrollBy(0, 300) //제일 밑 트랙을 300dp 위로 밀어줌.

                    // todo: 추후 rcView 사이즈 변경될 때 고려 ->정확한 calculation 필요  https://greedy0110.tistory.com/41
                    Log.d(TAG, "myOnItemClick: 살짝 슬라이드! 마지막 트랙 보이게!")

                }
            }
            // 완전히 펼쳐질 때
            if (!slidingUpPanelLayout.isOverlayed && slideOffset > 0.2f) { //안겹치게 설정된 상태에서 panel 이 열리는 중 (20%만 열리면 바로 모퉁이 감추기!)
                //Log.d(TAG, "onPanelSlide: Hiding 모퉁이! yo! ")
                slidingUpPanelLayout.isOverlayed =true // 모퉁이 edge 없애기 위해. Default 는 안 겹치게 false 값.
            }

        }

        @SuppressLint("ClickableViewAccessibility") // 아래 constLayout_entire.setxx... 이거 장애인 warning 없애기
        override fun onPanelStateChanged(panel: View?,previousState: SlidingUpPanelLayout.PanelState?,newState: SlidingUpPanelLayout.PanelState?) {

            when (newState) {
                SlidingUpPanelLayout.PanelState.EXPANDED -> {
                    //Log.d(TAG, "onPanelStateChanged: Sliding Panel Expanded")
                    iv_upperUi_ClickArrow.setImageResource(R.drawable.arrow_down_white)// ↓ arrow 전환 visibility }

                    // 계속 click 이 투과되는 문제(뒤에 recyclerView 의 버튼 클릭을 함)를 다음과같이 해결. 위에 나온 lowerUi 의 constraint layout 에 touch를 허용.
                    constLayout_entire.setOnTouchListener { _, _ -> true }

                }
                SlidingUpPanelLayout.PanelState.COLLAPSED -> {
                    //Log.d(TAG, "onPanelStateChanged: Sliding Panel Collapsed")
                    iv_upperUi_ClickArrow.setImageResource(R.drawable.arrow_up_white)// ↑ arrow 전환 visibility }
                    slidingUpPanelLayout.isOverlayed =false // 이렇게해야 rcView contents 와 안겹침 = (마지막 칸)이 자동으로 panel 위로 올라가서 보임.
                    }

                else -> {}
            }
            }
        })
    }
    // Sliding Panel 닫기
    private fun collapseSlidingPanel() {
        slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        iv_upperUi_ClickArrow.setImageResource(R.drawable.clickarrow)// ↑ arrow 전환 visibility }
        slidingUpPanelLayout.isOverlayed = false //

    }
// <2> MINI Player 재생/UI 관련 ================>

    //MiniPlayer Play/Pause btn UI Update
    // Show Pause Btn
    private fun showMiniPlayerPauseBtn() {
        Log.d(TAG, "showMiniPlayerPauseBtn: show Pause Btn")
        imgbtn_Play.visibility = View.GONE
        imgbtn_Pause.visibility = View.VISIBLE
    }
    // Show Play btn
    private fun showMiniPlayerPlayBtn() {
        Log.d(TAG, "showMiniPlayerPlayBtn: show Play Btn")
        imgbtn_Play.visibility = View.VISIBLE
        imgbtn_Pause.visibility = View.GONE
    }
    // Pause 상태에서 ▶  클릭했을 때
    private fun onMiniPlayerPlayClicked()  {
        if(ExoForLocal.currentPlayStatus == StatusMp.PAUSED) {
            exoForLocal.continueMusic()
            showMiniPlayerPauseBtn()
        }
    }
    //  Play 상태에서 ⏸ 클릭 했을 때 -> 음악 Pause 해야함.
    private fun onMiniPlayerPauseClicked() {
        if(ExoForLocal.currentPlayStatus == StatusMp.PLAY) {
            exoForLocal.pauseMusic()
            showMiniPlayerPlayBtn()
        }
    }
    //SeekBarListener (유저가 seekbar 를 만졌을 때 반응하는것.)
    private fun seekbarListenerSetUp(){

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean)
            {
                exoForLocal.removeHandler() // 새로 추가함.
                var progressLong = progress.toLong()
                if(fromUser) exoForLocal.onSeekBarTouchedYo(progressLong)

            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    // Badge 보여주기
    private fun showOrHideBadges(badgeStrList: List<String>?) {
        // 일단 다 gone 으로 꺼주고 시작 (안 그러면 RtPicker 갔다왔을 떄 기존에 켜진놈이 안 꺼지니께..)
        // 혹시 이렇게 꺼지는게 눈에 안 좋아보이면 위에서 RtPicker Activity 갈때 꺼줘도 됨..
        iv_badge1_Intense.visibility = View.GONE
        iv_badge2_Gentle.visibility = View.GONE
        iv_badge3_Nature.visibility = View.GONE
        iv_badge4_Location.visibility = View.GONE
        iv_badge5_Popular.visibility = View.GONE
        iv_badge6_Misc.visibility = View.GONE
        // String List 에서 이제 글자따라 다시 visible 시켜주기!
        Log.d(TAG, "showOrHideBadges: badgeStrList=$badgeStrList")
        if (badgeStrList != null) {
            for(i in badgeStrList.indices) {
                when(badgeStrList[i]) {
                    "INT" -> iv_badge1_Intense.visibility = View.VISIBLE
                    "GEN" -> iv_badge2_Gentle.visibility = View.VISIBLE
                    "NAT" -> iv_badge3_Nature.visibility = View.VISIBLE
                    "LOC" -> iv_badge4_Location.visibility = View.VISIBLE
                    "POP" -> iv_badge5_Popular.visibility = View.VISIBLE
                    "MIS" -> iv_badge6_Misc.visibility = View.VISIBLE
                }
            }
        }
        Log.d(TAG, "showOrHideBadges: done..")
    }


// BackButton 눌러서 원래 DetailsFrag 로 돌아가면 아래 onPause() & onDestroy() 둘다 불림.
    override fun onPause() {
        Log.d(TAG, "onPause: called")
        super.onPause()
    //1) 현재 음악이 재생중이든 아니든 (재생중이 아니었으면 어차피 pauseMusic() 은 의미가 없음)
        exoForLocal.pauseMusic() // a)일단 PAUSE 때리고
        exoForLocal.removeHandler() // b)handler 없애기
    // 최소한 여기서 재생중이던 음악 재생 pause, 아이콘 변경만?
    }
    override fun onDestroy() {
        Log.d(TAG, "onDestroy: called")
        super.onDestroy()
        exoForLocal.removeHandler()
        exoForLocal.releaseExoPlayer() // 여기서 EXO LiveData 를 IDLE 로 해주는데. 차라리 강제로 모든 LiveData observe 종료시키는것도 방법일듯?
        rtPickerRcvAdapter.initVariables()
        System.gc() // todo: 이거 별 역할 못 하는듯..
    }
}