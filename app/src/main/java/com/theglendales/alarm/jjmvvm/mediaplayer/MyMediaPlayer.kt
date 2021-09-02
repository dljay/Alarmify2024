package com.theglendales.alarm.jjmvvm.mediaplayer

import android.content.Context
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.webkit.URLUtil
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.theglendales.alarm.jjdata.GlbVars
import com.theglendales.alarm.jjdata.RingtoneClass
import com.theglendales.alarm.jjmvvm.JjMpViewModel
import java.io.IOException
import java.lang.Exception
import java.util.*

private const val TAG="MyMediaPlayer"


enum class StatusMp { IDLE, LOADING, PLAY, PAUSE} // LOADING: activateLC(),

class MyMediaPlayer(val receivedFragActivity: Context, val mpViewModel: JjMpViewModel) : Player.Listener {

    companion object {
        //var currentClickTrId: Int = -1
        //var prevClickedTrId: Int = -1
        val mp3UrlMap: HashMap<Int, String> = HashMap()
    }
//A) ExoPlayer
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

    fun initExoPlayerWithCache() { // MyCacher.kt 에서 호출됨.
    Log.d(TAG, "initExoPlayerWithCache: starts......")
    val lcControl = loadControlSetUp()

    simpleCacheReceived = MyCacher.simpleCache!! // todo: this is dangerous..근데 simpleCacheReceived 를 non-nullable 로 할수는 없구먼 현재는.
    httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)

    dataSourceFactory = DefaultDataSourceFactory(receivedFragActivity, httpDataSourceFactory)

    // Build data source factory with cache enabled, if data is available in cache it will return immediately,
    // otherwise it will open a new connection to get the data.
    cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(simpleCacheReceived!!) // simpleCache 를 non-null 타입으로 했으면..
        .setUpstreamDataSourceFactory(httpDataSourceFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    exoPlayer = SimpleExoPlayer.Builder(receivedFragActivity).setMediaSourceFactory(
        DefaultMediaSourceFactory(cacheDataSourceFactory)
    )
        .setLoadControl(lcControl).build()

    exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
    exoPlayer.addListener(this)
    Log.d(TAG, "initExoPlayerWithCache: ends......")
    }

    private fun prepPlayerWithCache(url:String?) { // Caching 위해 <TYPE:2>
        //1) to play a single song
        val mp3Uri = Uri.parse(url)
        val mediaItem = MediaItem.fromUri(mp3Uri)

        // Build data source factory with cache enabled, if data is available in cache it will return immediately, otherwise it will open a new connection to get the data.
        val mediaSource = ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(mediaItem)

        //playerView.player = simpleExoPlayer
        exoPlayer.setMediaSource(mediaSource, true)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

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
    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if (playbackState == Player.STATE_BUFFERING) {

            Log.d(TAG, "onPlayerStateChanged: Playback state=Player.STATE_BUFFERING. PlayWhenReady=$playWhenReady")
        // 신규추가!
            onExoLoading()

        }
        else if (playbackState == Player.STATE_READY) {  // 준비 완료! 기존 mp 의 setOnPreparedListener{} 내용이 여기로 왔음.
            if(playWhenReady) { // PLAYING! (or resume playing)
                Log.d(TAG, "onPlayerStateChanged: Playback state=Player.STATE_READY. PlayWhenReady=$playWhenReady")
        // 신규추가!
                onExoPlaying()
                //GlbVars.isSongPaused = false
                //GlbVars.isSongPlaying = true

                //initializeSeekBar()
                //seekbarListenerSetUp()
                feedLiveDataSongDuration()
                feedLiveDataCurrentPosition()

                Log.d(TAG, "Finally Playing! Global.currentPlayingTrNo: ${GlbVars.currentPlayingTrId}")
                //UI 변경: -> 아래 모든 것 LiveData 로 해결 가능할듯.
                // A) play/pause 버튼
//                imgbtn_Play?.visibility = View.GONE       // Play button to Pause button
//                imgbtn_Pause?.visibility = View.VISIBLE
//                assignVTL(GlbVars.currentPlayingTrId) // 그냥 clickedTrID 바로 전달해주고 싶은데. 여기 onPlayerStateChanged 에 clickTrId 를 전달할 방법이 없음 (.. )
//                deactivatePrevVMandCircle()
//                stopLoadingCircle()
//                activateCurrentVuMeter(GlbVars.currentPlayingTrId)

            } else { // PAUSED!
                Log.d(TAG, "onPlayerStateChanged: PAUSED.. Playback state=Player.STATE_READY. PlayWhenReady=$playWhenReady")
            }
        }else if (playbackState == Player.STATE_ENDED) {Log.d(TAG, "onPlayerStateChanged: Playback state=Player.STATE_ENDED(4).  Int=$playbackState")}
        else {
            Log.d(TAG, "onPlayerStateChanged: State=IDLE? int= $playbackState")
                onExoIdle()
            }
    }


// <----------<1>기존 코드들 ExoPlayer Related
// ----------><2>기존 코드들 Utility
    fun createMp3UrlMap(receivedRingtoneClassList: List<RingtoneClass>) {
        for (i in receivedRingtoneClassList.indices) {
            mp3UrlMap[receivedRingtoneClassList[i].id] = receivedRingtoneClassList[i].mp3URL
    }

// <----------<2>기존 코드들 Utility
}

// <3> 추가된 코드들--LiveData/Ui 외-------------- >>>>>>>>>

// Called From RcVAdapter> 클릭 ->
    fun prepareMusicPlay(receivedTrId: Int) {
    // 다른 트랙 재생중 (=play 버튼이 ||(pause) 상태) 일경우 => Play(>)  상태로 바꿈.
//    if(imgbtn_Pause?.visibility == View.VISIBLE) {
//        imgbtn_Play?.visibility = View.VISIBLE       // Play button to Pause button
//        imgbtn_Pause?.visibility = View.GONE
//    }
//
//    //Initialize
//    GlbVars.currentPlayingTrId = receivedTrId
//    GlbVars.errorTrackId = -44
//
//
//    assignVTL(receivedTrId)
//    deactivatePrevVMandCircle()
//
//    //Show Progress Circle
//    showLoadingCircle()

    // 불량 URL 확인, ErrorOccurred!
    val isUrlValid: Boolean = URLUtil.isValidUrl(mp3UrlMap[receivedTrId])

    // 1) 잘못된 mp3 url 이면
    if(!isUrlValid)
    {
        Toast.makeText(receivedFragActivity,"Invalid Url error at id: $receivedTrId. Cannot Play Ringtone", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "URL-ERROR 1): Invalid Url error at id: $receivedTrId. Cannot Play Ringtone")
        //isErrorOccurred = true

        GlbVars.errorTrackId = receivedTrId

        //1-a) 그런데 그전에 클릭한 다른 트랙을 play OR buffering 중일경우에는 play/buffering 중인 previous 트랙을 멈춤.
        if(exoPlayer.isPlaying||exoPlayer.playbackState == Player.STATE_BUFFERING) {
            Log.d(TAG, "URL-ERROR 1-a): Invalid Url && 그 전 트랙 playing/buffering 상태였어 .. error at id: $receivedTrId. Cannot Play Ringtone")
            exoPlayer.stop() // stop

            handler.removeCallbacks(runnable)
            mpViewModel.updateCurrentPosition(0) // => 결국 seekbar.progress = 0 과 같은 기능.

//            imgbtn_Play?.visibility = View.VISIBLE  // Play button 을 보이게함.
//            imgbtn_Pause?.visibility = View.GONE
//
//            deactivatePrevVMandCircle()
        }
        return // 그리고 그냥 더 이상 진행하지 않음!
    }
    try{

        // Play 전에 (가능하면) Caching 하기.
        prepPlayerWithCache(mp3UrlMap[receivedTrId])


    }catch(e: IOException) {
        Toast.makeText(receivedFragActivity, "Unknown error occurred: $e", Toast.LENGTH_LONG).show()
        GlbVars.errorTrackId = receivedTrId
        // 위에서 이미 url 에러 -> return 으로 잡아줬지만 혹시 모르니..
    }
    // 3) 실제 Play >>>>>>>>>>>>>>>>>>>>>>>>>>>>> !! 는 위에 onPlayerStateChanged 에서 핸들링함.
}
    private fun feedLiveDataCurrentPosition() {
        runnable = kotlinx.coroutines.Runnable {
            try {
                Log.d(TAG, "feedLiveDataCurrentPosition: runnable working")
                mpViewModel.updateCurrentPosition(exoPlayer.currentPosition) //livedata 로 feed 를 보냄
                handler.postDelayed(runnable,1000) // 1초에 한번씩
            }catch (e: Exception) {
                mpViewModel.updateCurrentPosition(0) // 문제 생기면 그냥 '0' 전달.
            }
        }
        handler.postDelayed(runnable, 1000) // 최초 실행? 무조건 한번은 실행해줘야함.
    }
    private fun feedLiveDataSongDuration() {
        if(exoPlayer.duration > 0) {
            mpViewModel.updateSongDuration(exoPlayer.duration)
        }
    }
    // SecondFrag.kt 에서 SeekBar 를 user 가 만졌을 때 exoPlayer 에게전달
    fun onSeekBarTouchedYo(progress: Long) = exoPlayer.seekTo(progress)

// called from MiniPlayer button (play/pause)
    fun continueMusic() {
        // exoplayer.play()
        onExoPlaying()

    }
    fun pauseMusic() {
        // exoplayer.pause()
        onExoPaused()
    }

    private fun onExoIdle() =  mpViewModel.updateStatusMpLiveData(StatusMp.IDLE)
    private fun onExoLoading() = mpViewModel.updateStatusMpLiveData(StatusMp.LOADING)
    private fun onExoPlaying() = mpViewModel.updateStatusMpLiveData(StatusMp.PLAY)
    private fun onExoPaused() = mpViewModel.updateStatusMpLiveData(StatusMp.PAUSE)


}