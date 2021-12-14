package com.theglendales.alarm.model

import android.net.Uri
import android.util.Log
import com.theglendales.alarm.configuration.AlarmApplication
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import java.lang.Exception

private const val TAG="Alarmtone.kt"
//private val defaultAlarmAlertUri = Settings.System.DEFAULT_ALARM_ALERT_URI.toString()
//private val defaultAlarmAlertUri = getDefaultRtaPath()
private val defrta1Path = AlarmApplication.getDefRtaPathStr("defrt01") // detailsFrag 에서의 파일 이름과 매칭 유의.
val mySharedPrefManager: MySharedPrefManager by globalInject() // Shared Pref by Koin!!

fun Alarmtone.ringtoneManagerString(): Uri? {
    Log.d(TAG, "ringtoneManagerString: this=$this")
    return when (this) {
        is Alarmtone.Silent -> null
        is Alarmtone.Default -> Uri.parse(defrta1Path) // <- 앱 인스톨시 생성되는 두개의 알람 관련.
        // **SQL 에서 최초 Install 알람 2개는 각 defrt01,02.mp3 로 설정해놓았지만 혹시 몰라서 적어둠. ++fab 버튼으로 생성되는 알람과는 무관..
        is Alarmtone.Sound -> Uri.parse(this.uriString)
    }
}
sealed class Alarmtone(open val persistedString: String?) {

    data class Silent(override val persistedString: String? = null) : Alarmtone(persistedString)
    data class Default(override val persistedString: String? = "") : Alarmtone(persistedString)
    data class Sound(val uriString: String) : Alarmtone(uriString)

    companion object {
        fun fromString(string: String?): Alarmtone {

            when (string) {
                null -> {
                    Log.d(TAG, "fromString: null")
                    return Silent()}
                "" -> {Log.d(TAG, "fromString: 이 로그디가 뜨면 곤란할듯..떠서는 안된다.") //
                    if(!defrta1Path.isNullOrEmpty()) {

                        return Sound(defrta1Path) //<- 최초 Install 알람 2개 설정시 여기로 들어옴
                        } else {
                        return Default() // 이거 괜찮을지..
                        }
                    }
//                defrta1Path -> {
//                    Log.d(TAG, "fromString: defaultAlarmAlertUri. string=$string")
//                    return Sound(defrta1Path)}
                else -> { // user 가 지정해놓은 rta 일 경우 string 이 이상한 경로겠지.. ex. string=/storage/emulated/0/Android/data/com.theglendales.alarm.debug/files/.AlarmRingTones/defrt5.rta
                    Log.d(TAG, "fromString: string=$string *Ringtone 의 경로가 지정되어있는 경우* (모든 경우임 이제는 Install 서 생성 알람 2개 포함) .. ")
                    return Sound(string)}
            }
        }
    }
}