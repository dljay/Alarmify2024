package com.theglendales.alarm.configuration

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import android.view.ViewConfiguration
import androidx.preference.PreferenceManager
import com.theglendales.alarm.R
import com.theglendales.alarm.alert.BackgroundNotifications
import com.theglendales.alarm.background.AlertServicePusher
import com.theglendales.alarm.createNotificationChannels
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.model.Alarms
import com.theglendales.alarm.model.AlarmsScheduler
import com.theglendales.alarm.presenter.ScheduledReceiver
import com.theglendales.alarm.presenter.ToastPresenter
import java.io.File


private const val TAG="AlarmApplication"
class AlarmApplication : Application() {
    // 내가 추가-->> 앱 install 후 최초 시행!-> 하자마자 diskSearcher->mySharedPref 에 rta/art path 를 올려놓기 위해서 -> 그 뒤에 default 알람 (아침 8시30분.주말 9시 설정)
        private val myDiskSearcher: DiskSearcher by globalInject()
        private val mySharedPrefManager: MySharedPrefManager by globalInject()

        companion object {
            var jjPackageName=""

            fun getDefRtaPathStr(rtaName: String): String {
                //raw 폴더에 저장된 defrt01.rta, defrt02.rta 로 AlarmDatabaseHelper 에서 Install 후 알람 두개의 ringtone 으로 적용시켜줌.
                return ContentResolver.SCHEME_ANDROID_RESOURCE + File.pathSeparator + File.separator + File.separator+ jjPackageName + "/raw/" + rtaName
                // File.pathSeparator = ":" , File.separator = "/"
            }
            fun getDefArtPathStr(artName: String): String {
                return ContentResolver.SCHEME_ANDROID_RESOURCE + File.pathSeparator + File.separator + File.separator+ jjPackageName + "/drawable/" + artName
            }
        }

    // 내가 추가 <<--


    override fun onCreate()
    {
//        val packageName = applicationContext.packageName // todo: 이걸 STATIC 으로 저장? 아니면 ListFrag 에서 바로 받기?
//        val heyho = "android.resource://" + packageName + R.raw.defrt1
//        Log.d(TAG, "onCreate: !!AlarmApplication onCreate!!! heyho=$heyho, packageName=$packageName")
        jjPackageName = applicationContext.packageName

        runCatching {
            ViewConfiguration::class.java
                    .getDeclaredField("sHasPermanentMenuKey")
                    .apply { isAccessible = true }
                    .setBoolean(ViewConfiguration.get(this), false)
        }

        val koin = startKoin(applicationContext)

        //koin.get<BugReporter>().attachToMainThread(this)

        // must be after sContainer
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        // TODO make it lazy
        koin.get<ScheduledReceiver>().start()
        koin.get<ToastPresenter>().start()
        koin.get<AlertServicePusher>()
        koin.get<BackgroundNotifications>()

        createNotificationChannels()

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
