package com.theglendales.alarm.jjmvvm.util

import android.graphics.Bitmap
import android.net.Uri

// 다운로드 (혹은 디폴트로 디스크에 저장되어있는) pxxx.rta 파일들을 DiskSearcher 에서 찾아서->AlarmDetailsFrag 로 전달하기 위해 정보를 저장하는 object.

data class RtOnThePhone(val trIdStr: String?="",
                        val rtTitle: String?="",
                        val audioFilePath: String?="",
                        var artFilePathStr: String?="",
                        val fileNameWithExt: String="",
                        val rtDescription: String?="",
                        val badgeStr: String?="",
                        val intensity: String?="",
                        var isRadioBtnChecked: Boolean=false) {
    //isRadioBtnChecked = RtPickerActivity 에서 Rt 선택할 때 체크표시 보여주기 용도로 사용.,
}