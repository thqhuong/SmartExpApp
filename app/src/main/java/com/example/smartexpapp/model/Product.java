package com.example.smartexpapp.model;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Product {
    public static final String SYNC_STATUS_LOCAL = "LOCAL";

    private final String id;
    private final String name;
    private final String category;
    private final String quantity;
    private final String unit;
    private final String storage;
    private final String storageLocationId;
    private final long expiryDateMillis;
    private final String barcode;
    private final String status;
    private final int iconRes;
    private final String imageUrl;
    private final long createdAt;
    private final long updatedAt;
    private final String cloudId;
    private final String ownerUserId;
    private final String syncStatus;
    private final Long lastSyncedAt;

    public Product(String name, String category, String amount, String storage, int daysUntilExpiry, int iconRes) {
        this(name, category, amount, storage, daysUntilExpiry, iconRes, null);
    }

    public Product(String name, String category, String amount, String storage, int daysUntilExpiry, int iconRes, String imageUrl) {
        this(
                newId(),
                name,
                category,
                splitAmount(amount)[0],
                splitAmount(amount)[1],
                storage,
                null,
                expiryDateFromDays(daysUntilExpiry),
                null,
                ProductStatus.ACTIVE,
                iconRes,
                imageUrl,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                null,
                null,
                SYNC_STATUS_LOCAL,
                null
        );
    }

    public Product(String name, String category, String quantity, String unit, String storage, long expiryDateMillis, int iconRes) {
        this(name, category, quantity, unit, storage, expiryDateMillis, iconRes, null);
    }

    public Product(String name, String category, String quantity, String unit, String storage, long expiryDateMillis, int iconRes, String imageUrl) {
        this(
                newId(),
                name,
                category,
                quantity,
                unit,
                storage,
                null,
                expiryDateMillis,
                null,
                ProductStatus.ACTIVE,
                iconRes,
                imageUrl,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                null,
                null,
                SYNC_STATUS_LOCAL,
                null
        );
    }

    public Product(
            String id,
            String name,
            String category,
            String quantity,
            String unit,
            String storage,
            String storageLocationId,
            long expiryDateMillis,
            String barcode,
            String status,
            int iconRes,
            String imageUrl,
            long createdAt,
            long updatedAt,
            String cloudId,
            String ownerUserId,
            String syncStatus,
            Long lastSyncedAt
    ) {
        this.id = valueOrDefault(id, newId());
        this.name = valueOrDefault(name, "Unnamed Product");
        this.category = valueOrDefault(category, "General");
        this.quantity = valueOrDefault(quantity, "1");
        this.unit = valueOrDefault(unit, "pcs");
        this.storage = valueOrDefault(storage, "Room Temp");
        this.storageLocationId = storageLocationId;
        this.expiryDateMillis = expiryDateMillis;
        this.barcode = barcode;
        this.status = valueOrDefault(status, ProductStatus.ACTIVE);
        this.iconRes = iconRes;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.cloudId = cloudId;
        this.ownerUserId = ownerUserId;
        this.syncStatus = valueOrDefault(syncStatus, SYNC_STATUS_LOCAL);
        this.lastSyncedAt = lastSyncedAt;
    }

    public Product withStatus(String newStatus, long updatedAt) {
        return new Product(
                id,
                name,
                category,
                quantity,
                unit,
                storage,
                storageLocationId,
                expiryDateMillis,
                barcode,
                newStatus,
                iconRes,
                imageUrl,
                createdAt,
                updatedAt,
                cloudId,
                ownerUserId,
                syncStatus,
                lastSyncedAt
        );
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public String getAmount() {
        if (unit == null || unit.trim().isEmpty()) {
            return quantity;
        }
        return quantity + " " + unit;
    }

    public String getStorage() {
        return storage;
    }

    public String getStorageLocationId() {
        return storageLocationId;
    }

    public long getExpiryDateMillis() {
        return expiryDateMillis;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getStatus() {
        return status;
    }

    public int getIconRes() {
        return iconRes;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public String getCloudId() {
        return cloudId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public Long getLastSyncedAt() {
        return lastSyncedAt;
    }

    public int getDaysUntilExpiry() {
        long startOfToday = startOfDay(System.currentTimeMillis());
        long expiryDay = startOfDay(expiryDateMillis);
        return (int) ((expiryDay - startOfToday) / TimeUnit.DAYS.toMillis(1));
    }

    public boolean isExpiringSoon() {
        int days = getDaysUntilExpiry();
        return days >= 0 && days <= 1 && !isExpired();
    }

    public boolean isExpired() {
        return ProductStatus.EXPIRED.equals(status) || getDaysUntilExpiry() < 0;
    }

    public String getFormattedExpiryDate() {
        return new SimpleDateFormat("MMM d, yyyy", Locale.US).format(new Date(expiryDateMillis));
    }

    public String getExpiryStatus() {
        int daysUntilExpiry = getDaysUntilExpiry();
        String date = getFormattedExpiryDate();
        if (ProductStatus.EXPIRED.equals(status) || daysUntilExpiry < 0) {
            return date + " (Expired)";
        }
        if (daysUntilExpiry == 0) {
            return date + " (Today)";
        }
        if (daysUntilExpiry == 1) {
            return date + " (Tomorrow)";
        }
        if (daysUntilExpiry >= 60) {
            return date + " (" + Math.max(1, daysUntilExpiry / 30) + " Months)";
        }
        return date + " (" + daysUntilExpiry + " Days)";
    }

    public String getDashboardBadge() {
        int daysUntilExpiry = getDaysUntilExpiry();
        if (ProductStatus.EXPIRED.equals(status) || daysUntilExpiry < 0) {
            return "EXPIRED";
        }
        if (daysUntilExpiry == 0) {
            return "TODAY";
        }
        if (daysUntilExpiry == 1) {
            return "TOMORROW";
        }
        return "IN " + daysUntilExpiry + " DAYS";
    }

    public String getGroup() {
        if (isExpired()) return "Expired";
        int days = getDaysUntilExpiry();
        if (days <= 1) return "Urgent";
        if (days <= 7) return "Soon";
        return "Safe";
    }

    public int getExpiryProgress() {
        int daysUntilExpiry = getDaysUntilExpiry();
        if (daysUntilExpiry <= 0) {
            return 100;
        }
        return Math.max(0, Math.min(100, 100 - Math.round(daysUntilExpiry / 14f * 100f)));
    }

    public boolean hasStatus(String expectedStatus) {
        return status.equals(expectedStatus);
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    private static String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    private static String[] splitAmount(String amount) {
        String safeAmount = valueOrDefault(amount, "1 pcs").trim();
        int firstSpace = safeAmount.indexOf(' ');
        if (firstSpace < 0) {
            return new String[]{safeAmount, ""};
        }
        return new String[]{
                safeAmount.substring(0, firstSpace).trim(),
                safeAmount.substring(firstSpace + 1).trim()
        };
    }

    private static long expiryDateFromDays(int daysUntilExpiry) {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.add(Calendar.DAY_OF_YEAR, daysUntilExpiry);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    private static long startOfDay(long millis) {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
