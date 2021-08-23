package com.theglendales.alarm.configuration

import android.app.Application
import android.util.Log
import android.view.ViewConfiguration
import androidx.preference.PreferenceManager
import com.theglendales.alarm.R
import com.theglendales.alarm.alert.BackgroundNotifications
import com.theglendales.alarm.background.AlertServicePusher
import com.theglendales.alarm.bugreports.BugReporter
import com.theglendales.alarm.createNotificationChannels
import com.theglendales.alarm.model.Alarms
import com.theglendales.alarm.model.AlarmsScheduler
import com.theglendales.alarm.presenter.ScheduledReceiver
import com.theglendales.alarm.presenter.ToastPresenter



private const val TAG="AlarmApplication"
class AlarmApplication : Application() {
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

        // must be started the last, because otherwise we may loose intents from it.
        val alarmsLogger = koin.logger("Alarms")
        alarmsLogger.debug { "Starting alarms" }
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
}
