package com.theglendales.alarm.jjmvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjmvvm.mediaplayer.ExoForLocal
import com.theglendales.alarm.jjmvvm.mediaplayer.ExoForUrl
import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp
import com.theglendales.alarm.jjmvvm.util.RtOnThePhone

private const val TAG="JjRtPickerVModel"
class JjRtPickerVModel : ViewModel() {
    private val exoForLocal: ExoForLocal by globalInject()


    private val _selectedRow = MutableLiveData<RtOnThePhone>() // Private & Mutable
    val selectedRow: LiveData<RtOnThePhone> = _selectedRow

    fun updateLiveData(rtOnThePhone: RtOnThePhone) { _selectedRow.value = rtOnThePhone}

    // Rt 재생 관련(Local)
    //*******************Media Player LiveData Observe 관련
    fun getMpStatusLiveData(): LiveData<StatusMp> = exoForLocal.mpStatus
    fun getSongDurationLiveData(): LiveData<Long> = exoForLocal.songDuration
    fun getCurrentPosLiveData(): LiveData<Long> = exoForLocal.currentPosition

    override fun onCleared() {
        Log.d(TAG, "onCleared: called")
        super.onCleared()

        //todo: 위에서 getMpxx() 등 exoForLocal 안에 있는 LiveData 들은 RtPickerActivitiy 이 종료(destroy) 됨에도 계속 살아있다. Memory Leak 일수 있음.
        // 해결책중 하나는 RtPickerActivity 에서 observable 을 따로 변수로 만들어주고 removeObservable() .. 이거 하는건데 복잡해서 일단은 생략. 추후 확인 필요.

    // 를 Observe 하던 여기서 따로 그동안 Observe 하던거 없애줘야할지? https://origogi.github.io/android/viewmodel-onCleared/
    }
}