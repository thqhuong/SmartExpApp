package com.example.smartexpapp.util;

import androidx.annotation.ColorRes;

import com.example.smartexpapp.R;

import java.util.HashMap;
import java.util.Map;

public class CategoryColorHelper {
    private static final Map<String, Integer> categoryColors = new HashMap<>();
    private static final Map<String, Integer> customColors = new HashMap<>();
    private static int nextColorIndex;
    private static final int[] palette = {
            R.color.category_dairy, R.color.category_produce, R.color.category_pantry,
            R.color.category_vegetables, R.color.category_general, R.color.category_meat
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
        int idx = nextColorIndex % palette.length;
        nextColorIndex++;
        customColors.put(category, palette[idx]);
        return palette[idx];
    }
}
