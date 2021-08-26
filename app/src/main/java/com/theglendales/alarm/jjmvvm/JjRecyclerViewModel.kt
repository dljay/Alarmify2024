package com.theglendales.alarm.jjmvvm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.theglendales.alarm.jjmvvm.data.ViewAndTrackIdClass

private const val TAG="JjRecyclerViewModel"

class JjRecyclerViewModel: ViewModel() {
    //val selectedRow = MutableLiveData<Int>() // Private & Mutable
    private val _selectedRow = MutableLiveData<ViewAndTrackIdClass>() // Private & Mutable
    val selectedRow: LiveData<ViewAndTrackIdClass> = _selectedRow

    fun updateLiveData(viewAndTrId: ViewAndTrackIdClass) { _selectedRow.value = viewAndTrId}
}