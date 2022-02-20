package com.theglendales.alarm.presenter

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.core.view.MenuItemCompat
import com.theglendales.alarm.BuildConfig
import com.theglendales.alarm.R
import com.theglendales.alarm.bugreports.BugReporter
import com.theglendales.alarm.interfaces.IAlarmsManager
import com.theglendales.alarm.lollipop
import io.reactivex.disposables.Disposables


/**
 * This class handles options menu and action bar: 내가 ActionBar-> ToolBar 로 변경했음.
 * AlarmsListActivity.kt 에서 ActionBarHandler() 만들어줌.
 * @author Kate
 */
private const val TAG="ActionBarHandler"


class ActionBarHandler(private val mContext: Activity,private val store: UiStore,private val alarms: IAlarmsManager,private val reporter: BugReporter) {
    private var sub = Disposables.disposed()

    /**
     * Delegate [Activity.onCreateOptionsMenu]
     *
     * @param menu
     * @param inflater
     * @param toolBarAsActionBar
     * @return
     */

    // 액티비티가 시작할 때 한번만 호출되는 함수로 Menu 와 같은 초기 설정 작업이 이뤄지는 함수.
    fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater, toolBarAsActionBar: ActionBar): Boolean {
        Log.d(TAG, "onCreateOptionsMenu: jj-called")
        inflater.inflate(R.menu.menu_action_bar, menu)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"

            addFlags(when {
                lollipop() -> Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                else -> @Suppress("DEPRECATION") Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
            })

            // Add data to the intent, the receiving app will decide what to do with
            // it.
            putExtra(Intent.EXTRA_SUBJECT, "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)
            putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)
        }

        val menuItem = menu.findItem(R.id.menu_share)
        val settingsIcon = menu.findItem(R.id.menu_item_settings)// 톱니바퀴(설정) 아이콘은 여기서 안보이게. 내가 추가
        val trashIcon = menu.findItem(R.id.set_alarm_menu_delete_alarm) // 내가 추가. 아래에서 trashIcon 대신 menu.findItem(R.id.... _delete_alarm) 넣어놨더니 계속 뻑나서..
        // todo: 뒷맛이 좋지는 않음. 일단 이걸로 모든 요일이 선택되어있는 DetailsFrag 열릴 때 뻑나는건 멈추기는 했음.

        val sp = MenuItemCompat.getActionProvider(menuItem) as androidx.appcompat.widget.ShareActionProvider
        sp.setShareIntent(intent)

        //(상황에 따른) ActionBar ICON 지정! --  요일을 Chip 으로 변경할떄마다 여기로 들어오네..
        sub = store.editing().subscribe { edited ->
            Log.d(TAG, "onCreateOptionsMenu: jj-inside sub=store.editing().subscribe{}. 'edited'=${edited.toString()}")
            //val showDeleteIcon = edited.isEdited && !edited.isNew //Boolean

            when(edited.isEdited && !edited.isNew) {
                true -> { //Details Frag 일 때
                    //a) 휴지통 ICON 보여주기 (O)
                    if(trashIcon!=null) { // nullCheck 내가 넣었음.
                        trashIcon.isVisible = true // 알람 요일설정땜에 여기서 자꾸 뻑남. 근데 이유는 findItem 이 null 값여서.. // 원래코드 = menu.findItem(R.id.... _delete_alarm).isVisible = showDelete
                    }
                    //b) 설정(톱니바퀴) ICON 가려주기. (X)
                    if(settingsIcon!=null) {
                        settingsIcon.isVisible = false
                    }
                }
                false -> { // 그 외 AlarmsListActivity 의 다른 Frag 들 일 때 (AlarmsListFrag, SecondFrag)
                    //a) 휴지통 ICON 없애주기 (X)
                    if(trashIcon!=null) {
                        trashIcon.isVisible = false
                    }
                    //b) 설정(톱니바퀴) ICON 보여주기 (O)
                    if(settingsIcon!=null) {
                        settingsIcon.isVisible = true
                    }

                }
            }
            toolBarAsActionBar.setDisplayHomeAsUpEnabled(edited.isEdited) // <- FAB 클릭 아니고 기존 알람 수정일때.. back button 보여주기 -- toolBar 로 바꾸고 나서는 여기서 .setHomexxx.. 실행 안되네.
        }
        return true
    }


    fun onDestroy() {
        sub.dispose()
    }

    /**
     * Delegate [Activity.onOptionsItemSelected]
     *
     * @param item
     * @return
     */
    fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_settings -> mContext.startActivity(Intent(mContext, SettingsActivity::class.java))
            R.id.menu_review -> showSayThanks() // store.createNewAlarm() <- 이걸로 해도 알람 잘 생성되는듯.
            R.id.menu_bugreport -> showBugreport()
            R.id.set_alarm_menu_delete_alarm -> deleteAlarm()
            R.id.menu_about -> showAbout()
            android.R.id.home -> store.onBackPressed().onNext("ActionBar")
        }
        return true
    }

    private fun showAbout() {
        AlertDialog.Builder(mContext).apply {
            setTitle(mContext.getString(R.string.menu_about_title))
            setView(View.inflate(mContext, R.layout.dialog_about, null).apply {
                findViewById<TextView>(R.id.dialog_about_text).run {
                    setText(R.string.dialog_about_content)
                    movementMethod = LinkMovementMethod.getInstance()
                }
            })
            setPositiveButton(android.R.string.ok) { _, _ ->
            }
                    .create()
                    .show()
        }
    }

    private fun deleteAlarm() {
        AlertDialog.Builder(mContext).apply {
            setTitle(mContext.getString(R.string.delete_alarm))
            setMessage(mContext.getString(R.string.delete_alarm_confirm))
            setPositiveButton(android.R.string.ok) { _, _ ->
                alarms.getAlarm(store.editing().blockingFirst().id())?.delete()
                store.hideDetails()
            }
            setNegativeButton(android.R.string.cancel, null)
        }.show()
    }

    private fun showSayThanks() {
        val inflator = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val dialogView = inflator.inflate(R.layout.dialog_say_thanks, null).apply {
            findViewById<Button>(R.id.dialog_say_thanks_button_review).setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID))
                mContext.startActivity(intent)
            }

            findViewById<TextView>(R.id.dialog_say_thanks_text_as_button_donate_premium).movementMethod = LinkMovementMethod.getInstance()
        }

        AlertDialog.Builder(mContext).apply {
            setPositiveButton(android.R.string.ok) { _, _ -> }
            setTitle(R.string.dialog_say_thanks_title)
            setView(dialogView)
            setCancelable(true)
        }
                .create()
                .show()
    }

    private fun showBugreport() {
        val inflator = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val dialogView = inflator.inflate(R.layout.dialog_bugreport, null)

        dialogView.findViewById<TextView>(R.id.dialog_bugreport_textview).movementMethod = LinkMovementMethod.getInstance()

        AlertDialog.Builder(mContext).apply {
            setPositiveButton(android.R.string.ok) { _, _ ->
                reporter.sendUserReport()
            }
            setTitle(R.string.menu_bugreport)
            setCancelable(true)
            setNegativeButton(android.R.string.cancel, EmptyClickListener())
            setView(dialogView)
        }
                .create()
                .show()
    }

    private class EmptyClickListener : DialogInterface.OnClickListener {
        override fun onClick(dialog: DialogInterface, which: Int) {
            //this listener does not do much
        }
    }
}
