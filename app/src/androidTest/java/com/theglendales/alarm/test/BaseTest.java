package com.theglendales.alarm.test;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.platform.app.InstrumentationRegistry;

import com.theglendales.alarm.R;
import com.theglendales.alarm.logger.Logger;
import com.theglendales.alarm.model.AlarmValue;
import com.theglendales.alarm.persistance.AlarmDatabaseHelper;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Predicate;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

/**
 * Created by Yuriy on 11.07.2017.
 */

public class BaseTest {
    private static final boolean DBG = true;

    private static void sleep(int howLong) {
        if (DBG) {
            try {
                Thread.sleep(howLong);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected static void sleep() {
        sleep(1000);
    }

    @BeforeClass
    @AfterClass
    public static void dropDatabase() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AlarmDatabaseHelper dbHelper = new AlarmDatabaseHelper(context, Logger.create());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS alarms");
        dbHelper.onCreate(db);
        db.close();
        System.out.println("Dropped database");
    }

    protected void deleteAlarm(int position) {
      //  onData(anything()).onChildView(withId(R.id.details_button_container)).atPosition(position).perform(longClick());
        sleep(200);
        onView(withText("Delete alarm")).perform(click());
        sleep(200);
        onView(withText("OK")).perform(click());
        sleep(200);
    }

    @Before
    public void closeSoftKeyBoard() {
        closeSoftKeyboard();
    }

    @After
    public void tearDown() throws InterruptedException {
        //TODO actually mock database access in these tests
        Thread.sleep(1000);
    }

    @androidx.annotation.NonNull
    protected Predicate<AlarmValue> enabled() {
        return new Predicate<AlarmValue>() {
            @Override
            public boolean test(@NonNull AlarmValue alarmValue) throws Exception {
                return alarmValue.isEnabled();
            }
        };
    }

    protected void assertTimerView(String s) {
        onView(withId(com.theglendales.alarm.R.id.hours_tens)).check(matches(withText("" + s.charAt(0))));
        onView(withId(com.theglendales.alarm.R.id.hours_ones)).check(matches(withText("" + s.charAt(1))));
        onView(withId(com.theglendales.alarm.R.id.minutes_tens)).check(matches(withText("" + s.charAt(3))));
        onView(withId(com.theglendales.alarm.R.id.minutes_ones)).check(matches(withText("" + s.charAt(4))));
    }

    @androidx.annotation.NonNull
    protected ListAsserts.ListAssert<AlarmValue> assertThatList() {
        return ListAsserts.assertThatList(R.id.list_fragment_list);
    }
}
