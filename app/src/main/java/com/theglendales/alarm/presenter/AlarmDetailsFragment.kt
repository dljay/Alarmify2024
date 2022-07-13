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
import android.transition.Transition
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.*
import com.theglendales.alarm.interfaces.IAlarmsManager
import com.theglendales.alarm.jjadapters.GlideApp
import com.theglendales.alarm.jjmvvm.helper.BadgeSortHelper
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjmvvm.util.RtOnThePhone
import com.theglendales.alarm.jjmvvm.util.showAlertIfRtIsMissing
import com.theglendales.alarm.jjongadd.TimePickerJjong
import com.theglendales.alarm.logger.Logger
import com.theglendales.alarm.lollipop
import com.theglendales.alarm.model.AlarmValue
import com.theglendales.alarm.model.Alarmtone
import com.theglendales.alarm.model.DaysOfWeek
import com.theglendales.alarm.util.Optional
import com.theglendales.alarm.util.modify
import com.theglendales.alarm.view.onChipDayClicked

import com.theglendales.alarm.view.summaryInNumber
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Details activity allowing for fine-grained alarm modification
 */
private const val TAG="*AlarmDetailsFragment*"
private const val REQ_CODE_FOR_RTPICKER = 588 // RTPICKER Intent 관련 (1)
private const val PICKER_RESULT_RT_TITLE="RtTitle" // RTPICKER Intent 관련 (2)
private const val PICKER_RESULT_AUDIO_PATH="AudioPath" // RTPICKER Intent 관련 (3)
private const val PICKER_RESULT_ART_PATH="ArtPath" // RTPICKER Intent 관련 (4)
private const val CURRENT_RT_FILENAME_KEY= "currentRtFileName_Key"


class AlarmDetailsFragment : Fragment() {

    // 내가 추가 ->
        companion object {
            var detailFragDisplayedRtFileName=""
        }
        // 폰에 저장된 ringtone (mp3 or ogg?) 과 앨범쟈켓(png) 을 찾기위해
            private val myDiskSearcher: DiskSearcher by globalInject()

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
        // RtPicker 관련
            private val tvRtPicker by lazy { fragmentView.findViewById(R.id.tv_rt_title) as TextView }
            private val clRtPickerContainer by lazy { fragmentView.findViewById(R.id.cl_RtPicker_Container) as ConstraintLayout} // Ringtone 이라고 써 있는 전체 박스!!
            private val tvRtDescription by lazy { fragmentView.findViewById(R.id.tv_rt_description_detailsFrag) as TextView }
        // Intensity 관련
            private val iv_lightning_1 by lazy { fragmentView.findViewById(R.id.iv_lightning_1) as ImageView }
            private val iv_lightning_2 by lazy { fragmentView.findViewById(R.id.iv_lightning_2) as ImageView }
            private val iv_lightning_3 by lazy { fragmentView.findViewById(R.id.iv_lightning_3) as ImageView }
            private val iv_lightning_4 by lazy { fragmentView.findViewById(R.id.iv_lightning_4) as ImageView }

        // Badge 관련
            private val iv_badge1_Intense by lazy {fragmentView.findViewById(R.id.iv_badge1_intense) as ImageView}
            private val iv_badge2_Gentle by lazy {fragmentView.findViewById(R.id.iv_badge2_gentle) as ImageView}
            private val iv_badge3_Nature by lazy {fragmentView.findViewById(R.id.iv_badge3_nature) as ImageView}
            private val iv_badge4_Location by lazy {fragmentView.findViewById(R.id.iv_badge4_location) as ImageView}
            private val iv_badge5_Popular by lazy {fragmentView.findViewById(R.id.iv_badge5_popular) as ImageView}
            private val iv_badge6_Misc by lazy {fragmentView.findViewById(R.id.iv_badge6_misc) as ImageView}
        // RtPickerActivity 로 넘어갈 때 현재 지정되어있는 알람 정보를 넘기기 위해..


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
    //private val mRepeatSummary by lazy { fragmentView.findViewById(R.id.details_repeat_summary) as TextView }
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
        Log.d(TAG, "onCreateView: [TIMING] #1-a INFLATE 시작")
        val view = inflater.inflate(R.layout.details_fragment_classic
                /* [JJLAY] 기존 코드.
                when (prefs.layout()) {
                    Layout.CLASSIC -> R.layout.details_fragment_classic
                    Layout.COMPACT -> R.layout.details_fragment_compact
                    else -> R.layout.details_fragment_bold
                }*/,container,false)
        this.fragmentView = view
        Log.d(TAG, "onCreateView: [TIMING] #1-b INFLATE 종료!!")
    //View Initializing <-

    // RTPicker onClickListener 셋업-- > 설정시 '현재 설정된 RT 의 File 이름' 이 필요하기에 밑에 line 5xx 'selectedRtFileName' 을 받을때 -> setRtPickerClickListener() 로 해줌.
            //[Ringtone] 써있는 전체 박스 중 아무데나 눌렀을 때
        clRtPickerContainer.setOnClickListener {
            val intent = Intent(requireActivity(), RtPickerActivity::class.java) //  현재 Activity 에서 -> RtPicker_Test1 Activity 로 이동.
            startActivityForResult(intent, REQ_CODE_FOR_RTPICKER)
        }
    // RtPicker onClickListener 셋업 <--

        rowHolder.run {
            this.container.setOnClickListener {
                modify("onOff") { editor ->
                    editor.copy(isEnabled = !editor.isEnabled)
                }
            }

            lollipop {
                this.digitalClock.transitionName = "clock$alarmId"
                this.container.transitionName = "onOff$alarmId"
                //this.detailsButton.transitionName = "detailsButton$alarmId"
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
        timePickerSpinner.setIs24HourView(false) // am Pm 시스템으로

        timePickerSpinner.setOnTimeChangedListener { view, hourOfDay, minute ->
            Log.d(TAG, "onCreateView: [setOnTimeChangedListener] Hour=$hourOfDay, minute=$minute")
            val pickedTime: PickedTime = PickedTime(hourOfDay, minute)
            disposableDialog = myTimePickerJjong.timePickerSpinnerTracker(hourOfDay,minute).subscribe(pickerConsumer)
            //disposableDialog = myTimePickerJjong.showTimePicker(alarmsListActivity.supportFragmentManager).subscribe(pickerConsumer) // 기존 material TimePicker 구현용도.
        }

    //TimePicker Spinner 로 시간 골랐을 때 시스템과 연결해주는 부분 <---

        view.findViewById<View>(R.id.details_activity_button_save).setOnClickListener {
            Log.d(TAG, "onCreateView[OK btn Listener] : current alarm ID=${alarmId} , isSaved=${alarms.getAlarm(alarmId)?.data?.isSaved}")
            modify("OkBtn-Clicked") {alarmValue -> alarmValue.copy(isSaved = true) } // 추후 ListFrag 에서 .isSaved 값이 False 면 삭제! [신규 알람 생성중 강제 종료시 자동Save 방지 위해]
            saveAlarm()
        }
        view.findViewById<View>(R.id.details_activity_button_revert).setOnClickListener { revert() } //Cancel 버튼

    //** 신규 알람 생성할때 TimePicker 보여주는 것. (기존에는 TimePickerDialogFragment.showxx() 였지만 -> myTimePickerJjong.. 으로 바꿈.)
        store.transitioningToNewAlarmDetails().firstOrError().subscribe { isNewAlarm ->
                    Log.d(TAG, "onCreateView: [isNewAlarm=$isNewAlarm] jj-!!subscribe-1")


        //** a)신규 User 가 알람 생성시 =>  RT를 "현재 사용 가능한 RT 중에서 Random 으로 골라주기"
                    if (isNewAlarm) {
                    // 현재 가용 가능한 RT 리스트 (스피너의 드랍다운 메뉴) 갯수를 파악하여 그중 하나 random! 으로 골라주기!
                        val availableRtCount= DiskSearcher.finalRtArtPathList.size
                        var rndRtPos = (0..availableRtCount).random()
                        if(rndRtPos == availableRtCount && rndRtPos >= 0 ) {rndRtPos = 0 } // ex. 총 갯수가 5개인데 5번이 뽑히면 안되니깐..

                        val randomRtaPath = DiskSearcher.finalRtArtPathList[rndRtPos].audioFilePath //todo: null error check or sharedPref 에서 꺼내오는게 낫지 않겠나? (sharedPref 는 항상..)
                        val randomArtPath = DiskSearcher.finalRtArtPathList[rndRtPos].artFilePathStr
                        val rtTitle= DiskSearcher.finalRtArtPathList[rndRtPos].rtTitle

                        Log.d(TAG, "onCreateView: [isNewAlarm=$isNewAlarm] jj-!!subscribe-2 NEW ALARM SETUP. rndRtPos=$rndRtPos \n rtTitle= $rtTitle")

                        changeAlarmTone(randomRtaPath,randomArtPath, rtTitle) // 알람톤 변경.

                        store.transitioningToNewAlarmDetails().onNext(false)
                        disposableDialog =myTimePickerJjong.showMaterialTimePicker(alarmsListActivity.supportFragmentManager).subscribe(pickerConsumer)

                    }

        }.addToDisposables()

        //pre-alarm
//        mPreAlarmRow.setOnClickListener {
//            modify("Pre-alarm") { editor -> editor.copy(isPrealarm = !editor.isPrealarm, isEnabled = true) }
//        }
    // 설정된 알람(Repeat) ChipGroup 안에서 요일을 선택 -> DaysOfWeek.onChipDayClicked() -> rxjava Single<DaysOfWeek> 을 만들고 -> 그것을 editor 가 subscribe!
        chipGroupDays = fragmentView.findViewById(R.id._chipGroupDays)

        CoroutineScope(IO).launch {
            for(i in 0 until chipGroupDays.childCount)
            {
                var oldDaysOfWeek: DaysOfWeek
                var newDaysOfWeek: DaysOfWeek

                val chipDay: Chip = chipGroupDays.getChildAt(i) as Chip
                chipDay.setOnCheckedChangeListener { _, isChecked ->
                    val whichInt = createWhichIntFromTickedChip(chipDay.id) // ex. Sat -> 5번을 받음.
// ** Subscribe 미리 된 상태에서-> chip 변화 -> onChipDayClicked..

                    val subscribe = editor.firstOrError().flatMap { alarmValue -> alarmValue.daysOfWeek.onChipDayClicked(whichInt, isChecked) }
                        .subscribe { daysOfWeek ->
                            // daysOfWeek 에 변화가 없을때도 굳이 modify->ActionBarHandler.kt 계속 불려지는지 확인 필요.
                            //Log.d(TAG, "onCreateView: 0 daysOfWeek = $daysOfWeek")
                            val thisAlarm = alarms.getAlarm(alarmId)
                            if(thisAlarm!=null) {
                                //Log.d(TAG, "onCreateView: 1 thisAlarm!=null")
                                //Log.d(TAG, "onCreateView: 2 [daysOfWeek] 요일 정보가 바꼈음. modify 실행하겠음!!")
                                modify("Repeat dialog") { prev ->prev.copy(daysOfWeek = daysOfWeek,isEnabled = true)}

                            }
                            Log.d(TAG,"onCreateView: daysOfWeekJJ_new=$daysOfWeek, whichInt=$whichInt, isChecked=$isChecked")
                        }.addToDisposables() // 원문에서 추가되어서 넣었음. 잘 작동하는지 면밀한 확인 필요.


                }

            }//for loop 여기까지
        }

    // 예전 버전 LABEL 유저가 입력할때 썼던것. 이제 필요 없음.
        /*class TextWatcherIR : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                editor.take(1) // take (RxJava) = take the number of elements you specified (n 번째까지만 받고 나머지는 무시)
                        .filter { it.label != s.toString() }.subscribe {modify("Label") { prev -> prev.copy(label = s.toString(), isEnabled = true) }
                        }.addToDisposables()}}*/

    // DetailsFrag 에서 !!*** APP 설치 중 설정된 알람 파악 ->  -> alarm.label 값은 "userCreated" 로 바꿔서 -> 다음부터는 여기에 걸리지 않게끔.
        val currentAlarms = alarms.getAlarm(alarmId)
        Log.d(TAG, "onCreateView: **** [현재 알람 정보] ${currentAlarms?.data}, isSaved=${currentAlarms?.data?.isSaved}")
        //val currentAlarmsLabel= currentAlarms!!.labelOrDefault

    // *인스톨시 생성된 알람 두개 관련: 이 시점에서는 이미 모든 DefRta/Art 파일이 폰에 Copy 되었다는 가정하에 -> 아래 modify 로 label 변경  [alertUri, artUri 는 각 def1,2 로 인스톨시 설정.]
        /*if(currentAlarms!!.labelOrDefault !="userCreated") {
            Log.d(TAG, "onCreateView: **MODIFYING ALARMS CREATED DURING APP INSTALLATION")
            modify("Label") {alarmValue -> alarmValue.copy(label = "userCreated", isEnabled = true)} // alarmValue= AlarmValue(Data Class) -> 여기의 .copy 기능을 사용.
        }*/

        return view
    }
// <<<<----------onCreateView


// ******** ====> DISK 에 있는 파일들(mp3) 찾고 거기서 mp3, albumArt(bitmap-mp3 안 메타데이터) 리스트를 받는 프로세스 (코루틴으로 실행X) ===> //막 0.01 초 걸리고 그래서.. 코루틴으로 하기가 좀 그래..
    // a)RT 제목(TextView), b)RT Description, c) Album Art (ImageView), d) Badge  UI 업데이트

    private fun updateUisForRt(selectedRtFileName: String) {

        var updatedRtFileName = selectedRtFileName

    //1) **앱 Install 과 동시에 설치된 알람의 경우 파일명에 확장자가 없다!! (raw 의 mp3 를 바로 알람음으로 지정했으므로) // todo: 다소 임시방편 느낌이 있음..
        if(!selectedRtFileName.contains(".rta")) {
            updatedRtFileName = "$selectedRtFileName.rta"
        }
        Log.d(TAG, "updateUisForRt: called #$#@% selectedRtFileName=$selectedRtFileName, updatedRtFileName=$updatedRtFileName")
    //2) 그 외 user 가 일반적으로 지정한 알람음의 경우>
        val indexOfSelectedRt = DiskSearcher.finalRtArtPathList.indexOfFirst { rtOnDisk -> rtOnDisk.fileNameWithExt == updatedRtFileName }
        /** .indexOfFirst (람다식을 충족하는 '첫번째' 대상의 위치를 반환. 없을때는 -1 반환) */
        if(indexOfSelectedRt!=-1) // 현재 disk 에 있는 rt list 에서 현재 '설정한(or 되어있던)' rt 를 찾았으면 CircleAlbumArt 보여주기.
        {
            //Log.d(TAG, "updateCircleAlbumArt: indexOfSelectedRt=$indexOfSelectedRt, selectedRtForThisAlarm=${SpinnerAdapter.rtOnDiskList[indexOfSelectedRt]}")

            val selectedRtForThisAlarm: RtOnThePhone = DiskSearcher.finalRtArtPathList[indexOfSelectedRt] // 리스트 업데이트 전에 실행-> indexOfSelectedRt 가 -1 ->  뻑남..
            val rtTitle = selectedRtForThisAlarm.rtTitle

            val rtDescription = selectedRtForThisAlarm.rtDescription
            val badgeStr = selectedRtForThisAlarm.badgeStr // ex. "I,N,H" -> Intense, Nature, History 뭔 이런식.
            val artPath = selectedRtForThisAlarm.artFilePathStr
            val alarmIntensity = selectedRtForThisAlarm.intensity


        // 2-b) 잠시! AlarmListFrag 에서 Row 에 보여줄 AlbumArt 의 art Path 수정/저장!  [alarmId, artPath] 가 저장된 Shared Pref(ArtPathForListFrag.xml) 업데이트..
            //mySharedPrefManager.saveArtPathForAlarm(alarmId, artPath) <-- SQLITE 로 alarm.artFilePath 저장 가능해져서 필요없음.
        // 2-c) Badge 보여주기 (너무 빨리 되서. showOrHideBadge() 완료까지 약 0.002 초.. 그냥 코루틴 안할계획임.
            val badgeStrList = BadgeSortHelper.getBadgesListFromStr(badgeStr) // ex. "I,N,H" 이렇게 metadata 로 받은 놈을 ',' 로 구분하여 String List 로 받음
            showOrHideBadges(badgeStrList) // 이니셜 따라 Ui 업뎃 (ex. [I,N,H] => Intense, Nature, Human 배지를 Visible 하게 UI 업뎃!


        // 2-d) Rt Title & Description 보여주는 TV 업데이트
            tvRtPicker.text = rtTitle
            tvRtDescription.text = rtDescription
            Log.d(TAG, "updateUisForRt: tvRtDescription=$rtDescription")
        // 2-e) Rt Title 옆에 있는 큰 앨범아트 ImageView 에 현재 설정된 rt 보여주기. Glide 시용 (Context 가 nullable 여서 context?.let 으로 시작함)
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
        // 2-f) Intensity 보여주기 [STRING 으로 받음]
            if(alarmIntensity!=null) {
                Log.d(TAG, "updateUisForRt: intensity=$alarmIntensity")
                when(alarmIntensity) {
                    "1" -> { iv_lightning_1.setImageResource(R.drawable.ic_speaker_1_yellow)
                        iv_lightning_2.setImageResource(R.drawable.ic_speaker_1_grayedout)
                        iv_lightning_3.setImageResource(R.drawable.ic_speaker_1_grayedout)
                        iv_lightning_4.setImageResource(R.drawable.ic_speaker_1_grayedout)
                    }
                    "2" -> { iv_lightning_1.setImageResource(R.drawable.ic_speaker_1_yellow)
                        iv_lightning_2.setImageResource(R.drawable.ic_speaker_1_yellow)
                        iv_lightning_3.setImageResource(R.drawable.ic_speaker_1_grayedout)
                        iv_lightning_4.setImageResource(R.drawable.ic_speaker_1_grayedout)
                    }
                    "3" -> { iv_lightning_1.setImageResource(R.drawable.ic_speaker_1_yellow)
                        iv_lightning_2.setImageResource(R.drawable.ic_speaker_1_yellow)
                        iv_lightning_3.setImageResource(R.drawable.ic_speaker_1_yellow)
                        iv_lightning_4.setImageResource(R.drawable.ic_speaker_1_grayedout)
                    }
                    "4" -> { iv_lightning_1.setImageResource(R.drawable.ic_speaker_1_yellow)
                        iv_lightning_2.setImageResource(R.drawable.ic_speaker_1_yellow)
                        iv_lightning_3.setImageResource(R.drawable.ic_speaker_1_yellow)
                        iv_lightning_4.setImageResource(R.drawable.ic_speaker_1_yellow)
                    }
                }
            } else { // null 인 경우

            }
        }
    }


// ***** <==== DISK 에 있는 파일들(mp3) 찾고 거기서 albumArt 메타데이터 복원하는 프로세스 (코루틴으로 위에서 실행)

    // RtActivity 에서 Ringtone 선택 후 결과값에 대한 처리를 여기서 해줌

    // 위에서 시작한 RtPickerActivity 에 대한 결과값을 아래에서 받음.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (data != null && requestCode == REQ_CODE_FOR_RTPICKER) {
            if (resultCode == RESULT_OK) { // RESULT_OK == -1 임!!
                val rtTitleFromIntent =
                    data.getStringExtra(PICKER_RESULT_RT_TITLE) // 결과 없을때 default Result 값은 'null'
                val rtaPathFromIntent =
                    data.getStringExtra(PICKER_RESULT_AUDIO_PATH) // 결과 없을때 default Result 값은 'null'
                val artPathFromIntent =
                    data.getStringExtra(PICKER_RESULT_ART_PATH) // 결과 없을때 default Result 값은 'null'

                Log.d(TAG,"onActivityResult: ----[FROM INTENT] Selected RT_Title=$rtTitleFromIntent, AudioPath=$rtaPathFromIntent, ArtPath=$artPathFromIntent")

                // 이제 ringtone 으로 설정 -> 기존 onActivityResult 에 있던 내용들 복붙! -->
                val alertSoundPath: String? = rtaPathFromIntent

                // 이제 새 ringtone 으로 설정(rtaPath 와 artpath 둘다 넘김) ->
                changeAlarmTone(alertSoundPath, artPathFromIntent, rtTitleFromIntent)
            }
        }

    }
    // 내가 추가한 function
    private fun changeAlarmTone(alertSoundPath: String?, artPath: String?, rtTitle: String?) {
        logger.debug { "Got ringtone: $alertSoundPath and artPath 쫑: $artPath" }

        val alarmtone: Alarmtone = when (alertSoundPath) {
            null -> Alarmtone.Silent() // 선택한 alarm 톤이 a)어떤 오류등으로 null 값일때 -> .Silent()
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString() -> Alarmtone.Default() // b)Default 일때
            else -> Alarmtone.Sound(alertSoundPath) // 내가 선택한 놈.
        }
        logger.debug { "RtPicker- onItemSelected! $alertSoundPath -> $alarmtone" }

        //checkPermissions(requireActivity(), listOf(alarmtone))
        showAlertIfRtIsMissing(requireActivity(), listOf(alarmtone))

        val nonNullableRtTitle = rtTitle.toString()
        modify("RTPicker[변경]") { prev ->prev.copy(alarmtone = alarmtone, artFilePath = artPath, isEnabled = true, label = nonNullableRtTitle)}
    }


    override fun onResume() {
        Log.d(TAG, "onResume: *here we have backButtonSub")
        super.onResume()
        // Tool Bar 가려주기

        disposables = CompositeDisposable()
        disposables.add(editor.distinctUntilChanged().subscribe { editor ->
            rowHolder.digitalClock.updateTime(Calendar.getInstance().apply {
                    //new) ** TimePickerSpinner 에 "기존에 설정된 알람 시간"을 그대로 보여주기!!!! 대성공!!=>
                        timePickerSpinner.hour = editor.hour
                        timePickerSpinner.minute = editor.minutes
                    })

            rowHolder.onOff.isChecked = editor.isEnabled //todo: 여기부터 22/4/4 11:07



                // Local 언어 때문에 when(요일이름) 을 작성할수 없어. Int 로 된 Str 을 받는 방법으로 바꿈 whichInt 는 ->   [0,3,4] (=월,목,금)
                // whichInt (칩 선택시 어떤 칩 클릭했는지 확인 용도):  월 = 0 화 = 1 수 = 2  목 = 3 금 = 4 토 = 5 일 = 6
                // intList  일 = 1 월 = 2 화 = 3 수 = 4  목 = 5 금 = 6 토 = 7
                    val alarmSetDaysIntList = editor.daysOfWeek.summaryInNumber(requireContext()) // 내가 만든 summaryInNumber !
                    viewLifecycleOwner.lifecycleScope.launch {
                        Log.d(TAG, "onResume: 코루틴 alarmSetDaysIntList=$alarmSetDaysIntList")
                        activateChipFromIntList(alarmSetDaysIntList)
                    }
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


//***DetailsFrag 에서 설정된 rt를 Spinner 에 보여주기   //mRingtoneSummary.text = it ..

                    detailFragDisplayedRtFileName = selectedRtFileName.toString()
                    if(!detailFragDisplayedRtFileName.contains(".rta")) { // 인스톨 알람( d1,d2) 의 경우 ".rta" 없이 RtPickerAdapter> BindView if 문에서 캐치 못함-> RadioBtn 작동 안된다.
                        detailFragDisplayedRtFileName = "$detailFragDisplayedRtFileName.rta"
                    }
                    Log.d(TAG, "onResume: [RT 변경] 설정된 알람톤 파일이름=$selectedRtFileName, detailFragDisplayedRtFileName=${detailFragDisplayedRtFileName}, alarmId=$alarmId") // ex) p1009.rta, alarmId=1

                    updateUisForRt(selectedRtFileName.toString())

                })

        //pre-alarm duration, if set to "none", remove the option
        disposables.add(prefs.preAlarmDuration
                .observe()
                .subscribe { value ->
                    //mPreAlarmRow.visibility = if (value.toInt() == -1) View.GONE else View.VISIBLE
                })

        //DetailsFrag 에서 뒤로가기 (<-) 눌렀을때도 save 함.
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
        //var finalEditedVersion: AlarmValue
        Log.d(TAG, "saveAlarm: called.")
        editor.firstOrError().subscribe { editedAlarmToSave ->
            Log.d(TAG, "saveAlarm: \n*** editedAlarmToSave=$editedAlarmToSave. isSaved=${editedAlarmToSave.isSaved}")
            alarms.getAlarm(alarmId)?.run {
                edit { withChangeData(editedAlarmToSave) }
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
            Log.d(TAG, "Picker consumer: inside..")
        }
    }

    private fun modify(reason: String, function: (AlarmValue) -> AlarmValue) {
        logger.debug { "Performing modification because of $reason" }
        store.editing().modify {
            copy(value = value.map { function(it) }) //  value: Optional<AlarmValue>
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
        //Log.d(TAG, "hackRippleAndAnimation: called")
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

                override fun onTransitionResume(transition: Transition?) {}
                override fun onTransitionPause(transition: Transition?) {}
                override fun onTransitionCancel(transition: Transition?) {}
                override fun onTransitionStart(transition: Transition?) {}
            })
        }
        //Log.d(TAG, "hackRippleAndAnimation: ")
    }
//*********** 내가 추가한 Utility Method **********
//1) Badge 보여주기 관련 (밑에 '요일 확인' 과 동일한 메커니즘)

    private fun showOrHideBadges(badgeStrList: List<String>?) {
        // 일단 다 gone 으로 꺼주고 시작 (안 그러면 RtPicker 갔다왔을 떄 기존에 켜진놈이 안 꺼지니께..)
        // 혹시 이렇게 꺼지는게 눈에 안 좋아보이면 위에서 RtPicker Activity 갈때 꺼줘도 됨..
        iv_badge1_Intense.visibility = View.GONE
        iv_badge2_Gentle.visibility = View.GONE
        iv_badge3_Nature.visibility = View.GONE
        iv_badge4_Location.visibility = View.GONE
        iv_badge5_Popular.visibility = View.GONE
        iv_badge6_Misc.visibility = View.GONE
        // String List 에서 이제 글자따라 다시 visible 시켜주기!
        Log.d(TAG, "showOrHideBadges: badgeStrList=$badgeStrList")
        if (badgeStrList != null) {
            for(i in badgeStrList.indices) {
                when(badgeStrList[i]) {
                    "INT" -> iv_badge1_Intense.visibility = View.VISIBLE
                    "GEN" -> iv_badge2_Gentle.visibility = View.VISIBLE
                    "NAT" -> iv_badge3_Nature.visibility = View.VISIBLE
                    "LOC" -> iv_badge4_Location.visibility = View.VISIBLE
                    "POP" -> iv_badge5_Popular.visibility = View.VISIBLE
                    "MIS" -> iv_badge6_Misc.visibility = View.VISIBLE
                }
            }
        }
        Log.d(TAG, "showOrHideBadges: done..")
    }
    // 기존에 알람이 설정된 요일을 IntList 로 받음. ex. 월, 수 알람이라면 [2,5] // 토=0, 일=1 ..... 금= 6
    private fun activateChipFromIntList(alarmSetDaysIntList: List<Int>) {
        Log.d(TAG, "activateChipFromIntList: -- alarmSetDaysIntList=$alarmSetDaysIntList")
        
        for(i in alarmSetDaysIntList.indices) {
            when(alarmSetDaysIntList[i]) 
            {
                8 -> { // 매일매일..
                    chipGroupDays.findViewById<Chip>(R.id._chipSun).isChecked = true
                    chipGroupDays.findViewById<Chip>(R.id._chipMon).isChecked = true
                    chipGroupDays.findViewById<Chip>(R.id._chipTue).isChecked = true
                    chipGroupDays.findViewById<Chip>(R.id._chipWed).isChecked = true
                    chipGroupDays.findViewById<Chip>(R.id._chipThu).isChecked = true
                    chipGroupDays.findViewById<Chip>(R.id._chipFri).isChecked = true
                    chipGroupDays.findViewById<Chip>(R.id._chipSat).isChecked = true
                }


                1 -> chipGroupDays.findViewById<Chip>(R.id._chipSun).isChecked = true // 일..
                2 -> chipGroupDays.findViewById<Chip>(R.id._chipMon).isChecked = true
                3 -> chipGroupDays.findViewById<Chip>(R.id._chipTue).isChecked = true
                4 -> chipGroupDays.findViewById<Chip>(R.id._chipWed).isChecked = true
                5 -> chipGroupDays.findViewById<Chip>(R.id._chipThu).isChecked = true
                6 -> chipGroupDays.findViewById<Chip>(R.id._chipFri).isChecked = true // 금..
                7 -> chipGroupDays.findViewById<Chip>(R.id._chipSat).isChecked = true // 토..
                // 요일 설정 안되어있을때는 -2 인데. Chip 들이 default 로 =false 되있으니깐 굳이 설정 필요 없음.
                else -> {
                    Log.d(TAG, "activateChipFromIntList: No Repeat..?")}

            }
        }
        //Log.d(TAG, "activateChipForAlarmSetDays: done.. alarmSetDaysStrList=$alarmSetDaysStrList")
    }

    // 선택된 Chip 날들 String 으로 받기.

//    private fun createStrListFromSelectedChips() {
//        val selectedChipsDaysList =  mutableListOf<String>()
//
//        chipGroupDays.checkedChipIds.forEach {selectedDay ->
//            when(selectedDay) {
//                R.id._chipSun -> selectedChipsDaysList.add("Sun")
//                R.id._chipMon -> selectedChipsDaysList.add("Mon")
//                R.id._chipTue -> selectedChipsDaysList.add("Tue")
//                R.id._chipWed -> selectedChipsDaysList.add("Wed")
//                R.id._chipThu -> selectedChipsDaysList.add("Thu")
//                R.id._chipFri -> selectedChipsDaysList.add("Fri")
//                R.id._chipSat -> selectedChipsDaysList.add("Sat")
//
//            }
//        }
//        Log.d(TAG, "createStrListFromSelectedChips: selectedChipsDaysList=$selectedChipsDaysList")
//    }
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