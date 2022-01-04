package com.theglendales.alarm.jjmvvm

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


private const val TAG="JjNetworkCheckVModel"
class JjNetworkCheckVModel : ViewModel() {

    init {Log.d(TAG, "called.. ")}

    //StateFlow
    private val _isNtWorking = MutableStateFlow<Boolean>(false)
    val isNtWorking: StateFlow<Boolean> = _isNtWorking

    fun updateNtViaFlow(isAvailable: Boolean) {
        Log.d(TAG, "updateNtViaFlow: called. isAvailable= $isAvailable")
        viewModelScope.launch {
            _isNtWorking.value = isAvailable
        }
    }


    //LiveData
    private val _isNetworkAvailable = MutableLiveData<Boolean>() // Private& Mutable
    val isNetworkAvailable: LiveData<Boolean> = _isNetworkAvailable // Public but! Immutable (즉 이놈은 언제나= _isNetworkAvailable)

    fun updateNetworkAvailability(isAvailable: Boolean) { //SecondFrag.kt 의 observer 에게 전달.
        Log.d(TAG, "updateNetworkAvailability: isAvailable=$isAvailable")
            _isNetworkAvailable.postValue(isAvailable)
    }



}