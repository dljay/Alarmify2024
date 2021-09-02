package com.theglendales.alarm.jjmvvm.helper

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import com.theglendales.alarm.jjadapters.RcViewAdapter
import com.theglendales.alarm.jjdata.GlbVars
import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp
import io.gresse.hugo.vumeterlibrary.VuMeterView

private const val TAG="VuMeterHandler"
// 이 클래스는 SecondFragment 에서 해결해야 될 Vumeter UI 업뎃 관련 일들을 대신 해 줌. SecondFragment.kt 에 적기에는 너무 방대하여 이 클래스를 만듬.
// 이건 Koin 으로 Singleton 으로 만들었으니 마음껏 활용합세!!
class VuMeterHandler {

    companion object {
        var currentStatusMp: StatusMp = StatusMp.IDLE
    }
    private var loadingCircle: ProgressBar? = null // 현재 혹은 이전에 activate 된 loadingCircle 을 여기에 저장.
    private var vuMeter: VuMeterView? = null
    private var ivThumbNail: ImageView? = null


    fun activateLC() {
        stopPrevAssignNew() // once this is done then
        turnOnLC() // run this.

    }

    private fun stopPrevAssignNew() { // 코루틴 + Status 를 input parameter 로..
        // 1-a)기존에 작동되는 loadingCircle 이 있었다면 (즉 다른 트랙을 play 중였다면.) 멈춤.
        if(loadingCircle!=null && loadingCircle?.visibility == View.VISIBLE) {
            Log.d(TAG, "stopPreviousLCnVm: inside if..")
            loadingCircle!!.visibility = View.INVISIBLE // loading Circle 안보이게.
            ivThumbNail!!.alpha = 1.0f // (b) 썸네일 밝기 원복
        }
        //1-b) VuMeter 도 안보이게
        if(vuMeter != null) {
            vuMeter!!.visibility = VuMeterView.GONE
        }
        //3) 새로 선택된 row 의 View 를 assign
            loadingCircle = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.loadingCircle // (a)
            ivThumbNail = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.iv_Thumbnail // (b)
            vuMeter = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.vuMeterView // (b)
    }

    private fun turnOnLC() {
        Log.d(TAG, "turnOnLC: called")
        loadingCircle!!.visibility = View.VISIBLE
        ivThumbNail!!.alpha = 0.3f
    }
    private fun playVM() {
        if (vuMeter != null) {
            vuMeter!!.resume(true)
            vuMeter!!.visibility = VuMeterView.VISIBLE
            ivThumbNail!!.alpha = 0.3f
        }
    }
    private fun pauseVM() {
        if(vuMeter!=null) {
            vuMeter!!.pause()
            vuMeter!!.visibility = VuMeterView.VISIBLE
            ivThumbNail!!.alpha = 0.3f
        }
    }
// VuMeter
    fun vumeterPlay() {
      stopPrevAssignNew()
    }
    fun vumeterPause() {

    }
}