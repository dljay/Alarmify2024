package com.theglendales.alarm.jjmvvm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.iapAndDnldManager.MyIAPHelperV3
import com.theglendales.alarm.jjmvvm.util.JjPlayStoreUnAvailableException
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import com.theglendales.alarm.model.mySharedPrefManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG="JjHelpUsVModel"

class JjHelpUsVModel : ViewModel() {

// Dummy List<RtInTheCloud>
    private val rtDonationA = RtInTheCloud(iapName = "donationIap1", itemPrice = "$4.44")
    private val rtDonationB = RtInTheCloud(iapName = "donationIap2", itemPrice = "$5.55")
    private val rtDummyList = mutableListOf<RtInTheCloud>(rtDonationA, rtDonationB)
// Price 받기 (Donation 이지만 사실상 MyIAPHelperV3.kt 에서 RtCloud 리스트받고 결제하는것과 동일!!)
    private val _rtListPlusPricesLiveData = MutableStateFlow<List<RtInTheCloud>>(rtDummyList)
    val rtListPlusPricesLiveData: StateFlow<List<RtInTheCloud>> = _rtListPlusPricesLiveData.asStateFlow()
//IAP
    private val iapV3: MyIAPHelperV3 by globalInject()
// Toast Deliverer
    private val toastMessenger: ToastMessenger by globalInject() //ToastMessenger
    init {
        // IAP INIT -> Price Update
        getAllProductsPrice()
    }

    //1) 가격을 받고 HelpOurTeamActivity.kt 에 알려서 Chip 에 적힌 가격 update
    private fun getAllProductsPrice() {
        // A) IAP Init
        
        // B) Exception Handler
        val handler = CoroutineExceptionHandler { _, throwable ->
            Log.d(TAG, "getAllProductsPrice: [B] [handler] Exception Thrown! Throwable=$throwable")
            if(throwable !is JjPlayStoreUnAvailableException) { //ResponseCode=3, typically PlayStore 로긴 안되서 생기는 에러가 "아닌 경우에만" toast 보여줌.
                toastMessenger.showMyToast("Error Occurred. Failed to fetch Donation price tag information. Error=$throwable",isShort = false)
            } else { // (throwable is JjPlayStoreUnAvailableException)
                toastMessenger.showMyToast("Please sign in to Google Play Store and try again.",isShort = false)
            }
        }
        // C-1) ViewModelScope.Launch [Runs on the Main Thread]
        val getAllPricesJob = viewModelScope.launch(handler) {
            Log.d(TAG, "getAllProductsPrice: [C] ViewModelScope.launch{}")

            //C-2) RtList (공갈) -> IAP 에 전달 -> 이후 정보 채워서 받을 예정
            iapV3.b_feedRtList(rtDummyList)
            //C-3) Billing Client - Start Connection
            iapV3.c_prepBillingClient()
        //D) get Price and add to the List in IAP
            launch {
                val skuDetailsList= iapV3.d2_A_addPriceToList()
                iapV3.d2_B_addPriceToList(skuDetailsList)
            }
        }
        //E) 위의 viewModelScope.launch{} 코루틴 job 이 끝나면(invokeOnCompletion) => **** 드디어 LiveData 업데이트
        getAllPricesJob.invokeOnCompletion { throwable ->
            //E-1) 에러 발생!
            if(throwable!=null) {
                Log.d(TAG, "getAllProductsPrice(invokeOnCompletion): [E] Error.. throwable=$throwable ")
                when(throwable) {
                    is JjPlayStoreUnAvailableException -> {_rtListPlusPricesLiveData.value = ArrayList() //todo: 한번 더 보기.
                        return@invokeOnCompletion
                    } // 공갈 리스트 보냄.
                    else -> { // 그 외 에러인 경우 (기기에 사전 저장되었던) sharedPref 받아서 -> LiveData 에 전달.
                        val listSavedOnPhone = mySharedPrefManager.getRtInTheCloudList()
                        _rtListPlusPricesLiveData.value = listSavedOnPhone
                        return@invokeOnCompletion
                    }
                }
            } else { // 에러 없으면
                val rtListPlusPriceInfo = iapV3.e_getFinalList()
                _rtListPlusPricesLiveData.value = rtListPlusPriceInfo
            }
        }
        // 최종적으로 LiveData 에 전달!

    }

    //2) Chip 을 클릭했을 때 결제 처리.
    fun onDonationBtnClicked(rtObj: RtInTheCloud) {
        Log.d(TAG, "onDonationBtnClicked: Clicked. rtobj.iapName= ${rtObj.iapName}")
    }
    //3) Chip 의 Tag 와 일치하는 IAP Name 을 갖고 있는 rtObj 을 반환.
    fun getRtObjectViaChipTag(chipTag: String): RtInTheCloud {
        val rtList = _rtListPlusPricesLiveData.value
        val rtObj = rtList.single{ rtObj -> rtObj.iapName == chipTag} //todo: IAP init 전에 클릭 테스트. try/catch?
        return rtObj
    }

}