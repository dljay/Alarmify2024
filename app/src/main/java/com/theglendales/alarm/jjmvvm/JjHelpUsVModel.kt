package com.theglendales.alarm.jjmvvm

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.SkuDetails
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.iapAndDnldManager.MyIAPHelperV3
import com.theglendales.alarm.jjmvvm.util.JjPlayStoreUnAvailableException
import com.theglendales.alarm.jjmvvm.util.JjServiceUnAvailableException
import com.theglendales.alarm.jjmvvm.util.MyStringStorage
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import com.theglendales.alarm.model.mySharedPrefManager
import kotlinx.coroutines.*

private const val TAG="JjHelpUsVModel"

class JjHelpUsVModel : ViewModel() {
//todo: String 영어 -> .. 번역..
//UTIL - String Storage & Toast Deliverer
    private val strStorage: MyStringStorage by globalInject()
    private val toastMessenger: ToastMessenger by globalInject() //ToastMessenger
    private var finalList: List<RtInTheCloud> = ArrayList()
// Price 받기 (Donation 이지만 사실상 MyIAPHelperV3.kt 에서 RtCloud 리스트받고 결제하는것과 동일!!)
    private val _rtListPlusPricesLiveData = MutableLiveData<List<RtInTheCloud>>()
    val rtListPlusPricesLiveData: LiveData<List<RtInTheCloud>> = _rtListPlusPricesLiveData



//IAP
    private val iapV3: MyIAPHelperV3 by globalInject()

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
            iapV3.b_feedRtList(getIapNameDummyList())
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
                        finalList = listSavedOnPhone
                        _rtListPlusPricesLiveData.value = finalList
                        return@invokeOnCompletion
                    }
                }
            } else { // 에러 없으면

                val rtListPlusPriceInfo = iapV3.e_getFinalList()
                Log.d(TAG, "getAllProductsPrice: Called. rtListPlusPriceInfo=$rtListPlusPriceInfo")
                finalList = rtListPlusPriceInfo
                _rtListPlusPricesLiveData.value = finalList
            }
        }
        // 최종적으로 LiveData 에 전달!

    }

    //2) Chip 을 클릭했을 때 결제 처리.
    fun onDonationBtnClicked(receivedActivity: Activity, rtObj: RtInTheCloud) {
        Log.d(TAG, "onDonationBtnClicked: Clicked. rtobj.iapName= ${rtObj.iapName}")

        val handler = CoroutineExceptionHandler { _, _ ->} // CoroutineExceptionHandler
        val donationClickJob = viewModelScope.launch(handler) {
        //a) Trigger Loading Circle
            triggerPurchaseLoadingCircle(0)

        //b) SkuDetailsList 받기.
            val donationIapNameAsList: List<QueryProductDetailsParams.Product> = listOf(rtObj.iapName) as List<QueryProductDetailsParams.Product>  // iap 이름을 String List 로 만들어서 ->
            val skuDetailsList: List<ProductDetails> = iapV3.h_getSkuDetails(donationIapNameAsList) // skuDetailsList 대충 이렇게 생김: [SkuDetails: {"productId":"p1002","type":"inapp","title":"p1002 name (Glendale Alarmify IAP Test)","name":"p1002 name","price":"₩2,000","price_amount_micros":2000000000,"price_currency_code":"KRW","description":"p1002 Desc","skuDetailsToken":"AEuhp4JNNfXu9iUBBdo26Rk-au0JBzRSWLYD63F77PIa1VxyOeVGMjKCFyrrFvITC2M="}]
        //c) 구매창 보여주기 + User 가 구매한 결과(Yes or No - purchaseResult) 받기
            triggerPurchaseLoadingCircle(2)
            val donationPurchaseResult: Purchase = iapV3.i_launchBillingFlow(receivedActivity, skuDetailsList)
        //d) Verify ->
            iapV3.j_checkVerification(donationPurchaseResult) // 문제 있으면 여기서 알아서 throw exception 던질것임. 결과 확인 따로 안해줌.
        //e) [구매인정!] Consume 만 해주고 Acknowledge 는 안해줬는데 잘되는듯.. 혹 둘 다 해줘야될지 차후 확인 필요 !!!! [기존 JjMainViewModel] 은 non-consumable 여서 Acknowledge 만 해줬음.
            val isDonationAllCompleted = iapV3.k_consumePurchase(donationPurchaseResult)
        //todo: f) Acknowledge 까지 해줄지. 동시에 Acknowledge 놓칠경우 잡아줄지도 확인 필요.
        }
        donationClickJob.invokeOnCompletion { throwable ->
            triggerPurchaseLoadingCircle(1)
            if (throwable != null && !throwable.message.isNullOrEmpty()) {
                when {
                    throwable.message!!.contains("USER_CANCELED") -> { // 구매창 바깥 눌러서 User 가 Cancel 한 경우 Toast 메시지나 기타 아무것도 안 보여주기.
                        return@invokeOnCompletion
                    }
                    throwable is JjServiceUnAvailableException -> {
                        toastMessenger.showMyToast("Error: Service Unavailable Error. Please check your internet connectivity.", isShort = false)
                    }
                    throwable is CancellationException -> { //- Donation Click-> app background -> 복귀 후 뒤로가기(<-) 클릭 -> Job Cancel 된다. 이때 JobCancellationException 뜬다 (Toast 없이 logd 로만 표시)
                        Log.d(TAG, "onDonationBtnClicked: job Cancellation Exception.. =_=")
                    }
                    else -> {
                        Log.d(TAG,"onTrackClicked: [donationClickJob-invokeOnCompletion(X)] - Error. throwable=$throwable ")
                        toastMessenger.showMyToast("Error: $throwable", isShort = false)
                    }
                }
            }else {
                toastMessenger.showMyToast("We sincerely appreciate your help!",isShort = true)
            }
        }
    }
    //3) Chip 의 Tag 와 일치하는 IAP Name 을 갖고 있는 rtObj 을 반환.
    fun getRtObjectViaChipTag(chipTag: String): RtInTheCloud? {
        val rtList = finalList
        try{
            val rtObj = rtList.single{ rtObj -> rtObj.iapName == chipTag} //todo: IAP init 전에 클릭 테스트
            return rtObj
        }catch (e:Exception) {
            Log.d(TAG, "getRtObjectViaChipTag: exception= $e")
            return null
        }
    }
    //4) Loading Circle 관련
    private val _donationClickLoadingCircleSwitch = MutableLiveData<Int>() // Purchase 클릭 -> 구매창 뜨기전 나올 LoadingCircle
    val donationClickLoadingCircleSwitch: LiveData<Int> = _donationClickLoadingCircleSwitch
    fun triggerPurchaseLoadingCircle(onOffNumber: Int) {_donationClickLoadingCircleSwitch.value = onOffNumber} // 0: 보여주기, 1: 아예 다 끄기, 2: 어두운 화면 그대로 두고 Circle 만 안보이게 없애기

    //5) <중요!> 여기서 DummyList 에 넣어준 iapName 을 기반으로 IAP -> PlayConsole 에 등록된 상품의 iapName 정보를 갖고 옴 -> 이후 가격 표시가 되는 원리.
    private fun getIapNameDummyList(): MutableList<RtInTheCloud> {
        // Dummy List<RtInTheCloud>
        val rtDonationP1 = RtInTheCloud(iapName = strStorage.getStringYo("donationP1"),itemPrice = "$0.00") // res/@string 에 있는데 context 가 있어야 R.string 접근가능해서..strStorage 사용키로.
        val rtDonationP2 = RtInTheCloud(iapName = strStorage.getStringYo("donationP2"), itemPrice = "$0.00")
        val rtDonationP3 = RtInTheCloud(iapName = strStorage.getStringYo("donationP3"), itemPrice = "$0.00")
        val rtDonationP4 = RtInTheCloud(iapName = strStorage.getStringYo("donationP4"), itemPrice = "$0.00")

        return mutableListOf(rtDonationP1, rtDonationP2, rtDonationP3, rtDonationP4)
    }
    //6) 현재 진행중인 Job 을 보여주는 function
    /*fun showCurrentJob() {
        Log.d(TAG, "showCurrentJob: currentJob=${viewModelScope.coroutineContext.job} ")
    }*/

}