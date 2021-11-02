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
import android.graphics.drawable.Drawable
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
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.theglendales.alarm.R
import com.theglendales.alarm.checkPermissions
import com.theglendales.alarm.configuration.Layout
import com.theglendales.alarm.configuration.Prefs
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.configuration.globalLogger
import com.theglendales.alarm.interfaces.IAlarmsManager
import com.theglendales.alarm.jjadapters.GlideApp
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
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
        private val ivRtArtBig: ImageView by lazy { fragmentView.findViewById(R.id.iv_ringtoneArtBig) as ImageView}

        private var isRtListReady=false
        //SharedPref
        private val mySharedPrefManager: MySharedPrefManager by globalInject()
    // 내가 추가 <-

    private val alarms: IAlarmsManager by globalInject()
    private val logger: Logger by globalLogger("AlarmDetailsFragment")
    private val prefs: Prefs by globalInject()
    private var disposables = CompositeDisposable()

    private var backButtonSub: Disposable = Disposables.disposed()
    private var disposableDialog = Disposables.disposed()

    private val alarmsListActivity by lazy { activity as AlarmsListActivity }
    private val store: UiStore by globalInject()

    private val rowHolder: RowHolder by lazy { RowHolder(fragmentView.findViewById(R.id.details_list_row_container), alarmId, prefs.layout()) }
    //private val mRingtoneRow by lazy { fragmentView.findViewById(R.id.details_ringtone_row) as LinearLayout }
    //private val mRingtoneSummary by lazy { fragmentView.findViewById(R.id.details_ringtone_summary) as TextView }
    private val mRepeatRow by lazy { fragmentView.findViewById(R.id.details_repeat_row) as LinearLayout }
    private val mRepeatSummary by lazy { fragmentView.findViewById(R.id.details_repeat_summary) as TextView }
// PreAlarm & Label 관련. 내가 없앰.
    //private val mPreAlarmRow by lazy {fragmentView.findViewById(R.id.details_prealarm_row) as LinearLayout}
    //private val mPreAlarmCheckBox by lazy {fragmentView.findViewById(R.id.details_prealarm_checkbox) as CheckBox}
    //private val mLabel: EditText by lazy { fragmentView.findViewById(R.id.details_label) as EditText }

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

//******************* Spinner 설정 ------------>
        spinner.adapter = spinnerAdapter
        spinner.isSelected = false // 이것과
        spinner.setSelection(0,true) // <=frag 열리자마자 자동으로 ItemSelect 하는것 막음. <== 트릭은 아래 spinner 에 selectedListener 를 등록하기 전에 미리 선택! -> 무반응!

//        CoroutineScope(IO).launch {
//            refreshSpinnerUi()
//        }
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

                val rtSelected = SpinnerAdapter.rtOnDiskList[position] // position -> SpinnerAdapter.kt 에 있는 rtOnDiskList(하드에 저장된 rt 리스트) 로..
                Log.d(TAG, "onItemSelected: [SPINNER] position=$position, id=$id, title=${rtSelected.rtTitle}, trId= ${rtSelected.trIdStr}, " +
                        "uri = ${rtSelected.audioFilePath}")

                // 이제 ringtone 으로 설정 -> 기존 onActivityResult 에 있던 내용들 복붙! -->
                val alert: String? = rtSelected.audioFilePath.toString()


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
//****** Spinner 설정 <------------ *************

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
                }.addToDisposables()

        //pre-alarm
//        mPreAlarmRow.setOnClickListener {
//            modify("Pre-alarm") { editor -> editor.copy(isPrealarm = !editor.isPrealarm, isEnabled = true) }
//        }

        mRepeatRow.setOnClickListener {
            editor.firstOrError()
                    .flatMap { editor -> editor.daysOfWeek.showDialog(requireContext()) }
                    .subscribe { daysOfWeek ->
                        modify("Repeat dialog") { prev -> prev.copy(daysOfWeek = daysOfWeek, isEnabled = true) }
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

        //mLabel.addTextChangedListener(TextWatcherIR())

        return view
    }
// <<<<----------onCreateView


// ******** ====> DISK 에 있는 파일들(mp3) 찾고 거기서 mp3, albumArt(bitmap-mp3 안 메타데이터) 리스트를 받는 프로세스 (코루틴으로 실행) ===>
    private fun initSpinner(selectedRtFileName: String) { // 기존에 설정해놓은 알람을 (onResume> disposables.xx 에서) 통보 받으면 이제 refreshSpinner & Big Circle Album Art UI 업뎃을 하자!
        CoroutineScope(IO).launch {refreshSpinnerUi(selectedRtFileName)}

    }
    private suspend fun refreshSpinnerUi(selectedRtFileName: String) {
        Log.d(TAG, "refreshSpinnerUi: called")

    //1) 다운받고->AlarmsList->scan+애니메이션 떠야함 -> DetailsFrag 로 다시 왔는데 리스트업이 안되었다면-> DiskSearcher.kt > onDiskRtSearcher() 진행.
        if(myDiskSearcher.isDiskScanNeeded()) {

            Log.d(TAG, "refreshSpinnerUi: isDiskScanNeeded(O) here")

            val rtOnDiskList = myDiskSearcher.onDiskRtSearcher()
            Log.d(TAG, "refreshSpinnerUi: result=$rtOnDiskList")
            spinnerAdapter.updateList(rtOnDiskList) // ******  이제 디스크에 있는 Rt 찾고, 그래픽 없는 놈 찾아서 디스크에 저장해주는 등 온갖것이 다 되었다는 가정하에! 드디어 UI 업데이트!
            refreshSpinnerAndCircleArt(selectedRtFileName)

        } else {
            Log.d(TAG, "refreshSpinnerUi: isDiskScanNeeded(X)")
            val rtOnDiskList = DiskSearcher.finalRtArtPathList // 현재 companion obj 로 메모리에 떠있는 rt obj 리스트 갖고오기.
            //val resultList = mySharedPrefManager.getRtaArtPathList() <- 이걸로도 대체 가능.

            Log.d(TAG, "refreshSpinnerUi: result=$rtOnDiskList")
            spinnerAdapter.updateList(rtOnDiskList) // ******  이제 디스크에 있는 Rt 찾고, 그래픽 없는 놈 찾아서 디스크에 저장해주는 등 온갖것이 다 되었다는 가정하에! 드디어 UI 업데이트!
            refreshSpinnerAndCircleArt(selectedRtFileName)

        }


    }
    private suspend fun refreshSpinnerAndCircleArt(selectedRtFileName: String) {
        Log.d(TAG, "notifySpinnerAdapterOnMainThread: called!!**")

        withContext(Main) {
        // 1)Spinner Update
            spinnerAdapter.notifyDataSetChanged()
        // 2)스피너 옆 큰 Circle Art 업데이트
            updateCircleAlbumArt(selectedRtFileName)
        // 3) 다 됐으니 최초 실행은 아님을 알려주기.
            isRtListReady = true

        }
    }
    private fun updateCircleAlbumArt(selectedRtFileName: String) {
        Log.d(TAG, "updateCircleAlbumArt: called #$#@% selectedRtFileName=$selectedRtFileName")
        // 2-a) 기존에 설정되어있는 링톤과 동일한 "파일명"을 가진 Rt 의 위치(index) 를 리스트에서 찾아서-> Spinner 에 세팅해주기.
        /** .indexOfFirst (람다식을 충족하는 '첫번째' 대상의 위치를 반환. 없을때는 -1 반환) */

        val indexOfSelectedRt = SpinnerAdapter.rtOnDiskList.indexOfFirst { rtOnDisk -> rtOnDisk.fileName == selectedRtFileName }

        if(indexOfSelectedRt!=-1) // 현재 disk 에 있는 rt list 에서 현재 '설정한(or 되어있던)' rt 를 찾았으면 CircleAlbumArt 보여주기.
        {
            Log.d(TAG, "updateCircleAlbumArt: indexOfSelectedRt=$indexOfSelectedRt, selectedRtForThisAlarm=${SpinnerAdapter.rtOnDiskList[indexOfSelectedRt]}")
            spinner.setSelection(indexOfSelectedRt)
            val selectedRtForThisAlarm: RtWithAlbumArt = SpinnerAdapter.rtOnDiskList[indexOfSelectedRt] // 리스트 업데이트 전에 실행-> indexOfSelectedRt 가 -1 ->  뻑남..
            val artPath = selectedRtForThisAlarm.artFilePathStr
        // 2-b) 잠시! AlarmListFrag 에서 Row 에 보여줄 AlbumArt 의 art Path 수정/저장!  [alarmId, artPath] 가 저장된 Shared Pref(ArtPathForListFrag.xml) 업데이트..
            mySharedPrefManager.saveArtPathForAlarm(alarmId, artPath)


        // 2-c) 스피너 옆에 있는 큰 앨범아트 ImageView 에 현재 설정된 rt 보여주기. Glide 시용 (Context 가 nullable 여서 context?.let 으로 시작함)
        context?.let {
            GlideApp.with(it).load(artPath).circleCrop()
                .error(R.drawable.errordisplay).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.placeholder).listener(object :
                    RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        Log.d(TAG, "onLoadFailed: Glide load failed!. Message: $e")
                        return false
                    }
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        Log.d(TAG,"onResourceReady: Glide loading success! Title=${selectedRtForThisAlarm.rtTitle}, trId: ${selectedRtForThisAlarm.trIdStr}") // debug 결과 절대 순.차.적으로 진행되지는 않음!
                        return false
                    }
                }).into(ivRtArtBig)
            }
        }
    }


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
                    //mPreAlarmCheckBox.isChecked = editor.isPrealarm

                    mRepeatSummary.text = editor.daysOfWeek.summary(requireContext())

//                    if (editor.label != mLabel.text.toString()) {
//                        mLabel.setText(editor.label)
//                    }
                })

        disposables.add(editor.distinctUntilChanged().observeOn(Schedulers.computation()).map { editor ->
                    when (editor.alarmtone) {
                        is Alarmtone.Silent -> {requireContext().getText(R.string.silent_alarm_summary)}
                        is Alarmtone.Default -> {RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)).title()}
                        is Alarmtone.Sound -> {RingtoneManager.getRingtone(context, Uri.parse(editor.alarmtone.uriString)).title()}
                    }
                }.observeOn(AndroidSchedulers.mainThread()).subscribe { selectedRtFileName ->
//** RT 변경 or 최초 DetailsFrag 열릴 때 이쪽으로 들어옴
//***DetailsFrag 에서 설정된 rt를 Spinner 에 보여주기   //mRingtoneSummary.text = it ..
                    Log.d(TAG, "onResume: 설정된 알람톤 파일이름=$selectedRtFileName, alarmId=$alarmId")
                    if(isRtListReady) { // 2) Rt 변경되었을 때  -> Circle albumArt 사진만 업데이트!
                        //Log.d(TAG, "onResume: 2) Rt 임의로 변경되었을 때")



                    //b) DetailsFrag 에 있는 Circle Album Art 변경
                        updateCircleAlbumArt(selectedRtFileName.toString())
                    } else { // 1) DetailsFrag (최초로) 열때 혹은 다른 Frag 갔다왔을 때 여기 무조건 실행됨(rxJava Trigger 때문)
                        //Log.d(TAG, "onResume: 1) rxJava Trigger")
                        initSpinner(selectedRtFileName.toString())
                    }
                })

        //pre-alarm duration, if set to "none", remove the option
        disposables.add(prefs.preAlarmDuration
                .observe()
                .subscribe { value ->
                    //mPreAlarmRow.visibility = if (value.toInt() == -1) View.GONE else View.VISIBLE
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
        Log.d(TAG, "onPause: called...")
        super.onPause()
        disposableDialog.dispose()
        backButtonSub.dispose()
        disposables.dispose()
        // 나갈 때  SpinnerAdapter 에 있는 Glide 없애기? 메모리 이슈? //
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: called!!")
        super.onDestroy()
//        try {
//            Runtime.getRuntime().gc() // Garbage collection . But 모든 variable 이 이미 null 값이 되어있어야 효과있음!!!
//
//        }catch (e: java.lang.Exception) {
//            Log.d(TAG, "onDestroy: unable to run .gc() e=$e")
//        }

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