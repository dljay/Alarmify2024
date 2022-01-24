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
import java.util.HashMap

private const val TAG="MyMediaPlayerV2"
enum class StatusMp { IDLE, BUFFERING, READY, PLAY, PAUSED, ERROR} // BUFFERING: activateLC(),

class MyMediaPlayerV2(val context: Context) : Player.Listener {
    companion object {
        val mp3UrlMap: HashMap<Int, String> = HashMap()
        //Current Status 모니터링
        var currentPlayStatus: StatusMp = StatusMp.IDLE
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

    //C) SeekBar Related
    private val handler = android.os.Handler(Looper.getMainLooper())
    private var runnable = kotlinx.coroutines.Runnable {}// null 되지 않기 위해서 여기서 빈값으로 initialize 해줌.

    //D) Util
    private val toastMessenger: ToastMessenger by globalInject() //ToastMessenger
////E) SharedPreference 저장 관련 (Koin  으로 대체!) -> 알람 fragment 갔다 왔을 때-> prepareMusicPlay() 에서 mySharedPref 를 통해 Pause 상태였던것을 확인함.
//    val mySharedPrefManager: MySharedPrefManager by globalInject()

    // <1>기존 코드들 ExoPlayer Related ---------->
    private fun loadControlSetUp(): LoadControl {
        //Minimum Video you want to buffer while Playing
        val MIN_BUFFER_MS = 1000 // (1)originally: 2000 // The default minimum duration of media that the player will attempt to ensure is buffered at all times= 최소한으로 늘 확보하고 있을 버퍼

        //Max Video you want to buffer during PlayBack
        val MAX_BUFFER_MS = 2500// (2)originally: 5000 // 최대한으로 확보하려고하는 버퍼

        //Min Video you want to buffer before start(or resume after pause) Playing it
        //The default duration of media that must be buffered for playback to start or resume following a user action such as a seek.
        val BUFFER_FOR_PLAYBACK_MS = 500 //(3) 원래는 1500 였음. 비디오 용인것 같지만..

        //Min video You want to buffer when user resumes video
        val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 100 // (4) originally: 2000

        val loadControl: LoadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, 16))
            .setBufferDurationsMs(
                MIN_BUFFER_MS,//'minBufferMS'(1) 'buffer for Playback MS'(3) 보다 커야 함.
                MAX_BUFFER_MS,//maxBufferMS(2) .. minBufferMs(1) 보다 커야 함.
                BUFFER_FOR_PLAYBACK_MS,//buffer for Playback MS
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS//buffer for Playback After rebuffer MS
            )
            .setTargetBufferBytes(-1)
            .setPrioritizeTimeOverSizeThresholds(true).createDefaultLoadControl()

        return loadControl
    }

    fun initExoPlayer(isCachingNeeded: Boolean) { // MyCacher.kt (url) 와 RtPickerActivity 두군데서 호출됨. == SecondFrag.kt 에서 사용될때는 Caching 되는걸로 사용!
        // A) SecondFrag 에서 URL 부르는 용도로 사용될때 (MyCacher.kt 에서 init 하면서 그전의 Caching 을 활용하라고 지시!)
        if(isCachingNeeded) {
            Log.d(TAG, "initExoPlayer: [Caching]starts......")
            val lcControl = loadControlSetUp()

            simpleCacheReceived = MyCacher.simpleCache!! // todo: this is dangerous..근데 simpleCacheReceived 를 non-nullable 로 할수는 없구먼 현재는.
            httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)

            dataSourceFactory = DefaultDataSourceFactory(context, httpDataSourceFactory)

            // Build data source factory with cache enabled, if data is available in cache it will return immediately,
            // otherwise it will open a new connection to get the data.
            cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(simpleCacheReceived!!) // simpleCache 를 non-null 타입으로 했으면..
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            exoPlayer = SimpleExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory)).setLoadControl(lcControl).build()

            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            exoPlayer.addListener(this)
            Log.d(TAG, "initExoPlayer: [Caching] ends......")
        }
        // B) LOCAL URI 재생 용도 (RtPicker 에서 사용)
        else{
            Log.d(TAG, "initExoPlayer: [LOCAL] .. begins")
            exoPlayer = SimpleExoPlayer.Builder(context).build()
            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            exoPlayer.addListener(this)
            Log.d(TAG, "initExoPlayer: [LOCAL] .. Ends")

        }

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
        }else if (playbackState == Player.STATE_ENDED) {
            Log.d(TAG, "onPlayerStateChanged: Playback state=Player.STATE_ENDED(4).  Int=$playbackState")}
        else {
            Log.d(TAG, "onPlayerStateChanged: State=IDLE(1번)-> 재생할 Media 없음 int= $playbackState")
            onExoError()
        }
    }


    // <----------<1>기존 코드들 ExoPlayer Related
// ----------><2>기존 코드들 Utility
    fun createMp3UrlMap(receivedRtInTheCloudList: List<RtInTheCloud>) {
        for (i in receivedRtInTheCloudList.indices) {
            mp3UrlMap[receivedRtInTheCloudList[i].id] = receivedRtInTheCloudList[i].mp3URL
        }
        Log.d(TAG, "createMp3UrlMap: finished.")

// <----------<2>기존 코드들 Utility
    }

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

        }
    }

    // b)Online Url 리소스를 가지는 음원 플레이(SecondFrag.kt) 에서 사용:  Called From RcVAdapter> 클릭 ->
    fun prepMusicPlayOnlineSrc(receivedTrId: Int, playWhenReady: Boolean) {
        //todo: 재생중일때 같은 트랙 또 클릭 -> 무시하기?
        removeHandler()
        setSeekbarToZero()


        // 불량 URL 확인, ErrorOccurred!
        val isUrlValid: Boolean = URLUtil.isValidUrl(mp3UrlMap[receivedTrId])

        // 1) 잘못된 mp3 url 이면
        if(!isUrlValid)
        {
            Toast.makeText(context,"Invalid Url error at id: $receivedTrId. Cannot Play Ringtone", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "URL-ERROR 1): Invalid Url error at id: $receivedTrId. Cannot Play Ringtone")

            GlbVars.errorTrackId = receivedTrId
            updateSongDuration(0)
            //setSeekbarToZero()
            //1-a) 그런데 그전에 클릭한 다른 트랙을 play OR buffering 중일경우에는 play/buffering 중인 previous 트랙을 멈춤.
            if(exoPlayer.isPlaying||exoPlayer.playbackState == Player.STATE_BUFFERING) {
                Log.d(TAG, "URL-ERROR 1-a): Invalid Url && 그 전 트랙 playing/buffering 상태였어 .. error at id: $receivedTrId. Cannot Play Ringtone")
                exoPlayer.stop() // stop
            }

        }
        try{
            // Play 전에 (가능하면) Caching 하기.
            prepPlayer(true, mp3UrlMap[receivedTrId], playWhenReady) // ->  신규클릭의 경우 playWhenReady = true, 재생 중 frag 왔다 다시왔을때는 false
        }catch(e: IOException) {
            toastMessenger.showMyToast("Unknown error occurred: $e", isShort = false)

            GlbVars.errorTrackId = receivedTrId
            // 위에서 이미 url 에러 -> return 으로 잡아줬지만 혹시 모르니..
        }
        // 3) 실제 Play >>>>>>>>>>>>>>>>>>>>>>>>>>>>> !! 는 위에 onPlayerStateChanged 에서 핸들링함.
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

    // 가) StatusMP ENUM=> 아래는 MyMediaPlayer 에서 전달받음. 그 후 전달받은 Status 는 _mpStatus 로 옮겨지고 SecondFrag 의 jjMpViewModel 이 이것을 Observe 하고 있음
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