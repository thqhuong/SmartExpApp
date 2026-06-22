package com.example.smartexpapp.notifications;

import android.content.Context;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.smartexpapp.data.SettingsRepository;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public final class ReminderScheduler {
    public static final String UNIQUE_DAILY_WORK = "smart_expiry_daily_reminders";
    public static final String UNIQUE_IMMEDIATE_WORK = "smart_expiry_immediate_reminder";

    private ReminderScheduler() {
    }

    public static void scheduleDaily(Context context) {
        Context appContext = context.getApplicationContext();
        SettingsRepository.getSettingsAsync(appContext,
                settings -> scheduleDaily(appContext, settings.getReminderNotifyTimeMinutes(), ExistingWorkPolicy.REPLACE),
                error -> scheduleDaily(appContext, SettingsRepository.DEFAULT_REMINDER_NOTIFY_TIME_MINUTES, ExistingWorkPolicy.REPLACE));
    }

    public static void scheduleNextDaily(Context context) {
        Context appContext = context.getApplicationContext();
        SettingsRepository.getSettingsAsync(appContext,
                settings -> scheduleDaily(appContext, settings.getReminderNotifyTimeMinutes(), ExistingWorkPolicy.APPEND_OR_REPLACE),
                error -> scheduleDaily(appContext, SettingsRepository.DEFAULT_REMINDER_NOTIFY_TIME_MINUTES, ExistingWorkPolicy.APPEND_OR_REPLACE));
    }

    public static void scheduleDailyAt(Context context, int minutesAfterMidnight) {
        scheduleDaily(context.getApplicationContext(), minutesAfterMidnight, ExistingWorkPolicy.REPLACE);
    }

    static void scheduleDaily(Context context, int minutesAfterMidnight) {
        scheduleDaily(context, minutesAfterMidnight, ExistingWorkPolicy.REPLACE);
    }

    private static void scheduleDaily(Context context, int minutesAfterMidnight, ExistingWorkPolicy policy) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ExpiryReminderWorker.class)
                .setInitialDelay(nextDelayMillisFor(minutesAfterMidnight, System.currentTimeMillis()), TimeUnit.MILLISECONDS)
                .addTag(UNIQUE_DAILY_WORK)
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(UNIQUE_DAILY_WORK, policy, request);
    }

    public static void runSoon(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ExpiryReminderWorker.class)
                .addTag(UNIQUE_IMMEDIATE_WORK)
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(UNIQUE_IMMEDIATE_WORK, ExistingWorkPolicy.REPLACE, request);
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(UNIQUE_DAILY_WORK);
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(UNIQUE_IMMEDIATE_WORK);
    }

    static long nextDelayMillisFor(int minutesAfterMidnight, long nowMillis) {
        int safeMinutes = Math.max(0, Math.min((24 * 60) - 1, minutesAfterMidnight));
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(nowMillis);

        Calendar next = (Calendar) now.clone();
        next.set(Calendar.HOUR_OF_DAY, safeMinutes / 60);
        next.set(Calendar.MINUTE, safeMinutes % 60);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(now)) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }
        return Math.max(0L, next.getTimeInMillis() - nowMillis);
    }
}
