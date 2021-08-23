package com.theglendales.alarm.jjmvvm

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjadapters.MyNetWorkChecker
import com.theglendales.alarm.jjdata.RingtoneClass
import com.theglendales.alarm.jjfirebaserepo.FirebaseRepoClass

private const val TAG="JjViewModel"
class JjViewModel : ViewModel() {
    private val myNetworkCheckerInstance: MyNetWorkChecker by globalInject() // Koin 으로 아래 줄 대체!! 성공!
    private val firebaseRepoInstance: FirebaseRepoClass by globalInject()
    var fullRtClassList: MutableList<RingtoneClass> = ArrayList()


    lateinit var liveRtList: MutableLiveData<MutableList<RingtoneClass>>

    init {
        liveRtList = MutableLiveData() // init this..
    }
//Return LiveData!
    fun getRtLiveDataObserver(): MutableLiveData<MutableList<RingtoneClass>> {
        return liveRtList
    }
// load from Fb! -
    fun loadFromFireBase() // todo: 1)JjFbRepository.kt 로 별도로 옮기기? 2)Make this Coroutine? SPotifyApp 클론 .fetchMediaData() 참고
     {
        firebaseRepoInstance.getPostList().addOnCompleteListener {
            if(it.isSuccessful)
            {
                Log.d(TAG, "<<<<<<<<<loadPostData: successful")
                fullRtClassList = it.result!!.toObjects(RingtoneClass::class.java)
                liveRtList.postValue(fullRtClassList) // LIVE DATA!


            }else { // 문제는 인터넷이 없어도 이쪽으로 오지 않음. always 위에 if(it.isSuccess) 로 감.
                Log.d(TAG, "<<<<<<<loadPostData: ERROR!! Exception message: ${it.exception!!.message}")
                //lottieAnimController(1) // this is useless at the moment..
            }
        }
    }
}