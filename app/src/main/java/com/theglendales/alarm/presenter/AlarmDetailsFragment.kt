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
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.theglendales.alarm.R
import com.theglendales.alarm.checkPermissions
import com.theglendales.alarm.configuration.Layout
import com.theglendales.alarm.configuration.Prefs
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.configuration.globalLogger
import com.theglendales.alarm.interfaces.IAlarmsManager
import com.theglendales.alarm.jjmvvm.spinner.MyCustomSpinner
import com.theglendales.alarm.jjmvvm.spinner.SpinnerAdapter
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjmvvm.util.RtWithAlbumArt
import com.theglendales.alarm.logger.Logger
import com.theglendales.alarm.lollipop
import com.theglendales.alarm.model.AlarmValue
import com.theglendales.alarm.model.Alarmtone
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Details activity allowing for fine-grained alarm modification
 */
private const val TAG="*AlarmDetailsFragment*"
class AlarmDetailsFragment : Fragment() {
    // 내가 추가 ->
        // 폰에 저장된 ringtone (mp3 or ogg?) 과 앨범쟈켓(png) 을 찾기위해
        private val myDiskSearcher: DiskSearcher by globalInject()
        // Spinner
        private val spinnerAdapter: SpinnerAdapter by globalInject()
        private val spinner: MyCustomSpinner by lazy { fragmentView.findViewById(R.id.id_spinner) as MyCustomSpinner}
        // 링톤 옆에 표시되는 앨범 아트
        private val ivRtArt: ImageView by lazy { fragmentView.findViewById(R.id.iv_ringtoneArtBig) as ImageView}
    // 내가 추가 <-

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
    //private val mRingtoneSummary by lazy { fragmentView.findViewById(R.id.details_ringtone_summary) as TextView }
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


        //View Initializing ->
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
        //View Initializing <-

    // Spinner 설정 ------------>
        spinner.adapter = spinnerAdapter
        spinner.isSelected = false // 이것과
        spinner.setSelection(0,true) // 요것을 통해서 frag 열리자마자 자동으로 ItemSelect 하는것 막음.

        CoroutineScope(IO).launch {
            refreshSpinnerUi()
        }
        // 열리고 닫힐 때 화살표 방향 변경
        spinner.setSpinnerEventsListener(object: MyCustomSpinner.OnSpinnerEventsListener {
            override fun onPopupWindowOpened(spinner: Spinner?) {
                spinner?.background = ResourcesCompat.getDrawable(resources, R.drawable.bg_spinner_arrow_up, null)
            }

            override fun onPopupWindowClosed(spinner: Spinner?) {
                spinner?.background = ResourcesCompat.getDrawable(resources, R.drawable.bg_spinner_arrow_down, null)
            }

        })
        // Spinner 에서 ringtone 을 골랐을 때 실행될 명령어들->

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?,view: View?,position: Int,id: Long) {


                Log.d(TAG, "onItemSelected: position=$position")
                val rtSelected = SpinnerAdapter.rtOnDiskList[position] // position -> SpinnerAdapter.kt 에 있는 rtOnDiskList(하드에 저장된 rt 리스트) 로..

                Log.d(TAG, "onItemSelected: [SPINNER] position=$position, id=$id, title=${rtSelected.rtTitle}, trId= ${rtSelected.trIdStr}, " +
                        "uri = ${rtSelected.uri}")

                // 이제 ringtone 으로 설정 -> 기존 onActivityResult 에 있던 내용들 복붙! -->
                val alert: String? = rtSelected.uri.toString()


               logger.debug { "Got ringtone: $alert" }

               val alarmtone = when (alert) {
                   null -> Alarmtone.Silent() // 선택한 alarm 톤이 a)어떤 오류등으로 null 값일때 -> .Silent()
                   RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString() -> Alarmtone.Default() // b)Default 일때
                   else -> Alarmtone.Sound(alert) // 내가 선택한 놈.
               }
               // 테스트중 <-

               logger.debug { "Spinner- onItemSelected! $alert -> $alarmtone" }

               checkPermissions(requireActivity(), listOf(alarmtone))

               modify("Ringtone picker") { prev ->
                   prev.copy(alarmtone = alarmtone, isEnabled = true)
                }
                // 이제 ringtone 으로 설정 -> 기존 onActivityResult 에 있던 내용들 복붙! <----
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d(TAG, "Spinner - onNothingSelected: ... ")
            }
        }
    // Spinner 설정 <------------

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
                    //Log.d(TAG, "onCreateView: jj- mRingtoneRow.setOnClickListener + Running my DISK Searcher!!! ")

                    //To show a ringtone picker to the user, use the "ACTION_RINGTONE_PICKER" intent to launch the picker.
                    /*startActivityForResult(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, editor.alarmtone.ringtoneManagerString()) // hmm. not sure what this does..

                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))

                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    }, 42)*/
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


// ******** ====> DISK 에 있는 파일들(mp3) 찾고 거기서 mp3, albumArt(bitmap-mp3 안 메타데이터) 리스트를 받는 프로세스 (코루틴으로 실행) ===>
    private suspend fun refreshSpinnerUi() {
        Log.d(TAG, "refreshSpinnerUi: called")
        val resultList = myDiskSearcher.rtAndArtSearcher()
        Log.d(TAG, "refreshSpinnerUi: result=$resultList")
        spinnerAdapter.updateList(resultList) // todo: 이 라인을 밑에 withContext 윗줄에서 실행? or Livedata .. 어떻게든?
        setSpinnerAdapterOnMainThread()
        //setIvArtImgOnMainThread(resultList[2].bitmap)
        //UI 업데이트
    }
    private suspend fun setSpinnerAdapterOnMainThread() {
        Log.d(TAG, "setSpinnerAdapterOnMainThread: called!!**")
        withContext(Main) {
            spinnerAdapter.notifyDataSetChanged()
        }
    }
//    private suspend fun setIvArtImgOnMainThread(bitmapReceived: Bitmap?) {
//        // UI 변경은 오직 MainThread 에서만 가능하므로 여기서 Coroutine 을 IO -> Main 으로 변경!
//        withContext(Main) {
//            setIvArtImage(bitmapReceived)
//        }
//    }
//    private fun setIvArtImage(bitmapReceived: Bitmap?) {
//        ivRtArt.setImageBitmap(bitmapReceived)
//    }

// ***** <==== DISK 에 있는 파일들(mp3) 찾고 거기서 albumArt 메타데이터 복원하는 프로세스 (코루틴으로 위에서 실행)

    // Line 179 에서 Ringtone 선택 후 결과값에 대한 처리를 여기서 해줌 -> 이제는 빈 깡통.. 안 씀.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {if (data != null && requestCode == 42) {}}

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
                .subscribe { prevRtFileName ->
                    //mRingtoneSummary.text = it .. 여기서 spinner 에 기존 설정되어있는 ringtone 보여줄것.
                    Log.d(TAG, "onResume: 기설정된 알람톤 파일이름=$prevRtFileName")
                    // 기존에 설정되어있는 링톤과 동일한 "파일명"을 가진 Rt 의 위치(index) 를 리스트에서 찾아서-> Spinner 에 세팅해주기.
                    val indexPrevChosenRt = SpinnerAdapter.rtOnDiskList.indexOfFirst { rtOnDisk -> rtOnDisk.fileName == prevRtFileName } // 동일한 "파일명" 을 가진 RtWithAlbumArt 를 반환!
                    spinner.setSelection(indexPrevChosenRt)
                    // Update ImageView(Big) - 스피너 옆에 있는 큰 앨범아트 ImageView
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
        // 나갈 때  SpinnerAdapter 에 있는 Glide 없애기? 메모리 이슈? //
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