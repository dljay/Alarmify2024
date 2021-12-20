package com.theglendales.alarm.jjmvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.theglendales.alarm.jjmvvm.iapAndDnldManager.StatusDNLD
import com.theglendales.alarm.jjmvvm.util.RtWithAlbumArt

private const val TAG="JjDNLDViewModel"

class JjDNLDViewModel : ViewModel() {

    //1) 다운됐을 때 인식할 RtWithAlbumArt Obj <일부 칸은 Null 로 되어있음. ListFrag 에서 DiskScan.. 하면서 채워주면 된다..>
    private val _dnldRtObj = MutableLiveData<RtWithAlbumArt>() //Private but Mutable (외부접근 금지)
    val dnldRtObj = _dnldRtObj // immutable & constantly monitors this.

    //2) 다운로드의 Status 정보
    private val _dnldStatus = MutableLiveData<Int>() //Private but Mutable (외부접근 금지)
    val dnldStatus = _dnldStatus // immutable & constantly monitors _dnldStatus

    //3) 다운로드시 Progress(Float) 정보
    private val _dnldPrgrs = MutableLiveData<Int>()
    val dnldPrgrs: LiveData<Int> = _dnldPrgrs

// ** Functions
    //가) RtWithAlbum Obj -> 이걸 바탕으로 다운로드 BtmSht UI 업뎃.
    fun updateDNLDRtObj(rtObjReceived: RtWithAlbumArt) {
        Log.d(TAG, "updateDNLDRtObj: called. rtObjReceived=$rtObjReceived")
        _dnldRtObj.value = rtObjReceived
    }

    //나) StatusDNLD ENUM=> 아래는 MyDownloader2.kt 에서 전달받음. 그 후 전달받은 Status 는 _dnldStatus 로 옮겨지고 SecondFrag 의 jjDNLDViewModel 이 이것을 Observe 하고 있음
    fun updateDNLDStatusLive(statusIntReceived: Int) {
        Log.d(TAG, "updateDNLDStatusLive: called. Status Number=$statusIntReceived")
        _dnldStatus.postValue(statusIntReceived) // .postValue 코루틴 등 BackGround Thread 에서 처리할 때 사용. "Posts a task to a main thread to set the given value."
    }
    //다) 다운로드 진행중인 Progress
    fun updateDNLDProgressLive(progressReceived: Int) {
        Log.d(TAG, "updateDNLDProgressLive: called")
        _dnldPrgrs.postValue(progressReceived)
    }
    //라) 모든 다운로드가 끝났으면 값을 초기화 (dnldStatus= IDLE, dnldPrgrs= 0)
    fun initDNLDLiveData() {
        _dnldStatus.value = -44
        _dnldPrgrs.value = -44

    }
}