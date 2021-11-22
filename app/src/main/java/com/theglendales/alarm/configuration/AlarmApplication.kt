package com.theglendales.alarm.configuration

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewConfiguration
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.theglendales.alarm.R
import com.theglendales.alarm.alert.BackgroundNotifications
import com.theglendales.alarm.background.AlertServicePusher
import com.theglendales.alarm.bugreports.BugReporter
import com.theglendales.alarm.createNotificationChannels
import com.theglendales.alarm.jjdata.GlbVars
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjongadd.LottieDiskScanDialogFrag
import com.theglendales.alarm.model.Alarms
import com.theglendales.alarm.model.AlarmsScheduler
import com.theglendales.alarm.presenter.ScheduledReceiver
import com.theglendales.alarm.presenter.ToastPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private const val TAG="AlarmApplication"
class AlarmApplication : Application() {
    // 내가 추가-->> 앱 install 후 최초 시행!-> 하자마자 diskSearcher->mySharedPref 에 rta/art path 를 올려놓기 위해서 -> 그 뒤에 default 알람 (아침 8시30분.주말 9시 설정)
        private val myDiskSearcher: DiskSearcher by globalInject()
        private val mySharedPrefManager: MySharedPrefManager by globalInject()
        //lateinit var lottieDialogFrag: LottieDiskScanDialogFrag

    // 내가 추가 <<--


    override fun onCreate() {
        Log.d(TAG, "onCreate: !!AlarmApplication onCreate!!!")
        runCatching {
            ViewConfiguration::class.java
                    .getDeclaredField("sHasPermanentMenuKey")
                    .apply { isAccessible = true }
                    .setBoolean(ViewConfiguration.get(this), false)
        }

        val koin = startKoin(applicationContext)

        koin.get<BugReporter>().attachToMainThread(this)

        // must be after sContainer
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        // TODO make it lazy
        koin.get<ScheduledReceiver>().start()
        koin.get<ToastPresenter>().start()
        koin.get<AlertServicePusher>()
        koin.get<BackgroundNotifications>()

        createNotificationChannels()


// 내가 추가---> DiskSearcher 시행 및 mySharedPref 생성하여 art,rta path 기록해놓기.
        //lottieDialogFrag = LottieDiskScanDialogFrag.newInstanceDialogFrag()
        // todo: 여기서 첫 install 후 런칭인지 확인 -> SplashScreen? Launch Activity? "Getting our app Ready !" ->
        // todo: 추후 여기서 Permission 관련도 해결해줬으면..

        if(myDiskSearcher.isDiskScanNeeded()) { // 만약 새로 스캔 후 리스트업 & Shared Pref 저장할 필요가 있다면
            Log.d(TAG, "onCreate: $$$ Alright let's scan the disk!")
            // 추후 SPLASH 스크린으로 대체 가능하지만.


            // ** diskScan 시작 시점-> ANIM(ON)!


                //lottieAnimCtrl(SHOW_ANIM)
                //1-a) /.AlbumArt 폴더 검색 -> art 파일 list up -> 경로를 onDiskArtMap 에 저장
                myDiskSearcher.readAlbumArtOnDisk()
                //1-b-1) onDiskRtSearcher 를 시작-> search 끝나면 Default Rt(raw 폴더) 와 List Merge!
                val resultList = myDiskSearcher.onDiskRtSearcher() // rtArtPathList Rebuilding 프로세스. resultList 는 RtWAlbumArt object 리스트고 각 Obj 에는 .trkId, .artPath, .audioFileUri 등의 정보가 있음.
                //** 1-b-2) 1-b-1) 과정에서 rtOnDisk object 의 "artFilePathStr" 이 비어잇으면-> extractArtFromSingleRta() & save image(.rta) on Disk

                // 1-c) Merge 된 리스트(rtWithAlbumArt obj 로 구성)를 얼른 Shared Pref 에다 저장! (즉 SharedPref 에는 art, rta 의 경로가 적혀있음)
                mySharedPrefManager.saveRtaArtPathList(resultList)

                // 1-d) DiskSearcher.kt>finalRtArtPathList (Companion obj 메모리) 에 띄워놓음(갱신)
                myDiskSearcher.updateList(resultList)

                Log.d(TAG, "onCreate: DiskScan DONE..(Hopefully..), resultList = $resultList!")


             // ** diskScan 종료 <--

        }
        //2) Scan 이 필요없음(X)!!! 여기서 SharedPref 에 있는 리스트를 받아서 -> DiskSearcher.kt>finalRtArtPathList (Companion obj 메모리) 에 띄워놓음(갱신)
        else if(!myDiskSearcher.isDiskScanNeeded()) {
            val resultList = mySharedPrefManager.getRtaArtPathList()

            Log.d(TAG, "onCreate: XXX no need to scan the disk. Instead let's check the list from Shared Pref => resultList= $resultList")
            myDiskSearcher.updateList(resultList)
        }
// 내가 추가<---

        Log.d(TAG, "onCreate: -----------after DiskSearch/MySharedPref DONE------------\n\n")

        // must be started the last, because otherwise we may loose intents from it.
        val alarmsLogger = koin.logger("Alarms")
        alarmsLogger.debug { "Starting alarms" } // 최초 설치시 신규 알람 2개 만들어주는 작업.
        koin.get<Alarms>().start()
        // start scheduling alarms after all alarms have been started
        koin.get<AlarmsScheduler>().start()

        with(koin.get<Store>()) {
            // register logging after startup has finished to avoid logging( O(n) instead of O(n log n) )
            alarms()
                    .distinctUntilChanged()
                    .subscribe { alarmValues ->
                        for (alarmValue in alarmValues) {
                            alarmsLogger.debug { "$alarmValue" }
                        }
                    }

            next()
                    .distinctUntilChanged()
                    .subscribe { next -> alarmsLogger.debug { "## Next: $next" } }
        }

        super.onCreate()
    }



    //Lottie 관련 추가-> 여기서 실행 안됨..
//    private fun showLottieDialogFrag() {
//        //lottieDialogFrag.show(requireActivity().supportFragmentManager, lottieDialogFrag.tag)
//
//    }
//    private fun hideLottieAndShowSnackBar() {
//        if(lottieDialogFrag.isAdded) {
//            lottieDialogFrag.dismissAllowingStateLoss()
//            //Snackbar.make(requireActivity().findViewById(android.R.id.content), "REBUILDING DATABASE COMPLETED", Snackbar.LENGTH_LONG).show()
//        }
//    }
}
