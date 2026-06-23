package com.example.smartexpapp.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.smartexpapp.data.SettingsRepository;

public class ExpiryReminderRescheduleReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_TIME_CHANGED.equals(action)
                && !Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            return;
        }
        Context appContext = context.getApplicationContext();
        PendingResult pendingResult = goAsync();
        SettingsRepository.getSettingsAsync(appContext, settings -> {
            if (settings.areNotificationsEnabled()) {
                ReminderScheduler.scheduleDailyAt(appContext, settings.getReminderNotifyTimeMinutes());
            } else {
                ReminderScheduler.cancel(appContext);
            }
            pendingResult.finish();
        }, error -> {
            ReminderScheduler.scheduleDailyAt(appContext, SettingsRepository.DEFAULT_REMINDER_NOTIFY_TIME_MINUTES);
            pendingResult.finish();
        });
    }
}
