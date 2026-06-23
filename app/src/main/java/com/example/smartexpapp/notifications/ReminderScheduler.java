package com.example.smartexpapp.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.smartexpapp.data.SettingsRepository;

import java.util.Calendar;

public final class ReminderScheduler {
    public static final String UNIQUE_DAILY_WORK = "smart_expiry_daily_reminders";
    public static final String UNIQUE_IMMEDIATE_WORK = "smart_expiry_immediate_reminder";
    static final int DAILY_ALARM_REQUEST_CODE = 2301;

    private ReminderScheduler() {
    }

    public static void scheduleDaily(Context context) {
        Context appContext = context.getApplicationContext();
        SettingsRepository.getSettingsAsync(appContext,
                settings -> scheduleDaily(appContext, settings.getReminderNotifyTimeMinutes()),
                error -> scheduleDaily(appContext, SettingsRepository.DEFAULT_REMINDER_NOTIFY_TIME_MINUTES));
    }

    public static void scheduleNextDaily(Context context) {
        Context appContext = context.getApplicationContext();
        SettingsRepository.getSettingsAsync(appContext,
                settings -> scheduleDaily(appContext, settings.getReminderNotifyTimeMinutes()),
                error -> scheduleDaily(appContext, SettingsRepository.DEFAULT_REMINDER_NOTIFY_TIME_MINUTES));
    }

    public static void scheduleDailyAt(Context context, int minutesAfterMidnight) {
        scheduleDaily(context.getApplicationContext(), minutesAfterMidnight);
    }

    static void scheduleDaily(Context context, int minutesAfterMidnight) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        long triggerAtMillis = nextTriggerAtMillisFor(minutesAfterMidnight, System.currentTimeMillis());
        PendingIntent pendingIntent = dailyAlarmPendingIntent(appContext, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    public static void runSoon(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ExpiryReminderWorker.class)
                .addTag(UNIQUE_IMMEDIATE_WORK)
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(UNIQUE_IMMEDIATE_WORK, ExistingWorkPolicy.REPLACE, request);
    }

    public static void cancel(Context context) {
        cancelDailyAlarm(context.getApplicationContext());
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(UNIQUE_DAILY_WORK);
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(UNIQUE_IMMEDIATE_WORK);
    }

    static void cancelDailyAlarm(Context context) {
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = dailyAlarmPendingIntent(appContext, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private static PendingIntent dailyAlarmPendingIntent(Context context, int extraFlags) {
        Intent intent = new Intent(context, ExpiryReminderAlarmReceiver.class)
                .setAction(ExpiryReminderAlarmReceiver.ACTION_SHOW_EXPIRY_REMINDER);
        return PendingIntent.getBroadcast(
                context,
                DAILY_ALARM_REQUEST_CODE,
                intent,
                extraFlags | PendingIntent.FLAG_IMMUTABLE
        );
    }

    static long nextTriggerAtMillisFor(int minutesAfterMidnight, long nowMillis) {
        return nowMillis + nextDelayMillisFor(minutesAfterMidnight, nowMillis);
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
