package com.theglendales.alarm.jjmvvm.util

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import com.theglendales.alarm.R
import com.theglendales.alarm.background.PlayerWrapper
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.configuration.globalLogger
import com.theglendales.alarm.logger.Logger
import com.theglendales.alarm.model.Alarmtone
import com.theglendales.alarm.model.ringtoneManagerString

private const val TAG="CheckUnPlaybleRt"
//Toast Messenger
private val toastMessenger: ToastMessenger by globalInject() //ToastMessenger

//[ListFrag] 열었을 때 -> 현재 설정된 알람 중 -> 재생 불가한 링톤이 있으면.. (파일이 없을때). Alert Dialog 띄워주기 (Ok 버튼 하나만 있고 별 의미 없음)
fun showAlertIfRtIsMissing(activity: Activity, tones: List<Alarmtone>) {


    //.READ_EXTERNAL_STORAGE 는 결국 Media 폴더 Shareable media files(image,audio - 내 음악,files..) 등등 인듯. 우리는 해당사항 없을듯.
    val logger: Logger by globalLogger("checkPermissions")
    // 재생 불가한 링톤이 있는지 여기서 확인하는듯.
    val unplayable = tones
        .filter { alarmtone ->
            runCatching {
                PlayerWrapper(context = activity, resources = activity.resources, log = logger).setDataSource(alarmtone)
            }.isFailure
        }
        .mapNotNull { tone -> RingtoneManager.getRingtone(activity, tone.ringtoneManagerString()) }
        .map { ringtone ->
            runCatching {
                ringtone.getTitle(activity) ?: "null"
            }.getOrDefault("null")
        }
    //재생 불가한 링톤이 있으면.. (파일이 없을때). Request Permission
    if (unplayable.isNotEmpty()) {
        //todo: RT 를 D1 으로 자동 설정?

        //toastMessenger.showMyToast("Detected missing file. Please visit Ringtone tab for auto recovery.",isShort = false)
        Log.d(TAG, "checkUnPlayableRt: --- 현재 설정된 알람의 RT 파일 삭제되었을것임.")
        AlertDialog.Builder(activity).setTitle(activity.getString(R.string.alert_missing_file))
            .setMessage(activity.getString(R.string.missing_ringtone_alert)) // 혹시나 파일명을 string 안에 넣어서 쓰고 싶다면 다음을 추가: unplayable.joinToString(", ") (Permissions.kt 참고)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                //activity.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 3)
            }.show()
    }
}
fun showAlertPlayStoreUnavailable(context: Context, activity: Activity) {
    Log.d(TAG, "showAlertPlayStoreUnavailable: called")
        AlertDialog.Builder(activity).setTitle(activity.getString(R.string.alert_ps_unavailable))
            .setMessage(activity.getString(R.string.ps_error_alert)) // 혹시나 파일명을 string 안에 넣어서 쓰고 싶다면 다음을 추가: unplayable.joinToString(", ") (Permissions.kt 참고)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                try {
                    val playStoreUri = "https://play.google.com"
                    startActivity(context,Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUri)), null)

                } catch (e: ActivityNotFoundException) {
                    //startActivity(context,Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                    Log.d(TAG, "showAlertPlayStoreUnavailable: Failed to Launch Google Play")
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
}

// ******** UTILITY METHODS ******
fun checkIfRtIsUnplayable(activity: Activity, tones: List<Alarmtone>): Boolean {

    //.READ_EXTERNAL_STORAGE 는 결국 Media 폴더 Shareable media files(image,audio - 내 음악,files..) 등등 인듯. 우리는 해당사항 없을듯.
    val logger: Logger by globalLogger("checkPermissions")
    // 재생 불가한 링톤이 있는지 여기서 확인하는듯.
    val unplayable = tones
        .filter { alarmtone ->runCatching {PlayerWrapper(context = activity, resources = activity.resources, log = logger).setDataSource(alarmtone)}.isFailure}
        .mapNotNull { tone -> RingtoneManager.getRingtone(activity, tone.ringtoneManagerString()) }
        .map { ringtone ->runCatching {ringtone.getTitle(activity) ?: "null"}.getOrDefault("null")}

    return unplayable.isNotEmpty() //재생 불가한 링톤이 있으면.. (파일이 없을때). true 를 반환!
}