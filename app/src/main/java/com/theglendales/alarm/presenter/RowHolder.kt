package com.theglendales.alarm.presenter

import android.view.View
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.Layout
import com.theglendales.alarm.view.DigitalClock

/**
 * Created by Yuriy on 05.08.2017.
 */
class RowHolder(view: View, alarmIdReceived: Int, val layout: Layout) : RecyclerView.ViewHolder(view) {
    val digitalClock: DigitalClock
    val digitalClockContainer: View
    val rowView: View = view
    val onOff: CompoundButton
    //val onOff: MaterialButton

    val container: View
    val alarmId: Int = alarmIdReceived
    val detailsButton: View
    val idHasChanged: Boolean
// 내가 추가->
    val swipeDeleteContainer: LinearLayout
    val albumArtContainer: LinearLayout//Album Art container (LinearLayout)
    val albumArt: ImageView// Album Art 추가 (detailsButton 대체 <== '...' 요렇게 생긴 놈.)
    val swipeDeleteIcon: ImageView // Swipe 했을 때  Delete 하는 버튼
    val tvSun: TextView
    val tvMon: TextView
    val tvTue: TextView
    val tvWed: TextView
    val tvThu: TextView
    val tvFri: TextView
    val tvSat: TextView

    init {
        digitalClock = find(R.id.list_row_digital_clock) as DigitalClock
        digitalClockContainer = find(R.id.list_row_digital_clock_container)
        onOff = find(R.id.list_row_on_off_switch) as CompoundButton
        //onOff = find(R.id.list_row_on_off_switch) as MaterialButton


        container = find(R.id.list_row_on_off_checkbox_container)
        detailsButton = find(R.id.details_button_container) // ' ... ' 이렇게 생긴 놈. -> 지금은 album art 로 대체되어 있음.
        val prev: RowHolder? = rowView.tag as RowHolder?
        idHasChanged = prev?.alarmId != alarmIdReceived
        rowView.tag = this

    // 내가 추가->
        swipeDeleteContainer = find(R.id.ll_swipeDeleteContainer) as LinearLayout
        digitalClockContainer.tag = this
        albumArtContainer = find(R.id.ll_albumArt_Container) as LinearLayout
        albumArtContainer.tag = this
        albumArt = find(R.id.id_row_albumArt) as ImageView
        swipeDeleteIcon = find(R.id.iv_swipe_deleteIcon) as ImageView

        tvSun = find(R.id._tvSun) as TextView
        tvMon = find(R.id._tvMon) as TextView
        tvTue = find(R.id._tvTue) as TextView
        tvWed = find(R.id._tvWed) as TextView
        tvThu = find(R.id._tvThu) as TextView
        tvFri = find(R.id._tvFri) as TextView
        tvSat = find(R.id._tvSat) as TextView
    // 내가 추가<-
        // 입력받는 id 를 활용해서 해당 알람이 설정해놓은 Album Art 이미지 찾기.
    }

    private fun find(id: Int): View = rowView.findViewById(id)

    fun fillInEmptyVHolder(emptyRowHolder: RowHolder) {

    }
}