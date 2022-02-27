package com.theglendales.alarm.presenter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewManager
import android.widget.Button
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.theglendales.alarm.R
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.JjHelpUsVModel
import kotlinx.coroutines.launch

private const val TAG="HelpOurTeamActivity"
/*Chips
0.99 Buy us a coffee
4.99
9.99
19.99
99.99*/
// 중복 선택 불가능, IAP 로 가격 표시,
class HelpOurTeamActivity : AppCompatActivity() {

    //ToolBar (ActionBar 대신하여 모든 Activity 에 만들어주는 중.)
    private lateinit var toolBar: Toolbar

    //Chips
    lateinit var chipGroup: ChipGroup
    var myIsChipChecked = false

    //Donate Btn
    lateinit var btnDonate: Button

    //ViewModel
    lateinit var jjHelpUsVModel: JjHelpUsVModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_our_team)

    //1) Activity 화면 및 UI Initialize (ActionBar 등..)
        toolBar = findViewById(R.id.id_toolbar_help_our_team)
        btnDonate = findViewById(R.id.id_btn_donateNow)
        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기(<-) 표시. null check?

    //2) Chip & Donate Btn Listener Setup
        setDonationChipListener()
        setBtnDonateListener()

    //3-A) ViewModel 생성
        jjHelpUsVModel = ViewModelProvider(this)[JjHelpUsVModel::class.java]
    //3-B) IAP 에서 받은 정보로 -- 가격 업뎃 Collect (onResume 다음에)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                jjHelpUsVModel.rtListLiveData.collect{ rtList ->
                    Log.d(TAG, "onCreate: rtList=$rtList")
                    displayPriceOnChips(rtList)
                }
            }

        }
    }



    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: called")
    }

// **** My Methods
    private fun setBtnDonateListener() {
        btnDonate.setOnClickListener {
            Log.d(TAG, "setBtnDonateListener: clicked to Donate!")
            //a) 현재 선택되어있는 Chip 과 동일한 IAPNAME 을 가진 RtObj 찾기
            val selectedChip = chipGroup.checkedChipId //todo: 실제 Chip 을 찾아야함.
            val rtObjViaChipTag = jjHelpUsVModel.getRtObjectViaChipTag(selectedChip.ta)
            //b) 실제 구입과정.
            jjHelpUsVModel.onDonationBtnClicked()
        }
    }
    private fun setDonationChipListener() {
        Log.d(TAG, "setDonationChipListener: called")
        chipGroup = findViewById(R.id.id_chipGroup_HelpOurTeam)
        for (i in 0 until chipGroup.childCount) {
            val chip: Chip = chipGroup.getChildAt(i) as Chip

            //Chip 이 체크/해제될때마다
            chip.setOnCheckedChangeListener { _, isChecked ->
                //a) 아이콘 hide/show
                when (isChecked) {
                    true -> {chip.isChipIconVisible = false
                        Log.d(TAG, "setDonationChipListener: Chip.id=${chip.id}, Chip.tag=${chip.tag}")
                        // donateProcess(chip.id)
                    }
                    false -> {chip.isChipIconVisible = true
                    }
                }

            }
        } //for loop
    }
    private fun displayPriceOnChips(rtList: List<RtInTheCloud>) {
        Log.d(TAG, "displayPriceOnChips: called")
        for(i in 0 until chipGroup.childCount) {
            val chip: Chip = chipGroup.getChildAt(i) as Chip
            val donationPrice = rtList.first { rtObj -> rtObj.iapName == chip.tag }.itemPrice // .tag 로 검색하여 동일한 Chip 을 찾은 뒤 chip 에 가격 표시

            chip.text = donationPrice
        }
    }



}