package com.theglendales.alarm.jjmvvm.iapAndDnldManager

import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.io.File
import kotlin.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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

    fun iap_A_initBillingClient() {
        Log.d(TAG, "iap_A_initBillingClient: <A> Called")
        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build()
        Log.d(TAG, "iap_A_initBillingClient: <A> Finished")
    }

    fun iap_B_feedRtList(rtListFromFb: MutableList<RtInTheCloud>) {
        Log.d(TAG, "iap_B_feedRtList: <B> Called")
        rtListPlusIAPInfo = rtListFromFb}

    suspend fun iap_C_prepBillingClient(): BillingResult {
        Log.d(TAG, "iap_C_prepBillingClient: <C> Called")
        if(!billingClient!!.isReady) {
            Log.d(TAG, "iap_C_prepBillingClient: <C> BillingClient Not Ready(X)! Re init!")
            billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build()
        }
        return suspendCoroutine { continuation -> // suspendCoroutine -> async Callback 등 잠시 대기가 필요할 때 사용 -> 여기서 잠시 기존 코루틴 정지(JjMainVModel)

            billingClient!!.startConnection(object : BillingClientStateListener{
                override fun onBillingSetupFinished(p0: BillingResult) {
                    Log.d(TAG, "iap_C_prepBillingClient: <C> BillingSetupFinished (O)")
                    continuation.resume(p0) // -> continuation 에서 이어서 진행 (원래 코루틴- JjMainVModel> iapParentJob 으로 복귀)
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
    suspend fun iap_D1_addPurchaseBoolToList() {

        Log.d(TAG, "iap_D1_addPurchaseBoolToList: <D1> called")
        billingClient!!.queryPurchasesAsync(BillingClient.SkuType.INAPP, object : PurchasesResponseListener{
            override fun onQueryPurchasesResponse(bResult: BillingResult, listPurchs: MutableList<Purchase>)
            {
                Log.d(TAG, "OnQPR: <D1> called ")
                multiDNLDNeededList.clear()
                purchaseFalseRtList.clear()

                //val purchaseFoundItemsTrIds = ArrayList<Int>()
                if(listPurchs.size > 0) // 구매건이 한 개 이상.
                {
                    Log.d(TAG, "OnQPR: <D1> 총 구매 갯수=listPurchs.size=${listPurchs.size}")
                    //myQryPurchListSize = listOfPurchases.size // 추후 MyDownloader_v1.kt > multiDownloadOrNot() 에서 활용.

                //**** D-1-A: 구매 기록이 있는 모든건에 대해 [(구매유효=PurchaseState.PURCHASED) + (구매했으나 Refund 등으로 PurchaseState.PURCHASED 가 아닌것도 포함))
                    for(purchase in listPurchs)
                    {
                        /**
                         * IAP Library 4.0 업뎃 => .sku 가 없어지고 .skus => List<String> 을 반환함. (여러개 살 수 있는 기능이 생겨서)
                         * 우리는 해당 기능 사용 계획이 없으므로 무조건 우리의 .skus list 는 1개여야만 한다! 만약 1개가 아니면 for loop 에서 다음 iteration 으로 이동
                         */
                        ///** .indexOfFirst (람다식을 충족하는 '첫번째' 대상의 위치를 반환. 없을때는 -1 반환) */
                        val indexOfRtObj: Int = rtListPlusIAPInfo.indexOfFirst { rtObj -> rtObj.iapName == purchase.skus[0] } //조건을 만족시키는 가장 첫 Obj 의 'index' 를 리턴. 없으면 -1 리턴.
                    // 우리가 구매한 물품이 현재 rtListPlusIAPInfo 에 없는 물품이면 (ex. p1,p7 등 아예 PlayConsole 카탈로그에서 Deactivate 시킨 물품인 경우) -> 다음 for loop 으로 넘어가기
                        if(purchase.quantity != 1 || indexOfRtObj < 0) { // 갯수가 1개 초과 or rtListPlusIAPInfo 리스트에 우리가 찾는 rtObj 이 없는 경우
                            if(purchase.quantity!=1) {Log.d(TAG, "OnQPR: <D1-A> [IAP] 심각한 문제. Quantity 가 1개가 아님! Quantity=${purchase.quantity}")
                                toastMessenger.showMyToast("More than one IAP sku IDs.",isShort = true)}
                            if(indexOfRtObj<0) {Log.d(TAG, "OnQPR: <D1-A> List 에서 현재 purchase.sku(=${purchase.skus[0]}) 에 매칭하는 rtObject 를 찾을 수 없음.")}
                            continue // 다음 for loop 의 iteration (purchase) 로 이동(?)
                        }

                    // 정상적으로 item 을 리스트에서 찾았다는 가정하에 다음을 진행->
                        val rtObject = rtListPlusIAPInfo[indexOfRtObj]
                        val trackID = rtObject.id
                        val iapName = rtObject.iapName//purchase.skus[0]
                        val fileSupposedToBeAt = context.getExternalFilesDir(null)!!.absolutePath + "/.AlarmRingTones" + File.separator + iapName +".rta" // 구매해서 다운로드 했다면 저장되있을 위치
                        Log.d(TAG, "OnQPR: <D1-A> trackId=$trackID, purchase.skus[0]=${purchase.skus[0]}, p.skus(list)=${purchase.skus}")

                        //purchaseFound.add(trackID) //For items that are found(purchased), add them to purchaseFound
                // **** D-1-B:********************************>>> 구매 확인된 건
                        if(purchase.purchaseState == Purchase.PurchaseState.PURCHASED)
                        {
                            Log.d(TAG, "OnQPR: <D1-B> ☺ PurchaseState is PURCHASED for trackID=$trackID, itemName=$iapName")
                            rtListPlusIAPInfo[indexOfRtObj].purchaseBool= true// [!!Bool 값 변경!!] default 값은 어차피 false ..rtObject 의 purchaseBool 값을 false -> true 로 변경

                            //*******구매는 확인되었으나 item(ex p1001.rta 등) 이 phone 에 없다 (삭제 혹은 재설치?)
                            if(!myDiskSearcher.isSameFileOnThePhone(fileSupposedToBeAt)) {
                                multiDNLDNeededList.add(rtObject)
                                Log.d(TAG, "OnQPR: <D1-B> [멀티] 복원 다운로드 필요한 리스트에 다음을 추가: $iapName ")
                            }
                        }
                // **** D-1-C: 구매한적이 있으나 뭔가 문제가 생겨서 PurchaseState.Purchased 가 아닐때 여기로 들어옴. 애당초 구입한적이 없는 물품은 여기 뜨지도 않음!
                        else
                        {
                            Log.d(TAG, "OnQPR: <D-1-C> iapName=$iapName, trkID=$trackID, 구매 기록은 있으나 (for some reason) PurchaseState.Purchased(X)- Phone 에서 삭제 요청 ")
                            //myDiskSearcher.deleteFromDisk(fileSupposedToBeAt)
                        }
                    }//end of for loop
                // **** D-1-D: purchaseBool=false 인 item 들 -> 삭제할 리스트에 추가해줌.
                    Log.d(TAG, "OnQPR: <D-1-D> [END of FOR LOOP]")
                }//end of if(listPurchs.size > 0)
                // **** D-1-E: 애당초 구매건이 하나도 없는 경우
                else {
                    Log.d(TAG, "OnQPR: <D-1-E>: 구매건이 하나도 없음! ")
                }
            // **** D-1-F 모든건에 대해 정리가 끝났으니 purchaseBool 이 false 인 놈들취합

                purchaseFalseRtList = rtListPlusIAPInfo.filter { rtObj -> !rtObj.purchaseBool }.toMutableList()
                Log.d(TAG, "OnQPR: <D-1-F> ************ iap_D1_addPurchaseBoolToList() 끝나는지점 *****")
            }

        })
        //Log.d(TAG, "iap_D1_addPurchaseBoolToList: <D1> Finished")
    }
    //<D2> 현재 리스트에 상품별 가격 적어주기 (ex. $1,000)
    suspend fun iap_D2_addPriceToList() {
        Log.d(TAG, "iap_D2_addPriceToList: <D2> called")
        val itemNameList = ArrayList<String>()
        rtListPlusIAPInfo.forEach {rtObject -> itemNameList.add(rtObject.iapName)}

        val myParams = SkuDetailsParams.newBuilder()
        myParams.setSkusList(itemNameList).setType(BillingClient.SkuType.INAPP)
        //**Very Nice: 아래 querySkuDetailsAsync 는 Async 콜이기 때문에 -> billingClient.query...Async{} 블락 아래 Logd 로 점프함 -> 그렇지만!
        //JjMainVModel>viewModelScope>IapParentJob.launch{} 안에서 이 전체 콜백이 끝날때까지 기다려줌
        billingClient!!.querySkuDetailsAsync(myParams.build()) {queryResult, skuDetailsList ->
            if(queryResult.responseCode == BillingClient.BillingResponseCode.OK && skuDetailsList!=null) {
                for(skuDetails in skuDetailsList) {
                    //something better than .single? -> 근데 실제 연산 속도가 개당 .001 초니깐
                    rtListPlusIAPInfo.single { rtObj -> rtObj.iapName == skuDetails.sku }.itemPrice = skuDetails.price
                    Log.d(TAG,"iap_D2_addPriceToList : <D2> a) item title=${skuDetails.title} b)item price= ${skuDetails.price}, c)item sku= ${skuDetails.sku}")
                }
                Log.d(TAG, "iap_D2_addPriceToList: <D2> Finished (O)")
            } else {
                Log.d(TAG, "iap_D2_addPriceToList: <D2> Finished(X) - Error! XXX loading price for items")
                throw Exception("<D2> Error ")
            }
            //todo: 다운로드 관련 여기서..?
        }
        //delay(4000L) // Delay Test .. +4.0 초 걸린다고 치고.
    }
    //<E> 완성 리스트를 전달 -> 라이브데이터 -> SecondFrag -> rcVUpdate
    fun iap_E_getFinalList(): MutableList<RtInTheCloud> {
        Log.d(TAG, "iap_E_getFinalList: <E> called")
        return rtListPlusIAPInfo
    }


// ****************** <2> 현재 구매 관련

//**
    override fun onPurchasesUpdated(p0: BillingResult, p1: MutableList<Purchase>?) {
        //TODO("Not yet implemented")
    }

}