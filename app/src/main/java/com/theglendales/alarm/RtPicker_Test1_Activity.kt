package com.theglendales.alarm

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

private const val TAG="RtPicker_Test1_Activity"
class RtPicker_Test1_Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rt_picker_test1)

    // todo: actionBar 꾸미기. 현재 사용중인 actionBar 스타일로 하려면  AlarmListActivity - mActionBarHandler 등 참고. DetailsFrag 는 또 다름 (쓰레기통 표시)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // null check?
    }
}