package com.theglendales.alarm.jjmvvm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

private const val TAG="JjRecyclerViewModel"

class JjRecyclerViewModel: ViewModel() {
    val selectedRow = MutableLiveData<Int>() // Private & Mutable

}