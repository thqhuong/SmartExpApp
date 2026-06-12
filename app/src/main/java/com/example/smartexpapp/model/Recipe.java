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
    private final String prepTime;
    private final String difficulty;
    private final String calories;
    private final String smartTip;
    private final List<String> allIngredients;
    private final List<String> instructions;

    public Recipe(String title, String summary, List<String> expiringIngredients, String actionText, int iconRes, boolean featured) {
        this(title, summary, expiringIngredients, actionText, iconRes, featured, null);
    }

    public Recipe(String title, String summary, List<String> expiringIngredients, String actionText, int iconRes, boolean featured, String imageUrl) {
        this(title, summary, expiringIngredients, actionText, iconRes, featured, imageUrl, "20 min", "Medium", "400 kcal", null, expiringIngredients, new ArrayList<>());
    }

    public Recipe(String title, String summary, List<String> expiringIngredients, String actionText, int iconRes, boolean featured, String imageUrl,
                  String prepTime, String difficulty, String calories, String smartTip, List<String> allIngredients, List<String> instructions) {
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
        this.prepTime = prepTime != null ? prepTime : "20 min";
        this.difficulty = difficulty != null ? difficulty : "Medium";
        this.calories = calories != null ? calories : "400 kcal";
        this.smartTip = smartTip;
        this.allIngredients = allIngredients != null ? new ArrayList<>(allIngredients) : new ArrayList<>(expiringIngredients);
        this.instructions = instructions != null ? new ArrayList<>(instructions) : new ArrayList<>();
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

    public String getPrepTime() {
        return prepTime;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getCalories() {
        return calories;
    }

    public String getSmartTip() {
        return smartTip;
    }

    public List<String> getAllIngredients() {
        return allIngredients;
    }

    public List<String> getInstructions() {
        return instructions;
    }
}
