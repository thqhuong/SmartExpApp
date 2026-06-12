package com.example.smartexpapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartexpapp.data.SampleData;
import com.example.smartexpapp.data.SettingsRepository;
import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.model.SettingItem;
import com.example.smartexpapp.util.ViewUtils;

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

        findViewById(R.id.signOutButton).setOnClickListener(v ->
                Toast.makeText(this, "SmartExpApp is running in local-only mode.", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindLocalProfile();
        bindSettings();
    }

    private void bindLocalProfile() {
        TextView displayName = findViewById(R.id.settingsDisplayNameText);
        SettingsRepository.getSettingsAsync(this, settings -> {
            if (displayName != null) {
                displayName.setText(settings.getDisplayName());
            }
        }, error -> {
            if (displayName != null) {
                displayName.setText("Local User");
            }
        });
    }

    private void bindSettings() {
        LinearLayout list = findViewById(R.id.settingsList);
        if (list != null) {
            list.removeAllViews();
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        List<SettingItem> settings = SampleData.settings();

        for (int i = 0; i < settings.size(); i++) {
            SettingItem item = settings.get(i);
            View row = inflater.inflate(R.layout.item_setting_row, list, false);
            ViewUtils.setIcon(row.findViewById(R.id.settingIcon), item.getIconRes(), R.color.smart_secondary);
            ((TextView) row.findViewById(R.id.settingTitle)).setText(item.getTitle());
            ((TextView) row.findViewById(R.id.settingSubtitle)).setText(item.getSubtitle());

            com.google.android.material.switchmaterial.SwitchMaterial switchBtn = row.findViewById(R.id.settingSwitch);
            if (item.isSwitchRow()) {
                switchBtn.setVisibility(View.VISIBLE);
                if ("Dark Mode".equals(item.getTitle())) {
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
                                        applyDarkMode(checked, retryError ->
                                                Toast.makeText(this, "Could not save theme setting.", Toast.LENGTH_SHORT).show()
                                        );
                                    }
                                });
                                Toast.makeText(this, "Could not save theme setting.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                }
            } else {
                switchBtn.setVisibility(View.GONE);
                row.setOnClickListener(v -> openSetting(item.getTitle()));
            }

            ((ImageView) row.findViewById(R.id.settingChevron)).setVisibility(item.isSwitchRow() ? View.GONE : View.VISIBLE);
            row.findViewById(R.id.settingDivider).setVisibility(i == settings.size() - 1 ? View.GONE : View.VISIBLE);
            list.addView(row);
        }
    }

    private void openSetting(String title) {
        Intent intent = null;
        if ("Notification Settings".equals(title)) {
            intent = new Intent(this, NotificationSettingsActivity.class);
        } else if ("Local Profile".equals(title)) {
            intent = new Intent(this, AccountDetailsActivity.class);
        } else if ("Help & Support".equals(title)) {
            intent = new Intent(this, HelpSupportActivity.class);
        } else if ("Storage Preferences".equals(title)) {
            showStoragePreferencesDialog();
        } else if ("Dietary Preferences".equals(title)) {
            showDietaryPreferencesDialog();
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

            final int[] selected = {checked};
            new AlertDialog.Builder(this)
                    .setTitle("Default Storage")
                    .setSingleChoiceItems(labels, checked, (dialog, which) -> selected[0] = which)
                    .setPositiveButton("Save", (dialog, which) ->
                            SettingsRepository.setDefaultStorageLocationAsync(
                                    this,
                                    ids[selected[0]],
                                    updated -> Toast.makeText(this, "Default storage set to " + updated.getDefaultStorageName() + ".", Toast.LENGTH_SHORT).show(),
                                    error -> Toast.makeText(this, "Could not save storage preference.", Toast.LENGTH_SHORT).show()
                            ))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }, error -> Toast.makeText(this, "Could not load storage preference.", Toast.LENGTH_SHORT).show());
    }

    private void showDietaryPreferencesDialog() {
        SettingsRepository.getSettingsAsync(this, settings -> {
            EditText input = new EditText(this);
            input.setSingleLine(false);
            input.setMinLines(2);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            input.setHint("Vegetarian, dairy-free, low sodium");
            input.setText(settings.getDietaryPreferences());
            input.setSelectAllOnFocus(false);
            int padding = (int) (16 * getResources().getDisplayMetrics().density);
            input.setPadding(padding, padding / 2, padding, padding / 2);

            new AlertDialog.Builder(this)
                    .setTitle("Dietary Preferences")
                    .setMessage("Used only on this device to guide recipe suggestions.")
                    .setView(input)
                    .setPositiveButton("Save", (dialog, which) ->
                            SettingsRepository.setDietaryPreferencesAsync(
                                    this,
                                    input.getText().toString(),
                                    updated -> Toast.makeText(this, "Dietary preferences saved.", Toast.LENGTH_SHORT).show(),
                                    error -> Toast.makeText(this, "Could not save dietary preferences.", Toast.LENGTH_SHORT).show()
                            ))
                    .setNeutralButton("Clear", (dialog, which) ->
                            SettingsRepository.setDietaryPreferencesAsync(
                                    this,
                                    "",
                                    updated -> Toast.makeText(this, "Dietary preferences cleared.", Toast.LENGTH_SHORT).show(),
                                    error -> Toast.makeText(this, "Could not clear dietary preferences.", Toast.LENGTH_SHORT).show()
                            ))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }, error -> Toast.makeText(this, "Could not load dietary preferences.", Toast.LENGTH_SHORT).show());
    }
}
