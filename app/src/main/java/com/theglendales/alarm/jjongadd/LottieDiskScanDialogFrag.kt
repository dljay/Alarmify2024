package com.theglendales.alarm.jjongadd

import android.animation.Animator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.snackbar.Snackbar
import com.theglendales.alarm.R

// 싱글톤으로..
private const val TAG="LottieDiskScanDialogFrag"
class LottieDiskScanDialogFrag: DialogFragment() {

    companion object {// Singleton 으로 사용하기 위해.
    fun newInstanceDialogFrag(): LottieDiskScanDialogFrag {
        return LottieDiskScanDialogFrag()
        }
    }

    lateinit var lottieView: LottieAnimationView

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {
        Log.d(TAG, "onCreateView: called")
        //return super.onCreateView(inflater, container, savedInstanceState)
    // 위치 설정
        //dialog?.window?.setGravity(Gravity.CENTER_HORIZONTAL)
        val window = dialog?.window
        val params = window?.attributes
        params?.x = 0
        params?.y = -100
        window?.attributes = params
    // 기타 설정

        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        isCancelable =false
        return inflater.inflate(R.layout.lottie_rebuild_rt, container, false) //xml 로 inflate..
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lottieView = view.findViewById(R.id.id_lottie_rebuild_rt)
        lottieView.addAnimatorListener(object : Animator.AnimatorListener{
            override fun onAnimationStart(animation: Animator?) {
                Log.d(TAG, "onAnimationStart: started..")
            }

            override fun onAnimationRepeat(animation: Animator?) {
                Log.d(TAG, "onAnimationRepeat: Repeated..")
            }

            override fun onAnimationCancel(animation: Animator?) {
                Log.d(TAG, "onAnimationCancel: Canceled..")
            }

            override fun onAnimationEnd(animation: Animator?) {
                Log.d(TAG, "onAnimationEnd: 끝! 이 Frag 닫는다!")
                if(activity!=null) {
                    Snackbar.make(requireActivity().findViewById(android.R.id.content), "DISK SCAN- REBUILDING DATABASE COMPLETED", Snackbar.LENGTH_LONG).show()
                }
                dismiss()
            }
        })
    }



    override fun show(manager: FragmentManager, tag: String?) {
        //super.show(manager, tag)
        if(isAdded) {
            Log.d(TAG, "show: Already Showing Lottie Anim. Return!")
            return
        }
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }


}