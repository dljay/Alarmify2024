package com.theglendales.alarm.jjmvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjfirebaserepo.FirebaseRepoClass
import kotlinx.coroutines.launch

/**
 * 쫑 한말씀: This ViewModel should follow Activity(AlarmsListActivity)'s life cycle.
 */
private const val TAG="JjMainViewModel"

class JjMainViewModel : ViewModel() {


//*******************FireBase LiveData
    var isFreshList= false
    private val firebaseRepoInstance: FirebaseRepoClass by globalInject()
    private val _rtInTheCloudList = MutableLiveData<MutableList<RtInTheCloud>>() // Private& Mutable LiveData
    val rtInTheCloudList: LiveData<MutableList<RtInTheCloud>> = _rtInTheCloudList // Public but! Immutable (즉 이놈은 언제나= _liveRtList)

    init {viewModelScope.launch {getRtListFromFb()}}
    //ViewModel 최초 로딩시 & Spinner 로 휘리릭~ 새로고침 할 때 아래 function 이 불림.
    fun getRtListFromFb() {
        // internet check?
        firebaseRepoInstance.getPostList().addOnCompleteListener {
            if(it.isSuccessful)
            {
                Log.d(TAG, "getRtList: <<<<<<<<<getRtList: successful")
                isFreshList = true
                _rtInTheCloudList.value = it.result!!.toObjects(RtInTheCloud::class.java)

            }else { // 문제는 인터넷이 없어도 이쪽으로 오지 않음. always 위에 if(it.isSuccess) 로 감.
                Log.d(TAG, "<<<<<<<getRtList: ERROR!! Exception message: ${it.exception!!.message}")
                //lottieAnimController(1) // this is useless at the moment..
            }
        }

    }
//*******************Network Detector -> LottieAnim 까지 연결
    var prevNT = true
    private val _isNetworkWorking = MutableLiveData<Boolean>() // Private& Mutable LiveData
    val isNetworkWorking: LiveData<Boolean> = _isNetworkWorking // Public but! Immutable (즉 이놈은 언제나= _liveRtList)

    fun updateNTWKStatus(isNetworkOK: Boolean) {
        _isNetworkWorking.postValue(isNetworkOK) // .postValue= backgroundThread 사용. // (이 job 은 발생지가 backgrouond thread 니깐 .value=xx 안되고 postValue() 써야함!)

    }

//***********************
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: called..")
    }
}