package com.theglendales.alarm.jjmvvm.permissionAndDownload

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.theglendales.alarm.R

private const val TAG="BtmSht_Perm"

object BtmSheetPermission : BottomSheetDialogFragment() {
    private var myContext = FragmentActivity()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {


        Log.d(TAG, "onCreateView: BottomSheet_PERMISSION ")
        val v: View = inflater.inflate(R.layout.bottom_sheet_permission, container, false) // 우리가 만든 Bottom Sheet xml 파일.
        val tvCancel: TextView = v.findViewById(R.id.tv_Cancel)
        val tvOpenSettings: TextView = v.findViewById(R.id.tv_OpenSettings)

        tvCancel.setOnClickListener {
            Log.d(TAG, "onCreate: Clicked Cancel. Puchase failed. Dismiss Bottom sheet & Resume RcView!")
            removePermBtmSheetAndResume()
        }
        tvOpenSettings.setOnClickListener {
            Log.d(TAG, "onCreate: Yes! Now go to settings to give permissions!")
            goToAppsSettings()

        }
        return v
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        //return super.onCreateDialog(savedInstanceState)
//        if(this.isAdded) {
//            Log.d(TAG, "showBottomDialog: frag already added yo!!")
//            val emptyDialog: Dialog = Dialog(receivedAct)
//            return emptyDialog
//        }
        Log.d(TAG, "onCreateDialog: .. ")

//        val ft: FragmentTransaction = fragmentManager!!.beginTransaction()
//        val prevFragment: Fragment? = fragmentManager!!.findFragmentByTag("myShitzYo")
//        if(prevFragment!=null) {
//            Log.d(TAG, "onCreateDialog: not null") //
//        }else if(prevFragment==null) {
//            Log.d(TAG, "onCreateDialog: null..")
//        }

        val btmSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        btmSheetDialog.behavior.isDraggable = false
//        btmSheetDialog.setOnShowListener {
//        // Bottom Sheet Dialog 가 보여졌을때 어떤 설정을 할지 여기서 지정.
//        }
        return btmSheetDialog
    }
    //    fun shouldRemoveBtmDialog(): Boolean {
//
//    }
    fun showBtmPermDialog(calledActivity: Activity) {
        myContext = calledActivity as FragmentActivity
        Log.d(TAG, "showBottomDialog: Starts!")
        val STYLE_NORMAL = 0 // Bottom Sheet 스타일 설정에 들어가는것.. 흐음..

        this.apply {
            setStyle(STYLE_NORMAL, R.style.BottomSheetDialogStyle)
            isCancelable = false // 배경 클릭 금지.
            show(myContext.supportFragmentManager,"myShitzYo") // "myShitzYo" 이 값은 identifier.. ?
        }
    }

    fun removePermBtmSheetAndResume() {

        this.apply {
            if(isAdded) { //1) BottomSheet 이 화면에 보이거나 존재하는 상태?. (isAdded=true) if the fragment is currently added to its activity.
                Log.d(TAG, "removePermBtmSheet: We'll dismiss our PERMISSION BOTTOM SHEEEEEETz Yo!")
                isCancelable = true
                dismiss() // close.. settings 에서 permission 을 주고 app 을 다시 열었을 때 bottom Sheet (Fragment) 자체가 없으므로 여기서 에러남!! 그래서 if(isAdded) 추가했음!
            }else { // 2) Bottom Fragment 가 없음= Permission Granted 된 상황일듯.
                Log.d(TAG, "removePermBottomSheet: 없앨 Permission Bottom Fragment 가 없음")
            }

        }
    }
    fun goToAppsSettings() {
        Log.d(TAG, "goToAppsSetting: Now we will go to SETTINGS PAGE!")

        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", myContext.packageName, null))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent) // onResume() 에서 다시 check 필요
    }
}