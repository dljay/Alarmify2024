package com.theglendales.alarm.jjmvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.QuerySnapshot
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjadapters.MyNetWorkChecker
import com.theglendales.alarm.jjdata.RingtoneClass
import com.theglendales.alarm.jjfirebaserepo.FirebaseRepoClass

private const val TAG="JjFirebaseViewModel"
class JjFirebaseViewModel : ViewModel() {
    private val myNetworkCheckerInstance: MyNetWorkChecker by globalInject() // Koin 으로 아래 줄 대체!! 성공!
    private val firebaseRepoInstance: FirebaseRepoClass by globalInject()
    var fullRtClassList: MutableList<RingtoneClass> = ArrayList()
//livedata
//    private val _liveRtList = MutableLiveData<MutableList<RingtoneClass>>() // Private& Mutable LiveData
//    val liveRtList: LiveData<MutableList<RingtoneClass>> = _liveRtList // Public but! Immutable (즉 이놈은 언제나= _liveRtList)

    private val _liveTaskQSnapShot = MutableLiveData<Task<QuerySnapshot>>() // Private& Mutable LiveData
    val liveTaskQSnapShot: LiveData<Task<QuerySnapshot>> = _liveTaskQSnapShot // 이놈을 SecondFrag 에서 Observe => Public but! Immutable (즉 이놈은 언제나= _liveRtList. Mirror...)

//    private val _liveChipTaskQSnapShot = MutableLiveData<Task<QuerySnapshot>>() // Private& Mutable LiveData
//    val liveChipTaskQSnapShot: LiveData<Task<QuerySnapshot>> = _liveChipTaskQSnapShot // 이놈을 SecondFrag 에서 Observe => Public but! Immutable (즉 이놈은 언제나= _liveRtList. Mirror...)

    init {
        loadFromFireBase()
    }
//1) 기본 Firebase 로딩 관련
    //Return LiveData!
    fun getRtLiveDataObserver(): LiveData<Task<QuerySnapshot>> {
        return liveTaskQSnapShot
    }
    // load from Fb! -
    fun loadFromFireBase()  {
        val qSnapShot= firebaseRepoInstance.getPostList() // Returns- Task<QuerySnapshot>
        _liveTaskQSnapShot.postValue(qSnapShot)
    }
//2) Chip Tag 로 Sorting 되는 결과물 받기 관련.
//    fun getChipSortedObserver(): LiveData<Task<QuerySnapshot>> {
//        return liveChipTaskQSnapShot
//    }
//
//    fun loadFromFireBaseWithChip(tagsList: MutableList<String>) {
//        val chipQSnapShot = firebaseRepoInstance.sortSingleOrMultipleTags(tagsList)
//        _liveChipTaskQSnapShot.postValue(chipQSnapShot)
//    }


//    fun loadFromFireBase() // todo: 1)Task 를 return. 2)JjFbRepository.kt 로 별도로 옮기기?
//    {
//        firebaseRepoInstance.getPostList().addOnCompleteListener {
//            if(it.isSuccessful)
//            {
//                Log.d(TAG, "<<<<<<<<<loadPostData: successful")
//                fullRtClassList = it.result!!.toObjects(RingtoneClass::class.java)
//                _liveRtList.postValue(fullRtClassList) // LIVE DATA!
//
//
//            }else { // 문제는 인터넷이 없어도 이쪽으로 오지 않음. always 위에 if(it.isSuccess) 로 감.
//                Log.d(TAG, "<<<<<<<loadPostData: ERROR!! Exception message: ${it.exception!!.message}")
//                //lottieAnimController(1) // this is useless at the moment..
//            }
//        }
//    }
}