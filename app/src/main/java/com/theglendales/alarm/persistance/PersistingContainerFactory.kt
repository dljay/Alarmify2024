package com.theglendales.alarm.persistance

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.model.AlarmStore
import com.theglendales.alarm.model.AlarmValue
import com.theglendales.alarm.model.Alarmtone
import com.theglendales.alarm.model.Calendars
import com.theglendales.alarm.model.ContainerFactory
import com.theglendales.alarm.model.DaysOfWeek
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import java.util.Calendar

/**
 * Active record container for all alarm data.
 *
 * @author Yuriy
 */
private const val TAG="PersistingContainerFactory"
class PersistingContainerFactory(private val calendars: Calendars, private val mContext: Context) : ContainerFactory {
    private val subscriptions = mutableMapOf<Int, Disposable>()
    //private val myDiskSearcher: DiskSearcher by globalInject() // 내가 추가

    private fun createStore(initial: AlarmValue): AlarmStore {
        class AlarmStoreIR : AlarmStore {
            private val subject = BehaviorSubject.createDefault(initial)
            override var value: AlarmValue
                get() = requireNotNull(subject.value)
                set(value) {
                    val values: ContentValues = value.createContentValues()
                    val uriWithAppendedId = ContentUris.withAppendedId(Columns.contentUri(), value.id.toLong())
                    mContext.contentResolver.update(uriWithAppendedId, values, null, null)
                    subject.onNext(value)
                }

            override fun observe(): Observable<AlarmValue> = subject.hide()
            override fun delete() {
                subscriptions.remove(value.id)?.dispose()
                val uri = ContentUris.withAppendedId(Columns.contentUri(), value.id.toLong())
                mContext.contentResolver.delete(uri, "", null)
            }
        }

        return AlarmStoreIR()
    }

    override fun create(cursor: Cursor): AlarmStore {
        return createStore(fromCursor(cursor, calendars))
    }

    override fun create(): AlarmStore {
        Log.d(TAG, "create: Creating alarm here!")
        return createStore(create(
                calendars = calendars,
                idMapper = { container ->
                    val inserted: Uri? = mContext.contentResolver.insert(Columns.contentUri(), container.createContentValues())
                    ContentUris.parseId(requireNotNull(inserted)).toInt()
                }
        )).also { container ->
            // persist created container
            container.value = container.value
        }
    }

    private fun fromCursor(c: Cursor, calendars: Calendars): AlarmValue {
        Log.d(TAG, "fromCursor: called! artFilePath=${c.getString(Columns.ALARM_ART_FILE_PATH)}")
        return AlarmValue(
                id = c.getInt(Columns.ALARM_ID_INDEX),
                isEnabled = c.getInt(Columns.ALARM_ENABLED_INDEX) == 1,
                hour = c.getInt(Columns.ALARM_HOUR_INDEX),
                minutes = c.getInt(Columns.ALARM_MINUTES_INDEX),
                daysOfWeek = DaysOfWeek(c.getInt(Columns.ALARM_DAYS_OF_WEEK_INDEX)),
                isVibrate = c.getInt(Columns.ALARM_VIBRATE_INDEX) == 1,
                isPrealarm = c.getInt(Columns.ALARM_PREALARM_INDEX) == 1,
                label = c.getString(Columns.ALARM_MESSAGE_INDEX) ?: "",
                alarmtone = Alarmtone.fromString(c.getString(Columns.ALARM_ALERT_INDEX)),
                state = c.getString(Columns.ALARM_STATE_INDEX),
                nextTime = calendars.now().apply { timeInMillis = c.getLong(Columns.ALARM_TIME_INDEX) },
                artFilePath = c.getString(Columns.ALARM_ART_FILE_PATH) ?: ""

        )
    }

    companion object {
        @JvmStatic
        fun create(calendars: Calendars, idMapper: (AlarmValue) -> Int): AlarmValue {
            Log.d(TAG, "create: 'Companion Obj'")
            val now = calendars.now()

            val defaultActiveRecord = AlarmValue(
                    id = -1,
                    isEnabled = false,
                    hour = now.get(Calendar.HOUR_OF_DAY),
                    minutes = now.get(Calendar.MINUTE),
                    daysOfWeek = DaysOfWeek(0),
                    isVibrate = true,
                    isPrealarm = false,
                    label = "userCreated", // 내가 추가했음!! App install 시 생성되는 알람과 구분짓기 위해!!
                    alarmtone = Alarmtone.Default(),
                    state = "",
                    nextTime = now,
                    artFilePath = null
            )

            //generate a new id
            val id = idMapper(defaultActiveRecord)

            return defaultActiveRecord.copy(id = id)
        }
    }

    private fun AlarmValue.createContentValues(): ContentValues {
        Log.d(TAG, "createContentValues: called")
        return ContentValues(12).apply {
            // id
            put(Columns.ENABLED, isEnabled)
            put(Columns.HOUR, hour)
            put(Columns.MINUTES, minutes)
            put(Columns.DAYS_OF_WEEK, daysOfWeek.coded)
            put(Columns.VIBRATE, isVibrate)
            put(Columns.MESSAGE, label)
            put(Columns.ALERT, alarmtone.persistedString)
            put(Columns.PREALARM, isPrealarm)
            put(Columns.ALARM_TIME, nextTime.timeInMillis)
            put(Columns.STATE, state)
            put(Columns.ART_FILE_PATH, artFilePath)
        }
    }
}
