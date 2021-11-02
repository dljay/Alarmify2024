package com.theglendales.alarm.jjongadd

import android.content.DialogInterface
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.presenter.DynamicThemeHandler
import com.theglendales.alarm.presenter.PickedTime
import com.theglendales.alarm.util.Optional
import com.theglendales.alarm.util.Optional.Companion.absent
import com.theglendales.alarm.util.Optional.Companion.of
import com.theglendales.alarm.view.TimePicker
import io.reactivex.Single
import io.reactivex.SingleEmitter
import java.lang.IllegalStateException
import java.time.LocalTime
import java.util.*

private const val TAG="TimePickerJjong"
private const val EXTRA_TIME = "time"


class TimePickerJjong: DialogFragment() {
    private val dynamicThemeHandler = globalInject(DynamicThemeHandler::class.java).value
    private var mPicker: TimePicker? = null
    private var emitter: SingleEmitter<Optional<PickedTime>>? = null

    //lateinit var localTime: LocalTime



//    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {
//        val v: View = inflater.inflate(R.layout.time_picker_dialog, null)
//        val set = v.findViewById<View>(R.id.set_button) as Button
//        val cancel = v.findViewById<View>(R.id.cancel_button) as Button
//        cancel.setOnClickListener {
//            notifyOnCancelListener()
//            dismiss()
//        }
//        mPicker = v.findViewById<View>(R.id.time_picker) as TimePicker
//        mPicker!!.setSetButton(set)
//        set.setOnClickListener {
//            val picked = PickedTime(mPicker!!.getHours(), mPicker!!.getMinutes())
//            emitter?.onSuccess(of(picked))
//            dismiss()
//        }
//        return v
//    }

//    override fun onCancel(dialog: DialogInterface) {
//        Log.d(TAG, "onCancel: cancel..")
//        notifyOnCancelListener()
//        super.onCancel(dialog!!)
//    }


    private fun notifyOnCancelListener() {
        Log.d(TAG, "notifyOnCancelListener: called")
        if (emitter != null) {
            emitter!!.onSuccess(absent<PickedTime>())
        }
    }

    private fun setEmitter(timePicker: MaterialTimePicker, emitterInput: SingleEmitter<Optional<PickedTime>>?) {
        emitter = emitterInput
    }

    fun showTimePicker(fragManagerReceived: FragmentManager): Single<Optional<PickedTime>> {
    //Material Time Picker Setup A <Basic>
        // 우선 현재 시간 구하기 API < 26 에서도 되는 방법으로..
        val rightNow: Calendar = Calendar.getInstance()
        val hrNow = rightNow.get(Calendar.HOUR) // 12 시간 포맷으로
        val minuteNow = rightNow.get(Calendar.MINUTE) //

        Log.d(TAG, "showTimePicker: entered.. ")
        val clockFormat = TimeFormat.CLOCK_12H // Always 12H format .. 무조건
        val timePickerDFrag = MaterialTimePicker.Builder()
            .setTimeFormat(clockFormat)
            .setHour(hrNow) // 현재 시간으로 설정
            .setMinute(minuteNow)
            .setTitleText("Set Alarm")
            .build()

        //setEmitter(timePickerDFrag, emitter)

        val ft = fragManagerReceived.beginTransaction()
        val prev = fragManagerReceived.findFragmentByTag("time_dialog_jjong")
        if (prev != null) {
            ft.remove(prev)
        }
        timePickerDFrag.show(fragManagerReceived,"time_dialog_jjong")

    //Material Time Picker Setup B <Callback>
        // a) Okay 버튼
        timePickerDFrag.addOnPositiveButtonClickListener {

            val pickedTime: PickedTime = PickedTime(timePickerDFrag.hour, timePickerDFrag.minute)
            if(emitter!=null) {// 아래 Sinigle.create 에서 emitter 가 다음 type 으로 잘 설정되었다면 (type: SingleEmitter<Optional<PickedTime>>)
                emitter!!.onSuccess(of(pickedTime)) // emitter 가 발산하는것을 여기서 capture 하여 -> AlarmListFrag subscribe 쪽으로 PickedTime 타입의 variable 을 전달
                Log.d(TAG, "showTimePicker: Hit okay.  (emitter!=null) Hour=${timePickerDFrag.hour}, Minute= ${timePickerDFrag.minute}")
            }
            //dismiss()
        }
        // b) Cancel 버튼
        timePickerDFrag.addOnNegativeButtonClickListener {
            Log.d(TAG, "showTimePicker: hit cancel. Hour=${timePickerDFrag.hour}, Minute= ${timePickerDFrag.minute}")
            notifyOnCancelListener()
        }
        // c) 다른데 클릭해서 cancel
        timePickerDFrag.addOnCancelListener {
            Log.d(TAG, "showTimePicker: hit outside. Hour=${timePickerDFrag.hour}, Minute= ${timePickerDFrag.minute}")
            notifyOnCancelListener()
        }
        // d) Dismissed
        timePickerDFrag.addOnDismissListener {
            Log.d(TAG, "showTimePicker: Dismissed..Hour=${timePickerDFrag.hour}, Minute= ${timePickerDFrag.minute}")
        }

        //RXJava
        return Single.create { emitter ->

            //val dialogFrag = TimePickerJjong.newInstance()

            try {
                Log.d(TAG, "showTimePicker: try block")
                //timePickerDFrag.show(fragmentManager, "time_dialog_jjong")
                setEmitter(timePickerDFrag, emitter)
                emitter.setCancellable {

                    if (timePickerDFrag.isAdded) {
                        timePickerDFrag.dismiss()
                    }
                }
            } catch (e: IllegalStateException) {
                Log.d(TAG, "showTimePicker: catch block. Error=$e")
                emitter.onSuccess(absent())
            }
        }
    }


}