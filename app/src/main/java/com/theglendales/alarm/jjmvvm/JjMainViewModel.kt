package com.theglendales.alarm.jjmvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjfirebaserepo.FirebaseRepoClass
import com.theglendales.alarm.jjmvvm.iapAndDnldManager.MyIAPHelperV3
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 쫑 한말씀: This ViewModel should follow Activity(AlarmsListActivity)'s life cycle.
 */
private const val TAG="JjMainViewModel"

class JjMainViewModel : ViewModel() {
//ToastMessenger
    private val toastMessenger: ToastMessenger by globalInject()
//IAP variable
    private val iapV3: MyIAPHelperV3 by globalInject()
//FireBase variables
    var isFreshList = false
    private val firebaseRepoInstance: FirebaseRepoClass by globalInject()
    private val _rtInTheCloudList = MutableLiveData<MutableList<RtInTheCloud>>() // Private& Mutable LiveData
    val rtInTheCloudList: LiveData<MutableList<RtInTheCloud>> = _rtInTheCloudList // Public but! Immutable (즉 이놈은 언제나= _liveRtList)

    init {
        Log.d(TAG, "init: called.. ^^ ")
        refreshAndUpdateLiveData()}
//********** FB ->rtList -> IAP -> rtListPlusIAPInfo -> LiveData(rtInTheCloudList) -> SecondFrag-> UI 업데이트 : ViewModel 최초 로딩시 & Spinner 로 휘리릭~ 새로고침 할 때 아래 function 이 불림.
    fun refreshAndUpdateLiveData() {
        Log.d(TAG, "refreshAndUpdateLiveData: called")
        firebaseRepoInstance.getPostList().addOnCompleteListener {
            if(it.isSuccessful)
            {
            //1)Fb 에서 RtList를 받아옴
                val rtList = it.result!!.toObjects(RtInTheCloud::class.java)

            //2)RtList 를 -> IAP 에 전달
                val iapJob = viewModelScope.launch {
                //** Coroutine 안에서는 순차적(Sequential) 으로 모두 진행됨. Async 걱정 안해도 될듯..?
                //B) Fb 에서 받을 리스트를 -> IAP 에 전달
                    iapV3.iap_B_feedRtList(rtList)
                //C) BillingClient 를 Ready 시킴 (이미 되어있으면 바로 BillingClient.startConnection)
                    val billingResult: BillingResult = iapV3.iap_C_prepBillingClient()
                    if(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                    {//each .launch{} running on separate thread
                        //D) Parallel Job  - D1
                        launch {
                            iapV3.iap_D1_addPriceToList()
                        }
                        //D) Parallel Job - D2
                        launch {
                            iapV3.iap_D2_addPurchaseBoolToList()
                        }
                    }
                }
                //위의 viewModelScope.launch{} 코루틴 job 이 끝나면(invokeOnCompletion) 다음이 불리면서 LiveData 업데이트
                iapJob.invokeOnCompletion {
                //E) IAP 에서 Price, PurchaseBool 을 채워준(+) rtList 를 받아옴.
                    Log.d(TAG, "refreshAndUpdateLiveData: invokeOnCompletion called.")
                    val rtListPlusIAPInfo = iapV3.iap_E_getFinalList()
                    Log.d(TAG, "refreshAndUpdateLiveData: rtListPlusIAPInfo[0].itemPrice=${rtListPlusIAPInfo[0].itemPrice} //purchaseBool= ${rtListPlusIAPInfo[0].purchaseBool}")
            //3) LiveData Update -> SecondFrag 에서는 a)Lottie OFF b)RefreshRcV! ---
                    isFreshList = true
                    _rtInTheCloudList.value = rtListPlusIAPInfo
                    Log.d(TAG, "refreshAndUpdateLiveData: <3> <<<<<<<<<getRtList: successful")
            //4) todo: 해당 List 를 혹시 모르니 sharedPref 에 GSON 으로 저장?
                }

            }else { // 문제는 인터넷이 없어도 이쪽으로 오지 않음. always 위에 if(it.isSuccess) 로 감.
                Log.d(TAG, "<<<<<<<refreshAndUpdateLiveData: ERROR!! Exception message: ${it.exception!!.message}")
                //lottieAnimController(1) // this is useless at the moment..
                toastMessenger.showMyToast("Unable to fetch Data. Please check your connection.", isShort = false)
            }
        }

    }
//*******************Network Detector -> LottieAnim 까지 연결
    var prevNT = true
    private val _isNetworkWorking = MutableLiveData<Boolean>() // Private& Mutable LiveData
    val isNetworkWorking: LiveData<Boolean> = _isNetworkWorking // Public but! Immutable (즉 이놈은 언제나= _liveRtList)

    fun updateNTWKStatus(isNetworkOK: Boolean) {
        _isNetworkWorking.postValue(isNetworkOK) // .postValue= backgroundThread 사용. // (이 job 은 발생지가 backgrouond thread 니깐 .value=xx 안되고 postValue() 써야함!)
    }

//***********************RecyclerView (클릭 -> SecondFrag 에 RtInTheCloud Obj 전달 -> SecondFrag 에서 UI 업뎃 및 복원(ListFrag 다녀왔을 때)
    val emptyRtObj = RtInTheCloud(id = -10) // 그냥 빈 깡통 -10 -> SecondFrag.kt > updateMiniPlayerUiOnClick() 에서 .id <0 -> 암것도 안함.
    private val _selectedRow = MutableStateFlow<RtInTheCloud>(emptyRtObj)
    val selectedRow = _selectedRow.asStateFlow()

    fun updateSelectedRt(rtObj: RtInTheCloud) {_selectedRow.value = rtObj}
//***********************
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: called..")
    }
}