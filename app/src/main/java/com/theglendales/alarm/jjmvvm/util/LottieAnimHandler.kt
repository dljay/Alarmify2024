package com.theglendales.alarm.jjmvvm.util

import android.app.Activity
import android.util.Log
import com.airbnb.lottie.LottieAnimationView
import com.theglendales.alarm.R

private const val TAG="LottieAnimHandler"
class LottieAnimHandler(private val receivedActivity: Activity, private val lottieAnimationView: LottieAnimationView) {

    //lottieAnimation Controller
    fun animController(status: String) {
        when (status) {
            "purchaseCircle" -> {
                receivedActivity.runOnUiThread {
                    Log.d(TAG, "animController: called")
                    lottieAnimationView.visibility = LottieAnimationView.VISIBLE
                    // 불투명 + 투과금지 + 사이즈
                    lottieAnimationView.setAnimation(R.raw.lottie_circular_loading)}
            }

            "initialLoading" -> {
                Log.d(TAG, "animController: initialLoading")
                lottieAnimationView.setAnimation(R.raw.lottie_loading1)
            } //최초 app launch->read.. auto play 기 때문에
            "error" -> {
                receivedActivity.runOnUiThread(Runnable
                {
                    Log.d(TAG, "animController: NO INTERNET ERROR!!")
                    lottieAnimationView.visibility = LottieAnimationView.VISIBLE
                    lottieAnimationView.setAnimation(R.raw.lottie_error1)

                    //snackBarDeliverer(lottieAnimationView,"Please kindly check your network connection status",false)

                    //todo: 여기 SnackBar 에서 View 가 불안정할수 있음=>try this? -> Snackbar.make(requireActivity().findViewById(android.R.id.content), "..", Snackbar.LENGTH_LONG).show()

                })
            }
            "stop" -> {
                receivedActivity.runOnUiThread(Runnable
                {
                    Log.d(TAG, "animController: STOP (any) Animation!!")
                    lottieAnimationView.cancelAnimation()
                    lottieAnimationView.visibility = LottieAnimationView.GONE
                })
            }

        }
    }

}