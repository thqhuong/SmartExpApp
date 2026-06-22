package com.example.smartexpapp;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.example.smartexpapp.data.SettingsRepository;
import com.example.smartexpapp.notifications.ReminderScheduler;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.DateFormat;
import java.util.Calendar;

public class NotificationSettingsActivity extends BaseActivity {
    private static final int[] PRESET_REMINDER_DAYS = {0, 1, 3, 7, 14, 30};

    private SwitchMaterial masterSwitch;
    private MaterialButton reminderTodayButton;
    private MaterialButton reminder1DayButton;
    private MaterialButton reminder3DayButton;
    private MaterialButton reminder7DayButton;
    private MaterialButton reminder14DayButton;
    private MaterialButton reminder30DayButton;
    private MaterialButton reminderCustomButton;
    private TextView notifyTimeValue;
    private View notifyTimeRow;
    private View permissionWarningPanel;
    private View permissionWarningDivider;
    private MaterialButton saveButton;
    private boolean draftNotificationsEnabled = true;
    private int draftReminderDaysBefore = 3;
    private int draftNotifyTimeMinutes = SettingsRepository.DEFAULT_REMINDER_NOTIFY_TIME_MINUTES;
    private boolean settingsLoaded = false;
    private boolean pendingSaveAfterPermission = false;
    private SettingsRepository.SettingsSnapshot currentSettings =
            new SettingsRepository.SettingsSnapshot(true, 3);

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    draftNotificationsEnabled = true;
                    bindDetailSwitchState();
                    if (pendingSaveAfterPermission) {
                        pendingSaveAfterPermission = false;
                        saveDraftSettings();
                    }
                } else {
                    setMasterSwitchChecked(false);
                    draftNotificationsEnabled = false;
                    pendingSaveAfterPermission = false;
                    bindDetailSwitchState();
                    showWarningNotification(getString(R.string.notification_permission_needed));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);
        setupChrome(R.id.nav_settings);
        setTopTitle("Notifications");
        useBackButton();
        bindPermissionWarning();
        bindReminderWindowButtons();
        bindNotifyTimeRow();
        bindSaveButton();
        bindMasterSwitch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindPermissionWarningState();
    }

    private void bindPermissionWarning() {
        permissionWarningPanel = findViewById(R.id.permissionWarningPanel);
        permissionWarningDivider = findViewById(R.id.permissionWarningDivider);
        MaterialButton openSettings = findViewById(R.id.openNotificationSettingsButton);
        if (openSettings != null) {
            openSettings.setOnClickListener(v -> openSystemNotificationSettings());
        }
    }

    private void bindReminderWindowButtons() {
        reminderTodayButton = findViewById(R.id.reminderTodayButton);
        reminder1DayButton = findViewById(R.id.reminder1DayButton);
        reminder3DayButton = findViewById(R.id.reminder3DayButton);
        reminder7DayButton = findViewById(R.id.reminder7DayButton);
        reminder14DayButton = findViewById(R.id.reminder14DayButton);
        reminder30DayButton = findViewById(R.id.reminder30DayButton);
        reminderCustomButton = findViewById(R.id.reminderCustomButton);
        bindPresetButton(reminderTodayButton, 0);
        bindPresetButton(reminder1DayButton, 1);
        bindPresetButton(reminder3DayButton, 3);
        bindPresetButton(reminder7DayButton, 7);
        bindPresetButton(reminder14DayButton, 14);
        bindPresetButton(reminder30DayButton, 30);
        if (reminderCustomButton != null) {
            reminderCustomButton.setOnClickListener(v -> showCustomReminderDialog());
        }
        bindReminderButtonState();
    }

    private void bindPresetButton(MaterialButton button, int days) {
        if (button != null) {
            button.setOnClickListener(v -> updateDraftReminderDays(days));
        }
    }

    private void bindNotifyTimeRow() {
        notifyTimeRow = findViewById(R.id.notifyTimeRow);
        notifyTimeValue = findViewById(R.id.notifyTimeValue);
        if (notifyTimeRow != null) {
            notifyTimeRow.setOnClickListener(v -> showNotifyTimePicker());
        }
        bindNotifyTimeState();
    }

    private void bindSaveButton() {
        saveButton = findViewById(R.id.saveNotificationSettingsButton);
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> {
                if (draftNotificationsEnabled && !hasNotificationPermission()) {
                    pendingSaveAfterPermission = true;
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    return;
                }
                saveDraftSettings();
            });
        }
        bindSaveButtonState();
    }

    private void bindMasterSwitch() {
        masterSwitch = findViewById(R.id.notificationsMasterSwitch);
        if (masterSwitch == null) {
            return;
        }
        masterSwitch.setEnabled(false);
        SettingsRepository.getSettingsAsync(this, settings -> {
            currentSettings = settings;
            bindLoadedSettings();
        }, error -> {
            masterSwitch.setEnabled(true);
            bindDetailSwitchState();
        });
    }

    private void bindLoadedSettings() {
        bindLoadedSettings(currentSettings);
    }

    private void bindLoadedSettings(SettingsRepository.SettingsSnapshot settings) {
        currentSettings = settings;
        draftNotificationsEnabled = settings.areNotificationsEnabled() && hasNotificationPermission();
        draftReminderDaysBefore = settings.getReminderDaysBefore();
        draftNotifyTimeMinutes = settings.getReminderNotifyTimeMinutes();
        settingsLoaded = true;
        setMasterSwitchChecked(draftNotificationsEnabled);
        masterSwitch.setEnabled(true);
        masterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> onMasterSwitchChanged(isChecked));
        bindDetailSwitchState();
    }

    private void onMasterSwitchChanged(boolean isChecked) {
        if (isChecked && !hasNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        draftNotificationsEnabled = isChecked;
        bindDetailSwitchState();
    }

    private void saveDraftSettings() {
        setEditingEnabled(false);
        SettingsRepository.setNotificationSettingsAsync(this, draftNotificationsEnabled, draftReminderDaysBefore, draftNotifyTimeMinutes, settings -> {
            currentSettings = settings;
            draftNotificationsEnabled = settings.areNotificationsEnabled();
            draftReminderDaysBefore = settings.getReminderDaysBefore();
            draftNotifyTimeMinutes = settings.getReminderNotifyTimeMinutes();
            if (settings.areNotificationsEnabled()) {
                ReminderScheduler.scheduleDailyAt(this, settings.getReminderNotifyTimeMinutes());
                ReminderScheduler.runSoon(this);
            } else {
                ReminderScheduler.cancel(this);
            }
            setEditingEnabled(true);
            setMasterSwitchChecked(settings.areNotificationsEnabled());
            bindDetailSwitchState();
            showSuccessNotification(getString(R.string.notification_settings_saved));
        }, error -> {
            setEditingEnabled(true);
            bindDetailSwitchState();
            showErrorNotification(getString(R.string.notification_settings_save_error));
        });
    }

    private void updateDraftReminderDays(int days) {
        draftReminderDaysBefore = Math.max(0, Math.min(SettingsRepository.MAX_REMINDER_DAYS_BEFORE, days));
        bindDetailSwitchState();
    }

    private void updateDraftNotifyTime(int minutesAfterMidnight) {
        draftNotifyTimeMinutes = Math.max(0, Math.min((24 * 60) - 1, minutesAfterMidnight));
        bindDetailSwitchState();
    }

    private void showCustomReminderDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_text_input, null);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setDimAmount(0.8f);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        ((TextView) dialogView.findViewById(R.id.dialogTitle)).setText(R.string.custom_reminder_title);
        TextView message = dialogView.findViewById(R.id.dialogMessage);
        message.setVisibility(View.VISIBLE);
        message.setText(R.string.custom_reminder_message);
        EditText input = dialogView.findViewById(R.id.dialogEditText);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.custom_reminder_hint);
        input.setText(String.valueOf(draftReminderDaysBefore));
        input.setSelectAllOnFocus(true);
        ((TextView) dialogView.findViewById(R.id.btnPositiveText)).setText(R.string.save_label);

        dialogView.findViewById(R.id.btnNegative).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnPositive).setOnClickListener(v -> {
            int days;
            try {
                days = Integer.parseInt(input.getText().toString().trim());
            } catch (NumberFormatException error) {
                showWarningNotification(getString(R.string.custom_reminder_invalid));
                return;
            }
            if (days < 0 || days > SettingsRepository.MAX_REMINDER_DAYS_BEFORE) {
                showWarningNotification(getString(R.string.custom_reminder_invalid));
                return;
            }
            dialog.dismiss();
            updateDraftReminderDays(days);
        });
        dialog.show();
    }

    private void showNotifyTimePicker() {
        int minutes = draftNotifyTimeMinutes;
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> updateDraftNotifyTime((hourOfDay * 60) + minute),
                minutes / 60,
                minutes % 60,
                android.text.format.DateFormat.is24HourFormat(this)
        );
        dialog.show();
    }

    private void bindDetailSwitchState() {
        boolean enabled = draftNotificationsEnabled;
        View details = findViewById(R.id.notificationDetailsPanel);
        if (details != null) {
            details.setAlpha(enabled ? 1f : 0.45f);
        }
        bindPermissionWarningState();
        bindReminderButtonState();
        bindNotifyTimeState();
        bindSaveButtonState();
    }

    private void bindPermissionWarningState() {
        boolean showWarning = Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission();
        if (permissionWarningPanel != null) {
            permissionWarningPanel.setVisibility(showWarning ? View.VISIBLE : View.GONE);
        }
        if (permissionWarningDivider != null) {
            permissionWarningDivider.setVisibility(showWarning ? View.VISIBLE : View.GONE);
        }
    }

    private void bindReminderButtonState() {
        updateReminderButton(reminderTodayButton, 0);
        updateReminderButton(reminder1DayButton, 1);
        updateReminderButton(reminder3DayButton, 3);
        updateReminderButton(reminder7DayButton, 7);
        updateReminderButton(reminder14DayButton, 14);
        updateReminderButton(reminder30DayButton, 30);
        updateCustomReminderButton();
    }

    private void updateReminderButton(MaterialButton button, int days) {
        if (button == null) {
            return;
        }
        boolean enabled = draftNotificationsEnabled;
        boolean selected = draftReminderDaysBefore == days;
        styleReminderButton(button, enabled, selected);
    }

    private void updateCustomReminderButton() {
        if (reminderCustomButton == null) {
            return;
        }
        int days = draftReminderDaysBefore;
        boolean customSelected = !isPresetReminderDays(days);
        reminderCustomButton.setText(customSelected
                ? getString(R.string.reminder_custom_selected_format, days)
                : getString(R.string.reminder_custom));
        styleReminderButton(reminderCustomButton, draftNotificationsEnabled, customSelected);
    }

    private void styleReminderButton(MaterialButton button, boolean enabled, boolean selected) {
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

    private void bindNotifyTimeState() {
        boolean enabled = draftNotificationsEnabled;
        if (notifyTimeRow != null) {
            notifyTimeRow.setEnabled(enabled);
            notifyTimeRow.setAlpha(enabled ? 1f : 0.45f);
        }
        if (notifyTimeValue != null) {
            notifyTimeValue.setText(formatNotifyTime(draftNotifyTimeMinutes));
        }
    }

    private void bindSaveButtonState() {
        if (saveButton != null) {
            saveButton.setEnabled(settingsLoaded && hasUnsavedChanges());
        }
    }

    private boolean hasUnsavedChanges() {
        return draftNotificationsEnabled != currentSettings.areNotificationsEnabled()
                || draftReminderDaysBefore != currentSettings.getReminderDaysBefore()
                || draftNotifyTimeMinutes != currentSettings.getReminderNotifyTimeMinutes();
    }

    private void setEditingEnabled(boolean enabled) {
        if (masterSwitch != null) {
            masterSwitch.setEnabled(enabled);
        }
        setReminderButtonsEnabled(enabled && draftNotificationsEnabled);
        if (notifyTimeRow != null) {
            notifyTimeRow.setEnabled(enabled && draftNotificationsEnabled);
        }
        if (saveButton != null) {
            saveButton.setEnabled(enabled && settingsLoaded && hasUnsavedChanges());
        }
    }

    private void setReminderButtonsEnabled(boolean enabled) {
        if (reminderTodayButton != null) reminderTodayButton.setEnabled(enabled);
        if (reminder1DayButton != null) reminder1DayButton.setEnabled(enabled);
        if (reminder3DayButton != null) reminder3DayButton.setEnabled(enabled);
        if (reminder7DayButton != null) reminder7DayButton.setEnabled(enabled);
        if (reminder14DayButton != null) reminder14DayButton.setEnabled(enabled);
        if (reminder30DayButton != null) reminder30DayButton.setEnabled(enabled);
        if (reminderCustomButton != null) reminderCustomButton.setEnabled(enabled);
    }

    private boolean isPresetReminderDays(int days) {
        for (int preset : PRESET_REMINDER_DAYS) {
            if (preset == days) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < 33
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void setMasterSwitchChecked(boolean checked) {
        if (masterSwitch == null) {
            return;
        }
        masterSwitch.setOnCheckedChangeListener(null);
        masterSwitch.setChecked(checked);
        masterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> onMasterSwitchChanged(isChecked));
    }

    private String formatNotifyTime(int minutesAfterMidnight) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, minutesAfterMidnight / 60);
        calendar.set(Calendar.MINUTE, minutesAfterMidnight % 60);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.getTime());
    }

    private void openSystemNotificationSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:" + getPackageName()));
        }
        startActivity(intent);
    }
}
