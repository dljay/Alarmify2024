package com.theglendales.alarm.jjongadd

//import android.app.Fragment
import android.annotation.SuppressLint
import android.media.Ringtone
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment // todo: Keep an eye on this guy..

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.theglendales.alarm.R
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.jjadapters.MyNetWorkChecker
import com.theglendales.alarm.jjadapters.MyOnItemClickListener
import com.theglendales.alarm.jjadapters.RcViewAdapter
import com.theglendales.alarm.jjdata.GlbVars
import com.theglendales.alarm.jjdata.RingtoneClass
import com.theglendales.alarm.jjfirebaserepo.FirebaseRepoClass
import com.theglendales.alarm.jjmvp.JJ_ITF
import com.theglendales.alarm.jjmvp.JJ_Presenter

/**
 * A simple [Fragment] subclass.
 * Use the [SecondFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

private const val TAG="SecondFragment"
class SecondFragment : androidx.fragment.app.Fragment(), MyOnItemClickListener, JJ_ITF.ViewITF  {

    //var fullRtClassList: MutableList<RingtoneClass> = ArrayList()
//    var iapInstance = MyIAPHelper(this,null, ArrayList())
//MVP related
    lateinit var presenter: JJ_ITF.PresenterITF
//RcView Related
    lateinit var rcvAdapterInstance: RcViewAdapter
    lateinit var rcView: RecyclerView

    private val myNetworkCheckerInstance: MyNetWorkChecker by globalInject() // Koin 으로 아래 줄 대체!! 성공!
    //(DEL) private val myNetworkCheckerInstance by lazy { context?.let { MyNetWorkChecker(it) } }
    //private val firebaseRepoInstance: FirebaseRepoClass by globalInject()

//Sliding Panel Related

    lateinit var slidingUpPanelLayout: SlidingUpPanelLayout    //findViewById(R.id.id_slidingUpPanel)  }

    //a) Sliding Panel: Upper Ui

    lateinit var upperUiHolder: LinearLayout // { this.view?.findViewById(R.id.id_upperUi_ll) }  // 추후 이 부분이 fade out
    lateinit var tv_upperUi_title: TextView // { findViewById<TextView>(R.id.id_upperUi_tv_title) }
    lateinit var iv_upperUi_thumbNail: ImageView //  { findViewById<ImageView>(R.id.id_upperUi_iv_coverImage)  }
    lateinit var iv_upperUi_ClickArrow: ImageView //  { findViewById<ImageView>(R.id.id_upperUi_iv_clickarrowUp) }
    lateinit var cl_upperUi_entireWindow: ConstraintLayout //  {findViewById<ConstraintLayout>(R.id.id_upperUi_ConsLayout)}

    //b) lower Ui
    lateinit var constLayout_entire: ConstraintLayout // {findViewById<ConstraintLayout>(R.id.id_lowerUI_entireConsLayout)}
    lateinit var iv_lowerUi_bigThumbnail: ImageView // {findViewById<ImageView>(R.id.id_lowerUi_iv_bigThumbnail)}
    lateinit var tv_lowerUi_about: TextView // { findViewById<TextView>(R.id.id_lowerUi_tv_Description) }

// Basic overridden functions -- >
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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
        rcView = view.findViewById<RecyclerView>(R.id.id_rcV_2ndFrag)
        rcView.layoutManager = LinearLayoutManager(context)

        rcvAdapterInstance = activity?.let { RcViewAdapter(ArrayList(), this, it) }!! // 공갈리스트 넣어서 instance 만듬
        rcView.adapter = rcvAdapterInstance
        rcView.setHasFixedSize(true)
    //RcView <--
        setUpLateInitUis(view)
    //MVP load from firebase.
        presenter = JJ_Presenter(this)
        Log.d(TAG, "onViewCreated: 1) loadFromFb()")
        presenter.loadFromFb()



    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //Log.d(TAG, "onSaveInstanceState: save some shit here!")
    }

//    override fun onViewStateRestored(savedInstanceState: Bundle?) {
//        super.onViewStateRestored(savedInstanceState)
//        Log.d(TAG, "onViewStateRestored: called")
//    }
// <-- Basic Overridden functions

// My Functions ==== >
    private fun setUpLateInitUis(v: View) {
        slidingUpPanelLayout = v.findViewById(R.id.id_slidingUpPanel)
        //a) Sliding Panel: Upper Ui

        upperUiHolder = v.findViewById(R.id.id_upperUi_ll)   // 추후 이 부분이 fade out
        tv_upperUi_title= v.findViewById<TextView>(R.id.id_upperUi_tv_title)
        iv_upperUi_thumbNail= v.findViewById<ImageView>(R.id.id_upperUi_iv_coverImage)
        iv_upperUi_ClickArrow= v.findViewById<ImageView>(R.id.id_upperUi_iv_clickarrowUp)
        cl_upperUi_entireWindow= v.findViewById<ConstraintLayout>(R.id.id_upperUi_ConsLayout)

        //b) lower Ui
        constLayout_entire= v.findViewById<ConstraintLayout>(R.id.id_lowerUI_entireConsLayout)
        iv_lowerUi_bigThumbnail= v.findViewById<ImageView>(R.id.id_lowerUi_iv_bigThumbnail)
        tv_lowerUi_about= v.findViewById<TextView>(R.id.id_lowerUi_tv_Description)

        setUpSlidingPanel()
    }

    private fun setUpSlidingPanel() {
        //slidingUpPanelLayout.setDragView(iv_upperUi_ClickArrow) // 클릭 가능 영역을 화살표(^) 로 제한
        slidingUpPanelLayout.setDragView(cl_upperUi_entireWindow)
        slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.HIDDEN // 일단 클릭전에는 감춰놓기!
        //slidingUpPanelLayout.anchorPoint = 0.6f //화면의 60% 만 올라오게.  그러나 2nd child 의 height 을 match_parent -> 300dp 로 설정해서 이걸 쓸 필요가 없어짐!
        //slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.ANCHORED // 위치를 60%로 초기 시작
        slidingUpPanelLayout.addPanelSlideListener(object :
            SlidingUpPanelLayout.PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                // Panel 이 열리고 닫힐때의 callback

                upperUiHolder.alpha = 1 - slideOffset + 0.3f // +0.3 은 살짝~ 보이게끔

                // 트랙 클릭-> 미니플레이어가 등장! (그 이전에는 offset = -xxx 값임.)
                //Log.d(TAG, "onPanelSlide: slideOffset= $slideOffset, rcvAdapterInstance.itemCount=${rcvAdapterInstance.itemCount}")
                val entireListCount = rcvAdapterInstance.itemCount

                if (slideOffset == 0.0f && GlbVars.clickedTrId == entireListCount) { //마지막 트랙 클릭.
                    rcView.post { // 메인 ui 스레드에서는 다른 업무 처리로 바뻐서 다른 thread (워커스레드?) 를 만들어줌.
                        rcView.smoothScrollBy(0, 300) //제일 밑 트랙을 300dp 위로 밀어줌.
                        // todo: 추후 rcView 사이즈 변경될 때 고려 ->정확한 calculation 필요  https://greedy0110.tistory.com/41
                        Log.d(TAG, "myOnItemClick: 살짝 슬라이드! 마지막 트랙 보이게!")
                        //Log.d(TAG, "onPanelSlide: entirelistcount: $entireListCount")
                    }
                }

                // 완전히 펼쳐질 때
                if (!slidingUpPanelLayout.isOverlayed && slideOffset > 0.2f) { //안겹치게 설정된 상태에서 panel 이 열리는 중 (20%만 열리면 바로 모퉁이 감추기!)
                    //Log.d(TAG, "onPanelSlide: Hiding 모퉁이! yo! ")
                    slidingUpPanelLayout.isOverlayed =true // 모퉁이 edge 없애기 위해. Default 는 안 겹치게 false 값.
                }

            }

            @SuppressLint("ClickableViewAccessibility") // 아래 constLayout_entire.setxx... 이거 장애인 warning 없애기
            override fun onPanelStateChanged(
                panel: View?,
                previousState: SlidingUpPanelLayout.PanelState?,
                newState: SlidingUpPanelLayout.PanelState?
            ) {

                when (newState) {
                    SlidingUpPanelLayout.PanelState.EXPANDED -> {
                        //Log.d(TAG, "onPanelStateChanged: Sliding Panel Expanded")
                        iv_upperUi_ClickArrow.setImageResource(R.drawable.clickarrow_down)// ^ arrow 전환 visibility }

                        // 계속 click 이 투과되는 문제(뒤에 recyclerView 의 버튼 클릭을 함)를 다음과같이 해결. 위에 나온 lowerUi 의 constraint layout 에 touch를 허용.
                        constLayout_entire.setOnTouchListener { v, event -> true }

                    }
                    SlidingUpPanelLayout.PanelState.COLLAPSED -> {
                        //Log.d(TAG, "onPanelStateChanged: Sliding Panel Collapsed")
                        iv_upperUi_ClickArrow.setImageResource(R.drawable.clickarrow)// ^ arrow 전환 visibility }
                        slidingUpPanelLayout.isOverlayed =
                            false // 이렇게해야 rcView contents 와 안겹침 = (마지막 칸)이 자동으로 panel 위로 올라가서 보임.
                    }
                }
            }
        })

        //Title Scroll horizontally. 흐르는 텍스트
        tv_upperUi_title.apply {
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.MARQUEE
            isSelected = true
            //text ="Song Title                                           "
            // text 제목이 일정 수준 이하면 여백을 추가, 추후 title.length < xx => 정확한 카운트 알고리즘.
        }



    }

    private fun loadFromFireBase() {
    // 여기 Frag -> Presenter(JJ_Presenter) -> Model -> 여기 Frag 의 showResult()


    }


    override fun myOnItemClick(v: View, trackId: Int) {

        val ringtoneClassFromtheList = rcvAdapterInstance.getDataFromMap(trackId)
        val ivInside_Rc = v.findViewById<ImageView>(R.id.id_ivThumbnail) // Recycler View 의 현재 row 에 있는 사진을 variable 로 생성

        tv_upperUi_title.text = ringtoneClassFromtheList?.title // miniPlayer(=Upper Ui) 의 Ringtone Title 변경
        tv_upperUi_title.append("                                                 ") // 흐르는 text 위해서. todo: 추후에는 글자 크기 계산-> 정확히 공백 더하기

        // Lower UI
        tv_lowerUi_about.text = ringtoneClassFromtheList?.description

        when(v.id) {
            //1) RcView > 왼쪽 큰 영역(album/title) 클릭했을때 처리.
            R.id.id_rL_including_title_description -> {
                //1) Mini Player 사진 변경 (RcView 에 있는 사진 그대로 옮기기)
                if (ivInside_Rc != null) { // 사실 RcView 가 제대로 setup 되어있으면 무조건 null 이 아님! RcView 클릭한 부분에 View 가 로딩된 상태 (사진 로딩 상태 x)
                    //Log.d(TAG, "myOnItemClick: Now setting Images for lower/upper Uis")
                    iv_upperUi_thumbNail.setImageDrawable(ivInside_Rc.drawable)
                    iv_lowerUi_bigThumbnail.setImageDrawable(ivInside_Rc.drawable)
                }
                //2-1) Mp!! Show mini player & play music right away! + EQ meter fx

//                mpClassInstance.playMusic(this, trackId, v)//*************************************************** Media Player Related *************************
//                Log.d(TAG, "myOnItemClick: temp list !!@#!@#!$@@!$!$!@$ templist = $tempList")

                // 최초 SlidingPanel 이 HIDDEN  일때만 열어주기. 이미 EXPAND 상태로 보고 있다면 Panel 은 그냥 둠
                if (slidingUpPanelLayout.panelState == SlidingUpPanelLayout.PanelState.HIDDEN) {
                    slidingUpPanelLayout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED // Show Panel! 아리러니하게도 .COLLAPSED 가 (위만) 보이는 상태임!
                }
            }
            // 2) 우측 FREE, GET THIS 클릭했을 때 처리.
            R.id.id_cl_entire_Purchase ->
            {
                Log.d(TAG, "myOnItemClick: You probably clicked FREE or GET This")
                // tvGetThis.text = "Clicked!" <-- 이거 에러남. 잘 됐었는데. 희한..
//                iapInstance.myOnPurchaseClicked(trackId)
            }

        }

    }

    override fun showResult(fullRtClassList: MutableList<RingtoneClass>) {
        Log.d(TAG, "showResult: 5) called..Finally! ")
        // 만약 기존에 선택해놓은 row 가 있으면 그쪽으로 이동.
//                mySmoothScroll()


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
    }


}