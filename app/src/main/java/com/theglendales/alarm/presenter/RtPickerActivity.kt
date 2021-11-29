package com.theglendales.alarm.presenter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.theglendales.alarm.R

private const val TAG="RtPickerActivity"
class RtPickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rt_picker)

    // todo: actionBar 꾸미기. 현재 사용중인 actionBar 스타일로 하려면  AlarmListActivity - mActionBarHandler 등 참고. DetailsFrag 는 또 다름 (쓰레기통 표시)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // null check?
    }
}