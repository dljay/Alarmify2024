package com.theglendales.alarm.jjmvvm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjmvvm.mediaplayer.ExoForLocal
import com.theglendales.alarm.jjmvvm.mediaplayer.ExoForUrl
import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp
import com.theglendales.alarm.jjmvvm.util.RtOnThePhone

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
}