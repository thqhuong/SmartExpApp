package com.example.smartexpapp.model;

import java.util.ArrayList;
import java.util.List;

public class Recipe {
    private final String title;
    private final String summary;
    private final List<String> expiringIngredients;
    private final String actionText;
    private final int iconRes;
    private final boolean featured;
    private final String imageUrl;

    public Recipe(String title, String summary, List<String> expiringIngredients, String actionText, int iconRes, boolean featured) {
        this(title, summary, expiringIngredients, actionText, iconRes, featured, null);
    }

    public Recipe(String title, String summary, List<String> expiringIngredients, String actionText, int iconRes, boolean featured, String imageUrl) {
        if (title == null) throw new IllegalArgumentException("title cannot be null");
        if (summary == null) throw new IllegalArgumentException("summary cannot be null");
        if (expiringIngredients == null) throw new IllegalArgumentException("expiringIngredients cannot be null");
        if (actionText == null) throw new IllegalArgumentException("actionText cannot be null");

        this.title = title;
        this.summary = summary;
        this.expiringIngredients = new ArrayList<>(expiringIngredients);
        this.actionText = actionText;
        this.iconRes = iconRes;
        this.featured = featured;
        this.imageUrl = imageUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getExpiringIngredients() {
        return expiringIngredients;
    }

    public String getActionText() {
        return actionText;
    }

    public int getIconRes() {
        return iconRes;
    }

    public boolean isFeatured() {
        return featured;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
