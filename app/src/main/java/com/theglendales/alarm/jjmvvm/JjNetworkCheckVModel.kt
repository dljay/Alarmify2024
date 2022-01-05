package com.theglendales.alarm.jjmvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


private const val TAG="JjNetworkCheckVModel"
class JjNetworkCheckVModel : ViewModel() {

    init {Log.d(TAG, "init.. ")}

    //StateFlow
    private val _isNtWorking = MutableStateFlow<Boolean>(true)
    val isNtWorking = _isNtWorking.asStateFlow() // 그냥 = _isNtWorking  이렇게 써도 되는데 그냥 이런 function 이 있네.

    fun updateNtAvailabilityByFlow(isAvailable: Boolean) {
        Log.d(TAG, "updateNtAvailabilityByFlow: called. isAvailable= $isAvailable")
        //viewModelScope -> viewModel 의 생명주기를 따름 (=SecondFrag 의 생명주기 -> 즉 SecondFrag 가 onDestroy() 될 때 같이 소멸됨!)
        viewModelScope.launch {
            //Log.d(TAG, "updateNtAvailabilityByFlow: [FLOW] updating _isNtWorking.value(=$isAvailable)")
            _isNtWorking.value = isAvailable
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: cleared!")
    }



}