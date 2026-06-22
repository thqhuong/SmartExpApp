package com.example.smartexpapp;

import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.model.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class StatsUiMapper {
    private StatsUiMapper() {
    }

    public static List<DashboardState.StorageSummaryEntry> buildStorageSummaries(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put(LocalDataContract.STORAGE_REFRIGERATOR_NAME, 0);
        counts.put(LocalDataContract.STORAGE_ROOM_TEMP_NAME, 0);
        counts.put(LocalDataContract.STORAGE_FREEZE_NAME, 0);

        for (Product product : products) {
            String storage = knownStorageName(product.getStorage());
            if (counts.containsKey(storage)) {
                counts.put(storage, counts.get(storage) + 1);
            }
        }

        int total = products.size();
        List<DashboardState.StorageSummaryEntry> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            int count = entry.getValue();
            int progress = Math.round(count / (float) total * 100f);
            result.add(new DashboardState.StorageSummaryEntry(entry.getKey(), count, progress));
        }
        return result;
    }

    public static Map<String, List<Product>> groupProducts(List<Product> products, int expiringSoonDays) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<Product>> groups = new LinkedHashMap<>();
        groups.put("Expired", new ArrayList<>());
        groups.put("Urgent", new ArrayList<>());
        groups.put("Soon", new ArrayList<>());
        groups.put("Safe", new ArrayList<>());

        for (Product product : products) {
            String group = product.getGroup(expiringSoonDays);
            List<Product> list = groups.get(group);
            if (list != null) {
                list.add(product);
            }
        }
        return groups;
    }

    private static String knownStorageName(String rawStorage) {
        if (rawStorage == null) {
            return null;
        }
        String normalized = rawStorage.trim().toLowerCase(Locale.US);
        if ("room temp".equals(normalized) || "room".equals(normalized)) {
            return LocalDataContract.STORAGE_ROOM_TEMP_NAME;
        }
        if ("refrigerator".equals(normalized) || "fridge".equals(normalized) || "cool".equals(normalized)) {
            return LocalDataContract.STORAGE_REFRIGERATOR_NAME;
        }
        if ("freezer".equals(normalized) || "freeze".equals(normalized) || "frozen".equals(normalized)) {
            return LocalDataContract.STORAGE_FREEZE_NAME;
        }
        return null;
    }
}
