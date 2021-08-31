package com.theglendales.alarm.jjmvvm.mediaplayer

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import com.theglendales.alarm.jjmvvm.JjMpViewModel

private const val TAG="MyMediaPlayer"


enum class StatusMp { IDLE, LOADING, PLAY, PAUSE, STOP}

class MyMediaPlayer(val receivedContext: Context, val mpViewModel: JjMpViewModel) {



// Called From SecondFragment>myOnLiveDataFromRCV()
    fun playMusicTest() {
        //prepare playing
        // ......(success)
        //when music is played
        Log.d(TAG, "playMusic: called..")
        mpViewModel.updateStatusMpLiveData(StatusMp.PLAY)
    }

}