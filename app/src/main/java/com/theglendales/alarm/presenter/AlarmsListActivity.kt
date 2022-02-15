

package com.theglendales.alarm.presenter

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.ChangeTransform
import android.transition.TransitionSet
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.theglendales.alarm.configuration.EditedAlarm
import com.theglendales.alarm.configuration.Store
import com.theglendales.alarm.configuration.globalGet
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.configuration.globalLogger
import com.theglendales.alarm.interfaces.IAlarmsManager
import com.theglendales.alarm.jjongadd.SecondFragment
import com.theglendales.alarm.logger.Logger
import com.theglendales.alarm.model.AlarmValue
import com.theglendales.alarm.model.Alarmtone
import com.theglendales.alarm.model.DaysOfWeek
import com.theglendales.alarm.util.Optional
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.theglendales.alarm.*
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import com.theglendales.alarm.jjmvvm.mediaplayer.ExoForUrl
import com.theglendales.alarm.jjmvvm.permissionAndDownload.BtmSheetPermission

import com.theglendales.alarm.jjmvvm.permissionAndDownload.MyPermissionHandler
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjmvvm.util.showAlertIfRtIsMissing
import io.reactivex.annotations.NonNull
import io.reactivex.disposables.Disposables
import io.reactivex.functions.Consumer

import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.Calendar


// 30708g (ToolBar 테스트)
// Achievements
//1) Settings 메뉴는 오직 ListFrag 일때만 보이게. DetailsFrag , RtPickerActivity 등에서는 <- 버튼만 보이게끔.. (O)
//2) SettingsActivity 에도 toolbar 적용+ 뒤로가기(<-) 보임 (O)

//Todos:
//1) Burger Menu Icon 안보이고 *(설정) ICON 으로만 사용
//2) Transparent 하게. / RtPickerActivity 에도 적용.
//3) 설정 Page 에 About.. 등 기존 Burger 에 있던 Menu 쓸것만 몇개 넣기.
//4) DARK THEME / 적용 안되게 바꾸기.
//5) AlarmListActivity 에서 setTheme() .. 현재 'Dark Theme' 으로 자동 선택되는듯.  무조건 Default 로 가게끔 -> 기타 코드 없애기
//-- DynamicThemeHandler.kt 확인.. logd 넣어보기.

/**
 * This activity displays a list of alarms and optionally a details fragment.
 */
private const val TAG="*AlarmsListActivity*"

class AlarmsListActivity : AppCompatActivity() {
    private lateinit var mActionBarHandler: ActionBarHandler

    //내가 추가-->
    private val mySharedPrefManager: MySharedPrefManager by globalInject()
    private val myDiskSearcher: DiskSearcher by globalInject()
    private val btmNavView by lazy { findViewById<BottomNavigationView>(R.id.id_bottomNavigationView) as BottomNavigationView }
    private val myPermHandler = MyPermissionHandler(this)
    private val exoForUrl: ExoForUrl by globalInject() // 여기 적혀있지만 init 은 실제 사용되는 SecondFrag 가 열릴 때  자동으로 이뤄짐.
    private lateinit var toolBar: Toolbar
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
                    Log.d(TAG, "hideDetails: called")
                    editing.onNext(EditedAlarm())
                }

                override fun hideDetails(holder: RowHolder) {
                    Log.d(TAG, "hideDetails2: [DetailFrag 에서 cancel/BackButton 누름] ")
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
        Log.d(TAG, "onCreate: !!AlarmsListActivity onCreate() !!! ")

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

        store.alarms().take(1).subscribe { alarms ->
                Log.d(TAG, "onCreate: here before checkRtMissing from phone!!")
                showAlertIfRtIsMissing(this, alarms.map { it.alarmtone })
                //checkPermissions(this, alarms.map { it.alarmtone })
            }.apply { }

// 추가1-A) Second Fragment 관련 -->
    // 2nd Frag 시작과 동시에 일단 SharedPref 파일 자체를 생성해줌. => 일단 사용 안함.
//        val defaultPlayInfo = PlayInfoContainer(-10,-10,-10,StatusMp.IDLE)
//        mySharedPrefManager.savePlayInfo(defaultPlayInfo)  // default 값은 -10, -10, -10, IDLE

        val secondFrag = SecondFragment()
        //val btmNavView = findViewById<BottomNavigationView>(R.id.id_bottomNavigationView)
        btmNavView.setOnNavigationItemSelectedListener {
            // 1) .setOnNav....Listener() 메써드는 onNavigationxxListener(인터페이스를 implement 한 인자를 람다로 받음)
            //2) OnNavigationItemSelectedListener(인터페이스) 안에는 onNavItemSelected(boolean 리턴 메써드) 하나만 있음. 그래서 override 생략 가능sam..
            // 밑에는 한마디로 Override fun onNavItemSelected: Boolean { 이 안에 들어가는 내용임.}
            when(it.itemId) {
                R.id.id_BtmNav_SetAlarm -> configureTransactions()
                R.id.id_BtmNav_RingTone -> showSecondFrag(secondFrag)
                //R.id.id_BtmNav_Settings -> this.startActivity(Intent(this, SettingsActivity::class.java))
            }
            Log.d(TAG, "onCreate: btmNavView.setOnNavigationItemListener -> before hitting true!")
            true
            // we don't write return true in the lambda function, it will always return the last line of that function
        }
    // 추가: Permission 검사 (App 최초 설치시 반드시 거치며, no 했을때는 벤치휭~ BtmSheet 계속 뜬다. yes 하면 그다음부터는 안 뜸.)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // API ~28 이하인 경우에는 Permission Check. API 29 이상에서는 Write, DOWNLOAD 에 특별한 Permission 필요 없는듯?
            Log.d(TAG, "onCreate: Permission Check. Build Ver=${Build.VERSION.SDK_INT}")

            myPermHandler.permissionToWriteOnInitialLaunch() //
        }

        toolBar = findViewById(R.id.id_toolbar)
        setSupportActionBar(toolBar)

    } // onCreate() 여기까지.
// 추가 1-B)-->




override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    myPermHandler.onRequestPermissionsResult(requestCode,permissions, grantResults) //MyPermissionHanlder.kt> onReqPerResult() 로 넘어감.
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
        // Settings Fragment 갔다왔으면
/*    // MyCacher Init() -> MediaPlayer(V2) Init [BackgroundThread] --- 원래 SecondFrag 에 있던것을 이쪽으로 옮겨옴 (ListFrag <-> SecondFrag 왔다리갔다리 무리없게 사용 위해.)
        lifecycleScope.launch {
            Log.d(TAG, "onResume: lifecycle.currentState= ${lifecycle.currentState}, Thread=${Thread.currentThread().name}")
            val myCacherInstance = MyCacher(applicationContext, applicationContext.cacheDir, mediaPlayer_v2)
            myCacherInstance.initCacheVariables() // -> MediaPlayer(V2) Init
        }*/

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        NotificationSettings().checkSettings(this)

    //Permission 관련
        // A) Permission 을 허용하라는 btmSheet 을 보여준뒤 복귀했을때!
        if(BtmSheetPermission.isAdded) {
            if (ContextCompat.checkSelfPermission(this.applicationContext,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            { // user 가 settings>app>perm 에서 허용해줬으면
                BtmSheetPermission.removePermBtmSheetAndResume() // btmSheet 을 없애줌! Perm 허용 안되었으면 (Cancel 누를때까지) BtmSheet 유지!
            }
        }


    }

    override fun onStop() { //RtPickerActivity 갈 때 onStop() 불린다.
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

        exoForUrl.removeHandler()
        exoForUrl.releaseExoPlayer()

    }
    // ActionBarHandler > onCreateOptionsMenu() override.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(TAG, "onCreateOptionsMenu: called")

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 이거 하면 ToolBar 에 백버튼(<-) 버거메뉴 다뜬다!
        supportActionBar?.setDisplayShowHomeEnabled(true)// ActionBar -> ToolBar 로 바꾸고 새로 추가한 Line
        supportActionBar?.setDisplayShowTitleEnabled(true)
        return supportActionBar?.let {mActionBarHandler.onCreateOptionsMenu(menu, menuInflater, it) } // 기존에는 it 으로 ActionBar 를 보냈지만 지금은 toolBar 를 전달.
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

        when(currentFragment)
        {
            is AlarmsListFragment -> { // listFrag 보고 있다 홈 버튼 눌러서 Background 로 앱 갔다 들어왓을 때. (암것도 로딩 안함. 그대로..)
                logger.debug { "skipping fragment transition, because already showing $currentFragment" }
                }
            else -> {
                logger.debug { "transition from: $currentFragment to show list, edited: $edited" }
                // ListFrag 를 로딩>
                    val listFragment = AlarmsListFragment()
                    supportFragmentManager.beginTransaction().apply {
                        this.setCustomAnimations(R.anim.push_down_in, R.anim.my_fade_out_time_short)
                        Log.d(TAG, "showList: not lollipop()")
                    }.replace(R.id.main_fragment_container, listFragment).commitAllowingStateLoss()
                }
        }

    //내가 추가-> btmNavView 다시 보이게 하기 (Detail 들어갈때는 visibility= GONE 으로)
    btmNavView.visibility =View.VISIBLE

    }
    fun showSecondFrag(secondFragReceived: Fragment) =supportFragmentManager.beginTransaction().apply{ //supportFragmentManager = get FragmentManager() class
        replace(R.id.main_fragment_container, secondFragReceived)
        commit() //todo: CommittAllowingStateLoss?
        Log.d(TAG, "showSecondFrag: ..... ")

    }

    private fun showDetails(@NonNull edited: EditedAlarm) {

        val currentFragment = supportFragmentManager.findFragmentById(R.id.main_fragment_container)

        if (currentFragment is AlarmDetailsFragment) {
            logger.debug { "skipping fragment transition, because already showing $currentFragment" }
        } else
        {
            logger.debug { "transition from: $currentFragment to show details, edited: $edited" }

            val detailsFragment = AlarmDetailsFragment().apply {arguments = Bundle()}
            supportFragmentManager.beginTransaction().replace(R.id.main_fragment_container, detailsFragment).commitAllowingStateLoss()
        }
        // 내가 추가- > btmNavView 감추기 (ShowList 에서 visibility= Visible로)
        btmNavView.visibility =View.GONE
        //
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
        Log.d(TAG, "editedAlarmFromSavedInstanceState: jj- artFilePath= ${savedInstanceState.getString("artFilePath")}")
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
                                    nextTime = Calendar.getInstance(),
                                    artFilePath = savedInstanceState.getString("artFilePath") ?: ""
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
                putString("artFilePath", edited.artFilePath)
            }

            logger.debug { "Saved state $toWrite" }
        }
    }
}
