package com.example.smartexpapp.model;

import org.junit.Test;

import java.util.Calendar;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProductTest {
    @Test
    public void expiryCalculationsUseExpiryDateMillis() {
        Product expired = productExpiringIn(-1);
        assertEquals(-1, expired.getDaysUntilExpiry());
        assertEquals("Expired", expired.getExpiryStatus());
        assertEquals("EXPIRED", expired.getDashboardBadge());
        assertEquals(100, expired.getExpiryProgress());

        Product today = productExpiringIn(0);
        assertEquals(0, today.getDaysUntilExpiry());
        assertEquals("Today", today.getExpiryStatus());
        assertEquals("TODAY", today.getDashboardBadge());
        assertEquals(100, today.getExpiryProgress());

        Product soon = productExpiringIn(3);
        assertEquals(3, soon.getDaysUntilExpiry());
        assertEquals("3 Days", soon.getExpiryStatus());
        assertTrue(soon.isExpiringSoon());
        assertTrue(soon.getExpiryProgress() > 0);

        Product safe = productExpiringIn(10);
        assertEquals(10, safe.getDaysUntilExpiry());
        assertEquals("10 Days", safe.getExpiryStatus());
        assertFalse(safe.isExpiringSoon());

        Product longRange = productExpiringIn(90);
        assertEquals(90, longRange.getDaysUntilExpiry());
        assertEquals("3 Months", longRange.getExpiryStatus());
        assertFalse(longRange.isExpiringSoon());
        assertEquals(0, longRange.getExpiryProgress());
    }

    @Test
    public void compatibilityConstructorPreservesDisplayAmountAndHelpers() {
        Product product = new Product("Milk", "Dairy", "1 Gal", "Refrigerator", 1, 0);

        assertEquals("1 Gal", product.getAmount());
        assertEquals("1 Day", product.getExpiryStatus());
        assertEquals("TOMORROW", product.getDashboardBadge());
        assertTrue(product.isExpiringSoon());
    }

    @Test
    public void explicitExpiredStatusOverridesFutureDateDisplay() {
        Product product = new Product(
                "id-1",
                "Milk",
                "Dairy",
                "1",
                "Gal",
                "Refrigerator",
                "refrigerator",
                expiryMillisForOffset(5),
                null,
                ProductStatus.EXPIRED,
                0,
                null,
                1L,
                2L,
                null,
                null,
                Product.SYNC_STATUS_LOCAL,
                null
        );

        assertEquals("Expired", product.getExpiryStatus());
        assertEquals("EXPIRED", product.getDashboardBadge());
    }

    private Product productExpiringIn(int days) {
        return new Product("Milk", "Dairy", "1", "Gal", "Refrigerator", expiryMillisForOffset(days), 0);
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
