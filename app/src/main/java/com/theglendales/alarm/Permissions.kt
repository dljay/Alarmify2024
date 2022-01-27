package com.theglendales.alarm

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import com.theglendales.alarm.background.PlayerWrapper
import com.theglendales.alarm.configuration.globalLogger
import com.theglendales.alarm.logger.Logger
import com.theglendales.alarm.model.Alarmtone
import com.theglendales.alarm.model.ringtoneManagerString

/**
 * Checks if all ringtones can be played, and requests permissions if it is not the case
 */
private const val TAG="checkPermissions"


// [기존 코드] 재생 불가한 파일이 있는것을 permission 관련 이슈라 (기존 개발자가) 판단하여-> Permission 관련 Alert 창을 보여줬음.
// [수정 코드] CheckUnPlayble.kt()> checkUnPlayableRt() 로 수정&사용
fun checkPermissions(activity: Activity, tones: List<Alarmtone>) {
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
        try {
            Log.d(TAG, "checkPermissions: unplayble[List/Str] is not empty = $unplayable")

            AlertDialog.Builder(activity).setTitle(activity.getString(R.string.alert))
                    .setMessage(activity.getString(R.string.permissions_external_storage_text, unplayable.joinToString(", "))) // Message = It seems that we cannot play ...
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        activity.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 3)
                        //todo: 내가 설치해놓은 PemissionHandler.kt 와 중복일수도..
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        } catch (e: Exception) {
            logger.e("Was not able to show dialog to request permission, continue without the dialog")
            activity.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 3)
        }
    }
}

