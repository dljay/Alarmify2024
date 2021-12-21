package com.theglendales.alarm.jjmvvm.unused

import android.animation.ObjectAnimator
import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.theglendales.alarm.R

private const val TAG="BtmSht_Sync"
object BtmSht_Sync  : BottomSheetDialogFragment() {
    private var myContext = FragmentActivity()

    lateinit var lpiMulti : LinearProgressIndicator
    lateinit var objAnim: ObjectAnimator

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        Log.d(TAG, "onCreateView: BottomSheet_SYNC.... ")
        val v: View = inflater.inflate(R.layout.bottom_sheet_multi_dnld, container, false) // 우리가 만든 Bottom Sheet xml 파일.
        //val tvConfirm: TextView = v.findViewById(R.id.id_tv_Confirm)

        lpiMulti = v.findViewById(R.id.id_dnld_lpi_multi)
        lpiMulti.progress = 0
        ObjectAnimator.ofInt(lpiMulti,"progress", 0) // 최초 progress 는 0으로 초기화.

//        tvConfirm.setOnClickListener {
//            Log.d(TAG, "onCreate: Clicked Confirm. Dismiss Bottom sheet & Resume RcView!")
//            removeMultiDNLDBtmSheet()
//        }
        return v
    }
    // 이게 먼저 실행됨!!!
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        Log.d(TAG, "onCreateDialog: BottomSheet_SYNC.. ")
        val btmSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        btmSheetDialog.behavior.isDraggable = false

        return btmSheetDialog
    }

    fun showBtmSyncDialog(calledActivity: Activity) {
        myContext = calledActivity as FragmentActivity
        Log.d(TAG, "showBtmSyncDialog: Starts!")
        val STYLE_NORMAL = 0 // Bottom Sheet 스타일 설정에 들어가는것.. 흐음..

        this.apply {
            setStyle(STYLE_NORMAL, R.style.BottomSheetDialogStyle)
            isCancelable = false // 배경 클릭 금지.
            show(myContext.supportFragmentManager,"myShitzYo") // "myShitzYo" 이 값은 identifier.. ?
        }
    }

    fun removeMultiDNLDBtmSheet() {

        this.apply {
            if(isAdded) { //1) BottomSheet 이 화면에 보이거나 존재하는 상태?. (isAdded=true) if the fragment is currently added to its activity.
                Log.d(TAG, "removeMultiDNLDBtmSheet: Dismiss BOTTOM Sheet")
                isCancelable = true
                dismiss() // close.. settings 에서 permission 을 주고 app 을 다시 열었을 때 bottom Sheet (Fragment) 자체가 없으므로 여기서 에러남!! 그래서 if(isAdded) 추가했음!
            }else { // 2) Bottom Fragment 가 없음= Permission Granted 된 상황일듯.
                Log.d(TAG, "removeMultiDNLDBtmSheet: Sync In Progress.. 없앨 Bottom Fragment 가 없음")
            }
        }
    }

    fun animateLPI(progressReceived: Int, durationMs: Long) { // LPI = Linear Progress Indicator
        objAnim = ObjectAnimator.ofInt(lpiMulti,"progress",progressReceived)
        objAnim.duration = durationMs
        objAnim.start()
        objAnim.setAutoCancel(true)
    }
//    fun initiObjAnim() { // 다운이 끝나면 기존 progress 값을 초기화. (이거 안하면 그전 prgrs 값(ex.75%) 에서 막 시작하고 그러네.
//        objAnim = ObjectAnimator.ofInt(lpiMulti,"progress",0)
//    }
//    fun isAnimationRunning(): Boolean = objAnim.isRunning
}