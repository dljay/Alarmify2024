package com.theglendales.alarm.jjmvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.theglendales.alarm.jjmvvm.iapAndDnldManager.StatusDNLD

private const val TAG="JjDNLDViewModel"

class JjDNLDViewModel : ViewModel() {

    //1) 다운로드의 Status 정보
    private val _dnldStatus = MutableLiveData<Int>() //Private but Mutable (외부접근 금지)
    val dnldStatus = _dnldStatus // immutable & constantly monitors _dnldStatus

    //2) 다운로드시 Progress(Float) 정보
    private val _dnldPrgrs = MutableLiveData<Int>()
    val dnldPrgrs: LiveData<Int> = _dnldPrgrs

// ** Functions
    //가) StatusDNLD ENUM=> 아래는 MyDownloader2.kt 에서 전달받음. 그 후 전달받은 Status 는 _dnldStatus 로 옮겨지고 SecondFrag 의 jjDNLDViewModel 이 이것을 Observe 하고 있음
    fun updateDNLDStatusLive(statusIntReceived: Int) {
        Log.d(TAG, "updateStatusMpLiveData: called. Status Number=$statusIntReceived")
        _dnldStatus.value = statusIntReceived
    }
    //나) 다운로드 진행중인 Progress
    fun updateDNLDProgressLive(progressReceived: Int) {
        Log.d(TAG, "updateSongDuration: called")
        _dnldPrgrs.value = progressReceived
    }
    //다) 모든 다운로드가 끝났으면 값을 초기화 (dnldStatus= IDLE, dnldPrgrs= 0)
    fun initDNLDLiveData() {
        _dnldStatus.value = -44
        _dnldPrgrs.value = -44

    }
}