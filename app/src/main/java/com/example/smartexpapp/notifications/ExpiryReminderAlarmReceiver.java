package com.example.smartexpapp.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ExpiryReminderAlarmReceiver extends BroadcastReceiver {
    static final String ACTION_SHOW_EXPIRY_REMINDER =
            "com.example.smartexpapp.notifications.SHOW_EXPIRY_REMINDER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }
        String action = intent.getAction();
        if (ACTION_SHOW_EXPIRY_REMINDER.equals(action)) {
            ReminderScheduler.runSoon(context);
        }
    }
}
