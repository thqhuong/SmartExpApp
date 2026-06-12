package com.example.smartexpapp.data;

import com.example.smartexpapp.R;
import com.example.smartexpapp.model.SettingItem;

import java.util.Arrays;
import java.util.List;

public final class SampleData {
    private SampleData() {
    }

    public static List<SettingItem> settings() {
        return Arrays.asList(
                new SettingItem("Notification Settings", "Manage local expiry alerts", R.drawable.ic_notification_bell, false),
                new SettingItem("Storage Preferences", "Default locations and categories", R.drawable.ic_nav_inventory, false),
                new SettingItem("Dietary Preferences", "Guide local and Gemini recipes", R.drawable.ic_nav_meals, false),
                new SettingItem("Local Profile", "Device-only profile and local data", R.drawable.ic_nav_profile, false),
                new SettingItem("Help & Support", "FAQs and contact information", R.drawable.ic_help, false),
                new SettingItem("Dark Mode", "Toggle dark theme across the application", R.drawable.ic_theme_moon, true)
        );
    }
}
