/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.theglendales.alarm.persistance;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import com.theglendales.alarm.configuration.AlarmApplication;
import com.theglendales.alarm.logger.Logger;

/**
 * Helper class for opening the database from multiple providers. Also provides
 * some common functionality.
 */
public class AlarmDatabaseHelper extends SQLiteOpenHelper {

    // 내가 추가 -->
    private static final String ON_APP_INSTALL_LABEL = "InstallAlarm";
    private static final String TAG = "AlarmDatabaseHelper.java";
    // 내가 추가 <--
    private static final String DATABASE_NAME = "alarms.db";
    private static final int DATABASE_VERSION = 5;
    private final Logger log;

    public AlarmDatabaseHelper(Context context, Logger log) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.log = log;
    }

    @SuppressLint("SQLiteString")
    @Override
    public void onCreate(SQLiteDatabase db) {


        String defart01Path = AlarmApplication.Companion.getDefArtPathStr("d1"); // d1.jpg -> d1 으로 확장자 제외.
        String defrta01Path = AlarmApplication.Companion.getDefRtaPathStr("defrt01"); // defrt01.mp3 -> 폰에는 .rta 로 저장!
        String defrta02Path = AlarmApplication.Companion.getDefRtaPathStr("defrt02"); // .mp3 아니면 Notification 에서 소리 안난다!




        //Log.d(TAG, "onCreate: (1)SQL creator's onCreate called-jj. defrta01Path="+defrta01Path); <- defrta1 Path 전달은 Alarmtone.kt 에서 이뤄짐. 여기서는 그 결과값을 받기만 함.
        Log.d(TAG, "onCreate: (2)SQL creator's onCreate called-jj. d1.jpg Path="+ defart01Path);
        // @formatter:off
        db.execSQL("CREATE TABLE alarms (" +
                "_id INTEGER PRIMARY KEY," +
                "hour INTEGER, " +
                "minutes INTEGER, " +
                "daysofweek INTEGER, " +
                "alarmtime INTEGER, " +
                "enabled INTEGER, " +
                "vibrate INTEGER, " +
                "message TEXT, " + // message = Label => 위의 ON_APP_INSTALL_LABEL 넣어줄 예정.
                "alert TEXT, " + // alaert = raw 폴더에 있는 defrt1.rta 경로 저장될 예정
                "prealarm INTEGER, " +
                "state STRING," +
                "artfilepath TEXT);"); // artfilePath= drawables 폴더에 있는 d1.jpg 경로 저장될 예정
        // @formatter:on
        // insert default alarms = ** APP 설치시 생성되는 두개!! **
        String insertMe = "INSERT INTO alarms " + "(hour, minutes, daysofweek, alarmtime, enabled, vibrate, "
                + "message, alert, prealarm, state, artfilepath) VALUES ";
        db.execSQL(insertMe + "(8, 30, 31, 0, 0, 1, '" + ON_APP_INSTALL_LABEL +"', '" + defrta01Path +"', 0, '', '" + defart01Path +"');"); // d1.jpg 의 경로를 넣어줬음(Raw 폴더에 기본으로 탑재되어 있음.)
        db.execSQL(insertMe + "(8, 30, 96, 0, 0, 1, '" + ON_APP_INSTALL_LABEL +"', '" + defrta02Path +"', 0, '', '" + defart01Path +"');"); // d1.jpg 의 경로를 넣어줬음(Raw 폴더에 기본으로 탑재되어 있음.)




    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
        Log.d(TAG, "onUpgrade: called");
        log.d("Upgrading alarms database from version " + oldVersion + " to " + currentVersion
                + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS alarms");
        onCreate(db);
    }

    public Uri commonInsert(ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        long rowId = db.insert("alarms", Columns.MESSAGE, values);
        if (rowId < 0) throw new SQLException("Failed to insert row");
        log.d("Added alarm rowId = " + rowId);

        return ContentUris.withAppendedId(Columns.contentUri(), rowId);
    }
}
