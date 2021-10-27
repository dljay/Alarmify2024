package com.theglendales.alarm.jjongadd

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.theglendales.alarm.R

// 싱글톤으로..
class LottieDiskScanDialogFrag: DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {
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

    companion object {
        fun newInstanceDialogFrag(): LottieDiskScanDialogFrag {
            return LottieDiskScanDialogFrag()
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        //super.show(manager, tag)
        val ft = manager.beginTransaction()
        ft.add(this, tag)
        ft.commitAllowingStateLoss()
    }
}