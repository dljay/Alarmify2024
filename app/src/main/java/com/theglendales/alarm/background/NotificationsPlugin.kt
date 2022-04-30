/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2012 Yuriy Kulikov yuriy.kulikov.87@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.theglendales.alarm.background

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.theglendales.alarm.CHANNEL_ID_HIGH_PRIO
import com.theglendales.alarm.R
import com.theglendales.alarm.alert.AlarmAlertFullScreen
import com.theglendales.alarm.configuration.Prefs
import com.theglendales.alarm.configuration.globalInject
import com.theglendales.alarm.interfaces.Intents
import com.theglendales.alarm.interfaces.PresentationToModelIntents
import com.theglendales.alarm.isOreo
import com.theglendales.alarm.logger.Logger
import com.theglendales.alarm.notificationBuilder

/**
 * Glue class: connects AlarmAlert IntentReceiver to AlarmAlert activity. Passes
 * through Alarm ID.
 */
private const val TAG="NotificationsPlugin"
class NotificationsPlugin(
        private val logger: Logger,
        private val mContext: Context,
        private val nm: NotificationManager,
        private val enclosingService: EnclosingService
) {
    private val sp: Prefs = globalInject(Prefs::class.java).value// 내가 추가 (Prefs - Snooze 설정된 타임 확인 위해 -> Snooze 시간이 Off 로 설정된 경우-> Popup (notification) 에서 SNOOZE 버튼을 없애기.
    // Notification (상단 버블) 에서 중간 클릭
    fun show(alarm: PluginAlarmData, index: Int, startForeground: Boolean) {
        /* Close dialogs and window shade */
        //mContext.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) //t 수정..

        // Trigger a notification that, when clicked, will show the alarm
        // alert dialog. No need to check for fullscreen since this will always
        // be launched from a user action.
        val notify = Intent(mContext, AlarmAlertFullScreen::class.java)
        notify.putExtra(Intents.EXTRA_ID, alarm.id)
        val pendingNotify = PendingIntent.getActivity(mContext, alarm.id, notify, PendingIntent.FLAG_IMMUTABLE) // 원래는 마지막 칸이 '0' 였음.
        val pendingSnooze = PresentationToModelIntents.createPendingIntent(mContext,
                PresentationToModelIntents.ACTION_REQUEST_SNOOZE, alarm.id)
        val pendingDismiss = PresentationToModelIntents.createPendingIntent(mContext,
                PresentationToModelIntents.ACTION_REQUEST_DISMISS, alarm.id)

        val notification = mContext.notificationBuilder(CHANNEL_ID_HIGH_PRIO) {
//            setContentTitle(alarm.label)
            val nowPlayingTitle = "NOW PLAYING \""+alarm.label +"\"" //todo: STR res 쓰기
            //todo: BOLD?
            setContentTitle("ALARMIFY") // todo: APP TITLE
            setContentText(nowPlayingTitle) //곡 제목? 가능하면 흐르는 텍스트로?
            //setSmallIcon(R.drawable.stat_notify_alarm)
            setSmallIcon(R.drawable.ic_jj_notification_icon_1) // <- 기존 알람 아이콘 대신 앱아이콘으로 대체하고 싶었지만 오직 깔끔한 (흰배경) 사진만 된다네!
            // 참고: https://stackoverflow.com/a/44299487/13930304
            priority = NotificationCompat.PRIORITY_HIGH
            //priority = NotificationCompat.PRIORITY_MAX
            setCategory(NotificationCompat.CATEGORY_ALARM)
            // setFullScreenIntent to show the user AlarmAlert dialog at the same time  when the Notification Bar was created.
            setFullScreenIntent(pendingNotify, true)
        // setContentIntent to show the user AlarmAlert dialog  when he will click on the Notification Bar.
            //Notification (알람 울릴 때 상단 팝업) 클릭하면 -> activity (전체화면으로 이동) 로 이동
            setContentIntent(pendingNotify)
            setOngoing(true)

            if(sp.snoozeDuration.value > 0) {
                addAction(R.drawable.ic_action_snooze, getString(R.string.alarm_alert_snooze_text), pendingSnooze)
                addAction(R.drawable.ic_action_dismiss, getString(R.string.alarm_alert_dismiss_text), pendingDismiss)
            } else { // User 가 Preference 에서 SNooze 를 껐을때
                addAction(R.drawable.ic_action_snooze, getString(R.string.alarm_alert_snooze_disabled_text), pendingSnooze)
                addAction(R.drawable.ic_action_dismiss, getString(R.string.alarm_alert_dismiss_text), pendingDismiss)
            }

            setDefaults(Notification.DEFAULT_LIGHTS)
        }

        if (startForeground && isOreo()) {
            logger.debug { "startForeground() for ${alarm.id}" }
            enclosingService.startForeground(index + OFFSET, notification)
        } else {
            logger.debug { "nm.notify() for ${alarm.id}" }
            nm.notify(index + OFFSET, notification)
        }
    }

    fun cancel(index: Int) {
        nm.cancel(index + OFFSET)
    }

    private fun getString(id: Int) = mContext.getString(id)

    companion object {
        private const val OFFSET = 100000
    }
}
