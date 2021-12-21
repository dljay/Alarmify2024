package com.theglendales.alarm.jjmvvm.iapAndDnldManager

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.theglendales.alarm.R
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

private const val TAG="BtmSht_SingleDNLD2"

class BtmSht_SingleDNLD2 : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(): BtmSht_SingleDNLD2 {
            return BtmSht_SingleDNLD2()
        }
    }

    lateinit var linearPrgsIndicator : LinearProgressIndicator
    lateinit var objAnim: ObjectAnimator

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView: BtmSht_Single_DNLD2_Called")
        val v: View = inflater.inflate(R.layout.bottom_sheet_single_download, container, false) // 우리가 만든 Bottom Sheet xml 파일.


        return v
        //return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    // always initialize other view here!! onCreateView 에서는 'v' 가 proper 하게 init 되지 않아서 뻑나는 경우가 있음
        linearPrgsIndicator = view.findViewById(R.id.id_dnld_linearPrgsBar)
        //linearPrgsIndicator.progress = 0 // 다음 다운로드를 위해 Prgrs 를 '0' 으로 초기화.
        objAnim = ObjectAnimator.ofInt(linearPrgsIndicator,"progress", 0) // 최초 progress 는 0으로 초기화.
    }
// Show & Remove -->
    override fun show(manager: FragmentManager, tag: String?) {
        //super.show(manager, tag)
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }
    fun removeBtmSheetAfterOneSec() {
        Log.d(TAG, "remove: called!")
        this.apply {
            if(isAdded) { //1) BottomSheet 이 화면에 보이거나 존재하는 상태?. (isAdded=true) if the fragment is currently added to its activity.
                Log.d(TAG, "remove: Dismiss BOTTOM Sheet- 없앨놈 있음! ^^ (OO)")

                Handler(Looper.getMainLooper()).postDelayed(
                    {// This method will be executed once the timer is over
                        isCancelable = true
                        dismiss() // close.. settings 에서 permission 을 주고 app 을 다시 열었을 때 bottom Sheet (Fragment) 자체가 없으므로 여기서 에러남!! 그래서 if(isAdded) 추가했음!
                    },
                    1000 // value in milliseconds
                )
            }else { // 2) Bottom Fragment 가 없음= Permission Granted 된 상황일듯.

                Log.d(TAG, "removeBtmSheet: Downloading Single item: 없앨 Bottom Fragment 가 없음! T_T")
            }

        }
    }
// Animation
    fun animateLPI(progressReceived: Int, durationMs: Long) { // LPI = Linear Progress Indicator
        Log.d(TAG, "animateLPI: called!")
        objAnim = ObjectAnimator.ofInt(linearPrgsIndicator,"progress",progressReceived). apply {
            setAutoCancel(true)
            duration = durationMs
            start()
        }
    }
    fun isAnimationRunning(): Boolean {
        Log.d(TAG, "isAnimationRunning: objAnim.isRunning=${objAnim.isRunning}")
        return objAnim.isRunning 
    }
    fun prepAnim(prgrsReceived: Int) {
        if(isAnimationRunning()) {
            Log.d(TAG, "prepAnim: Animation is already running! No more action is needed!")
            return
        }
        Log.d(TAG, "prepAnim: PrgrsReceived=$prgrsReceived ")
        val randomDuration = (2000L..5000L).random() // 그래프 차는 빠르기 랜덤 시간 값.
        animateLPI(prgrsReceived, randomDuration)
//        when (prgrsReceived) { <- 이런거 필요없음.
//            in 0..20 -> { // 0 이상 20 이하
//                Log.d(TAG, "prepAnim: Between 0 & 20. PrgrsReceived=$prgrsReceived ") }

    }
    
    /*fun cancelAndNullifyAnim() {
        objAnim.cancel()
        objAnim = null
    }*/


}