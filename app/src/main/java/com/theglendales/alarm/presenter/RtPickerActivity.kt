package com.theglendales.alarm.presenter

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.theglendales.alarm.R
import com.theglendales.alarm.jjadapters.RtPickerAdapter
import com.theglendales.alarm.jjmvvm.JjMpViewModel
import com.theglendales.alarm.jjmvvm.JjRtPickerVModel
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
        //a) 전체 SlidingUpPanel & 상단(돌출부) 전체 Layout
        slidingUpPanelLayout = findViewById(R.id.id_sldUpPnlRtPickerActivity) // 전체 SlidingUpPanel
        allBtmSlideLayout = findViewById(R.id.ir_rl_entireSlider) // SlidingUpPanel 중 상단(돌출부) 전체

        //b) ListenerSetup: Play & Pause Button onClickListener

        imgbtn_Play.setOnClickListener {
            //onMiniPlayerPlayClicked()
            Log.d(TAG, "onCreate: Play Clicked")}
        imgbtn_Pause.setOnClickListener {
            //onMiniPlayerPauseClicked()
            Log.d(TAG, "onCreate: Pause Clicked")}

        // seekBarListenerSetUp()
        


    //3) RcView 셋업-->
        rcView = findViewById<RecyclerView>(R.id.rcV_RtPicker)
        layoutManager = LinearLayoutManager(this)
        rcView.layoutManager = layoutManager


        
    //4)  LIVEDATA -> // 참고로 별도로 Release 해줄 필요 없음. if you are using observe method, LiveData will be automatically cleared in onDestroy state.
        //(1) RtPicker ViewModel
            // A)생성(RcvVModel)
            val rtPickerVModel = ViewModelProvider(this).get(JjRtPickerVModel::class.java)

            // B) Observe - RtPicker 로 User 가 RingTone 을 골랐을 때 => Intent 에 현재 RT 로 설정
            rtPickerVModel.selectedRow.observe(this, { rtWithAlbumArt->
                Log.d(TAG, "onCreate: rtPickerVModel 옵저버!! rtTitle=${rtWithAlbumArt.rtTitle}, \n rtaPath= ${rtWithAlbumArt.audioFilePath}, artPath= ${rtWithAlbumArt.artFilePathStr}")
                // Intent 에 현재 선택된 RT 의 정보담기  (AlarDetailsFrag.kt 로 연결됨) .. RT 계속 바꿀때마다 Intent.putExtra 하는데 overWrite 되는듯.
                resultIntent.putExtra(PICKER_RESULT_RT_TITLE,rtWithAlbumArt.rtTitle)
                resultIntent.putExtra(PICKER_RESULT_AUDIO_PATH,rtWithAlbumArt.audioFilePath)
                resultIntent.putExtra(PICKER_RESULT_ART_PATH,rtWithAlbumArt.artFilePathStr)
                setResult(RESULT_OK, resultIntent)
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
                    // b) VuMeter/Loading Circle 등 UI 컨트롤
                    //VHolderUiHandler.LcVmIvController(StatusEnum)

                })

                //B-2) seekbar 업뎃을 위한 현재 곡의 길이(.duration) observe. (MyMediaPlayer -> JjMpViewModel-> 여기로)
                jjMpViewModel.songDuration.observe(this, { dur ->
                    Log.d(TAG, "onViewCreated: duration received = ${dur.toInt()}")
                    //seekBar.max = dur.toInt()
                    
                })
                //2-D) seekbar 업뎃을 위한 현재 곡의 길이(.duration) observe. (MyMediaPlayer -> JjMpViewModel-> 여기로)
                jjMpViewModel.currentPosition.observe(this, { playbackPos ->
                    //Log.d(TAG, "onViewCreated: playback Pos=${playbackPos.toInt()} ")
                    //seekBar.progress = playbackPos.toInt() +200
                    
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
    // ===================================== My Functions ==== >

    //MiniPlayer Play/Pause btn UI Update
    // Show Pause Btn
    private fun showMiniPlayerPauseBtn() {
        Log.d(TAG, "showMiniPlayerPauseBtn: show Pause Btn")
//        imgbtn_Play.visibility = View.GONE
//        imgbtn_Pause.visibility = View.VISIBLE
    }
    // Show Play btn
    private fun showMiniPlayerPlayBtn() {
        Log.d(TAG, "showMiniPlayerPlayBtn: show Play Btn")
//        imgbtn_Play.visibility = View.VISIBLE
//        imgbtn_Pause.visibility = View.GONE
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
    // BackButton 눌러서 원래 DetailsFrag 로 돌아가면 아래 onPause() & onDestroy() 둘다 불림.
    override fun onPause() {super.onPause()}
    override fun onDestroy() {
        super.onDestroy()
        //todo: ExoPlayer 아예 없애주기! (release 말고 destroy? 그래야 MyCacher.kt 에서-> mediaPlayer.initExoPlayer(캐슁버전) -> Caching 준비하여 SecondFrag.kt 에서 사용)
        mediaPlayer.removeHandler()
        mediaPlayer.releaseExoPlayer()
    }
}