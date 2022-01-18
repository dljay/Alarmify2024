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
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager
import com.theglendales.alarm.jjmvvm.iapAndDnldManager.*
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
//IAP & DNLD variables
    private val iapV3: MyIAPHelperV3 by globalInject()
    private val singleDownloaderV3: SingleDownloaderV3 by globalInject()
    private val multiDownloaderV3: MultiDownloaderV3 by globalInject()
//FireBase variables
    var isFreshList = false
    private val firebaseRepoInstance: FirebaseRepoClass by globalInject()
    private val _rtInTheCloudList = MutableLiveData<MutableList<RtInTheCloud>>() // Private& Mutable LiveData
    val rtInTheCloudList: LiveData<MutableList<RtInTheCloud>> = _rtInTheCloudList // Public but! Immutable (즉 이놈은 언제나= _liveRtList)

    init {
        Log.d(TAG, "init: called.. ^^ ")
        refreshAndUpdateLiveData()
    }
//********** FB ->rtList -> IAP -> rtListPlusIAPInfo -> LiveData(rtInTheCloudList) -> SecondFrag-> UI 업데이트 : ViewModel 최초 로딩시 & Spinner 로 휘리릭~ 새로고침 할 때 아래 function 이 불림.
    fun refreshAndUpdateLiveData() {
        Log.d(TAG, "refreshAndUpdateLiveData: (0) called")
        firebaseRepoInstance.getPostList().addOnCompleteListener {
            if(it.isSuccessful)
            {
            //1)Fb 에서 RtList를 받아옴
                val rtList = it.result!!.toObjects(RtInTheCloud::class.java)
                Log.d(TAG, "refreshAndUpdateLiveData: (1) Got the list from FB!!")
            //2)RtList 를 -> IAP 에 전달
                //Exception handler -> iapParentJob 에서 문제가 생겼을 때 Exception 을 받고 -> 아래 iapParentJob.invokeOnCompletion 에서 sharedPref 에 있는 데이터를 읽기.

                val handler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
                    Log.d(TAG, "handler: Exception thrown in one of the children: $throwable") // Handler 가 있어야 에러나도 Crash 되지 않는다.
                    toastMessenger.showMyToast("Failed to fetch IAP information. Error=$throwable",isShort = false)
                }
            //** viewModelscope.launch!!!  <<<<<<runs on the Main thread>>>>> !!!!

                val iapParentJob = viewModelScope.launch(handler) {
                    Log.d(TAG, "refreshAndUpdateLiveData: (2) RtList ->IAP")
                //iapV3-B) Fb 에서 받을 리스트를 -> IAP 에 전달 //** Coroutine 안에서는 순차적(Sequential) 으로 모두 진행됨.
                    iapV3.b_feedRtList(rtList)
                //iapV3-C) BillingClient 를 Ready 시킴 (이미 되어있으면 바로 BillingClient.startConnection)
                    val billingResult: BillingResult = iapV3.c_prepBillingClient()
                    if(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                    {
                //iapV3-D) Each .launch{} running on separate thread (동시 실행) //todo: D1&D2 는 같이 시작하지만.. suspendCoroutine() 사용하니깐.. 진정한 의미에서 parallel 이 아님.
                        //D) Parallel Job  - D1
                        launch {
                            val listOfPurchases = iapV3.d1_A_addPurchaseBoolToList() // D1-A ** AsyncCallback 이 있어서 suspendCoroutine->continuation(result)-> d1_b(result)
                            iapV3.d1_B_addPurchaseBoolToList(listOfPurchases)// D1-B
                        }
                        //D) Parallel Job - D2
                        launch {
                            val skuDetailsList = iapV3.d2_A_addPriceToList() // D2-A ** AsyncCallback 이 있어서 suspendCoroutine->continuation(result)-> d2_b(result)
                            iapV3.d2_B_addPriceToList(skuDetailsList)//D2-B
                        }
                    }
                }
            //3) 위의 viewModelScope.launch{} 코루틴 job 이 끝나면(invokeOnCompletion) => **** 드디어 LiveData 업데이트
                iapParentJob.invokeOnCompletion { throwable ->
                    Log.d(TAG, "refreshAndUpdateLiveData: (3) invoke on Completion called")
                // 3-a) 에러 있으면  -> (기기에 저장된) sharedPref 에서 받아서 -> LiveData 전달!
                    if(throwable!=null) {
                        Log.d(TAG, "refreshAndUpdateLiveData: ERROR (3-a) (个_个) iapParentJob Failed: $throwable")
                        val listSavedOnPhone = mySharedPrefManager.getRtInTheCloudList() // get old list From SharedPref (없을땐 그냥 깡통 arrayListOf<RtInTheCloud>() 를 받음.
                        isFreshList = true
                        _rtInTheCloudList.value = listSavedOnPhone // update LiveData -> SecondFrag 에서는 a)Lottie OFF b)RefreshRcV! ---
                        return@invokeOnCompletion
                    }
                //3-b) *** 에러 없으면 '최종 리스트' iapV3-E) iap 정보(price/purchaseBool) 입힌 리스트를 받아서 -> LiveData 전달 + sharedPref 에 저장.
                    else //에러 없으면
                    {
                        val rtListPlusIAPInfo = iapV3.e_getFinalList()
                        isFreshList = true
                        _rtInTheCloudList.value = rtListPlusIAPInfo // update LiveData!! -> SecondFrag 에서는 a)Lottie OFF b)RefreshRcV! ---
                        Log.d(TAG, "refreshAndUpdateLiveData: (3-b) <<<<<<<<<getRtList: updated LiveData!")

            //4) [***후속작업- PARALLEL+ Background TASK**] 이제 리스트 없이 되었으니:  a)sharedPref 에 리스트 저장 b) 삭제 필요한 파일 삭제 c) 멀티 다운로드 필요하면 실행 //
                        // a), b), c) 는 모두 동시 실행(Parallel)

                        viewModelScope.launch(Dispatchers.IO) {
//[Background Thread]   //4-a) SharedPref 에 현재 받은 리스트 저장. (새로운 coroutine)
                            launch {
                                Log.d(TAG, "refreshAndUpdateLiveData: (4-a) saving current RtList+IAPInfo to Shared Pref. Thread=${Thread.currentThread().name}")
                                mySharedPrefManager.saveRtInTheCloudList(rtListPlusIAPInfo)
                            }
//[Background Thread]   //4-b) iapV3-F) Purchase=false 인 리스트를 받음 (purchaseBool= true 를 제외한 리스트의 모든 항목으로 폰에 있는지 여부는 쓰레드가 한가한 여기서 확인 예정!!)
                            launch {
                                Log.d(TAG, "refreshAndUpdateLiveData: (4-b) xxxxx deleting where purchaseBool=false. Thread=${Thread.currentThread().name}")
                                val neverPurchasedList = iapV3.f_getPurchaseFalseRtList()
                                neverPurchasedList.forEach {
                                        rtInTheCloud -> myDiskSearcher.deleteFileByIAPName(rtInTheCloud.iapName)
                                }
                            }
//[Background Thread]   //4-c) [멀티 DNLD] 구입했으나 폰에 없는 RT(s) list 확인-> 다운로드 (새로운 coroutine) (iapV3-G)
                            val multiDnldJob = launch {
                                //Log.d(TAG, "refreshAndUpdateLiveData: (4-c) ↓ ↓ ↓ ↓ Launching multiDnld. Thread=${Thread.currentThread().name} ")
                                val multiDnldNeededList= iapV3.g_getMultiDnldNeededList()
                                if(multiDnldNeededList.size >0) {
                                    Log.d(TAG, "refreshAndUpdateLiveData: (4-c) [멀티] ↓ ↓ ↓ ↓ Launching multiDnld. Thread=${Thread.currentThread().name} ")
                                    multiDownloaderV3.launchMultipleFileDNLD(multiDnldNeededList)
                                }
                            }
/*[Background Thread] */     multiDnldJob.invokeOnCompletion {// 어차피 Throwable 은 MultiDownloaderV3.kt 에서 try/catch 로 잡아줘서 여기까지 전달 안되는 상황 (으로 추측됨..)
                                Log.d(TAG, "refreshAndUpdateLiveData: [멀티DNLD] invokeOnCompletion, thread=${Thread.currentThread().name} ") // thread=DefaultDispatch.. (BackgroundThread)
                                multiDownloaderV3.resetCurrentStateToIdle() // 다 끝났으면 이제 .IDLE 상태로 ENUm 바꿔주기 -> SecondFrag 에서 ListFrag 다녀와서 호출되도 괜찮게끔..
                            }

                        }
                    }
                }

            }else { // 문제는 인터넷이 없어도 이쪽으로 오지 않음. always 위에 if(it.isSuccess) 로 감.
                Log.d(TAG, "<<<<<<<refreshAndUpdateLiveData: ERROR!! (个_个) Exception message: ${it.exception!!.message}")
                //lottieAnimController(1) // this is useless at the moment..
                toastMessenger.showMyToast("Unable to fetch data from host. Please check your connection.", isShort = false)
            }
        }

    }
//*********************Multi Downloader
    fun getLiveDataMultiDownloader(): LiveData<MultiDnldState> {
        val multiState = multiDownloaderV3.getMultiDnldState()
        return multiState
    }
//*******************Network Detector -> LottieAnim 까지 연결
    var prevNT = true
    private val _isNetworkWorking = MutableLiveData<Boolean>() // Private& Mutable LiveData
    val isNetworkWorking: LiveData<Boolean> = _isNetworkWorking // Public but! Immutable (즉 이놈은 언제나= _liveRtList)

    fun updateNTWKStatus(isNetworkOK: Boolean) {
        _isNetworkWorking.postValue(isNetworkOK) // .postValue= backgroundThread 사용->Main 쓰레드에서 반영하게 schedule.. // (이 job 은 발생지가 backgrouond thread 니깐 .value=xx 안되고 postValue() 써야함!)
    }

//*********************** [CLICK] a) 단순 UI 업데이트 (클릭-> SecondFrag 에 RtInTheCloud Obj 전달 -> UI 업뎃 + 복원(ListFrag 다녀왔을 때)
//******************************* b) IAP & Download (Single)

    val emptyRtObj = RtInTheCloud(id = -10) // 그냥 빈 깡통 -10 -> SecondFrag.kt > updateMiniPlayerUiOnClick() 에서 .id <0 -> 암것도 안함.
    private val _selectedRow = MutableStateFlow<RtInTheCloud>(emptyRtObj)
    val selectedRow = _selectedRow.asStateFlow()

    fun onTrackClicked(rtObj: RtInTheCloud, isPurchaseClicked: Boolean) { // todo: Click <-> RCV ViewModel 더 정석으로 찾아서 바꿔보기.
//[A] 단순 음악 재생용 클릭일때 -> LiveData(selectedRow.value) 업뎃 -> SecondFrag 에서 UI 업뎃
        if(!isPurchaseClicked) {
            _selectedRow.value = rtObj
            return
        }
//[B] Purchase 클릭했을때 -> UI 업뎃 필요없고 purchase logic & download 만 실행.
        Log.d(TAG, "onTrackClicked: clicked to purchase..isPurchaseClicked=true")
    //1-a) 구입시도 Purchase Process -> Return RtObj (만약 구입 취소의 경우에는....)
        val rtInTheCloudObj = iapV3.myOnPurchaseClicked(rtObj) // => todo: get RtObj or (이미 구입했거나 뭔가 틀어지면 여기서 quit..)

    //1-b)구입 성공(O) -> 다운로드 준비. 오류 때 Crash 안나게 Handler 사용
            val handler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
                Log.d(TAG, "handler: Exception thrown in one of the children: $throwable") // Handler 가 있어야 에러나도 Crash 되지 않는다.
                //toastMessenger.showMyToast("Failed to Download. Error=$throwable", isShort = false)
            }

    //2) 다운로드 Process
        val dnldParentJob = viewModelScope.launch(handler) {
        //2-a) Background Thread 에서 dnldId 받기: MyDNLDV3.kt> launchDownload -> and get "downloadId:Long" -> 오류 없으면 제대로 된 dnldId 값을 반환하며 이미 다운로드는 시작 중
            launch(Dispatchers.IO) {
                val dnldId: Long = singleDownloaderV3.launchDNLD(rtInTheCloudObj) //Long 값. 여기서 문제가 발생하면 다음 줄로 진행이 안되고 바로 위에 handler 가 잡아서 exception 을 던짐.
        //2-b)  다운중인 dnldId 정보를 전달하여 -> 현재 다운로드 Status 를 계속 LiveModel 로 전달 -> main Thread 에서 UI 업데이트.
                Log.d(TAG, "onTrackClicked: dnldID=$dnldId")
                singleDownloaderV3.watchDnldProgress(dnldId, rtInTheCloudObj) // -> 여기서 myDNLDV3.kt> liveData 들을 자체적으로 업뎃중. SecondFrag 에서는 아래 getDnldStatus() 값을 observe 하기에 -> 자동으로 UI 업뎃.
            }
        }
    //3-c) (2-a)~(2-c) 과정에서 에러가 발생했다면
        dnldParentJob.invokeOnCompletion { throwable->
            if(throwable!=null) {
                Log.d(TAG, "onTrackClicked: [invokeOnCompletion] ERROR!! called. Throwable=$throwable")
                singleDownloaderV3.errorWhileDownloading() // A)SecondFrag 에서 BtmSht 없애주기 (toastMessage 는 위에 Handler 로 자동으로 보여주기)
                singleDownloaderV3.resetLiveDataToInitialState()
            } else {
                Log.d(TAG, "onTrackClicked: dnldParentJob.invokeOnCompletion : No Error! Now Resetting DNLDINFO to initial state")
                singleDownloaderV3.resetLiveDataToInitialState()

            }

        }

        // ********** 여기서부터는 '순차적' 코드가 의미 없음 (위에서 dnldParentJob 을 Main thread 에서 실행시키고 (또 다른 main 스레드?)로 요 밑에줄 써놓으면 바로 concurrent 로 바로 실행됨)
        //Log.d(TAG, "onTrackClicked: this shall be printed. Thread name= ${Thread.currentThread().name}") // 이게 위에 dnldParentJob 보다 먼저 뜨는데 이것도 main 임.. 흐음..한마디로 main 이 블락 안당했다는뜻.
    }
    fun getLiveDataSingleDownloader(): LiveData<DNLDInfoContainer> { //SecondFrag 에서  해당 method (와 이로 인한 결과값을) ** Observe!! ***  하고 있음.
        val dnldInfoObj: LiveData<DNLDInfoContainer> = singleDownloaderV3.getMyDnldLiveData()
        return dnldInfoObj
    }



//***********************
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: called..")
    }
}