package com.example.smartexpapp.notifications;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.example.smartexpapp.InventoryActivity;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.model.ProductStatus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ExpiryReminderContentTest {
    @Test
    public void windowForLeadDaysStartsTodayAndEndsAtLeadDayEnd() {
        ExpiryReminderContent.Window window = ExpiryReminderContent.windowForLeadDays(3);

        Calendar start = Calendar.getInstance(Locale.US);
        start.setTimeInMillis(window.startMillis);
        assertEquals(0, start.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, start.get(Calendar.MINUTE));
        assertEquals(0, start.get(Calendar.SECOND));
        assertEquals(0, start.get(Calendar.MILLISECOND));

        Calendar expectedEnd = Calendar.getInstance(Locale.US);
        expectedEnd.add(Calendar.DAY_OF_YEAR, 3);
        expectedEnd.set(Calendar.HOUR_OF_DAY, 23);
        expectedEnd.set(Calendar.MINUTE, 59);
        expectedEnd.set(Calendar.SECOND, 59);
        expectedEnd.set(Calendar.MILLISECOND, 999);
        assertEquals(expectedEnd.getTimeInMillis(), window.endMillis);
    }

    @Test
    public void windowForLeadDaysClampsNegativeDaysToToday() {
        ExpiryReminderContent.Window window = ExpiryReminderContent.windowForLeadDays(-2);

        Calendar end = Calendar.getInstance(Locale.US);
        end.setTimeInMillis(window.endMillis);
        Calendar today = Calendar.getInstance(Locale.US);

        assertEquals(today.get(Calendar.DAY_OF_YEAR), end.get(Calendar.DAY_OF_YEAR));
        assertEquals(23, end.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, end.get(Calendar.MINUTE));
    }

    @Test
    public void messageForEmptyListReturnsNull() {
        assertNull(ExpiryReminderContent.messageFor(Collections.emptyList()));
    }

    @Test
    public void messageForSingleProductNamesTheProduct() {
        ExpiryReminderContent.Message message =
                ExpiryReminderContent.messageFor(Collections.singletonList(product("Milk", 1)));

        assertEquals("Milk expires 1 Ngày", message.title);
        assertEquals("Open SmartExpApp for recipe ideas and actions.", message.text);
    }

    @Test
    public void messageForMultipleProductsSummarizesFirstItem() {
        ExpiryReminderContent.Message message =
                ExpiryReminderContent.messageFor(Arrays.asList(product("Milk", 1), product("Spinach", 2)));

        assertEquals("2 items need attention", message.title);
        assertTrue(message.text.contains("Milk is first"));
    }

    @Test
    public void reminderIntentOpensInventoryWithExpiringSoonFilter() {
        Context context = ApplicationProvider.getApplicationContext();

        Intent intent = ExpiryReminderWorker.inventoryReminderIntent(context);

        assertEquals(
                InventoryActivity.FILTER_EXPIRING_SOON,
                intent.getStringExtra(InventoryActivity.EXTRA_FILTER)
        );
        assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0);
    }

    private Product product(String name, int daysUntilExpiry) {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.add(Calendar.DAY_OF_YEAR, daysUntilExpiry);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        long now = System.currentTimeMillis();
        return new Product(
                name + "-id",
                name,
                "General",
                "1",
                "pcs",
                "Room Temp",
                null,
                calendar.getTimeInMillis(),
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
}
