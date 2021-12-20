package com.theglendales.alarm.jjmvvm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.theglendales.alarm.jjmvvm.iapAndDnldManager.StatusDNLD

private const val TAG="JjDNLDViewModel"

class JjDNLDViewModel : ViewModel() {

    //1) 다운로드의 Status 정보
    private val _dnldStatus = MutableLiveData<StatusDNLD>() //Private but Mutable (외부접근 금지)
    val dnldStatus = _dnldStatus // immutable & constantly monitors _dnldStatus

    //2) 다운로드시 Progress(Float) 정보
    private val _dnldPrgrs = MutableLiveData<Long>()
    val dnldPrgrs: LiveData<Long> = _dnldPrgrs
}