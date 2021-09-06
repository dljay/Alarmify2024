package com.theglendales.alarm.jjmvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp


private const val TAG="JjMpViewModel"
// MediaPlayer 과 SecondFragment 를 연결해줄 ViewModel. 이 VModel 을 통해서 SecondFrag 는 VuMeter 를 업뎃해줌. (MediaPlayer <->MpVmodel <-> SecondFrag)
class JjMpViewModel: ViewModel() {

    //1) StatusMp ENUM 클래스 정보 갖고 있음.
    private val _mpStatus = MutableLiveData<StatusMp>() // Private & Mutable
    val mpStatus: LiveData<StatusMp> = _mpStatus

    //2-A) 재생할 곡 길이 (exoPlayer.duration)
    private val _songDuration = MutableLiveData<Long>()
    val songDuration: LiveData<Long> = _songDuration
    //2-B) 재생중인 포지션 (exoPlayer.currentPosition(Long))
    private val _currentPosition = MutableLiveData<Long>()
    val currentPosition: LiveData<Long> = _currentPosition

    // 1) StatusMP ENUM=> 아래는 MyMediaPlayer 에서 전달받음. 그 후 전달받은 Status 는 _mpStatus 로 옮겨지고 SecondFrag 의 jjMpViewModel 이 이것을 Observe 하고 있음
    fun updateStatusMpLiveData(statusReceived: StatusMp) {
        Log.d(TAG, "updateStatusMpLiveData: called")
        _mpStatus.value = statusReceived
    }
    //2-A) 재생할 곡 길이 업데이트 (exoPlayer.duration)
    fun updateSongDuration(durationReceived: Long) {
        Log.d(TAG, "updateSongDuration: called")
        _songDuration.value = durationReceived
    }
    //2-B) 재생중인 포지션 업데이트 (exoPlayer.currentPosition(Long))
    fun updateCurrentPosition(positionReceived: Long) {
        //Log.d(TAG, "updatedCurrentPosition: called")
        _currentPosition.value = positionReceived
    }
}