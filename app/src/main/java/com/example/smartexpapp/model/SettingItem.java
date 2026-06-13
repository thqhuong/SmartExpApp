package com.example.smartexpapp.model;

public class SettingItem {
    public static final String KEY_NOTIFICATIONS = "notifications";
    public static final String KEY_STORAGE = "storage";
    public static final String KEY_DIETARY = "dietary";
    public static final String KEY_PROFILE = "profile";
    public static final String KEY_HELP = "help";
    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_DARK_MODE = "dark_mode";

    private final String key;
    private final String title;
    private final String subtitle;
    private final int iconRes;
    private final boolean switchRow;

    public SettingItem(String key, String title, String subtitle, int iconRes, boolean switchRow) {
        this.key = key;
        this.title = title;
        this.subtitle = subtitle;
        this.iconRes = iconRes;
        this.switchRow = switchRow;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public int getIconRes() {
        return iconRes;
    }

    public boolean isSwitchRow() {
        return switchRow;
    }
}
