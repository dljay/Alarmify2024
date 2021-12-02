

package com.theglendales.alarm.presenter

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.ChangeTransform
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.LinearLayout

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.theglendales.alarm.BuildConfig
import com.theglendales.alarm.NotificationSettings
import com.theglendales.alarm.R
import com.theglendales.alarm.checkPermissions
import com.theglendales.alarm.configuration.EditedAlarm
import com.theglendales.alarm.configuration.Store
import com.theglendales.alarm.configuration.globalGet
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.configuration.globalLogger
import com.theglendales.alarm.interfaces.IAlarmsManager
import com.theglendales.alarm.jjongadd.SecondFragment
import com.theglendales.alarm.logger.Logger
import com.theglendales.alarm.lollipop
import com.theglendales.alarm.model.AlarmValue
import com.theglendales.alarm.model.Alarmtone
import com.theglendales.alarm.model.DaysOfWeek
import com.theglendales.alarm.util.Optional
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.theglendales.alarm.jjdata.GlbVars
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.Disposables
import io.reactivex.functions.Consumer

import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.Calendar

// v0.35a  [요일 local 글자 말고 숫자로 받기 테스트전]
// 갤S20 에서 DetailsFrag 에서 요일 표시 안되는 문제 -> 요일 글자를 Local 로 받아서 영문작성된 when 문에서 설정이 안되서 그런거였음.
// DaysOfWeek 에 extension Function 넣어서 숫자로 받아보기 시도중.

//badge 와 ChipDays ? 포함 Coroutine 으로 바꿔주기.,
// A)음악 재생 B)처음 DetailsFrag 에서 -> RtPicker Activity 갔을 때 '현재 설정되어 있는 RT 로 자동 select?'



// rtPicker rcView 열었을 때 현재 선택해놓은 rt 재생? <- 이 기능은 어려우면 굳이 넣지 말것..

//- fab 버튼 -> 상단 + 로 변경? => xx 후에 울립니다 시간 표시-> 상단 .. ActionBar 진화형태.

/**
 * This activity displays a list of alarms and optionally a details fragment.
 */
private const val TAG="*AlarmsListActivity*"

class AlarmsListActivity : AppCompatActivity() {
    private lateinit var mActionBarHandler: ActionBarHandler

    //내가 추가-->
    private val mySharedPrefManager: MySharedPrefManager by globalInject()
    private val myDiskSearcher: DiskSearcher by globalInject()
    //내가 추가<-

    // lazy because it seems that AlarmsListActivity.<init> can be called before Application.onCreate()
    private val logger: Logger by globalLogger("AlarmsListActivity")
    private val alarms: IAlarmsManager by globalInject()
    private val store: Store by globalInject()

    private var subscriptions = Disposables.disposed()

    private val uiStore: UiStore by globalInject()
    private val dynamicThemeHandler: DynamicThemeHandler by globalInject()
//Companion Object--->
    companion object
    {

        val uiStoreModule: Module = module {
            Log.d(TAG, "uiStoreModule: jj-...")
            single<UiStore> { createStore(EditedAlarm(), get()) } //koin single!!
        }
    //fun createStore -->>>>
        private fun createStore(edited: EditedAlarm, alarms: IAlarmsManager): UiStore
        {
            Log.d(TAG, "createStore: jj- called")
            class UiStoreIR : UiStore {
                var onBackPressed = PublishSubject.create<String>()
                var editing: BehaviorSubject<EditedAlarm> = BehaviorSubject.createDefault(edited)
                var transitioningToNewAlarmDetails: Subject<Boolean> = BehaviorSubject.createDefault(false)

                override fun editing(): BehaviorSubject<EditedAlarm> {
                    return editing
                }

                override fun onBackPressed(): PublishSubject<String> {
                    Log.d(TAG, "onBackPressed(Line111): no.1 jj-!!called!!")
                    return onBackPressed
                }

            // USER 가 직접 생성하는 알람에 대해서만 createNewAlarm() 이 불림!
                override fun createNewAlarm() {
                    Log.d(TAG, "(line97) createNewAlarm: jj- called")
                    transitioningToNewAlarmDetails.onNext(true)
                    val newAlarm = alarms.createNewAlarm()
                    editing.onNext(EditedAlarm(
                            isNew = true,
                            value = Optional.of(newAlarm.data),
                            id = newAlarm.id,
                            holder = Optional.absent()))
                }

                override fun transitioningToNewAlarmDetails(): Subject<Boolean> {
                    return transitioningToNewAlarmDetails
                }

                override fun edit(id: Int) {
                    Log.d(TAG, "edit: (1) Begins.")
                    alarms.getAlarm(id)?.let { alarm ->
                        //alarm.labelOrDefault
                        editing.onNext(EditedAlarm(
                                isNew = false,
                                value = Optional.of(alarm.data),
                                id = id,
                                holder = Optional.absent()))
                    }
                }

                override fun edit(id: Int, holder: RowHolder) {
                    Log.d(TAG, "edit: (2) Begins.")
                    alarms.getAlarm(id)?.let { alarm ->
                            Log.d(TAG, "edit: (2-B) 기존에 만든 알람 수정")
                            editing.onNext(EditedAlarm(
                                isNew = false,
                                value = Optional.of(alarm.data),
                                id = id,
                                holder = Optional.of(holder)))
                    }
                }

                override fun hideDetails() {
                    editing.onNext(EditedAlarm())
                }

                override fun hideDetails(holder: RowHolder) {
                    editing.onNext(EditedAlarm(
                            isNew = false,
                            value = Optional.absent(),
                            id = holder.alarmId,
                            holder = Optional.of(holder)))
                }
            }

            return UiStoreIR()
        }
        //<---fun createStore
    }
//<------ Companion Object
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.getStringExtra("reason") == SettingsFragment.themeChangeReason) {
            finish()
            startActivity(Intent(this, AlarmsListActivity::class.java))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("version", BuildConfig.VERSION_CODE)
        uiStore.editing().value?.writeInto(outState)
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: !!AlarmListActivitiy onCreate() !!! ")
        setTheme(dynamicThemeHandler.getIdForName(AlarmsListActivity::class.java.name))
        super.onCreate(savedInstanceState)

        when {
            savedInstanceState != null && savedInstanceState.getInt("version", BuildConfig.VERSION_CODE) == BuildConfig.VERSION_CODE -> {
                val restored = editedAlarmFromSavedInstanceState(savedInstanceState)
                logger.debug { "Restored $this with $restored" }
                uiStore.editing().onNext(restored)
            }
            else -> {
                val initialState = EditedAlarm()
                logger.debug { "Created $this with $initialState" }
            }
            // if (intent != null && intent.hasExtra(Intents.EXTRA_ID)) {
            //     //jump directly to editor
            //     uiStore.edit(intent.getIntExtra(Intents.EXTRA_ID, -1))
            // }
        }

        this.mActionBarHandler = ActionBarHandler(this, uiStore, alarms, globalGet())

        val isTablet = !resources.getBoolean(R.bool.isTablet)
        if (isTablet) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        setContentView(R.layout.list_activity)

        store
                .alarms()
                .take(1)
                .subscribe { alarms ->
                    checkPermissions(this, alarms.map { it.alarmtone })
                }.apply { }

// 추가1-A) Second Fragment 관련 -->
    // 2nd Frag 시작과 동시에 일단 SharedPref 파일 자체를 생성해줌. => 일단 사용 안함.
//        val defaultPlayInfo = PlayInfoContainer(-10,-10,-10,StatusMp.IDLE)
//        mySharedPrefManager.savePlayInfo(defaultPlayInfo)  // default 값은 -10, -10, -10, IDLE

        val secondFrag = SecondFragment()
        val btmNavView = findViewById<BottomNavigationView>(R.id.id_bottomNavigationView)
        btmNavView.setOnNavigationItemSelectedListener {
            // 1) .setOnNav....Listener() 메써드는 onNavigationxxListener(인터페이스를 implement 한 인자를 람다로 받음)
            //2) OnNavigationItemSelectedListener(인터페이스) 안에는 onNavItemSelected(boolean 리턴 메써드) 하나만 있음. 그래서 override 생략 가능sam..
            // 밑에는 한마디로 Override fun onNavItemSelected: Boolean { 이 안에 들어가는 내용임.}
            when(it.itemId) {
                R.id.id_setTime -> configureTransactions()
                R.id.id_RingTone -> jjSetCurrentFragment(secondFrag)

            }
            Log.d(TAG, "onCreate: btmNavView.setOnNavigationItemListener -> before hitting true!")
            true
            // we don't write return true in the lambda function, it will always return the last line of that function
        }

    } // onCreate() 여기까지.
// 추가 1-B)-->

    fun jjSetCurrentFragment(receivedFragment: Fragment) =
        supportFragmentManager.beginTransaction().apply{ //supportFragmentManager = get FragmentManager() class

            replace(R.id.main_fragment_container, receivedFragment)
            commit()
            Log.d(TAG, "jjSetCurrentFragment: ..... ")
        }

// <--추가 1-B)




    override fun onStart() {
        Log.d(TAG, "onStart: jj-called")
        super.onStart()
// ** 추가 -->
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_fragment_container)

        if (currentFragment is SecondFragment) {
            Log.d(TAG, "onStart: 과거 머물러있던 fragment 는 SecondFragment 로 예상됨.")
            return
        } else {
            configureTransactions() // <- 원래는 if, else 문 없이 이것만 있었음.
        }
// ** 추가 <--
    }

    override fun onResume() {
        Log.d(TAG, "onResume: jj-called")
        super.onResume()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        NotificationSettings().checkSettings(this)
    }

    override fun onStop() {
        Log.d(TAG, "onStop: jj-called")
    //SharedPref 에 저장되어 있는 현재 second Frag 의 재생정보를 삭제!
        //mySharedPrefManager.calledFromActivity()

        super.onStop()
        this.subscriptions.dispose()

    }

    override fun onDestroy() {


    //
        Log.d(TAG, "onDestroy: jj-called")
        logger.debug { "$this" }
        super.onDestroy()
        this.mActionBarHandler.onDestroy()


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return supportActionBar?.let { mActionBarHandler.onCreateOptionsMenu(menu, menuInflater, it) }
                ?: false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return mActionBarHandler.onOptionsItemSelected(item)
    }
// onBackPressed no.2
    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed: no.2 - onNext") // 이건 print 되지 않고 밑에 pSubject 만 자동반응 업뎃..?
        uiStore.onBackPressed().onNext(AlarmsListActivity::class.java.simpleName)
    }
// ***** !!! 여기서 showList() 로 감!!!! *****
    private fun configureTransactions() {
        Log.d(TAG, "(line316)configureTransactions: . Begins.")
        subscriptions = uiStore.editing()
                .distinctUntilChanged { edited -> edited.isEdited }
                .subscribe(Consumer { edited ->
                    Log.d(TAG, "(line268!)configureTransactions: jj- edited= $edited") // edited 는 (type) EditedAlarm
                    when {
                        lollipop() && isDestroyed -> return@Consumer
                        edited.isEdited -> showDetails(edited)
                        else -> {
                            Log.d(TAG, "(line270)configureTransactions: else->showlist() 안!!")
                            showList(edited)}
                    }
                })
    }

// 알람 리스트를 보여주는 !! fragment 전환!! 중요!!
    private fun showList(@NonNull edited: EditedAlarm) {
    //추가->
    Log.d(TAG, "(Line281)showList: jj-called")
    //<-추가
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_fragment_container)


        if (currentFragment is AlarmsListFragment) {
            logger.debug { "skipping fragment transition, because already showing $currentFragment" }
        }
//        else if (currentFragment is SecondFragment) {
//            Log.d(TAG, "showList: jj-currentFragment is SecondFragment !!")
//        }
        else // 현재 fragment 가 AlarmsListFragment 가 아니면 AlarmsListFragment 로 이동. 문제는 나갔다오면 onStart 에서 일로 온다는것..
        {
            logger.debug { "transition from: $currentFragment to show list, edited: $edited" }
            // 애니메이션
            supportFragmentManager.findFragmentById(R.id.main_fragment_container)?.apply {
                lollipop {
//**ANIM 관련줄인데 삭제 a) exitTransition = Fade()
                }
            }
            //Below is equivalent to : val listFragment= AlarmsListFragment()
        //                          listFragment.sharedElementEnter = moveTransition()
        //                          listFragment.enterTransition = Fade() .. 기타 등등.
            val listFragment = AlarmsListFragment().apply {
                lollipop {
            // ANIM  관련 다음 3줄 삭제하면 Fade In/Out 안함!!
//                        sharedElementEnterTransition = moveTransition()
//                        enterTransition = Fade()
//                        allowEnterTransitionOverlap = true
                }
            }

            supportFragmentManager.beginTransaction()
                    .apply {
                        lollipop { // SDK 21 인듯
                            edited.holder.getOrNull()?.addSharedElementsToTransition(this)
                            //Log.d(TAG, "showList: yes lollipop")
                        }
                    }
                    .apply {
                        if (!lollipop()) {
                            this.setCustomAnimations(R.anim.push_down_in, R.anim.my_fade_out_time_short)
                            Log.d(TAG, "showList: not lollipop()")
                        }
                    }.replace(R.id.main_fragment_container, listFragment).commitAllowingStateLoss()
        }
    }

    private fun showDetails(@NonNull edited: EditedAlarm) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_fragment_container)

        if (currentFragment is AlarmDetailsFragment) {
            logger.debug { "skipping fragment transition, because already showing $currentFragment" }
        } else {
            logger.debug { "transition from: $currentFragment to show details, edited: $edited" }
            currentFragment?.apply {
                lollipop {
//**ANIM 관련줄인데 삭제 a)                    exitTransition = Fade()
                }
            }

            val detailsFragment = AlarmDetailsFragment().apply {
                arguments = Bundle()
            }.apply {
                lollipop {
// ANIM  관련 다음 3줄 삭제하면 Fade In/Out 안함!!
//                    enterTransition = TransitionSet().addTransition(Slide()).addTransition(Fade())
//                    sharedElementEnterTransition = moveTransition()
//                    allowEnterTransitionOverlap = true
                }
            }

            supportFragmentManager.beginTransaction()
                    .apply {
                        if (!lollipop()) { //lollipop = SDK 21인듯..
                            //this.setCustomAnimations(R.anim.push_down_in, R.anim.my_fade_out_time_short)
                            Log.d(TAG, "showDetails: not lollipop")

                        }
                    }
                    .apply {
                        lollipop {
                            edited.holder.getOrNull()?.addSharedElementsToTransition(this)
                        }
                    }
                    .replace(R.id.main_fragment_container, detailsFragment)
                    .commitAllowingStateLoss()
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun moveTransition(): TransitionSet {
        return TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(ChangeBounds())
            addTransition(ChangeTransform())
        }
    }

    private fun RowHolder.addSharedElementsToTransition(fragmentTransaction: androidx.fragment.app.FragmentTransaction) {
        fragmentTransaction.addSharedElement(digitalClock, "clock" + alarmId)
        fragmentTransaction.addSharedElement(container, "onOff" + alarmId)
        fragmentTransaction.addSharedElement(detailsButton, "detailsButton" + alarmId)

    }

    /**
     * restores an [EditedAlarm] from SavedInstanceState. Counterpart of [EditedAlarm.writeInto].
     */
    private fun editedAlarmFromSavedInstanceState(savedInstanceState: Bundle): EditedAlarm {
        return EditedAlarm(
                isNew = savedInstanceState.getBoolean("isNew"),
                id = savedInstanceState.getInt("id"),
                value = if (savedInstanceState.getBoolean("isEdited")) {
                    Optional.of(
                            AlarmValue(
                                    id = savedInstanceState.getInt("id"),
                                    isEnabled = savedInstanceState.getBoolean("isEnabled"),
                                    hour = savedInstanceState.getInt("hour"),
                                    minutes = savedInstanceState.getInt("minutes"),
                                    daysOfWeek = DaysOfWeek(savedInstanceState.getInt("daysOfWeek")),
                                    isPrealarm = savedInstanceState.getBoolean("isPrealarm"),
                                    alarmtone = Alarmtone.fromString(savedInstanceState.getString("alarmtone")),
                                    label = savedInstanceState.getString("label") ?: "",
                                    isVibrate = true,
                                    state = savedInstanceState.getString("state") ?: "",
                                    nextTime = Calendar.getInstance()
                            )
                    )
                } else {
                    Optional.absent()
                }
        )
    }

    /**
     * Saves EditedAlarm into SavedInstanceState. Counterpart of [editedAlarmFromSavedInstanceState]
     */
    private fun EditedAlarm.writeInto(outState: Bundle?) {
        val toWrite: EditedAlarm = this
        outState?.run {
            putBoolean("isNew", isNew)
            putInt("id", id)
            putBoolean("isEdited", isEdited)

            value.getOrNull()?.let { edited ->
                putInt("id", edited.id)
                putBoolean("isEnabled", edited.isEnabled)
                putInt("hour", edited.hour)
                putInt("minutes", edited.minutes)
                putInt("daysOfWeek", edited.daysOfWeek.coded)
                putString("label", edited.label)
                putBoolean("isPrealarm", edited.isPrealarm)
                putBoolean("isVibrate", edited.isVibrate)
                putString("alarmtone", edited.alarmtone.persistedString)
                putBoolean("skipping", edited.skipping)
                putString("state", edited.state)
            }

            logger.debug { "Saved state $toWrite" }
        }
    }
}
