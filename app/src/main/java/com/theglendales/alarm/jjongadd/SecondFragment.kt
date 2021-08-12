package com.theglendales.alarm.jjongadd

//import android.app.Fragment
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment // todo: Keep an eye on this guy..

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjadapters.MyNetWorkChecker
import com.theglendales.alarm.jjadapters.MyOnItemClickListener
import com.theglendales.alarm.jjadapters.RcViewAdapter
import com.theglendales.alarm.jjdata.RingtoneClass
import com.theglendales.alarm.jjfirebaserepo.FirebaseRepoClass

/**
 * A simple [Fragment] subclass.
 * Use the [SecondFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

private const val TAG="SecondFragment"
class SecondFragment : androidx.fragment.app.Fragment(), MyOnItemClickListener  {

    var fullRtClassList: MutableList<RingtoneClass> = ArrayList()
//    var iapInstance = MyIAPHelper(this,null, ArrayList())
    lateinit var rcvAdapterInstance: RcViewAdapter
    private val myNetworkCheckerInstance: MyNetWorkChecker by globalInject() // Koin 으로 아래 줄 대체!! 성공!
    //(DEL) private val myNetworkCheckerInstance by lazy { context?.let { MyNetWorkChecker(it) } }
    private val firebaseRepoInstance: FirebaseRepoClass = FirebaseRepoClass()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment

        Log.d(TAG, "onCreateView: jj- lineNumberTest.. ")
        val view: View = inflater.inflate(R.layout.fragment_second, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated: jj- begins..")
        super.onViewCreated(view, savedInstanceState)
    //RcView-->
        val rcView = this.view?.findViewById<RecyclerView>(R.id.id_rcV_2ndFrag)
        rcView?.layoutManager = LinearLayoutManager(context)

        rcvAdapterInstance = activity?.let { RcViewAdapter(ArrayList(), this, it) }!! // 공갈리스트 넣어서 instance 만듬
        rcView?.adapter = rcvAdapterInstance
        rcView?.setHasFixedSize(true)
    //RcView <--
        loadFromFireBase()


    }
    private fun loadFromFireBase() {
    //1. 인터넷 가능한지 체크
        //인터넷되는지 체크

        val isInternetAvailable: Boolean = myNetworkCheckerInstance!!.isNetWorkAvailable()
        if(!isInternetAvailable) { // 인터넷 사용 불가!
            Log.d(TAG, "loadFromFireBase: jj- isInternetAvailable= $isInternetAvailable")
            //lottieAnimController(1)
            return //더이상 firebase 로딩이고 나발이고 진행 안함!!
        }
        else {Log.d(TAG, "loadFromFireBase: jj- isInternetAvailable = $isInternetAvailable") }


    //2. If we have internet connectivity, then call FireStore!
        firebaseRepoInstance.getPostList().addOnCompleteListener {
            if(it.isSuccessful)
            {
                Log.d(TAG, "<<<<<<<<<loadPostData: successful")

                // 만약 기존에 선택해놓은 row 가 있으면 그쪽으로 이동.
//                mySmoothScroll()

                fullRtClassList = it.result!!.toObjects(RingtoneClass::class.java)
                // IAP related: Initialize IAP and send instance <- 이게 시간이 젤 오래걸리는듯.

//                iapInstance = MyIAPHelper(this, rcvAdapterInstance, fullRtClassList) //reInitialize
//                iapInstance.refreshItemIdsAndMp3UrlMap() // !!!!!!!!!!!!!!여기서 일련의 과정을 거쳐서 rcView 화면 onBindView 까지 해줌!!


                // Update MediaPlayer.kt
//                mpClassInstance.createMp3UrlMap(fullRtClassList)

                // Update Recycler View
                rcvAdapterInstance.updateRecyclerView(fullRtClassList) // todo: 추후 // comment 시킬것. MyIAPHelper.kt 에서 해주기로 함!
                rcvAdapterInstance.updateRingToneMap(fullRtClassList)// todo: 이 map 안 쓰이는것 같은데 흐음.. (우리는 Map 기반이므로 list 정보를 -> 모두 Map 으로 업데이트!)

                // SwipeRefresh 멈춰 (aka 빙글빙글 animation 멈춰..)
//                if(swipeRefreshLayout.isRefreshing) {
//                    Log.d(TAG, "loadPostData: swipeRefresh.isRefreshing = true")
//                    swipeRefreshLayout.isRefreshing = false
//                }
                // 우선 lottie Loading animation-stop!!
//                lottieAnimController(2) //stop!

            } else { // 문제는 인터넷이 없어도 이쪽으로 오지 않음. always 위에 if(it.isSuccess) 로 감.
                Log.d(TAG, "<<<<<<<loadPostData: ERROR!! Exception message: ${it.exception!!.message}")
//                lottieAnimController(1) // this is useless at the moment..
            }
        }
    }


    override fun myOnItemClick(v: View, trackId: Int) {

        val ringtoneClassFromtheList = rcvAdapterInstance.getDataFromMap(trackId)
        val ivInside_Rc = v.findViewById<ImageView>(R.id.id_ivThumbnail) // Recycler View 의 현재 row 에 있는 사진을 variable 로 생성

//        tv_upperUi_title.text = ringtoneClassFromtheList?.title // miniPlayer(=Upper Ui) 의 Ringtone Title 변경
//        tv_upperUi_title.append("                                                 ") // 흐르는 text 위해서. todo: 추후에는 글자 크기 계산-> 정확히 공백 더하기
//
//        // Lower UI
//        tv_lowerUi_about.text = ringtoneClassFromtheList?.description
//
//        when(v.id) {
//            //1) RcView > 왼쪽 큰 영역(album/title) 클릭했을때 처리.
//            R.id.id_rL_including_title_description -> {
//                //1) Mini Player 사진 변경 (RcView 에 있는 사진 그대로 옮기기)
//                if (ivInside_Rc != null) { // 사실 RcView 가 제대로 setup 되어있으면 무조건 null 이 아님! RcView 클릭한 부분에 View 가 로딩된 상태 (사진 로딩 상태 x)
//                    //Log.d(TAG, "myOnItemClick: Now setting Images for lower/upper Uis")
//                    iv_upperUi_thumbNail.setImageDrawable(ivInside_Rc.drawable)
//                    iv_lowerUi_bigThumbnail.setImageDrawable(ivInside_Rc.drawable)
//                }
//
//                //2-1) Mp!! Show mini player & play music right away! + EQ meter fx
//
//                mpClassInstance.playMusic(this, trackId, v)//*************************************************** Media Player Related *************************
//
////                Log.d(TAG, "myOnItemClick: temp list !!@#!@#!$@@!$!$!@$ templist = $tempList")
//
//                // 최초 SlidingPanel 이 HIDDEN  일때만 열어주기. 이미 EXPAND 상태로 보고 있다면 Panel 은 그냥 둠
//                if (slidingUpPanelLayout.panelState == SlidingUpPanelLayout.PanelState.HIDDEN) {
//                    slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED // Show Panel! 아리러니하게도 .COLLAPSED 가 (위만) 보이는 상태임!
//                }
//
//            }
//            // 2) 우측 FREE, GET THIS 클릭했을 때 처리.
//            R.id.id_cl_entire_Purchase ->
//
//            {
//                Log.d(TAG, "myOnItemClick: You probably clicked FREE or GET This")
//                // tvGetThis.text = "Clicked!" <-- 이거 에러남. 잘 됐었는데. 희한..
//                iapInstance.myOnPurchaseClicked(trackId)
//            }
//
//        }

    }


}