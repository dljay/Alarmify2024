package com.theglendales.alarm.presenter

import android.app.AlertDialog
import android.content.Context
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
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
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
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjongadd.LottieDiskScanDialogFrag
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    //lateinit var lottieAnimView: LottieAnimationView //Lottie Animation(Loading & Internet Error)
    lateinit var lottieDialogFrag: LottieDiskScanDialogFrag
    private val mySharedPrefManager: MySharedPrefManager by globalInject()
    private val myDiskSearcher: DiskSearcher by globalInject()
    //lateinit var listView: ListView // 기존에는 onCreateView 에서 그냥 val listView 해줬었음.
    //내가 추가<-


    /** changed by [Prefs.listRowLayout] in [onResume]*/
    private var listRowLayoutId = R.layout.list_row_classic

    /** changed by [Prefs.listRowLayout] in [onResume]*/
    private var listRowLayout = prefs.layout()

    inner class AlarmListAdapter(alarmTime: Int, label: Int, private val values: List<AlarmValue>) : ArrayAdapter<AlarmValue>(requireContext(), alarmTime, label, values) {

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
        //추가->
            Log.d(TAG, "getView: jj-called")
        //<-추가
            val alarm = values[position]

            val row = recycleView(convertView, parent, alarm.id)

            row.onOff.isChecked = alarm.isEnabled

            lollipop {
                row.digitalClock.transitionName = "clock" + alarm.id
                row.container.transitionName = "onOff" + alarm.id
                row.detailsButton.transitionName = "detailsButton" + alarm.id
            }

            //Delete add, skip animation
            if (row.idHasChanged) {
                row.onOff.jumpDrawablesToCurrentState()
            }

            row.container
                    //onOff
                    .setOnClickListener {
                        val enable = !alarm.isEnabled
                        logger.debug { "onClick: ${if (enable) "enable" else "disable"}" }
                        alarms.enable(alarm, enable)
                    }
        // 시간 적혀있는 부분 눌렀을 때 -> TimePicker 보여주기
            row.digitalClockContainer.setOnClickListener {
                timePickerDialogDisposable =
                    TimePickerDialogFragment.showTimePicker(parentFragmentManager)
                        .subscribe { picked ->
                            if (picked.isPresent()) {
                                alarms.getAlarm(alarm.id)?.also { alarm ->
                                    alarm.edit {
                                        copy(
                                            isEnabled = true,
                                            hour = picked.get().hour,
                                            minutes = picked.get().minute
                                        )
                                    }
                                }
                            }
                        }
            }

            row.digitalClockContainer.setOnLongClickListener {
                false
            }

            // set the alarm text
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, alarm.hour)
            c.set(Calendar.MINUTE, alarm.minutes)
            row.digitalClock.updateTime(c)

            val removeEmptyView = listRowLayout == Layout.CLASSIC || listRowLayout == Layout.COMPACT
            // Set the repeat text or leave it blank if it does not repeat.

            row.daysOfWeek.run {
                text = daysOfWeekStringWithSkip(alarm)
                visibility = when {
                    text.isNotEmpty() -> View.VISIBLE
                    removeEmptyView -> View.GONE
                    else -> View.INVISIBLE
                }
            }

            // Set the repeat text or leave it blank if it does not repeat.
            row.label.text = alarm.label

            row.label.visibility = when {
                alarm.label.isNotBlank() -> View.VISIBLE
                removeEmptyView -> View.GONE
                else -> View.INVISIBLE
            }

            // row.labelsContainer.visibility = when {
            //     row.label().visibility == View.GONE && row.daysOfWeek().visibility == View.GONE -> GONE
            //     else -> View.VISIBLE
            // }

            return row.rowView
        }

        private fun daysOfWeekStringWithSkip(alarm: AlarmValue): String {
            val daysOfWeekStr = alarm.daysOfWeek.toString(context, false)
            return if (alarm.skipping) "$daysOfWeekStr (skipping)" else daysOfWeekStr
        }
    }

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

        lottieDialogFrag = LottieDiskScanDialogFrag.newInstanceDialogFrag()





    //추가2) DiskSearcher --> rta .art 파일 핸들링 작업 (앱 시작과 동시에)

        //1) DiskSearcher.downloadedRtSearcher() 를 실행할 필요가 있는경우(O) (우선적으로 rta 파일 갯수와 art 파일 갯수를 비교.)
             // [신규 다운로드 후 rta 파일만 추가되었거나, user 삭제, 오류 등.. rt (.rta) 중 art 값이 null 인 놈이 있거나 등]



        if(myDiskSearcher.isDiskScanNeeded()) { // 만약 새로 스캔 후 리스트업 & Shared Pref 저장할 필요가 있다면
            Log.d(TAG, "onCreate: $$$ Alright let's scan the disk!")
            // ** diskScan 시작 시점-> ANIM(ON)!
            showLottieDialogFrag()
            val handler: Handler = Handler(Looper.getMainLooper())
            handler.postDelayed({hideLottieAndShowSnackBar()}, 2000) // 2초후에 애니메이션 없애기-> 보통 0.1초 사이에 실 작업은 다 끝나기는 함..


            CoroutineScope(Dispatchers.IO).launch {
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

                Log.d(TAG, "onCreate: DiskScan DONE..(Hopefully..), resultList = $resultList!")


            } // ** diskScan 종료 <--

        }

        //2) Scan 이 필요없음(X)!!! 여기서 SharedPref 에 있는 리스트를 받아서 -> DiskSearcher.kt>finalRtArtPathList (Companion obj 메모리) 에 띄워놓음(갱신)
        else if(!myDiskSearcher.isDiskScanNeeded()) {
            val resultList = mySharedPrefManager.getRtaArtPathList()
            Log.d(TAG, "onCreate: XXX no need to scan the disk. Instead let's check the list from Shared Pref => resultList= $resultList")
            myDiskSearcher.updateList(resultList)
        }


    //추가2) <-- DiskSearcher

        listView.adapter = mAdapter

        listView.isVerticalScrollBarEnabled = false
        listView.setOnCreateContextMenuListener(this)
        listView.choiceMode = AbsListView.CHOICE_MODE_SINGLE

        listView.onItemClickListener = AdapterView.OnItemClickListener { _, listRow, position, _ ->
            mAdapter.getItem(position)?.id?.let {
                uiStore.edit(it, listRow.tag as RowHolder)
            }
        }

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
                            Log.d(TAG, "(Line 251) onCreateView: alarmsSub~!!")
                            val sorted = alarms
                                    .sortedWith(Comparators.MinuteComparator())
                                    .sortedWith(Comparators.HourComparator())
                                    .sortedWith(Comparators.RepeatComparator())
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

}