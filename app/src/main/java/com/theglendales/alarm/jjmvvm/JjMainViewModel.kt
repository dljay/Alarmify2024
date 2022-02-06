package com.theglendales.alarm.jjmvvm

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.*
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjfirebaserepo.FirebaseRepoClass
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import com.theglendales.alarm.jjmvvm.iapAndDnldManager.*
import com.theglendales.alarm.jjmvvm.mediaplayer.ExoForUrl
import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjmvvm.util.PlayStoreUnAvailableException
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 쫑 한말씀: This ViewModel should follow Activity(AlarmsListActivity)'s life cycle.
 *
 */
private const val TAG="JjMainViewModel"

class JjMainViewModel : ViewModel() {
//Utils
    private val toastMessenger: ToastMessenger by globalInject() //ToastMessenger
    private val mySharedPrefManager: MySharedPrefManager by globalInject() //SharedPref
    private val myDiskSearcher: DiskSearcher by globalInject() // DiskSearcher (PurchaseBool=false 인데 디스크에 있으면 삭제용도)
// MediaPlayer
    private val exoForUrl: ExoForUrl by globalInject()
//IAP & DNLD variables
    private val iapV3: MyIAPHelperV3 by globalInject()
    private val singleDownloaderV3: SingleDownloaderV3 by globalInject()
    private val multiDownloaderV3: MultiDownloaderV3 by globalInject()
//FireBase variables
    private val firebaseRepoInstance: FirebaseRepoClass by globalInject()

    private var unfilteredRtList: List<RtInTheCloud> = ArrayList()
    private val _rtInTheCloudList = MutableLiveData<List<RtInTheCloud>>() // Private& Mutable LiveData
    val rtInTheCloudList: LiveData<List<RtInTheCloud>> = _rtInTheCloudList // Public but! Immutable (즉 이놈은 언제나= _liveRtList)

    init {
        Log.d(TAG, "init: called.. ^^ Thread=${Thread.currentThread().name} ")
        refreshFbAndIAPInfo()
    }
//********** FB ->rtList -> IAP -> rtListPlusIAPInfo -> LiveData(rtInTheCloudList) -> SecondFrag-> UI 업데이트 : ViewModel 최초 로딩시 & Spinner 로 휘리릭~ 새로고침 할 때 아래 function 이 불림.
    fun refreshFbAndIAPInfo() {
        Log.d(TAG, "refreshFbAndIAPInfo: (0) called")
        firebaseRepoInstance.getPostList().addOnCompleteListener {
            if(it.isSuccessful)
            {
            //1)Fb 에서 RtList를 받아옴
                val rtList = it.result!!.toObjects(RtInTheCloud::class.java)
                Log.d(TAG, "refreshFbAndIAPInfo: (1) Got the list from FB!!")
            //2)RtList 를 -> IAP 에 전달
                //Exception handler -> iapParentJob 에서 문제가 생겼을 때 Exception 을 받고 -> 아래 iapParentJob.invokeOnCompletion 에서 sharedPref 에 있는 데이터를 읽기.

                val handler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
                    Log.d(TAG, "handler: Exception thrown in one of the children: $throwable") // Handler 가 있어야 에러나도 Crash 되지 않는다.

                    if(throwable !is PlayStoreUnAvailableException) { // BillingUnAvailable(ResponseCode=3) - (typically) PlayStore 로그인 안되서 생기는 에러가 아니라면 Toast 메시지 보여주기.
                        toastMessenger.showMyToast("Failed to fetch IAP information. Error=$throwable",isShort = false)
                    }
                }
            //** viewModelscope.launch!!!  <<<<<<runs on the Main thread>>>>> !!!!

                val iapParentJob = viewModelScope.launch(handler) {
                    Log.d(TAG, "refreshFbAndIAPInfo: (2) RtList ->IAP // Thread=${Thread.currentThread().name}")

                //iapV3-B) Fb 에서 받은 리스트를 -> IAP 에 전달 //** Coroutine 안에서는 순차적(Sequential) 으로 모두 진행됨.
                    iapV3.b_feedRtList(rtList)
                //iapV3-C) BillingClient - startConnection!
                    iapV3.c_prepBillingClient()

                //iapV3-D) Each .launch{} running on separate thread (동시 실행) //D1&D2 는 같이 시작하지만.. suspendCoroutine() 사용하니깐.. 진정한 의미에서 parallel 이 아님 -> 성능상 거의 차이 없음 그냥 둬!!!
                        //D) Parallel Job  - D1
                        launch {
                            val listOfPurchases = iapV3.d1_A_getAllPurchasedList() // D1-A ** AsyncCallback 이 있어서 suspendCoroutine->continuation(result)-> d1_b(result)
                            iapV3.d1_B_checkUnacknowledged(listOfPurchases) //[D1-B] 기존 구매중 Network 이상 등으로 acknowledge 가 안된 물품들 처리
                            iapV3.d1_C_addPurchaseBoolToList(listOfPurchases)// D1-B => D1-C 되야함.

                        }
                        //D) Parallel Job - D2
                        launch {
                            val skuDetailsList = iapV3.d2_A_addPriceToList() // D2-A ** AsyncCallback 이 있어서 suspendCoroutine->continuation(result)-> d2_b(result)
                            iapV3.d2_B_addPriceToList(skuDetailsList)//D2-B
                        }
                }
            //3) 위의 viewModelScope.launch{} 코루틴 job 이 끝나면(invokeOnCompletion) => **** 드디어 LiveData 업데이트
                iapParentJob.invokeOnCompletion { throwable ->
                    Log.d(TAG, "refreshFbAndIAPInfo: (3) invoke on Completion called")
                // 3-a) 에러 발생!
                    if(throwable!=null) {
                        Log.d(TAG, "refreshFbAndIAPInfo: ERROR (3-a) (个_个) iapParentJob Failed: $throwable")

                        when(throwable) {
                            // Billing Unavailable (Typically Play Store 로그인 안됐을 때 발생.) -> Alert 창으로 PlayStore 이동하게 만들고. 그냥 빈 깡통 리스트 보여주기.
                            is PlayStoreUnAvailableException -> {
                                //Log.d(TAG, "refreshFbAndIAPInfo: PlayStore 안될때: (typically) Play Store 로그인 안되어있는 경우 발생")
                                _rtInTheCloudList.value = ArrayList() // 빈깡통 Return -> SecondFrag 에서 a) Lottie Error 애니메이션 띄우고 B) Alert 창 -> PlayStore 이동
                                return@invokeOnCompletion
                            }
                            // 그 외 에러인 경우 (기기에 저장된) sharedPref 에서 받아서 -> LiveData 전달!
                            else -> {
                                val listSavedOnPhone = mySharedPrefManager.getRtInTheCloudList() // get old list From SharedPref (없을땐 그냥 깡통 arrayListOf<RtInTheCloud>() 를 받음.
                                unfilteredRtList = listSavedOnPhone
                                _rtInTheCloudList.value = listSavedOnPhone // update LiveData -> SecondFrag 에서는 a)Lottie OFF b)RefreshRcV! ---
                                return@invokeOnCompletion
                            }
                        }

                    }
                //3-b) *** 에러 없으면 '최종 리스트' iapV3-E) iap 정보(price/purchaseBool) 입힌 리스트를 받아서 -> LiveData 전달 + sharedPref 에 저장.
                    else //에러 없으면
                    {
                        val rtListPlusIAPInfo = iapV3.e_getFinalList() // gets immutable List!
                        unfilteredRtList = rtListPlusIAPInfo // 가장 최신의 List 를 variable 에 저장 (추후 Chip 관련 SecondFrag 활용)
                        _rtInTheCloudList.value = rtListPlusIAPInfo // !!! update LiveData!! -> SecondFrag 에서는 a)Lottie OFF b)RefreshRcV! ---
                        Log.d(TAG, "refreshFbAndIAPInfo: (3-b) <<<<<<<<<getRtList: updated LiveData!")

            //4) [***후속작업- PARALLEL+ Background TASK**] 이제 리스트 없이 되었으니:  a)sharedPref 에 리스트 저장 b) 삭제 필요한 파일 삭제 c) 멀티 다운로드 필요하면 실행 //
                        // a), b), c) 는 모두 동시 실행(Parallel)

                        viewModelScope.launch(Dispatchers.IO) {
//[Background Thread]   //4-a) SharedPref 에 현재 받은 리스트 저장. (새로운 coroutine)
                            launch {
                                Log.d(TAG, "refreshFbAndIAPInfo: (4-a) saving current RtList+IAPInfo to Shared Pref. Thread=${Thread.currentThread().name}")
                                mySharedPrefManager.saveRtInTheCloudList(rtListPlusIAPInfo)
                            }
//[Background Thread]   //4-b) iapV3-F) Purchase=false 인 리스트를 받음 (purchaseBool= true 를 제외한 리스트의 모든 항목으로 폰에 있는지 여부는 쓰레드가 한가한 여기서 확인 예정!!)
                            launch {
                                Log.d(TAG, "refreshFbAndIAPInfo: (4-b) xxxxx deleting where purchaseBool=false. Thread=${Thread.currentThread().name}")
                                val neverPurchasedList = iapV3.f_getPurchaseFalseRtList()
                                neverPurchasedList.forEach {
                                        rtInTheCloud -> myDiskSearcher.deleteFileByIAPName(rtInTheCloud.iapName)
                                }
                            }
//[Background Thread]   //4-c) [멀티 DNLD] 구입했으나 폰에 없는 RT(s) list 확인-> 다운로드 (새로운 coroutine) (iapV3-G)
                            launch {
                                val multiDnldNeededList= iapV3.g_getMultiDnldNeededList()
                                if(multiDnldNeededList.size >0) {
                                    Log.d(TAG, "refreshFbAndIAPInfo: (4-c-1) [멀티] ↓ ↓ ↓ ↓ Launching multiDnld. Thread=${Thread.currentThread().name} ")
                                    val resultEnum: MultiDnldState = multiDownloaderV3.launchMultipleFileDNLD(multiDnldNeededList)
                                    // 여기서 Coroutine 이 기다려줌.
/*[Main Thread 로 전환]*/                withContext(Dispatchers.Main) {
                                        Log.d(TAG, "refreshFbAndIAPInfo: (4-c-2)")
                                        multiDownloaderV3.updateLiveDataEnum(resultEnum) // 이제 .ERROR 든 .SUCCESSFUL 이든 SecondFrag 에 보고 -> SnackBar 출력 목표는 달성했으니
                                        multiDownloaderV3.resetCurrentStateToIdle() // .IDLE 로 상태 변경->LiveData 에 전달 -> 추후 ListFrag 등 갔다와도 복원 지랄 안나게.
                                        Log.d(TAG, "refreshFbAndIAPInfo: (4-c-3)")
                                    }
                                }
                            }

                        }
                    }
                }

            }else { // 문제는 인터넷이 없어도 이쪽으로 오지 않음. always 위에 if(it.isSuccess) 로 감.
                Log.d(TAG, "<<<<<<<refreshFbAndIAPInfo: ERROR!! (个_个) Exception message: ${it.exception!!.message}")
                //lottieAnimController(1) // this is useless at the moment..
                toastMessenger.showMyToast("Unable to fetch data from host. Please check your connection.", isShort = false)
            }
        }

    }
    /*fun getBillingDisconnectedAlert(): MutableSharedFlow<Int> { // SecondFrag 에서 Observe 중
        return iapV3._billingDisconnectAlert
    }*/


//*********************Multi Downloader
    fun getLiveDataMultiDownloader(): LiveData<MultiDnldState> { // SecondFrag 에서 Observe 중
        return multiDownloaderV3.getMultiDnldState()
    }
//*******************Network Detector -> LottieAnim 까지 연결
    var prevNT = true
    private val _isNetworkWorking = MutableLiveData<Boolean>() // Private& Mutable LiveData
    val isNetworkWorking: LiveData<Boolean> = _isNetworkWorking // Public but! Immutable (즉 이놈은 언제나= _liveRtList)

    fun updateNTWKStatus(isNetworkOK: Boolean) {
        _isNetworkWorking.postValue(isNetworkOK) // .postValue= backgroundThread 사용->Main 쓰레드에서 반영하게 schedule.. // (이 job 은 발생지가 backgrouond thread 니깐 .value=xx 안되고 postValue() 써야함!)
    }

//*********************** [CLICK] a) 단순 UI 업데이트 (클릭-> SecondFrag 에 RtInTheCloud Obj 전달 -> UI 업뎃 + 복원(ListFrag 다녀왔을 때)
//************************[CLICK] b) IAP & Download (Single)

    val emptyRtObj = RtInTheCloud(id = -10) // 그냥 빈 깡통 -10 -> SecondFrag.kt > updateMiniPlayerUiOnClick() 에서 .id <0 -> 암것도 안함.
    private val _selectedRow = MutableStateFlow<RtInTheCloud>(emptyRtObj)
    val selectedRow = _selectedRow.asStateFlow()

    private val _purchaseCircle = MutableLiveData<Int>() // Purchase 클릭 -> 구매창 뜨기전 나올 LoadingCircle
    val purchaseCircle: LiveData<Int> = _purchaseCircle

    fun onTrackClicked(rtObj: RtInTheCloud, isPurchaseClicked: Boolean, receivedActivity: Activity) { // todo: Click <-> RCV ViewModel 더 정석으로 찾아서 바꿔보기 + Activity 에 대한 고민..
//[A] 단순 음악 재생용 클릭일때 -> LiveData(selectedRow.value) 업뎃 -> SecondFrag 에서 UI 업뎃
        if(!isPurchaseClicked) {
            _selectedRow.value = rtObj
            return
        }
//[B] Purchase 클릭했을때 -> UI 업뎃 필요없고 purchase logic & download 만 실행.

    //1) 디스크에 파일이 이미 있으면 -> return // 유저 입장에서는 클릭-> 무반응 (어차피 유저가 보고있는RcV 리스트에 'Purchased' 아이콘이 뜬 상태여서 이게 맞는듯)
        if(myDiskSearcher.isSameFileOnThePhone_RtObj(rtObj)) return //
    //2) 구입시도 Purchase Process -> [Sequential] & 최종적으로 Returns RtObj! (만약 구입 취소의 경우에는....)
/*알파벳은 IAPV3 안 method 를 따라감*/

        // [**SEQUENTIAL**] // 기존 구입 과정을 Coroutine 으로 blocking+순차적 라인으로 보기 쉽게 했음.
        val handler = CoroutineExceptionHandler { _, _ ->} // CoroutineExceptionHandler - 원래 logd 넣어줬으나 그냥 뺴줌.

        val purchaseParentJob = viewModelScope.launch(handler) {

            _purchaseCircle.value = 0 //로딩 Circle 보여주기 -> 보통 구매창 뜨기까지 2초정도 걸림~

        //2-h) Get the list of SkuDetails [SuspendCoroutine 사용] =>
            val iapNameAsList: List<String> = listOf(rtObj.iapName) // iap 이름을 String List 로 만들어서 ->
            val skuDetailsList: List<SkuDetails> = iapV3.h_getSkuDetails(iapNameAsList) // skuDetailsList 대충 이렇게 생김: [SkuDetails: {"productId":"p1002","type":"inapp","title":"p1002 name (Glendale Alarmify IAP Test)","name":"p1002 name","price":"₩2,000","price_amount_micros":2000000000,"price_currency_code":"KRW","description":"p1002 Desc","skuDetailsToken":"AEuhp4JNNfXu9iUBBdo26Rk-au0JBzRSWLYD63F77PIa1VxyOeVGMjKCFyrrFvITC2M="}]
        //2-i) 구매창 보여주기 + User 가 구매한 결과 (Yes or No- purchaseResult) 받기

            _purchaseCircle.value = 2 // 어두운 화면 그대로 두고 Circle 만 안보이게 없애기 (LaunchBilling Flow 에도 Circle 같이 떠서 신경쓰임)
            val purchaseResult: Purchase = iapV3.i_launchBillingFlow(receivedActivity, skuDetailsList)
        //2-j) Verify ->
            iapV3.j_checkVerification(purchaseResult) // 문제 있으면 여기서 알아서 throw exception 던질것임. 결과 확인 따로 안해줌.
        //2-k) [구매인정!] Acknowledge!!
            val isPurchaseAllCompleted = iapV3.k_acknowledgePurchase(purchaseResult, rtObj) // acknowledge 를 여기서 해주고 이제 모든 구입 절차가 끝이 남.
            Log.d(TAG, "onTrackClicked: -----[Acknowledge 부여 O] isPurchaseAllCompleted=$isPurchaseAllCompleted")
        // => 여기서 구매절차는 COMPLETE! => invokeOnCompletion 으로 이동
            _purchaseCircle.value = 1 //로딩 Circle 없애기

        }
        purchaseParentJob.invokeOnCompletion { throwable ->
            _purchaseCircle.value = 1 // 만약 Purchase Loading Circle 이 켜져있었다면 꺼주기 (Handler 에러 잡히든 말든 무조건 꺼!!)
            Log.d(TAG, "onTrackClicked: [purchaseParentJob-invokeOnCompletion] Called..Thread= ${Thread.currentThread().name}")
            if (throwable != null && !throwable.message.isNullOrEmpty()) {
                if (throwable.message!!.contains("USER_CANCELED")) {
                    return@invokeOnCompletion
                } // 구매창 바깥 눌러서 User 가 Cancel 한 경우 Toast 메시지나 기타 아무것도 안 보여주기.
                else {
                    Log.d(TAG,"onTrackClicked: [purchaseParentJob-invokeOnCompletion(X)] - Error. throwable=$throwable ")
                    toastMessenger.showMyToast("Purchase Error: $throwable", isShort = false)
                }
            } else {// 아무 문제없이 구매가 끝이남.
                Log.d(TAG,"onTrackClicked: [purchaseParentJob-invokeOnCompletion(O)] - !!No problemo!!!")
        //3) 구입 끝 -> 신규리스트 전달+ RcV 업뎃!
                val rtListPlusIAPInfo = iapV3.e_getFinalList()
                unfilteredRtList = rtListPlusIAPInfo // 가장 최신의 List 를 variable 에 저장 (추후 Chip 관련 정보- SecondFrag 에서 넘어왔을 떄 활용)

                _rtInTheCloudList.value = rtListPlusIAPInfo // update LiveData!! -> SecondFrag 에서는 a)Lottie OFF b)RefreshRcV! ---

        //4) [***후속작업- PARALLEL+ Background TASK**] 이제 리스트 없이 되었으니:  a)sharedPref 에 리스트 저장 b) 삭제 필요한 파일 삭제 c) 멀티 다운로드 필요하면 실행 //
                // a), b), c) 는 모두 동시 실행(Parallel)
                viewModelScope.launch(Dispatchers.IO) {
//[Background Thread]   //4-a) SharedPref 에 현재 받은 리스트 저장. (새로운 coroutine)
                    launch {
                        Log.d(TAG,"onTrackClicked: (4-a) saving current RtList+IAPInfo to Shared Pref. Thread=${Thread.currentThread().name}")
                        mySharedPrefManager.saveRtInTheCloudList(rtListPlusIAPInfo)
                    }
//[Background Thread]   //4-b) iapV3-F) Purchase=false 인 리스트를 받음 (purchaseBool= true 를 제외한 리스트의 모든 항목으로 폰에 있는지 여부는 쓰레드가 한가한 여기서 확인 예정!!)
                    launch {
                        Log.d(TAG,"onTrackClicked: (4-b) xxxxx deleting where purchaseBool=false. Thread=${Thread.currentThread().name}")
                        val neverPurchasedList = iapV3.f_getPurchaseFalseRtList()
                        neverPurchasedList.forEach { rtInTheCloud ->
                            myDiskSearcher.deleteFileByIAPName(rtInTheCloud.iapName)
                        }
                    }
                }// end of Dispatcher.IO
                Log.d(TAG, "onTrackClicked: [purchaseParentJob-invokeOnCompletion] run download..Thread= ${Thread.currentThread().name}")
                downloadPurchased(rtObj)//todo: run download -진짜
            }
        }// end of invokeOnCompletion.
        Log.d(TAG, "onTrackClicked: [outside-purchaseParentJob] 위 코루틴과 상관없이 빨리 불림..Thread=${Thread.currentThread().name}")
    }

    //fun getPurchaseState(): LiveData<MyPurchResultENUM> = iapV3.getPurchStateLiveData()

    //3) 다운로드 Process
    fun downloadPurchased(rtInTheCloudObj: RtInTheCloud) {
        Log.d(TAG, "downloadPurchased: called. Thread=${Thread.currentThread().name}")

        val handler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.d(TAG, "downloadPurchased: Exception thrown in one of the children: $throwable") // Handler 가 있어야 에러나도 Crash 되지 않는다.
            //toastMessenger.showMyToast("Failed to Download. Error=$throwable", isShort = false)
        }
        val dnldParentJob = viewModelScope.launch(handler) {
        //3-a) Background Thread 에서 dnldId 받기: MyDNLDV3.kt> launchDownload -> and get "downloadId:Long" -> 오류 없으면 제대로 된 dnldId 값을 반환하며 이미 다운로드는 시작 중
            launch(Dispatchers.IO) {
                // Log.d(TAG, "onTrackClicked: // 로딩 Circle [4] X") <- 로딩 Circle 보여줄 필요 없음 (실제 0.05초 사이에 DNLD 창이 뜬다.)
                val dnldId: Long = singleDownloaderV3.launchDNLD(rtInTheCloudObj) //Long 값. 실행과 동시에 Download 창 보여줌
        //3-b)  다운중인 dnldId 정보를 전달하여 -> 현재 다운로드 Status 를 계속 LiveModel 로 전달 -> main Thread 에서 UI 업데이트.
                Log.d(TAG, "downloadPurchased: dnldID=$dnldId")
                // -> 여기서 myDNLDV3.kt> liveData 들을 자체적으로 업뎃중. SecondFrag 에서는 아래 getLiveDataSingleDownloader() 값을 observe 하기에 -> 자동으로 UI 업뎃.
                singleDownloaderV3.watchDnldProgress(dnldId, rtInTheCloudObj)

            }
        }
        //3-c) (3-a)~(3-c) 과정에서 에러가 발생했다면
        dnldParentJob.invokeOnCompletion { throwable->
            if(throwable!=null) {
                //**Main Thread**
                viewModelScope.launch {
                    Log.d(TAG, "downloadPurchased: [invokeOnCompletion] ERROR!! called. Thread=${Thread.currentThread().name}, Throwable=$throwable") //Main Thread
                    singleDownloaderV3.errorWhileDownloading() // A)SecondFrag 에서 BtmSht 없애주기
                    singleDownloaderV3.resetLiveDataToInitialState() // listFrag 돌아갔다와서 개지랄 안나게.
                }
            } else {
                Log.d(TAG, "downloadPurchased: dnldParentJob.invokeOnCompletion : No Error! Now Resetting DNLDINFO to initial state. Thread=${Thread.currentThread().name}")
                viewModelScope.launch {
                    singleDownloaderV3.resetLiveDataToInitialState()
                }
            }

        }
    }
    fun getLiveDataSingleDownloader(): LiveData<DNLDInfoContainer> { //SecondFrag 에서  해당 method (와 이로 인한 결과값을) ** Observe!! ***  하고 있음.
        val dnldInfoObj: LiveData<DNLDInfoContainer> = singleDownloaderV3.getMyDnldLiveData()
        return dnldInfoObj
    }
//    fun testDiffutil() { // 클릭한 놈의 purchaseBool=true 로 바꿔서 이게 바로 화면에 반영되는지 확인 (구매시 바로 icon 바뀌는 기능 확인 위해)
//        val modifiedList = iapV3.modifiyListAndGetList()
//        _rtInTheCloudList.value = modifiedList // SecondFrag 에서 RcV 화면 갱신!
//    }


//*******************Media Player LiveData Observe 관련
    fun getMpStatusLiveData(): LiveData<StatusMp> = exoForUrl.mpStatus
    fun getSongDurationLiveData(): LiveData<Long> = exoForUrl.songDuration
    fun getCurrentPosLiveData(): LiveData<Long> = exoForUrl.currentPosition


//*********************Utility Methods
    //Chip 관련
    fun getUnfilteredList() = unfilteredRtList
    fun showUnfilteredList() {_rtInTheCloudList.value = unfilteredRtList}
//***********************
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: called..")
    }
}