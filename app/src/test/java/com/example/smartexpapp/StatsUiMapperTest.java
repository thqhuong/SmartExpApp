package com.example.smartexpapp;

import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.model.ProductStatus;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class StatsUiMapperTest {
    @Test
    public void groupProducts_emptyList_returnsEmptyMap() {
        Map<String, List<Product>> groups = StatsUiMapper.groupProducts(Collections.emptyList());
        assertNotNull(groups);
        assertTrue(groups.isEmpty());
    }

    @Test
    public void groupProducts_multipleProducts_distributesByExpiryGroup() {
        List<Product> products = Arrays.asList(
                product("e1", "Milk", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, -2),
                product("u1", "Yogurt", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 1),
                product("s1", "Cheese", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 5),
                product("f1", "Butter", "Dairy", LocalDataContract.STORAGE_FREEZE_NAME, 90)
        );

        Map<String, List<Product>> groups = StatsUiMapper.groupProducts(products);

        assertEquals(1, groups.get("Expired").size());
        assertEquals(1, groups.get("Urgent").size());
        assertEquals(1, groups.get("Soon").size());
        assertEquals(1, groups.get("Safe").size());
    }

    @Test
    public void buildStorageSummaries_emptyList_returnsEmptyList() {
        List<DashboardState.StorageSummaryEntry> summaries =
                StatsUiMapper.buildStorageSummaries(Collections.emptyList());
        assertNotNull(summaries);
        assertTrue(summaries.isEmpty());
    }

    @Test
    public void buildStorageSummaries_countsAndOrdersKnownStorageLocations() {
        List<Product> products = Arrays.asList(
                product("1", "Milk", "Dairy", "Refrigerator", 10),
                product("2", "Cheese", "Dairy", "Refrigerator", 20),
                product("3", "Apple", "Fruits", "Room Temp", 15),
                product("4", "Meat", "Meat", "Freeze", 30)
        );

        List<DashboardState.StorageSummaryEntry> summaries =
                StatsUiMapper.buildStorageSummaries(products);

        assertEquals(3, summaries.size());
        assertEquals("Refrigerator", summaries.get(0).getStorageValue());
        assertEquals(2, summaries.get(0).getCount());
        assertEquals(50, summaries.get(0).getProgressPercent());
        assertEquals("Room Temp", summaries.get(1).getStorageValue());
        assertEquals(1, summaries.get(1).getCount());
        assertEquals(25, summaries.get(1).getProgressPercent());
        assertEquals("Freeze", summaries.get(2).getStorageValue());
        assertEquals(1, summaries.get(2).getCount());
        assertEquals(25, summaries.get(2).getProgressPercent());
    }

    @Test
    public void buildStorageSummaries_unknownStorage_notCounted() {
        List<Product> products = Collections.singletonList(
                product("1", "Item", "General", "Pantry", 5)
        );

        List<DashboardState.StorageSummaryEntry> summaries =
                StatsUiMapper.buildStorageSummaries(products);

        int totalCount = 0;
        for (DashboardState.StorageSummaryEntry summary : summaries) {
            totalCount += summary.getCount();
        }
        assertEquals(0, totalCount);
    }

    private Product product(String id, String name, String category, String storage, int daysUntilExpiry) {
        long now = System.currentTimeMillis();
        return new Product(
                id,
                name,
                category,
                "1",
                "pcs",
                storage,
                null,
                expiryMillisForOffset(daysUntilExpiry),
                null,
                ProductStatus.ACTIVE,
                0,
                null,
                now,
                now,
                null,
                null,
                Product.SYNC_STATUS_LOCAL,
                null
        );
    }

    private long expiryMillisForOffset(int days) {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
