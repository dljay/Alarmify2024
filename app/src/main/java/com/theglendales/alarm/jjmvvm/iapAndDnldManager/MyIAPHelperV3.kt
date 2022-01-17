package com.theglendales.alarm.jjmvvm.iapAndDnldManager

import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/********************************************************************************************************************************************************************************************
    a_initBillingClient() => BillingClient() Init 하는 method (현재 사용 안하고 init{} 에서 해주는데 효과없네
    b_FeedRtList() => JJMainViewModel -> Fb에서 받은 rtList ->rtListPlusIAPInfo 에 덧씌워줌.
    c_PrepBillingClient() => billingClient.startConnection -> AsyncCallback (코루틴 잠시 정지) -> billingResult -> JjMainVModel -> d1, d2 (parallel) 호출

    d1 & d2 는 Parallel, 각각의 a&b 는 순차진행(sequential)

    -----d1_A() => billingClient.queryPurchasesAsync -> AsyncCallback(코루틴 잠시 정지) -> d1_b()로 구매내역 전달(listOfPurchases)
    -> d1_B() => (1) Purchase True-> rtListPlusIapInfo Bool 값 변경, (폰에 없으면) 다운로드 받을 리스에 추가 (2)Purchase False-> purchaseFalseRtList 에 추가-> 추후 JJMainVModel 에서 삭제토록 실행

    -----d2_A() => billingCliinet.querySkuDetailsAsync -> AsyncCallback(코루틴 잠시 정지) -> d2_b()로 skuDetailsList 전달
    ->d2_B() => skuDetailsList 에 있는 itemPrice 정보를 ->rtListPlusIapInfo 에 추가

    e_getIapInfoAddedList() => Price+PurchaseBool 정보가 추가된 리스트를 요청차(JjMainVModel) 에게 전달
    f_getPurchaseFalseRtList() => 폰에 있다면 삭제되어야할 리스트를 요청차(JjMainVModel) 에게 전달
    g_getMultiDnldNeededList() => 복원 다운로드 필요한 리스트를 요청차(JjMainVModel) 에게 전달

********************************************************************************************************************************************************************************************/

private const val TAG="MyIAPHelperV3"

class MyIAPHelperV3(val context: Context ) : PurchasesUpdatedListener {

    private val toastMessenger: ToastMessenger by globalInject() // ToastMessenger
//IAP
    var rtListPlusIAPInfo= mutableListOf<RtInTheCloud>()
    private var billingClient: BillingClient? = null
//복원(Multi DNLD) 및 삭제 관련
    var multiDNLDNeededList: MutableList<RtInTheCloud> = ArrayList() // ##### <기존 구매했는데 a) 삭제 후 재설치 b) Pxx.rta 파일 소실 등으로 복원이 필요한 파일들 리스트> [멀티]
    var purchaseFalseRtList: MutableList<RtInTheCloud> = ArrayList() // 현재 PurchaseState=false 이 놈들 -> JjMainViewModel 로 전달-> myDiskSearcher.deleteFromDisk(fileSupposedToBeAt)
    private val myDiskSearcher: DiskSearcher by globalInject()
//PurchaseBool=false 인 놈들 (Disk 에 있다면 삭제 필요) -> JjMainVModel 로 전달.

    init {
        Log.d(TAG, "init MyIapHelperV3 called. ")
        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build() //todo: 밑에 C .isReady 는 항상 false 네..
    }
// ****************** <1> 최초 SecondFrag 로딩 후 (과거 정보) 복원 관련

    fun a_initBillingClient() {
        Log.d(TAG, "a_initBillingClient: <A> Called")
        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build()
        Log.d(TAG, "a_initBillingClient: <A> Finished")
    }

    fun b_feedRtList(rtListFromFb: MutableList<RtInTheCloud>) {
        Log.d(TAG, "b_feedRtList: <B> Called")
        rtListPlusIAPInfo = rtListFromFb}

    suspend fun c_prepBillingClient(): BillingResult {
        Log.d(TAG, "c_prepBillingClient: <C> Called")
        if(!billingClient!!.isReady) {
            Log.d(TAG, "c_prepBillingClient: <C> BillingClient Not Ready(X)! Re init!")
            billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build()
        }
        return suspendCoroutine { continuation -> // suspendCoroutine -> async Callback 등 잠시 대기가 필요할 때 사용 -> 여기서 잠시 기존 코루틴 정지(JjMainVModel)

            billingClient!!.startConnection(object : BillingClientStateListener{
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    Log.d(TAG, "c_prepBillingClient: <C> BillingSetupFinished (O)")

                    continuation.resume(billingResult) // -> continuation 에서 이어서 진행 (원래 코루틴- JjMainVModel> iapParentJob 으로 복귀)
                }
                override fun onBillingServiceDisconnected() {
                    //continuation.resumeWithException(Exception("Error <C> Billing Service Disconnectoed")) :
                // todo: 가만 두면 그냥 끊어져서 exception 하면 -> crash!! -> 차라리 throw? Coroutine Handler 가 잡아주게? viewModelScope 는 계속 이어질테니..
                    //todo:  Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                }
            })
        }
    }

    //<D1> 현재 리스트에 상품별 구매 여부 (true,false) 적어주기.
    suspend fun d1_A_addPurchaseBoolToList(): List<Purchase> {

        Log.d(TAG, "d1_A_addPurchaseBoolToList: <D1-A> called. ThreadName=${Thread.currentThread().name}")
        multiDNLDNeededList.clear()
        purchaseFalseRtList.clear()

        return suspendCoroutine { continuation ->
            billingClient!!.queryPurchasesAsync(BillingClient.SkuType.INAPP) { _, listOfPurchases ->
                Log.d(TAG, "d1_A_addPurchaseBoolToList: <D1-A> called ")
                continuation.resume(listOfPurchases)
            }
        }
    }
    fun d1_B_addPurchaseBoolToList(listOfPurchases: List<Purchase>) {

        if (listOfPurchases.size > 0) // 구매건이 한 개 이상.
        {
            Log.d(TAG, "d1_B_addPurchaseBoolToList: <D1-B> 총 구매 갯수=listPurchs.size=${listOfPurchases.size}")
            //myQryPurchListSize = listOfPurchases.size // 추후 MyDownloader_v1.kt > multiDownloadOrNot() 에서 활용.
        //**** [D1-B-1]: 구매 기록이 있는 모든건에 대해 [(구매유효=PurchaseState.PURCHASED) + (구매했으나 Refund 등으로 PurchaseState.PURCHASED 가 아닌것도 포함))
            for (purchase in listOfPurchases)
            {
                /**
                 * IAP Library 4.0 업뎃 => .sku 가 없어지고 .skus => List<String> 을 반환함. (여러개 살 수 있는 기능이 생겨서)
                 * 우리는 해당 기능 사용 계획이 없으므로 무조건 우리의 .skus list 는 1개여야만 한다! 만약 1개가 아니면 for loop 에서 다음 iteration 으로 이동
                 */
                ///** .indexOfFirst (람다식을 충족하는 '첫번째' 대상의 위치를 반환. 없을때는 -1 반환) */
                val indexOfRtObj: Int =rtListPlusIAPInfo.indexOfFirst { rtObj -> rtObj.iapName == purchase.skus[0] } //조건을 만족시키는 가장 첫 Obj 의 'index' 를 리턴. 없으면 -1 리턴.
                // 우리가 구매한 물품이 현재 rtListPlusIAPInfo 에 없는 물품이면 (ex. p1,p7 등 아예 PlayConsole 카탈로그에서 Deactivate 시킨 물품인 경우) -> 다음 for loop 으로 넘어가기
                if (purchase.quantity != 1 || indexOfRtObj < 0) { // 갯수가 1개 초과 or rtListPlusIAPInfo 리스트에 우리가 찾는 rtObj 이 없는 경우
                    if (purchase.quantity != 1) {
                        Log.d(TAG,"d1_B_addPurchaseBoolToList: <D1-B-1> [IAP] 심각한 문제. Quantity 가 1개가 아님! Quantity=${purchase.quantity}")
                        toastMessenger.showMyToast("More than one IAP sku IDs.", isShort = true)
                    }
                    if (indexOfRtObj < 0) {
                        Log.d(TAG,"d1_B_addPurchaseBoolToList: <D1-B-1> List 에서 현재 purchase.sku(=${purchase.skus[0]}) 에 매칭하는 rtObject 를 찾을 수 없음.")
                    }
                    continue // 다음 for loop 의 iteration (purchase) 로 이동(?)
                }
                // 정상적으로 item 을 리스트에서 찾았다는 가정하에 다음을 진행->
                val rtObject = rtListPlusIAPInfo[indexOfRtObj]
                val trackID = rtObject.id
                val iapName = rtObject.iapName//purchase.skus[0]
                val fileSupposedToBeAt =context.getExternalFilesDir(null)!!.absolutePath + "/.AlarmRingTones" + File.separator + iapName + ".rta" // 구매해서 다운로드 했다면 저장되있을 위치
                Log.d(TAG,"d1_B_addPurchaseBoolToList: trackId=$trackID, purchase.skus[0]=${purchase.skus[0]}, p.skus(list)=${purchase.skus}")

                //purchaseFound.add(trackID) //For items that are found(purchased), add them to purchaseFound
        // **** [D1-B-2] :********************************>>> 구매 확인된 건
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED)
                {
                    Log.d(TAG,"d1_B_addPurchaseBoolToList: <D1-B-2> ☺ PurchaseState is PURCHASED for trackID=$trackID, itemName=$iapName")
                    rtListPlusIAPInfo[indexOfRtObj].purchaseBool =true// [!!Bool 값 변경!!] default 값은 어차피 false ..rtObject 의 purchaseBool 값을 false -> true 로 변경

                    //*******구매는 확인되었으나 item(ex p1001.rta 등) 이 phone 에 없다 (삭제 혹은 재설치?)
                    if (!myDiskSearcher.isSameFileOnThePhone(fileSupposedToBeAt)) {
                        multiDNLDNeededList.add(rtObject)
                        Log.d(TAG, "d1_B_addPurchaseBoolToList: <D1-B-2> [멀티] 복원 다운로드 필요한 리스트에 다음을 추가: $iapName ")
                    }
                }
            // **** D1-B-3: 구매한적이 있으나 뭔가 문제가 생겨서 PurchaseState.Purchased 가 아닐때 여기로 들어옴. 애당초 구입한적이 없는 물품은 여기 뜨지도 않음!
                else {
                    Log.d(TAG,"d1_B_addPurchaseBoolToList: <D1-B-3> iapName=$iapName, trkID=$trackID, 구매 기록은 있으나 (for some reason) PurchaseState.Purchased(X)- Phone 에서 삭제 요청 ")
                    //myDiskSearcher.deleteFromDisk(fileSupposedToBeAt)
                }
            }//end of for loop
        // **** D1-B-4: purchaseBool=false 인 item 들 -> 삭제할 리스트에 추가해줌.
            Log.d(TAG, "d1_B_addPurchaseBoolToList: <D1-B-4> [END of FOR LOOP]")
        }//end of if(listPurchs.size > 0)
        // **** D1-B-5: 애당초 구매건이 하나도 없는 경우
        else {
            Log.d(TAG, "d1_B_addPurchaseBoolToList: <D1-B-5>: 구매건이 하나도 없음! ")
        }
        // **** D1-B-6 모든건에 대해 정리가 끝났으니 purchaseBool 이 false 인 놈들취합
        purchaseFalseRtList = rtListPlusIAPInfo.filter { rtObj -> !rtObj.purchaseBool }.toMutableList()
        Log.d(TAG, "d1_B_addPurchaseBoolToList: <D1-B-6> ************ iap_D1_B_addPurchaseBoolToList() 끝나는지점 *****")
    }

    //<D2> 현재 리스트에 상품별 가격 적어주기 (ex. $1,000)
    suspend fun d2_A_addPriceToList(): List<SkuDetails> {
        Log.d(TAG, "d2_A_addPriceToList: <D2-A> called. ThreadName=${Thread.currentThread().name}")
        val itemNameList = ArrayList<String>()
        rtListPlusIAPInfo.forEach {rtObject -> itemNameList.add(rtObject.iapName)}

        val myParams = SkuDetailsParams.newBuilder()
        myParams.setSkusList(itemNameList).setType(BillingClient.SkuType.INAPP)
        //여기서 잠시 JJMainViewModel 코루틴스코프 정지! (suspend!)
        return suspendCoroutine { continuation ->
            billingClient!!.querySkuDetailsAsync(myParams.build()) {queryResult, skuDetailsList ->
                if(queryResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList!=null)
                {
                    continuation.resume(skuDetailsList)
                } else {
                    Log.d(TAG, "d2_A_addPriceToList: <D2-A> Finished(X) - Error! XXX loading price for items")
                    continuation.resumeWithException(Exception("<D2-A> Error "))
                }

            }
        }

    }
    fun d2_B_addPriceToList(skuDetailsList: List<SkuDetails>) {
        for(skuDetails in skuDetailsList)
        {
            //something better than .single? -> 근데 실제 연산 속도가 개당 .001 초니깐
            rtListPlusIAPInfo.single { rtObj -> rtObj.iapName == skuDetails.sku }.itemPrice = skuDetails.price
            Log.d(TAG,"d2_B_addPriceToList : <D2-B> item title=${skuDetails.title} b)item price= ${skuDetails.price}," +
                    " c)item sku= ${skuDetails.sku}")
        }
        Log.d(TAG, "d2_B_addPriceToList: <D2-B> Finished (O)")

    }
    //<E> 완성 리스트를 전달 -> 라이브데이터 -> SecondFrag -> rcVUpdate
    fun e_getFinalList(): MutableList<RtInTheCloud> {
        Log.d(TAG, "e_getFinalList: <E> called")
        return rtListPlusIAPInfo
    }
    fun f_getPurchaseFalseRtList() = purchaseFalseRtList
    fun g_getMultiDnldNeededList() = multiDNLDNeededList



// ************************************************** <2> 현재 구매 관련
    fun myOnPurchaseClicked(rtObj: RtInTheCloud): RtInTheCloud {
    Log.d(TAG, "myOnPurchaseClicked: clicked to purchase..")
    val testRt= RtInTheCloud("TestDNLD TITLE", mp3URL = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3")
        return testRt // todo: 이런저런 jjMainViewModel 과의 협업을 통해 결국 구입 완료된 RtInTheCloud obj 을 뱉어야함!
    }

    override fun onPurchasesUpdated(p0: BillingResult, p1: MutableList<Purchase>?) {
        //TODO("Not yet implemented")
    }

}