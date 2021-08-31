package com.theglendales.alarm.jjmvvm.helper

import android.widget.ImageButton
import com.theglendales.alarm.jjmvvm.mediaplayer.MyMediaPlayer

// Flow 는 MyMediaPlayer -> MpVModel -> SecondFrag -> MiniPlayerHandler..이런식으로
// todo: ExoPlayer 가 재생중 멈췄을때 (혹은 반대 상황에) -> mpVModel -> SecondFrag -> MiniPlayerClickHandler  이런식으로 전달 되야함.

class MiniPlayerClickHandler(val mediaPlayerInstance: MyMediaPlayer, val playButton: ImageButton, val pauseButton: ImageButton) {
}