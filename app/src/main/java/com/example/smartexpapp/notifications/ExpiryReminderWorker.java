package com.example.smartexpapp.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.smartexpapp.InventoryActivity;
import com.example.smartexpapp.R;
import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.data.SettingsRepository;
import com.example.smartexpapp.model.Product;

import java.util.List;

public class ExpiryReminderWorker extends Worker {
    private static final String CHANNEL_ID = "expiry_reminders";
    private static final int NOTIFICATION_ID = 1001;

    public ExpiryReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (!SettingsRepository.areNotificationsEnabled(context)) {
            return Result.success();
        }

        int reminderDays = SettingsRepository.getReminderDaysBefore(context);
        ExpiryReminderContent.Window window = ExpiryReminderContent.windowForLeadDays(reminderDays);
        List<Product> expiring = ProductRepository.getExpiringBetween(context, window.startMillis, window.endMillis);
        ExpiryReminderContent.Message message = ExpiryReminderContent.messageFor(expiring);
        if (message == null) {
            return Result.success();
        }

        createChannel(context);
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success();
        }

        Intent intent = inventoryReminderIntent(context);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_bell)
                .setContentTitle(message.title)
                .setContentText(message.text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message.text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        return Result.success();
    }

    static Intent inventoryReminderIntent(Context context) {
        return new Intent(context, InventoryActivity.class)
                .putExtra(InventoryActivity.EXTRA_FILTER, InventoryActivity.FILTER_EXPIRING_SOON)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Expiry reminders",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Local reminders for products nearing expiry.");
        manager.createNotificationChannel(channel);
    }

}
