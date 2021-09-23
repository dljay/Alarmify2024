package com.theglendales.alarm.jjmvvm.spinner

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.theglendales.alarm.R

private const val TAG="SpinnerAdapter"
class SpinnerAdapter(val context: Context) : BaseAdapter() {
    val rtOnDiskList= mutableListOf<Uri>()

    override fun getCount(): Int {
        Log.d(TAG, "getCount: ")
        return rtOnDiskList.size
    }

    override fun getItem(position: Int): Any {
        Log.d(TAG, "getItem: position=$position")
        return position
    }

    override fun getItemId(position: Int): Long {
        Log.d(TAG, "getItemId: position.toLong= ${position.toLong()}")
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        Log.d(TAG, "getView: called.")
        val rootView: View = LayoutInflater.from(context).inflate(R.layout.item_rt_on_disk, parent, false)

        val tvName = rootView.findViewById<TextView>(R.id.item_name)
        val ivImage = rootView.findViewById<ImageView>(R.id.item_image)

//        tvName.setText(rtOnDiskList.get(position).name)
//        ivImage.setImageResource(rtOnDiskList.get(position).imageInt)

        return rootView
    }
}