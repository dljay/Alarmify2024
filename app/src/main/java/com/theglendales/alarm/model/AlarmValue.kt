package com.theglendales.alarm.model

import android.net.Uri
import android.util.Log
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import java.util.Calendar

private const val TAG="AlarmValue.kt"
data class AlarmValue(
        val nextTime: Calendar,
        val state: String,
        val id: Int,
        val isEnabled: Boolean,
        val hour: Int,
        val minutes: Int,
        val isPrealarm: Boolean,
        val alarmtone: Alarmtone,
        val artFilePath: String?, // 내가 추가.
        val isVibrate: Boolean,
        val label: String,
        val daysOfWeek: DaysOfWeek
) {

    val skipping = state.contentEquals("SkippingSetState")

    val isSilent: Boolean
        get() = alarmtone is Alarmtone.Silent

    // If the database alert is null or it failed to parse, use the
    // default alert.
    // **!! +FAB 버튼으로 알람 생성할때만 적용되는듯 ** (!!app 인스톨 후 생성되는 2개 말고..)
    @Deprecated("TODO move to where it is used")
    val alertSoundUri: Uri by lazy {
        when (alarmtone) {
            is Alarmtone.Silent -> throw RuntimeException("Alarm is silent")
            is Alarmtone.Default -> {
                //RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                Uri.parse(getRandomDefaultRtaPath())
                }
            is Alarmtone.Sound -> try {
                Uri.parse(alarmtone.uriString)
            } catch (e: Exception) {
                //RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                Uri.parse(getRandomDefaultRtaPath())
            }
        }

    }
    private fun getRandomDefaultRtaPath(): String? { // ** 신규 알람 생성할때만! 불리네. 인스톨시 2개 생성 말고.. **
        // 알람에 defrta1~5 사이 아무거나 지정..? 사실상 이건 의미없고. AlarmDetailsFrag.getRandomDefaultRtaPath 가 결정지음!!
        Log.d(TAG, "getRandomDefaultRta: called!!")
        val randomNumber = (0..4).random()

        try{
            var rtaPath = DiskSearcher.finalRtArtPathList[randomNumber].audioFilePath

            if(!rtaPath.isNullOrEmpty()) {
                return rtaPath
            } else { // 위에서 만약 실패했을 경우
                val listFromSharedPref = mySharedPrefManager.getRtaArtPathList()
                rtaPath = listFromSharedPref[randomNumber].audioFilePath
                return rtaPath
            }
        }catch (e: java.lang.Exception) {
            Log.d(TAG, "getDefaultRta: error getting default rta path.. error=$e ")
            return null
        }
    }

    override fun toString(): String {
        Log.d(TAG, "toString: called. id=$id hour=$hour minutes=$minutes // \nalertSoundUri= ${alertSoundUri}, \nalarmtone=${alarmtone.toString()}, \nartFilePath=$artFilePath \nlabel=$label")

        // 여기서
//        val listFromSharedPref = mySharedPrefManager.getRtaArtPathList()
//        rtaPath = listFromSharedPref[randomNumber].audioFilePath

        val box = if (isEnabled) "[x]" else "[ ]"
        return "$id $box $hour:$minutes $daysOfWeek $label"
    }

    fun withId(id: Int): AlarmValue = copy(id = id)
    fun withState(name: String): AlarmValue = copy(state = name)
    fun withIsEnabled(enabled: Boolean): AlarmValue = copy(isEnabled = enabled)
    fun withNextTime(calendar: Calendar): AlarmValue = copy(nextTime = calendar)
    fun withChangeData(data: AlarmValue) = copy(
            id = data.id,
            isEnabled = data.isEnabled,
            hour = data.hour,
            minutes = data.minutes,
            isPrealarm = data.isPrealarm,
            alarmtone = data.alarmtone,
            isVibrate = data.isVibrate,
            label = data.label,
            daysOfWeek = data.daysOfWeek,
            artFilePath = data.artFilePath // 내가 추가.
    )

    fun withLabel(label: String) = copy(label = label)
    fun withHour(hour: Int) = copy(hour = hour)
    fun withDaysOfWeek(daysOfWeek: DaysOfWeek) = copy(daysOfWeek = daysOfWeek)
    fun withIsPrealarm(isPrealarm: Boolean) = copy(isPrealarm = isPrealarm)
}
