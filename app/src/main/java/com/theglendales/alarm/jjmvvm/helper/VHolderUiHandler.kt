package com.theglendales.alarm.jjmvvm.helper

import android.content.Context
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
class VHolderUiHandler(context: Context) {

    companion object {
        //var currentStatusMp: StatusMp = StatusMp.IDLE
    }
    private var loadingCircle: ProgressBar? = null // 현재 혹은 이전에 activate 된 loadingCircle 을 여기에 저장.
    private var vuMeter: VuMeterView? = null
    private var ivThumbNail: ImageView? = null

    fun LcVmIvController(playStatus: StatusMp) {
        Log.d(TAG, "LcVmIvController: playStatus= $playStatus")
    // 1) Pause
        if(playStatus == StatusMp.PAUSED) {
            if(vuMeter==null) {
                Log.d(TAG, "LcVmIvController: vuMeter Null.")}
        // 1-a) 단순히 현재 재생중인 트랙을 재생중 Pause 시킨거였다면 VuMeter 만 Pause 시킬것.
            if(vuMeter!=null && vuMeter == RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.vuMeterView) {vuMeter!!.pause()}
        // 1-b) 다른 fragment 갔다와서 메모리의 vuMeter 가 변경된 상태(rcv-viewHolderMap 가 새로 업뎃되었으니깐) 에서 PAUSE animation 이 필요한 상태라면
            if(vuMeter!=null && vuMeter != RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.vuMeterView) {
                //1-b-1) 새로운 view 를 assign
                loadingCircle = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.loadingCircle // (a)
                vuMeter = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.vuMeterView // (b) 새로운 vuMeter 를 assign
                ivThumbNail = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.iv_Thumbnail // (c)
                // 1-b-2) vuMeter pause 시켜줌.
                vuMeter!!.pause()//
            }
            return
        }
        if(playStatus == StatusMp.READY) { // 음악 재생중->Pause 후 -> Seek Bar 아무데나 클릭했을 때. Loading Circle 없애주기
            loadingCircle!!.visibility = View.INVISIBLE
            vuMeter!!.visibility = VuMeterView.VISIBLE
            vuMeter!!.pause()
            return
        }
    // 2) 그 외 신규 트랙 클릭했을 때는 아래의 과정이 실행.
        // 2-A) 기존에 LC/Vm/IV 작동되고 있는 트랙이 있으면 다 Clear
        if(loadingCircle!=null && ivThumbNail!=null && vuMeter !=null) {
            Log.d(TAG, "newController: clearing previous viewholder UIs")
            loadingCircle!!.visibility = View.INVISIBLE // loading Circle 안보이게. (a)
            vuMeter!!.visibility = VuMeterView.GONE // VuMeter 도 안보이게 (b)
            ivThumbNail!!.alpha = 1.0f // (c) 썸네일 밝기 원복
        }
        // 2-B) 새로 선택된 trakId 정보를 바탕으로 새로운 view 들을 assign
            loadingCircle = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.loadingCircle // (a)
            vuMeter = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.vuMeterView // (b)
            ivThumbNail = RcViewAdapter.viewHolderMap[GlbVars.clickedTrId]?.iv_Thumbnail // (c)


        // 2-C) UI 업뎃 (Play & Loading)
        when(playStatus) {
            StatusMp.BUFFERING -> {
                loadingCircle!!.visibility = View.VISIBLE
                ivThumbNail!!.alpha = 0.3f
                vuMeter!!.visibility = VuMeterView.GONE
                }
            StatusMp.PLAY -> {
                vuMeter!!.visibility = VuMeterView.VISIBLE
                vuMeter!!.resume(true)
                ivThumbNail!!.alpha = 0.3f
                }
        }
    }


// ************************************ //




}