package com.theglendales.alarm.jjmvvm.iapAndDnldManager

import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import kotlinx.coroutines.delay
import kotlin.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG="MyIAPHelperV3"

class MyIAPHelperV3(val context: Context ) : PurchasesUpdatedListener {
// ToastMessenger
    private val toastMessenger: ToastMessenger by globalInject()
// IAP Related
    var rtListPlusIAPInfo= mutableListOf<RtInTheCloud>()
    private var billingClient: BillingClient? = null

    init {
        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build() //todo: 왜 init 이 안되었지?
    }
    //todo: Try & Catch

    suspend fun iapManager(rtListFromFb: MutableList<RtInTheCloud>): MutableList<RtInTheCloud> {
        //<0> 우선 받은 리스트를 variable 에 복붙.
        rtListPlusIAPInfo = rtListFromFb
        Log.d(TAG, "iapManager: <0> called. \n rtListPlusIAPInfo[0].itemPrice=${rtListPlusIAPInfo[0].itemPrice}")

        //<1> BillingClient Connection 확인
        Log.d(TAG, "iapManager: <1> called. \n rtListPlusIAPInfo[0].itemPrice=${rtListPlusIAPInfo[0].itemPrice}")
        val billingResult: BillingResult = iap1_prepBillingClient()

        //<1-a> Billing Client: Start Connection
        if(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
        {
        Log.d(TAG, "iapManager: <1-a> called \n rtListPlusIAPInfo[0].itemPrice=${rtListPlusIAPInfo[0].itemPrice}")
            //<2> Add price Info to our List
            iap2_addPriceToList()
            //<3> Add purchaseBool Info to our list
            iap3_addPurchaseBoolToList()
            //<4> 위의 <2>+<3> 이 끝났으면 list 를 반환
            Log.d(TAG, "iapManager: <4> Called. \n rtListPlusIAPInfo[0].itemPrice=${rtListPlusIAPInfo[0].itemPrice}")

        }else {
            Log.d(TAG, "iapManager: <1-b> called")
            toastMessenger.showMyToast("Failed to get IAP info", isShort = false)
        }


        return rtListPlusIAPInfo
    }

    private suspend fun iap1_prepBillingClient(): BillingResult {
        if(!billingClient!!.isReady) {
            Log.d(TAG, "iap1_prepBillingClient: <1> BillingClient Not Ready(X)! Re init!")
            billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build()
        }
        return suspendCoroutine { continuation ->
            Log.d(TAG, "iap1_prepBillingClient: <1> BillingClient Ready(O)")
            billingClient!!.startConnection(object : BillingClientStateListener{
                override fun onBillingSetupFinished(p0: BillingResult) {
                    continuation.resume(p0)
                }
                override fun onBillingServiceDisconnected() {
                    continuation.resumeWithException(Exception("Error <1> Billing Service Disconnectoed"))
                }
            })
        }
    }
    private suspend fun iap2_addPriceToList() {
        Log.d(TAG, "iap2_addPriceToList: <2> called")
        delay(1500L) // 1.5 초 걸린다고 치고.
        rtListPlusIAPInfo[0].itemPrice="$2,000"
    }
    private suspend fun iap3_addPurchaseBoolToList() {
        Log.d(TAG, "iap3_addPurchaseBoolToList: <3> called")
        delay(1000L) // 1.0 초 걸린다고 치고.
        rtListPlusIAPInfo[0].purchaseBool = true
    }




//**
    override fun onPurchasesUpdated(p0: BillingResult, p1: MutableList<Purchase>?) {
        //TODO("Not yet implemented")
    }

}