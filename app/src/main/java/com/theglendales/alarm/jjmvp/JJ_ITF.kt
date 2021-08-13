package com.theglendales.alarm.jjmvp

import com.theglendales.alarm.jjdata.RingtoneClass

interface JJ_ITF {
    interface ViewITF {
        fun showResult(fullRtClassList: MutableList<RingtoneClass>)
    }
    interface PresenterITF {
        fun loadFromFb() {}

    }
}