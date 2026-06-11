package com.example.smartexpapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.UserSettingsEntity;
import com.example.smartexpapp.util.NotificationHelper;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class NotificationSettingsActivity extends BaseActivity {
    private SwitchMaterial masterSwitch;
    private SwitchMaterial expiryAlertsSwitch;
    private SeekBar reminderDaysSeekBar;
    private TextView reminderDaysValue;
    private AppDatabase database;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    masterSwitch.setChecked(true);
                    updateDbSetting(true);
                    NotificationHelper.rescheduleAll(this);
                } else {
                    masterSwitch.setChecked(false);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            && !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                        showPermissionSettingsDialog();
                    } else {
                        Toast.makeText(this, "Notification permission required for alerts", Toast.LENGTH_LONG).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);
        setupChrome(R.id.nav_settings);
        setTopTitle("Notifications");
        useBackButton();

        database = AppDatabase.getInstance(this);
        NotificationHelper.createNotificationChannel(this);

        masterSwitch = findViewById(R.id.notificationsMasterSwitch);
        expiryAlertsSwitch = findViewById(R.id.expiryAlertsSwitch);
        reminderDaysSeekBar = findViewById(R.id.reminderDaysSeekBar);
        reminderDaysValue = findViewById(R.id.reminderDaysValue);

        loadSettings();
        bindMasterSwitch();
        bindReminderDays();
    }

    private void loadSettings() {
        UserSettingsEntity settings = database.userSettingsDao().getById("default");
        if (settings == null) return;
        boolean notificationEnabled = settings.notificationEnabled;
        masterSwitch.setChecked(notificationEnabled);
        expiryAlertsSwitch.setChecked(notificationEnabled);
        expiryAlertsSwitch.setEnabled(notificationEnabled);
        expiryAlertsSwitch.setAlpha(notificationEnabled ? 1f : 0.45f);
        reminderDaysSeekBar.setProgress(settings.reminderDaysBefore);
        reminderDaysValue.setText(settings.reminderDaysBefore + " days");
    }

    private void updateDbSetting(boolean enabled) {
        UserSettingsEntity settings = database.userSettingsDao().getById("default");
        if (settings == null) return;
        settings.notificationEnabled = enabled;
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
    }

    private void bindMasterSwitch() {
        masterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                } else {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                }
                return;
            }
            expiryAlertsSwitch.setEnabled(isChecked);
            expiryAlertsSwitch.setAlpha(isChecked ? 1f : 0.45f);
            reminderDaysSeekBar.setEnabled(isChecked);
            reminderDaysSeekBar.setAlpha(isChecked ? 1f : 0.45f);
            updateDbSetting(isChecked);
            if (isChecked) {
                NotificationHelper.rescheduleAll(this);
            }
        });
    }

    private void bindReminderDays() {
        reminderDaysSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int days = Math.max(1, progress);
                reminderDaysValue.setText(days + " days");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int days = Math.max(1, seekBar.getProgress());
                UserSettingsEntity settings = database.userSettingsDao().getById("default");
                if (settings != null) {
                    settings.reminderDaysBefore = days;
                    settings.updatedAt = System.currentTimeMillis();
                    database.userSettingsDao().insert(settings);
                }
                NotificationHelper.rescheduleAll(NotificationSettingsActivity.this);
            }
        });
    }

    private void showPermissionSettingsDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Enable Notifications")
                .setMessage("Notification permission is required for expiry alerts. Go to App Settings to enable it.")
                .setPositiveButton("Open Settings", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
