package com.theglendales.alarm.jjmvvm.permissionAndDownload

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat



/**
 * 현재(21.12.24) 로선 내 알람앱에서는 별도의 Permission 확인이 필요없는것처럼 보이기는 함.(App 내부 폴더만 Read/Download 하는정도여서)
 * Permission_안내 Fragment 도 현재 Obj 상태로 잘 열리니 일단 그냥 둠.
 * 이 페이지에 있는 코드들도 추후 분명 다시 쓸 수 있어서 남겨둠.
 */

private const val TAG="MyPermissionHandler"
class MyPermissionHandler(val receivedActivity: Activity) : ActivityCompat.OnRequestPermissionsResultCallback
{
    private val MY_READ_PERMISSION_CODE = 812 // Download(Write) 후 파일 읽을때. WRITE PERMISSION 하면 자동으로 Permission Granted!
    private val MY_WRITE_PERMISSION_CODE = 2480 // Download 용
    private val myBtmShtObjInst = BtmSheetPermission // OBJECT 로 만들었음! BottomSheet 하나만 뜨게하기 위해!
    //private var needToDownloadList = mutableListOf<DownloadableItem>()

    //1-a> Permission to Write (앱 최초 설치시 등장)
    fun permissionToWriteOnInitialLaunch() {

        Log.d(TAG, "permissionToWriteOnInitialLaunch: called")

        Log.d(TAG, "permissionToWriteOnInitialLaunch: API higher >>>than 23. API LVL= ${Build.VERSION.SDK_INT}")
        if (ContextCompat.checkSelfPermission(receivedActivity.applicationContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
        { // 1) 권한 허용 안된 상태
            if (ActivityCompat.shouldShowRequestPermissionRationale(receivedActivity,android.Manifest.permission.WRITE_EXTERNAL_STORAGE))// 이전 거부한적있으면 should..()가 true 반환
            {// 1-a) 이전에 한번 거부한적이 있는경우 showBottomDialog() 함수를 실행
                Log.d(TAG, "permissionToWriteOnInitialLaunch: 1-a) 이전에 한번 거부한적이 있다.")

                // bottomSheet 두번 뜨는것 방지위해 다음 코드를 넣었음!

                val fm = myBtmShtObjInst.fragmentManager
                if(fm==null) {
                    Log.d(TAG, "permissionToWriteOnInitialLaunch: fm is null==bottomSheet NOT(XX) displayed or prepared!!!!")
                }
                if(fm!=null) { // BottomSheet 이 display 된 상태.
                    Log.d(TAG, "permissionToWriteOnInitialLaunch: fm not null..something is(OO) displayed already")
                    val fragTransaction = fm.beginTransaction()
                    fm.executePendingTransactions()
                    //fm 에서의 onCreateView/Dialog() 작용은 Asynchronous 기 때문에. <-요기 executePending() 을 통해서 다 실행(?)한 후.에.야 밑에 .isAdded 에 걸림.
                }

                if(!myBtmShtObjInst.isAdded) {//아무것도 display 안된 상태.
                    myBtmShtObjInst.showBtmPermDialog(receivedActivity)
                    Log.d(TAG, "permissionToWriteOnInitialLaunch: ***DISPLAY 벤치휭~ BOTTOM SHEET NOW!! .isAdded= FALSE!!..")
                }

            } else { // 1-b) 최초로 권한 요청!
                //권한 요청
                Log.d(TAG, "permissionToWriteOnInitialLaunch: 1-b) 처음으로 권한 요청드립니다!!")
                reqPermToWrite()
            }
        } else { // 2) 이미 권한 허용이 된 상태
            Log.d(TAG, "permissionToWriteOnInitialLaunch: 2) 이미 권한이 허용된 상태!")
            myBtmShtObjInst.removePermBtmSheetAndResume()

        }
    }
    private fun reqPermToWrite() {
        ActivityCompat.requestPermissions(receivedActivity, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_WRITE_PERMISSION_CODE)
        // 폰에 기본으로 설정된 "xx Permission 허락합니까?" Dialog -> 여기서 결과 Y/N 여부에 따라 아래 onRequestPermissionsResult 로 반응.
    }
    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray)
    {
        Log.d(TAG, "onRequestPermissionsResult: INSIDE PermissionHandler.kt!")
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            MY_WRITE_PERMISSION_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){ //Permission 허용 Yes 했을 때
                    Log.d(TAG, "onRequestPermissionsResult: Permission Allowed(O)!!")
                    myBtmShtObjInst.removePermBtmSheetAndResume() //todo : 이 줄 없애도 될듯.. 여기서 보여주지도 않은 벤치췽~ bottomsht 왜 없애?
                }
                else if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED){ //Permission 허용 No 했을 때
                    Log.d(TAG, "onRequestPermissionsResult: PERMISSION_DENIED (or BACKGROUND clicked.)")
                    //todo: LiveData -> SecondFrag 로 "Permission Denied" 되었음 전달?
                    if(!myBtmShtObjInst.isAdded) {//아무것도 display 안된 상태.
                        Log.d(TAG, "permissionToWrite: ***DISPLAY 벤치휭~ BOTTOM SHEET NOW!! .isAdded= FALSE!!..")
                        myBtmShtObjInst.showBtmPermDialog(receivedActivity) //Settings & Cancel 갈 수 있는 BottomFrag
                    }
                }
                else {
                    Log.d(TAG, "onRequestPermissionsResult: Permission DENIED!!(XXX)..") //기타? 아마도 No 인듯..
                    Toast.makeText(receivedActivity,"Permission Denied.", Toast.LENGTH_LONG).show()
                }
            }

        }

    }
// 이전 IAP->Download <-> Permission 연결되었을때 코드 (21.12.26 전)
/*    fun permissionForSingleDNLD(receivedDownloadableItem: DownloadableItem) {
        Log.d(TAG, "permissionForSingleDNLD: Downloadable Item =$receivedDownloadableItem")

        Log.d(TAG, "permissionForSingleDNLD: API higher >>>than 23. API LVL= ${Build.VERSION.SDK_INT}")
        if (ContextCompat.checkSelfPermission(receivedActivity.applicationContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
        { // 1) 권한 허용 안된 상태
            if (ActivityCompat.shouldShowRequestPermissionRationale(receivedActivity,android.Manifest.permission.WRITE_EXTERNAL_STORAGE))// 이전 거부한적있으면 should..()가 true 반환
            {// 1-a) 이전에 한번 거부한적이 있는경우 showBottomDialog() 함수를 실행
                Log.d(TAG, "permissionForSingleDNLD: 1-a) 이전에 한번 거부한적이 있다.")

                // bottomSheet 두번 뜨는것 방지위해 다음 코드를 넣었음!

                val fm = myBtmShtObjInst.fragmentManager
                if(fm==null) {
                    Log.d(TAG, "permissionForSingleDNLD: fm is null==bottomSheet NOT(XX) displayed or prepared!!!!")
                }
                if(fm!=null) { // BottomSheet 이 display 된 상태.
                    Log.d(TAG, "permissionForSingleDNLD: fm not null..something is(OO) displayed already")
                    val fragTransaction = fm.beginTransaction()
                    fm.executePendingTransactions()
                    //fm 에서의 onCreateView/Dialog() 작용은 Asynchronous 기 때문에. <-요기 executePending() 을 통해서 다 실행(?)한 후.에.야 밑에 .isAdded 에 걸림.
                }

                if(!myBtmShtObjInst.isAdded) {//아무것도 display 안된 상태.
                    myBtmShtObjInst.showBtmPermDialog(receivedActivity)
                    Log.d(TAG, "permissionForSingleDNLD: ***DISPLAY BOTTOM SHEET NOW!! .isAdded= FALSE!!..")
                }

            } else { // 1-b) 최초로 권한 요청!
                //권한 요청
                Log.d(TAG, "permissionForSingleDNLD: 1-b) 처음으로 권한 요청드립니다!!")
                reqPermToWrite()
            }
        } else { // 2) 이미 권한 허용이 된 상태
            Log.d(TAG, "permissionForSingleDNLD: 2) 이미 권한이 허용된 상태!")
            myBtmShtObjInst.removePermBtmSheetAndResume()
            val myDownloaderInstance = MyDownloader_v1(receivedActivity) // todo: 이렇게하면 결국  MyDownloader_v1() 를 두개 만들어주니..memory issue.
                myDownloaderInstance.singleFileDNLD(receivedDownloadableItem)
        }

    }*/


    //1-b> Permission to Write 신규 다운로드 or 앱 재설치시 기존에 '구매해놓은' RT 를 다운받아야될 때.)
    /*fun permissionForMultipleDNLD(receivedDownloadableList: MutableList<DownloadableItem>) { // called from: MyDownloader_v1.kt>preDownloadPrep()
        needToDownloadList = receivedDownloadableList
        Log.d(TAG, "permissionForMultipleDNLD: needToDownloadList.size = ${needToDownloadList.size}, contents=$needToDownloadList")

        Log.d(TAG, "permissionForMultipleDNLD: API higher >>>than 23. API LVL= ${Build.VERSION.SDK_INT}")
        if (ContextCompat.checkSelfPermission(receivedActivity.applicationContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
        { // 1) 권한 허용 안된 상태
            if (ActivityCompat.shouldShowRequestPermissionRationale(receivedActivity,android.Manifest.permission.WRITE_EXTERNAL_STORAGE))// 이전 거부한적있으면 should..()가 true 반환
            {// 1-a) 이전에 한번 거부한적이 있는경우 showBottomDialog() 함수를 실행
                Log.d(TAG, "permissionForMultipleDNLD: 1-a) 이전에 한번 거부한적이 있다.")

                // bottomSheet 두번 뜨는것 방지위해 다음 코드를 넣었음!

                val fm = myBtmShtObjInst.fragmentManager
                if(fm==null) {
                    Log.d(TAG, "permissionForMultipleDNLD: fm is null==bottomSheet NOT(XX) displayed or prepared!!!!")
                }
                if(fm!=null) { // BottomSheet 이 display 된 상태.
                    Log.d(TAG, "permissionForMultipleDNLD: fm not null..something is(OO) displayed already")
                    val fragTransaction = fm.beginTransaction()
                    fm.executePendingTransactions()
                    //fm 에서의 onCreateView/Dialog() 작용은 Asynchronous 기 때문에. <-요기 executePending() 을 통해서 다 실행(?)한 후.에.야 밑에 .isAdded 에 걸림.
                }

                if(!myBtmShtObjInst.isAdded) {//아무것도 display 안된 상태.
                    myBtmShtObjInst.showBtmPermDialog(receivedActivity)
                    Log.d(TAG, "permissionForMultipleDNLD: ***DISPLAY BOTTOM SHEET NOW!! .isAdded= FALSE!!..")
                }

            } else { // 1-b) 최초로 권한 요청!
                //권한 요청
                Log.d(TAG, "permissionForMultipleDNLD: 1-b) 처음으로 권한 요청드립니다!!")
                reqPermToWrite()
            }
        } else { // 2) 이미 권한 허용이 된 상태
            Log.d(TAG, "permissionForMultipleDNLD: 2) 이미 권한이 허용된 상태!")
            myBtmShtObjInst.removePermBtmSheetAndResume()
            val myDownloaderInstance = MyDownloader_v1(receivedActivity) // todo: 이렇게하면 결국  MyDownloader_v1() 를 두개 만들어주니..memory issue.

            if(needToDownloadList.size == 1) { // 한개 단순 구매의 경우. 리스트에는 하나만 있음.
                myDownloaderInstance.singleFileDNLD(needToDownloadList[0])
                needToDownloadList.clear()
            } else if(needToDownloadList.size>1) { // 두개 이상이 리스트에 있다= 과거 구입건에 대한 다운로드(sync) 건.
                myDownloaderInstance.multipleFileDNLD(needToDownloadList)
                needToDownloadList.clear()
            }

        }
    }*/



    //onReqPerm 은 MainActivity 에만 써줘야 되더라..그래서 Main에 onReqPerm.. 하고 override 해줬음.
   /* override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray)
    {
        Log.d(TAG, "onRequestPermissionsResult: INSIDE PermissionHandler.kt!")
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            MY_WRITE_PERMISSION_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG, "onRequestPermissionsResult: Permission Allowed(O)!!")
                    myBtmShtObjInst.removePermBtmSheetAndResume()

                    val myDownloaderInstance = MyDownloader_v1(receivedActivity) // todo: 이렇게하면 결국  MyDownloader_v1() 를 두개 만들어주니..memory issue.
                    if(needToDownloadList.size == 1) { // 한개 단순 구매의 경우. 리스트에는 하나만 있음.
                        myDownloaderInstance.singleFileDNLD(needToDownloadList[0])
                        needToDownloadList.clear()
                    } else if(needToDownloadList.size>1) { // 두개 이상이 리스트에 있다= 과거 구입건에 대한 다운로드(sync) 건.
                        myDownloaderInstance.multipleFileDNLD(needToDownloadList)
                        needToDownloadList.clear()
                    }

                }
                else if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED){
                    Log.d(TAG, "onRequestPermissionsResult: PERMISSION_DENIED (or BACKGROUND clicked.)")
                    //todo: LiveData -> SecondFrag 로 "Permission Denied" 되었음 전달?
                    if(!myBtmShtObjInst.isAdded) {//아무것도 display 안된 상태.
                        Log.d(TAG, "permissionToWrite: ***DISPLAY 벤치휭~ BOTTOM SHEET NOW!! .isAdded= FALSE!!..")
                        myBtmShtObjInst.showBtmPermDialog(receivedActivity) //Settings & Cancel 갈 수 있는 BottomFrag

                    }

                }
                else {
                    Log.d(TAG, "onRequestPermissionsResult: Permission DENIED!!(XXX)..")
                    Toast.makeText(receivedActivity,"Permission Denied. Purchase/Sync Canceled", Toast.LENGTH_LONG).show()
                }
            }

        }

    }*/

}