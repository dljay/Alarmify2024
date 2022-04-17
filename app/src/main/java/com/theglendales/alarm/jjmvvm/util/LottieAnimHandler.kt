package com.theglendales.alarm.jjmvvm.util

import android.app.Activity
import android.util.Log
import com.airbnb.lottie.LottieAnimationView
import com.theglendales.alarm.R

private const val TAG="LottieAnimHandler"
enum class LottieENUM { INIT_LOADING, ERROR_GENERAL, PURCHASED_ITEM_EMPTY, STOP_ALL}
class LottieAnimHandler(private val receivedActivity: Activity, private val lottieAnimationView: LottieAnimationView) {

    //lottieAnimation Controller
    fun animController(lottieEnum: LottieENUM) {
        when (lottieEnum) {
        //최초 app launch->read.. auto play 기 때문에
            LottieENUM.INIT_LOADING -> {
                Log.d(TAG, "animController: initialLoading")
                lottieAnimationView.setAnimation(R.raw.lottie_loading_threedot_accent_color)


            }
        // 인터넷 안되는 에러
            LottieENUM.ERROR_GENERAL -> {
                receivedActivity.runOnUiThread(Runnable
                {
                    Log.d(TAG, "animController: GENERAL ERROR!!")
                    lottieAnimationView.visibility = LottieAnimationView.VISIBLE
                    lottieAnimationView.setAnimation(R.raw.lottie_error1)
                    //snackBarDeliverer(lottieAnimationView,"Please kindly check your network connection status",false)
                    //여기 SnackBar 에서 View 가 불안정할수 있음=>try this? -> Snackbar.make(requireActivity().findViewById(android.R.id.content), "..", Snackbar.LENGTH_LONG).show()
                })
            }
            LottieENUM.PURCHASED_ITEM_EMPTY -> {
                receivedActivity.runOnUiThread {
                    lottieAnimationView.visibility = LottieAnimationView.VISIBLE
                    lottieAnimationView.setAnimation(R.raw.lottie_empty_box)
                }
            }
            LottieENUM.STOP_ALL -> {
                receivedActivity.runOnUiThread(Runnable
                {
                    Log.d(TAG, "animController: STOP ALL Animation!!")
                    lottieAnimationView.cancelAnimation()
                    lottieAnimationView.visibility = LottieAnimationView.GONE
                })
            }

        }
    }

}