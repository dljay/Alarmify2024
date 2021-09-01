package com.theglendales.alarm.jjmvvm.mediaplayer

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import com.theglendales.alarm.jjmvvm.JjMpViewModel
import java.util.*
import java.util.logging.Handler

private const val TAG="MyMediaPlayer"


enum class StatusMp { IDLE, LOADING, PLAY, PAUSE} // LOADING: activateLC(),

class MyMediaPlayer(val receivedContext: Context, val mpViewModel: JjMpViewModel) {



// Called From RcVAdapter> 클릭 ->
    fun prepareMusicPlay() {
        //first set to Idle state.
        //mpViewModel.updateStatusMpLiveData(StatusMp.LOADING) // SecondFrag 로 전달.
        //when ready we hit play
         onMusicLoading()
        // 3초 지연
        Timer().schedule(object: TimerTask() {
            override fun run() {
                         }
        }, 3000)
        continueMusic()

    }

// called from MiniPlayer button (play/pause)
    fun continueMusic() {
        // exoplayer.play()
        onMusicPlay()
    }
    fun pauseMusic() {
        // exoplayer.pause()
        onMusicPaused()
    }

    private fun onMusicLoading() = mpViewModel.updateStatusMpLiveData(StatusMp.LOADING)
    private fun onMusicPlay() = mpViewModel.updateStatusMpLiveData(StatusMp.PLAY)
    private fun onMusicPaused() = mpViewModel.updateStatusMpLiveData(StatusMp.PAUSE)


}