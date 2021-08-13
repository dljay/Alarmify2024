package com.theglendales.alarm.jjmvp

import android.util.Log
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjadapters.MyNetWorkChecker
import com.theglendales.alarm.jjdata.RingtoneClass
import com.theglendales.alarm.jjfirebaserepo.FirebaseRepoClass
import io.reactivex.Single
import java.io.IOException

private const val TAG="JJ_MODEL"

class JJ_Model(val presenter: JJ_Presenter) {
    private val myNetworkCheckerInstance: MyNetWorkChecker by globalInject() // Koin 으로 아래 줄 대체!! 성공!
    private val firebaseRepoInstance: FirebaseRepoClass by globalInject()
    var fullRtClassList: MutableList<RingtoneClass> = ArrayList()

    fun loadFromFbModel(): Single<MutableList<RingtoneClass>> {
        Log.d(TAG, "loadFromFbModel: 3) Starts.")
        //1. 인터넷 가능한지 체크
        //인터넷되는지 체크
//        val isInternetAvailable: Boolean = myNetworkCheckerInstance!!.isNetWorkAvailable()
//        if(!isInternetAvailable) { // 인터넷 사용 불가!
//            Log.d(TAG, "loadFromFireBase: jj- isInternetAvailable= $isInternetAvailable")
//            //lottieAnimController(1)
//            return //더이상 firebase 로딩이고 나발이고 진행 안함!!
//        }
//        else {Log.d(TAG, "loadFromFireBase: jj- isInternetAvailable = $isInternetAvailable") }

        //2. If we have internet connectivity, then call FireStore!
        return Single.create { emitter -> // emitter 는 아래에서 행해지는 일들(firebase 로딩) 에 대한 결과 전달자?!
            try {
                firebaseRepoInstance.getPostList().addOnCompleteListener {
                    if(it.isSuccessful)
                    {
                        Log.d(TAG, "4-a) loadFromFbModel: <<<<<<<<<loadPostData: successful")

                        // 만약 기존에 선택해놓은 row 가 있으면 그쪽으로 이동.
//                mySmoothScroll()

                        fullRtClassList = it.result!!.toObjects(RingtoneClass::class.java)
                        emitter.onSuccess(fullRtClassList)
                    }
                }
            } catch (e: IOException) {
                Log.d(TAG, "4-B) loadFromFbModel: failed to get data from FB!!")
                emitter.onError(e)
            }

        }


    }
}