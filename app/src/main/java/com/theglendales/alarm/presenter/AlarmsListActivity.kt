

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
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.bottomappbar.BottomAppBar
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
import com.google.android.material.navigation.NavigationView

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


// 30708V1.18e28 [ListFrag- 요일 색 변경 전] 22/4/3 (Sun) 오후 10:44
//
// ** 하위 API (26 이하) 테스트 해볼것.
// 1) Achievements:
// 요일 단순 Bold + accent Color + Font 변경

// 2) todos:
// Fab 좀 더 키우고 + baseline 일치?? or 약간 위로 보내기?
// 0) ListFrag 요일 수정 -> Sun, Sat 빨간색, 파란색 없애고 다 한가지 색으로 통일, -> 선택되었을 때 네모 감싸기 말고 그냥 text 색 변화+Bold. 아님 위에 점. 로 (폰트 다 바꾸기. Listfrag 에서 Programatically 되고있네.)
// Switch 제발 바꾸자.. ㅅㅂ
//1) PNG 더 찾아보고 -> 여기에 맞춰서 색 설정하기. -> Png 적용 스샷으로 테스트 하고 구입/License 받기.
//https://www.vecteezy.com/vector-art/5437953-coniferous-night-forest-landscape-vector-illustration

//3) SecondFrag 칩 깔쌈하게 해보기.. 흐음.. 연구해볼것. 폰트 색 (회색->흰색으로?) 흐음..포인트 색도 1) PNG 로 최대한 맞춰서 쓰기.
//4) miniplayer design: 폰트 등. 열었을 때도!
//5) 에러 처리 : *** 2nd Frag 에서 나갔다 온 뒤 (+) Create Alarm 작동 문제 있음. Harsh Test 필요. 음악 Play -> ListFrag -> SecondFrag -> 나갔다 오고나서 -> (+) or ListFrag -> ListFrag 암것도 안 떴음 심지어!
//6) Free ITEM -> IAP 절차 생략하고 곧바로 다운로드로. 그런데 아이템 복원에서 skuList 를 따라가니.. 나름 복잡할수도 있음. 그럼에도 FREE 는 반드시 있어야한다!!
//7) Details Frag- Spinner Time Picker



// 전체 색!!!! 결정해야 Divider 색도 결정!!!
// play 할 때 텍스트 색/ AlbuMArt 회색빛 변하는 것.
// -- BtmNav 와 SecondFrag Player 사이 View 회색 Line Border 제대로 넣기 (Umano 에서 보이는 범위를 height dp 말고 '이 View 까지' 는 없을까?)
// Eq Meter? Rubik's Cube 에서 'Purchased' (Owned?) 로 글자로 표기?
// Font 찾아서 적용. $39 인데.. 음 분명 찾으면 있을것 같긴 함.
// 제일 윗칸 Chip 색.


//DetailsFrag> spinning time picker 디자인.
//


//
//font.. textAppearance 스타일별?
// Flaticon 도 그렇고 공짜 Android icon svg vector 찾아서 a) 좀 이쁜놈들로(컬러풀한것도 괜춘) filled/outline 등도 구분 가능한지 알아볼것.
// drawable 등 안 쓰는 asset 지우기 (백업하고)
//- system navigation 은 살짝 다른색으로 할수도 있겠다.

//3) Issues:
// 테스트 중 ListFrag <-> SecondFrag<-> CreateNewAlarm(+) 왔다갔다 하던중 화면 로딩이 멈췄고 재실행시 crash 났음 (listFrag 에서 삭제 logd 에서 listindex error..)
// 이후 재현 불가지만 추후 면밀히 테스트 해볼것.

// border - attr/windowBackground? 요건가? https://www.youtube.com/watch?v=Gmzk9kKA0WI
// 해결책: 어쩔수없이 BtmNav 에 Border/Divider (위에 살짝) 넣는 느낌으로 해주고. 사실상 Spotify 스샷 clone + 카톡스샷(Medium) 느낌으로...?
// 문제ㅐ는 SecondFrag 에서 MiniPlayer 나왔을 때 (모든 기기에서) 완벽하게 맞춰줄 수 있을지. layout_anchor 등 사용?


// Todos:
//- values.xml > colorprimary (Line 22xx) 정도에. // themes_dark.xml 에서 colorPrimary holo_blue_bright 으로 바꾸니 collapsing toolbar 배경색 바꼈네.

// -- City Night Vector 다른 그림으로. 기기별로 늘어나는 것 어떻게 대처할지?


// Todos :

//[알람 울릴 때 Android 12.0 (API 31) 크래쉬는 일단 안 나니 a)추가 테스트+ b)하위 API emulator 테스트 해볼것.]
// 우선은 현재 버전 백업 -> 1.17C 로 다시 복귀 후 다시 현재 상태로 거슬러 올라가기 .. OR 현재 상태에서 api.SDKINT= s(API31) .. 이걸로 조져보기..
// 하위 버전 호환 테스트 API 30만 됐음.
// 하위 버전 호환 테스트 API 30만 됐음.
// ToolBar 꾸미기 (메뉴 없애고 등..)

// RtPickerActivity 에도...
// ToolBar 꾸미기 (메뉴 없애고 등..)
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
    private val btmAppBar by lazy {findViewById(R.id.bottomAppBar2) as BottomAppBar}


    private val addNewAlarmBtn by lazy {findViewById<ImageButton>(R.id.imgBtn_Add_BtmNav)}
    private val myPermHandler = MyPermissionHandler(this)
    private val exoForUrl: ExoForUrl by globalInject() // 여기 적혀있지만 init 은 실제 사용되는 SecondFrag 가 열릴 때  자동으로 이뤄짐.
    // AppBarLayout & ToolBar 관련
    private lateinit var toolBar: Toolbar
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var alarmTimeReminder: FrameLayout
    private lateinit var collapsingTBLayout: CollapsingToolbarLayout

    // Drawer Layout (Navigation View) 왼쪽에서 튀어나오는 메뉴 관련
    private lateinit var toggleDrawer: ActionBarDrawerToggle
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerNavView: NavigationView

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

                    Log.d(TAG, "onBackPressed(Line111): no.1 jj-!!called!! ")
                    return onBackPressed // 마치 LiveData 에서 function 을 Observe 하듯.
                }

            // USER 가 직접 생성하는 알람에 대해서만 createNewAlarm() 이 불림!
                override fun createNewAlarm() {

                    Log.d(TAG, "(line160) createNewAlarm: jj- called. editing.value=${editing.value}")
                    transitioningToNewAlarmDetails.onNext(true)
                    val newAlarm = alarms.createNewAlarm()
                    Log.d(TAG, "(line163) createNewAlarm: newAlarm=$newAlarm, newAlarm.hashCode=${newAlarm.hashCode()}") // 각 1회씩 뜬다.
                // 새로 만든 newAlarm 을 EditedAlarm Object 로 만들어서 ConfigureTransActions()로 보냄
                    val editedAlarm = EditedAlarm(isNew = true, value = Optional.of(newAlarm.data),id = newAlarm.id,holder = Optional.absent())
                    editing.onNext(editedAlarm)
                    Log.d(TAG, "(line 166) createNewAlarm: ----------finished sending via RxJava. editedAlarm=$editedAlarm \n  editing.value=${editing.value}")
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

    override fun onSaveInstanceState(outState: Bundle) { // Background 로 app 나갈 때 이쪽으로 들어와서 saveInstance 함.
        Log.d(TAG, "onSaveInstanceState: called")
        super.onSaveInstanceState(outState)
        outState.putInt("version", BuildConfig.VERSION_CODE)
        uiStore.editing().value?.writeInto(outState)
        Log.d(TAG, "onSaveInstanceState: ${uiStore.editing()}")
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: **** !!AlarmsListActivity onCreate() !!!****")
        window.navigationBarColor = ContextCompat.getColor(applicationContext, R.color.jj_bg_color_2)//System NAV BAR (최하단 뒤로가기/Home 버튼 등 구성되어있는) 배경색 설정

        setTheme(dynamicThemeHandler.getIdForName(AlarmsListActivity::class.java.name))
        super.onCreate(savedInstanceState)

        when { // Activity 가 완전히 종료(Destroy) 되고 다시 onCreate 할 때 기존에 save 된 InstanceState 가 있으면 여기서 불림. (Drawer>Help our team.activity 등 갔다와서는 안 불리네..)
            savedInstanceState != null && savedInstanceState.getInt("version", BuildConfig.VERSION_CODE) == BuildConfig.VERSION_CODE -> {
                val restored = editedAlarmFromSavedInstanceState(savedInstanceState)
                logger.debug { "Restored $this with $restored" }
                Log.d(TAG, "onCreate: restored with saveInstance (restored=$restored)")
                uiStore.editing().onNext(restored)
            }
            else -> { // ** APP Launch 했을 때 일로 들어옴.. (SavedInstanceState 가 없으니깐..)
                val editedAlarm = EditedAlarm() //
                logger.debug { "Created $this with $editedAlarm / editedAlarm.hashCode=${editedAlarm.hashCode()}" } // = Created AlarmsListActivity with EditedAlarm(id=-1)
            }
            // if (intent != null && intent.hasExtra(Intents.EXTRA_ID)) {
            //     //jump directly to editor
            //     uiStore.edit(intent.getIntExtra(Intents.EXTRA_ID, -1))
            // }
        }
        this.mActionBarHandler = ActionBarHandler(this, uiStore, alarms, globalGet())

        val isTablet = !resources.getBoolean(R.bool.isTablet)
        if (isTablet) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT //todo: 종맨 Table Orientation check.
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

        btmNavView.setOnItemSelectedListener {
            //btmNavView.menu.setGroupCheckable(0, true, true)
            when(it.itemId) {
                R.id.id_BtmNav_SetAlarm -> {showList(isCreateNewClicked = false)
                    it.isChecked = true
                    btmNavView.menu.findItem(R.id.id_BtmNav_RingTone).isChecked = false
                }
                R.id.id_BtmNav_RingTone -> {showSecondFrag(secondFrag)
                    btmNavView.menu.findItem(R.id.id_BtmNav_SetAlarm).isChecked = false
                    it.isChecked = true

                }
            }
            Log.d(TAG, "onCreate: btmNavView.setOnNavigationItemListener \n   -> it.title=${it.title}, it.isChecked=${it.isChecked}")
            true // we don't write return true in the lambda function, it will always return the last line of that function
        }
        //btmNavView.itemIconTintList = null
    // 추가: Permission 검사 (App 최초 설치시 반드시 거치며, no 했을때는 벤치휭~ BtmSheet 계속 뜬다. yes 하면 그다음부터는 안 뜸.)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { // API ~28 이하인 경우에는 Permission Check. API 29 이상에서는 Write, DOWNLOAD 에 특별한 Permission 필요 없는듯?
            Log.d(TAG, "onCreate: Permission Check. Build Ver=${Build.VERSION.SDK_INT}")

            myPermHandler.permissionToWriteOnInitialLaunch() //
        }
        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.S) { // S = API 31
            myPermHandler.permissionReadPhoneState()

        }
    // Toolbar & appbarLayout
        toolBar = findViewById(R.id.id_toolbar_Collapsable) // Collapsible toolBar
        setSupportActionBar(toolBar)
        //supportActionBar?.setDisplayShowHomeEnabled(true)
        appBarLayout = findViewById(R.id.id_appBarLayout) // 늘였다 줄였다 관장하는 App Bar Layout
        collapsingTBLayout = findViewById(R.id.id_collapsingToolBarLayout)
        collapsingTBLayout.isTitleEnabled = false // 일단 화면에 제목 안 보여주고 시작 -> 추후 AppBarLayout 줄어들면 보여주기.

        // xx 시간 후에 알람이 울립니다
        alarmTimeReminder = findViewById(R.id.alarmTimeReminder) //FrameLayout

    // Drawer Layout (navigation view)
        drawerLayout = findViewById(R.id.id_drawerLayout) //String Resource= 시각 장애인 위함.
        drawerNavView = findViewById(R.id.id_nav_view)
        toggleDrawer = ActionBarDrawerToggle(this, drawerLayout,toolBar,R.string.openNavBlind, R.string.closeNavBlind)// Drawer (Navigaton View)열리는 Toggle
        drawerLayout.addDrawerListener(toggleDrawer)
        toggleDrawer.syncState() // 그냥 이제 사용준비 되었다는 의미라네.
        // Nav Menu 클릭 했을 때 -> ActionBar Handler > onOptionsItemSelected 로 보냄.
        drawerNavView.setNavigationItemSelectedListener {
            drawerLayout.closeDrawers() // 메뉴 선택했으니 Drawer 닫아줄것.
            mActionBarHandler.onOptionsItemSelected(it)
        }

    // AppBaR Expand/Collapse Listener. AppBar 가 (72% 이상) 접혔을 때 'xHrxMn 후 울립니다' 표기하는 View 를 없애줌.
        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
        //todo: 실제 기기별로 totalScrollRange 가 다를것임. 실 기기에서 Double Check. (Ex. 에뮬레이터 8 FOLDABLE 로 했을 때 totalScrollRange = 357)
            // totalScrollRange = 전체 스크롤 가능 범위 Ex) 갤 S20 에서 totalScrollRange = 378, verticalOffset: 완전 확장시 (=0), 완전 Collapsed = -378 // 기기마다 값은 다를것으로 예상됨.
            val percentClosed = (kotlin.math.abs(verticalOffset).toFloat() / appBarLayout.totalScrollRange.toFloat() * 100).toInt()
            //Log.d(TAG, "onCreate: verticalOffset=$verticalOffset, totalScrollRange = ${appBarLayout.totalScrollRange}, percentClosed=$percentClosed")

            when(percentClosed) {
                in 0..72 -> { // xx 시간 후 울립니다 표시(O) // 0 -> 72 이하.
                    if(alarmTimeReminder.visibility == View.GONE) {
                        //Log.d(TAG, "onCreate: alarmTimeReminder: Gone -> Visible 로 만들기!")
                        alarmTimeReminder.visibility = View.VISIBLE
                        collapsingTBLayout.isTitleEnabled=false
                    }
                }
                in 73..100 -> { // xx 시간 후 울립니다 표시(X) // (73 이상 100 이하)
                    // 이미 View 가 Hide 되어있으면 return
                    if(alarmTimeReminder.visibility == View.VISIBLE) {
                        //Log.d(TAG, "onCreate: alarmTimeReminder: Visible-> Gone 으로 만들기!")
                        alarmTimeReminder.visibility = View.GONE
                    }
                    if(percentClosed>90) {
                        collapsingTBLayout.isTitleEnabled=true
                    } else if(percentClosed <90) {
                        collapsingTBLayout.isTitleEnabled=false
                    }
                } //
            }

        })

        addNewAlarmBtn.setOnClickListener { showList(isCreateNewClicked = true)

        }

    } // onCreate() 여기까지.
// 추가 1-B)-->


    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        myPermHandler.onRequestPermissionsResult(requestCode,permissions, grantResults) //MyPermissionHandler.kt> onReqPerResult() 로 넘어감.
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


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onResume() {
        Log.d(TAG, "onResume: jj-called. subscriptions.toString=${subscriptions.toString()}")
        super.onResume()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        NotificationSettings().checkSettings(this)
        //Permission 관련
        // A) Permission 을 허용하라는 btmSheet 을 보여준뒤 복귀했을때! [WRITE_EXTERNAL_STORAGE]
        if(BtmSheetPermission.isAdded) {
            if (ContextCompat.checkSelfPermission(this.applicationContext,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            { // user 가 settings>app>perm 에서 허용해줬으면
                BtmSheetPermission.removePermBtmSheetAndResume() // btmSheet 을 없애줌! Perm 허용 안되었으면 (Cancel 누를때까지) BtmSheet 유지!
            }
        }

        Log.d(TAG, "onResume: supportFrag.fragments=${supportFragmentManager.fragments}")
        //permission.SCHEDULE_EXACT_ALARM 퍼미션 됐는지 확인작업 + read_phone_storage permission 관련. =>MANIFEST 에 써놓았으니 생략 가능?
       /* val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val hasPermission: Boolean = alarmManager.canScheduleExactAlarms()
        Log.d(TAG, "onResume: Schedule Exact.. hasPermission=$hasPermission") // 현재 true 로 뜬다.*/
      /*  if(Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {

            Log.d(TAG, "onCreate: read_phonestorage 체크..int=${ContextCompat.checkSelfPermission(this.applicationContext, android.Manifest.permission.READ_PHONE_STATE)}")
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_PHONE_STATE), 8914)
            Log.d(TAG, "onCreate: read_phonestorage 체크..int=${ContextCompat.checkSelfPermission(this.applicationContext, android.Manifest.permission.READ_PHONE_STATE)}")
        }*/
        // Settings Fragment 갔다왔으면
/*    // MyCacher Init() -> MediaPlayer(V2) Init [BackgroundThread] --- 원래 SecondFrag 에 있던것을 이쪽으로 옮겨옴 (ListFrag <-> SecondFrag 왔다리갔다리 무리없게 사용 위해.)
        lifecycleScope.launch {
            Log.d(TAG, "onResume: lifecycle.currentState= ${lifecycle.currentState}, Thread=${Thread.currentThread().name}")
            val myCacherInstance = MyCacher(applicationContext, applicationContext.cacheDir, mediaPlayer_v2)
            myCacherInstance.initCacheVariables() // -> MediaPlayer(V2) Init
        }*/
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(TAG, "onCreateOptionsMenu: called")
        return false
        // 세로 점 세개 (overflow menu) 를 사용하고 싶으면 아래를 활성화 시킬것.
//        supportActionBar?.setDisplayShowTitleEnabled(false)
//        return supportActionBar?.let {mActionBarHandler.onCreateOptionsMenu(menu, menuInflater, it) } // 기존에는 it 으로 ActionBar 를 보냈지만 지금은 toolBar 를 전달.
//                ?: false
    }

    // ** NavViewClickListener 사용 후 이건 더 이상 안 들어오는듯..
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "onOptionsItemSelected: called")
        if(toggleDrawer.onOptionsItemSelected(item)) {
            return true // user 가 toggle 을 click 했으면 -> true 를 보냄.
        }
        return mActionBarHandler.onOptionsItemSelected(item)

    }
// onBackPressed no.2
    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed: no.2 - onNext") // 이건 print 되지 않고 밑에 pSubject 만 자동반응 업뎃..?
        uiStore.onBackPressed().onNext(AlarmsListActivity::class.java.simpleName)
    }
// ***** !!! 여기서 showList() 로 감!!!! *****
    private fun configureTransactions() { // 주의: 만약 configureTransActions() 가 n번 불리면, Subscribe 를 n번함.. -> ListFrag 에서 FAB 버튼/ DetailsFrag 열려고 할 때 우수수 열림.
    Log.d(TAG, "configureTransactions: called")

    // uiStore.editing().subscribe = UiStore Class 의 editing (BehaviorSubject = LiveData 의 Emit 으로 생각하면 편함) 을 subscribe (=observe)
    // USER 가 FAB 버튼 누르거나 ListFrag 의 알람칸 클릭했을 때 여기서 uiStore.editing 을 '구독중(=observe)' 하다 알아서 DetailsFrag 보여줌.
        subscriptions = uiStore.editing().distinctUntilChanged { editedAlarm -> editedAlarm.isEdited } //distinctUntilChanged= 중복 filtering (ex. 1,2,2,3,1,1,2 => 1,2,3,1,2 만 받음) +  .isEdited=true 인 놈만 걸름.
                .subscribe(Consumer { editedAlarm ->
                    Log.d(TAG, "[SUB] (line486!)configureTransactions: jj- editedAlarm.isEdited= ${editedAlarm.isEdited}, editedAlarm.hashCode= ${editedAlarm.hashCode()}") // editedAlarm 는 (type) EditedAlarm

                    when {
                        lollipop() && isDestroyed -> return@Consumer
                        editedAlarm.isEdited -> showDetails(editedAlarm)
                        else -> {
                            Log.d(TAG, "[SUB] (line490)configureTransactions: else->showList() 안!! editedAlarm=$editedAlarm")
                            showList(isCreateNewClicked = false)}
                    }
                })
    }

// 알람 리스트를 보여주는 !! AlarmsListFragment 로 전환!! 중요!!
    private fun showList(isCreateNewClicked: Boolean) {
    //추가->
    Log.d(TAG, "(Line281)showList: jj-called")
    appBarLayout.setExpanded(true,true) // A) ToolBar 포함된 넓은 부분 Expand 시키기!
    btmAppBar.setBackgroundResource(R.drawable.btm_nav_bg_round_corner) // SecondFrag 에서 돌아왔을 때 Corner 가 직사각형 -> Round Corner 로 다시 변경.

    // 혹시나 Btm Nav> Set ALARM 의 메뉴 아이콘이 isChecked bool 값이 틀린 경우 바꿔주기(회색 -> 흰색)(Ex. SecondFrag 에서 (+) 누른 경우
    // Mystery..  : 여기서 if 문을 안해주고 그냥 .isChecked=true 해주면 오히려 결과값이 반대(false) 가 된다 .. ?? 그래서 if 문 붙여줬음.
    val listFragMenuItem = btmNavView.menu.findItem(R.id.id_BtmNav_SetAlarm)
    val secondFragMenuItem = btmNavView.menu.findItem(R.id.id_BtmNav_RingTone)
    if(!listFragMenuItem.isChecked) {
        listFragMenuItem.isChecked = true
    }
    if(secondFragMenuItem.isChecked) {
        secondFragMenuItem.isChecked = false
    }



//<-추가
    val currentFragment = supportFragmentManager.findFragmentById(R.id.main_fragment_container) // first searches through fragments that are currently added to the manager's activity

    when(currentFragment)
    {
        is AlarmsListFragment -> { // listFrag 보고 있다 홈 버튼 눌러서 Background 로 앱 갔다 들어왓을 때. (암것도 로딩 안함. 그대로..)
            logger.debug { "skipping fragment transition, because already showing $currentFragment" }
            }
        else -> {
            //logger.debug { "transition from: $currentFragment to show list, editedAlarm.hashCode= ${editedAlarm.hashCode()}, edited: $editedAlarm" }
            // ListFrag 를 로딩>
                val listFragment = AlarmsListFragment()
            Log.d(TAG, "showList: created New ListFrag = $listFragment, ListFrag.hashCode= ${listFragment.hashCode()}")
                supportFragmentManager.beginTransaction().apply {
                    this.setCustomAnimations(R.anim.push_down_in, R.anim.my_fade_out_time_short)
                    Log.d(TAG, "showList: not lollipop()")
                }.replace(R.id.main_fragment_container, listFragment).commitNowAllowingStateLoss()

        }
    }

//내가 추가->
// a) btmNavView 다시 보이게 하기 (Detail 들어갈때는 visibility= GONE 으로)
    btmNavView.visibility =View.VISIBLE
    btmAppBar.visibility= View.VISIBLE

// b) AppBarLayout 설정변경 -> ToolBar 보여주는데까지 Collapse 시키기(O)
    //if(supportActionBar != null) {supportActionBar?.show()}
    val params = collapsingTBLayout.layoutParams as AppBarLayout.LayoutParams
    params.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
    // % 참고: Java 에서는 params.setScrollFlags(xxx SCROLL | FLAG_EXIT ... ) 이런식으로 '|' 파이프라인을 쓰는데. 그게 Kotlin 에서는 or 임. bitwise INT... 흐음.
// c) 만약 (+) 버튼 눌러서 여기로 온거면 바로 새 알람 생성(->DetailsFrag)
    if(isCreateNewClicked) {
        uiStore.createNewAlarm()
    }
    Log.d(TAG, "showList: BtmNav_SetAlarm.isChecked=${btmNavView.menu.findItem(R.id.id_BtmNav_SetAlarm).isChecked} , BtmNav_RingTone.isChecked=${btmNavView.menu.findItem(R.id.id_BtmNav_RingTone).isChecked}")


}
private fun showSecondFrag(secondFragReceived: Fragment) =supportFragmentManager.beginTransaction().apply{ //supportFragmentManager = get FragmentManager() class
// ListFrag 나 DetailsFrag 는 서로 이동시에는 subscription 으로 EditeAlarm 받아서 이동하지만, BtmNav 로 오갈때  열릴 때 configureTransActions() 함수가 불리면서 계속 subscribe 함.
    //subscriptions.dispose() // todo: 추후 a) 신규알람생성(createNewAlarm-FAB 버튼)시 TimePicker 가 Spinner TimePicker 에 반영 안되는문제 발생 or
    //todo: b) Second Frag N번 왔다갔다 한 후 Details Frag 열 때 log 에  '다수의 ListFrag 로부터의 이동' 이 뜨면. 해당 subscriptions.dispose 다시 활성화해보기.
// A) ToolBar 포함된 넓은 부분 Collapse 시키기!
    appBarLayout.setExpanded(false,true)
// B) SecondFrag 로딩
    replace(R.id.main_fragment_container, secondFragReceived)
    commit() //현재 상태에서 특별히 쓸 이유 안 보임.

// D) AppBarLayout 설정변경 -> 완전히 Collapse (ToolBar 영역도 무시)
    val params = collapsingTBLayout.layoutParams as AppBarLayout.LayoutParams
    params.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS// Scroll|snap|enterAlways

    Log.d(TAG, "showSecondFrag: ..... ")

}


private fun showDetails(@NonNull editedAlarm: EditedAlarm) {
    Log.d(TAG, "showDetails: called. ")
    appBarLayout.setExpanded(false,true) // A)ToolBar 포함된 넓은 부분 Collapse 시키기!

    val currentFragment = supportFragmentManager.findFragmentById(R.id.main_fragment_container)

    if (currentFragment is AlarmDetailsFragment) {
        logger.debug { "skipping fragment transition, because already showing $currentFragment" }
    } else
    {
        logger.debug { "transition from: $currentFragment to show details, editedAlarm.Hashcode= ${editedAlarm.hashCode()}, edited: $editedAlarm" }

        val detailsFragment = AlarmDetailsFragment().apply {arguments = Bundle()}
        supportFragmentManager.beginTransaction().replace(R.id.main_fragment_container, detailsFragment).commitAllowingStateLoss()
    //todo: [혹시 showDetails x2 뜨는것 방지 위해 ].CommitNow()(Synchronous) 가 나을것 같은데.. 느려질려나?
    }
    // 내가 추가- > btmNavView 감추기 (ShowList 에서 visibility= Visible로)
    btmNavView.visibility =View.GONE
    btmAppBar.visibility= View.GONE

    // C) AppBarLayout 설정변경 -> 완전히 Collapse (ToolBar 영역도 무시)
    val params = collapsingTBLayout.layoutParams as AppBarLayout.LayoutParams
    params.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS// Scroll|snap|enterAlways


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
