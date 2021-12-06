package com.theglendales.alarm.presenter

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.theglendales.alarm.R
import com.theglendales.alarm.jjadapters.GlideApp
import com.theglendales.alarm.jjadapters.RtPickerAdapter
import com.theglendales.alarm.jjdata.GlbVars
import com.theglendales.alarm.jjmvvm.JjMpViewModel
import com.theglendales.alarm.jjmvvm.JjRtPickerVModel
import com.theglendales.alarm.jjmvvm.helper.BadgeSortHelper
import com.theglendales.alarm.jjmvvm.mediaplayer.MyMediaPlayer
import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjmvvm.util.RtWithAlbumArt
import java.util.ArrayList

// startActivityForResult 참고: https://youtu.be/AD5qt7xoUU8


private const val TAG="RtPickerActivity"

private const val PICKER_RESULT_RT_TITLE="RtTitle"
private const val PICKER_RESULT_AUDIO_PATH="AudioPath"
private const val PICKER_RESULT_ART_PATH="ArtPath"


class RtPickerActivity : AppCompatActivity() {

    //RcView Related
    lateinit var rcvAdapter: RtPickerAdapter
    lateinit var rcView: RecyclerView
    lateinit var layoutManager: LinearLayoutManager

    //Media Player
    lateinit var mediaPlayer: MyMediaPlayer
    //SlidingUp Panel (AKA mini Player) UIs
    lateinit var slidingUpPanelLayout: SlidingUpPanelLayout // SlideUpPanel 전체
    lateinit var allBtmSlideLayout: RelativeLayout // SlideUpPanel 중 상단(돌출부) 전체

    //SlidingUp Panel [음악재생 mini player]
        // a) 상단 Uis
        private val imgbtn_Play by lazy { allBtmSlideLayout.findViewById(R.id.id_imgbtn_upperUi_play) as ImageButton }
        private val imgbtn_Pause by lazy { allBtmSlideLayout.findViewById(R.id.id_imgbtn_upperUi_pause) as ImageButton }
        private val seekBar by lazy { allBtmSlideLayout.findViewById(R.id.id_upperUi_Seekbar) as SeekBar }

        private val upperUiHolder by lazy { allBtmSlideLayout.findViewById(R.id.id_upperUi_ll) as LinearLayout }    // 추후 이 부분이 fade out
        private val tv_upperUi_title by lazy { allBtmSlideLayout.findViewById(R.id.id_upperUi_tv_title) as TextView }

        private val iv_upperUi_thumbNail by lazy { allBtmSlideLayout.findViewById(R.id.id_upperUi_iv_coverImage) as ImageView}
        private val iv_upperUi_ClickArrow by lazy { allBtmSlideLayout.findViewById(R.id.id_upperUi_iv_clickarrowUp) as ImageView }
        private val cl_upperUi_entireWindow by lazy { allBtmSlideLayout.findViewById(R.id.id_upperUi_ConsLayout) as ConstraintLayout }
        // b) 하단 Uis
        private val constLayout_entire by lazy {allBtmSlideLayout.findViewById(R.id.id_lowerUI_entireConsLayout) as ConstraintLayout}
        private val iv_lowerUi_bigThumbnail by lazy { allBtmSlideLayout.findViewById(R.id.id_lowerUi_iv_bigThumbnail) as ImageView }
        private val tv_lowerUi_about by lazy { allBtmSlideLayout.findViewById(R.id.id_lowerUi_tv_Description) as TextView }
        // c) Badges
        private val iv_badge1_Intense by lazy {allBtmSlideLayout.findViewById(R.id.mPlayer_badge1_Intense) as ImageView}
        private val iv_badge2_Gentle by lazy {allBtmSlideLayout.findViewById(R.id.mPlayer_badge2_Gentle) as ImageView}
        private val iv_badge3_Nature by lazy {allBtmSlideLayout.findViewById(R.id.mPlayer_badge3_Nature) as ImageView}
        private val iv_badge4_Human by lazy {allBtmSlideLayout.findViewById(R.id.mPlayer_badge_4_History) as ImageView}



    override fun onCreate(savedInstanceState: Bundle?) {
        val resultIntent = Intent()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rt_picker)

    //1) Activity 화면 Initialize (ActionBar 등..)
        // 화면 위에 뜨는 타이틀
            setTitle("Ringtone Picker")
        // todo: actionBar 꾸미기. 현재 사용중인 actionBar 스타일로 하려면  AlarmListActivity - mActionBarHandler 등 참고. DetailsFrag 는 또 다름 (쓰레기통 표시)
            supportActionBar?.setDisplayHomeAsUpEnabled(true) // null check?

    // 2) SlidingUpPanel (AKA MiniPlayer) UI Initialize 및 onClickListener 장착

        //a) 전체 SlidingUpPanel
        slidingUpPanelLayout = findViewById(R.id.id_sldUpPnlRtPickerActivity) // 전체 SlidingUpPanel
        allBtmSlideLayout = findViewById(R.id.ir_rl_entireSlider) // SlidingUpPanel 중 상단(돌출부) 전체
        setUpSlidingPanel()

        //b) ListenerSetup: Play & Pause Button onClickListener

        imgbtn_Play.setOnClickListener {onMiniPlayerPlayClicked()}
        imgbtn_Pause.setOnClickListener {onMiniPlayerPauseClicked()}

        seekbarListenerSetUp()


    //3) RcView 셋업-->
        rcView = findViewById<RecyclerView>(R.id.rcV_RtPicker)
        layoutManager = LinearLayoutManager(this)
        rcView.layoutManager = layoutManager


        
    //4)  LIVEDATA -> // 참고로 별도로 Release 해줄 필요 없음. if you are using observe method, LiveData will be automatically cleared in onDestroy state.
        //(1) RtPicker ViewModel
            // A)생성(RcvVModel)
            val rtPickerVModel = ViewModelProvider(this).get(JjRtPickerVModel::class.java)

            // B) Observe - RtPicker 로 User 가 RingTone 을 골랐을 때

            rtPickerVModel.selectedRow.observe(this, { rtWithAlbumArt->
                Log.d(TAG, "onCreate: rtPickerVModel 옵저버!! rtTitle=${rtWithAlbumArt.rtTitle}, \n rtaPath= ${rtWithAlbumArt.audioFilePath}, artPath= ${rtWithAlbumArt.artFilePathStr}")
            //B-1) Intent 에 현재 선택된 RT 의 정보담기  (AlarDetailsFrag.kt 로 연결됨) .. RT 계속 바꿀때마다 Intent.putExtra 하는데 overWrite 되는듯.
                resultIntent.putExtra(PICKER_RESULT_RT_TITLE,rtWithAlbumArt.rtTitle)
                resultIntent.putExtra(PICKER_RESULT_AUDIO_PATH,rtWithAlbumArt.audioFilePath)
                resultIntent.putExtra(PICKER_RESULT_ART_PATH,rtWithAlbumArt.artFilePathStr)
                setResult(RESULT_OK, resultIntent)
            //B-2) 음악 Player 에 UI 업데이트
                //B-2-a) Sliding Panel 전체
                // 최초 SlidingPanel 이 HIDDEN(안보이는 상태)면 열어주기.
                if (slidingUpPanelLayout.panelState == SlidingUpPanelLayout.PanelState.HIDDEN) {
                    slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED } // 이미 EXPAND/Collapsed 상태로 보이면 Panel 은 그냥 둠 [.COLLAPSED = (위만) 보이는 상태임!]

                //B-2-b) Sliding Panel - Upper UI
                    //제목
                tv_upperUi_title.text = rtWithAlbumArt.rtTitle // miniPlayer(=Upper Ui) 의 Ringtone Title 변경
                tv_upperUi_title.append("                                                 ") // 흐르는 text 위해서. todo: 추후에는 글자 크기 계산-> 정확히 공백 더하기
                    //AlbumCover
                val glideBuilder = GlideApp.with(this).load(rtWithAlbumArt.artFilePathStr).centerCrop()
                    .error(R.drawable.errordisplay).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).placeholder(R.drawable.placeholder)

                glideBuilder.into(iv_upperUi_thumbNail)
                glideBuilder.into(iv_lowerUi_bigThumbnail)

                //B-2-c) Sliding Panel -  Lower UI
                tv_lowerUi_about.text = rtWithAlbumArt.rtDescription // Rt 설명
                iv_lowerUi_bigThumbnail.setImageDrawable(iv_upperUi_thumbNail.drawable) // AlbumArt 현재 상단 UI 앨범아트 고대로 갖고와서 설정.
                //Badges
                val badgeStrRaw = rtWithAlbumArt.badgeStr // ex. "I,N,H" -> Intense, Nature, History 뭔 이런식.
                val badgeStrList = BadgeSortHelper.getBadgesListFromStr(badgeStrRaw)
                showOrHideBadges(badgeStrList)

            })
    //(2) MediaPlayer ViewModel - 기존 SecondFrag 에서 사용했던 'JjMpViewModel' & MyMediaPlayer 그대로 사용 예정.
        // (음악 재생 상태에 따른 플레이어 UI 업데이트) (RT 선택시 음악 재생은 RtPickerAdapter 에서 바로함.)
            //A) 생성
            val jjMpViewModel = ViewModelProvider(this).get(JjMpViewModel::class.java)
            //B) Observe
                //B-1) MediaPlayer 에서의 Play 상태(loading/play/pause) 업뎃을 observe
                jjMpViewModel.mpStatus.observe(this, { StatusEnum ->
                    Log.d(TAG, "onViewCreated: !!! 'MpViewModel' 옵저버! Current Music Play Status: $StatusEnum")
                    // a) MiniPlayer Play() Pause UI 업데이트 (현재 SecondFragment.kt 에서 해결)
                    when(StatusEnum) {
                        StatusMp.PLAY -> {showMiniPlayerPauseBtn()} // 최초의 ▶,⏸ 아이콘 변경을 위하여 사용. 그후에는 해당버튼 Click -> showMiniPlayerPause/Play 실행됨.
                        StatusMp.BUFFERING -> {showMiniPlayerPlayBtn()}
                        StatusMp.ERROR -> {showMiniPlayerPlayBtn()}
                        }
                        //todo: b) VuMeter/Loading Circle 등 UI 컨트롤
                    })

                    //VHolderUiHandler.LcVmIvController(StatusEnum)

                //B-2) Seekbar 관련
                    //2-C) seekbar 업뎃을 위한 현재 곡의 길이(.duration) observe. (MyMediaPlayer -> JjMpViewModel-> 여기로)
                    jjMpViewModel.songDuration.observe(this, { dur ->
                        Log.d(TAG, "onViewCreated: duration received = ${dur.toInt()}")
                        seekBar.max = dur.toInt()

                    })
                    //2-D) seekbar 업뎃을 위한 현재 곡의 길이(.duration) observe. (MyMediaPlayer -> JjMpViewModel-> 여기로)
                    jjMpViewModel.currentPosition.observe(this, { playbackPos ->
                        seekBar.progress = playbackPos.toInt() +200
                        Log.d(TAG, "onCreate: playbackPos=$playbackPos")
                    })



    // 5) Media Player Init
        mediaPlayer = MyMediaPlayer(this, jjMpViewModel)
        mediaPlayer.initExoPlayer(false) // 우리는 Local RTAs 의 URI 를 받아서 재생할것이므로 Caching 사용 안함(=>False 전달)

    //6) RcVAdapter Init
        rcvAdapter = RtPickerAdapter(ArrayList(), this, rtPickerVModel, mediaPlayer)
        rcView.adapter = rcvAdapter
        rcView.setHasFixedSize(true)

    //7) RcVAdapter 에 보여줄 List<RtWithAlbumArt> 를 제공 (이미 DiskSearcher 에 로딩되어있으니 특별히 기다릴 필요 없지..)
        val rtOnDiskList:  MutableList<RtWithAlbumArt> = DiskSearcher.finalRtArtPathList
        rcvAdapter.updateRcV(rtOnDiskList)




    // RT 고르기(X) Cancel Btn 눌렀을 때

    }

// <1> SlidingPanel 세팅 (펼치기 접기) 관련
private fun setUpSlidingPanel() {

    slidingUpPanelLayout.setDragView(cl_upperUi_entireWindow) //setDragView = 펼치는 Drag 가능 영역 지정
    //감춰놓기.
    slidingUpPanelLayout.panelState =SlidingUpPanelLayout.PanelState.HIDDEN // 일단 클릭전에는 감춰놓기!

    slidingUpPanelLayout.addPanelSlideListener(object :
        SlidingUpPanelLayout.PanelSlideListener {
        override fun onPanelSlide(panel: View?, slideOffset: Float) {

            upperUiHolder.alpha =1 - slideOffset + 0.5f // +0.5 은 어느정도 보이게끔 // todo: 나중에는 그냥 invisible 하는게 더 좋을수도. 너무 주렁주렁

            // 트랙 클릭-> 미니플레이어가 등장! (그 이전에는 offset = -xxx 값임.)
            //Log.d(TAG, "onPanelSlide: slideOffset= $slideOffset, rcvAdapterInstance.itemCount=${rcvAdapterInstance.itemCount}")
            val entireListCount = rcvAdapter.itemCount
            if (slideOffset == 0.0f && GlbVars.clickedTrId == entireListCount) { //마지막 트랙 클릭.
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
                    iv_upperUi_ClickArrow.setImageResource(R.drawable.clickarrow_down)// ↓ arrow 전환 visibility }

                    // 계속 click 이 투과되는 문제(뒤에 recyclerView 의 버튼 클릭을 함)를 다음과같이 해결. 위에 나온 lowerUi 의 constraint layout 에 touch를 허용.
                    constLayout_entire.setOnTouchListener { _, _ -> true }

                }
                SlidingUpPanelLayout.PanelState.COLLAPSED -> {
                    //Log.d(TAG, "onPanelStateChanged: Sliding Panel Collapsed")
                    iv_upperUi_ClickArrow.setImageResource(R.drawable.clickarrow)// ↑ arrow 전환 visibility }
                    slidingUpPanelLayout.isOverlayed =false // 이렇게해야 rcView contents 와 안겹침 = (마지막 칸)이 자동으로 panel 위로 올라가서 보임.
                    }
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
        if(MyMediaPlayer.currentPlayStatus == StatusMp.PAUSED) {
            mediaPlayer.continueMusic()
            showMiniPlayerPauseBtn()
        }
    }
    //  Play 상태에서 ⏸ 클릭 했을 때 -> 음악 Pause 해야함.
    private fun onMiniPlayerPauseClicked() {
        if(MyMediaPlayer.currentPlayStatus == StatusMp.PLAY) {
            mediaPlayer.pauseMusic()
            showMiniPlayerPlayBtn()
        }
    }
    //SeekBarListener (유저가 seekbar 를 만졌을 때 반응하는것.)
    private fun seekbarListenerSetUp(){

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean)
            {
                mediaPlayer.removeHandler() // 새로 추가함.
                var progressLong = progress.toLong()
                if(fromUser) mediaPlayer.onSeekBarTouchedYo(progressLong)

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
        iv_badge4_Human.visibility = View.GONE
        // String List 에서 이제 글자따라 다시 visible 시켜주기!
        Log.d(TAG, "showOrHideBadges: badgeStrList=$badgeStrList")
        if (badgeStrList != null) {
            for(i in badgeStrList.indices) {
                when(badgeStrList[i]) {
                    "I" -> iv_badge1_Intense.visibility = View.VISIBLE
                    "G" -> iv_badge2_Gentle.visibility = View.VISIBLE
                    "N" -> iv_badge3_Nature.visibility = View.VISIBLE
                    "H" -> iv_badge4_Human.visibility = View.VISIBLE
                }
            }
        }
        Log.d(TAG, "showOrHideBadges: done..")
    }


// BackButton 눌러서 원래 DetailsFrag 로 돌아가면 아래 onPause() & onDestroy() 둘다 불림.
    override fun onPause() {super.onPause()}
    override fun onDestroy() {
        super.onDestroy()
        //todo: ExoPlayer 아예 없애주기! (release 말고 destroy? 그래야 MyCacher.kt 에서-> mediaPlayer.initExoPlayer(캐슁버전) -> Caching 준비하여 SecondFrag.kt 에서 사용)
        mediaPlayer.removeHandler()
        mediaPlayer.releaseExoPlayer()
    }
}