package com.theglendales.alarm.jjmvvm.data

import android.view.View
import androidx.recyclerview.widget.RecyclerView

// 이놈은 RcView 로부터 클릭된 ViewHolder 안의 View 와 trackId 를 받게됨.
data class ViewAndTrIdClass(val view: View, val trId: Int) {
}