package com.theglendales.alarm.presenter

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.AdapterContextMenuInfo
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
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
import com.theglendales.alarm.logger.Logger
import com.theglendales.alarm.lollipop
import com.theglendales.alarm.model.AlarmValue
import com.melnykov.fab.FloatingActionButton
import com.theglendales.alarm.jjadapters.GlideApp
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjongadd.LottieDiskScanDialogFrag
import com.theglendales.alarm.jjongadd.TimePickerJjong
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

    private val mAdapter: AlarmListAdapter by lazy { AlarmListAdapter(R.layout.list_row_classic, R.string.alarm_list_title, ArrayList()) }
    private val inflater: LayoutInflater by lazy { requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater }

    private var alarmsSub: Disposable = Disposables.disposed()
    private var backSub: Disposable = Disposables.disposed()
    private var timePickerDialogDisposable = Disposables.disposed()

// 내가 추가-->
    lateinit var lottieAnimView: LottieAnimationView //Lottie Animation(Loading & Internet Error)
    lateinit var lottieDialogFrag: LottieDiskScanDialogFrag
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
//내가 추가<-

    /** changed by [Prefs.listRowLayout] in [onResume]*/
    private var listRowLayoutId = R.layout.list_row_classic

    /** changed by [Prefs.listRowLayout] in [onResume]*/
    private var listRowLayout = prefs.layout()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttach: called")

        lottieDialogFrag = LottieDiskScanDialogFrag.newInstanceDialogFrag()


        //추가2) DiskSearcher --> rta .art 파일 핸들링 작업 (앱 시작과 동시에)
        //1) DiskSearcher.downloadedRtSearcher() 를 실행할 필요가 있는경우(O) (최초 앱 설치 or 신규 다운로드 등.. )
        // [신규 다운로드 후 rta 파일만 추가되었거나, user 삭제, 오류 등.. rt (.rta) 중 art 값이 null 인 놈이 있거나 등]
        // todo: 여기서 추후 permission 관련된것도 해주기?

        if(myDiskSearcher.isDiskScanNeeded()) { // 만약 새로 스캔 후 리스트업 & Shared Pref 저장할 필요가 있다면
            Log.d(TAG, "onCreate: $$$ Alright let's scan the disk!")
            // ** diskScan 시작 시점-> ANIM(ON)!
            showLottieDialogFrag()
            // 2.5초후에 애니메이션 없애기->  loop=false, repeat 없이 보여주기.(lottie_rebuild_rt.xml)
            // 만약 여기서 handler 문제가 생겨서 안 없어지면 Lottie 다 재생하고( 약 5초) LottieDiskScanDiaFrag.kt > onAnimationEnd 로 DialFrag 자체를 없애줌!
            val handler: Handler = Handler(Looper.getMainLooper())
            handler.postDelayed({hideLottieAndShowSnackBar()}, 2500) // todo: handler 는 올바른 방법이 아니라고 보긴 봤음.. 일단은 잘되네.


            //CoroutineScope(Dispatchers.IO).launch { <== ** 일부러 코루틴에서 제외-> 그래야 여기서 update SharedPref 등이 끝나고나서 밑에 innerClass>getView 실행됨.
                //lottieAnimCtrl(SHOW_ANIM)
                //1-a) /.AlbumArt 폴더 검색 -> art 파일 list up -> 경로를 onDiskArtMap 에 저장
                myDiskSearcher.readAlbumArtOnDisk()
                //1-b-1) onDiskRtSearcher 를 시작-> search 끝나면 Default Rt(raw 폴더) 와 List Merge!
                val resultList = myDiskSearcher.onDiskRtSearcher() // rtArtPathList Rebuilding 프로세스. resultList 는 RtWAlbumArt object 리스트고 각 Obj 에는 .trkId, .artPath, .audioFileUri 등의 정보가 있음.
                //** 1-b-2) 1-b-1) 과정에서 rtOnDisk object 의 "artFilePathStr" 이 비어잇으면-> extractArtFromSingleRta() & save image(.rta) on Disk

                // 1-c) Merge 된 리스트(rtWithAlbumArt obj 로 구성)를 얼른 Shared Pref 에다 저장! (즉 SharedPref 에는 art, rta 의 경로가 적혀있음)
                mySharedPrefManager.saveRtaArtPathList(resultList)

                // 1-d) DiskSearcher.kt>finalRtArtPathList (Companion obj 메모리) 에 띄워놓음(갱신)
                myDiskSearcher.updateList(resultList)


                Log.d(TAG, "onCreate: --------------------------- DiskScan DONE..(Hopefully..)---------- \n\n resultList = $resultList!")


            //} // ** diskScan 종료 <--

        }

        //2) Scan 이 필요없음(X)!!! 여기서 SharedPref 에 있는 리스트를 받아서 -> DiskSearcher.kt>finalRtArtPathList (Companion obj 메모리) 에 띄워놓음(갱신)
        else if(!myDiskSearcher.isDiskScanNeeded()) {
            val resultList = mySharedPrefManager.getRtaArtPathList()

            Log.d(TAG, "onCreate: XXX no need to scan the disk. Instead let's check the list from Shared Pref => resultList= $resultList")
            myDiskSearcher.updateList(resultList)
        }
    }

// ## Inner Class ##
    inner class AlarmListAdapter(alarmTime: Int, label: Int, private val values: List<AlarmValue>) : ArrayAdapter<AlarmValue>(requireContext(), alarmTime, label, values)
    {

        private fun recycleView(convertView: View?, parent: ViewGroup, id: Int): RowHolder
        {
            val tag = convertView?.tag


            return when {
                tag is RowHolder && tag.layout == listRowLayout -> RowHolder(convertView, id, listRowLayout)
                else -> {
                    val rowView = inflater.inflate(listRowLayoutId, parent, false)
                    RowHolder(rowView, id, listRowLayout).apply {
                        digitalClock.setLive(false)
                    }
                }
            }
        }



        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            // get the alarm which we have to display

            val alarm = values[position]
            val rowHolder = recycleView(convertView, parent, alarm.id)

            rowHolder.onOff.isChecked = alarm.isEnabled

            lollipop {
                rowHolder.digitalClock.transitionName = "clock" + alarm.id
                rowHolder.container.transitionName = "onOff" + alarm.id
                rowHolder.detailsButton.transitionName = "detailsButton" + alarm.id
            }
        // 추가-> READ: Row 의 a) AlbumArt 에 쓰일 아트 Path 읽고 b)Glide 로 이미지 보여주기->
            var artPathFromAlarmValue = alarm.artFilePath // +++


        // ** Row 의 앨범아트 path가 비어있을 때. (정상적인 Install 후 SQL 이 잘 진행되었다면 여기를 거쳐가서는 안된다!)
            // Update: '21.12.14=> 앱 Install 후 생성되는 두개의 알람은 모두 SQL 에서 자동으로 각각 defrt1-2 rta/art 경로를 저장한다! (label: InstallAlarm)
            // DetailsFrag 를 들어가는 순간

            if(artPathFromAlarmValue.isNullOrEmpty()) {
                Log.d(TAG, "getView: (1-No Art File Found) artPathFromAlarmValue=$artPathFromAlarmValue, \nalarm.id=${alarm.id}, alarm.alarmtone.persistedString=${alarm.alarmtone.persistedString}")
                val currentRtaArtPathList = mySharedPrefManager.getRtaArtPathList()
                if(currentRtaArtPathList.size>0) { // 리스트에 rta 가 1개 이상 있으면
                        try{
                            val defRta1 = currentRtaArtPathList.filter { rtWithAlbumArt -> rtWithAlbumArt.fileName=="defrt01" }.single() // **todo: Safety Check 실제로 뻑남.
                            val artPath: String? = defRta1.artFilePathStr

                            //mySharedPrefManager.saveArtPathForAlarm(alarm.id, artPath) // 새로 지정된 artPath 주소를 SharedPref 에 저장 => 다시는 (!) 떠서는 안됨!!
                            artPathFromAlarmValue = artPath // 이것도 다시 지정 -> Glide 가 잘 로딩되야함!
                            //alarm.artFilePath = artPath <- 이것도 해줘야되지만. val 여서 안됨, 추후에 Alarmtone.kt 와 Permissions.kt 잘 조져야함..

                        }catch (e: Exception) {
                            Toast.makeText(requireContext(), "Unable to load default ringtones.",Toast.LENGTH_SHORT).show()
                            Log.d(TAG, "getView: Unable to load default ringtones. Error=$e")
                        }
                 }
            }

            Log.d(TAG, "getView: (2-정상) alarm.id=${alarm.id},  \nartPathFromAlarmValue= $artPathFromAlarmValue, \nalarm.alarmtone= ${alarm.alarmtone}, ")

            context?.let {
                GlideApp.with(it).load(artPathFromAlarmValue).circleCrop() //
                    .error(R.drawable.errordisplay).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .placeholder(R.drawable.placeholder).listener(object :
                        RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            Log.d(TAG, "onLoadFailed: Glide load failed!. Message: $e")
                            return false
                        }
                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                             Log.d(TAG,"onResourceReady: Glide - 알람 ID[${alarm.id}]의 ROW Album Art 로딩 성공!") // debug 결과 절대 순.차.적으로 진행되지는 않음!
                            return false
                        }
                    }).into(rowHolder.albumArt)
            }
        //<-추가

            //Delete add, skip animation
            if (rowHolder.idHasChanged) {
                rowHolder.onOff.jumpDrawablesToCurrentState()
            }

            rowHolder.container
                    //onOff
                    .setOnClickListener {
                        val enable = !alarm.isEnabled
                        logger.debug { "onClick: ${if (enable) "enable" else "disable"}" }
                        alarms.enable(alarm, enable)
                    }
//      // Option A-1) 만약 ListFrag 에서 시간 눌렀을 때 => 바로 Details Frag 로 가고 싶다면 아래를 넣으면 된다!
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
        // swipe 했을 때 imgBtn 과 DELETE (textView) 담고 있는 LinearLayout

            rowHolder.swipeDeleteContainer.setOnClickListener {
                val currentAlarm = mAdapter.getItem(position)
                if(currentAlarm!=null) {
                    alarms.delete(currentAlarm)
                    Log.d(TAG, "getView: [DELETING ALARM] currentAlarm=$currentAlarm, position=$position")

                } else {
                    Log.d(TAG, "getView: Failed to DELETE ALARM! currentAlarm=$currentAlarm, position=$position")
                }

            }
            // set the alarm text
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, alarm.hour)
            c.set(Calendar.MINUTE, alarm.minutes)
            rowHolder.digitalClock.updateTime(c)

            val removeEmptyView: Boolean = listRowLayout == Layout.CLASSIC || listRowLayout == Layout.COMPACT
            // Set the repeat text or leave it blank if it does not repeat.
        // 내가 추가:: 요일 표시--> (단순화 버전)
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
            }

            return rowHolder.rowView
        }
    }
// InnerClass <--

    //LongClick related.. i think..
    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as AdapterContextMenuInfo
        val alarm = mAdapter.getItem(info.position) ?: return false
        when (item.itemId) {
            R.id.delete_alarm -> {
                // Confirm that the alarm will be deleted.
                AlertDialog.Builder(activity).setTitle(getString(R.string.delete_alarm))
                        .setMessage(getString(R.string.delete_alarm_confirm))
                        .setPositiveButton(android.R.string.ok) { _, _ -> alarms.delete(alarm) }.setNegativeButton(android.R.string.cancel, null).show()
            }
            R.id.list_context_enable -> {
                alarms.getAlarm(alarmId = alarm.id)?.run {
                    edit {
                        copy(isEnabled = true)
                    }
                }
            }
            R.id.list_context_menu_disable -> {
                alarms.getAlarm(alarmId = alarm.id)?.run {
                    edit {
                        copy(isEnabled = false)
                    }
                }
            }
            R.id.skip_alarm -> {
                alarms.getAlarm(alarmId = alarm.id)?.run {
                    if (isSkipping()) {
                        // removes the skip
                        edit { this }
                    } else {
                        requestSkip()
                    }
                }
            }
        }
        return true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    //추가1) ->
        Log.d(TAG, "(Line213)onCreateView: jj-created")
    //<-추가1)

        logger.debug { "onCreateView $this" }

        val view = inflater.inflate(R.layout.list_fragment, container, false)
        val listView = view.findViewById(R.id.list_fragment_list) as ListView


    //추가2) <-- DiskSearcher

    // ListView -->
        listView.adapter = mAdapter

        listView.isVerticalScrollBarEnabled = false
        listView.setOnCreateContextMenuListener(this)
        listView.choiceMode = AbsListView.CHOICE_MODE_SINGLE
      /*  listView.setOnItemClickListener(object: AdapterView.OnItemClickListener{
            override fun onItemClick(parent: AdapterView<*>?,view: View?,position: Int,id: Long) {}})*/

    // listView.setOnItemClickListener(object: Adapt.....ClickListener{ override...xx} 이것과 같음.
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, view, position, _ ->
            mAdapter.getItem(position)?.id?.let {
                Log.d(TAG, "onCreateView: Detail 들어가는 IV click listener: alarmId=$it, view.tag= ${view.tag}")
                uiStore.edit(it, view.tag as RowHolder) // it = AlarmId 임!
            }
        }

    // ListView <--
        registerForContextMenu(listView)

        setHasOptionsMenu(true)

        val fab: View = view.findViewById(R.id.fab)
        fab.setOnClickListener { uiStore.createNewAlarm() }

        lollipop {
            (fab as FloatingActionButton).attachToListView(listView)
        }

        alarmsSub =
                prefs.listRowLayout
                        .observe()
                        .switchMap { uiStore.transitioningToNewAlarmDetails() }
                        .switchMap { transitioning -> if (transitioning) Observable.never() else store.alarms() }
                        .subscribe { alarms ->
                            Log.d(TAG, "(Line 251) onCreateView: alarmsSub~!! alarms=$alarms")
                            val sorted = alarms
                                    .sortedWith(Comparators.MinuteComparator())
                                    .sortedWith(Comparators.HourComparator())
                                    //.sortedWith(Comparators.RepeatComparator())
                            mAdapter.clear()
                            mAdapter.addAll(sorted)
                        }

        return view
    }



    override fun onResume() {
        Log.d(TAG, "onResume: jj-OnResume() TOP line")
        super.onResume()
        backSub = uiStore.onBackPressed().subscribe {
            Log.d(TAG, "(Line267) onResume: jj-backsub=uiStore.xxx.. requireActivity()")
            requireActivity().finish() }
        listRowLayout = prefs.layout()
        listRowLayoutId = when (listRowLayout) {
            Layout.COMPACT -> R.layout.list_row_compact
            Layout.CLASSIC -> R.layout.list_row_classic
            else -> R.layout.list_row_bold
        }
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
        alarmsSub.dispose()
    }

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

// **** 내가 추가한 Utility Methods **
// 추가 3) Lottie 관련-->
    private fun showLottieDialogFrag() {

        lottieDialogFrag.show(requireActivity().supportFragmentManager, lottieDialogFrag.tag)
    }
    private fun hideLottieAndShowSnackBar() {
        if(lottieDialogFrag.isAdded) {
            lottieDialogFrag.dismissAllowingStateLoss()
            Snackbar.make(requireActivity().findViewById(android.R.id.content), "REBUILDING DATABASE COMPLETED", Snackbar.LENGTH_LONG).show()
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
// 추가 5)


}