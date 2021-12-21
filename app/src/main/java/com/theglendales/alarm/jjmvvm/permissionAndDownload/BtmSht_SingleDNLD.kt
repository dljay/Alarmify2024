package com.theglendales.alarm.jjmvvm.permissionAndDownload

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

private const val TAG="BtmSht_SingleDownload"
class BtmSht_SingleDNLD : BottomSheetDialogFragment() {

    private var myContext = FragmentActivity()
    lateinit var linearPrgsIndicator : LinearProgressIndicator
    lateinit var objAnim: ObjectAnimator



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        Log.d(TAG, "onCreateView: BottomSheet_Single Download! ")
        val v: View = inflater.inflate(R.layout.bottom_sheet_single_download, container, false) // 우리가 만든 Bottom Sheet xml 파일.
        //val tvClose: TextView = v.findViewById(R.id.id_tv_Close)
        linearPrgsIndicator = v.findViewById(R.id.id_dnld_linearPrgsBar)
        objAnim = ObjectAnimator.ofInt(linearPrgsIndicator,"progress", 0) // 최초 progress 는 0으로 초기화.

        return v
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        Log.d(TAG, "onCreateDialog: BottomSheet_SINGLE_DOWNLOAD!! ")
        val btmSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        btmSheetDialog.behavior.isDraggable = false

        return btmSheetDialog
    }

    fun showBtmSingleDNLDSheet(calledActivity: Activity) {
        myContext = calledActivity as FragmentActivity
        Log.d(TAG, "showBottomDialog: Starts!")
        val STYLE_NORMAL = 0 // Bottom Sheet 스타일 설정에 들어가는것.. 흐음..

        this.apply {
            setStyle(STYLE_NORMAL, R.style.BottomSheetDialogStyle)
            isCancelable = false // 배경 클릭 금지.
            show(myContext.supportFragmentManager,"myShitzYo") // "myShitzYo" 이 값은 identifier.. ?
        }
    }

    fun removeSingleDNLDBtmSheet() {
        Log.d(TAG, "removeSingleDNLDBtmSheet: called!")
        this.apply {
            if(isAdded) { //1) BottomSheet 이 화면에 보이거나 존재하는 상태?. (isAdded=true) if the fragment is currently added to its activity.
                Log.d(TAG, "removeBtmSheet_SINLGE DOWNLOAD: Dismiss BOTTOM Sheet- 없앨놈 있음! ^^ (OO)")
                isCancelable = true
                dismiss() // close.. settings 에서 permission 을 주고 app 을 다시 열었을 때 bottom Sheet (Fragment) 자체가 없으므로 여기서 에러남!! 그래서 if(isAdded) 추가했음!
            }else { // 2) Bottom Fragment 가 없음= Permission Granted 된 상황일듯.
                Log.d(TAG, "removeBottomSheet_SINLGE DOWNLOAD: Downloading Single item: 없앨 Bottom Fragment 가 없음! T_T")
            }

        }
    }
//Linear Progress Indicator(aka the new ProgressBar) Animation Related
    fun animateLPI(progressReceived: Int, durationMs: Long) { // LPI = Linear Progress Indicator
        Log.d(TAG, "animateLPI: called!")
        objAnim = ObjectAnimator.ofInt(linearPrgsIndicator,"progress",progressReceived)
        objAnim.setAutoCancel(true) // todo.. hmm.. test?
        objAnim.duration = durationMs
        objAnim.start()
    }

    fun isAnimationRunning(): Boolean = objAnim.isRunning


}