package com.theglendales.alarm.configuration

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import android.os.Vibrator
import android.telephony.TelephonyManager
import com.theglendales.alarm.alert.BackgroundNotifications
import com.theglendales.alarm.background.AlertServicePusher
import com.theglendales.alarm.background.KlaxonPlugin
import com.theglendales.alarm.background.PlayerWrapper
import com.theglendales.alarm.bugreports.BugReporter
import com.theglendales.alarm.interfaces.IAlarmsManager
import com.theglendales.alarm.jjadapters.MyNetWorkChecker
import com.theglendales.alarm.jjfirebaserepo.FirebaseRepoClass
import com.theglendales.alarm.jjmvvm.permissionAndDownload.MyPermissionHandler
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import com.theglendales.alarm.jjmvvm.helper.VHolderUiHandler
import com.theglendales.alarm.jjmvvm.spinner.SpinnerAdapter
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjongadd.TimePickerJjong
import com.theglendales.alarm.logger.LogcatLogWriter
import com.theglendales.alarm.logger.Logger
import com.theglendales.alarm.logger.LoggerFactory
import com.theglendales.alarm.logger.StartupLogWriter
import com.theglendales.alarm.model.AlarmCore
import com.theglendales.alarm.model.AlarmCoreFactory
import com.theglendales.alarm.model.AlarmSetter
import com.theglendales.alarm.model.AlarmStateNotifier
import com.theglendales.alarm.model.Alarms
import com.theglendales.alarm.model.AlarmsScheduler
import com.theglendales.alarm.model.Calendars
import com.theglendales.alarm.model.ContainerFactory
import com.theglendales.alarm.model.IAlarmsScheduler
import com.theglendales.alarm.persistance.DatabaseQuery
import com.theglendales.alarm.persistance.PersistingContainerFactory
import com.theglendales.alarm.presenter.AlarmsListActivity
import com.theglendales.alarm.presenter.DynamicThemeHandler
import com.theglendales.alarm.presenter.ScheduledReceiver
import com.theglendales.alarm.presenter.ToastPresenter
import com.theglendales.alarm.stores.SharedRxDataStoreFactory
import com.theglendales.alarm.util.Optional
import com.theglendales.alarm.wakelock.WakeLockManager
import com.theglendales.alarm.wakelock.Wakelocks
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject


import org.koin.core.Koin
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module
import java.util.ArrayList
import java.util.Calendar

fun Scope.logger(tag: String): Logger {
    return get<LoggerFactory>().createLogger(tag)
}

fun Koin.logger(tag: String): Logger {
    return get<LoggerFactory>().createLogger(tag)
}

fun startKoin(context: Context): Koin {
    // The following line triggers the initialization of ACRA



    val module = module {
        single<DynamicThemeHandler> { DynamicThemeHandler(get()) }
        single<StartupLogWriter> { StartupLogWriter.create() }
        single<LoggerFactory> {
            Logger.factory(
                    LogcatLogWriter.create(),
                    get<StartupLogWriter>()
            )
        }
        single<BugReporter> { BugReporter(logger("BugReporter"), context, lazy { get<StartupLogWriter>() }) }
        factory<Context> { context }
        factory(named("dateFormatOverride")) { "none" }
        factory<Single<Boolean>>(named("dateFormat")) {
            Single.fromCallable {
                get<String>(named("dateFormatOverride")).let { if (it == "none") null else it.toBoolean() }
                        ?: android.text.format.DateFormat.is24HourFormat(context)
            }
        }

        single<Prefs> {
            val factory = SharedRxDataStoreFactory.create(get(), logger("preferences"))
            Prefs.create(get(named("dateFormat")), factory)
        }

        single<Store> {
            Store(
                    alarmsSubject = BehaviorSubject.createDefault(ArrayList()),
                    next = BehaviorSubject.createDefault<Optional<Store.Next>>(Optional.absent()),
                    sets = PublishSubject.create(),
                    events = PublishSubject.create())
        }

        factory { get<Context>().getSystemService(Context.ALARM_SERVICE) as AlarmManager }
        single<AlarmSetter> { AlarmSetter.AlarmSetterImpl(logger("AlarmSetter"), get(), get()) }
        factory { Calendars { Calendar.getInstance() } }
        single<AlarmsScheduler> { AlarmsScheduler(get(), logger("AlarmsScheduler"), get(), get(), get()) }
        factory<IAlarmsScheduler> { get<AlarmsScheduler>() }
        single<AlarmCore.IStateNotifier> { AlarmStateNotifier(get()) }
        single<ContainerFactory> { PersistingContainerFactory(get(), get()) }
        factory { get<Context>().contentResolver }
        single<DatabaseQuery> { DatabaseQuery(get(), get()) }
        single<AlarmCoreFactory> { AlarmCoreFactory(logger("AlarmCore"), get(), get(), get(), get(), get()) }
        single<Alarms> { Alarms(get(), get(), get(), get(), logger("Alarms")) }
        factory<IAlarmsManager> { get<Alarms>() }
        single { ScheduledReceiver(get(), get(), get(), get()) }
        single { ToastPresenter(get(), get()) }
        single { AlertServicePusher(get(), get(), get(), logger("AlertServicePusher")) }
        single { BackgroundNotifications(get(), get(), get(), get(), get()) }
        factory<Wakelocks> { get<WakeLockManager>() }
        factory<Scheduler> { AndroidSchedulers.mainThread() }
        single<WakeLockManager> { WakeLockManager(logger("WakeLockManager"), get()) }
        factory { get<Context>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
        factory { get<Context>().getSystemService(Context.POWER_SERVICE) as PowerManager }
        factory { get<Context>().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager }
        factory { get<Context>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
        factory { get<Context>().getSystemService(Context.AUDIO_SERVICE) as AudioManager }
        factory { get<Context>().resources }

        // 내가 추가 -->
        single<MyNetWorkChecker> { MyNetWorkChecker(context = context)}
        single<VHolderUiHandler> { VHolderUiHandler()}
        single<FirebaseRepoClass> { FirebaseRepoClass()}
        single<MySharedPrefManager> {MySharedPrefManager(context = context)}
        single<DiskSearcher> { DiskSearcher(context = context)}
        single<SpinnerAdapter> { SpinnerAdapter(context = context) }
        single<TimePickerJjong> {TimePickerJjong()}

        // 내가 추가 <--

        factory(named("volumePreferenceDemo")) {
            KlaxonPlugin(
                    log = logger("VolumePreference"),
                    playerFactory = { PlayerWrapper(get(), get(), logger("VolumePreference")) },
                    prealarmVolume = get<Prefs>().preAlarmVolume.observe(),
                    fadeInTimeInMillis = Observable.just(100),
                    inCall = Observable.just(false),
                    scheduler = get()
            )
        }
    }


    return startKoin {
        modules(module)
        modules(AlarmsListActivity.uiStoreModule)
    }.koin
}

fun overrideIs24hoursFormatOverride(is24hours: Boolean) {
    loadKoinModules(module = module(override = true) {
        factory(named("dateFormatOverride")) { is24hours.toString() }
    })
}