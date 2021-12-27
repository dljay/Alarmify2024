package com.theglendales.alarm.jjmvvm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.theglendales.alarm.jjmvvm.util.RtOnThePhone

class JjRtPickerVModel : ViewModel() {
    //val selectedRow = MutableLiveData<Int>() // Private & Mutable
    private val _selectedRow = MutableLiveData<RtOnThePhone>() // Private & Mutable
    val selectedRow: LiveData<RtOnThePhone> = _selectedRow



    fun updateLiveData(rtOnThePhone: RtOnThePhone) { _selectedRow.value = rtOnThePhone}
}