package com.theglendales.alarm.jjmvvm.iapAndDnldManager

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.theglendales.alarm.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG="BtmShtSingleDNLDV2"

class BtmShtSingleDNLDV2 : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(): BtmShtSingleDNLDV2 {
            return BtmShtSingleDNLDV2()
        }
    }
    lateinit var tvRtTitle: TextView
    lateinit var linearPrgsIndicator : LinearProgressIndicator
    lateinit var objAnim: ObjectAnimator
    lateinit var lottieCircle: LottieAnimationView

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView: BtmSht_Single_DNLD2_Called!")
        val v: View = inflater.inflate(R.layout.bottom_sheet_single_download, container, false) // 우리가 만든 Bottom Sheet xml 파일.
        return v
        //return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: called!")
    // 이 Btm Frag 에 대한 설정

        this.apply {
            setStyle(STYLE_NORMAL, R.style.BottomSheetDialogStyle)
            isCancelable = false // 배경 클릭 금지.
        }
    // INIT! always initialize other view here!! onCreateView 에서는 'v' 가 proper 하게 init 되지 않아서 뻑나는 경우가 있음
        tvRtTitle = view.findViewById(R.id.tv_dnldRtTitle)
        lottieCircle = view.findViewById(R.id.id_lottie_loading_circle)
        linearPrgsIndicator = view.findViewById(R.id.id_dnld_linearPrgsBar)
        linearPrgsIndicator.progress = 0 // 다음 다운로드를 위해 Prgrs 를 '0' 으로 초기화.
        // Dnld_BtmSheet 이 열린 순간에 : LottieCircle(VISIBLE), LPI(GONE) [추후 첫 DNLD PRGRS 받자마자 반대로 Lottie(X) LPI(O), tvRtTitle=곡제목
        lottieCircle.visibility = LottieAnimationView.VISIBLE
        linearPrgsIndicator.visibility = View.GONE
        updateTitleTextView(null) // title=Null -> PREPARING TO DOWNLOAD 라고 뜸



        objAnim = ObjectAnimator.ofInt(linearPrgsIndicator,"progress", 0) // 최초 progress 는 0으로 초기화.


    }
// Show & Remove -->
    override fun show(manager: FragmentManager, tag: String?) {
        //super.show(manager, tag)
        if(isAdded) {
            Log.d(TAG, "show: Already showing BtmSheetFrag. return!")
            return
        } else {
            Log.d(TAG, "show: Show BtmSheetFrag now. 기존에 띄워있던 창이 없는듯.. ")
            val ft = manager.beginTransaction()
            ft.add(this, tag)
            ft.commitAllowingStateLoss()
        }
        
    }

    fun removeBtmSheetImmediately() {
        Log.d(TAG, "removeBtmSheetImmediately: called!")
        this.apply {
            if(isAdded) { //1) BottomSheet 이 화면에 보이거나 존재하는 상태?. (isAdded=true) if the fragment is currently added to its activity.
                Log.d(TAG, "removeBtmSheetImmediately: Dismiss BOTTOM Sheet- 없앨놈 있음! ^^ (OO)")
                dismiss()
            }else { // 2) Bottom Fragment 가 없음= Permission Granted 된 상황일듯.
                Log.d(TAG, "removeBtmSheetImmediately: Downloading Single item: 없앨 Bottom Fragment 가 없음! T_T")
            }
        }
    }
    fun removeBtmSheetAfterOneSec() {
        Log.d(TAG, "removeBtmSheetAfterOneSec: called!")
        this.apply {
            if(isAdded) { //1) BottomSheet 이 화면에 보이거나 존재하는 상태?. (isAdded=true) if the fragment is currently added to its activity.
                Log.d(TAG, "removeBtmSheetAfterOneSec: Dismiss BOTTOM Sheet- 없앨놈 있음! ^^ (OO)")

                Handler(Looper.getMainLooper()).postDelayed(
                    {// This method will be executed once the 다운로드 is over
                        if(!this::linearPrgsIndicator.isInitialized) { linearPrgsIndicator.progress = 0 } // prgrsBar 0 값으로. 혹시 몰라서 init 확인 if 문에 넣어놨음.
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
    fun prepAndAnimateLPI(prgrsReceived: Int) {
        // 우선 objAnim 이나, linearPrgsIndicator 가 Init 이 안됐으면 바로 종료
        if(!this::linearPrgsIndicator.isInitialized||!this::objAnim.isInitialized) {
            Log.d(TAG, "prepAnim: linearPrgsBar or ObjAnim not initialized yet. return!")
            return
        }
    //        if(isAnimationRunning()) { // 간혹 Prgrs 가 1초만에 두번 입력되는 경우가 있음( 1->85..) 이런식으로. 그랬을때 85로 뛰는 Prgrs 는 진행이 안되고 멈춰있는것처럼 보임. 일단 없애도 문제는 없는것처럼 보이기는 함.
    //            Log.d(TAG, "prepAnim: Animation is already running!")
    //            return
    //        }
        Log.d(TAG, "prepAnim: PrgrsReceived=$prgrsReceived ")
        val randomDuration = (2000L..5000L).random() // 그래프 차는 빠르기 랜덤 시간 값.
        animateLPI(prgrsReceived, randomDuration)
    }
    fun animateLPI(progressReceived: Int, durationMs: Long) { // LPI = Linear Progress Indicator
        Log.d(TAG, "animateLPI: called! PrgrsReceived=$progressReceived")
    // 우선 objAnim 이나, linearPrgsIndicator 가 Init 이 안됐으면 바로 종료
        if(!this::linearPrgsIndicator.isInitialized||!this::objAnim.isInitialized) {Log.d(TAG, "prepAnim: linearPrgsBar or ObjAnim not initialized yet. return!")
        return}

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
    fun showLPIAndHideLottieCircle(isPreparingToDNLD: Boolean) {
        
        if(!isPreparingToDNLD && this::linearPrgsIndicator.isInitialized && this::lottieCircle.isInitialized) {
            //b) SingleDownloaderV3.kt > 첫 DNLD 프로그레스 받는 순간 -> isPreparingToDNLD.false ->  LottieCircle(GONE), LPI(VISIBLE))\
            Log.d(TAG, "showLPIAndHideLottieCircle: Lottie(X) LPI(O)")
            lottieCircle.visibility = LottieAnimationView.GONE
            linearPrgsIndicator.visibility = View.VISIBLE
        }
    }

// 다운시작하면 TextView 에 현재 다운 받는 곡 명 써주기.
    fun updateTitleTextView(rtTitle: String?) {
        if(!this::tvRtTitle.isInitialized) {
            return
        }
        if(rtTitle.isNullOrEmpty()) {
            tvRtTitle.text = "PREPARING TO DOWNLOAD .."
        } else {
            tvRtTitle.text = "DOWNLOADING $rtTitle "
        }
    }
    
    /*fun cancelAndNullifyAnim() {
        objAnim.cancel()
        objAnim = null
    }*/


}