

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


// v0.20a
// <앱 시작과 동시에 백그라운드에서 rta, art 파일 핸들링하는 작업>
//1. 앱 시작-> AlarmListActivity -> onCreate() 안에서
//a) 앱 최초 실행인지 확인 -> 인스톨 process (Package 안에 있는 rta 파일 설치->추출까지)
//b) DiskScan 이 필요한지 확인: // rta & art 파일이 매칭하는지 / artPath 가 null 값인게 있는지(신규 다운로드 음원) // SharedPref 파일 자체가 없는지 등
//b-1-a) DiskScan 필요한 경우-> Coroutine 으로 스캔 돌리고 => resultList 를 SharedPref 에 저장
//b-1-b) DiskScan 필요한 경우-> Coroutine 으로 스캔 돌리고 => resultList 를 DiskSearcher.kt>finalRtArtPathList (Companion obj 메모리) 에 띄워놓음(갱신)
//b-2) 스캔이 필요없음-> 그냥 SharedPref 에 있는 리스트를 받아서 -DiskSearcher.kt>finalRtArtPathList (Companion obj 메모리) 에 띄워놓음(갱신)

// 현재 RtaArtPathList.xml(Shared Pref) 저장/Read 까지 잘됨.
// 현재 artUri 자체가 아예 안 심어져있는 .rta (mp3) 도 있기에 무조건 b-1) 로 실행이 될것임 -> 모두 잘되는 .rta 로 SharedPref 잘 읽히는지 테스트해보기.
// AlarmDetailsFrag 에서 이제 spinner 업데이트를 finalRtArtPathList 놈을 사용해서 빠르게 해보자 (DiskSearcher.kt> Companion obj 메모리)
// 다른 알람버튼 눌렀을때, fab 눌렀을 때 - crash




/**
 * This activity displays a list of alarms and optionally a details fragment.
 */
private const val TAG="*AlarmsListActivity*"

class AlarmsListActivity : AppCompatActivity() {
    private lateinit var mActionBarHandler: ActionBarHandler

    //내가 추가-->
    val mySharedPrefManager: MySharedPrefManager by globalInject()
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
                    Log.d(TAG, "onBackPressed(Line93): no.1 jj-!!called!!")
                    return onBackPressed
                }

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
                    alarms.getAlarm(id)?.let { alarm ->
                        editing.onNext(EditedAlarm(
                                isNew = false,
                                value = Optional.of(alarm.data),
                                id = id,
                                holder = Optional.absent()))
                    }
                }

                override fun edit(id: Int, holder: RowHolder) {
                    alarms.getAlarm(id)?.let { alarm ->
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

// 추가1) Second Fragment 관련 -->
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
// <--추가1) Second Fragment 관련

// 추가2) --> .rta .art 파일 핸들링 작업 (앱 시작과 동시에)
        //a) 앱 최초 실행인지 확인 : /.AlarmRingtones Folder 나 /.AlbumArt Folder 가 존재 안함 (or 폴더가 비어있는 상태)  -> install -> rta , art 폴더 생성 후 저장.
        if(myDiskSearcher.isInitialLaunch()) {
            //todo: Package 에 있는 rta 파일 설치-> art 추출까지..
        }
        //b) DiskSearcher.rtOnDiskSearcher() 를 실행할 필요가 있는지 bool 값 확인
            // b-1) 실행이 필요함(O) [신규 다운로드 후 rta 파일만 추가되었거나, 최초 app 실행, user 삭제, 오류 등.. rt (.rta) 중 art 값이 null 인 놈이 있거나 등]
            if(myDiskSearcher.isDiskScanNeeded()) { // 만약 새로 스캔 후 리스트업 & Shared Pref 저장할 필요가 있다면
                Log.d(TAG, "onCreate: $$$ Alright let's scan the disk!")
                // rtOnDiskSearch 를 시작-> S
                CoroutineScope(Dispatchers.IO).launch {

                    myDiskSearcher.readAlbumArtOnDisk()
                    val resultList = myDiskSearcher.rtOnDiskSearcher() // rtArtPathList Rebuilding 프로세스. resultList 는 RtWAlbumArt object 리스트고 각 Obj 에는 .trkId, .artPath, .audioFileUri 등의 정보가 있음.
                // b-1-a) 받은 정보를 얼른 Shared Pref 에다 저장!
                    mySharedPrefManager.saveRtaArtPathList(resultList)
                // b-1-b) DiskSearcher.kt>finalRtArtPathList (Companion obj 메모리) 에 띄워놓음(갱신)
                    myDiskSearcher.updateList(resultList)
                    Log.d(TAG, "onCreate: rebuilding Shared Pref DONE..(Hopefully..) resultList = $resultList!")
                }

            }
            //b-2) Scan 이 필요없음(X)!!! 여기서 SharedPref 에 있는 리스트를 받아서 -> DiskSearcher.kt>finalRtArtPathList (Companion obj 메모리) 에 띄워놓음(갱신)
            else if(!myDiskSearcher.isDiskScanNeeded()) {
                val resultList = mySharedPrefManager.getRtaArtPathList()
                Log.d(TAG, "onCreate: XXX no need to scan the disk. Instead let's check the list from Shared Pref => resultList= $resultList")

            }


    // B) 현재 /.AlbumArt 에 있는 albumArt 그래픽 파일들을 우선 READ->
        //myDiskSearcher.readAlbumArtOnDisk() // -> 완료되면 DiskSearcher.kt>  onDiskArtMap <trkId, Path> 가 완성됨. 완성되기 전에 DetailsFrag 에 들어갔을때는 myDiskSearcher.rtOnDiskSearch() 에서 보완!

// <-- 추가2)
    } // onCreate() 여기까지.
// 추가 -->

    fun jjSetCurrentFragment(receivedFragment: Fragment) =
        supportFragmentManager.beginTransaction().apply{ //supportFragmentManager = get FragmentManager() class

            replace(R.id.main_fragment_container, receivedFragment)
            commit()
            Log.d(TAG, "jjSetCurrentFragment: .... ")
        }

// <--추가

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
        Log.d(TAG, "(line264)configureTransactions: . Begins.")
        subscriptions = uiStore.editing()
                .distinctUntilChanged { edited -> edited.isEdited }
                .subscribe(Consumer { edited ->
                    Log.d(TAG, "(line268!)configureTransactions: jj- edited= $edited")
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
                    exitTransition = Fade()
                }
            }
            //Below is equivalent to : val listFragment= AlarmsListFragment()
        //                          listFragment.sharedElementEnter = moveTransition()
        //                          listFragment.enterTransition = Fade() .. 기타 등등.
            val listFragment = AlarmsListFragment().apply {
                lollipop {
                    sharedElementEnterTransition = moveTransition()
                    enterTransition = Fade()
                    allowEnterTransitionOverlap = true
                }
            }

            supportFragmentManager.beginTransaction()
                    .apply {
                        lollipop { // SDK 21 인듯
                            edited.holder.getOrNull()?.addSharedElementsToTransition(this)
                        }
                    }
                    .apply {
                        if (!lollipop()) {
                            this.setCustomAnimations(R.anim.push_down_in, R.anim.my_fade_out_time_short)
                        }
                    }
                    .replace(R.id.main_fragment_container, listFragment)
                    .commitAllowingStateLoss()
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
                    exitTransition = Fade()
                }
            }

            val detailsFragment = AlarmDetailsFragment().apply {
                arguments = Bundle()
            }.apply {
                lollipop {
                    enterTransition = TransitionSet().addTransition(Slide()).addTransition(Fade())
                    sharedElementEnterTransition = moveTransition()
                    allowEnterTransitionOverlap = true
                }
            }

            supportFragmentManager.beginTransaction()
                    .apply {
                        if (!lollipop()) { //lollipop = SDK 21인듯..
                            this.setCustomAnimations(R.anim.push_down_in, R.anim.my_fade_out_time_short)
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
