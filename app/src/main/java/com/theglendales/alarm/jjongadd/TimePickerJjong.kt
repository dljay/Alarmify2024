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

private const val TAG="TimePickerJjong"
private const val EXTRA_TIME = "time"


class TimePickerJjong: DialogFragment() {
    private val dynamicThemeHandler = globalInject(DynamicThemeHandler::class.java).value
    private var mPicker: TimePicker? = null
    private var emitter: SingleEmitter<Optional<PickedTime>>? = null

    lateinit var localTime: LocalTime

    companion object {

    }

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

    override fun onCancel(dialog: DialogInterface) {
        notifyOnCancelListener()
        super.onCancel(dialog!!)
    }


    private fun notifyOnCancelListener() {
        if (emitter != null) {
            emitter!!.onSuccess(absent<PickedTime>())
        }
    }

    private fun setEmitter(timePicker: MaterialTimePicker, emitterInput: SingleEmitter<Optional<PickedTime>>?) {
        emitter = emitterInput
    }

    fun showTimePicker(fragManagerReceived: FragmentManager): Single<Optional<PickedTime>> {
    //Material Time Picker Setup A <Basic>
        Log.d(TAG, "showTimePicker: entered.. ")
        val clockFormat = TimeFormat.CLOCK_12H
        val timePickerDFrag = MaterialTimePicker.Builder()
            .setTimeFormat(clockFormat)
            .setHour(12) // todo: 현재 시간으로 설정
            .setMinute(0)
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
            if(emitter!=null) {
                emitter!!.onSuccess(of(pickedTime))
                Log.d(TAG, "showTimePicker: emitter!=null. Hit okay. Hour=${timePickerDFrag.hour}, Minute= ${timePickerDFrag.minute}")
            }
            //dismiss()
        }
        // b) Cancel 버튼
        timePickerDFrag.addOnNegativeButtonClickListener {
            Log.d(TAG, "showTimePicker: hit cancel. Hour=${timePickerDFrag.hour}, Minute= ${timePickerDFrag.minute}")
        }
        // c) 다른데 클릭해서 cancel
        timePickerDFrag.addOnCancelListener {
            Log.d(TAG, "showTimePicker: hit outside. Hour=${timePickerDFrag.hour}, Minute= ${timePickerDFrag.minute}")
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