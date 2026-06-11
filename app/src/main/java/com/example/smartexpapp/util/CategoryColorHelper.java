package com.example.smartexpapp.util;

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
}
