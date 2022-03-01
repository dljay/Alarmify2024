package com.theglendales.alarm.presenter

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Observer
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.JjHelpUsVModel
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import kotlinx.coroutines.launch

private const val TAG="HelpOurTeamActivity"
/*Chips
0.99 Buy us a coffee
2.99 Buy us a burger
9.99 Buy us a bottle of wine
19.99 Buy us a programming book  */
// 중복 선택 불가능, IAP 로 가격 표시,
class HelpOurTeamActivity : AppCompatActivity() {

    //ToolBar (ActionBar 대신하여 모든 Activity 에 만들어주는 중.)
    private lateinit var toolBar: Toolbar
    //Toast Messenger
    private val toastMessenger: ToastMessenger by globalInject() //ToastMessenger
    //Chips
    lateinit var chipGroup: ChipGroup
    var myIsChipChecked = false

    //Donate Btn
    lateinit var btnDonate: Button

    // Loading Circle
    lateinit var frameLayoutForCircle: FrameLayout
    lateinit var centerLoadingCircle: CircularProgressIndicator

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

        frameLayoutForCircle = findViewById(R.id.fl_loadingCircle) // Loading Circle 관련
        frameLayoutForCircle.visibility = View.GONE// 일단 LoadingCircle 안보이게 하기.
        centerLoadingCircle = findViewById(R.id.loadingCircle_itself)

    //2) Chip & Donate Btn Listener Setup
        setDonationChipListener()
        setBtnDonateListener()

    //3-A) ViewModel 생성
        jjHelpUsVModel = ViewModelProvider(this)[JjHelpUsVModel::class.java]
    //3-B) IAP 에서 받은 정보로 -- 가격 업뎃 Collect (onResume 다음에)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    jjHelpUsVModel.rtListPlusPricesLiveData.observe(this@HelpOurTeamActivity){ rtList ->
                        Log.d(TAG, "onCreate: rtList=$rtList")
                        displayPriceOnChips(rtList)
                    }
                }

                launch {
                    jjHelpUsVModel.donationClickLoadingCircleSwitch.observe(this@HelpOurTeamActivity) {onOffNumber ->
                        Log.d(TAG, "onCreate: onOffNumber=$onOffNumber")
                        when(onOffNumber){
                            0 -> {frameLayoutForCircle.visibility = View.VISIBLE
                                centerLoadingCircle.visibility = View.VISIBLE} // 보여주기(O)
                            1 -> {frameLayoutForCircle.visibility = View.GONE} // 끄기(X)
                            2 -> {centerLoadingCircle.visibility = View.GONE}// 2 -> circle 만 없애주기 ()
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // onPause 에서 ViewModel 통해 ViewModelScope.job 을 Cancel 해주면. 돌아왔을 때 Donation 버튼 눌러도 계속 JobCancelException 에러 뜬다.
        jjHelpUsVModel.triggerPurchaseLoadingCircle(1) // APP 이 Background 로 갈때는 => 아예 다 꺼주기
        Log.d(TAG, "onPause: called")
    }

    override fun onStop() {
        super.onStop()
        //jjHelpUsVModel.showCurrentJob()
        Log.d(TAG, "onStop: called")

    }
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: called")
        //init IAP?
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d(TAG, "onDestroy: called")
    }

// **** My Methods
    private fun setBtnDonateListener() {
        btnDonate.setOnClickListener {
            Log.d(TAG, "setBtnDonateListener: clicked to Donate!")
            //a) Chip 이 선택 안되어있을 때
            if(chipGroup.checkedChipId == View.NO_ID) {
                snackBarDeliverer(getString(R.string.select_at_least_one),isShort = true)
                return@setOnClickListener
            }
            //b) 현재 선택되어있는 Chip 과 동일한 IAPNAME 을 가진 RtObj 찾기
            val checkedChipId = chipGroup.checkedChipId
            val selectedChip: Chip = when(checkedChipId) {
                R.id.donation_chip_1 -> {findViewById(R.id.donation_chip_1)}
                R.id.donation_chip_2 -> {findViewById(R.id.donation_chip_2)}
                R.id.donation_chip_3 -> {findViewById(R.id.donation_chip_3)}
                R.id.donation_chip_4 -> {findViewById(R.id.donation_chip_4)}
                else -> {findViewById<Chip>(R.id.donation_chip_1)} // Chip 설정 안되었을 때 위에서 SnackBar 보여주니깐 여기로는 절대 들어올 일 없음.
            }
            val rtObjViaChipTag = jjHelpUsVModel.getRtObjectViaChipTag(selectedChip.tag as String)

            //b) 실제 구입과정.
            if(rtObjViaChipTag!=null) { // 사전에 IAP 에서 데이터 가져오는데 문제가 없었다는 가정하에.
                jjHelpUsVModel.onDonationBtnClicked(this, rtObjViaChipTag)
            } else {
                toastMessenger.showMyToast("We're sorry. We cannot process your donation at the moment.",isShort = false)
            }

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
        var exceptionToCatch= Exception("")
        var isErrorDisplayingPrice = false

        for(i in 0 until chipGroup.childCount) {
            val chip: Chip = chipGroup.getChildAt(i) as Chip
            try{
                val donationPrice = rtList.first { rtObj -> rtObj.iapName == chip.tag as String }.itemPrice // .tag 로 검색하여 동일한 Chip 을 찾은 뒤 chip 에 가격 표시
                chip.text = donationPrice
                isErrorDisplayingPrice = false
            }catch (e: Exception) {
                Log.d(TAG, "displayPriceOnChips: Exception .. e=$e")
                isErrorDisplayingPrice = true
                exceptionToCatch = e
                //snackBarDeliverer("Unable to display donation price in local currency. \nError= $e", isShort = false) // GooglePlay Store - Sign In 안됐을때 나오는 Toast 메시지와 겹쳐서..
            }
        }
        if(isErrorDisplayingPrice && !exceptionToCatch.message.isNullOrEmpty()) {
            //todo: 아래 Toast 로 하면 jjHelpVModel > Please sign in to Google Play 와 연달아서 나오니깐 그냥 textView 에 "Unable to display price in local currency" 로 써주도록 하자.
            //toastMessenger.showMyToast(getString(R.string.unable_to_display_price)+ "\nError= $exceptionToCatch",isShort = true) //jjHelpVModel > Please sign in to Google Play 와 연달아서 나옴.
        }

    }
    private fun snackBarDeliverer(msg: String, isShort: Boolean) {
        Log.d(TAG, "snackBarMessenger: Show Snackbar. Message=[$msg]")
        if(isShort) {
            Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show()
        }else {
            Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show()
        }


    }



}