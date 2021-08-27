package com.theglendales.alarm.jjmvvm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.theglendales.alarm.jjmvvm.data.ViewAndTrIdClass

private const val TAG="JjRecyclerViewModel"

class JjRecyclerViewModel: ViewModel() {
    //val selectedRow = MutableLiveData<Int>() // Private & Mutable
    private val _selectedRow = MutableLiveData<ViewAndTrIdClass>() // Private & Mutable
    val selectedRow: LiveData<ViewAndTrIdClass> = _selectedRow

    fun updateLiveData(viewAndTrId: ViewAndTrIdClass) { _selectedRow.value = viewAndTrId}
}