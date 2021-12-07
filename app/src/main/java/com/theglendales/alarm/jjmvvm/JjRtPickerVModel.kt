package com.theglendales.alarm.jjmvvm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.theglendales.alarm.jjmvvm.data.ViewAndTrIdClass
import com.theglendales.alarm.jjmvvm.util.RtWithAlbumArt

class JjRtPickerVModel : ViewModel() {
    //val selectedRow = MutableLiveData<Int>() // Private & Mutable
    private val _selectedRow = MutableLiveData<RtWithAlbumArt>() // Private & Mutable
    val selectedRow: LiveData<RtWithAlbumArt> = _selectedRow



    fun updateLiveData(rtWithAlbumArt: RtWithAlbumArt) { _selectedRow.value = rtWithAlbumArt}
}