package com.theglendales.alarm

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import com.theglendales.alarm.background.PlayerWrapper
import com.theglendales.alarm.configuration.globalLogger
import com.theglendales.alarm.logger.Logger
import com.theglendales.alarm.model.Alarmtone
import com.theglendales.alarm.model.ringtoneManagerString

/**
 * Checks if all ringtones can be played, and requests permissions if it is not the case
 */
fun checkPermissions(activity: Activity, tones: List<Alarmtone>) {
    if (Build.VERSION.SDK_INT >= 23 && activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
        //재생 불가한 링톤이 있으면.. Request Permission
        if (unplayable.isNotEmpty()) {
            try {
                AlertDialog.Builder(activity).setTitle(activity.getString(R.string.alert))
                        .setMessage(activity.getString(R.string.permissions_external_storage_text, unplayable.joinToString(", ")))
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
}