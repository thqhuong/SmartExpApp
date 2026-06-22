package com.example.smartexpapp.notifications;

import com.example.smartexpapp.model.Product;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

final class ExpiryReminderContent {
    private ExpiryReminderContent() {
    }

    static final class Window {
        final long startMillis;
        final long endMillis;

        Window(long startMillis, long endMillis) {
            this.startMillis = startMillis;
            this.endMillis = endMillis;
        }
    }

    static final class Message {
        final String title;
        final String text;

        Message(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }

    static Window windowForLeadDays(int days) {
        return new Window(startOfToday(), endOfDayOffset(Math.max(0, days)));
    }

    static Message messageFor(List<Product> expiring) {
        if (expiring == null || expiring.isEmpty()) {
            return null;
        }
        Product first = expiring.get(0);
        if (expiring.size() == 1) {
            return new Message(
                    first.getName() + " is about to expire!",
                    "Make sure to use it soon!"
            );
        }
        if (expiring.size() == 2) {
            Product second = expiring.get(1);
            return new Message(
                    "Some products are about to expire!",
                    first.getName() + " and " + second.getName() + " are about to expire, make sure to use them soon!"
            );
        }
        Product second = expiring.get(1);
        return new Message(
                "Some products are about to expire!",
                first.getName() + ", " + second.getName() + " and more are about to expire, make sure to use them soon!"
        );
    }

    private static long startOfToday() {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static long endOfDayOffset(int days) {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }
}
