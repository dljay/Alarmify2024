package com.theglendales.alarm.jjongadd

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.theglendales.alarm.R
import com.theglendales.alarm.jjadapters.MyOnItemClickListener
import com.theglendales.alarm.jjadapters.RcViewAdapter
import com.theglendales.alarm.jjdata.RingtoneClass


/**
 * A simple [Fragment] subclass.
 * Use the [SecondFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
private const val TAG="SecondFragment"
class SecondFragment : Fragment(), MyOnItemClickListener  {

    var fullRtClassList: MutableList<RingtoneClass> = ArrayList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        Log.d(TAG, "onCreateView: jj- .. ")

        val view: View = inflater.inflate(R.layout.fragment_second, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated: jj- begins..")
        super.onViewCreated(view, savedInstanceState)
    //RcView-->
        val rcView = this.view?.findViewById<RecyclerView>(R.id.id_rcV_2ndFrag)
        rcView?.layoutManager = LinearLayoutManager(context)

        val rcvAdapterInstance = activity?.let { RcViewAdapter(ArrayList(), this, it) } // 공갈리스트 넣어서 instance 만듬
        rcView?.adapter = rcvAdapterInstance
        rcView?.setHasFixedSize(true)
        // Fake FullrtClassList
        val rtOneFake = RingtoneClass("titleYo","tags","descriptionYo","imageURL","mp3Url",0,"iapName")
        fullRtClassList.add(rtOneFake)

        rcvAdapterInstance?.updateRecyclerView(fullRtClassList)
    //RcView <--



    }


    override fun myOnItemClick(v: View, trackId: Int) {
        Toast.makeText(this.context,"myOnItemClick",Toast.LENGTH_SHORT).show()
    }


}