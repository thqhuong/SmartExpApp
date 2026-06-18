package com.example.smartexpapp.util;

import android.content.Context;

import androidx.annotation.ColorRes;

import com.example.smartexpapp.R;

import java.util.HashMap;
import java.util.Map;

public class CategoryColorHelper {
    private static final Map<String, Integer> categoryColors = new HashMap<>();
    private static final Map<String, Integer> customColors = new HashMap<>();
    private static int nextColorIndex;
    private static final int[] builtinPalette = {
            R.color.category_dairy, R.color.category_produce, R.color.category_pantry,
            R.color.category_vegetables, R.color.category_general, R.color.category_meat
    };
    private static final int[] customPalette = {
            R.color.category_custom_teal, R.color.category_custom_pink,
            R.color.category_custom_cyan, R.color.category_custom_yellow,
            R.color.category_custom_deep_orange, R.color.category_custom_brown
    };

    static {
        categoryColors.put("Dairy", R.color.category_dairy);
        categoryColors.put("Meat", R.color.category_meat);
        categoryColors.put("Produce", R.color.category_produce);
        categoryColors.put("Pantry", R.color.category_pantry);
        categoryColors.put("Vegetables", R.color.category_vegetables);
        categoryColors.put("General", R.color.category_general);
        nextColorIndex = 0;
    }

    @ColorRes
    public static int getColor(String category) {
        if (category == null) return R.color.category_general;
        Integer color = categoryColors.get(category);
        if (color != null) return color;
        Integer assigned = customColors.get(category);
        if (assigned != null) return assigned;
        int idx = nextColorIndex % customPalette.length;
        nextColorIndex++;
        customColors.put(category, customPalette[idx]);
        return customPalette[idx];
    }

    @ColorRes
    public static int getColor(Context context, String category) {
        return getColor(toCanonical(context, category));
    }

    public static String toCanonical(Context context, String category) {
        if (category == null) return null;
        String[] canonicals = {"Dairy", "General", "Meat", "Pantry", "Produce", "Vegetables"};
        int[] resIds = {R.string.cat_dairy, R.string.cat_general, R.string.cat_meat,
                R.string.cat_pantry, R.string.cat_produce, R.string.cat_vegetables};
        for (int i = 0; i < canonicals.length; i++) {
            if (canonicals[i].equals(category) || context.getString(resIds[i]).equals(category)) {
                return canonicals[i];
            }
        }
        return category;
    }

    public static boolean isBuiltInCanonical(String category) {
        if (category == null) return false;
        return "Dairy".equals(category) || "General".equals(category)
                || "Meat".equals(category) || "Pantry".equals(category)
                || "Produce".equals(category) || "Vegetables".equals(category);
    }

    public static java.util.Set<String> getCustomizedDefaultCategories(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("category_prefs", Context.MODE_PRIVATE);
        return prefs.getStringSet("customized_default_categories", new java.util.HashSet<>());
    }

    public static void markDefaultCategoryCustomized(Context context, String canonicalName) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("category_prefs", Context.MODE_PRIVATE);
        java.util.Set<String> set = new java.util.HashSet<>(prefs.getStringSet("customized_default_categories", new java.util.HashSet<>()));
        set.add(canonicalName);
        prefs.edit().putStringSet("customized_default_categories", set).apply();
    }

    public static boolean isDefaultCategoryActive(Context context, String category) {
        if (category == null) return false;
        String canonical = toCanonical(context, category);
        if (!isBuiltInCanonical(canonical)) {
            return false;
        }
        java.util.Set<String> customized = getCustomizedDefaultCategories(context);
        return !customized.contains(canonical);
    }

    public static String getLocalizedCategory(Context context, String category) {
        if (category == null) return null;
        String canonical = toCanonical(context, category);
        if (isDefaultCategoryActive(context, canonical)) {
            String[] canonicals = {"Dairy", "General", "Meat", "Pantry", "Produce", "Vegetables"};
            int[] resIds = {R.string.cat_dairy, R.string.cat_general, R.string.cat_meat,
                    R.string.cat_pantry, R.string.cat_produce, R.string.cat_vegetables};
            for (int i = 0; i < canonicals.length; i++) {
                if (canonicals[i].equals(canonical)) {
                    return context.getString(resIds[i]);
                }
            }
        }
        return category;
    }
}
