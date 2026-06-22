package com.example.smartexpapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.example.smartexpapp.data.AuthStateRepository;
import com.example.smartexpapp.data.SampleData;
import com.example.smartexpapp.data.SettingsRepository;
import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.model.SettingItem;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ViewUtils;

import java.io.File;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class SettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupChrome(R.id.nav_settings);
        bindLocalProfile();

        findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            Intent intent = new Intent(this, AccountDetailsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        findViewById(R.id.notificationSummaryCard).setOnClickListener(v -> openSetting(SettingItem.KEY_NOTIFICATIONS));

        // signOutButton is dynamically bound in onResume
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindLocalProfile();
        bindSettings();
        bindSignOutButton();
    }

    private void bindSignOutButton() {
        com.google.android.material.button.MaterialButton signOutBtn = findViewById(R.id.signOutButton);
        if (signOutBtn == null)
            return;

        AuthStateRepository.AuthState authState = AuthStateRepository.getAuthState(this);
        if (authState.isSignedIn()) {
            signOutBtn.setText(R.string.sign_out_label);
            signOutBtn.setIconResource(R.drawable.ic_close);
            signOutBtn.setOnClickListener(v -> {
                AuthStateRepository.signOut(this);
                navigateToSignIn();
            });
        } else {
            signOutBtn.setText(R.string.settings_sign_in);
            signOutBtn.setIconResource(R.drawable.ic_info);
            signOutBtn.setOnClickListener(v -> navigateToSignIn());
        }
    }

    private void bindLocalProfile() {
        TextView displayName = findViewById(R.id.settingsDisplayNameText);
        TextView subtitle = findViewById(R.id.settingsProfileSubtitleText);
        ImageView profileImage = findViewById(R.id.profileImage);
        bindProfileAvatar(profileImage);
        AuthStateRepository.AuthState authState = AuthStateRepository.getAuthState(this);
        if (authState.isSignedIn()) {
            String identity = authState.getBestDisplayName(getString(R.string.profile_local_user));
            if (displayName != null) {
                displayName.setText(identity);
            }
            if (subtitle != null) {
                subtitle.setText(getString(R.string.profile_signed_in_format, signedInSubtitleIdentity(authState)));
            }
            return;
        }
        if (authState.isGuest()) {
            if (displayName != null) {
                displayName.setText(R.string.profile_guest_mode);
            }
            if (subtitle != null) {
                subtitle.setText(R.string.profile_guest_device);
            }
            return;
        }
        SettingsRepository.getSettingsAsync(this, settings -> {
            if (displayName != null) {
                displayName.setText(settings.getDisplayName());
            }
            if (subtitle != null) {
                subtitle.setText(R.string.profile_stored_device);
            }
        }, error -> {
            if (displayName != null) {
                displayName.setText(R.string.profile_local_user);
            }
            if (subtitle != null) {
                subtitle.setText(R.string.profile_stored_device);
            }
        });
    }

    private void bindProfileAvatar(ImageView profileImage) {
        if (profileImage == null) {
            return;
        }
        profileImage.setImageResource(R.drawable.ic_nav_profile);
        profileImage.setImageTintList(ColorStateList.valueOf(getColor(R.color.smart_primary)));
        SettingsRepository.getSettingsAsync(this, settings -> {
            String avatarPath = settings.getProfileAvatarPath();
            if (hasUsableAvatar(avatarPath)) {
                profileImage.setImageTintList(null);
                ImageLoader.load(profileImage, avatarPath);
            }
        }, error -> {
            profileImage.setImageResource(R.drawable.ic_nav_profile);
            profileImage.setImageTintList(ColorStateList.valueOf(getColor(R.color.smart_primary)));
        });
    }

    private boolean hasUsableAvatar(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        String filePath = path.startsWith("file://") ? path.substring(7) : path;
        return new File(filePath).isFile();
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String signedInSubtitleIdentity(AuthStateRepository.AuthState authState) {
        if (authState.getEmail() != null && !authState.getEmail().trim().isEmpty()) {
            return authState.getEmail().trim();
        }
        return authState.getBestDisplayName(getString(R.string.profile_local_user));
    }

    private void bindSettings() {
        bindNotificationSummary();
        bindSettingsGroup(findViewById(R.id.inventoryPreferencesList), Arrays.asList(
                setting(SettingItem.KEY_STORAGE),
                setting(SettingItem.KEY_DIETARY),
                setting(SettingItem.KEY_EXPIRING_SOON)));
        bindSettingsGroup(findViewById(R.id.appPreferencesList), Arrays.asList(
                setting(SettingItem.KEY_LANGUAGE),
                setting(SettingItem.KEY_DARK_MODE)));
        bindSettingsGroup(findViewById(R.id.supportList), Arrays.asList(
                setting(SettingItem.KEY_HELP)));
        bindSettingsGroup(findViewById(R.id.accountList), Arrays.asList(
                setting(SettingItem.KEY_PROFILE)));
    }

    private SettingItem setting(String key) {
        for (SettingItem item : SampleData.settings(this)) {
            if (item.getKey().equals(key)) {
                return item;
            }
        }
        throw new IllegalArgumentException("Unknown setting key: " + key);
    }

    private void bindSettingsGroup(LinearLayout list, List<SettingItem> settings) {
        if (list == null) {
            return;
        }
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < settings.size(); i++) {
            SettingItem item = settings.get(i);
            View row = inflater.inflate(R.layout.item_setting_row, list, false);
            ViewUtils.setIcon(row.findViewById(R.id.settingIcon), item.getIconRes(), R.color.smart_secondary);
            ((TextView) row.findViewById(R.id.settingTitle)).setText(item.getTitle());
            ((TextView) row.findViewById(R.id.settingSubtitle)).setText(item.getSubtitle());

            com.google.android.material.switchmaterial.SwitchMaterial switchBtn = row.findViewById(R.id.settingSwitch);
            if (item.isSwitchRow()) {
                switchBtn.setVisibility(View.VISIBLE);
                if (SettingItem.KEY_DARK_MODE.equals(item.getKey())) {
                    switchBtn.setSaveEnabled(false);
                    switchBtn.setOnCheckedChangeListener(null);
                    switchBtn.setChecked(SettingsRepository.getCachedDarkMode(this));
                    switchBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked != SettingsRepository.getCachedDarkMode(this)) {
                            switchBtn.setEnabled(false);
                            applyDarkMode(isChecked, error -> {
                                switchBtn.setOnCheckedChangeListener(null);
                                switchBtn.setChecked(SettingsRepository.getCachedDarkMode(this));
                                switchBtn.setEnabled(true);
                                switchBtn.setOnCheckedChangeListener((retryButton, checked) -> {
                                    if (checked != SettingsRepository.getCachedDarkMode(this)) {
                                        applyDarkMode(checked, retryError -> Toast
                                                .makeText(this, R.string.theme_save_error, Toast.LENGTH_SHORT).show());
                                    }
                                });
                                Toast.makeText(this, R.string.theme_save_error, Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                }
            } else {
                switchBtn.setVisibility(View.GONE);
                row.setOnClickListener(v -> openSetting(item.getKey()));
            }

            ((ImageView) row.findViewById(R.id.settingChevron))
                    .setVisibility(item.isSwitchRow() ? View.GONE : View.VISIBLE);
            row.findViewById(R.id.settingDivider).setVisibility(i == settings.size() - 1 ? View.GONE : View.VISIBLE);
            list.addView(row);
        }
    }

    private void bindNotificationSummary() {
        TextView status = findViewById(R.id.settingsNotificationStatusText);
        TextView detail = findViewById(R.id.settingsNotificationDetailText);
        SettingsRepository.getSettingsAsync(this, settings -> {
            if (status != null) {
                status.setText(settings.areNotificationsEnabled()
                        ? R.string.settings_notifications_enabled_summary
                        : R.string.settings_notifications_disabled_summary);
            }
            if (detail != null) {
                if (settings.areNotificationsEnabled()) {
                    detail.setText(getString(
                            R.string.settings_notification_summary_format,
                            reminderWindowLabel(settings.getReminderDaysBefore()),
                            formatNotifyTime(settings.getReminderNotifyTimeMinutes())));
                } else {
                    detail.setText(R.string.settings_notification_summary_off);
                }
            }
        }, error -> {
            if (status != null) {
                status.setText(R.string.settings_notifications_disabled_summary);
            }
            if (detail != null) {
                detail.setText(R.string.settings_notification_desc);
            }
        });
    }

    private String reminderWindowLabel(int days) {
        if (days == 0) {
            return getString(R.string.reminder_today);
        }
        if (days == 1) {
            return getString(R.string.reminder_1_day);
        }
        return getString(R.string.reminder_days_format, days);
    }

    private String formatNotifyTime(int minutesAfterMidnight) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, minutesAfterMidnight / 60);
        calendar.set(Calendar.MINUTE, minutesAfterMidnight % 60);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(calendar.getTime());
    }

    private void openSetting(String key) {
        Intent intent = null;
        if (SettingItem.KEY_NOTIFICATIONS.equals(key)) {
            intent = new Intent(this, NotificationSettingsActivity.class);
        } else if (SettingItem.KEY_PROFILE.equals(key)) {
            intent = new Intent(this, AccountDetailsActivity.class);
        } else if (SettingItem.KEY_HELP.equals(key)) {
            intent = new Intent(this, HelpSupportActivity.class);
        } else if (SettingItem.KEY_STORAGE.equals(key)) {
            showStoragePreferencesDialog();
        } else if (SettingItem.KEY_DIETARY.equals(key)) {
            showDietaryPreferencesDialog();
        } else if (SettingItem.KEY_LANGUAGE.equals(key)) {
            showLanguageDialog();
        } else if (SettingItem.KEY_EXPIRING_SOON.equals(key)) {
            showExpiringSoonDialog();
        }

        if (intent != null) {
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }

    private void showStoragePreferencesDialog() {
        String[] labels = {
                LocalDataContract.STORAGE_ROOM_TEMP_NAME,
                LocalDataContract.STORAGE_REFRIGERATOR_NAME,
                LocalDataContract.STORAGE_FREEZE_NAME
        };
        String[] ids = {
                LocalDataContract.STORAGE_ROOM_TEMP_ID,
                LocalDataContract.STORAGE_REFRIGERATOR_ID,
                LocalDataContract.STORAGE_FREEZE_ID
        };

        SettingsRepository.getSettingsAsync(this, settings -> {
            int checked = 0;
            for (int i = 0; i < ids.length; i++) {
                if (ids[i].equals(settings.getDefaultStorageLocationId())) {
                    checked = i;
                    break;
                }
            }

            final int[] selected = { checked };
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_single_choice, null);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setDimAmount(0.8f);
                dialog.getWindow()
                        .setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            }

            TextView title = dialogView.findViewById(R.id.dialogTitle);
            title.setText(R.string.default_storage_title);

            RadioGroup radioGroup = dialogView.findViewById(R.id.dialogRadioGroup);
            radioGroup.removeAllViews();
            for (int i = 0; i < labels.length; i++) {
                RadioButton radioButton = new RadioButton(this);
                radioButton.setText(labels[i]);
                radioButton.setId(i);
                radioButton.setTextColor(getColor(R.color.dialog_text_primary));
                radioButton.setTextSize(16);
                radioButton.setPadding(ViewUtils.dp(this, 12), ViewUtils.dp(this, 12), ViewUtils.dp(this, 12),
                        ViewUtils.dp(this, 12));
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    radioButton
                            .setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF8C00")));
                }
                radioGroup.addView(radioButton);
                if (i == checked) {
                    radioGroup.check(i);
                }
            }

            radioGroup.setOnCheckedChangeListener((group, checkedId) -> selected[0] = checkedId);

            dialogView.findViewById(R.id.btnNegative).setOnClickListener(v -> dialog.dismiss());
            dialogView.findViewById(R.id.btnPositive).setOnClickListener(v -> {
                dialog.dismiss();
                SettingsRepository.setDefaultStorageLocationAsync(
                        this,
                        ids[selected[0]],
                        updated -> Toast
                                .makeText(this,
                                        getString(R.string.default_storage_saved_format,
                                                updated.getDefaultStorageName()),
                                        Toast.LENGTH_SHORT)
                                .show(),
                        error -> Toast.makeText(this, R.string.default_storage_save_error, Toast.LENGTH_SHORT).show());
            });

            dialog.show();
        }, error -> Toast.makeText(this, R.string.default_storage_load_error, Toast.LENGTH_SHORT).show());
    }

    private void showDietaryPreferencesDialog() {
        SettingsRepository.getSettingsAsync(this, settings -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_text_input, null);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setDimAmount(0.8f);
                dialog.getWindow()
                        .setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            }

            TextView title = dialogView.findViewById(R.id.dialogTitle);
            title.setText(R.string.settings_dietary_title);

            TextView message = dialogView.findViewById(R.id.dialogMessage);
            message.setVisibility(View.VISIBLE);
            message.setText(R.string.dietary_preferences_message);

            EditText input = dialogView.findViewById(R.id.dialogEditText);
            input.setHint(R.string.dietary_preferences_hint);
            input.setText(settings.getDietaryPreferences());

            TextView positiveText = dialogView.findViewById(R.id.btnPositiveText);
            positiveText.setText(R.string.save_label);

            TextView btnNeutral = dialogView.findViewById(R.id.btnNeutral);
            btnNeutral.setVisibility(View.VISIBLE);
            btnNeutral.setText(R.string.clear_label);
            btnNeutral.setOnClickListener(v -> {
                dialog.dismiss();
                SettingsRepository.setDietaryPreferencesAsync(
                        this,
                        "",
                        updated -> Toast.makeText(this, R.string.dietary_preferences_cleared, Toast.LENGTH_SHORT)
                                .show(),
                        error -> Toast.makeText(this, R.string.dietary_preferences_clear_error, Toast.LENGTH_SHORT)
                                .show());
            });

            dialogView.findViewById(R.id.btnNegative).setOnClickListener(v -> dialog.dismiss());
            dialogView.findViewById(R.id.btnPositive).setOnClickListener(v -> {
                dialog.dismiss();
                SettingsRepository.setDietaryPreferencesAsync(
                        this,
                        input.getText().toString(),
                        updated -> Toast.makeText(this, R.string.dietary_preferences_saved, Toast.LENGTH_SHORT).show(),
                        error -> Toast.makeText(this, R.string.dietary_preferences_save_error, Toast.LENGTH_SHORT)
                                .show());
            });

            dialog.show();
        }, error -> Toast.makeText(this, R.string.dietary_preferences_load_error, Toast.LENGTH_SHORT).show());
    }

    private void showExpiringSoonDialog() {
        int currentDays = SettingsRepository.getExpiringSoonDays(this);

        String[] labels = {
                getString(R.string.expiring_soon_1_day),
                getString(R.string.expiring_soon_3_days),
                getString(R.string.expiring_soon_1_week),
                getString(R.string.expiring_soon_2_weeks),
                getString(R.string.expiring_soon_1_month),
                getString(R.string.expiring_soon_custom)
        };
        int[] values = { 1, 3, 7, 14, 30, -1 };

        int checked = values.length - 1;
        for (int i = 0; i < values.length - 1; i++) {
            if (values[i] == currentDays) {
                checked = i;
                break;
            }
        }
        if (checked == values.length - 1 && currentDays != -1) {
            labels[values.length - 1] = getString(R.string.expiring_soon_custom_format, currentDays);
        }

        final int[] selected = { checked };
        final int[] customValue = { currentDays };
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_single_choice, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setDimAmount(0.8f);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        title.setText(R.string.expiring_soon_dialog_title);

        RadioGroup radioGroup = dialogView.findViewById(R.id.dialogRadioGroup);
        radioGroup.removeAllViews();
        for (int i = 0; i < labels.length; i++) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(labels[i]);
            radioButton.setId(i);
            radioButton.setTextColor(getColor(R.color.dialog_text_primary));
            radioButton.setTextSize(16);
            radioButton.setPadding(ViewUtils.dp(this, 12), ViewUtils.dp(this, 12), ViewUtils.dp(this, 12),
                    ViewUtils.dp(this, 12));
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                radioButton.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF8C00")));
            }
            radioGroup.addView(radioButton);
            if (i == checked) {
                radioGroup.check(i);
            }
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            selected[0] = checkedId;
            if (checkedId == values.length - 1) {
                dialog.dismiss();
                showExpiringSoonCustomDialog(customValue[0]);
            }
        });

        dialogView.findViewById(R.id.btnNegative).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnPositive).setOnClickListener(v -> {
            dialog.dismiss();
            int idx = selected[0];
            if (idx >= 0 && idx < values.length - 1) {
                SettingsRepository.setExpiringSoonDays(this, values[idx]);
                Toast.makeText(this, getString(R.string.expiring_soon_saved_format, labels[idx]), Toast.LENGTH_SHORT)
                        .show();
            }
        });

        dialog.show();
    }

    private void showExpiringSoonCustomDialog(int currentValue) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_text_input, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setDimAmount(0.8f);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }

        TextView title = dialogView.findViewById(R.id.dialogTitle);
        title.setText(R.string.expiring_soon_custom_title);

        TextView message = dialogView.findViewById(R.id.dialogMessage);
        message.setVisibility(View.VISIBLE);
        message.setText(R.string.expiring_soon_custom_message);

        EditText input = dialogView.findViewById(R.id.dialogEditText);
        input.setHint(R.string.expiring_soon_custom_hint);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(currentValue));
        input.setSelection(input.getText().length());

        TextView positiveText = dialogView.findViewById(R.id.btnPositiveText);
        positiveText.setText(R.string.save_label);

        dialogView.findViewById(R.id.btnNegative).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnPositive).setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            try {
                int days = Integer.parseInt(text);
                if (days < 1 || days > 365) {
                    Toast.makeText(this, R.string.expiring_soon_custom_invalid, Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                SettingsRepository.setExpiringSoonDays(this, days);
                Toast.makeText(this, getString(R.string.expiring_soon_saved_format,
                        getString(R.string.expiring_soon_custom_format, days)), Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.expiring_soon_custom_invalid, Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showLanguageDialog() {
        String[] labels = {
                getString(R.string.language_english),
                getString(R.string.language_vietnamese)
        };
        String[] tags = { "en", "vi" };

        SettingsRepository.getSettingsAsync(this, settings -> {
            int checked = "vi".equals(settings.getLanguageTag()) ? 1 : 0;
            final int[] selected = { checked };

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_single_choice, null);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setDimAmount(0.8f);
                dialog.getWindow()
                        .setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            }

            TextView title = dialogView.findViewById(R.id.dialogTitle);
            title.setText(R.string.settings_language_title);

            RadioGroup radioGroup = dialogView.findViewById(R.id.dialogRadioGroup);
            radioGroup.removeAllViews();
            for (int i = 0; i < labels.length; i++) {
                RadioButton radioButton = new RadioButton(this);
                radioButton.setText(labels[i]);
                radioButton.setId(i);
                radioButton.setTextColor(getColor(R.color.dialog_text_primary));
                radioButton.setTextSize(16);
                radioButton.setPadding(ViewUtils.dp(this, 12), ViewUtils.dp(this, 12), ViewUtils.dp(this, 12),
                        ViewUtils.dp(this, 12));
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    radioButton
                            .setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF8C00")));
                }
                radioGroup.addView(radioButton);
                if (i == checked) {
                    radioGroup.check(i);
                }
            }

            radioGroup.setOnCheckedChangeListener((group, checkedId) -> selected[0] = checkedId);

            dialogView.findViewById(R.id.btnNegative).setOnClickListener(v -> dialog.dismiss());
            dialogView.findViewById(R.id.btnPositive).setOnClickListener(v -> {
                dialog.dismiss();
                String languageTag = tags[selected[0]];
                SettingsRepository.setLanguageTagAsync(
                        this,
                        languageTag,
                        updated -> {
                            AppCompatDelegate
                                    .setApplicationLocales(LocaleListCompat.forLanguageTags(updated.getLanguageTag()));
                            Toast.makeText(this, R.string.language_saved, Toast.LENGTH_SHORT).show();
                        },
                        error -> Toast.makeText(this, R.string.language_save_error, Toast.LENGTH_SHORT).show());
            });

            dialog.show();
        }, error -> Toast.makeText(this, R.string.language_load_error, Toast.LENGTH_SHORT).show());
    }
}
