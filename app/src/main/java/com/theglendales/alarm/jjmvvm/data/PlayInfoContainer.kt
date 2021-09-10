package com.theglendales.alarm.jjmvvm.data

import com.theglendales.alarm.jjmvvm.mediaplayer.StatusMp

// SecondFrag 나갈 때 SharedPref 에 현재 재생중인 곡 정보 저장 (ex.1st Frag 이동, background 로 이동..)
data class PlayInfoContainer(var trackID: Int, var seekBarMax: Int, var seekbarProgress: Int, var songStatusMp: StatusMp) {
    // 추후 Chip 설정값도 저장 필요할듯..
}