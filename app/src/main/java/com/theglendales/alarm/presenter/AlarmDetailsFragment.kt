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
import android.app.Activity.RESULT_OK
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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
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
import com.theglendales.alarm.jjongadd.TimePickerJjong
import com.theglendales.alarm.logger.Logger
import com.theglendales.alarm.lollipop
import com.theglendales.alarm.model.AlarmValue
import com.theglendales.alarm.model.Alarmtone
import com.theglendales.alarm.util.Optional
import com.theglendales.alarm.util.modify
import com.theglendales.alarm.view.onChipDayClicked

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
private const val REQ_CODE_FOR_RTPICKER = 588 // RTPICKER Intent 관련 (1)
private const val PICKER_RESULT_RT_TITLE="RtTitle" // RTPICKER Intent 관련 (2)
private const val PICKER_RESULT_AUDIO_PATH="AudioPath" // RTPICKER Intent 관련 (3)
private const val PICKER_RESULT_ART_PATH="ArtPath" // RTPICKER Intent 관련 (4)


class AlarmDetailsFragment : Fragment() {
    // 내가 추가 ->
        // 폰에 저장된 ringtone (mp3 or ogg?) 과 앨범쟈켓(png) 을 찾기위해
            private val myDiskSearcher: DiskSearcher by globalInject()
        // Spinner
            private val spinnerAdapter: SpinnerAdapter by globalInject()
            private val spinner: MyCustomSpinner by lazy { fragmentView.findViewById(R.id.id_spinner) as MyCustomSpinner}
        // 링톤 옆에 표시되는 앨범 아트
            private val ivRtArtBig: ImageView by lazy { fragmentView.findViewById(R.id.iv_ringtoneArtBig) as ImageView}

            private var isDropDownSpinnerReady=false
        //SharedPref
            private val mySharedPrefManager: MySharedPrefManager by globalInject()
        //Time Picker (material design)
            private val myTimePickerJjong: TimePickerJjong by globalInject()
        //요일 표시 ChipGroup + TextView
            lateinit var chipGroupDays: ChipGroup
            private val tv_repeatDaysSum by lazy { fragmentView.findViewById(R.id.details_repeat_sum_jj) as TextView }
        // RtPicker Test
            private val tvRtPicker by lazy { fragmentView.findViewById(R.id.tv_RtPicker_Test) as TextView }


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
    private val timePickerSpinner by lazy { fragmentView.findViewById(R.id._tPicker_jj_Spinner) as TimePicker }//<-내가 추가 timePicker Spinner
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

//******************* RT 보여주는 Spinner 설정 ------------>
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
                val alertSoundPath: String? = rtSelected.audioFilePath.toString()


               logger.debug { "Got ringtone: $alertSoundPath" }

               val alarmtone: Alarmtone = when (alertSoundPath) {
                   null -> Alarmtone.Silent() // 선택한 alarm 톤이 a)어떤 오류등으로 null 값일때 -> .Silent()
                   RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString() -> Alarmtone.Default() // b)Default 일때
                   else -> Alarmtone.Sound(alertSoundPath) // 내가 선택한 놈.
               }
               // 테스트중 <-

               logger.debug { "Spinner- onItemSelected! $alertSoundPath -> $alarmtone" }

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
//****** RT 보여주는 Spinner 설정 <------------ *************
        // RTPicker Test -- >
        tvRtPicker.setOnClickListener {
        val intent = Intent(requireActivity(), RtPickerActivity::class.java) //  현재 Activity 에서 -> RtPicker_Test1 Activity 로 이동.
        startActivityForResult(intent, REQ_CODE_FOR_RTPICKER)
        }

        // RtPicker Test <--
        rowHolder.run {
            this.container.setOnClickListener {
                modify("onOff") { editor ->
                    editor.copy(isEnabled = !editor.isEnabled)
                }
            }

            // detailsButton().visibility = View.INVISIBLE
            //daysOfWeek.visibility = View.INVISIBLE
            //label.visibility = View.INVISIBLE


            lollipop {
                this.digitalClock.transitionName = "clock$alarmId"
                this.container.transitionName = "onOff$alarmId"
                this.detailsButton.transitionName = "detailsButton$alarmId"
            }

            digitalClock.setLive(false)
            // 원래 rowHolder 를 빌려서 채워줬던 view (linear layout) 자체를 visibility=Gone 으로 해버림. 아래는 필요없게됨.
//            digitalClockContainer.setOnClickListener {
//                disposableDialog = myTimePickerJjong.showTimePicker(alarmsListActivity.supportFragmentManager).subscribe(pickerConsumer)
//            }


            rowView.setOnClickListener {
                saveAlarm()

            }
        // 내가 추가-> 스위치 파트&시계 파트 둘다 아예 &&안보이게&& 만들기!
//            digitalClockContainer.visibility=View.GONE
//            onOff.visibility=View.GONE

        // 내가 추가<--

        } // rowHolder.run <--

    //TimePicker Spinner 설정 및 시간 골랐을 때 시스템과 연결해주는 부분 --->
        timePickerSpinner.setIs24HourView(false) // amp Pm 시스템으로
        //기존에 설정된 알람 시간은 밑에 onResume() >disposables.. 라인 410(?) 언저리 에서 해줬음!! 대 성공!!!

        timePickerSpinner.setOnTimeChangedListener { view, hourOfDay, minute ->
            Log.d(TAG, "onCreateView: Hour=$hourOfDay, minute=$minute")
            val pickedTime: PickedTime = PickedTime(hourOfDay, minute)
            disposableDialog = myTimePickerJjong.timePickerSpinnerTracker(hourOfDay,minute).subscribe(pickerConsumer)
            //disposableDialog = myTimePickerJjong.showTimePicker(alarmsListActivity.supportFragmentManager).subscribe(pickerConsumer) // 기존 material TimePicker 구현용도.
        }

    //TimePicker Spinner 로 시간 골랐을 때 시스템과 연결해주는 부분 <---

        view.findViewById<View>(R.id.details_activity_button_save).setOnClickListener { saveAlarm() }
        view.findViewById<View>(R.id.details_activity_button_revert).setOnClickListener { revert() }

    //** 신규 알람 생성할때 TimePicker 보여주는 것. (기존에는 TimePickerDialogFragment.showxx() 였지만 -> myTimePickerJjong.. 으로 바꿈.)
        store.transitioningToNewAlarmDetails().firstOrError().subscribe { isNewAlarm ->
                    Log.d(TAG, "onCreateView: jj-!!subscribe-1")


        //** a)신규 User 가 알람 생성시 =>  RT를 "현재 사용 가능한 RT 중에서 Random 으로 골라주기"

                    if (isNewAlarm) {
                    // 현재 가용 가능한 RT 리스트 (스피너의 드랍다운 메뉴) 갯수를 파악하여 그중 하나 random! 으로 골라주기!
                        val availableRtCount= SpinnerAdapter.rtOnDiskList.size
                        var rndRtPos = (0..availableRtCount).random()
                        if(rndRtPos == availableRtCount && rndRtPos >= 0 ) {rndRtPos = 0 } // ex. 총 갯수가 5개인데 5번이 뽑히면 안되니깐..

                        spinner.adapter=spinnerAdapter
                        spinner.setSelection(rndRtPos,true) // 이 순간 editor.alarm = Alarmtone.Default 에서 -> Sound 타입이 되버림!

                        Log.d(TAG, "onCreateView: jj-!!subscribe-2 NEW ALARM SETUP. rndRtPos=$rndRtPos")


                        store.transitioningToNewAlarmDetails().onNext(false)
                        disposableDialog =
                            //TimePickerDialogFragment.showTimePicker(alarmsListActivity.supportFragmentManager) <- 기존 timePicker  코드
                            myTimePickerJjong.showMaterialTimePicker(alarmsListActivity.supportFragmentManager).subscribe(pickerConsumer)

                    }

                }.addToDisposables()

        //pre-alarm
//        mPreAlarmRow.setOnClickListener {
//            modify("Pre-alarm") { editor -> editor.copy(isPrealarm = !editor.isPrealarm, isEnabled = true) }
//        }
    // 설정된 알람(Repeat) ChipGroup 안에서 요일을 선택 -> DaysOfWeek.onChipDayClicked() -> rxjava Single 을 만들고 -> 그것을 editor 가 subscribe!
        chipGroupDays = fragmentView.findViewById(R.id._chipGroupDays)
        for(i in 0 until chipGroupDays.childCount) {
            val chipDay: Chip = chipGroupDays.getChildAt(i) as Chip
            chipDay.setOnCheckedChangeListener { _, isChecked ->
                val whichInt = createWhichIntFromTickedChip(chipDay.id)
// ** Subscribe 미리 된 상태에서-> chip 변화 -> onChipDayClicked..
                val subscribe = editor.firstOrError()
                    .flatMap { editor -> editor.daysOfWeek.onChipDayClicked(whichInt, isChecked) }
                    .subscribe { daysOfWeek ->modify("Repeat dialog") { prev ->prev.copy(daysOfWeek = daysOfWeek,isEnabled = true)}
                        Log.d(TAG,"onCreateView: daysOfWeekJJ_new=$daysOfWeek, whichInt=$whichInt, isChecked=$isChecked")
                    }
            }
        }

//        mRepeatRow.setOnClickListener {
//            editor.firstOrError()
//                    .flatMap { editor -> editor.daysOfWeek.showDialog(requireContext()) }
//                    .subscribe { daysOfWeek ->
//                        modify("Repeat dialog") { prev -> prev.copy(daysOfWeek = daysOfWeek, isEnabled = true) }
//                        Log.d(TAG, "onCreateView: daysOfWeekJJ_old=$daysOfWeek")
//                    }
//        }

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

    // !!*** APP 설치 중 설정된 알람일 경우 -> 무조건 defaultRta1 로 설정해주고(spinner.setSelection 이용) -> alarm.label 값은 "userCreated" 로 바꿔서 -> 다음부터는 여기에 걸리지 않게끔.
        if(alarms.getAlarm(alarmId)!!.labelOrDefault!="userCreated") {
            Log.d(TAG, "onCreateView: **THIS ALARM WAS CREATED DURING APP INSTALLATION")

            spinner.adapter = spinnerAdapter
            spinner.setSelection(0,true)

            modify("Label") {prev -> prev.copy(label = "userCreated", isEnabled = true)}
        }

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
            isDropDownSpinnerReady = true

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
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && requestCode == REQ_CODE_FOR_RTPICKER) {
            if(resultCode == RESULT_OK) { // RESULT_OK == -1 임!!
                val rtTitleFromIntent = data.getStringExtra(PICKER_RESULT_RT_TITLE) // 결과 없을때 default Result 값은 'null'
                val rtaPathFromIntent = data.getStringExtra(PICKER_RESULT_AUDIO_PATH) // 결과 없을때 default Result 값은 'null'
                val artPathFromIntent = data.getStringExtra(PICKER_RESULT_ART_PATH) // 결과 없을때 default Result 값은 'null'

                Log.d(TAG, "onActivityResult: ----[FROM INTENT] Selected RT_Title=$rtTitleFromIntent, AudioPath=$rtaPathFromIntent, ArtPath=$artPathFromIntent")
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
                    //old) 기존 Digital Clock 에 설정된 알람 시간을 그대로 보여주는 기능 2줄. (RowHolder 칸을 빌려서 DetailsFrag 에 보여줄 때)
//                        set(Calendar.HOUR_OF_DAY, editor.hour)
//                        set(Calendar.MINUTE, editor.minutes)

                    //new) ** TimePickerSpinner 에 "기존에 설정된 알람 시간"을 그대로 보여주기!!!! 대성공!!=>
                        timePickerSpinner.hour = editor.hour
                        timePickerSpinner.minute = editor.minutes
                    })


                    rowHolder.onOff.isChecked = editor.isEnabled
                    //mPreAlarmCheckBox.isChecked = editor.isPrealarm

            //****알람 repeat 설정된 요일을 Chip 으로 표시해주는 것!!
                    mRepeatSummary.text = editor.daysOfWeek.summary(requireContext()) // 기존 Repeat 요일 메뉴에 쓰이던 것. 지워도 됨.
                    //tv_repeatDaysSum.text = "Repeat" + editor.daysOfWeek.summary(requireContext())
                    val alarmSetDaysStr = editor.daysOfWeek.summary(requireContext()) // 여기서 'Str 리스트로 기존에 설정된 요일들 받음' -> ex. [Tue, Thu, Sat, Sun]
                    val alarmSetDaysStrList = getAlarmSetDaysListFromStr(alarmSetDaysStr)
                    Log.d(TAG, "onResume: 현재 알람 설정된 요일들 String_List=$alarmSetDaysStrList ")
                // 기존에 알람이 설정된 요일을 일단 Chip 으로 Selected 표시해주기.
                    activateChipForAlarmSetDays(alarmSetDaysStrList)
                //



//                    if (editor.label != mLabel.text.toString()) {
//                        mLabel.setText(editor.label)
//                    }
                })
    // DetailsFragment 열었을때 '기존 설정 되어있는' & 새로 설정한 알람톤에 대해 반응.
        disposables.add(editor.distinctUntilChanged().observeOn(Schedulers.computation()).map { editor ->
            Log.d(TAG, "onResume:  [PRE] editor.alarmtone=${editor.alarmtone}, editor.alarmtone.persistedstring=")
                    when (editor.alarmtone) {
                        is Alarmtone.Silent -> {requireContext().getText(R.string.silent_alarm_summary)}
                        is Alarmtone.Default -> {RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)).title()}
                        is Alarmtone.Sound -> {RingtoneManager.getRingtone(context, Uri.parse(editor.alarmtone.uriString)).title()}
                        else -> {
                            Log.d(TAG, "onResume: !! 갑자기 여기에 else 문 넣으라고 오류가 뜨네. 이해 불가!!!!!! wtf????")}
                    }
                    // 여기 logd 넣으면 안됨.
                }.observeOn(AndroidSchedulers.mainThread()).subscribe { selectedRtFileName ->
//** RT 변경 or 최초 DetailsFrag 열릴 때 이쪽으로 들어옴
//***DetailsFrag 에서 설정된 rt를 Spinner 에 보여주기   //mRingtoneSummary.text = it ..
                    Log.d(TAG, "onResume: 설정된 알람톤 파일이름=$selectedRtFileName, alarmId=$alarmId")

                    if(isDropDownSpinnerReady) { // 2) Rt 를 User 가 변경하였을 때  -> 변경된 RT에 해당하는  Circle albumArt 사진만 업데이트!
                                Log.d(TAG, "onResume: 2) Rt 임의로 변경되었을 때")
                    //b) DetailsFrag 에 있는 Circle Album Art 변경
                                updateCircleAlbumArt(selectedRtFileName.toString())
                    } else { // 1) DetailsFrag (최초로) 열렸을때 혹은 다른 Frag 갔다왔을 때 여기 무조건 실행됨(rxJava Trigger 때문)
                        Log.d(TAG, "onResume: 1) rxJava Trigger")

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
// DetailFrag 에서 Cancel 때리고 나갈 때 생성된 알람을 지움!
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
            //Log.d(TAG, "Picker consumer: inside..")

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
//*********** 내가 추가한 Utility Method **********
    private fun getAlarmSetDaysListFromStr(alarmSetDaysStr: String): List<String> {
    // Ex) "Mon, Tue," 이렇게 생긴 String 을 받아서 ',' 을 기준으로 split
    val alarmSetDaysStrList: List<String> = alarmSetDaysStr.split(",").map {dayStr -> dayStr.trim()}
    Log.d(TAG, "getAlarmSetDaysListFromStr: alarmSetDaysStrList=$alarmSetDaysStrList")
    return alarmSetDaysStrList

    }
    // 기존에 알람이 설정된 요일을 일단 Chip 으로 Selected 표시해주기.
    private fun activateChipForAlarmSetDays(alarmSetDaysStrList: List<String>) {
        for(i in alarmSetDaysStrList.indices) {
            when(alarmSetDaysStrList[i]) {
                "Sun","Sunday" -> chipGroupDays.findViewById<Chip>(R.id._chipSun).isChecked = true
                "Mon", "Monday" -> chipGroupDays.findViewById<Chip>(R.id._chipMon).isChecked = true
                "Tue", "Tuesday" -> chipGroupDays.findViewById<Chip>(R.id._chipTue).isChecked = true
                "Wed", "Wednesday" -> chipGroupDays.findViewById<Chip>(R.id._chipWed).isChecked = true
                "Thu", "Thursday" -> chipGroupDays.findViewById<Chip>(R.id._chipThu).isChecked = true
                "Fri", "Friday" -> chipGroupDays.findViewById<Chip>(R.id._chipFri).isChecked = true
                "Sat", "Saturday" -> chipGroupDays.findViewById<Chip>(R.id._chipSat).isChecked = true
                "Every day" -> {
                    chipGroupDays.findViewById<Chip>(R.id._chipSun).isChecked = true
                    chipGroupDays.findViewById<Chip>(R.id._chipMon).isChecked = true
                    chipGroupDays.findViewById<Chip>(R.id._chipTue).isChecked = true
                    chipGroupDays.findViewById<Chip>(R.id._chipWed).isChecked = true
                    chipGroupDays.findViewById<Chip>(R.id._chipThu).isChecked = true
                    chipGroupDays.findViewById<Chip>(R.id._chipFri).isChecked = true
                    chipGroupDays.findViewById<Chip>(R.id._chipSat).isChecked = true
                }
            }
        }



    }
    // 선택된 Chip 날들 String 으로 받기.

    private fun createStrListFromSelectedChips() {
        val selectedChipsDaysList =  mutableListOf<String>()

        chipGroupDays.checkedChipIds.forEach {selectedDay ->
            when(selectedDay) {
                R.id._chipSun -> selectedChipsDaysList.add("Sun")
                R.id._chipMon -> selectedChipsDaysList.add("Mon")
                R.id._chipTue -> selectedChipsDaysList.add("Tue")
                R.id._chipWed -> selectedChipsDaysList.add("Wed")
                R.id._chipThu -> selectedChipsDaysList.add("Thu")
                R.id._chipFri -> selectedChipsDaysList.add("Fri")
                R.id._chipSat -> selectedChipsDaysList.add("Sat")

            }
        }
        Log.d(TAG, "createStrListFromSelectedChips: selectedChipsDaysList=$selectedChipsDaysList")
    }
    private fun createWhichIntFromTickedChip(dayTickedId: Int): Int {
        return when(dayTickedId) {
            R.id._chipSun -> 6
            R.id._chipMon -> 0
            R.id._chipTue -> 1
            R.id._chipWed -> 2
            R.id._chipThu -> 3
            R.id._chipFri -> 4
            R.id._chipSat -> 5

            else -> {
                Log.d(TAG, "createWhichIntFromTickedChip: error. Well let's just returning 10")
                444
            }
        }
    }
}