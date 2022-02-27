package com.theglendales.alarm.jjmvvm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.theglendales.alarm.jjdata.RtInTheCloud
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG="JjHelpUsVModel"

class JjHelpUsVModel : ViewModel() {

// Dummy List<RtInTheCloud>
    private val rtDonationA = RtInTheCloud(iapName = "donationIap1", itemPrice = "$1.99")
    private val rtDonationB = RtInTheCloud(iapName = "donationIap2", itemPrice = "$4.99")
    private val rtWithPriceInfo = listOf<RtInTheCloud>(rtDonationA, rtDonationB)
// Price 받기 (Donation 이지만 사실상 MyIAPHelperV3.kt 에서 RtCloud 리스트받고 결제하는것과 동일!!)
    private val _rtListLiveData = MutableStateFlow<List<RtInTheCloud>>(rtWithPriceInfo)
    val rtListLiveData: StateFlow<List<RtInTheCloud>> = _rtListLiveData.asStateFlow()

    init {
        // IAP INIT -> Price Update
        getPrices()
    }

    //1) 가격을 받고 HelpOurTeamActivity.kt 에 알려서 Chip 에 적힌 가격 update
    private fun getPrices() {
        // IAP 에서 쭉 해서 rtList 받음
        _rtListLiveData.value = rtWithPriceInfo
    }

    //2) Chip 을 클릭했을 때 결제 처리.

}