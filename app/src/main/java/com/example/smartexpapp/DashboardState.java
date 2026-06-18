package com.example.smartexpapp;

import com.example.smartexpapp.model.Product;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DashboardState {
    private final int urgentCount;
    private final int totalTracked;
    private final int wastePreventedCount;
    private final List<StorageSummaryEntry> storageSummaries;
    private final Map<String, List<Product>> productGroups;
    private final boolean isLoading;
    private final boolean isError;

    public DashboardState(int urgentCount, int totalTracked, int wastePreventedCount,
                          List<StorageSummaryEntry> storageSummaries,
                          Map<String, List<Product>> productGroups,
                          boolean isLoading, boolean isError) {
        this.urgentCount = urgentCount;
        this.totalTracked = totalTracked;
        this.wastePreventedCount = wastePreventedCount;
        this.storageSummaries = storageSummaries != null ? storageSummaries : Collections.emptyList();
        this.productGroups = productGroups != null ? productGroups : Collections.emptyMap();
        this.isLoading = isLoading;
        this.isError = isError;
    }

    public int getUrgentCount() { return urgentCount; }
    public int getTotalTracked() { return totalTracked; }
    public int getWastePreventedCount() { return wastePreventedCount; }
    public List<StorageSummaryEntry> getStorageSummaries() { return storageSummaries; }
    public Map<String, List<Product>> getProductGroups() { return productGroups; }
    public boolean isLoading() { return isLoading; }
    public boolean isError() { return isError; }

    public static final class StorageSummaryEntry {
        private final String storageValue;
        private final int count;
        private final int progressPercent;

        public StorageSummaryEntry(String storageValue, int count, int progressPercent) {
            this.storageValue = storageValue;
            this.count = count;
            this.progressPercent = progressPercent;
        }

        public String getStorageValue() { return storageValue; }
        public int getCount() { return count; }
        public int getProgressPercent() { return progressPercent; }
    }

    public static DashboardState loading() {
        return new DashboardState(0, 0, 0, Collections.emptyList(),
                Collections.emptyMap(), true, false);
    }

    public static DashboardState error() {
        return new DashboardState(0, 0, 0, Collections.emptyList(),
                Collections.emptyMap(), false, true);
    }
}
