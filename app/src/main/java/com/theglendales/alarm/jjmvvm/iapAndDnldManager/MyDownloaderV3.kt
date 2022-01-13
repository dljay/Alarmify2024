package com.theglendales.alarm.jjmvvm.iapAndDnldManager

import android.content.Context
import android.util.Log
import com.theglendales.alarm.jjdata.RtInTheCloud
import kotlinx.coroutines.delay

private const val TAG="MyDownloaderV3"
class MyDownloaderV3(val context: Context) {

    suspend fun getDnldId(rtClassObj: RtInTheCloud): Long {
        Log.d(TAG, "getDnldId: <A> Begins. TrId= ${rtClassObj.id}, rtTitle=${rtClassObj.title}, rtClassObj=${rtClassObj}, ")

        //A) Download Prep - 여기서 try{} catch{} 쓰면 이미 Exception 이 여기서 잡혀서 Parent 코루틴 Handler 로 전달 안됨 (dnldParentJob)
        delay(1000L)
        try{
            //Download Process (...)
            val dnldId= 500L // .. enqueue - 다운로드 시작
            return dnldId
        } catch (e: Exception) {
            Log.d(TAG, "getDnldId: <A> caught Exception.")
            return -444L
        }

    }
    suspend fun getDnldProgress(dnldId: Long){
    }


}