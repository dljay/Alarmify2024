package com.theglendales.alarm.presenter

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.AdapterView.AdapterContextMenuInfo
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amulyakhare.textdrawable.TextDrawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.snackbar.Snackbar

import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.Layout
import com.theglendales.alarm.configuration.Prefs
import com.theglendales.alarm.configuration.Store
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.configuration.globalLogger
import com.theglendales.alarm.interfaces.IAlarmsManager
import com.theglendales.alarm.jjadapters.GlideApp
import com.theglendales.alarm.logger.Logger
import com.theglendales.alarm.model.AlarmValue
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import com.theglendales.alarm.jjmvvm.util.checkIfRtIsUnplayable
import com.theglendales.alarm.jjongadd.LottieDiskScanDialogFrag
import com.theglendales.alarm.jjongadd.TimePickerJjong
import com.theglendales.alarm.model.Alarmtone
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import java.util.ArrayList
import java.util.Calendar

/**
 * Shows a list of alarms. To react on user interaction, requires a strategy. An
 * activity hosting this fragment should provide a proper strategy for single
 * and multi-pane modes.
 *
 * @author Yuriy
 */
private const val TAG="*AlarmsListFragment*"
private const val SHOW_ANIM="showANIM"
private const val HIDE_ANIM="hideANIM"


class AlarmsListFragment : Fragment() {
    private val alarms: IAlarmsManager by globalInject()
    private val store: Store by globalInject()
    private val uiStore: UiStore by globalInject()
    private val prefs: Prefs by globalInject()
    private val logger: Logger by globalLogger("AlarmsListFragment")

    //private val mAdapter: AlarmListAdapter by lazy { AlarmListAdapter(R.layout.list_row_classic, R.string.alarm_list_title, ArrayList()) }

    private val mAdapter: AlarmListRcvAdapter by lazy { AlarmListRcvAdapter(R.layout.list_row_classic, R.string.alarm_list_title, ArrayList()) } // Rcv 로 교체 했음!!
    private val inflater: LayoutInflater by lazy { requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater }

    private var alarmsSub: Disposable = Disposables.disposed()
    private var backSub: Disposable = Disposables.disposed()
    private var timePickerDialogDisposable = Disposables.disposed()

// 내가 추가-->
    private var unSavedAlarmsList = mutableListOf<AlarmValue>() // [Fab 버튼으로 신규 알람 생성중 APP '강제종료' 했을 때 -> Save 안된 알람이 담길 리스트 (아래 Subscribe 에서 찾아서 채워줄 예정)
    private val toastMessenger: ToastMessenger by globalInject() //ToastMessenger
    //lateinit var lottieAnimView: LottieAnimationView //Lottie Animation(Loading & Internet Error)
    lateinit var lottieDialogFrag: LottieDiskScanDialogFrag
    //lateinit var myPermHandler: MyPermissionHandler
    private val mySharedPrefManager: MySharedPrefManager by globalInject()
    private val myDiskSearcher: DiskSearcher by globalInject()

    private val myTimePickerJjong: TimePickerJjong by globalInject()

    //lateinit var listView: ListView // 기존에는 onCreateView 에서 그냥 val listView 해줬었음.

    //"알람 선택된 요일" 표시용 동그래미 Text Drawable
    private val yesAlarmSun = getYesAlarmDayDrawable("Sun")
    private val yesAlarmMon = getYesAlarmDayDrawable("M")
    private val yesAlarmTue = getYesAlarmDayDrawable("T")
    private val yesAlarmWed = getYesAlarmDayDrawable("W")
    private val yesAlarmThu = getYesAlarmDayDrawable("T")
    private val yesAlarmFri = getYesAlarmDayDrawable("F")
    private val yesAlarmSat = getYesAlarmDayDrawable("Sat")

    private var isLottiePlayedOnce = false // DiskScan 에서 한번이라도 Lottie 애니메이션이 재생됐는지 확인( -> recoverMissingPurchasedFiles() 때도 틀어줘야될수도 있거덩)
//내가 추가<-

    /** changed by [Prefs.listRowLayout] in [onResume]*/
    private var listRowLayoutId = R.layout.list_row_classic

    /** changed by [Prefs.listRowLayout] in [onResume]*/
    private var listRowLayout = prefs.layout()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttach: called")

        lottieDialogFrag = LottieDiskScanDialogFrag.newInstanceDialogFrag()


    //최초 Data INSTALL 및 신규다운로드 후 listFrag 시작 / rta 나 art 파일이 삭제& 갯수 매칭 안될때. -----------------
        // 1) DiskSearcher.downloadedRtSearcher() 를 실행할 필요가 있는경우(O) (최초 앱 설치 or 신규 다운로드 등.. )
        // [신규 다운로드 후 rta 파일만 추가되었거나, user 삭제, 오류 등.. rt (.rta) 중 art 값이 null 인 놈이 있거나 등]
        if(myDiskSearcher.isDiskScanNeeded()) { // 만약 새로 스캔 후 리스트업 & Shared Pref 저장할 필요가 있다면
            Log.d(TAG, "onCreate: $$$ Alright let's scan the disk!")
            // ** diskScan 시작 시점-> ANIM(ON)!
            showLottieDialogFrag()
            // 2.5초후에 애니메이션 없애기->  loop=false, repeat 없이 보여주기.(lottie_rebuild_rt.xml)
            // 만약 여기서 handler 문제가 생겨서 안 없어지면 Lottie 다 재생하고( 약 5초) LottieDiskScanDiaFrag.kt > onAnimationEnd 로 DialFrag 자체를 없애줌!
            val handler: Handler = Handler(Looper.getMainLooper())
            handler.postDelayed({hideLottieAndShowSnackBar()}, 2500) // todo: handler 는 올바른 방법이 아니라고 보긴 봤음.. 일단은 잘되네.


            //CoroutineScope(Dispatchers.IO).launch { <== ** 일부러 코루틴에서 제외-> 그래야 여기서 update SharedPref 등이 끝나고나서 밑에 innerClass>getView 실행됨.
            //코루틴 안 쓰고 DiskScan 가동시에는 어떻게든 Animation 으로 시간 끌기?
                //lottieAnimCtrl(SHOW_ANIM)
            //1-a) /.AlbumArt 폴더 검색 -> art 파일 list up -> 경로를 onDiskArtMap 에 저장 <trkId, ArtPath>
                myDiskSearcher.readAlbumArtOnDisk()
            //1-b-1) onDiskRtSearcher 를 시작-> search 끝나면 Default Rt(raw 폴더) 와 List Merge!
                val resultList = myDiskSearcher.onDiskRtSearcher() // rtArtPathList Rebuilding 프로세스. resultList 는 RtWAlbumArt object 리스트고 각 Obj 에는 .trkId, .artPath, .audioFileUri 등의 정보가 있음.
            //** 1-b-2) 1-b-1) 과정에서 rtOnDisk object 의 "artFilePathStr" 이 비어잇으면-> extractArtFromSingleRta() & save image(.rta) on Disk

            // 1-c) Merge 된 리스트(rtWithAlbumArt obj 로 구성)를 얼른 Shared Pref 에다 저장! (즉 SharedPref 에는 art, rta 의 경로가 적혀있음)
                mySharedPrefManager.saveRtOnThePhoneList(resultList)
            // 1-d) DiskSearcher.kt>finalRtArtPathList (Companion obj 메모리) 에 띄워놓음(갱신)
                myDiskSearcher.updateList(resultList)
                Log.d(TAG, "onCreate: --------------------------- DiskScan DONE..(Hopefully..)---------- \n\n resultList = $resultList!")
            //} // ** diskScan 종료 <--

        }

        //2) Scan 이 필요없음(X)!!! 여기서 SharedPref 에 있는 리스트를 받아서 -> DiskSearcher.kt>finalRtArtPathList (Companion obj 메모리) 에 띄워놓음(갱신)
        else if(!myDiskSearcher.isDiskScanNeeded()) {
            val resultList = mySharedPrefManager.getRtOnThePhoneList()

            Log.d(TAG, "onCreate: XXX no need to scan the disk. Instead let's check the list from Shared Pref => resultList= $resultList")
            myDiskSearcher.updateList(resultList)

        }
        //1-a. isDiskScanNeeded -> false -> Lottie Anim (X)
        //1-b. isDiskScanNeeded -> true -> Lottie Anim (O)
        //2-a. isMissingPurchasedFiles -> false -> Snackbar: Rebuilding DB Completed.
        //2-b. isMissingPurchasedFiles -> true -> Lottie Anim (O)--재생중이거나 1-b 에서 한번 재생했으면 생략 -> download -> Snackbar: Recovering.. please restart later.
    }
//<1> (내가 작성) RcV Adapter --------------->
    inner class AlarmListRcvAdapter(alarmTime: Int,label: Int,private var alarmValuesList: List<AlarmValue>) : RecyclerView.Adapter<RowHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup,viewType: Int): RowHolder {
            // <1> 여기서 RowHolder 를 담을 View 를 기제작된 xml 을 통해 제공
            val view = LayoutInflater.from(parent.context).inflate(listRowLayoutId, parent, false) // 선택 상황에 따라 -> R.layout.list_row_classic or else..

            // <2> RowHolder() Instance 생성: 위에서 받은 View 에 원래 딸려있는 button, textView 등을 findViewById() 로 찾아주고 -> 그것을 RowHolder 형태로 onBindViewHolder() 에 제공.
            val rowHolder = RowHolder(view,0,prefs.layout()) // 기존에 Yuriv 가 작성해놓은 코드가 RowHolder 제작시 alarmId 를 넣게되어있음. (ListView 사용했을 때는 가능했지)
            // 하지만 우리는 ListView 를 RcView 로 교체를 했고 여기서는 position 정보가 없어서 alarmId 를 알 수 없으니 그냥 '0' 으로 제공.
            // 중요한것은 ViewHolder 인터페이스를 승계한 RowHolder instance를 onBindViewHolder 에 넘긴다는 것. 추후 onBindViewHolder 에서 position 통해 alarmId 찾아서 채워줄 예정.
            rowHolder.apply { digitalClock.setLive(false) } // .setLive 는 현재 시간을 보여줌! 우리는

            return rowHolder

        }
        // <3> 제공받은 ViewHolder 의 UI(button, textView 등)들은 모두 findViewById() 가 끝났으니, 이제 여기서 내용을 채워줌!
        override fun onBindViewHolder(rowHolder: RowHolder, position: Int) {
            val alarm = alarmValuesList[position]
            val alarmId = alarm.id

        /**
         * 위에서 받은 Position 별 알람의 정보로 각 Alarm 의 실 내용물 채워주기
         */
        //a) 알람 on/off 여부
            rowHolder.onOff.isChecked = alarm.isEnabled
        //b) set the alarm text
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, alarm.hour)
            c.set(Calendar.MINUTE, alarm.minutes)
            rowHolder.digitalClock.updateTime(c)
        //c-1) Row 의 AlbumArt 에 쓰일 아트 Path 읽고
            var artPathFromAlarmValue: String? = alarm.artFilePath // +++
            val alarmtoneList: List<Alarmtone> = listOf<Alarmtone>(alarm.alarmtone) // 사실 alarmtone 한개인데 checkIfRtIsMissing 이 List<Alarmtone> 을 받게끔 디자인되어있어서.
        //c-2) Glide 로 이미지 보여주기->
            context?.let {
                GlideApp.with(it).load(artPathFromAlarmValue).circleCrop() //
                    .error(R.drawable.errordisplay).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .placeholder(R.drawable.placeholder).listener(object :
                        RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?,model: Any?,target: Target<Drawable>?,isFirstResource: Boolean): Boolean {
                            Log.d(TAG, "onLoadFailed: Glide load failed!. Message: $e")
                            return false
                        }
                        override fun onResourceReady(resource: Drawable?,model: Any?,target: Target<Drawable>?,dataSource: DataSource?,isFirstResource: Boolean): Boolean {
                            //Log.d(TAG,"onResourceReady: Glide - 알람 ID[${alarm.id}]의 ROW Album Art 로딩 성공!") // debug 결과 절대 순.차.적으로 진행되지는 않음!
                            return false
                        }

                    }).into(rowHolder.albumArt)
            }

        // d) 현재 설정되어있는 알람의 '재생'이 문제 있는지 확인 (mainly .rta 나 .art 파일이 폰에 없을때) - art 는 없으면 rta 에서 자동으로 추출되어서 art.isNull.. 은 일어날 확률이 없겠찌?
            // 정상적인 Install 후 SQL 이 잘 진행되었다면 여기를 거쳐가서는 안된다!**
            // Update: '21.12.14=> 앱 Install 후 생성되는 두개의 알람은 모두 SQL 에서 자동으로 각각 defrt1-2 rta/art 경로를 저장한다! (label: InstallAlarm)
            val isRtUnplayable = checkIfRtIsUnplayable(requireActivity(),alarmtoneList)
            if(isRtUnplayable) {
                artPathFromAlarmValue = null // 위에서 지정한 .art 경로를 null 로 없애줌. 왜냐면 RTA 파일이 없는데  커버사진(ART) 보여준들 무슨 의미 있나. 헷가리기만할뿐. [!] 표시됨! (Error Image)
            }
            //Log.d(TAG, "onBindViewHolder: alarm= $alarm, position=$position,  isRtUnplayable=$isRtUnplayable") // <-- 여기 logd 중요 정보 많음.

        // e-1) Delete add, skip animation
            if (rowHolder.idHasChanged) {rowHolder.onOff.jumpDrawablesToCurrentState()}
        //e-2) onOff
            rowHolder.container.setOnClickListener {
                    val enable = !alarm.isEnabled
                    logger.debug { "onClick: ${if (enable) "enable" else "disable"}" }
                    alarms.enable(alarm, enable)
                }

            //Log.d(TAG, "onBindViewHolder: (2-정상) alarm.id=${alarm.id},  \nartPathFromAlarmValue= $artPathFromAlarmValue, \nalarm.alarmtone= ${alarm.alarmtone}, ")
        // f)  시간 누르거나 or AlbumArt 눌렀을 떄 ->
            // Option A-1) 만약 ListFrag 에서 시간 눌렀을 때 => 바로 Details Frag 로 가고 싶다면 아래를 넣으면 된다!
            rowHolder.digitalClockContainer.setOnClickListener {
                val id = mAdapter.getItem(position)?.id
                //Log.d(TAG, "getView: clicked ++CLOCK CONTAINER++. ID=$id, alarmId= ${alarm.id}, view.tag= ${it.tag}") // 여기서 tag 설정은 RowHolder - init 에서 해줌!!!!
                uiStore.edit(alarm.id, it.tag as RowHolder)
            }

            // Option A-2) AlbumArt 쪽 클릭햇을 때 위와 동일!!하게 DetailsFrag 로 감!
            rowHolder.albumArtContainer.setOnClickListener {
                val id = mAdapter.getItem(position)?.id
                //Log.d(TAG, "getView: clicked **ALBUM ART CONTAINER**. ID=$id, alarmId= ${alarm.id}, view.tag= ${it.tag}") // 여기서 tag 설정은 RowHolder - init 에서 해줌!!!!
                uiStore.edit(alarm.id, it.tag as RowHolder)
            }
        // g) Swipe 했을 때 imgBtn 과 DELETE (textView) 담고 있는 LinearLayout
            rowHolder.swipeDeleteContainer.setOnClickListener {
                //val currentAlarm = mAdapter.getItem(position) // 이전에는 getItem(position) 였는데 -> 삭제시 기존 Position 을 승계해서 밑에줄 아이템이 지워지는 문제
                val currentAlarm = mAdapter.getItem(rowHolder.adapterPosition) //  => 삭제 순간의 Pos 을 반영한 getAdapterPosition() 으로 변경!
                alarms.delete(currentAlarm) // 문제는 이게 되기전에 아래 sub 에서 한번 refresh 하고. 삭제되면 또 refresh 한다. 두번..
                Log.d(TAG, "onBindViewHolder: [DELETING ALARM] currentAlarm=$currentAlarm, position=$position")
            }
        // h) 흐음. 이건 쓸모 없는듯 (우리는 Layout 선택 못하게 할것이니..)
            //val removeEmptyView: Boolean = listRowLayout == Layout.CLASSIC || listRowLayout == Layout.COMPACT

        // i)  내가 추가:: 요일 표시--> (단순화 버전)
            //todo: Dark Mode 관련..
            val daysOfWeekStr = alarm.daysOfWeek.toString() // 설정된 요일 ex) _tw___s (화/목/일 알람 repeat 설정된 상태)
            for(i in daysOfWeekStr.indices) {
                if(daysOfWeekStr[i] == '_') { // 알파벳 없는 빈칸이면
                    // do nothing
                }  else { // 알파벳이 있으면
                    when(i) {

                        0 ->{rowHolder.tvMon.text=""
                            rowHolder.tvMon.background=yesAlarmMon}
                        1 ->{rowHolder.tvTue.text=""
                            rowHolder.tvTue.background=yesAlarmTue}
                        2 ->{rowHolder.tvWed.text=""
                            rowHolder.tvWed.background=yesAlarmWed}
                        3 ->{rowHolder.tvThu.text=""
                            rowHolder.tvThu.background=yesAlarmThu}
                        4 ->{rowHolder.tvFri.text=""
                            rowHolder.tvFri.background=yesAlarmFri}
                        5 ->{rowHolder.tvSat.text=""
                            rowHolder.tvSat.background=yesAlarmSat}
                        6-> {rowHolder.tvSun.text=""
                            rowHolder.tvSun.background=yesAlarmSun}
                    }
                }
            } // 요일추가 for loop 여기까지

        }
        override fun getItemCount(): Int {
            return alarmValuesList.size
        }
        
        fun refreshAlarmList(newAlarmList: List<AlarmValue>) {

            val oldAlarmList = alarmValuesList
            Log.d(TAG, "refreshAlarmList: oldAlarmList=$oldAlarmList,\nnewAlarmList = $newAlarmList")

            val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(AlarmDiffUtilCallback(oldAlarmList,newAlarmList))
            alarmValuesList = newAlarmList
            Log.d(TAG, "refreshAlarmList: @@@@@@@@ AlarmList.size: (OLD)=${oldAlarmList.size}, (NEW): ${newAlarmList.size}")
            diffResult.dispatchUpdatesTo(this)
        }

        fun getItem(pos: Int): AlarmValue {
            return if(pos >= alarmValuesList.size) { //todo: 이거 절대 일어나서는 안되는 에러. 벌써 두번이나 알람 지우다 발생한적이 있음. 유심히 관찰.. + User 에게 toast 안 뜨게 그냥 지울 것.
                Log.d(TAG, "getItem: [**DEADLY ERROR**] Unable to fetch Alarm(getItem(). Returning list[0].\n Pos=$pos, AlarmValueList.size=${alarmValuesList.size}")
                toastMessenger.showMyToast("[**DEADLY ERROR**] Unable to fetch Alarm(getItem(). Returning list[0].\n Pos=$pos, AlarmValueList.size=${alarmValuesList.size}",isShort = false)
                alarmValuesList[0]
            } else {
                Log.d(TAG, "getItem: returning alarm at pos[$pos], alarm=${alarmValuesList[pos]}")
                alarmValuesList[pos]
            }

        }
    }
    class AlarmDiffUtilCallback(var oldAlarmList: List<AlarmValue>, var newAlarmList: List<AlarmValue>) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldAlarmList.size
        override fun getNewListSize(): Int = newAlarmList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return (oldAlarmList[oldItemPosition].id == newAlarmList[newItemPosition].id) // id 의존 왜냐면 id is unique and unchangeable.
        }
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return (oldAlarmList[oldItemPosition] == newAlarmList[newItemPosition])
        }
    }
//<1> <-------------(내가 작성) RcV Adapter& Classes--

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    //추가1) ->
        Log.d(TAG, "(Line330)onCreateView: jj-created")

    //<-추가1)

        logger.debug { "onCreateView $this" }

        val view = inflater.inflate(R.layout.list_fragment, container, false)
        val recyclerV = view.findViewById(R.id.list_fragment_list) as RecyclerView // listView -> recyclerV 로 변경함.
        val layoutManager: LinearLayoutManager = LinearLayoutManager(context)


    //추가2) <-- DiskSearcher

    // ListView -->

        recyclerV.adapter = mAdapter
        recyclerV.setHasFixedSize(true)
        recyclerV.layoutManager = layoutManager
        recyclerV.isVerticalScrollBarEnabled = false
        recyclerV.setOnCreateContextMenuListener(this)
        //listView.choiceMode = AbsListView.CHOICE_MODE_SINGLE

    // ListView <--
        registerForContextMenu(recyclerV)
        setHasOptionsMenu(true)
    // 기존 Fab 코드 (Fragment 에 있을 때)
        /*val fab: View = view.findViewById(R.id.fab)
        fab.setOnClickListener { uiStore.createNewAlarm() } // 22_02_19: 여기와 list_fragment.xml 에 있는 R.id.fab 버튼 없애고 AlarmsListActivity.XML 에 FAB 심음.*/


       /* lollipop {
            (fab as FloatingActionButton).attachToListView(listView)
        }*/


        alarmsSub = prefs.listRowLayout.observe().switchMap { uiStore.transitioningToNewAlarmDetails() }
            .switchMap { transitioning -> if (transitioning) Observable.never() else store.alarms() }
            .subscribe { alarmsList ->
                            Log.d(TAG, "(Line 363) onCreateView: alarmsSub~!! alarmsList=$alarmsList")
                            val sorted = alarmsList // 참고: sorted = List<AlarmValue>
                                    .sortedWith(Comparators.MinuteComparator())
                                    .sortedWith(Comparators.HourComparator())
//[1] UnSavedAlarm 찾아서 List 에 담아주기 [FAB 버튼으로 신규 생성 중 APP 강제 종료시 .isSaved 값은 false 임-- 오직 OK 눌러서 저장됐을때만 .isSaved=true]
                            unSavedAlarmsList.clear()
                            unSavedAlarmsList = sorted.filter { alarmValue -> !alarmValue.isSaved }.toMutableList()

//[2] 위 [1] 과 무관하게 UnSavedAlarm "제외한 리스트"로 일단 rcV 업데이트 -- 아래 onResume() 에서 이제 UnSavedAlarmsList 에 있던 놈들 삭제 처리!
                            val alarmListWithoutUnSaved = sorted.filter { alarmValue -> alarmValue.isSaved }
                            mAdapter.refreshAlarmList(alarmListWithoutUnSaved)
                            //mAdapter.notifyDataSetChanged() // 이거 넣으면 훨씬 빠르지만 일단은 안 쓰는것으로..
            }
        return view
    }

    override fun onResume() {
        Log.d(TAG, "onResume: jj-OnResume() TOP line")
        super.onResume()
//[3] 위의 .subscribe 에서 찾아줬던 unSaved Alarm 들 지워주기
        if(unSavedAlarmsList.isNotEmpty()) {
            for(i in unSavedAlarmsList.indices) {
                Log.d(TAG, "alarmsSub(Line371): [Deleted Alarm: ${unSavedAlarmsList[i]}]")
                alarms.delete(unSavedAlarmsList[i])    // 알람 자체를 삭제.
            }
        }

        backSub = uiStore.onBackPressed().subscribe { // .subscribe = livedata 의 observe 와 같음.  onBackPressed  var 를 return 하는 onBackPressed() 를 Subscribe.
            // 여기 onBackPressed -> ListActivity onBackPressed 가 실행됨.
            Log.d(TAG, "(Line267) onResume: jj-backsub=uiStore.xxx.. requireActivity()")
            requireActivity().finish()
        }

        listRowLayout = prefs.layout()
        listRowLayoutId = R.layout.list_row_classic
        // [JJLAY] 기존 코드
        /*listRowLayoutId = when (listRowLayout) {
            Layout.COMPACT -> R.layout.list_row_compact // classic 아니면 계속 RowHolder 에서 findView 에서 null 에러..
            Layout.CLASSIC -> R.layout.list_row_classic
            else -> R.layout.list_row_bold
        }*/
        //Log.d(TAG, "onResume: listRowLayoutId= $listRowLayoutId, listRowLayout=$listRowLayout") // Layout.COMPACT 였음.

        // ListActivity 로 Fab 버튼을 옮긴 후 코드 (Fragment View 생성과 동시에 ListActivity 에 있는 Fab 을 찾아서 보여줌)

    }

    override fun onPause() {
        Log.d(TAG, "(Line278)onPause: ..")
        super.onPause()
        backSub.dispose()
        //dismiss the time picker if it was showing. Otherwise we will have to uiStore the state and it is not nice for the user
        timePickerDialogDisposable.dispose()
    }

    override fun onDestroy() {
        Log.d(TAG, "(Line286)onDestroy: ,,,")
        super.onDestroy()
    //ListFrag 가 Destroy 될 때가 비로소 SecondFrag , DetailsFrag 준비가 완료되는 시점과 비슷함. 따라서 Fab 버튼을 먼저 없애주지 않고 해당 시점에 없애주기 위해 넣을려했으나
    // 되려 Listfrag 에서 SecondFrag 버튼 클릭과 동시에 FAB 는 없어지는게 자연스러운것 같아서 일단 Comment Out 시킴.
//        val fabInListActivity = requireActivity().findViewById<FloatingActionButton>(R.id.fab_listActivity)
//        if(fabInListActivity!=null && fabInListActivity.visibility == View.VISIBLE) {
//            fabInListActivity.visibility = View.GONE
//        }

        alarmsSub.dispose()
    }

    /*override fun onDetach() {
        Log.d(TAG, "onDetach: called.")
        super.onDetach()
    }*/

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenuInfo?) {
        // Inflate the menu from xml.
        requireActivity().menuInflater.inflate(R.menu.list_context_menu, menu)

        // Use the current item to create a custom view for the header.
        val info = menuInfo as AdapterContextMenuInfo
        val alarm = mAdapter.getItem(info.position)

        // Construct the Calendar to compute the time.
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, alarm!!.hour)
        cal.set(Calendar.MINUTE, alarm.minutes)

        val visible = when {
            alarm.isEnabled -> when {
                alarm.skipping -> listOf(R.id.list_context_enable)
                alarm.daysOfWeek.isRepeatSet -> listOf(R.id.skip_alarm)
                else -> listOf(R.id.list_context_menu_disable)
            }
            // disabled
            else -> listOf(R.id.list_context_enable)
        }

        listOf(R.id.list_context_enable, R.id.list_context_menu_disable, R.id.skip_alarm)
                .minus(visible)
                .forEach { menu.removeItem(it) }
    }


// <--- override Functions

// **** 내가 추가한 Utility Methods **
// 추가 3) Lottie 관련-->
    private fun showLottieDialogFrag() {
        Log.d(TAG, "showLottieDialogFrag: isLottiePlayedOnce=$isLottiePlayedOnce")
        if(!isLottiePlayedOnce) {
            lottieDialogFrag.show(requireActivity().supportFragmentManager, lottieDialogFrag.tag) // 이 ListFrag 시작 후 재생된적이 없으면..
            isLottiePlayedOnce = true // 어차피 다른 Frag 로 가는순간 이 Frag 는 Destroy 된다. (배경화면 나가는건 그냥 pause)./
        }

    }
    private fun hideLottieAndShowSnackBar() {
        if(lottieDialogFrag.isAdded) {
            lottieDialogFrag.dismissAllowingStateLoss()
            if(activity != null) {
                snackBarDeliverer(requireActivity().findViewById(android.R.id.content),"REBUILDING DATABASE COMPLETED",isShort = false )
            }

        }
    }

// <--추가 3) Lottie 관련 <---

// 추가 4) daysOfWeekStringWithSkip() 에서 받은 String 에서 알람 설정된 날들을 찾기. (Ex. M,T
    /*private fun getEnabledDaysList(daysInString: String): List<String> {
        // Ex) "Mon, Tue," 이렇게 생긴 String 을 받아서 ',' 을 기준으로 split
        val enabledDaysList: List<String> = daysInString.split(",").map {dayStr -> dayStr.trim()}
        Log.d(TAG, "getEnabledDaysList: enabledDaysList=$enabledDaysList")
        return enabledDaysList

    }*/

    // ImageView 에 주입할 Circle (text) Drawable Builder: https://github.com/amulyakhare/TextDrawable
    // 기본 1주일 전체 알람 없는 날 표시용 drawable [회색 배경, 흰 글씨]
    private fun getYesAlarmDayDrawable(day: String): TextDrawable {
    //amulyakhare 라이브러리에서 color builder 는 유저에 따라 key 값을 다른 색 부여하는것일 뿐. 내가 쓸만한 것은 아님: https://github.com/amulyakhare/TextDrawable
    // 추후에 Color.xx 에 다양한 색 넣는것으로 해보자. Color Class: https://developer.android.com/reference/android/graphics/Color

    return when (day) {
        "Sun" -> {
            TextDrawable.builder().beginConfig().textColor(Color.RED).useFont(Typeface.SANS_SERIF)
                .fontSize(31).endConfig()
                .buildRoundRect("S",Color.LTGRAY,10)
                //.buildRound("S", Color.LTGRAY)
        }
        "Sat" -> {
            TextDrawable.builder().beginConfig().textColor(Color.BLUE).useFont(Typeface.SANS_SERIF)
                .fontSize(31).endConfig()
                .buildRoundRect("S",Color.LTGRAY,10)
                //.buildRound("S", Color.LTGRAY)
        }
        else -> {
            TextDrawable.builder().beginConfig().textColor(Color.WHITE).useFont(Typeface.SANS_SERIF)
                .fontSize(31).endConfig()
                .buildRoundRect(day,Color.LTGRAY,10)
                //.buildRound(day, Color.LTGRAY)
            }
        }
    }
// 추가 5) SnackBar 대신 전달 (view & fragment 문제없는지도 확인)
    private fun snackBarDeliverer(view: View, msg: String, isShort: Boolean) {
        if(activity!=null && isAdded) { // activity 가 존재하며, 현재 Fragment 가 attached 되있으면 Snackbar 를 표시.
            Log.d(TAG, "snackBarMessenger: Show Snackbar. Fragment isAdded=$isAdded, Activity=$activity")
            if(isShort) {
                Snackbar.make(view, msg, Snackbar.LENGTH_SHORT).show()
            }else {
                Snackbar.make(view, msg, Snackbar.LENGTH_LONG).show()
            }
        } else {
            Log.d(TAG, "snackBarDeliverer: Unable to Deliver Snackbar message!!")
        }
    }


}