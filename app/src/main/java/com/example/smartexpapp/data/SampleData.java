package com.example.smartexpapp.data;

import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.model.Recipe;
import com.example.smartexpapp.model.SettingItem;
import com.example.smartexpapp.model.StorageSummary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SampleData {
    private SampleData() {
    }

    public static List<Product> products() {
        List<Product> products = new ArrayList<>();
        products.add(new Product("Fresh Milk", "Dairy", "1 Gal", "Refrigerator", 1, android.R.drawable.ic_menu_upload, "https://images.unsplash.com/photo-1550583724-125581f77833?auto=format&fit=crop&q=80&w=200"));
        products.add(new Product("Whole Wheat Bread", "Pantry", "1 Loaf", "Room Temp", 5, android.R.drawable.ic_menu_agenda, "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&q=80&w=200"));
        products.add(new Product("Baby Spinach", "Produce", "200g", "Refrigerator", 2, android.R.drawable.ic_menu_upload, "https://images.unsplash.com/photo-1576045057995-568f588f82fb?auto=format&fit=crop&q=80&w=200"));
        products.add(new Product("Frozen Peas", "Vegetables", "1 Bag", "Freeze", 90, android.R.drawable.ic_menu_compass, "https://images.unsplash.com/photo-1590779033100-9f60705a2f3b?auto=format&fit=crop&q=80&w=200"));
        products.add(new Product("Greek Yogurt", "Dairy", "500g", "Refrigerator", 10, android.R.drawable.ic_menu_upload, "https://images.unsplash.com/photo-1488477181946-6428a0291777?auto=format&fit=crop&q=80&w=200"));
        return products;
    }

    public static List<StorageSummary> storageSummaries() {
        return Arrays.asList(
                new StorageSummary("Refrigerator", 64, 45, android.R.drawable.ic_menu_upload),
                new StorageSummary("Room Temperature", 53, 30, android.R.drawable.ic_menu_agenda),
                new StorageSummary("Freezer", 25, 15, android.R.drawable.ic_menu_compass)
        );
    }

    public static List<Recipe> recipes() {
        return Arrays.asList(
                new Recipe(
                        "Rustic Bell Pepper & Tomato Shakshuka",
                        "A hearty one-pan meal for using remaining fresh produce before it turns.",
                        Arrays.asList("Red Bell Peppers - Today", "Cherry Tomatoes - Tomorrow", "Eggs - 3 Days"),
                        "View Recipe",
                        android.R.drawable.ic_menu_gallery,
                        true,
                        "https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&q=80&w=600&h=400"
                ),
                new Recipe(
                        "Super-Green Power Smoothie",
                        "A quick blend to save leafy greens and fruit.",
                        Arrays.asList("Spinach - 1 Day", "Banana"),
                        "Make It",
                        android.R.drawable.ic_menu_myplaces,
                        false,
                        "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&q=80&w=600&h=400"
                ),
                new Recipe(
                        "Crispy Radish & Romaine Salad",
                        "A simple side built around crisp produce.",
                        Arrays.asList("Romaine - 2 Days"),
                        "Make It",
                        android.R.drawable.ic_menu_crop,
                        false,
                        "https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&q=80&w=600&h=400"
                ),
                new Recipe(
                        "Ultimate Grilled Cheddar & Tomato",
                        "Comfort food that uses dairy and bread while they are still fresh.",
                        Arrays.asList("Cheddar Block - 2 Days", "Sourdough Loaf - 3 Days"),
                        "View Recipe",
                        android.R.drawable.ic_menu_gallery,
                        true,
                        "https://images.unsplash.com/photo-1528735602780-2552fd46c7af?auto=format&fit=crop&q=80&w=600&h=400"
                )
        );
    }

    public static List<SettingItem> settings() {
        return Arrays.asList(
                new SettingItem("Notification Settings", "Manage expiry alerts and emails", android.R.drawable.ic_dialog_info, true),
                new SettingItem("Storage Preferences", "Default locations and categories", android.R.drawable.ic_menu_upload, false),
                new SettingItem("Account Details", "Password, billing, and data", android.R.drawable.ic_menu_myplaces, false),
                new SettingItem("Help & Support", "FAQs and contact information", android.R.drawable.ic_menu_help, false)
        );
    }
}
