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
        assertEquals("Đã hết hạn", expired.getExpiryStatus());
        assertEquals("HẾT HẠN", expired.getDashboardBadge());
        assertEquals(100, expired.getExpiryProgress());

        Product today = productExpiringIn(0);
        assertEquals(0, today.getDaysUntilExpiry());
        assertEquals("Hôm nay", today.getExpiryStatus());
        assertEquals("HÔM NAY", today.getDashboardBadge());
        assertEquals(100, today.getExpiryProgress());

        Product soon = productExpiringIn(1);
        assertEquals(1, soon.getDaysUntilExpiry());
        assertEquals("1 Ngày", soon.getExpiryStatus());
        assertTrue(soon.isExpiringSoon());
        assertTrue(soon.getExpiryProgress() > 0);

        Product safe = productExpiringIn(10);
        assertEquals(10, safe.getDaysUntilExpiry());
        assertEquals("10 Ngày", safe.getExpiryStatus());
        assertFalse(safe.isExpiringSoon());

        Product longRange = productExpiringIn(90);
        assertEquals(90, longRange.getDaysUntilExpiry());
        assertEquals("3 Tháng", longRange.getExpiryStatus());
        assertFalse(longRange.isExpiringSoon());
        assertEquals(0, longRange.getExpiryProgress());
    }

    @Test
    public void compatibilityConstructorPreservesDisplayAmountAndHelpers() {
        Product product = new Product("Milk", "Dairy", "1 Gal", "Refrigerator", 1, 0);

        assertEquals("1 Gal", product.getAmount());
        assertEquals("1 Ngày", product.getExpiryStatus());
        assertEquals("NGÀY MAI", product.getDashboardBadge());
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

        assertEquals("Đã hết hạn", product.getExpiryStatus());
        assertEquals("HẾT HẠN", product.getDashboardBadge());
    }

    @Test
    public void testExpiryStatusAndDashboardBadgeWithLanguageTag() {
        Product expired = productExpiringIn(-1);
        assertEquals("Expired", expired.getExpiryStatus("en"));
        assertEquals("Đã hết hạn", expired.getExpiryStatus("vi"));
        assertEquals("EXPIRED", expired.getDashboardBadge("en"));
        assertEquals("HẾT HẠN", expired.getDashboardBadge("vi"));

        Product today = productExpiringIn(0);
        assertEquals("Today", today.getExpiryStatus("en"));
        assertEquals("Hôm nay", today.getExpiryStatus("vi"));
        assertEquals("TODAY", today.getDashboardBadge("en"));
        assertEquals("HÔM NAY", today.getDashboardBadge("vi"));

        Product soon = productExpiringIn(1);
        assertEquals("1 Day", soon.getExpiryStatus("en"));
        assertEquals("1 Ngày", soon.getExpiryStatus("vi"));
        assertEquals("TOMORROW", soon.getDashboardBadge("en"));
        assertEquals("NGÀY MAI", soon.getDashboardBadge("vi"));

        Product safe = productExpiringIn(10);
        assertEquals("10 Days", safe.getExpiryStatus("en"));
        assertEquals("10 Ngày", safe.getExpiryStatus("vi"));
        assertEquals("10 DAYS LEFT", safe.getDashboardBadge("en"));
        assertEquals("CÒN 10 NGÀY", safe.getDashboardBadge("vi"));

        Product longRange = productExpiringIn(90);
        assertEquals("3 Months", longRange.getExpiryStatus("en"));
        assertEquals("3 Tháng", longRange.getExpiryStatus("vi"));
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
