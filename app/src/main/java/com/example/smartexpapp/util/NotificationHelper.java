package com.example.smartexpapp.util;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.smartexpapp.R;
import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.UserSettingsEntity;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.receiver.ExpiryBroadcastReceiver;

import java.util.List;

public class NotificationHelper {
    private static final String CHANNEL_ID = "expiry_channel";
    private static final String CHANNEL_NAME = "Expiry Alerts";
    private static final int BASE_REQUEST_CODE = 1000;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders before products expire");
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    public static void scheduleExpiryNotification(Context context, Product product) {
        if (product == null || product.getId() == null) return;
        if (product.isExpired()) return;

        UserSettingsEntity settings = AppDatabase.getInstance(context).userSettingsDao().getById("default");
        if (settings == null) return;
        int reminderDays = settings.reminderDaysBefore;
        if (!settings.notificationEnabled) return;

        long notifyAt = product.getExpiryDateMillis() - (reminderDays * 86400000L);
        if (notifyAt <= System.currentTimeMillis()) return;

        Intent intent = new Intent(context, ExpiryBroadcastReceiver.class);
        intent.putExtra("product_id", product.getId());
        intent.putExtra("product_name", product.getName());
        intent.putExtra("product_category", product.getCategory());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                BASE_REQUEST_CODE + product.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyAt, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, notifyAt, pendingIntent);
        }
    }

    public static void cancelNotification(Context context, Product product) {
        if (product == null || product.getId() == null) return;

        Intent intent = new Intent(context, ExpiryBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                BASE_REQUEST_CODE + product.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    public static void showNotification(Context context, String productName, String category) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Product Expiring Soon")
                .setContentText(productName + " (" + category + ") is about to expire!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(productName.hashCode(), builder.build());
    }

    public static void rescheduleAll(Context context) {
        List<Product> products = ProductRepository.getProducts(context);
        for (Product p : products) {
            scheduleExpiryNotification(context, p);
        }
    }
}
