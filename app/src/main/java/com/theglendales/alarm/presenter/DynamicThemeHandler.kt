package com.theglendales.alarm.presenter

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.theglendales.alarm.R
import com.theglendales.alarm.alert.AlarmAlertFullScreen

private const val TAG="DynamicThemeHandler"
class DynamicThemeHandler(context: Context) {
    private val themeKey = "theme"
    private val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val synthwave = "synthwave" // 다 안 쓸 예정..
    private val light = "light" // 이것도 안 쓸 예쩡..
    private val dark = "dark"

    init {
        when (sp.getString(themeKey, dark)) {
            light, dark, synthwave -> {}
            else -> {
                sp.edit().putString(themeKey, dark).apply()
            }
        }
    }
    fun defaultTheme(): Int { //dark 로 되있군 현재는 흐음..
        val prefStr = preference()

        var resultInt = -1

        when(prefStr) {
            light -> {resultInt= R.style.DefaultLightTheme}
            dark -> {resultInt= R.style.DefaultDarkTheme}
            synthwave -> {resultInt= R.style.DefaultSynthwaveTheme}
            else -> {resultInt= R.style.DefaultDarkTheme}
        }
        Log.d(TAG, "dT: prefStr=$prefStr, resultInt=$resultInt")
        return resultInt
    }

    /*//원래 써있떤 function. LogD 넣기 위해 위에 방식으로 바꿈.
    fun defaultTheme(): Int = when (preference()) {
        light -> R.style.DefaultLightTheme
        dark -> R.style.DefaultDarkTheme
        synthwave -> R.style.DefaultSynthwaveTheme
        else -> R.style.DefaultDarkTheme
    }*/

    private fun preference(): String = sp.getString(themeKey, dark) ?: dark

    fun getIdForName(name: String): Int = when {
        preference() == light && name == AlarmAlertFullScreen::class.java.name -> R.style.AlarmAlertFullScreenLightTheme
        preference() == light && name == TimePickerDialogFragment::class.java.name -> R.style.TimePickerDialogFragmentLight
        preference() == dark && name == AlarmAlertFullScreen::class.java.name -> R.style.AlarmAlertFullScreenDarkTheme
        preference() == dark && name == TimePickerDialogFragment::class.java.name -> R.style.TimePickerDialogFragmentDark
        preference() == synthwave && name == AlarmAlertFullScreen::class.java.name -> R.style.AlarmAlertFullScreenSynthwaveTheme
        preference() == synthwave && name == TimePickerDialogFragment::class.java.name -> R.style.TimePickerDialogFragmentSynthwave
        else -> defaultTheme()
    }
}
