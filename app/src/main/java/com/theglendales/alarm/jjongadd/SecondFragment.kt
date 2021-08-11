package com.theglendales.alarm.jjongadd

import android.os.Bundle
import androidx.fragment.app.Fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.theglendales.alarm.R
import com.theglendales.alarm.jjadapters.RcViewAdapter


/**
 * A simple [Fragment] subclass.
 * Use the [SecondFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SecondFragment : Fragment() {

//JJ- RcView related ->
    lateinit var rcView: RecyclerView
    val rcvAdapterInstance = RcViewAdapter(ArrayList(), this, this.activity) // 공갈리스트 넣어서 instance 만듬
    val layoutManager = LinearLayoutManager(this.context)
//JJ- RcView related <-

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_second, container, false)

    // List frag 로 돌아가는 버튼.
        //view.findViewById<Button>(R.id.id_btn_backToListFrag).setOnClickListener { onBackToListFragClicked() }
        return view
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

// 추가
//    private fun onBackToListFragClicked() {
//        //
//    }

//    companion object {
//        /**
//         * Use this factory method to create a new instance of
//         * this fragment using the provided parameters.
//         *
//         * @param param1 Parameter 1.
//         * @param param2 Parameter 2.
//         * @return A new instance of fragment SecondFragment.
//         */
//        // TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//            SecondFragment().apply {
//                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
//                }
//            }
//    }
}