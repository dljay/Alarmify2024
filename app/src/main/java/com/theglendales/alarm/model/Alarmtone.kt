package com.theglendales.alarm.model

import android.net.Uri
import android.provider.Settings
import android.util.Log

private const val TAG="Alarmtone.kt"
private val defaultAlarmAlertUri = Settings.System.DEFAULT_ALARM_ALERT_URI.toString()

fun Alarmtone.ringtoneManagerString(): Uri? {
    Log.d(TAG, "ringtoneManagerString: this=$this")
    return when (this) {
        is Alarmtone.Silent -> null
        is Alarmtone.Default -> Uri.parse(defaultAlarmAlertUri)
        is Alarmtone.Sound -> Uri.parse(this.uriString)
    }
}

sealed class Alarmtone(open val persistedString: String?) {

    data class Silent(override val persistedString: String? = null) : Alarmtone(persistedString)
    data class Default(override val persistedString: String? = "") : Alarmtone(persistedString)
    data class Sound(val uriString: String) : Alarmtone(uriString)

    companion object {
        fun fromString(string: String?): Alarmtone {
            return when (string) {
                null -> Silent()
                "" -> Default()
                defaultAlarmAlertUri -> Default()
                else -> Sound(string)
            }
        }
    }
}