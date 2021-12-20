package com.theglendales.alarm.jjmvvm.permissionAndDownload

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.theglendales.alarm.jjmvvm.iapAndDnldManager.DownloadableItem

private const val TAG="MyPermissionHandler"
class MyPermissionHandler(val receivedActivity: Activity) : ActivityCompat.OnRequestPermissionsResultCallback
{
    private val MY_READ_PERMISSION_CODE = 812 // Download(Write) 후 파일 읽을때. WRITE PERMISSION 하면 자동으로 Permission Granted!
    private val MY_WRITE_PERMISSION_CODE = 2480 // Download 용
    private val myBtmShtObjInst = BtmSheetPermission // OBJECT 로 만들었음! BottomSheet 하나만 뜨게하기 위해!
    private var needToDownloadList = mutableListOf<DownloadableItem>()

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

    //1-b> Permission to Write 신규 다운로드 or 앱 재설치시 기존에 '구매해놓은' RT 를 다운받아야될 때.)
    fun permissionToWriteOnDNLD(receivedDownloadableList: MutableList<DownloadableItem>) { // called from: MyDownloader.kt>preDownloadPrep()
        needToDownloadList = receivedDownloadableList
        Log.d(TAG, "permissionToWriteOnDNLD: needToDownloadList.size = ${needToDownloadList.size}, contents=$needToDownloadList")

        Log.d(TAG, "permissionToWriteOnDNLD: API higher >>>than 23. API LVL= ${Build.VERSION.SDK_INT}")
        if (ContextCompat.checkSelfPermission(receivedActivity.applicationContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
        { // 1) 권한 허용 안된 상태
            if (ActivityCompat.shouldShowRequestPermissionRationale(receivedActivity,android.Manifest.permission.WRITE_EXTERNAL_STORAGE))// 이전 거부한적있으면 should..()가 true 반환
            {// 1-a) 이전에 한번 거부한적이 있는경우 showBottomDialog() 함수를 실행
                Log.d(TAG, "permissionToWriteOnDNLD: 1-a) 이전에 한번 거부한적이 있다.")

                // bottomSheet 두번 뜨는것 방지위해 다음 코드를 넣었음!

                val fm = myBtmShtObjInst.fragmentManager
                if(fm==null) {
                    Log.d(TAG, "permissionToWriteOnDNLD: fm is null==bottomSheet NOT(XX) displayed or prepared!!!!")
                }
                if(fm!=null) { // BottomSheet 이 display 된 상태.
                    Log.d(TAG, "permissionToWriteOnDNLD: fm not null..something is(OO) displayed already")
                    val fragTransaction = fm.beginTransaction()
                    fm.executePendingTransactions()
                    //fm 에서의 onCreateView/Dialog() 작용은 Asynchronous 기 때문에. <-요기 executePending() 을 통해서 다 실행(?)한 후.에.야 밑에 .isAdded 에 걸림.
                }

                if(!myBtmShtObjInst.isAdded) {//아무것도 display 안된 상태.
                    myBtmShtObjInst.showBtmPermDialog(receivedActivity)
                    Log.d(TAG, "permissionToWriteOnDNLD: ***DISPLAY BOTTOM SHEET NOW!! .isAdded= FALSE!!..")
                }

            } else { // 1-b) 최초로 권한 요청!
                //권한 요청
                Log.d(TAG, "permissionToWriteOnDNLD: 1-b) 처음으로 권한 요청드립니다!!")
                reqPermToWrite()
            }
        } else { // 2) 이미 권한 허용이 된 상태
            Log.d(TAG, "permissionToWriteOnDNLD: 2) 이미 권한이 허용된 상태!")
            myBtmShtObjInst.removePermBtmSheetAndResume()
            val myDownloaderInstance = MyDownloader(receivedActivity) // todo: 이렇게하면 결국  MyDownloader() 를 두개 만들어주니..memory issue.

            if(needToDownloadList.size == 1) { // 한개 단순 구매의 경우. 리스트에는 하나만 있음.
                myDownloaderInstance.singleFileDNLD(needToDownloadList[0])
                needToDownloadList.clear()
            } else if(needToDownloadList.size>1) { // 두개 이상이 리스트에 있다= 과거 구입건에 대한 다운로드(sync) 건.
                myDownloaderInstance.multipleFileDNLD(needToDownloadList)
                needToDownloadList.clear()
            }

        }
    }

    private fun reqPermToWrite() {
        ActivityCompat.requestPermissions(receivedActivity, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_WRITE_PERMISSION_CODE)
    }

    //onReqPerm 은 MainActivity 에만 써줘야 되더라..그래서 Main에 onReqPerm.. 하고 override 해줬음.
    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray)
    {
        Log.d(TAG, "onRequestPermissionsResult: INSIDE PermissionHandler.kt!")
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            MY_WRITE_PERMISSION_CODE -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG, "onRequestPermissionsResult: Permission Allowed(O)!!")
                    myBtmShtObjInst.removePermBtmSheetAndResume()

                    val myDownloaderInstance = MyDownloader(receivedActivity) // todo: 이렇게하면 결국  MyDownloader() 를 두개 만들어주니..memory issue.
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

    }

}