package com.example.smartexpapp.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.AlarmManager;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.Configuration;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;

import java.util.Calendar;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ReminderSchedulerTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        Configuration config = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config);
    }

    @Test
    public void scheduleDailySetsAlarmBackedReminder() {
        ReminderScheduler.scheduleDaily(context, 9 * 60);

        ShadowAlarmManager.ScheduledAlarm alarm = shadowAlarmManager().peekNextScheduledAlarm();

        assertNotNull(alarm);
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.type);
        assertTrue(alarm.isAllowWhileIdle());
    }

    @Test
    public void runSoonReplacesExistingImmediateReminderWork() throws Exception {
        ReminderScheduler.runSoon(context);
        ReminderScheduler.runSoon(context);

        List<WorkInfo> infos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(ReminderScheduler.UNIQUE_IMMEDIATE_WORK)
                .get();

        assertEquals(1, infos.size());
    }

    @Test
    public void cancelClearsScheduledReminderWork() throws Exception {
        ReminderScheduler.scheduleDaily(context, 9 * 60);
        ReminderScheduler.cancel(context);

        assertNull(shadowAlarmManager().peekNextScheduledAlarm());

        List<WorkInfo> infos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(ReminderScheduler.UNIQUE_DAILY_WORK)
                .get();

        for (WorkInfo info : infos) {
            assertTrue(info.getState().isFinished());
        }
    }

    @Test
    public void nextDelayUsesTodayWhenTimeIsStillAhead() {
        Calendar now = Calendar.getInstance();
        now.set(2026, Calendar.JUNE, 21, 8, 30, 0);
        now.set(Calendar.MILLISECOND, 0);

        assertEquals(30 * 60 * 1000L, ReminderScheduler.nextDelayMillisFor(9 * 60, now.getTimeInMillis()));
    }

    @Test
    public void nextDelayUsesTomorrowWhenTimeAlreadyPassed() {
        Calendar now = Calendar.getInstance();
        now.set(2026, Calendar.JUNE, 21, 9, 30, 0);
        now.set(Calendar.MILLISECOND, 0);

        assertEquals((23L * 60L + 30L) * 60L * 1000L, ReminderScheduler.nextDelayMillisFor(9 * 60, now.getTimeInMillis()));
    }

    private ShadowAlarmManager shadowAlarmManager() {
        return Shadows.shadowOf((AlarmManager) context.getSystemService(Context.ALARM_SERVICE));
    }
}
