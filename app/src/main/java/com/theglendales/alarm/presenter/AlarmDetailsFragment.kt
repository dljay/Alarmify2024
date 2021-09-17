/*
 * Copyright (C) 2017 Yuriy Kulikov yuriy.kulikov.87@gmail.com
 * Copyright (C) 2012 Yuriy Kulikov yuriy.kulikov.87@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theglendales.alarm.presenter

import android.annotation.TargetApi
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.Transition
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.theglendales.alarm.R
import com.theglendales.alarm.checkPermissions
import com.theglendales.alarm.configuration.Layout
import com.theglendales.alarm.configuration.Prefs
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.configuration.globalLogger
import com.theglendales.alarm.interfaces.IAlarmsManager
import com.theglendales.alarm.logger.Logger
import com.theglendales.alarm.lollipop
import com.theglendales.alarm.model.AlarmValue
import com.theglendales.alarm.model.Alarmtone
import com.theglendales.alarm.model.ringtoneManagerString
import com.theglendales.alarm.util.Optional
import com.theglendales.alarm.util.modify
import com.theglendales.alarm.view.showDialog
import com.theglendales.alarm.view.summary
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import java.util.Calendar

/**
 * Details activity allowing for fine-grained alarm modification
 */
private const val TAG="*AlarmDetailsFragment*"
class AlarmDetailsFragment : Fragment() {
    private val alarms: IAlarmsManager by globalInject()
    private val logger: Logger by globalLogger("AlarmDetailsFragment")
    private val prefs: Prefs by globalInject()
    private var disposables = CompositeDisposable()

    private var backButtonSub: Disposable = Disposables.disposed()
    private var disposableDialog = Disposables.disposed()

    private val alarmsListActivity by lazy { activity as AlarmsListActivity }
    private val store: UiStore by globalInject()
    private val mLabel: EditText by lazy { fragmentView.findViewById(R.id.details_label) as EditText }
    private val rowHolder: RowHolder by lazy { RowHolder(fragmentView.findViewById(R.id.details_list_row_container), alarmId, prefs.layout()) }
    private val mRingtoneRow by lazy { fragmentView.findViewById(R.id.details_ringtone_row) as LinearLayout }
    private val mRingtoneSummary by lazy { fragmentView.findViewById(R.id.details_ringtone_summary) as TextView }
    private val mRepeatRow by lazy { fragmentView.findViewById(R.id.details_repeat_row) as LinearLayout }
    private val mRepeatSummary by lazy { fragmentView.findViewById(R.id.details_repeat_summary) as TextView }
    private val mPreAlarmRow by lazy {
        fragmentView.findViewById(R.id.details_prealarm_row) as LinearLayout
    }
    private val mPreAlarmCheckBox by lazy {
        fragmentView.findViewById(R.id.details_prealarm_checkbox) as CheckBox
    }

    private val editor: Observable<AlarmValue> by lazy { store.editing().filter { it.value.isPresent() }.map { it.value.get() } }

    private val alarmId: Int by lazy { store.editing().value!!.id }

    private lateinit var fragmentView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: jj-called")
        lollipop {
            hackRippleAndAnimation()
        }
    }

//onCreateView ---------->>>
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
    {
        Log.d(TAG, "onCreateView: jj-called")
        logger.debug { "$this with ${store.editing().value}" }

        val view = inflater.inflate(
                when (prefs.layout()) {
                    Layout.CLASSIC -> R.layout.details_fragment_classic
                    Layout.COMPACT -> R.layout.details_fragment_compact
                    else -> R.layout.details_fragment_bold
                },
                container,
                false
        )
        this.fragmentView = view

        rowHolder.run {
            this.container.setOnClickListener {
                modify("onOff") { editor ->
                    editor.copy(isEnabled = !editor.isEnabled)
                }
            }

            // detailsButton().visibility = View.INVISIBLE
            daysOfWeek.visibility = View.INVISIBLE
            label.visibility = View.INVISIBLE

            lollipop {
                this.digitalClock.transitionName = "clock$alarmId"
                this.container.transitionName = "onOff$alarmId"
                this.detailsButton.transitionName = "detailsButton$alarmId"
            }

            digitalClock.setLive(false)
            digitalClockContainer.setOnClickListener {
                disposableDialog = TimePickerDialogFragment.showTimePicker(alarmsListActivity.supportFragmentManager).subscribe(pickerConsumer)
            }

            rowView.setOnClickListener {
                saveAlarm()
            }
        }

        view.findViewById<View>(R.id.details_activity_button_save).setOnClickListener { saveAlarm() }
        view.findViewById<View>(R.id.details_activity_button_revert).setOnClickListener { revert() }

    //!! 여기서 subscribe?? !!
        store.transitioningToNewAlarmDetails().firstOrError().subscribe { isNewAlarm ->
                    Log.d(TAG, "onCreateView: jj-!!inside .subscribe-1")
                    if (isNewAlarm) {
                        Log.d(TAG, "onCreateView: jj-!!inside .subscribe-2")
                        store.transitioningToNewAlarmDetails().onNext(false)
                        disposableDialog = TimePickerDialogFragment.showTimePicker(alarmsListActivity.supportFragmentManager)
                                .subscribe(pickerConsumer)
                    }
                }
                .addToDisposables()

        //pre-alarm
        mPreAlarmRow.setOnClickListener {
            modify("Pre-alarm") { editor -> editor.copy(isPrealarm = !editor.isPrealarm, isEnabled = true) }
        }

        mRepeatRow.setOnClickListener {
            editor.firstOrError()
                    .flatMap { editor -> editor.daysOfWeek.showDialog(requireContext()) }
                    .subscribe { daysOfWeek ->
                        modify("Repeat dialog") { prev -> prev.copy(daysOfWeek = daysOfWeek, isEnabled = true) }
                    }
        }
// Alarm 링톤 리스트 쭈~욱 뜨는 곳. -> 여기서 뭔가를 선택하면-> startActivityForResult 이므로-> 아래 Line 222 onActivityResult 로 감.
        mRingtoneRow.setOnClickListener {
            editor.firstOrError().subscribe { editor ->
                try {
                    Log.d(TAG, "onCreateView: jj- mRingtoneRow.setOnClickListener.. ")
                    //To show a ringtone picker to the user, use the "ACTION_RINGTONE_PICKER" intent to launch the picker.
                    startActivityForResult(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, editor.alarmtone.ringtoneManagerString())

                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))

                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    }, 42)
                } catch (e: Exception) {
                    Toast.makeText(context, requireContext().getString(R.string.details_no_ringtone_picker), Toast.LENGTH_LONG)
                            .show()
                }
            }
        }

        class TextWatcherIR : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                editor.take(1) // take (RxJava) = take the number of elements you specified (n 번째까지만 받고 나머지는 무시)
                        .filter { it.label != s.toString() }
                        .subscribe {
                            modify("Label") { prev -> prev.copy(label = s.toString(), isEnabled = true) }
                        }
                        .addToDisposables()
            }
        }

        mLabel.addTextChangedListener(TextWatcherIR())

        return view
    }
// <<<<----------onCreateView

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && requestCode == 42) {
            val alert: String? = data.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)?.toString()

            logger.debug { "Got ringtone: $alert" }

            val alarmtone = when (alert) {
                null -> Alarmtone.Silent()
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString() -> Alarmtone.Default()
                else -> Alarmtone.Sound(alert)
            }

            logger.debug { "onActivityResult $alert -> $alarmtone" }

            checkPermissions(requireActivity(), listOf(alarmtone))

            modify("Ringtone picker") { prev ->
                prev.copy(alarmtone = alarmtone, isEnabled = true)
            }
        }
    }

    override fun onResume() {
        Log.d(TAG, "onResume: *here we have backButtonSub")
        super.onResume()
        disposables = CompositeDisposable()

        disposables.add(editor
                .distinctUntilChanged() // DistinctUntilChanged: 중복방 지 ex) Dog-Cat-Cat-Dog => Dog-Cat-Dog
                .subscribe { editor ->
                    rowHolder.digitalClock.updateTime(Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, editor.hour)
                        set(Calendar.MINUTE, editor.minutes)
                    })

                    rowHolder.onOff.isChecked = editor.isEnabled
                    mPreAlarmCheckBox.isChecked = editor.isPrealarm

                    mRepeatSummary.text = editor.daysOfWeek.summary(requireContext())

                    if (editor.label != mLabel.text.toString()) {
                        mLabel.setText(editor.label)
                    }
                })
        // 경로 변경 테스트-->

        // 경로 변경 테스트 <--
        disposables.add(editor
                .distinctUntilChanged()
                .observeOn(Schedulers.computation())
                .map { editor ->
                    when (editor.alarmtone) {
                        is Alarmtone.Silent -> {requireContext().getText(R.string.silent_alarm_summary)}
                        is Alarmtone.Default -> {RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)).title()}
                        is Alarmtone.Sound -> {RingtoneManager.getRingtone(context, Uri.parse(editor.alarmtone.uriString)).title()}
                    }
                }.observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    mRingtoneSummary.text = it
                })

        //pre-alarm duration, if set to "none", remove the option
        disposables.add(prefs.preAlarmDuration
                .observe()
                .subscribe { value ->
                    mPreAlarmRow.visibility = if (value.toInt() == -1) View.GONE else View.VISIBLE
                })

        backButtonSub = store.onBackPressed().subscribe {
            Log.d(TAG, "onResume(Line288): backButtonSub=store.onBackPressed().subscribe{} .. ")
            saveAlarm() }

        store.transitioningToNewAlarmDetails().onNext(false)
    }

    fun Ringtone?.title(): CharSequence {
        return try {
            this?.getTitle(requireContext())
                    ?: requireContext().getText(R.string.silent_alarm_summary)
        } catch (e: Exception) {
            requireContext().getText(R.string.silent_alarm_summary)
        }
    }

    override fun onPause() {
        super.onPause()
        disposableDialog.dispose()
        backButtonSub.dispose()
        disposables.dispose()
    }

    private fun saveAlarm() {
        editor.firstOrError().subscribe { editorToSave ->
            alarms.getAlarm(alarmId)?.run {
                edit { withChangeData(editorToSave) }
            }
            store.hideDetails(rowHolder)
        }.addToDisposables()
    }

    private fun revert() {

        store.editing().value?.let { edited ->
            Log.d(TAG, "(line322)revert: jj- ") // // 알람List -> Detail(...) 클릭-> cancel 클릭-> 여기 log 뜸!!!
            // "Revert" on a newly created alarm should delete it.
            if (edited.isNew) {
                alarms.getAlarm(edited.id)?.delete()
                Log.d(TAG, "(line326)revert: jj- edited.isNew")
            }
            // else do not save changes
            store.hideDetails(rowHolder)
        }
    }

    private val pickerConsumer = { picked: Optional<PickedTime> ->
        if (picked.isPresent()) {
            modify("Picker") { editor: AlarmValue ->
                editor.copy(hour = picked.get().hour,
                        minutes = picked.get().minute,
                        isEnabled = true)
            }
        }
    }

    private fun modify(reason: String, function: (AlarmValue) -> AlarmValue) {
        logger.debug { "Performing modification because of $reason" }
        store.editing().modify {
            copy(value = value.map { function(it) })
        }
    }

    private fun Disposable.addToDisposables() {
        disposables.add(this)
    }

    /**
     * his nice hack here is required, because if you do this in XML it will break transitions of the first list row
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun hackRippleAndAnimation() {
        if (enterTransition is Transition) {
            (enterTransition as Transition).addListener(object : Transition.TransitionListener {
                override fun onTransitionEnd(transition: Transition?) {
                    activity?.let { parentActivity ->
                        val selectableItemBackground = TypedValue().apply {
                            parentActivity.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
                        }.resourceId
                        rowHolder.rowView.setBackgroundResource(selectableItemBackground)
                    }
                    (enterTransition as Transition).removeListener(this)
                }

                override fun onTransitionResume(transition: Transition?) {
                }

                override fun onTransitionPause(transition: Transition?) {
                }

                override fun onTransitionCancel(transition: Transition?) {
                }

                override fun onTransitionStart(transition: Transition?) {
                }
            })
        }
    }
}