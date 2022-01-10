package com.theglendales.alarm.jjmvvm.iapAndDnldManager

import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjdata.RtInTheCloud
import com.theglendales.alarm.jjmvvm.util.ToastMessenger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
        Log.d(TAG, "init MyIapHelperV3 called. ")
        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build() //todo: 밑에 C .isReady 는 항상 false 네..
    }
    //todo: Try & Catch
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
        return suspendCoroutine { continuation ->
            Log.d(TAG, "iap_C_prepBillingClient: <C> BillingClient Ready(O)")
            billingClient!!.startConnection(object : BillingClientStateListener{
                override fun onBillingSetupFinished(p0: BillingResult) {
                    continuation.resume(p0)
                }
                override fun onBillingServiceDisconnected() {
                    continuation.resumeWithException(Exception("Error <C> Billing Service Disconnectoed"))
                    //todo:  Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                }
            })
        }
    }
    suspend fun iap_D1_addPriceToList() {
        Log.d(TAG, "iap_D1_addPriceToList: <D1> called")
        delay(1500L) // 1.5 초 걸린다고 치고.
        rtListPlusIAPInfo[0].itemPrice="$2,000"
    }
    suspend fun iap_D2_addPurchaseBoolToList() {
        Log.d(TAG, "iap_D2_addPurchaseBoolToList: <D2> called")
        delay(1000L) // 1.0 초 걸린다고 치고.
        rtListPlusIAPInfo[0].purchaseBool = true
    }
    fun iap_E_getFinalList(): MutableList<RtInTheCloud> {
        Log.d(TAG, "iap_E_getFinalList: <E> called")
        return rtListPlusIAPInfo
    }




//**
    override fun onPurchasesUpdated(p0: BillingResult, p1: MutableList<Purchase>?) {
        //TODO("Not yet implemented")
    }

}