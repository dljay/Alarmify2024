package com.theglendales.alarm.presenter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjadapters.PurchasedItemRcVAdapter
import com.theglendales.alarm.jjadapters.RtPickerAdapter
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import java.text.SimpleDateFormat
import java.util.*

private const val TAG="PurchasedItemsActivity"
class PurchasedItemsActivity : AppCompatActivity() {
    //ToolBar (ActionBar 대신하여 모든 Activity 에 만들어주는 중.)
    private lateinit var toolBar: Toolbar
    private val mySharedPrefManager: MySharedPrefManager by globalInject()

    //RcView Init Related
    lateinit var rcvAdapter: PurchasedItemRcVAdapter
    lateinit var rcView: RecyclerView
    lateinit var layoutManager: LinearLayoutManager

    //UIs


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchased_items)

    //1) Activity 화면 Initialize (ActionBar 등..)
        // 화면 위에 뜨는 타이틀
        toolBar = findViewById(R.id.id_toolBar_PurchasedItems)
        setSupportActionBar(toolBar)
        //toolBar.title = "Ringtone Picker" // 이미 .xml 에서 해줌.
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기(<-) 표시. null check?

    //2) Shared Pref 에서 a) 현재 RcV 에 보여지는 모든 List 받고-> b) 그 중 purchaseBool=true 인것만 걸러서 c) RcV 로 List 전달.
        val entireRtInTheCloudList = mySharedPrefManager.getRtInTheCloudList() // 현재 Firebase 에 등록된 모든 Ringtone 들
        val purchaseBoolTrueList = entireRtInTheCloudList.filter { rtInTheCloud -> rtInTheCloud.purchaseBool } // 그 중에서 purchaseBool=true 인 놈만 받기.
        Log.d(TAG, "onCreate: purchaseBoolTrueList=$purchaseBoolTrueList") //todo:  [*Purchase Date], [Art URL], [RT_NAME], [ORDER_ID],  [PRICE]


    //3) RcView 셋업
        rcView = findViewById(R.id.purch_items_rcView)
        layoutManager = LinearLayoutManager(this)
        rcView.layoutManager = layoutManager

    //4) RcV Adapter Init
        rcvAdapter = PurchasedItemRcVAdapter(this, purchaseBoolTrueList)
        rcView.adapter = rcvAdapter
        rcView.setHasFixedSize(true)


    }

}