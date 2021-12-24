package com.theglendales.alarm.jjmvvm.iapAndDnldManager

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.*
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjadapters.RcViewAdapter
import com.theglendales.alarm.jjdata.RingtoneClass
import com.theglendales.alarm.jjmvvm.helper.MySharedPrefManager

import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import java.io.File
import java.io.IOException

private const val TAG="MyIAPHelper_v2"
class MyIAPHelper_v2(private val receivedActivity: Activity,
                     private val rcvAdapterInstance: RcViewAdapter?,
                     private val myDownloaderVInstance: MyDownloader_v2) :  PurchasesUpdatedListener
{
    private val mySharedPrefManager: MySharedPrefManager by globalInject()
    private val myDiskSearcher: DiskSearcher by globalInject()

    var currentRtList: MutableList<RingtoneClass> = ArrayList()
    var multiDNLDNeededList: MutableList<RingtoneClass> = ArrayList() // ##### <기존 구매했는데 a) 삭제 후 재설치 b) Pxx.rta 파일 소실 등으로 복원이 필요한 파일들 리스트> [멀티]
    var clickedToBuyTrkID = -10 // 유저가 클릭할 때 myOnPurchaseClicked()에서 TrID 로 바뀜. [앱 Launch 후 IAP 초기화중 기존에 구입된 품목이 한개일경우-handlePurchase() 에서 자동 다운로드로 연결되는것을 방지]


    companion object
    {
        val itemPricesMap: HashMap<String, String> = HashMap() // <trackID, price> ex) <1, 1000>, <2, 1200> 현재는 KRW 그러나 지역/국가별 자동 설정 될듯..
        var myQryPurchListSize : Int = 0
    }

    private var billingClient: BillingClient? = null

    //## <B> A. refreshItemIdIapNameTitle -> B. initIAP() -> (wait..) C. onBillingSetupFinished() -> D. refreshPurchaseStatsMap() -> E. refreshItemsPriceMap() =>(finally..) rcvAdapter.refreshRcView()!
    /**
     * initIAP(): 아이템의 구입여부 확인 후 myPref 에 bool 값 저장 -> 끝나면 rcView update!
     */
    private fun initIAP() //
    {
        Log.d(TAG, "B) initIAP: init starts")
        // Establish connection to billing client
        // Check purchase status from google play store cache on every app start
        billingClient = BillingClient.newBuilder(receivedActivity.applicationContext)
            .enablePendingPurchases().setListener(this).build()

        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "C) onBillingSetupFinished: starts..")


                // billing setup 이 문제없이 되었다면.
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                {
                    //C-1) (!!기존!!) 구매한 물품들에 대해서만! 더.블.체크..? Check which items are in purchase list and which are not in purchase list
                    val queryPurchase = billingClient!!.queryPurchases(BillingClient.SkuType.INAPP)
                    val queryPurchases = queryPurchase.purchasesList

                    //check status of found items and save values to preference
                    //item which are not found simply save false values to their preference
                    //indexOf return index of item in purchase list from 0-2 (because we have 3 items) else returns -1 if not found
                    val purchaseFound = ArrayList<Int>()
                    if (queryPurchases != null && queryPurchases.size > 0)
                    {
                        Log.d(TAG, "C) onBillingSetupFinished: queryPurchases.size = ${queryPurchases.size}")
                        myQryPurchListSize = queryPurchases.size // 추후 MyDownloader_v1.kt > multiDownloadOrNot() 에서 활용.
                        multiDNLDNeededList.clear() // [멀티] 필요한 리스트는 우선 '0' 값으로, 바로 밑에서 채워줌.
                        handlePurchaseNotification(queryPurchases)

                        //check item in purchase list. 구매 상태인 물품에 대해서! status check! 한번 더 확인. 문제없으면 true 로..
                        for (p in queryPurchases)
                        {
                            val rtObject = currentRtList.single { RtClass -> RtClass.iapName == p.sku } // iapName 이 p.sku (ex.p1, p2) 와 매칭하는 rtObject
                            val trackID = rtObject.id
                            val iapName = rtObject.iapName
                            val fileNameAndFullPath = receivedActivity.getExternalFilesDir(null)!!
                                .absolutePath + "/.AlarmRingTones" + File.separator + iapName +".rta" // rta= Ring Tone Audio 내가 만든 확장자..

                            //if purchase found. 구입 내역이 있는! item 만 나옴 (ex. 현재 21/06/4에는 rt1, rt2 만 여기에 해당됨..)
                            if (trackID > -1)
                            {
                                purchaseFound.add(trackID) //For items that are found(purchased), add them to purchaseFound

                                // ********************************>>>기존 구매건
                                if (p.purchaseState == Purchase.PurchaseState.PURCHASED)
                                {
                                    mySharedPrefManager.savePurchaseBoolPerIapName(iapName, true)
                                    Log.d(TAG, "C-1) ☺ onBillingSetupFinished: PurchaseState is PURCHASED for trackID=$trackID, itemName=$iapName")
                            // 기존 구매가 확인된 RT 는 무조건 URL 을 SharedPref (MyIapUrl.xml)에 저장 -> 이후 multiDNLNDNeededList 에 추가되며 다운로드로 진행.
                                    val dnldURL = rtObject.mp3URL
                                    mySharedPrefManager.saveUrlPerIap(iapName, dnldURL)
                                    //downloadOrDeleteSinglePurchase(trackID, true, downloadNow = false)
                            // (기존 구매가 확인되었으나 파일이없다) -> multiDNLDNeededList 에 추가! [멀티]
                                    if(!myDiskSearcher.isSameFileOnThePhone(fileNameAndFullPath)) {
                                        Log.d(TAG, "onBillingSetupFinished: [멀티] 복원 다운로드 필요한 리스트에 다음을 추가: $iapName")
                                        multiDNLDNeededList.add(rtObject)
                                    }

                                // <<<********************************기존 구매건
                                } else
                                { // 구매한적이 있으나 뭔가 문제가 생겨서 PurchaseState.Purchased 가 아닐때 여기로 들어옴. 애당초 구입한적이 없는 물품은 여기 뜨지도 않음!
                                    mySharedPrefManager.savePurchaseBoolPerIapName(iapName, false)
                                    Log.d(TAG, "C-ERROR) ‼ onBillingSetupFinished: PurchaseState is (not)PURCHASED for trackID=$trackID, itemName=$iapName")
                                    downloadOrDeleteSinglePurchase(trackID, false, downloadNow = false) // Disk 에 있으면 삭제하기 위해 이쪽으로 전달..
                                }
                            }
                        }

                        //C-2) (기존) 구매안된 물품들(굉장히 다수겠지..)에 대해서는 SharedPref 에 false 로 표시!. //items that are not found in purchase list mark false
                        //indexOf returns -1 when item is not in foundlist. 리스트에 없으면 -1 반환.

                        currentRtList.forEach { rtObject->
                            if(purchaseFound.indexOf(rtObject.id) == -1)
                            { // itemIDsMap 에서 "구매한목록(purchaseFound)" 에 없는 놈들은 다 false 로!
                                val iapName = rtObject.iapName
                                val trId = rtObject.id
                                val trTitle = rtObject.title
                                //val badgeStr -> FB 업뎃 후 입력 가능.

                            mySharedPrefManager.savePurchaseBoolPerIapName(iapName, false)
                            Log.d(TAG, "C-2) ₥₥₥ onBillingSetupFinished: trackId=$trId 물품(상품 이름은=$iapName)은 purchaseFound 에 없음! 고로 false 로 SharedPref 에 저장됨! ")


                    /**
                     * 모든 파일명은 .iapName 과 일치해야함 (ex p1.rta, p2.rta .. )
                     */
                            val fileNameAndFullPath = receivedActivity.getExternalFilesDir(null)!!
                                .absolutePath + "/.AlarmRingTones" + File.separator + iapName +".rta" // rta= Ring Tone Audio 내가 만든 확장자..

                            //todo: 다음을 구매 안한 상품들에 대해서 매번 해줘야 한다는 것= too CPU expensive..
                            if(myDiskSearcher.isSameFileOnThePhone(fileNameAndFullPath)) { // 혹시나..구매한적도 없는데 만약 디스크에 있으면
                                // 디스크에서 삭제
                                Log.d(TAG, "onBillingSetupFinished: $iapName 는(은) 산 놈도 아닌데 하드에 있음. 지워야함!! ")
                                myDiskSearcher.deleteFromDisk(rtObject, fileNameAndFullPath)
                            }
                        }}
                    }
                    // C-3) 애당초 구매건이 하나도 없으면. 모두 false!
                    else {
                        currentRtList.forEach { rtObject -> mySharedPrefManager.savePurchaseBoolPerIapName(rtObject.iapName, false) }
                        Log.d(TAG, "C-3) ☺ onBillingSetupFinished:  The User has never ever 산적이 없으면 일로 오는듯! (queryPurchase.size 가 0 이란 뜻..?)")
                    }
                }

                refreshItemsPriceMap()

                Log.d(TAG, "C) onBillingSetupFinished: finished..")
            }

            override fun onBillingServiceDisconnected() {}
        })
        Log.d(TAG, "initIAP: finished..")
    }
    //## <A> A. refreshItemIdIapNameTitle -> B. initIAP() -> (wait..) C. onBillingSetupFinished() -> D. refreshPurchaseStatsMap() -> E. refreshItemsPriceMap() =>(finally..) rcvAdapter.refreshRcView()!
/**
 *  제일 먼저 호출되는 곳
 */
    fun refreshItemIdIapNameTitle(newRtList: MutableList<RingtoneClass>) { // 새로 받은 ringToneList 로 itemIDsMap 을 수정/업뎃해줌. initIAP() 에서 호출됨.
        currentRtList.clear()
        currentRtList = newRtList

        Log.d(TAG, "A) refreshItemIdsMap: begins!")

        initIAP()
    }

    //##<E>  A. refreshItemIdsMap-> B. initIAP() -> (wait..) C. onBillingSetupFinished() -> D. refreshPurchaseStatsMap() -> E. refreshItemsPriceMap() ==> (finally..) rcvAdapter.refreshRcView()!
    private fun refreshItemsPriceMap() {
        Log.d(TAG, "refreshItemsPriceMap: called")
        val itemNameList = ArrayList<String>()

        if(!currentRtList.isNullOrEmpty())
        {
            currentRtList.forEach { rtObject -> itemNameList.add(rtObject.iapName)} // 1) (iap name= p1, p2, p3..)를 itemNameList 에 저장-> 이게 myParams.setSkusList(string List!!) 에 사용됨

            val myParams = SkuDetailsParams.newBuilder()
            myParams.setSkusList(itemNameList).setType(BillingClient.SkuType.INAPP)

            billingClient!!.querySkuDetailsAsync(myParams.build()) { myQueryResultYo, mySkuDetailsListYo -> // SAM .. object: SkuDetailsResponseListener 인터페이스 심기와 같음.

                if (myQueryResultYo.responseCode == BillingClient.BillingResponseCode.OK && mySkuDetailsListYo != null)
                {
                    for (skuDetails in mySkuDetailsListYo) {

                        itemPricesMap[skuDetails.sku] = skuDetails.price// itemPricesMap[iapName] = 가격. ex) (p1, 1000) (p2, 2000) ...
                        Log.d(TAG,"E) refreshItemsPriceMap: a) item title=${skuDetails.title} b)item price= ${skuDetails.price}, c)item sku= ${skuDetails.sku}")
                        // logd 결과 예시: a) item title=p1 b)item price= ₩1,000, c)item sku= p1
                    }
                }
            /** 사실상 모든 INIT IAP 작업이 끝나는 곳. 여기서 RCView Update & Multi DNLD 진행
             *  왜 rcvAdapter.refresh 나 myDownloader.multiple..() 이 여기에 있어야 하는지. 아래에서 rcvAdapter.refresh() & myDownloader.multipleFileDNLND() 넣으면 안되는지는 아직 모르겠음.
             */
                Log.d(TAG, "refreshItemsPriceMap: ------------IAP 모든게 끝나는 지점-----")
                rcvAdapterInstance!!.refreshRecyclerView(currentRtList) // #$#$#$$#$#$!@#$!!#! FINALLY 여기서 rcView 를 업뎃-> onBindView 하게끔! #$#$@!$#@@$@#$#
                rcvAdapterInstance.notifyDataSetChanged() // 현재 Firebase 가 두번씩 로딩되면서. 간혹 값이 null 로 표기되는 경우가 있음.
                if(multiDNLDNeededList.size > 0) { // 복원 필요한 파일 갯수가 있으면 -> [멀티] 다운로드 진행->
                    myDownloaderVInstance.multipleFileDNLD(multiDNLDNeededList)
                }

            }
        } else {
            Log.d(TAG, "E) refreshItemsPriceMap: itemIdsMap is null or empty..")
            Toast.makeText(receivedActivity, "Error Loading Billing Info: Error stage <E>", Toast.LENGTH_SHORT).show()
            rcvAdapterInstance!!.refreshRecyclerView(currentRtList) // #$#$#$$#$#$!@#$!!#! FINALLY 여기서 rcView 를 업뎃-> onBindView 하게끔! #$#$@!$#@@$@#$#
        }
// 여기서는 안 먹힘.       rcvAdapterInstance!!.refreshRecyclerView(currentRtList) // #$#$#$$#$#$!@#$!!#! FINALLY 여기서 rcView 를 업뎃-> onBindView 하게끔! #$#$@!$#@@$@#$#
//        myDownloaderVInstance.multipleFileDNLD(multiDNLDNeededList)// [멀티] 다운로드 진행->

    }

//Clicked to Purchase 관련

//정상적인 purchase Flow: myOnPurchaseClicked() > initiatePurchase() > launchBillingFlow(구매화면) > onPurchaseUpdated() > handlePurchaseNotification()> downloadHanlderBridge()
    fun myOnPurchaseClicked(trackID: Int) { // position -> trackID: Int..  position 쓰면 Chip 걸린 상태로 리스트 변경되었을 때 다른 물품을 사게되겠찌..
        clickedToBuyTrkID = trackID
        val iapName = getIapNameByTrkId(trackID)
        val fileNameAndFullPath = receivedActivity.getExternalFilesDir(null)!!
        .absolutePath + "/.AlarmRingTones" + File.separator + iapName +".rta" // rta= Ring Tone Audio 내가 만든 확장자..
        Log.d(TAG, "myOnPurchaseClicked: ATTEMPTING TO BUY!! 두구두구.. $iapName")

        if (mySharedPrefManager.getPurchaseBoolPerIapName(iapName) && myDiskSearcher.isSameFileOnThePhone(fileNameAndFullPath)) { //selected item is already purchased // todo: 혹은 디스크에 있는지? 그러나 내가 자동적으로 recover 되게 만들었음.
            Toast.makeText(receivedActivity, "You already own $iapName", Toast.LENGTH_SHORT).show()
            //Snackbar.make(receivedActivity,"You already own " +itemIDsMap[trackID], Snackbar.LENGTH_SHORT).show()
            return
        }

        //Initiate purchase on selected product item click
        //Check if service is already connected
        if (billingClient!!.isReady) {
            initiatePurchase(iapName)

        } else {
            billingClient = BillingClient.newBuilder(receivedActivity).enablePendingPurchases().setListener(this@MyIAPHelper_v2).build()
            billingClient!!.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        initiatePurchase(iapName)
                    } else {
                        Toast.makeText(receivedActivity, "Error " + billingResult.debugMessage, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onBillingServiceDisconnected() {}
            })
        }
    }

    private fun initiatePurchase(PRODUCT_ID: String) {
        Log.d(TAG, "initiatePurchase: begins..")
        val skuList: MutableList<String> = ArrayList()
        skuList.add(PRODUCT_ID)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

        billingClient!!.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (skuDetailsList != null && skuDetailsList.size > 0) {
                    val flowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetailsList[0])
                        .build()
                    billingClient!!.launchBillingFlow(receivedActivity, flowParams) // Display purchase dialog

                }
                else {
                    //try to add item/product id "p1" "p2" "p3" inside managed product in google play console
                    Toast.makeText(receivedActivity, "Purchase Item $PRODUCT_ID not Found", Toast.LENGTH_SHORT).show()
                }
            }
            else {
                Toast.makeText(receivedActivity," Error " + billingResult.debugMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Check purchase result and handle it accordingly// Result of purchase dialog (launchBillingFlow 가 invoke!) will be reported here
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        Log.d(TAG, "onPurchasesUpdated: begins..")
        //if item newly purchased
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null)
        {
            //Log.d(TAG, "onPurchasesUpdated: A- 정상 신규 구매! (파일 확인 후 없으면)다운로드 진행!")
            handlePurchaseNotification(purchases)
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)
        {
            //Log.d(TAG, "onPurchasesUpdated: B- 이미 있는 물품 구매! (파일 확인 후 없으면) 다운로드 진행 ")
            val queryAlreadyPurchasesResult = billingClient!!.queryPurchases(BillingClient.SkuType.INAPP)
            val alreadyPurchased = queryAlreadyPurchasesResult.purchasesList
            alreadyPurchased?.let { handlePurchaseNotification(it) }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED)
        {
            Log.d(TAG, "onPurchasesUpdated: C- 구매 취소!! (파일 확인 후 있으면) 삭제 ")

            Toast.makeText(receivedActivity, "Purchase Canceled", Toast.LENGTH_SHORT).show()
        } else
        {
            Log.d(TAG, "onPurchasesUpdated: D -기타 에러.. ")

            Toast.makeText(receivedActivity, "Error " + billingResult.debugMessage, Toast.LENGTH_SHORT).show()
        }
    }
//List 에서 원하는 값 받아오는 functions. 내가 만든 ------------>>>>>
    // 아래 Method 들 모두 logd 로 시작/끝 테스트해보니 0.01 초도 안걸림.
    fun getRtInstanceByIapName(iapName: String) = currentRtList.first { rtObj -> rtObj.iapName == iapName }
    fun getRtInstanceByTrkId(trkId: Int) = currentRtList.first { rtObj -> rtObj.id == trkId }

    fun getIapNameByTrkId(trkId: Int): String {

        val rtObj: RingtoneClass = currentRtList.first { rtObj -> rtObj.id == trkId } // .first() : returns the first object matching { predicate}
        return rtObj.iapName
    }

//List 에서 원하는 값 받아오는 functions. 내가 만든 <<<------------

    fun handlePurchaseNotification(purchases: List<Purchase>) {
        Log.d(TAG, "handlePurchaseNotification: begins.")

        for (purchase in purchases) {
            val rtInstance = getRtInstanceByIapName(purchase.sku)
            val trackId = rtInstance.id
            val iapName = rtInstance.iapName

            //purchase found
            if (trackId > -1)
            {

                //1) if item is purchased
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED)
                {
                    // 1-A) Invalid purchase
                    if (!verifyValidSignature(purchase.originalJson, purchase.signature))
                    {
                        // show error to user
                        Log.d(TAG, "handlePurchaseNotification: 1-A Invalid purchase. 다운취소???")
                        Toast.makeText(receivedActivity, "Error : Invalid Purchase", Toast.LENGTH_SHORT).show()
                        downloadOrDeleteSinglePurchase(trackId, false, downloadNow = true) //todo: downloadOrDelete(trackId) (X)
                        continue  //skip current iteration only because other items in purchase list must be checked if present
                    }
                    // 1-B) else purchase is valid but! 인식이 안된경우?

                    if (!purchase.isAcknowledged)
                    {
                        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                        billingClient!!.acknowledgePurchase(acknowledgePurchaseParams)
                        { billingResult ->
                            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                            {
                                //if purchase is acknowledged then save value in preference
                                mySharedPrefManager.savePurchaseBoolPerIapName(iapName, true)  // ex) (p1, true) (p2, false) ..
                                Toast.makeText(receivedActivity, "You have purchased $iapName", Toast.LENGTH_SHORT).show()
                                Log.d(TAG, "handlePurchaseNotification: 1-B 정상구매(not acknowledged). 다운로드 진행 ")
                                // ############################## 신규 구매건. 과거 구매건은 정상 처리되었어도 여기까지는 안 들어옴. (logd 로 확인)
                                downloadOrDeleteSinglePurchase(trackId, true, downloadNow = true) //todo: downloadOrDelete(trackId) (O)
                                // ############################## 신규 구매건

                                rcvAdapterInstance!!.notifyDataSetChanged() //todo: 이거왠만하면 쓰지 말라는데. 구매상태 변화니깐 item Change 만 하면 되는데. 현재 작성되있는 refreshRcV(rtList) 로는 반영이 안되서..
                                // 왜냐면 단순히 refreshRecyclerView(list) 방식으로는 (차이가 없으므로) (구매여부 ImageView) UI 업뎃 안된다.
                            }
                        }
                    }
                    // 1-C) (일반) 정상 구매. !!과거 구입건이 있을 때 앱 재설치 후 "최초" 실행시는 이쪽으로 들어옴.!! -> 그래서 다운받게 되는 이슈..
                    else
                    {
                        // Grant entitlement to the user on item purchase
                        if (mySharedPrefManager.getPurchaseBoolPerIapName(iapName)) {
                            mySharedPrefManager.savePurchaseBoolPerIapName(iapName, true)

                            if(myQryPurchListSize == 1 && clickedToBuyTrkID == trackId) { // A) 정상 신규 구매 1개 + 유저가 클릭한 trId 와 일치하면 진행
                                Log.d(TAG, "handlePurchaseNotification: 1-C-a) 정상 single 구매. 다운로드 진행")
                                // ############################## 신규 구매건. 과거 구매건은 정상 처리되었어도 여기까지는 안 들어옴. (logd 로 확인)
                                // !!!!!!!!!!!!! 그러나!! 전체 리스트중 한개만 구입해놓은 상태면 다운받게됨.
                                Toast.makeText(receivedActivity, "You have purchased $iapName", Toast.LENGTH_SHORT).show()

                            // 실 다운로드 실행->
                                downloadOrDeleteSinglePurchase(trackId, true, downloadNow = true)
                                // ############################## 신규 구매건

                                rcvAdapterInstance!!.notifyDataSetChanged()
                                return
                            }
                            else if(myQryPurchListSize > 1) { // B) 앱 재설치 후 최초 실행시 Sync 진행 (이건 위에서 해주므로 여기서는 할게 없음)
                                Log.d(TAG, "handlePurchaseNotification: 1-C-b) 정상 Multi Sync. 기존 구매건에 대한 Sync C-1 에서 진행 예정.")
                                return
                            }

                        }
                    }
                }

                //2) 펜딩 및 기타 이슈,
                else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                    Log.d(TAG, "handlePurchaseNotification: Purchase is pending")
                    Toast.makeText(receivedActivity,"$iapName: 2-A Purchase is Pending. Please complete Transaction", Toast.LENGTH_SHORT).show()
                    downloadOrDeleteSinglePurchase(trackId, false, downloadNow = true)
                }
                else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
                    //mark purchase false in case of UNSPECIFIED_STATE
                    mySharedPrefManager.savePurchaseBoolPerIapName(iapName, false)
                    Toast.makeText(receivedActivity, "$iapName: Purchase Status Unknown", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "handlePurchaseNotification:  2-B Purchase Status Unknown. 파일 확인후 삭제?")

                    downloadOrDeleteSinglePurchase(trackId, false, downloadNow = true)
                    rcvAdapterInstance!!.notifyDataSetChanged()
                }
            }
        }
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


    // 신규 단일 구매건 & 기존 단일 구매건에 대해서- file 디스크에 있는지 체크 및 download 할지 여부를 정함.
    private fun downloadOrDeleteSinglePurchase(trId: Int, keepTheFile: Boolean, downloadNow: Boolean) {
        Log.d(TAG, "downloadOrDeleteSinglePurchase: <<<MyDownloader_v1.Kt 로 임무 전달!!> trackId= $trId")
        val rtInstance = getRtInstanceByTrkId(trId)

        //val trTitle = rtInstance.title
        val iapName = rtInstance.iapName
        val fileNameAndFullPath = receivedActivity.getExternalFilesDir(null)!!
            .absolutePath + "/.AlarmRingTones" + File.separator + iapName +".rta" // rta= Ring Tone Audio 내가 만든 확장자..

        if(!keepTheFile && myDiskSearcher.isSameFileOnThePhone(fileNameAndFullPath)) {
            // (잘못된 구매 사유 등으로) Keep 할 필요 없는 파일인데 디스크에 있을경우 삭제! //todo: 테스트 필요.
            Log.d(TAG, "downloadOrDeleteSinglePurchase: !![WARNING] Deleting this File!!=$iapName")
                myDiskSearcher.deleteFromDisk(rtInstance, fileNameAndFullPath)
            return
        }
        else if(keepTheFile && myDiskSearcher.isSameFileOnThePhone(fileNameAndFullPath)) {
            // (정상 구매로 Keep 은 맞는데 이미 Disk 에 동일한 파일이 있다.) //todo: mp3가 정상 파일인지 다운로드 안 끊겼는지도 확인 필요. 이거 좀 불가능한듯. FileSizeCheck?
            Log.d(TAG, "downloadOrDeleteSinglePurchase: 다운로드 필요 없음. 이미 파일= $iapName 이 폰에 있으므로.. ")
            return

        }
        else if(downloadNow && keepTheFile) { // 정상 단독 구매건 다운로드 (sync 와 무관하고 app 에서 한개 샀을 때)
            Log.d(TAG, "downloadOrDeleteSinglePurchase: ***SINGLE PURCHASE!!")
            myDownloaderVInstance.singleFileDNLD(rtInstance)
        }

    }


}