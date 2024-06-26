/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.theglendales.alarm.alert;

import static com.theglendales.alarm.configuration.Prefs.LONGCLICK_DISMISS_DEFAULT;
import static com.theglendales.alarm.configuration.Prefs.LONGCLICK_DISMISS_KEY;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.theglendales.alarm.R;
import com.theglendales.alarm.background.Event;
import com.theglendales.alarm.configuration.InjectKt;
import com.theglendales.alarm.configuration.Prefs;
import com.theglendales.alarm.configuration.Store;
import com.theglendales.alarm.interfaces.Alarm;
import com.theglendales.alarm.interfaces.IAlarmsManager;
import com.theglendales.alarm.interfaces.Intents;
import com.theglendales.alarm.logger.Logger;
import com.theglendales.alarm.model.AlarmValue;
import com.theglendales.alarm.presenter.DynamicThemeHandler;
import com.theglendales.alarm.presenter.PickedTime;
import com.theglendales.alarm.presenter.TimePickerDialogFragment;
import com.theglendales.alarm.util.Optional;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;

/**
 * Alarm Clock alarm alert: pops visible indicator and plays alarm tone. This
 * activity is the full screen version which shows over the lock screen with the
 * wallpaper as the background.
 */
// 알람 울리고 (버블에서 클릭했을 때 뜨는 전체화면 - SNOOZE/DISMISS 이렇게 두 칸 뜬다.) -> alert_fullscreen.xml
public class AlarmAlertFullScreen extends FragmentActivity {
    private final String TAG="AlarmAlertFullScreen";// 내가 추가
    protected static final String SCREEN_OFF = "screen_off";
    private final Store store = InjectKt.globalInject(Store.class).getValue();
    private final IAlarmsManager alarmsManager = InjectKt.globalInject(IAlarmsManager.class).getValue();
    private final Prefs sp = InjectKt.globalInject(Prefs.class).getValue();
    private final Logger logger = InjectKt.globalLogger("AlarmAlertFullScreen").getValue();
    private final DynamicThemeHandler dynamicThemeHandler = InjectKt.globalInject(DynamicThemeHandler.class).getValue();

    protected Alarm mAlarm;

    private boolean longClickToDismiss;

    private Disposable disposableDialog = Disposables.disposed();
    private Disposable subscription;

    @Override
    protected void onCreate(Bundle icicle) {
        setTheme(dynamicThemeHandler.getIdForName(getClassName()));
        super.onCreate(icicle);

        if (getResources().getBoolean(R.bool.isTablet)) {
            // preserve initial rotation and disable rotation change
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(getRequestedOrientation());
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        final int id = getIntent().getIntExtra(Intents.EXTRA_ID, -1);
        try {
            mAlarm = alarmsManager.getAlarm(id);

            final Window win = getWindow();
            win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            // Turn on the screen unless we are being launched from the
            // AlarmAlert
            // subclass as a result of the screen turning off.
            if (!getIntent().getBooleanExtra(SCREEN_OFF, false)) {
                win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
            }

            updateLayout();

            // Register to get the alarm killed/snooze/dismiss intent.
            subscription = store.getEvents()
                    .filter(new Predicate<Event>() {
                        @Override
                        public boolean test(Event event) throws Exception {
                            return (event instanceof Event.SnoozedEvent && ((Event.SnoozedEvent) event).getId() == id)
                                    || (event instanceof Event.DismissEvent && ((Event.DismissEvent) event).getId() == id)
                                    || (event instanceof Event.Autosilenced && ((Event.Autosilenced) event).getId() == id);
                        }
                    }).subscribe(new Consumer<Event>() {
                        @Override
                        public void accept(Event event) throws Exception {
                            finish();
                        }
                    });
        } catch (Exception e) {
            logger.e("Alarm not found", e);
        }
    }
// Label 현재 보여주는것으로 -> ImageView 로 보여줄 예정..
    private void setTitleAndAlbumArt() { // 기존 이름 setTitle()
        final String spaceFifteen="               "; // 15칸
        final String spaceTwenty="                    "; // 20칸
        final String spaceSixty="                                                           "; //60칸
        final String labelReceived = mAlarm.getLabelOrDefault(); // 받은 제목(레이블에 적어놓음)
        String titleText = spaceFifteen + "\"" +  labelReceived + "\"";

        if(labelReceived.length() < 6) {
            titleText += spaceSixty; // [제목이 너무 짧으면 6글자 이하] -> [뒤에 공백 60칸 추가]
        } else {
            titleText += spaceTwenty;
        }
    // 내가 추가-->

        AlarmValue aVal= mAlarm.getData();
        final String artFilePath = aVal.getArtFilePath();
        Log.d(TAG, "setTitle: artFilePath= "+artFilePath + " , title=" + titleText);

        //todo: Glide 로 Image 로딩.
        Uri imageUri = Uri.parse(artFilePath);
        ImageView ivAlbumArt = findViewById(R.id.alert_iv_albumart);
        ivAlbumArt.setImageURI(imageUri);
    // 내가 추가-->
        setTitle(titleText);
        TextView tv_titleView = findViewById(R.id.alarm_alert_label_real);
        // 흐르는 Marquee FX 위해..
        tv_titleView.setHorizontallyScrolling(true);
        tv_titleView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        tv_titleView.setSelected(true);
        // set Title.
        tv_titleView.setText(titleText);
        // Snooze 시간
        Button btn_Snooze = findViewById(R.id.alert_button_snooze);
        final String currentSNZValue = sp.getSnoozeDuration().getValue().toString();
        if(sp.getSnoozeDuration().getValue() > 0) {
            final String snoozeString = "SNOOZE (" + currentSNZValue + " mins)"; //이거 할지 말지.
            btn_Snooze.setText(snoozeString);
        } else { // Snooze 가 Disable 된 경우. (-1 을 리턴)
            Log.d(TAG, "setTitleAndAlbumArt: currentSNZValue=" + currentSNZValue);
            btn_Snooze.setVisibility(View.INVISIBLE);
        }



    }

    protected int getLayoutResId() {
        return R.layout.alert_fullscreen;
    }

    protected String getClassName() {
        return AlarmAlertFullScreen.class.getName();
    }

    private void updateLayout() {
        LayoutInflater inflater = LayoutInflater.from(this);

        setContentView(inflater.inflate(getLayoutResId(), null));

        /*
         * snooze behavior: pop a snooze confirmation view, kick alarm manager.
         */
        final Button snooze = (Button) findViewById(R.id.alert_button_snooze);
        snooze.requestFocus();
        snooze.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                snoozeIfEnabledInSettings();
            }
        });

        snooze.setOnLongClickListener(new Button.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (isSnoozeEnabled()) {
                    disposableDialog = TimePickerDialogFragment.showTimePicker(getSupportFragmentManager())
                            .subscribe(new Consumer<Optional<PickedTime>>() {
                                @Override
                                public void accept(@NonNull Optional<PickedTime> picked) {
                                    if (picked.isPresent()) {
                                        mAlarm.snooze(picked.get().getHour(), picked.get().getMinute());
                                    } else {
                                        AlarmAlertFullScreen.this.sendBroadcast(new Intent(Intents.ACTION_DEMUTE));
                                    }
                                }
                            });
                    store.getEvents().onNext(new Event.MuteEvent());
                    new android.os.Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            store.getEvents().onNext(new Event.DemuteEvent());
                        }
                    }, 10000);
                }
                return true;
            }
        });

        /* dismiss button: close notification */
        final Button dismissButton = (Button) findViewById(R.id.alert_button_dismiss);
        dismissButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (longClickToDismiss) {
                    dismissButton.setText(getString(R.string.alarm_alert_hold_the_button_text));
                } else {
                    dismiss();
                }
            }
        });

        dismissButton.setOnLongClickListener(new Button.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dismiss();
                return true;
            }
        });

        /* Set the title from the passed in alarm */
        setTitleAndAlbumArt();
    }

    // Attempt to snooze this alert.
    private void snoozeIfEnabledInSettings() {
        if (isSnoozeEnabled()) {
            alarmsManager.snooze(mAlarm);
        }
    }

    // Dismiss the alarm.
    private void dismiss() {
        alarmsManager.dismiss(mAlarm);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }

    private boolean isSnoozeEnabled() {
        return sp.getSnoozeDuration().getValue() != -1;
    }

    /**
     * this is called when a second alarm is triggered while a previous alert
     * window is still active.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        logger.d("AlarmAlert.OnNewIntent()");

        int id = intent.getIntExtra(Intents.EXTRA_ID, -1);
        try {
            mAlarm = alarmsManager.getAlarm(id);
            setTitleAndAlbumArt();
        } catch (Exception e) {
            logger.d("Alarm not found");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        longClickToDismiss = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(LONGCLICK_DISMISS_KEY,
                LONGCLICK_DISMISS_DEFAULT);

        Button snooze = findViewById(R.id.alert_button_snooze);
        //View snoozeText = findViewById(R.id.alert_text_snooze);
        if (snooze != null) snooze.setEnabled(isSnoozeEnabled());
        //if (snoozeText != null) snoozeText.setEnabled(isSnoozeEnabled());
    }

    @Override
    protected void onPause() {
        super.onPause();
        disposableDialog.dispose();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // No longer care about the alarm being killed.
        subscription.dispose();
    }

    @Override
    public void onBackPressed() {
        // Don't allow back to dismiss
    }
}
