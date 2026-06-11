package com.example.smartexpapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smartexpapp.data.SampleData;
import com.example.smartexpapp.model.SettingItem;
import com.example.smartexpapp.util.ImageLoader;
import com.example.smartexpapp.util.ViewUtils;

import java.util.List;

public class SettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupChrome(R.id.nav_settings);
        ImageLoader.load(findViewById(R.id.profileImage), "https://images.unsplash.com/photo-1599566150163-29194dcaad36?auto=format&fit=crop&q=80&w=200");
        bindSettings();
        findViewById(R.id.signOutButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        });
    }

    private void bindSettings() {
        LinearLayout list = findViewById(R.id.settingsList);
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
                    android.content.SharedPreferences prefs = getSharedPreferences("smart_settings", MODE_PRIVATE);
                    boolean isNight = prefs.getBoolean("dark_mode", false);
                    switchBtn.setChecked(isNight);
                    switchBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked != isNight) {
                            prefs.edit().putBoolean("dark_mode", isChecked).apply();
                            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                    isChecked ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                            );
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
        } else if ("Account Details".equals(title)) {
            intent = new Intent(this, AccountDetailsActivity.class);
        } else if ("Help & Support".equals(title)) {
            intent = new Intent(this, HelpSupportActivity.class);
        } else if ("Storage Preferences".equals(title)) {
            Toast.makeText(this, "Storage preferences are managed when adding products.", Toast.LENGTH_SHORT).show();
        }

        if (intent != null) {
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }
}
