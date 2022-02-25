package com.theglendales.alarm.presenter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.Toolbar
import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager

private const val TAG="PurchasedItemsActivity"
class PurchasedItemsActivity : AppCompatActivity() {
    //ToolBar (ActionBar 대신하여 모든 Activity 에 만들어주는 중.)
    private lateinit var toolBar: Toolbar
    private val mySharedPrefManager: MySharedPrefManager by globalInject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchased_items)

    //1) Activity 화면 Initialize (ActionBar 등..)
        // 화면 위에 뜨는 타이틀
        toolBar = findViewById(R.id.id_toolBar_PurchasedItems)
        setSupportActionBar(toolBar)
        //toolBar.title = "Ringtone Picker" // 이미 .xml 에서 해줌.
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기(<-) 표시. null check?

        // todo: SharedPref 에 savePurchasedItemsHistory() 넣기? iap 에서 진행하면서 저장..
        val entireList = mySharedPrefManager.getRtInTheCloudList() // 현재 Firebase 에 등록된 모든 Ringtone 들
        Log.d(TAG, "onCreate: entireList=$entireList")
        val purchaseBoolTrueList = entireList.filter { rtInTheCloud -> rtInTheCloud.purchaseBool }
        Log.d(TAG, "onCreate: purchaseBoolTrueList=  $purchaseBoolTrueList") //todo: [RT_NAME], [*Purchase Date], [*Transaction ID], [*Price], [Art 경로]


    }
}