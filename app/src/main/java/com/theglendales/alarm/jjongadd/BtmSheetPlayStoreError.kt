package com.theglendales.alarm.jjongadd

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.theglendales.alarm.R

private const val TAG="BtmSht_Perm"

object BtmSheetPlayStoreError : BottomSheetDialogFragment() {
    private var fragActivity = FragmentActivity()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {

        Log.d(TAG, "onCreateView: BottomSheet_PERMISSION ")
        val v: View = inflater.inflate(R.layout.bottom_sheet_playstore_error, container, false) // 우리가 만든 Bottom Sheet xml 파일.
        val tvCancel: TextView = v.findViewById(R.id.tv_PlayStore_Cancel)
        val ivGoToPlayStore: ImageView = v.findViewById(R.id.iv_PlayStore)

        tvCancel.setOnClickListener {
            Log.d(TAG, "onCreate: Clicked Cancel.Dismiss Bottom sheet! - GooglePlay")
            removePlayErrorBtmSheetAndResume()
        }
        ivGoToPlayStore.setOnClickListener {
            Log.d(TAG, "onCreate: Yes! Now go to GooglePlay!!")
            goToPlayStore()

        }
        return v
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        //return super.onCreateDialog(savedInstanceState)

        Log.d(TAG, "onCreateDialog: .. ")

        val btmSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        btmSheetDialog.behavior.isDraggable = false
        return btmSheetDialog
    }
    fun showBtmSheetPlayStoreError(calledActivity: Activity) {
        if(isAdded) {
            Log.d(TAG, "showBtmSheetPlayStoreError: Already Showing .. Return!")
            return
        }
        fragActivity = calledActivity as FragmentActivity
        Log.d(TAG, "showBtmSheetPlayStoreError: Starts!")
        val STYLE_NORMAL = 0 // Bottom Sheet 스타일 설정에 들어가는것.. 흐음..

        this.apply {
            setStyle(STYLE_NORMAL, R.style.BottomSheetDialogStyle)
            isCancelable = false // 배경 클릭 금지.
            show(fragActivity.supportFragmentManager,"playStoreTag") // "myShitzYo" 이 값은 identifier.. ?
        }
    }

    fun removePlayErrorBtmSheetAndResume() {

        this.apply {
            if(isAdded) { //1) BottomSheet 이 화면에 보이거나 존재하는 상태?. (isAdded=true) if the fragment is currently added to its activity.
                Log.d(TAG, "removePlayErrorBtmSheetAndResume: We'll dismiss our GooglePlay BOTTOM SHEEEEEETz Yo!")
                isCancelable = true
                dismiss() // close.. settings 에서 permission 을 주고 app 을 다시 열었을 때 bottom Sheet (Fragment) 자체가 없으므로 여기서 에러남!! 그래서 if(isAdded) 추가했음!
            }else { // 2) Bottom Fragment 가 없음= Permission Granted 된 상황일듯.
                Log.d(TAG, "removePlayErrorBtmSheetAndResume: 없앨 GOOGLE PLAY Bottom Fragment 가 없음")
            }

        }
    }
    fun goToPlayStore() {
        Log.d(TAG, "goToPlayStore: Now we will go to Play Store")

        try {
            val playStoreUri = "https://play.google.com"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUri))
            startActivity(intent)

        } catch (e: ActivityNotFoundException) {
            Log.d(TAG, "goToPlayStore: Failed to Launch Google Play. Error=$e")
        }
    }
}