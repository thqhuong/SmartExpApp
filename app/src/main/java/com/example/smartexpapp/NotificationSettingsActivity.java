package com.example.smartexpapp;

import android.os.Bundle;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class NotificationSettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);
        setupChrome(R.id.nav_settings);
        setTopTitle("Notifications");
        useBackButton();
        bindMasterSwitch();
    }

    private void bindMasterSwitch() {
        SwitchMaterial master = findViewById(R.id.notificationsMasterSwitch);
        SwitchMaterial[] detailSwitches = new SwitchMaterial[] {
                findViewById(R.id.expiryAlertsSwitch),
                findViewById(R.id.dailySummarySwitch),
                findViewById(R.id.recipeSuggestionSwitch),
                findViewById(R.id.aiQuotaSwitch)
        };
        if (master == null) {
            return;
        }
        master.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (SwitchMaterial detailSwitch : detailSwitches) {
                if (detailSwitch != null) {
                    detailSwitch.setEnabled(isChecked);
                    detailSwitch.setAlpha(isChecked ? 1f : 0.45f);
                }
            }
        });
    }
}
