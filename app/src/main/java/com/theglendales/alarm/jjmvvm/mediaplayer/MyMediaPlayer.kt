package com.theglendales.alarm.jjmvvm.mediaplayer

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import com.theglendales.alarm.jjmvvm.JjMpViewModel

private const val TAG="MyMediaPlayer"


enum class StatusMp { LOADING, PLAY, PAUSE, STOP} // LOADING: activateLC(),

class MyMediaPlayer(val receivedContext: Context, val mpViewModel: JjMpViewModel) {



// Called From SecondFragment>myOnLiveDataFromRCV()
    fun prepareMusicPlay() {
        //first set to Idle state.
        mpViewModel.updateStatusMpLiveData(StatusMp.LOADING) // SecondFrag 로 전달.
        //when ready we hit play
//        onMusicLoading()
//        onMusicPlay()
    }

    fun onMusicLoading() = mpViewModel.updateStatusMpLiveData(StatusMp.LOADING)
    fun onMusicPlay() = mpViewModel.updateStatusMpLiveData(StatusMp.PLAY)
    fun onMusicPaused() = mpViewModel.updateStatusMpLiveData(StatusMp.PAUSE)


}