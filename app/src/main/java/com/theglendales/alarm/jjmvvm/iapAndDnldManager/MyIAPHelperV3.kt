package com.theglendales.alarm.jjmvvm.iapAndDnldManager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import kotlinx.coroutines.CompletableDeferred
import java.io.File
import java.io.IOException
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

 참고:
1)IAP 의 코루틴화:  https://proandroiddev.com/google-play-billing-library-meets-kotlin-coroutine-c68e10553786
2) 1)에서 Channel 부분은 CompletableDeferred 로 다음 코드 보며 대체 : https://stackoverflow.com/questions/61388646/billingclient-billingclientstatelistener-onbillingsetupfinished-is-called-multip
********************************************************************************************************************************************************************************************/

private const val TAG="MyIAPHelperV3"


class MyIAPHelperV3(val context: Context ) {

    private val toastMessenger: ToastMessenger by globalInject() // ToastMessenger
//IAP
    var rtListPlusIAPInfo= mutableListOf<RtInTheCloud>()
    private var billingClient: BillingClient? = null

//복원(Multi DNLD) 및 삭제 관련
    var multiDNLDNeededList: MutableList<RtInTheCloud> = ArrayList() // ##### <기존 구매했는데 a) 삭제 후 재설치 b) Pxx.rta 파일 소실 등으로 복원이 필요한 파일들 리스트> [멀티]
    var purchaseFalseRtList: MutableList<RtInTheCloud> = ArrayList() // 현재 PurchaseState=false 이 놈들 -> JjMainViewModel 로 전달-> myDiskSearcher.deleteFromDisk(fileSupposedToBeAt)
    private val myDiskSearcher: DiskSearcher by globalInject()

//purchaseMap
    private val purchaseMap = HashMap<String, CompletableDeferred<Purchase>>() // <K,V> = ex) <p1001, PurchaseObject>


    init {
        Log.d(TAG, "init MyIapHelperV3 called. Thread=${Thread.currentThread().name}")

        a_initBillingClient()
    }
// ****************** <0> billingClient Init + Purchase Result Listener (유저 구매창 반응에 따른 Listener) i_launchBillingFlow() 와 연계되서 사용.

    fun a_initBillingClient() { //여기서 billingClient (lateinit) 을 init + Purchase Updated Listener setup!
        Log.d(TAG, "a_initBillingClient: <A> Called")
        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener { bResult, purchasesListReceived ->
            // Purchase Updated Listener! => Network Error, Offline Payment 등 Issue 있을 수 있음. => Finalized(=isAcknowledged) 필요!
            if (bResult.responseCode == BillingClient.BillingResponseCode.OK && purchasesListReceived != null)
            {
                Log.d(TAG, "i-1) PurchaseResult Listener: A- 정상 신규 구매! (파일 확인 후 없으면)다운로드 진행!. Thread=${Thread.currentThread().name}")
            //***** 여러 종류의 물건을 여러개 살 수 있는 IAP4.0 기능 떄문에 다음과 같은 forLoop 과 purchase.skus[0] 이런 수식이 나왔음. 그러나 우리는 오로지 '단일 종류, 1개 구매' 만 가능.
                for(purchase in purchasesListReceived) { // List 에는 딱 한개만 들어있어야 한다! (우리는 1개 이상 구매를 허용하지 않으니깐!)

                    if(purchase.skus.size !=1) { // 절대 여기에 들어와서는 안되지만. 어떤 이유로 1개 이상의 ringtone 갯수가 구입이 되었다면.
                    emptyMapAndThrowExceptionToDeferred("purchase.skus.size != 1")
                    } else {
                        //*** i_launchBillingFlow() 에서 등록된 oldDeferredPurchase 를 이어받고! 여기서 .complete 으로 때린 결과를 -> i_launchBillingFlow() 의 old 가 이어받음. => 즉 old=new! ***
                        val newDeferredPurchase: CompletableDeferred<Purchase>? = purchaseMap[purchase.skus[0]]
                        newDeferredPurchase!!.complete(purchase) // .complete = Completes this Deferred value with a given value.
                    }
                }
            }
            else if (bResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)
            {// RcV 에서 이미 구입한 물건은 사진 아이콘 등으로 Purchased 구분이 되고-> 클릭했을 때 아무 반응 X -> 따라서 여기로 들어올 확률은 거의 없음.
                Log.d(TAG, "i-1) PurchaseResult Listener: B- 이미 있는 물품 구매! [매우 드문 에러: Ex. P1002 구매 클릭-> Google IAP 에서 (이미 구입한) P1001 로 등록되어있는 경우.]  ")
                emptyMapAndThrowExceptionToDeferred("ITEM_ALREADY_OWNED") // 구매창에서 "You already own this Item" 이라고 뜬다. -> 토스트 메시지(X) 할 필요 없음.

                // 혹시라도 어떤 연유로 이미 구입한 물건이 클릭 가능하게되어 재구매-> 이쪽으로 들어오게되도 다운받을 이유는 없다 ( 이미 구입한 물품들은 MultiDnldV3.kt 로 시작과 동시에 복원작업해주니깐)
            } else if (bResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED)
            {
                Log.d(TAG, "i-1) PurchaseResult Listener: C- 구매 취소") // User 가 백그라운드 클릭 등..
                emptyMapAndThrowExceptionToDeferred("USER_CANCELED")
                //toastMessenger.showMyToast("Purchase Canceled", isShort = true)
            } else
            {
                Log.d(TAG, "i-1) PurchaseResult Listener: D -기타 에러.. ")
                emptyMapAndThrowExceptionToDeferred(bResult.debugMessage)

            }
        }.build()
        Log.d(TAG, "a_initBillingClient: <A> Finished")
    }

// ****************** <1> 최초 SecondFrag 로딩 후 (과거 정보) 복원 관련
    fun b_feedRtList(rtListFromFb: MutableList<RtInTheCloud>) {
        Log.d(TAG, "b_feedRtList: <B> Called")
        rtListPlusIAPInfo = rtListFromFb}

    suspend fun c_prepBillingClient() {
        Log.d(TAG, "c_prepBillingClient: <C> Called")
        /*if(!billingClient!!.isReady) */

        return suspendCoroutine { continuation -> // suspendCoroutine -> async Callback 등 잠시 대기가 필요할 때 사용 -> 여기서 잠시 기존 코루틴 정지(JjMainVModel)

            billingClient!!.startConnection(object : BillingClientStateListener{
                override fun onBillingSetupFinished(billingResult: BillingResult) {

                    if(billingResult.responseCode == BillingClient.BillingResponseCode.OK) { // 0 disconnected= -1
                        Log.d(TAG, "c_prepBillingClient: <C> BillingSetupFinished (O)")
                        continuation.resume(Unit) // -> continuation 에서 이어서 진행 (원래 코루틴- JjMainVModel> iapParentJob 으로 복귀)
                    } else {
                        continuation.resumeWithException(Exception("Error on c_prepBillingClient"))
                    }
                }
                override fun onBillingServiceDisconnected() {
                    Log.d(TAG, "onBillingServiceDisconnected: xxxxxxxxxxxxxxxxxxxx 연결되었으나 이제 끊어짐.. called!")
                    toastMessenger.showMyToast("Billing Service Disconnected. Please try again",isShort = false)
                    /**
                     * This will be called when there 'was' a connection -> but it gets lost. 간혹 구매 코드가 서로 콜백 racing 하다 잘못되도 이쪽으로 온다고 함..
                     * 결론적으론 여기서 .startConnection 을 다시 해줘야하는데.
                     * a) 현재 disconnected 받을 테스트 방법이 없음 b) 이 에러가 아직 뜬적이 없음 c)우리는 인터넷이 끊길때 자체 대응책이 있으므로 -> 일단은 보류 & 추후 살펴볼 예정.
                     * 추후 여기 코드를 넣게된다면:  Disconnect -> flow 로 전달 + SecondFrag 에서 jvmodel 에서 모니터 (+timeOut) -> .startConnection 으로
                     * 매우 좋은 참고!!: https://stackoverflow.com/questions/61388646/billingclient-billingclientstatelistener-onbillingsetupfinished-is-called-multip
                     */
                }
            })
        }
    }

    //<D1> 현재 리스트에 상품별 구매 여부 (true,false) 적어주기.
    suspend fun d1_A_getAllPurchasedList(): List<Purchase> {

        Log.d(TAG, "d1_A_addPurchaseBoolToList: <D1-A> called. ThreadName=${Thread.currentThread().name}")
        multiDNLDNeededList.clear()
        purchaseFalseRtList.clear()

        return suspendCoroutine { continuation ->
            billingClient!!.queryPurchasesAsync(BillingClient.SkuType.INAPP) { _, listOfPurchases ->
                Log.d(TAG, "d1_A_addPurchaseBoolToList: <D1-A> called. ")
                continuation.resume(listOfPurchases)
            }
        }
    }
    suspend fun d1_B_checkUnacknowledged(listOfPurchases: List<Purchase>): Unit {
        // 구입 절차 진행중 Connection 문제등으로 acknowledge 가 안된 물품들 여기서  Purchase.PurchaseState.PURCHASED 면 acknowledge 해주기!
        Log.d(TAG, "d1_B_checkUnacknowledged: called. Thread=${Thread.currentThread().name}")

        if (listOfPurchases.isNotEmpty()) // [유효한 모든 구매건. Refund 된 경우 현 리스트에서 사라짐!]
        {
            for (purchase in listOfPurchases){
                val indexOfRtObj: Int =rtListPlusIAPInfo.indexOfFirst { rtObj -> rtObj.iapName == purchase.skus[0] } //조건을 만족시키는 가장 첫 Obj 의 'index' 를 리턴. 없으면 -1 리턴.

                if(indexOfRtObj!=-1 && purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged){
                    Log.d(TAG, "d1_B_checkUnacknowledged: $#%#$%@$#@@%@#%%#% iapName= ${purchase.skus[0]}, 이럴수가!!!! acknowledge 를 부여할 대상이 있따니!!@$# @4# @#%@#%@#%#@")
                    return suspendCoroutine { continuation ->
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                        billingClient!!.acknowledgePurchase(acknowledgePurchaseParams) // [후속] 구매인정 요청!!
                        { billingResult ->
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK){
                                // ############################## 신규 구매 정상적으로 완료 #######################
                                //[구입인정] + purchaseBool 값 변경 반영(true)
                                Log.d(TAG, "d1_B_checkUnacknowledged: [후속] 구매인정 부가 했음!!")
                                rtListPlusIAPInfo[indexOfRtObj].purchaseBool =true// [!!Bool 값 변경!!] default 값은 어차피 false ..rtObject 의 purchaseBool 값을 false -> true 로 변경
                                continuation.resume(Unit) // isPurchaseCompleted =true 로 모든 구입 절차가 끝났음을 알림.
                            } else {
                                //BillingResult 문제로 -> [후속 구입인정 부가 실패 X] + purchaseBool 값 변경 반영(false)
                                rtListPlusIAPInfo[indexOfRtObj].purchaseBool =false// [!!Bool 값 변경!!] default 값은 어차피 false ..혹시 몰라서 넣어줌.
                                continuation.resume(Unit) // 그럼에도 그냥 나머지 IAP 확인 절차 진행 (최악의 경우에 예전 구입인정 못 받은 한 놈 안 걸리는것이니..)
                                Log.d(TAG, "d1_B_checkUnacknowledged: [후속] 구매 인정 부가 실패!!! XXf")
                                //continuation.resumeWithException(Exception("d1_B_checkUnacknowledged 부여중 Error 흐음. billingResult.responseCode= ${billingResult.responseCode} "))
                            }
                        }
                    }
                }
            }
        }

    }

    fun d1_C_addPurchaseBoolToList(listOfPurchases: List<Purchase>) {

        if (listOfPurchases.isNotEmpty()) // 구매건이 한 개 이상. // [살아있는 모든 구매건. Refund 된 경우 현 리스트에서 사라짐!]
        {
            //인도놈은 일단 여기서 handlePurchases(list<Purchase>) 넣었지만 우리는 그냥 refund Play Console 에서 하자마자 -> 바로 purchaesList 에서 빠지고 RCV 에 반영..대박..
            Log.d(TAG, "d1_C_addPurchaseBoolToList: <D1-C> Thread=${Thread.currentThread().name}, 총 구매 갯수=listPurchs.size=${listOfPurchases.size} \n listOfPurchases=$listOfPurchases")
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
                        Log.d(TAG,"d1_C_addPurchaseBoolToList: <D1-C-1> [IAP] 심각한 문제. Quantity 가 1개가 아님! Quantity=${purchase.quantity}")
                        toastMessenger.showMyToast("More than one IAP sku IDs.", isShort = true)
                    }
                    if (indexOfRtObj < 0) {
                        Log.d(TAG,"d1_C_addPurchaseBoolToList: <D1-C-1> List 에서 현재 purchase.sku(=${purchase.skus[0]}) 에 매칭하는 rtObject 를 찾을 수 없음.")
                    }
                    continue // 다음 for loop 의 iteration (purchase) 로 이동(?)
                }
                // 정상적으로 item 을 리스트에서 찾았다는 가정하에 다음을 진행->
                val rtObject = rtListPlusIAPInfo[indexOfRtObj]
                val trackID = rtObject.id
                val iapName = rtObject.iapName//purchase.skus[0]
                val fileSupposedToBeAt =context.getExternalFilesDir(null)!!.absolutePath + "/.AlarmRingTones" + File.separator + iapName + ".rta" // 구매해서 다운로드 했다면 저장되있을 위치
                Log.d(TAG, "d1_C_addPurchaseBoolToList: <D1-C-1> [PURCHASED ITEM=${purchase.skus[0]}] purchaseState=${purchase.purchaseState}, purchase=$purchase") // todo: 여기서 acknowledged 확인 가능!
                // 첫 purchaseState=1 로 뜨고, 바로 옆 purchase= 에서는 purchaseState 가 0으로 뜨는 희한..

                //purchaseFound.add(trackID) //For items that are found(purchased), add them to purchaseFound
        // **** [D1-B-2] :********************************>>> 구매 확인된 건 //todo: Refund 건 처리 + Acknowledge 가 false 로 처리된 경우도 가능성 있음 (구매중 폰 끊김 등..)
                // 인도놈 튜토리얼 참고: https://programtown.com/how-to-make-multiple-in-app-purchase-in-android-kotlin-using-google-play-billing-library/
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED)
                {
                    Log.d(TAG,"d1_C_addPurchaseBoolToList: <D1-C-2> ☺ PurchaseState is PURCHASED for trackID=$trackID, itemName=$iapName")
                    rtListPlusIAPInfo[indexOfRtObj].purchaseBool =true// [!!Bool 값 변경!!] default 값은 어차피 false ..rtObject 의 purchaseBool 값을 false -> true 로 변경

                    //*******구매는 확인되었으나 item(ex p1001.rta 등) 이 phone 에 없다 (삭제 혹은 재설치?)
                    if (!myDiskSearcher.isSameFileOnThePhone(fileSupposedToBeAt)) {
                        multiDNLDNeededList.add(rtObject)
                        Log.d(TAG, "d1_C_addPurchaseBoolToList: <D1-C-2> [멀티] 복원 다운로드 필요한 리스트에 다음을 추가: $iapName ")
                    }
                }
            // **** D1-B-3: 구매한적이 있으나 뭔가 문제가 생겨서 PurchaseState.Purchased 가 아닐때 여기로 들어옴. 애당초 구입한적이 없는 물품은 여기 뜨지도 않음!
                else {
                    Log.d(TAG,"d1_C_addPurchaseBoolToList: <D1-C-3> iapName=$iapName, trkID=$trackID, 구매 기록은 있으나 (for some reason) PurchaseState.Purchased(X)- Phone 에서 삭제 요청 ")

                }
            }//end of for loop
        // **** D1-B-4: purchaseBool=false 인 item 들 -> 삭제할 리스트에 추가해줌.
            Log.d(TAG, "d1_C_addPurchaseBoolToList: <D1-C-4> [END of FOR LOOP]")
        }//end of if(listPurchs.size > 0)
        // **** D1-B-5: 애당초 구매건이 하나도 없는 경우
        else {
            Log.d(TAG, "d1_C_addPurchaseBoolToList: <D1-C-5>: 구매건이 하나도 없음! ")
        }
        // **** D1-B-6 모든건에 대해 정리가 끝났으니 purchaseBool 이 false 인 놈들취합
        purchaseFalseRtList = rtListPlusIAPInfo.filter { rtObj -> !rtObj.purchaseBool }.toMutableList()
        Log.d(TAG, "d1_C_addPurchaseBoolToList: <D1-C-6> ************ iap_D1_B_addPurchaseBoolToList() 끝나는지점 *****")
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
    fun e_getFinalList(): List<RtInTheCloud> {
        Log.d(TAG, "e_getFinalList: <E> called")
    //Create a complete new list (그렇지 않으면 object 의 memory 값만 복사한 list 가 생성 -> rcvAdapter 안의 List(rtPlusIapInfoList) 도 같이 변함!! -> DiffUtil 아예 안 먹히는 문제!!
        val finalList = rtListPlusIAPInfo.map { it.copy() } //여기서 .map 으로 개별 객체를 .copy() 해줄 때 deep copy 가 된다 (단순 메모리 address reference 가 아님)
        return finalList //mutable -> immutable 로! //참고로 .map{} 무지 빠름. 시간 소요 전혀 없음.
    }
    fun f_getPurchaseFalseRtList() = purchaseFalseRtList
    fun g_getMultiDnldNeededList() = multiDNLDNeededList

    // DiffUtil 테스트 용도. 지워줘도 된다..
    /*fun modifiyListAndGetList(): List<RtInTheCloud> {
        rtListPlusIAPInfo[3].purchaseBool = true
        val modifiedList = rtListPlusIAPInfo.map {it.copy()}
        return modifiedList
    }*/






// ************************************************** <2> Clicked to buy
//****** h) SkuDetail 받기
    suspend fun h_getSkuDetails(iapNameAsList: List<String>): List<SkuDetails> {
        Log.d(TAG, "h_getSkuDetails: called")
        val myParams = SkuDetailsParams.newBuilder().apply {setSkusList(iapNameAsList).setType(BillingClient.SkuType.INAPP)}.build()

        return suspendCoroutine { continuation ->
            billingClient!!.querySkuDetailsAsync(myParams) {billingResult, skuDetailsList ->
                if(billingResult.responseCode == BillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                    continuation.resume(skuDetailsList)
                } else {
                    continuation.resumeWithException(Exception("billingResult-ResponseCode=${billingResult.responseCode}"))
                }
            }
        }
    }

//***** i) 구매창 보여주기-> i_launchBilliFlow 에서 실행한 구매창->콜백 받는동안 코루틴 멈춰주는것이 굉장히 힘들었으나 결국 Deferred 로 해냈음. [**CompletableDeferred 관련해서 스샷으로 정리해뒀음 **]
// SharedFlow(replay=1) 도 대안이었으나 아직 생경하여 CompletableDeferred 로 해결. (특히 ListFrag 돌아왓을 때 복원 지랄 문제)

    //i-2) 구매창 띄워주기
    suspend fun i_launchBillingFlow(receivedActivity: Activity, skuDetailsList: List<SkuDetails>): Purchase  {
        Log.d(TAG, "i_launchBillingFlow: called")
        // 여기서 if 썼을 때 error throw catch 확인?
        val oldDeferredPurchase = CompletableDeferred<Purchase>() // 나중에 값을 받는 놈!
        if(skuDetailsList.size != 1) {
            emptyMapAndThrowExceptionToDeferred("skuDetailsList size !=1 ")
        }
    /**
     * [*** purchaseMap 에 해당 'CompletableDeferred<Purchase>' 를 등록**]
     */
        purchaseMap[skuDetailsList[0].sku] = oldDeferredPurchase //  <K,V> = <p1001, CompletableDeferred<Purchase>> //todo: test... actual purchase

        val flowParams: BillingFlowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetailsList[0]).build()
        billingClient!!.launchBillingFlow(receivedActivity, flowParams) // ->결제창 열기 -> User 인풋 받으면 -> a_initBillingClient() 안에 있는 Listener 에서 반응
        return oldDeferredPurchase.await() // oldDeferredPurchase 가 값을 받을때까지 ViewModel 에서 시작된 코루틴 대기(suspend) -> 값을 받는대로 return + 코루틴 재개
    }

    fun j_checkVerification(purchaseResult: Purchase) { //verify
        Log.d(TAG, "j_checkVerification: called. PurchaseResult= $purchaseResult")
        //1) 구입은 되었는데->
        if(purchaseResult.purchaseState == Purchase.PurchaseState.PURCHASED) {
            //1-A) (X) Verification 문제 발생(Signature 문제) - 해커등..
            if (!verifyValidSignature(purchaseResult.originalJson, purchaseResult.signature))
            {
                Log.d(TAG, "j_checkVerification: 1-A) Signature 문제 발생")
                throw Exception("Verify Valid Signature Error")
            }

        } else {
            throw Exception("if문 통과못했음. Verify Valid Signature Error")
        }
    }

    suspend fun k_acknowledgePurchase(purchaseResult: Purchase, rtInTheCloud: RtInTheCloud): Boolean {

        if(purchaseResult.isAcknowledged) { // 현재 진행중인 구매건이기때문에 이쪽으로 들어올 확률은 없음. 이전 인도놈 코드 따라 쓴것으로 과거 물품에 대해서도 isAcknowledged 체크를 해줘서 안전빵으로 써줌.
            Log.d(TAG, "k_acknowledgePurchase: iapName= ${rtInTheCloud.iapName}, 이미 구매인정 되있는 상태 return!") // 어떤 연유로 이미 구매인정이 되있는 상태라면 그냥 true 반환하고 return
            return true
        } else {//[구매 마무리 절차] 신규구매시 100% 이쪽으로 들어와야함 -> 기본적으로 신규 물품은 우리가 직접 구매확인(isAcknowledged) 을 해줘야함!

            //Network 문제등으로 끊겨서 Acknowledge 가 안된 경우를 대비해 onResume() 에서 retryPurchase() 할수도 있지만. 우리는 끊겼다 자동 연결되면 IAP 가 reload 되서 D1_B_checkUnacknowledged() 에 써주기로 함.
            return suspendCoroutine { continuation ->
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseResult.purchaseToken).build()
                billingClient!!.acknowledgePurchase(acknowledgePurchaseParams)
                { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                    {
                        // ############################## 신규 구매 정상적으로 완료 #######################
                        //[구입인정] + purchaseBool 값 변경 반영(true)
                        Log.d(TAG, "k_acknowledgePurchase: iapName= ${rtInTheCloud.iapName}, 구매인정 부가 했음!!")
                        reflectPurchaseToOurLists(true, rtInTheCloud)
                        continuation.resume(true) // isPurchaseCompleted =true 로 모든 구입 절차가 끝났음을 알림.
                    } else {
                        //문제 발생으로 -> [구입인정X] + purchaseBool 값 변경 반영(false)
                        reflectPurchaseToOurLists(false, rtInTheCloud)
                        continuation.resumeWithException(Exception("isAcknowledged 부여중 Error 흐음. billingResult.responseCode= ${billingResult.responseCode} "))
                    }
                }
            }//suspendCoroutine
        }
    }

// ** Utility Methods
    //결재창 에러 대응용도: Map 을 다 비워주고 Exception 을 던져 대기중인 oldPurchaseDeferred 가 대기하지 않도록 한다.
    private fun emptyMapAndThrowExceptionToDeferred(errorReason: String) { // Map 에 등록된 모든걸 삭제+ 개별 Deferred 구매건에 Exception 을 주며 i_launchBillingFlow() 에서 return 이 진행가능하게끔 한다!
    // (그래봤자 우리는 구매가 1개만 가능해서 결국 1개여야만 한다!)
    Log.d(TAG, "emptyMapAndThrowExceptionToDeferred: [BEFORE] Map=$purchaseMap, ErrorReason=$errorReason")
    val mapIterator = purchaseMap.iterator() // HashMap 속 털기
        while(mapIterator.hasNext())
        {
            val itemInMap = mapIterator.next() //Map 안의 물건 <String, CompletableDeferred<Purchase>>
            val deferredPurchase = itemInMap.value
            deferredPurchase.completeExceptionally(Exception("Unable to finish billing process. Error= USER_CANCELED")) //Completes this deferred value exceptionally with a given exception.
            mapIterator.remove() // 현재 Map 에서 삭제(?)
        }
    Log.d(TAG, "emptyMapAndThrowExceptionToDeferred: [AFTER] Map=$purchaseMap")
    }
    
    //Reflect purchase to the list: 정상 구매가 이뤄진 경우 List 두군데에 반영=> (어차피 refresh 하면 다 반영 되지만 굳이 billingClient 연결 안하고도 즉각 RcV 반영 위해)
    private fun reflectPurchaseToOurLists(isOkayToOwn: Boolean, rtReceived: RtInTheCloud) {
        /*Log.d(TAG, "reflectPurchaseToOurLists:[BEFORE] rtListPlusIAPInfo= $rtListPlusIAPInfo")
        Log.d(TAG, "reflectPurchaseToOurLists: [BEFORE] purchaseFalseRtList = $purchaseFalseRtList")*/

        Log.d(TAG, "reflectPurchaseToOurLists: isOkayToOwn=$isOkayToOwn")
        //a) rtListPlusIAPInfo 에서 해당 rt 를 찾아 -> purchaseBool -> true/false 로 변경

        val index1 = rtListPlusIAPInfo.indexOf(rtReceived)
        if(index1 != -1) {
            rtListPlusIAPInfo[index1].purchaseBool = isOkayToOwn // 문제는 여기서 purchaseBool 을 변경 -> rcVAdapter rtPlustIapInfoList 도 자동으로 변경되어있음 -> DiffUtil() 안 먹힘.

        }
        when(isOkayToOwn) {
            true-> { //b-1) isOkayToOwn= true => purchaseFalseRtList 에서 해당 rt 를 삭제
                val index2 = purchaseFalseRtList.indexOf(rtReceived)
                if(index2 != -1) {purchaseFalseRtList.removeAt(index2)}
            }
            false -> { //b-1) isOkayToOwn= false => purchaseFalseRtList 는 전 품목중 purchaseBool=false 전체가 기록되어 있기에 이미 항목이 리스트에 존재 되있어야 한다. 
                // 그럼에도 혹시 모르니 리스트에 이미 있는지 확인후 없으면 해당 rt 를 추가!
                if(!purchaseFalseRtList.contains(rtReceived)) {
                    purchaseFalseRtList.add(rtReceived)
                }
            }
        }
        /*Log.d(TAG, "reflectPurchaseToOurLists: [AFTER] rtListPlusIAPInfo= $rtListPlusIAPInfo")
        Log.d(TAG, "reflectPurchaseToOurLists: [AFTER] purchaseFalseRtList = $purchaseFalseRtList")*/

    }

    /**
     * Verifies that the purchase was signed correctly for this developer's public key.
     *
     * Note: It's strongly recommended to perform such check on your backend since hackers can
     * replace this method with "constant true" if they decompile/rebuild your app.
     *
     */
    private fun verifyValidSignature(signedData: String, signature: String): Boolean {
        Log.d(TAG, "verifyValidSignature: begins..")
        return try {
            //for old playconsole
            // To get key go to Developer Console > Select your app > Development Tools > Services & APIs.
            //for new play console
            //To get key go to Developer Console > Select your app > Monetize > Monetization setup

            val base64Key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjxP65hcVxu3nM/XR89EoZzEwK1itdhPcTOT+itC6Guf5omQLHe3A4cDLlTSjZqoNMy3jzNK7mSiPG8NRTa6waHaaHx3fxatR4Or8KeS8WzFNQsKbFz2OCt3kTRQ5lUuoIvyjj+VjEv9XwyPrFRb8Lxq47KqHnjiJyeBcXznLXD//4YOsTaTp2dBxuLXjJQEzkp4EPgvhNh6BE+bX+SvXRPc3x3dghqAUtdaoM3C77QgCnRc94nYnWyXyQqqX2PvEX3KNKM//nQbKtJbNUB/NpKlzodiY3WdFMVNS3ySw9S9irikhDv7jOQ1OnI+dzKMLCeQIRTxqFHB2RxkqpzOHtQIDAQAB"

            Security.verifyPurchase(base64Key, signedData, signature)
        } catch (e: IOException) {
            false
        }
    }

}
//a) Data class - responseCode and Purchases (포맷 제공)
//data class MyPurchResultENUM(val billingResult: BillingResult, val purchasesList: List<Purchase>?)

