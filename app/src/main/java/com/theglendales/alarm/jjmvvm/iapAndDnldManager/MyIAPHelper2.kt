package com.theglendales.alarm.jjmvvm.iapAndDnldManager

import android.app.Activity
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.*
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjadapters.RcViewAdapter
import com.theglendales.alarm.jjdata.RingtoneClass

import com.theglendales.alarm.jjmvvm.util.DiskSearcher
import java.io.File
import java.io.IOException

private const val TAG="MyIAPHelper2"
class MyIAPHelper2(private val receivedActivity: Activity,
                   private val rcvAdapterInstance: RcViewAdapter?,
                   private val myDownloaderInstance: MyDownloader2) :  PurchasesUpdatedListener
{
    var currentRtList: MutableList<RingtoneClass> = ArrayList()

    //Map containing IDs of Products (will replace 'purchaseItemIDsList')
    val itemIDsMap: HashMap<Int,String> = HashMap() // <trackID, productID> ex) <1,p1> <2,p2> ...
    val downloadUrlMap: HashMap<Int, String> = HashMap() // <trackID, mp3URL> ex) <1, http://xxx.xxx> ....


    private val myDiskSearcher: DiskSearcher by globalInject()

    // download 필요한 item 들 목록



    companion object
    {
        const val PREF_FILE = "MyPref"
        val purchaseStatsMap: HashMap<Int,Boolean> = HashMap() // <trackID, Bool> ex) <1,true> <2,false> ...
        val itemPricesMap: HashMap<String, String> = HashMap() // <trackID, price> ex) <1, 1000>, <2, 1200> 현재는 KRW 그러나 지역/국가별 자동 설정 될듯..
        //var filesToDNLDCount : Int = 0 // 전체 다운로드가 필요한 파일 갯수. 해당 숫자를 추후 MyDownloader.kt>preDownloadPrep() 에서 참조하여 진행.
        var myQryPurchListSize : Int = 0
    }

    private var billingClient: BillingClient? = null

    //## <B> A. refreshItemIdsMap-> B. initIAP() -> (wait..) C. onBillingSetupFinished() -> D. refreshPurchaseStatsMap() -> E. refreshItemsPriceMap() =>(finally..) rcvAdapter.refreshRcView()!
    private fun initIAP() // Check which items are in purchase list and which are not in purchase list -> 끝나면 rcView 를 update 해야함..
    {
        Log.d(TAG, "B) initIAP: init starts")
        // Establish connection to billing client
        //check purchase status from google play store cache on every app start
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
                        myQryPurchListSize = queryPurchases.size // 추후 MyDownloader.kt > multiDownloadOrNot() 에서 활용.
                        handlePurchaseNotification(queryPurchases)
                        //check item in purchase list. 구매 상태인 물품에 대해서! status check! 한번 더 확인. 문제없으면 true 로..
                        for (p in queryPurchases)
                        {
                            val trackID = getKeyFromMap(itemIDsMap, p.sku) // p.sku = "productId" = p1, p2...
                            //val trackTitle = currentRtList.filter {  } currentRtList 통해서 받을수있지만 연산시간이 많이 걸리니 차라리 MAP 을 사용? MAP 을 안 썼음 좋겠는데..

                            //if purchase found. 구입 내역이 있는! item 만 나옴 (ex. 현재 21/06/4에는 rt1, rt2 만 여기에 해당됨..)
                            if (trackID > -1)
                            {

                                purchaseFound.add(trackID) //For items that are found(purchased), add them to purchaseFound

                                // ********************************기존 구매건
                                if (p.purchaseState == Purchase.PurchaseState.PURCHASED)
                                {
                                    itemIDsMap[trackID]?.let { savePurchaseItemBoolValueToPref(it, true) } // Shared Pref 에 ex) "p1, true" 로 저장.
                                    Log.d(TAG, "C-1) ☺ onBillingSetupFinished: PurchaseState is PURCHASED for trackID=$trackID, itemName=${itemIDsMap[trackID]}")
                                    downloadHandlerBridge(trackID, true, singlePurchase = false)  //todo: downloadOrDelete(trackId) (O) <- 기존 구매건 관련!
                                    // ********************************기존 구매건
                                } else
                                { // 구매한적이 있으나 뭔가 문제가 생겨서 PurchaseState.Purchased 가 아닐때 여기로 들어옴. 애당초 구입한적이 없는 물품은 여기 뜨지도 않음!
                                    itemIDsMap[trackID]?.let { savePurchaseItemBoolValueToPref(it, false) }
                                    Log.d(TAG, "C-ERROR) ‼ onBillingSetupFinished: PurchaseState is (not)PURCHASED for trackID=$trackID, itemName=${itemIDsMap[trackID]}")
                                    downloadHandlerBridge(trackID, false, singlePurchase = false) //todo: downloadOrDelete(trackId) (X)

                                }
                            }
                        }
                        //C-2) (기존) 구매안된 물품들(굉장히 다수겠지..)에 대해서는 SharedPref 에 false 로 표시!. //items that are not found in purchase list mark false
                        //indexOf returns -1 when item is not in foundlist
                        itemIDsMap.forEach { (k,v) -> if(purchaseFound.indexOf(k) == -1) { // itemIDsMap 에서 "구매한목록(purchaseFound)" 에 없는 놈들은 다 false 로!
                            savePurchaseItemBoolValueToPref(v, false)
                            Log.d(TAG, "C-2) ₥₥₥ onBillingSetupFinished: trackId=$k 물품(상품 이름은=$v)은 purchaseFound 에 없음! 고로 false 로 SharedPref 에 저장됨! ")

                            val fileNameShort = itemIDsMap[k] // p1, p2 등 productId 를 return
                            // 확장자 이름은 혹시 모르니 mp3 대신 rta (ring tone audio)로 변경!.
                            val fileNameAndFullPath = receivedActivity.getExternalFilesDir(null)!!
                                .absolutePath + "/.AlarmRingTones" + File.separator + fileNameShort +".rta" // rta= Ring Tone Audio 내가 만든 확장자..

                            //todo: 다음을 구매 안한 상품들에 대해서 매번 해줘야 한다는 것= too CPU expensive..
                            if(myDiskSearcher.checkDuplicatedFileOnDisk(fileNameAndFullPath)) { // 혹시나..구매한적도 없는데 만약 디스크에 있으면
                                // 디스크에서 삭제
                                Log.d(TAG, "onBillingSetupFinished: $fileNameShort 는(은) 산 놈도 아닌데 하드에 있음. 지워야함!! ")
                                val downloadableItem = DownloadableItem(k, fileNameAndFullPath)
                                myDiskSearcher.deleteFromDisk(downloadableItem)
                            }

                        }}

                    }
                    // C-3) 애당초 구매건이 하나도 없으면. 모두 false!
                    else {
                        itemIDsMap.forEach { (_,v) -> savePurchaseItemBoolValueToPref(v, false)} // MAP iteration.
                        Log.d(TAG, "C-3) ☺ onBillingSetupFinished:  The User has never ever 산적이 없으면 일로 오는듯! (queryPurchase.size 가 0 이란 뜻..?)")
                    }
                }
                refreshPurchaseStatsMap()
                refreshItemsPriceMap()
                Log.d(TAG, "C) onBillingSetupFinished: finished..")
            }

            override fun onBillingServiceDisconnected() {}
        })
        Log.d(TAG, "initIAP: finished..")
    }
    //## <A> A. refreshItemIdsMap-> B. initIAP() -> (wait..) C. onBillingSetupFinished() -> D. refreshPurchaseStatsMap() -> E. refreshItemsPriceMap() =>(finally..) rcvAdapter.refreshRcView()!
    fun refreshItemIdsAndMp3UrlMap(newRtList: MutableList<RingtoneClass>) { // 새로 받은 ringToneList 로 itemIDsMap 을 수정/업뎃해줌. initIAP() 에서 호출됨.
        currentRtList.clear()
        currentRtList = newRtList


        Log.d(TAG, "A) refreshItemIdsMap: begins!")
        for (i in currentRtList.indices) {
            itemIDsMap[currentRtList[i].id] = currentRtList[i].iapName //ex) itemIDsMap[1=trackID] = p1
            downloadUrlMap[currentRtList[i].id] = currentRtList[i].mp3URL //ex) itemIDsMap[1=trackID] = http://www.xxxxx
        }
        initIAP()
    }
    //##<D>  A. refreshItemIdsMap-> B. initIAP() -> (wait..) C. onBillingSetupFinished() -> D. refreshPurchaseStatsMap() -> E. refreshItemsPriceMap() =>(finally..) rcvAdapter.refreshRcView()!
    private fun refreshPurchaseStatsMap() { //Refresh PurchaseStatsMap from the result of refreshItemIdsMap()

        Log.d(TAG, "D) refreshPurchaseStatsMap: begins!")
        for (product in itemIDsMap) { //product = pair = <TrackId,iapName> = <1,p1>
            purchaseStatsMap[product.key] = getPurchaseItemBoolValueFromPref(product.value) // ex) purchaseStatsMap[track id 1번] = true 구매되었을 경우.
            //Log.d(TAG, "D) refreshPurchaseStatsMap: purchase stat of trackId: ${product.key} = ${getPurchaseItemBoolValueFromPref(product.value)}  ")

            // 어차피 Fire 베이스 refresh 될 때 위에서 download/delete 실행해줄테니 여기서 굳이 체크할 필요 없음. fun checkPurchasedItemOnDisk(trackid) ..
        }

    }
    // Download/Delete Manager
    private fun checkPurchasedItemOnDisk(trackId: Int) { // 산놈이면 확실히 다운로드. 안샀거나 취소했으면 반드시 삭제!

    }
    //##<E>  A. refreshItemIdsMap-> B. initIAP() -> (wait..) C. onBillingSetupFinished() -> D. refreshPurchaseStatsMap() -> E. refreshItemsPriceMap() ==> (finally..) rcvAdapter.refreshRcView()!
    private fun refreshItemsPriceMap() {
        Log.d(TAG, "refreshItemsPriceMap: called")
        val itemNameList = ArrayList<String>()

        if(!itemIDsMap.isNullOrEmpty())
        {
            itemIDsMap.forEach { (k, v) -> itemNameList.add(v)} // 1)map 에 있는 value(product id=p1, p2, p3..)를 itemNameList 에 저장-> 이게 myParams.setSkusList(string List!!) 에 사용됨

            val myParams = SkuDetailsParams.newBuilder()
            myParams.setSkusList(itemNameList).setType(BillingClient.SkuType.INAPP)

            billingClient!!.querySkuDetailsAsync(myParams.build()) { myQueryResultYo, mySkuDetailsListYo -> // SAM .. object: SkuDetailsResponseListener 인터페이스 심기와 같음.

                if (myQueryResultYo.responseCode == BillingClient.BillingResponseCode.OK && mySkuDetailsListYo != null)
                {
                    for (skuDetails in mySkuDetailsListYo) {

                        itemPricesMap[skuDetails.sku] = skuDetails.price// itemPricesMap[iapName] = 가격. ex) itemPricesMap[p1]= 1000
                        Log.d(TAG,"E) refreshItemsPriceMap: a) item title=${skuDetails.title} b)item price= ${skuDetails.price}, c)item sku= ${skuDetails.sku}")
                        // logd 결과 예시: a) item title=p1 b)item price= ₩1,000, c)item sku= p1
                    }
                }
                rcvAdapterInstance!!.refreshRecyclerView(currentRtList) // #$#$#$$#$#$!@#$!!#! FINALLY 여기서 rcView 를 업뎃-> onBindView 하게끔! #$#$@!$#@@$@#$#
            }
        } else {
            Log.d(TAG, "E) refreshItemsPriceMap: itemIdsMap is null or empty..")
            Toast.makeText(receivedActivity, "Error Loading Billing Info: Error stage <E>", Toast.LENGTH_SHORT).show()
            rcvAdapterInstance!!.refreshRecyclerView(currentRtList) // #$#$#$$#$#$!@#$!!#! FINALLY 여기서 rcView 를 업뎃-> onBindView 하게끔! #$#$@!$#@@$@#$#
        }

    }

    //Clicked to Purchase
//정상적인 purchase Flow: myOnPurchaseClicked() > initiatePurchase() > launchBillingFlow(구매화면) > onPurchaseUpdated() > handlePurchaseNotification()> downloadHanlderBridge()
    fun myOnPurchaseClicked(trackID: Int) { // position -> trackID: Int..  position 쓰면 Chip 걸린 상태로 리스트 변경되었을 때 다른 물품을 사게되겠찌..
        Log.d(TAG, "myOnPurchaseClicked: ATTEMPTING TO BUY!! 두구두구.. ${itemIDsMap[trackID]}")
        if (itemIDsMap[trackID]?.let { getPurchaseItemBoolValueFromPref(it) } == true) { //selected item is already purchased
            Toast.makeText(receivedActivity, "You already own " +itemIDsMap[trackID], Toast.LENGTH_SHORT).show()
            //Snackbar.make(receivedActivity,"You already own " +itemIDsMap[trackID], Snackbar.LENGTH_SHORT).show()
            //download 잘되있는지 체크?
            return
        }

        //Initiate purchase on selected product item click
        //Check if service is already connected
        if (billingClient!!.isReady) {
            itemIDsMap[trackID]?.let { initiatePurchase(it) } // 계속 이렇게 wrap.. 왜냐면 String? 형태니깐.

        } else {
            billingClient = BillingClient.newBuilder(receivedActivity).enablePendingPurchases().setListener(this@MyIAPHelper2).build()
            billingClient!!.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        itemIDsMap[trackID]?.let { initiatePurchase(it) }
                    } else {
                        Toast.makeText(receivedActivity, "Error " + billingResult.debugMessage, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onBillingServiceDisconnected() {}
            })
        }
    }



    private val preferenceObject: SharedPreferences
        private get() = receivedActivity.getSharedPreferences(PREF_FILE, 0)
    private val preferenceEditObject: SharedPreferences.Editor
        private get() {
            val pref = receivedActivity.getSharedPreferences(PREF_FILE, 0)
            return pref.edit()
        }
    // Saving to Shared Prefs on Disk (Local) ---------->
    // 1) 구매 여부 Bool value (KEY,BOOL) / ex) (p1, true)
    private fun getPurchaseItemBoolValueFromPref(PURCHASE_KEY: String): Boolean {

        //preferenceEditObject.clear().commit() // 강제 지우기!!! 밑에 string 이랑 꼬여서 넣어봤음! 지워 이 line 은!
        return preferenceObject.getBoolean(PURCHASE_KEY, false)
    }

    private fun savePurchaseItemBoolValueToPref(PURCHASE_KEY: String, value: Boolean) { //PURCHASE_KEY = p1, p2, p3....
        preferenceEditObject.putBoolean(PURCHASE_KEY, value).commit()
    }
    //    // 2) 가격 저장... (KEY, PRICE=string) / ex) (p1, ₩1,000)
//    private fun getItemPriceFromPref(iapName: String): String? {
//        return preferenceObject.getString(iapName+"strYo", "N/A")
//    }
//
//    private fun saveItemPriceToPref(iapName: String, value: String) { //PURCHASE_KEY = p1, p2, p3....
//        preferenceEditObject.putString(iapName+"strYo", value).commit()
//    }
//// Saving to Shared Prefs on Disk (Local) <----------
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
    //MAP UTIL functions. 내가 만든. ------------>>>>>
    fun <K,V> getKeyFromMap(mapReceived: Map<K,V>, target: V): K {
        return mapReceived.filter { target == it.value }.keys.first()
    }
//MAP UTIL functions. 내가 만든. <<<------------

    fun handlePurchaseNotification(purchases: List<Purchase>) {
        Log.d(TAG, "handlePurchaseNotification: begins.")

        for (purchase in purchases) {
            val trackId = getKeyFromMap(itemIDsMap, purchase.sku) // too expensive.. 맵에서 value 로 key 찾는게 은근 cpu 소모할듯..? 그러나 다른 방법이 없음.
            // https://www.techiedelight.com/get-key-specified-value-map-kotlin/

            //purchase found
            if (trackId > -1)
            {

                //1) if item is purchased
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED)
                {
                    // 1-a) Invalid purchase
                    if (!verifyValidSignature(purchase.originalJson, purchase.signature))
                    {
                        // show error to user
                        Log.d(TAG, "handlePurchaseNotification: 1-A Invalid purchase. 다운취소???")
                        Toast.makeText(receivedActivity, "Error : Invalid Purchase", Toast.LENGTH_SHORT).show()
                        downloadHandlerBridge(trackId, false, singlePurchase = true) //todo: downloadOrDelete(trackId) (X)
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
                                itemIDsMap[trackId]?.let { savePurchaseItemBoolValueToPref(it, true) }
                                Toast.makeText(receivedActivity,"You have purchased " + itemIDsMap[trackId], Toast.LENGTH_SHORT).show()
                                Log.d(TAG, "handlePurchaseNotification: 1-B 정상구매(not acknowledged). 다운로드 진행 ")
                                // ############################## 신규 구매건. 과거 구매건은 정상 처리되었어도 여기까지는 안 들어옴. (logd 로 확인)
                                downloadHandlerBridge(trackId, true, singlePurchase = true) //todo: downloadOrDelete(trackId) (O)
                                // ############################## 신규 구매건
                                refreshPurchaseStatsMap() // onBindView 에서 여기에 의존!
                                rcvAdapterInstance!!.notifyDataSetChanged()
                                // 왜냐면 단순히 refreshRecyclerView(list) 방식으로는 (차이가 없으므로) (구매여부 ImageView) UI 업뎃 안된다.
                            }
                        }
                    }
                    // 1-C) (일반) 정상 구매. !!과거 구입건이 있을 때 앱 재설치 후 "최초" 실행시 이쪽으로 들어옴.!!
                    else
                    {
                        // Grant entitlement to the user on item purchase
                        if (!itemIDsMap[trackId]?.let { getPurchaseItemBoolValueFromPref(it) }!!) {
                            itemIDsMap[trackId]?.let { savePurchaseItemBoolValueToPref(it, true) }

                            if(myQryPurchListSize == 1) { // A) 정상 신규 구매 한건
                                Log.d(TAG, "handlePurchaseNotification: 1-C-a) 정상 single 구매. 다운로드 진행")
                                // ############################## 신규 구매건. 과거 구매건은 정상 처리되었어도 여기까지는 안 들어옴. (logd 로 확인)
                                Toast.makeText(receivedActivity, "You have purchased " + itemIDsMap[trackId], Toast.LENGTH_SHORT).show()
                                downloadHandlerBridge(trackId, true, singlePurchase = true) //todo: downloadOrDelete(trackId) (O)
                                // ############################## 신규 구매건
                                refreshPurchaseStatsMap() // onBindView 에서 MyIAPHelper.purchaseStatsMap[currentTrId].toString() 에 의존!확인!
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
                    Toast.makeText(receivedActivity, itemIDsMap[trackId] + " 2-A Purchase is Pending. Please complete Transaction", Toast.LENGTH_SHORT).show()
                    downloadHandlerBridge(trackId, false, singlePurchase = true) //todo: downloadOrDelete(trackId) (X)
                }
                else if (purchase.purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE) {
                    //mark purchase false in case of UNSPECIFIED_STATE
                    itemIDsMap[trackId]?.let { savePurchaseItemBoolValueToPref(it, false) }
                    Toast.makeText(receivedActivity, itemIDsMap[trackId] + " Purchase Status Unknown", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "handlePurchaseNotification:  2-B Purchase Status Unknown. 파일 확인후 삭제?")

                    downloadHandlerBridge(trackId, false, singlePurchase = true) //todo: downloadOrDelete(trackId) (X)

                    refreshPurchaseStatsMap() // onBindView 에서 여기에 의존!
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
            //todo: Get a new key!
            val base64Key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjxP65hcVxu3nM/XR89EoZzEwK1itdhPcTOT+itC6Guf5omQLHe3A4cDLlTSjZqoNMy3jzNK7mSiPG8NRTa6waHaaHx3fxatR4Or8KeS8WzFNQsKbFz2OCt3kTRQ5lUuoIvyjj+VjEv9XwyPrFRb8Lxq47KqHnjiJyeBcXznLXD//4YOsTaTp2dBxuLXjJQEzkp4EPgvhNh6BE+bX+SvXRPc3x3dghqAUtdaoM3C77QgCnRc94nYnWyXyQqqX2PvEX3KNKM//nQbKtJbNUB/NpKlzodiY3WdFMVNS3ySw9S9irikhDv7jOQ1OnI+dzKMLCeQIRTxqFHB2RxkqpzOHtQIDAQAB"

            Security.verifyPurchase(base64Key, signedData, signature)
        } catch (e: IOException) {
            false
        }
    }


    // 이전 (신규) 구매건에 대해서 처리-> single/multiDownloadOrNot() 에서 file 디스크에 있는지 체크 및 download 할지 여부를 정함.
    private fun downloadHandlerBridge(trId: Int, keepTheFile: Boolean,singlePurchase: Boolean) {
        //Log.d(TAG, "downloadHandlerBridge: <<<MyDownloader.Kt 로 임무 전달!!> trackId= $trId")
        val fileNameShort = itemIDsMap[trId] // p1, p2 등 productId 를 return
        // 확장자 이름은 혹시 모르니 mp3 대신 rta (ring tone audio)로 변경!.
        val fileNameAndFullPath = receivedActivity.getExternalFilesDir(null)!!
            .absolutePath + "/.AlarmRingTones" + File.separator + fileNameShort +".rta" // rta= Ring Tone Audio 내가 만든 확장자..
        val downloadableItem = DownloadableItem(trId, fileNameAndFullPath)

        if(!keepTheFile && myDiskSearcher.checkDuplicatedFileOnDisk(downloadableItem.filePathAndName)) { 
            // (잘못된 구매 사유 등으로) Keep 할 필요 없는 파일인데 디스크에 있을경우 삭제!
            Log.d(TAG, "downloadHandlerBridge: !![WARNING] Deleting this File!!=${downloadableItem.filePathAndName}")
                myDiskSearcher.deleteFromDisk(downloadableItem)
        }
        else if(singlePurchase && keepTheFile) { // 정상 단독 구매건 다운로드 (sync 와 무관하고 app 에서 한개 샀을 때)
            Log.d(TAG, "downloadHandlerBridge: ***SINGLE PURCHASE!!")
            myDownloaderInstance.singleFileDNLD(downloadableItem)
            return
        }

        //Log.d(TAG, "downloadHandlerBridge: ########### myQryPurchListSize=${myQryPurchListSize},trackId= $trId, toDownloadItem=$downloadableItem")

    }


}