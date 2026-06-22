package com.example.smartexpapp.util;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class CategoryColorHelperTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        // Clear shared preferences before each test
        context.getSharedPreferences("category_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    @Test
    public void testIsBuiltInCanonical() {
        assertTrue(CategoryColorHelper.isBuiltInCanonical("Dairy"));
        assertTrue(CategoryColorHelper.isBuiltInCanonical("General"));
        assertTrue(CategoryColorHelper.isBuiltInCanonical("Meat"));
        assertTrue(CategoryColorHelper.isBuiltInCanonical("Pantry"));
        assertTrue(CategoryColorHelper.isBuiltInCanonical("Produce"));
        assertTrue(CategoryColorHelper.isBuiltInCanonical("Vegetables"));

        assertFalse(CategoryColorHelper.isBuiltInCanonical("Dairy "));
        assertFalse(CategoryColorHelper.isBuiltInCanonical("CustomCategory"));
        assertFalse(CategoryColorHelper.isBuiltInCanonical(null));
    }

    @Test
    public void testToCanonical() {
        assertEquals("Dairy", CategoryColorHelper.toCanonical(context, "Dairy"));
        assertEquals("Meat", CategoryColorHelper.toCanonical(context, "Meat"));
        // Matches localized names (default config in Robolectric is English)
        assertEquals("Dairy", CategoryColorHelper.toCanonical(context, "Dairy"));
        assertEquals("Custom Category", CategoryColorHelper.toCanonical(context, "Custom Category"));
    }

    @Test
    public void testDefaultCategoryActiveAndCustomization() {
        // Initially active
        assertTrue(CategoryColorHelper.isDefaultCategoryActive(context, "Dairy"));
        assertTrue(CategoryColorHelper.isDefaultCategoryActive(context, "Meat"));

        // Customize Dairy (e.g. rename or delete it)
        CategoryColorHelper.markDefaultCategoryCustomized(context, "Dairy");

        // Now customized is inactive
        assertFalse(CategoryColorHelper.isDefaultCategoryActive(context, "Dairy"));
        // Meat remains active
        assertTrue(CategoryColorHelper.isDefaultCategoryActive(context, "Meat"));
    }

    @Test
    public void testGetLocalizedCategory() {
        // Active default gets localized name (in Robolectric English it is "Dairy")
        assertEquals("Dairy", CategoryColorHelper.getLocalizedCategory(context, "Dairy"));

        // Custom category returns verbatim
        assertEquals("My Fresh Dairy", CategoryColorHelper.getLocalizedCategory(context, "My Fresh Dairy"));

        // Customize/rename Dairy
        CategoryColorHelper.markDefaultCategoryCustomized(context, "Dairy");

        // Once customized, it behaves as custom and returns verbatim (no longer translated)
        assertEquals("Dairy", CategoryColorHelper.getLocalizedCategory(context, "Dairy"));
    }
}
