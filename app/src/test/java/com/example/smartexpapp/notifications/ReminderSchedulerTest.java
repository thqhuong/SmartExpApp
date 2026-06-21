package com.example.smartexpapp.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

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
    public void scheduleDailyEnqueuesUniquePeriodicReminderWork() throws Exception {
        ReminderScheduler.scheduleDaily(context);

        List<WorkInfo> infos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(ReminderScheduler.UNIQUE_DAILY_WORK)
                .get();

        assertEquals(1, infos.size());
        assertEquals(WorkInfo.State.ENQUEUED, infos.get(0).getState());
    }

    @Test
    public void cancelClearsScheduledReminderWork() throws Exception {
        ReminderScheduler.scheduleDaily(context);
        ReminderScheduler.cancel(context);

        List<WorkInfo> infos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(ReminderScheduler.UNIQUE_DAILY_WORK)
                .get();

        for (WorkInfo info : infos) {
            assertTrue(info.getState().isFinished());
        }
    }
}
