package com.theglendales.alarm.presenter

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.theglendales.alarm.R
import com.theglendales.alarm.jjadapters.RtPickerAdapter
import com.theglendales.alarm.jjmvvm.JjRtPickerVModel
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjmvvm.util.RtWithAlbumArt
import java.util.ArrayList

// startActivityForResult 참고: https://youtu.be/AD5qt7xoUU8


private const val TAG="RtPickerActivity"

private const val PICKER_RESULT_RT_TITLE="RtTitle"
private const val PICKER_RESULT_AUDIO_PATH="AudioPath"
private const val PICKER_RESULT_ART_PATH="ArtPath"


class RtPickerActivity : AppCompatActivity() {

    //RcView Related
    lateinit var rcvAdapter: RtPickerAdapter
    lateinit var rcView: RecyclerView
    lateinit var layoutManager: LinearLayoutManager
    //


    override fun onCreate(savedInstanceState: Bundle?) {
        val resultIntent = Intent()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rt_picker)

    //1) Activity 화면 Initialize (ActionBar 등..)
        // 화면 위에 뜨는 타이틀
            setTitle("Ringtone Picker")
        // todo: actionBar 꾸미기. 현재 사용중인 actionBar 스타일로 하려면  AlarmListActivity - mActionBarHandler 등 참고. DetailsFrag 는 또 다름 (쓰레기통 표시)
            supportActionBar?.setDisplayHomeAsUpEnabled(true) // null check?

    // 2) XXX


    //3) RcView 셋업-->
        rcView = findViewById<RecyclerView>(R.id.rcV_RtPicker)
        layoutManager = LinearLayoutManager(this)
        rcView.layoutManager = layoutManager


        
    //4)  LIVEDATA -> // 참고로 별도로 Release 해줄 필요 없음. if you are using observe method, LiveData will be automatically cleared in onDestroy state.
        //1) RtPicker ViewModel
        // A)생성(RcvVModel)
        val rtPickerVModel = ViewModelProvider(this).get(JjRtPickerVModel::class.java)

        // B) LiveData Observe - RtPicker 로 User 가 RingTone 을 골랐을 때 -> a)음악 재생 b)Intent 에 현재 RT 로 설정
        rtPickerVModel.selectedRow.observe(this, { rtWithAlbumArt->
            // B-1) 음악 재생

            Log.d(TAG, "onCreate: rtPickerVModel 옵저버!! rtTitle=${rtWithAlbumArt.rtTitle}, \n rtaPath= ${rtWithAlbumArt.audioFilePath}, artPath= ${rtWithAlbumArt.artFilePathStr}")
            // B-2) Intent 에 현재 선택된 RT 의 정보담기  (AlarDetailsFrag.kt 로 연결됨) .. RT 계속 바꿀때마다 Intent.putExtra 하는데 overWrite 되겠지?
            resultIntent.putExtra(PICKER_RESULT_RT_TITLE,rtWithAlbumArt.rtTitle)
            resultIntent.putExtra(PICKER_RESULT_AUDIO_PATH,rtWithAlbumArt.audioFilePath)
            resultIntent.putExtra(PICKER_RESULT_ART_PATH,rtWithAlbumArt.artFilePathStr)
            setResult(RESULT_OK, resultIntent)
        })

    //5) RcVAdapter Init
        rcvAdapter = RtPickerAdapter(ArrayList(), this, rtPickerVModel)
        rcView.adapter = rcvAdapter
        rcView.setHasFixedSize(true)

    //6) RcVAdapter 에 보여줄 List<RtWithAlbumArt> 를 제공 (이미 DiskSearcher 에 로딩되어있으니 특별히 기다릴 필요 없지..)
        val rtOnDiskList:  MutableList<RtWithAlbumArt> = DiskSearcher.finalRtArtPathList
        rcvAdapter.updateRcV(rtOnDiskList)




    // RT 고르기(X) Cancel Btn 눌렀을 때

    }
}