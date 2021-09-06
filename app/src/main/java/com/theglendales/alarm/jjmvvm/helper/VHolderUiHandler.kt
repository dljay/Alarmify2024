package com.theglendales.alarm.jjmvvm.helper

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import com.theglendales.alarm.jjadapters.RcViewAdapter
import com.theglendales.alarm.jjdata.GlbVars
import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp
import io.gresse.hugo.vumeterlibrary.VuMeterView

private const val TAG="VHolderUiHandler"
// 이 클래스는 SecondFragment 에서 해결해야 될 Vumeter/Loading Circle/IV Thumbnail UI 관련 일들을 대신 해 줌. SecondFragment.kt 에 적기에는 너무 방대하여 이 클래스를 만듬.
// 이건 Koin 으로 Singleton 으로 만들었으니 마음껏 활용합세!!
class VHolderUiHandler {

    companion object {
        //var currentStatusMp: StatusMp = StatusMp.IDLE
    }
    private var loadingCircle: ProgressBar? = null // 현재 혹은 이전에 activate 된 loadingCircle 을 여기에 저장.
    private var vuMeter: VuMeterView? = null
    private var ivThumbNail: ImageView? = null

    fun LcVmIvUiCtrl(playStatus: StatusMp) {
        Log.d(TAG, "newController: playStatus= $playStatus")
        // 1) 기존에 LC/Vm/IV 작동되고 있는 트랙이 있으면 다 Clear
        if(loadingCircle!=null && ivThumbNail!=null && vuMeter !=null) {
            Log.d(TAG, "newController: clearing previous viewholder UIs")
            loadingCircle!!.visibility = View.INVISIBLE // loading Circle 안보이게. (a)
            vuMeter!!.visibility = VuMeterView.GONE // VuMeter 도 안보이게 (b)
            ivThumbNail!!.alpha = 1.0f // (c) 썸네일 밝기 원복
        }
        // 2) 새로 선택된 trakId 정보를 바탕으로 새로운 view assign
        loadingCircle = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.loadingCircle // (a)
        vuMeter = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.vuMeterView // (b)
        ivThumbNail = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.iv_Thumbnail // (c)

        // 3) UI 업뎃 (Play & Loading)
        when(playStatus) {
            StatusMp.LOADING -> {
                loadingCircle!!.visibility = View.VISIBLE
                ivThumbNail!!.alpha = 0.3f
                vuMeter!!.visibility = VuMeterView.GONE
                }
            StatusMp.PLAY -> {
                vuMeter!!.visibility = VuMeterView.VISIBLE
                vuMeter!!.resume(true)
                ivThumbNail!!.alpha = 0.3f
                }
            StatusMp.PAUSED -> {
                //Music is paused (by force)
                if(vuMeter!=null) {vuMeter!!.pause()}



            }
        }
    }


// ************************************ //




}