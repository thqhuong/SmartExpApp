package com.example.smartexpapp.model;

public class Product {
    private final String name;
    private final String category;
    private final String amount;
    private final String storage;
    private final int daysUntilExpiry;
    private final int iconRes;
    private final String imageUrl;

    public Product(String name, String category, String amount, String storage, int daysUntilExpiry, int iconRes) {
        this(name, category, amount, storage, daysUntilExpiry, iconRes, null);
    }

    public Product(String name, String category, String amount, String storage, int daysUntilExpiry, int iconRes, String imageUrl) {
        this.name = name;
        this.category = category;
        this.amount = amount;
        this.storage = storage;
        this.daysUntilExpiry = daysUntilExpiry;
        this.iconRes = iconRes;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getAmount() {
        return amount;
    }

    public String getStorage() {
        return storage;
    }

    public int getDaysUntilExpiry() {
        return daysUntilExpiry;
    }

    public int getIconRes() {
        return iconRes;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isExpiringSoon() {
        return daysUntilExpiry <= 3;
    }

    public String getExpiryStatus() {
        if (daysUntilExpiry <= 0) {
            return "Today";
        }
        if (daysUntilExpiry == 1) {
            return "1 Day";
        }
        if (daysUntilExpiry >= 60) {
            return Math.max(1, daysUntilExpiry / 30) + " Months";
        }
        return daysUntilExpiry + " Days";
    }

    public String getDashboardBadge() {
        if (daysUntilExpiry <= 0) {
            return "TODAY";
        }
        if (daysUntilExpiry == 1) {
            return "TOMORROW";
        }
        return "IN " + daysUntilExpiry + " DAYS";
    }

    public int getExpiryProgress() {
        if (daysUntilExpiry <= 0) {
            return 100;
        }
        return Math.min(100, Math.max(0, Math.round(daysUntilExpiry / 14f * 100f)));
    }
}
