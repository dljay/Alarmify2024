package com.theglendales.alarm.jjadapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.theglendales.alarm.jjmvvm.JjRtPickerVModel
import com.theglendales.alarm.jjmvvm.util.RtWithAlbumArt
import kotlin.math.sign

private const val TAG="RtPickerAdapter"
class RtPickerAdapter(var rtaArtPathList: MutableList<RtWithAlbumArt>,
                      rcVModelVModel: JjRtPickerVModel) : RecyclerView.Adapter<RtPickerAdapter.RtPickerVHolder>()
{
// Override Methods
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RtPickerVHolder {
        val xmlToViewObject = LayoutInflater.from(this).inflate()
    }

    override fun onBindViewHolder(holder: RtPickerVHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }
// My Methods
    fun updateRcV(newList: MutableList<RtWithAlbumArt>) {
        val oldList= rtaArtPathList // Constructor 로 받은 리스트
        val diffResult: DiffUtil.DiffResult = DiffUtil.calculateDiff(JjDiffCallback(oldList, newList))
        rtaArtPathList = newList
    Log.d(TAG, "updateRcV: !!!!!!! rtaArtPathList.size (AFTER DIFF UTIL) = ${rtaArtPathList.size}")
        diffResult.dispatchUpdatesTo(this)
    }
// DiffUtil Class
    class JjDiffCallback(var oldList: MutableList<RtWithAlbumArt>, var newList: MutableList<RtWithAlbumArt>) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // check if two items represent the same item. 흠.. 다 똑같은지말고 id 만 같아도 true 라는듯..
        return (oldList[oldItemPosition].rtTitle == newList[newItemPosition].rtTitle)
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // 1차 선발된 놈들을 2차로 여기서 아예 동일한 놈인지(data 로 파악) 판명. // 모든 Field 가 아예 똑같은건지 확인 (rtTitle, rtaFilePath 등..)
        return (oldList[oldItemPosition] == newList[newItemPosition])
    }

}
// Inner Class

    inner class RtPickerVHolder(xmlToView: View) : RecyclerView.ViewHolder(xmlToView), View.OnClickListener {

        override fun onClick(v: View?) {
            //TODO("Not yet implemented")
        }

    }


}