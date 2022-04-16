package com.theglendales.alarm.presenter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjadapters.PurchasedItemRcVAdapter
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import com.theglendales.alarm.jjmvvm.util.LottieAnimHandler
import com.theglendales.alarm.jjmvvm.util.LottieENUM

private const val TAG="PurchasedItemsActivity"
class PurchasedItemsActivity : AppCompatActivity() {
    //ToolBar (ActionBar 대신하여 모든 Activity 에 만들어주는 중.)
    private lateinit var toolBar: Toolbar
    private val mySharedPrefManager: MySharedPrefManager by globalInject()

    //RcView Init Related
    private lateinit var rcvAdapter: PurchasedItemRcVAdapter
    private lateinit var rcView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager

    //Lottie Related
    private lateinit var lottieAnimationView: LottieAnimationView
    private lateinit var lottieAnimHandler: LottieAnimHandler
    private lateinit var tvNoPurchase: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchased_items)

        window.navigationBarColor = ContextCompat.getColor(applicationContext, R.color.jj_bg_color_1)//System NAV BAR (최하단 뒤로가기/Home 버튼 등 구성되어있는) 배경색 설정

    //1) Activity 화면 Initialize (ActionBar 등..)
        // 화면 위에 뜨는 타이틀
        toolBar = findViewById(R.id.id_toolBar_PurchasedItems)
        setSupportActionBar(toolBar)
        //toolBar.title = "Ringtone Picker" // 이미 .xml 에서 해줌.
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기(<-) 표시. null check?

    //2) Shared Pref 에서 a) 현재 RcV 에 보여지는 모든 List 받고-> b) 그 중 purchaseBool=true 인것만 걸러서 c) RcV 로 List 전달.
        val entireList = mySharedPrefManager.getRtInTheCloudList() // 현재 Firebase 에 등록된 모든 Ringtone 들
        val purchaseBoolTrueList = entireList.filter { rtInTheCloud -> rtInTheCloud.purchaseBool } // 그 중에서 purchaseBool=true 인 놈만 받기.
        //val purchaseBoolTrueList = listOf<RtInTheCloud>() // Lottie TEST 위해 : 강제로 list==0 으로..
        Log.d(TAG, "onCreate: purchaseBoolTrueList=$purchaseBoolTrueList") //  [*Purchase Date], [Art URL], [RT_NAME], [ORDER_ID],  [PRICE]

    //3) RcView 셋업
        rcView = findViewById(R.id.purch_items_rcView)
        layoutManager = LinearLayoutManager(this)
        rcView.layoutManager = layoutManager

    //4) RcV Adapter Init & Lottie Init
        rcvAdapter = PurchasedItemRcVAdapter(this)
        rcView.adapter = rcvAdapter
        rcView.setHasFixedSize(true)


    //5) Lottie Anim 관련
        lottieAnimationView = findViewById(R.id.lottie_purchased_item)
        tvNoPurchase = findViewById(R.id.tvNoPurchase)
        lottieAnimHandler = LottieAnimHandler(this, lottieAnimationView)

    //6) Refresh RcV or Show Lottie (구매내역 없음!)

        if(purchaseBoolTrueList.isEmpty()) {
            lottieAnimHandler.animController(LottieENUM.PURCHASED_ITEM_EMPTY)
            tvNoPurchase.visibility = View.VISIBLE
        }
        rcvAdapter.refreshRecyclerView(purchaseBoolTrueList)

    }

    override fun onDestroy() {
        super.onDestroy()
        lottieAnimHandler.animController(LottieENUM.STOP_ALL) // 혹시 모르니..
    }

}