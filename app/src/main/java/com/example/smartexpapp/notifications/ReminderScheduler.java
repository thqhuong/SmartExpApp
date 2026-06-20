package com.example.smartexpapp.notifications;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public final class ReminderScheduler {
    public static final String UNIQUE_DAILY_WORK = "smart_expiry_daily_reminders";

    private ReminderScheduler() {
    }

    public static void scheduleDaily(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(ExpiryReminderWorker.class, 24, TimeUnit.HOURS)
                .addTag(UNIQUE_DAILY_WORK)
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(UNIQUE_DAILY_WORK, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    public static void runSoon(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ExpiryReminderWorker.class).build();
        WorkManager.getInstance(context.getApplicationContext()).enqueue(request);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(UNIQUE_DAILY_WORK);
    }
}
