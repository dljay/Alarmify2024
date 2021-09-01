package com.theglendales.alarm.jjmvvm.helper

import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import com.theglendales.alarm.jjadapters.RcViewAdapter
import com.theglendales.alarm.jjdata.GlbVars
import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp
import io.gresse.hugo.vumeterlibrary.VuMeterView

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
        // 1) 만약 다른 holder 의 loadingCircle 이 작동중였다면 (빙글빙글.) 이놈을 우선 정지하고(a). ivThumbNail 밝기를 원래로 변경(b)
        stopPreviousLCnVm()
        // 2) 새로 선택된 viewHolder 의 circle (progress bar) (a) 과 ivThumbNail (b)을 assign 하고.
        assignLcIvVm()
        // 3) 새로 assign 한 loadingCircle 을 작동!
        turnOnLC()
    }

    private fun stopPreviousLCnVm() {
        if(loadingCircle!=null && loadingCircle?.visibility == View.VISIBLE) {
            loadingCircle!!.visibility = View.INVISIBLE // (a) loading Circle 안보이게.
            ivThumbNail!!.alpha = 1.0f // (b) 밝기 원복
        }
        if(vuMeter != null) {
            vuMeter!!.visibility = VuMeterView.GONE
        }
    }
    private fun assignLcIvVm() { // Assign Loading Circle / IvThumbNail / VuMeter
        loadingCircle = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.loadingCircle // (a)
        ivThumbNail = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.iv_Thumbnail // (b)
        vuMeter = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.vuMeterView // (b)
    }
    private fun turnOnLC() {

        loadingCircle!!.visibility = View.VISIBLE
        ivThumbNail!!.alpha = 0.3f
    }
// VuMeter
    fun vumeterPlay() {
        stopPreviousLCnVm()
        assignLcIvVm()

        if (vuMeter != null) {
            vuMeter!!.resume(true)
            vuMeter!!.visibility = VuMeterView.VISIBLE
            ivThumbNail!!.alpha = 0.3f
        }

    }
    fun vumeterPause() {
        stopPreviousLCnVm()
        assignLcIvVm()

        if(vuMeter!=null) {
            vuMeter!!.pause()
            vuMeter!!.visibility = VuMeterView.VISIBLE
            ivThumbNail!!.alpha = 0.3f
        }
    }
}