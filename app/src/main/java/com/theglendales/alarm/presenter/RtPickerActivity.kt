package com.theglendales.alarm.presenter

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.theglendales.alarm.R

private const val TAG="RtPickerActivity"
private const val PICKER_RESULT_KEY_YO="result"

class RtPickerActivity : AppCompatActivity() {

    private val btnRtPicked by lazy { findViewById<Button>(R.id.btn_rtPicked) }
    private val btnRtCancel by lazy { findViewById<Button>(R.id.btn_rtPickCanceled) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rt_picker)

    // 화면 위에 뜨는 타이틀
        setTitle("Ringtone Picker")
    // todo: actionBar 꾸미기. 현재 사용중인 actionBar 스타일로 하려면  AlarmListActivity - mActionBarHandler 등 참고. DetailsFrag 는 또 다름 (쓰레기통 표시)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // null check?

    // RT 고르기(O) Btn 눌렀을 때
        btnRtPicked.setOnClickListener {
            val intentToOpenThisActivity = intent
            val resultIntent = Intent()
            resultIntent.putExtra(PICKER_RESULT_KEY_YO,"String Path is This")

            setResult(RESULT_OK, resultIntent)
            finish()
        }

    // RT 고르기(X) Cancel Btn 눌렀을 때

    }
}