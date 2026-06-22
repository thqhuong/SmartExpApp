package com.example.smartexpapp.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.ColorRes;

import com.example.smartexpapp.R;

import java.util.HashMap;
import java.util.Map;

public class CategoryColorHelper {
    private static final Map<String, Integer> categoryColors = new HashMap<>();
    private static final Map<String, Integer> customColors = new HashMap<>();
    private static int nextColorIndex;
    private static final String COLOR_PREFS = "category_color_prefs";
    private static final int[] builtinPalette = {
            R.color.category_dairy, R.color.category_produce, R.color.category_pantry,
            R.color.category_vegetables, R.color.category_general, R.color.category_meat
    };
    private static final int[] customPalette = {
            R.color.category_custom_teal, R.color.category_custom_pink,
            R.color.category_custom_cyan, R.color.category_custom_yellow,
            R.color.category_custom_deep_orange, R.color.category_custom_brown,
            R.color.category_custom_06, R.color.category_custom_07,
            R.color.category_custom_08, R.color.category_custom_09,
            R.color.category_custom_10, R.color.category_custom_11,
            R.color.category_custom_12, R.color.category_custom_13,
            R.color.category_custom_14, R.color.category_custom_15,
            R.color.category_custom_16, R.color.category_custom_17,
            R.color.category_custom_18, R.color.category_custom_19,
            R.color.category_custom_20, R.color.category_custom_21,
            R.color.category_custom_22, R.color.category_custom_23,
            R.color.category_custom_24, R.color.category_custom_25,
            R.color.category_custom_26, R.color.category_custom_27,
            R.color.category_custom_28, R.color.category_custom_29
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
        if (category == null) return R.color.category_general;
        String canonical = toCanonical(context, category);
        Integer color = categoryColors.get(canonical);
        if (color != null) return color;
        Integer assigned = customColors.get(canonical);
        if (assigned != null) return assigned;
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(COLOR_PREFS, Context.MODE_PRIVATE);
            int storedIdx = prefs.getInt(canonical, -1);
            if (storedIdx >= 0 && storedIdx < customPalette.length) {
                customColors.put(canonical, customPalette[storedIdx]);
                return customPalette[storedIdx];
            }
        }
        int idx = nextColorIndex % customPalette.length;
        nextColorIndex++;
        customColors.put(canonical, customPalette[idx]);
        if (context != null) {
            context.getSharedPreferences(COLOR_PREFS, Context.MODE_PRIVATE)
                    .edit().putInt(canonical, idx).apply();
        }
        return customPalette[idx];
    }

    public static void transferColorMapping(Context context, String oldCanonical, String newCanonical) {
        if (context == null || oldCanonical == null || newCanonical == null) return;
        SharedPreferences prefs = context.getSharedPreferences(COLOR_PREFS, Context.MODE_PRIVATE);
        int colorIdx = prefs.getInt(oldCanonical, -1);
        if (colorIdx >= 0) {
            prefs.edit().remove(oldCanonical).putInt(newCanonical, colorIdx).apply();
        } else {
            colorIdx = 0;
            prefs.edit().putInt(newCanonical, colorIdx).apply();
        }
        Integer oldColor = customColors.remove(oldCanonical);
        if (oldColor != null) {
            customColors.put(newCanonical, oldColor);
        } else if (colorIdx >= 0 && colorIdx < customPalette.length) {
            customColors.put(newCanonical, customPalette[colorIdx]);
        }
    }

    public static String toCanonical(Context context, String category) {
        if (category == null) return null;
        String trimmed = category.trim();
        String[] canonicals = {"Dairy", "General", "Meat", "Pantry", "Produce", "Vegetables"};
        String[][] translations = {
            {"Dairy", "Sữa"},
            {"General", "Chung"},
            {"Meat", "Thịt"},
            {"Pantry", "Đồ khô"},
            {"Produce", "Rau quả"},
            {"Vegetables", "Rau củ"}
        };
        for (int i = 0; i < canonicals.length; i++) {
            if (canonicals[i].equalsIgnoreCase(trimmed)) {
                return canonicals[i];
            }
            for (String translation : translations[i]) {
                if (translation.equalsIgnoreCase(trimmed)) {
                    return canonicals[i];
                }
            }
        }
        if (context != null) {
            int[] resIds = {R.string.cat_dairy, R.string.cat_general, R.string.cat_meat,
                    R.string.cat_pantry, R.string.cat_produce, R.string.cat_vegetables};
            for (int i = 0; i < canonicals.length; i++) {
                if (context.getString(resIds[i]).equalsIgnoreCase(trimmed)) {
                    return canonicals[i];
                }
            }
        }
        return trimmed;
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
