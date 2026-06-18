package com.example.smartexpapp;

import android.Manifest;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.smartexpapp.data.SettingsRepository;
import com.example.smartexpapp.notifications.ReminderScheduler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class NotificationSettingsActivity extends BaseActivity {
    private SwitchMaterial masterSwitch;
    private MaterialButton reminder1DayButton;
    private MaterialButton reminder3DayButton;
    private MaterialButton reminder7DayButton;
    private SettingsRepository.SettingsSnapshot currentSettings =
            new SettingsRepository.SettingsSnapshot(true, 3);

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    persistNotificationState(true);
                } else {
                    if (masterSwitch != null) {
                        masterSwitch.setOnCheckedChangeListener(null);
                        masterSwitch.setChecked(false);
                        masterSwitch.setOnCheckedChangeListener((buttonView, checked) -> onMasterSwitchChanged(checked));
                    }
                    persistNotificationState(false);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);
        setupChrome(R.id.nav_settings);
        setTopTitle(getString(R.string.settings_notification_title));
        useBackButton();
        bindReminderWindowButtons();
        bindMasterSwitch();
    }

    private void bindReminderWindowButtons() {
        reminder1DayButton = findViewById(R.id.reminder1DayButton);
        reminder3DayButton = findViewById(R.id.reminder3DayButton);
        reminder7DayButton = findViewById(R.id.reminder7DayButton);
        if (reminder1DayButton != null) {
            reminder1DayButton.setOnClickListener(v -> persistReminderDays(1));
        }
        if (reminder3DayButton != null) {
            reminder3DayButton.setOnClickListener(v -> persistReminderDays(3));
        }
        if (reminder7DayButton != null) {
            reminder7DayButton.setOnClickListener(v -> persistReminderDays(7));
        }
        bindReminderButtonState();
    }

    private void bindMasterSwitch() {
        masterSwitch = findViewById(R.id.notificationsMasterSwitch);
        if (masterSwitch == null) {
            return;
        }
        masterSwitch.setEnabled(false);
        SettingsRepository.getSettingsAsync(this, settings -> {
            currentSettings = settings;
            masterSwitch.setOnCheckedChangeListener(null);
            masterSwitch.setChecked(settings.areNotificationsEnabled());
            masterSwitch.setEnabled(true);
            bindDetailSwitchState();
            masterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> onMasterSwitchChanged(isChecked));
        }, error -> {
            masterSwitch.setEnabled(true);
        });
    }

    private void onMasterSwitchChanged(boolean isChecked) {
        if (isChecked && Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        persistNotificationState(isChecked);
    }

    private void persistNotificationState(boolean isChecked) {
        if (masterSwitch != null) {
            masterSwitch.setEnabled(false);
        }
        SettingsRepository.setNotificationsEnabledAsync(this, isChecked, settings -> {
            currentSettings = settings;
            if (isChecked) {
                ReminderScheduler.scheduleDaily(this);
                ReminderScheduler.runSoon(this);
            } else {
                ReminderScheduler.cancel(this);
            }
            if (masterSwitch != null) {
                masterSwitch.setOnCheckedChangeListener(null);
                masterSwitch.setChecked(settings.areNotificationsEnabled());
                masterSwitch.setEnabled(true);
                masterSwitch.setOnCheckedChangeListener((buttonView, checked) -> onMasterSwitchChanged(checked));
            }
            bindDetailSwitchState();
        }, error -> {
            if (masterSwitch != null) {
                masterSwitch.setEnabled(true);
                masterSwitch.setChecked(currentSettings.areNotificationsEnabled());
            }
        });
    }

    private void persistReminderDays(int days) {
        setReminderButtonsEnabled(false);
        SettingsRepository.setReminderDaysBeforeAsync(this, days, settings -> {
            currentSettings = settings;
            bindDetailSwitchState();
            if (settings.areNotificationsEnabled()) {
                ReminderScheduler.scheduleDaily(this);
                ReminderScheduler.runSoon(this);
            }
        }, error -> setReminderButtonsEnabled(true));
    }

    private void bindDetailSwitchState() {
        View details = findViewById(R.id.notificationDetailsPanel);
        if (details != null) {
            details.setAlpha(currentSettings.areNotificationsEnabled() ? 1f : 0.45f);
        }
        bindReminderButtonState();
    }

    private void bindReminderButtonState() {
        updateReminderButton(reminder1DayButton, 1);
        updateReminderButton(reminder3DayButton, 3);
        updateReminderButton(reminder7DayButton, 7);
    }

    private void updateReminderButton(MaterialButton button, int days) {
        if (button == null) {
            return;
        }
        boolean enabled = currentSettings.areNotificationsEnabled();
        boolean selected = currentSettings.getReminderDaysBefore() == days;
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.45f);
        button.setBackgroundTintList(ColorStateList.valueOf(getColor(selected
                ? R.color.smart_primary_container
                : R.color.smart_surface_container)));
        button.setTextColor(getColor(selected ? R.color.smart_on_primary : R.color.smart_on_surface));
        button.setStrokeColor(ColorStateList.valueOf(getColor(selected
                ? R.color.smart_primary_container
                : R.color.smart_glass_stroke)));
    }

    private void setReminderButtonsEnabled(boolean enabled) {
        if (reminder1DayButton != null) reminder1DayButton.setEnabled(enabled);
        if (reminder3DayButton != null) reminder3DayButton.setEnabled(enabled);
        if (reminder7DayButton != null) reminder7DayButton.setEnabled(enabled);
    }
}
