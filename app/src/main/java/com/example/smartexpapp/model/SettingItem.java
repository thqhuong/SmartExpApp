package com.example.smartexpapp.model;

public class SettingItem {
    private final String title;
    private final String subtitle;
    private final int iconRes;
    private final boolean switchRow;

    public SettingItem(String title, String subtitle, int iconRes, boolean switchRow) {
        this.title = title;
        this.subtitle = subtitle;
        this.iconRes = iconRes;
        this.switchRow = switchRow;
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
