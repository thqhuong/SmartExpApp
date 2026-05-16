package com.example.smartexpapp.model;

public class StorageSummary {
    private final String name;
    private final int itemCount;
    private final int progressPercent;
    private final int iconRes;

    public StorageSummary(String name, int itemCount, int progressPercent, int iconRes) {
        this.name = name;
        this.itemCount = itemCount;
        this.progressPercent = progressPercent;
        this.iconRes = iconRes;
    }

    public String getName() {
        return name;
    }

    public int getItemCount() {
        return itemCount;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public int getIconRes() {
        return iconRes;
    }
}
