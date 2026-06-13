package com.example.smartexpapp.data;

import android.content.Context;

import com.example.smartexpapp.R;
import com.example.smartexpapp.model.SettingItem;

import java.util.Arrays;
import java.util.List;

public final class SampleData {
    private SampleData() {
    }

    public static List<SettingItem> settings(Context context) {
        return Arrays.asList(
                new SettingItem(SettingItem.KEY_NOTIFICATIONS, context.getString(R.string.settings_notification_title), context.getString(R.string.settings_notification_desc), R.drawable.ic_notification_bell, false),
                new SettingItem(SettingItem.KEY_STORAGE, context.getString(R.string.settings_storage_title), context.getString(R.string.settings_storage_desc), R.drawable.ic_nav_inventory, false),
                new SettingItem(SettingItem.KEY_DIETARY, context.getString(R.string.settings_dietary_title), context.getString(R.string.settings_dietary_desc), R.drawable.ic_nav_meals, false),
                new SettingItem(SettingItem.KEY_PROFILE, context.getString(R.string.settings_profile_title), context.getString(R.string.settings_profile_desc), R.drawable.ic_nav_profile, false),
                new SettingItem(SettingItem.KEY_HELP, context.getString(R.string.settings_help_title), context.getString(R.string.settings_help_desc), R.drawable.ic_help, false),
                new SettingItem(SettingItem.KEY_LANGUAGE, context.getString(R.string.settings_language_title), context.getString(R.string.settings_language_desc), R.drawable.ic_nav_settings_gear, false),
                new SettingItem(SettingItem.KEY_DARK_MODE, context.getString(R.string.settings_dark_mode_title), context.getString(R.string.settings_dark_mode_desc), R.drawable.ic_theme_moon, true)
        );
    }
}
