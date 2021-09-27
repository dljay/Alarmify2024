package com.theglendales.alarm.jjmvvm.util

import android.graphics.Bitmap
import android.net.Uri

// 다운로드 (혹은 디폴트로 디스크에 저장되어있는) pxxx.mp3 파일들을 DiskSearcher 에서 찾아서->AlarmDetailsFrag 로 전달하기 위해 정보를 저장하는 object.

data class RtWithAlbumArt(val trId: String?="", val rtTitle: String?="", val uri: Uri) {
}