package com.theglendales.alarm.jjmvvm

import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.theglendales.alarm.jjmvvm.data.ViewAndTrIdClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG="JjRecyclerViewModel"

class JjRecyclerViewModel: ViewModel() {
    //val selectedRow = MutableLiveData<Int>() // Private & Mutable
//    private val _selectedRow = MutableLiveData<ViewAndTrIdClass>() // Private & Mutable
//    val selectedRow: LiveData<ViewAndTrIdClass> = _selectedRow

    //StateFlow

    val emptyVTrIDClass = ViewAndTrIdClass(null,0)
    private val _selectedRow = MutableStateFlow<ViewAndTrIdClass>(emptyVTrIDClass)
    val selectedRow = _selectedRow.asStateFlow() // 그냥 = _isNtWorking  이렇게 써도 되는데 그냥 이런 function 이 있네.

    fun updateLiveData(viewAndTrId: ViewAndTrIdClass) { _selectedRow.value = viewAndTrId}
}