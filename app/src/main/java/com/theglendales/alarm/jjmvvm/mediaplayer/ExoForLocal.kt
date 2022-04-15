package com.theglendales.alarm.jjmvvm.mediaplayer

import android.content.Context
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.webkit.URLUtil
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.GlbVars
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import java.io.IOException
import java.lang.Exception
import java.util.*

private const val TAG="ExoForLocal"

// [RtPicker 에서 Local 재생용도로 쓰임]

class ExoForLocal(val context: Context) : Player.Listener {

    private val toastMessenger: ToastMessenger by globalInject() //ToastMessenger


    companion object {
        val mp3UrlMap: HashMap<Int, String> = HashMap()
    //Current Status 모니터링
        var currentPlayStatus: StatusMp = StatusMp.IDLE // 현재 상태 저장. RtPickerAdapter 에서 쓰임.
    // 다른 fragment 갔다 왔을 떄 대비해서 currentSongPosition(INT), clickedTrackID(INT) 등이 필요함.
    }

//A) LiveData 관련 (JjMainVModel 에서 getXX() 로 등록 <- SecondFrag 에서 Observe 중)
    //1) StatusMp ENUM 클래스 정보 갖고 있음.
    private val _mpStatus = MutableLiveData<StatusMp>() // Private & Mutable
    val mpStatus: LiveData<StatusMp> = _mpStatus


    //2-A) 재생할 곡 길이 (exoPlayer.duration)
    private val _songDuration = MutableLiveData<Long>()
    val songDuration: LiveData<Long> = _songDuration
    //2-B) 재생중인 포지션 (exoPlayer.currentPosition(Long))
    private val _currentPosition = MutableLiveData<Long>()
    val currentPosition: LiveData<Long> = _currentPosition


    //B) ExoPlayer
    //1-a) Exo Player Related
    private lateinit var exoPlayer: SimpleExoPlayer
    private var playbackPosition:Long = 0
    //private val dataSourceFactory: DataSource.Factory by lazy { DefaultDataSourceFactory(receivedContext,"exoplayer-sample") } //Store the source of Media
    //1-b) Exo Caching Related
    private lateinit var httpDataSourceFactory: HttpDataSource.Factory
    private lateinit var cacheDataSourceFactory: DataSource.Factory
    private lateinit var dataSourceFactory: DataSource.Factory  //Store the source of Media
    private lateinit var simpleCacheReceived: SimpleCache

//B) SeekBar Related
    private val handler = android.os.Handler(Looper.getMainLooper())
    private var runnable = kotlinx.coroutines.Runnable {}// null 되지 않기 위해서 여기서 빈값으로 initialize 해줌.

    /*init {
        initExoForLocalPlay()
    }*/


    fun initExoForLocalPlay() { // MyCacher.kt (url) 와 RtPickerActivity 두군데서 호출됨. == SecondFrag.kt 에서 사용될때는 Caching 되는걸로 사용!
    // B) LOCAL URI 재생 용도 (RtPicker 에서 사용)
        Log.d(TAG, "initExoPlayer: [LOCAL] .. begins")
        exoPlayer = SimpleExoPlayer.Builder(context).build()
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
        exoPlayer.addListener(this)
        Log.d(TAG, "initExoPlayer: [LOCAL] .. Ends")

    }
// ***** 실제 재생!
    // 아래 prepMusicPlayLocal & Online 두군데서 불림.
    private fun prepPlayer(isCachingNeeded: Boolean, sourceLocation:String?, playWhenReady: Boolean) { // Caching & Local Play
    //1) SecondFrag.kt 에서 URL 재생 용도: to play a single song
        if(isCachingNeeded) {

            val mp3Uri = Uri.parse(sourceLocation)
            val mediaItem = MediaItem.fromUri(mp3Uri)

            // Build data source factory with cache enabled, if data is available in cache it will return immediately, otherwise it will open a new connection to get the data.
            val mediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(mediaItem)

            //playerView.player = simpleExoPlayer
            exoPlayer.setMediaSource(mediaSource, true)
            exoPlayer.prepare()

            exoPlayer.playWhenReady = playWhenReady
        }
    //2) DetailsFrag>RtPicker 들어가서 Local URI 로 음악 들으려 할 때
        else {
            val rtaFileUri = Uri.parse(sourceLocation)
            val mediaItem = MediaItem.fromUri(rtaFileUri)
            val mediaSource = ProgressiveMediaSource.Factory(DefaultDataSourceFactory(context)).createMediaSource(mediaItem)

            exoPlayer.setMediaSource(mediaSource, true)
            exoPlayer.prepare()

            exoPlayer.playWhenReady = playWhenReady

        }


    }
    fun releaseExoPlayer() { //todo: 후에 activity ? fragment? onDestroy 에 넣어야 할듯..
        playbackPosition = exoPlayer.currentPosition
        onExoIdle() // 현재의 상태를 IDLE 로 강제 집행 (RtPickerAdapter 에 재진입했을 때 EQ(vumeter) 안 보이게 하기 위해)
        _songDuration.value = 0L
        _currentPosition.value = 0L
        exoPlayer.release()
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        // handle error
        when(error.type) {
            ExoPlaybackException.TYPE_SOURCE -> Log.d(TAG,"!!!TYPE_SOURCE: " + error.sourceException)
            ExoPlaybackException.TYPE_RENDERER -> Log.d(TAG,"!!TYPE_RENDERER: " + error.rendererException)
            ExoPlaybackException.TYPE_UNEXPECTED -> Log.d(TAG,"!!TYPE_UNEXPECTED: " + error.unexpectedException)
            ExoPlaybackException.TYPE_REMOTE -> Log.d(TAG,"!!TYPE_REMOTE: " + error.message)

        }

    }
    // 여기서 playbackState 에 따라 onExoXXX() 로 전달 -> JjMpViewModel.kt 로 LiveData 전달-> SecondFrag -> VHolderUiHandler 에서 Ui 업데이트.
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if (playbackState == Player.STATE_BUFFERING) {

            Log.d(TAG, "onPlayerStateChanged: Playback state=Player.STATE_BUFFERING. PlayWhenReady=$playWhenReady")
        // 신규추가!
            onExoBuffering()

        }
        else if (playbackState == Player.STATE_READY) {  // 준비 완료! 기존 mp 의 setOnPreparedListener{} 내용이 여기로 왔음.
            Log.d(TAG, "onPlayerStateChanged: Playback state=Player.STATE_READY. PlayWhenReady=$playWhenReady")
            onExoReady()

            if(playWhenReady) { // PLAYING! (or resume playing)
                feedLiveDataSongDuration()
                feedLiveDataCurrentPosition()

                Log.d(TAG, "Finally Playing! Global.currentPlayingTrNo: ${GlbVars.currentPlayingTrId}")
                onExoPlaying()

            }
            else { // PAUSED! (playWhenReady=False 상태)
                Log.d(TAG, "onPlayerStateChanged: PAUSED.. Playback state=Player.STATE_READY. PlayWhenReady=$playWhenReady")
                onExoPaused()
            }
        }else if (playbackState == Player.STATE_ENDED) {Log.d(TAG, "onPlayerStateChanged: Playback state=Player.STATE_ENDED(4).  Int=$playbackState")}
        else {
            Log.d(TAG, "onPlayerStateChanged: State=IDLE(1번)-> 재생할 Media 없음 int= $playbackState")
                onExoError()
            }
    }


// <----------<1>기존 코드들 ExoPlayer Related
// ----------><2>기존 코드들 Utility


// <3> 추가된 코드들--LiveData/Ui 외-------------- >>>>>>>>>

    //a) Local URI 리소스를 가지는 음원 플레이 (RtPicker.kt) 에서 사용
    fun prepMusicPlayLocalSrc(audioFilePath: String?, playWhenReady: Boolean) {
        removeHandler()
        setSeekbarToZero()

        try{
            // LOCAL 재생 용도이기 때문에 Caching=false
            prepPlayer(false, audioFilePath, playWhenReady) // -> playWhenReady = true
        }catch(e: IOException) {
            toastMessenger.showMyToast("Unknown error occurred while trying to play the ringtone: $e",isShort = false)
            updateSongDuration(0)
            //GlbVars.errorTrackId = receivedTrId
            // 위에서 이미 url 에러 -> return 으로 잡아줬지만 혹시 모르니..
        }
    }


// Seekbar related-- >
    fun removeHandler() = handler.removeCallbacks(runnable)
    fun setSeekbarToZero() { // a)새로운 트랙 클릭했을 때 prepareMusicPlay() 에서 실행. b) 2ndFrag 에서 onPause() 에서도 실행됨.
    // 기존 진행되던 seekbar reset 하기-->
    updateCurrentPosition(0) // 기존 song position 의 위치가 있었을테니 0 으로 reset
    // 기존 진행되던 seekbar reset 하기<--
    }
    fun setSeekbarToPrevPosition(prevPlaybackPos: Long) {
        updateCurrentPosition(prevPlaybackPos)
    }
    private fun feedLiveDataCurrentPosition() {

        runnable = kotlinx.coroutines.Runnable {
            try {
                Log.d(TAG, "feedLiveDataCurrentPosition: runnable working")
                updateCurrentPosition(exoPlayer.currentPosition) //livedata 로 feed 를 보냄
                handler.postDelayed(runnable,1000) // 1초에 한번씩
            }catch (e: Exception) {
                updateCurrentPosition(0) // 문제 생기면 그냥 '0' 전달.
                Log.d(TAG, "feedLiveDataCurrentPosition: XXX issue !! Occurred exoPlayer.currentPos=${exoPlayer.currentPosition}")

            }
        }
        handler.postDelayed(runnable, 1000) // 최초 실행? 무조건 한번은 실행해줘야함.

    }
    private fun feedLiveDataSongDuration() {
        if(exoPlayer.duration > 0) {
            updateSongDuration(exoPlayer.duration)
        }
    }
    // SecondFrag.kt 에서 SeekBar 를 user 가 만졌을 때 exoPlayer 에게전달
    fun onSeekBarTouchedYo(progress: Long) = exoPlayer.seekTo(progress)

// called from MiniPlayer button (play/pause)
    fun continueMusic() {

        if(!exoPlayer.isPlaying) { // 트랙 재생중 pause 했다 continue 할 때
            exoPlayer.play()
            exoPlayer.playWhenReady = true
        }
    }
    fun pauseMusic() {

        if(exoPlayer.isPlaying) {
            removeHandler()
            exoPlayer.pause()
            exoPlayer.playWhenReady = false
            // onExoPaused() < - 이건 자동으로 ExoPlayStatusListener 에서 SecondFrag 로 livedata 로 전달해줌.
        }

    }
    private fun onExoIdle() {
        currentPlayStatus = StatusMp.IDLE
        updateStatusMpLiveData(StatusMp.IDLE)
    }
    private fun onExoBuffering() {
        currentPlayStatus = StatusMp.BUFFERING
        updateStatusMpLiveData(StatusMp.BUFFERING)}
    private fun onExoReady() {
        currentPlayStatus = StatusMp.READY
        updateStatusMpLiveData(StatusMp.READY)}
    private fun onExoPlaying() {
        currentPlayStatus = StatusMp.PLAY
        updateStatusMpLiveData(StatusMp.PLAY)}
    private fun onExoPaused() {
        currentPlayStatus = StatusMp.PAUSED
        updateStatusMpLiveData(StatusMp.PAUSED)}
    private fun onExoError() {
        currentPlayStatus = StatusMp.ERROR
        updateStatusMpLiveData(StatusMp.ERROR)}


    // 가) StatusMP ENUM=> 아래는 ExoForLocal 에서 전달받음. 그 후 전달받은 Status 는 _mpStatus 로 옮겨지고 SecondFrag 의 jjMpViewModel 이 이것을 Observe 하고 있음
    private fun updateStatusMpLiveData(statusReceived: StatusMp) {
        Log.d(TAG, "updateStatusMpLiveData: called")
        _mpStatus.value = statusReceived
    }
    //나-A) 재생할 곡 길이 업데이트 (exoPlayer.duration)
    private fun updateSongDuration(durationReceived: Long) {
        Log.d(TAG, "updateSongDuration: called")
        _songDuration.value = durationReceived

    }
    //나-B) 재생중인 포지션 업데이트 (exoPlayer.currentPosition(Long))
    private fun updateCurrentPosition(positionReceived: Long) {
        Log.d(TAG, "updatedCurrentPosition: positionReceived= $positionReceived")
        _currentPosition.value = positionReceived
    }
}