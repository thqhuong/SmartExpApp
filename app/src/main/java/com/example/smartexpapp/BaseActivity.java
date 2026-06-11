package com.example.smartexpapp;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved theme preference BEFORE super.onCreate so the correct
        // configuration is used from the start. Only call setDefaultNightMode
        // when the current mode doesn't match the preference (e.g. cold start
        // or after a process death). This avoids triggering redundant activity
        // recreations.
        android.content.SharedPreferences prefs = getSharedPreferences("smart_settings", MODE_PRIVATE);
        boolean isNightMode = prefs.getBoolean("dark_mode", false);
        int targetMode = isNightMode
                ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
        int currentMode = androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode();
        if (currentMode != targetMode) {
            // This will recreate the activity; calling super first then returning
            // ensures the framework lifecycle is satisfied.
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(targetMode);
        }
        super.onCreate(savedInstanceState);
    }

    protected void setupChrome(@IdRes int selectedNavItemId) {
        ImageButton menuButton = findViewById(R.id.topActionMenu);
        View btnToggleLightMode = findViewById(R.id.btnToggleLightMode);
        View btnToggleDarkMode = findViewById(R.id.btnToggleDarkMode);
        View sunBg = findViewById(R.id.themeToggleSunBg);
        View moonBg = findViewById(R.id.themeToggleMoonBg);
        ImageView sunIcon = findViewById(R.id.themeToggleSunIcon);
        ImageView moonIcon = findViewById(R.id.themeToggleMoonIcon);
        View searchButton = findViewById(R.id.topActionSearch);
        View bottomNavigation = findViewById(R.id.bottomNavigation);

        if (menuButton != null) {
            menuButton.setOnClickListener(v -> Toast.makeText(this, "Menu placeholder", Toast.LENGTH_SHORT).show());
        }

        android.content.SharedPreferences prefs = getSharedPreferences("smart_settings", MODE_PRIVATE);
        boolean isNightMode = prefs.getBoolean("dark_mode", false);

        if (sunBg != null && moonBg != null && sunIcon != null && moonIcon != null) {
            if (isNightMode) {
                moonBg.setVisibility(View.VISIBLE);
                sunBg.setVisibility(View.GONE);
                moonIcon.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
                sunIcon.setImageTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.smart_secondary)));
            } else {
                sunBg.setVisibility(View.VISIBLE);
                moonBg.setVisibility(View.GONE);
                sunIcon.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
                moonIcon.setImageTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.smart_secondary)));
            }
        }

        if (btnToggleLightMode != null) {
            btnToggleLightMode.setOnClickListener(v -> {
                if (isNightMode) {
                    v.setEnabled(false);
                    prefs.edit().putBoolean("dark_mode", false).apply();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                    }, 150);
                }
            });
        }

        if (btnToggleDarkMode != null) {
            btnToggleDarkMode.setOnClickListener(v -> {
                if (!isNightMode) {
                    v.setEnabled(false);
                    prefs.edit().putBoolean("dark_mode", true).apply();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                    }, 150);
                }
            });
        }

        if (searchButton != null) {
            searchButton.setOnClickListener(v -> Toast.makeText(this, "Search placeholder", Toast.LENGTH_SHORT).show());
        }

        if (bottomNavigation == null) {
            return;
        }

        applyBottomInsets(bottomNavigation);
        setupNavItem(R.id.nav_inventory, selectedNavItemId);
        setupNavItem(R.id.nav_stats, selectedNavItemId);
        setupNavItem(R.id.nav_add, selectedNavItemId);
        setupNavItem(R.id.nav_recipes, selectedNavItemId);
        setupNavItem(R.id.nav_settings, selectedNavItemId);
    }

    protected void setTopTitle(String title) {
        TextView topTitle = findViewById(R.id.topTitle);
        if (topTitle != null) {
            topTitle.setText(title);
        }
    }

    protected void useBackButton() {
        ImageButton menuButton = findViewById(R.id.topActionMenu);
        if (menuButton != null) {
            menuButton.setImageResource(android.R.drawable.ic_menu_revert);
            menuButton.setOnClickListener(v -> finish());
        }
    }

    private void setupNavItem(@IdRes int itemId, @IdRes int selectedNavItemId) {
        View navItem = findViewById(itemId);
        if (navItem == null) {
            return;
        }
        navItem.setSelected(itemId == selectedNavItemId);
        navItem.setOnClickListener(v -> {
            if (itemId == selectedNavItemId) {
                return;
            }
            Class<?> target = targetActivityFor(itemId);
            if (target == null) {
                return;
            }
            // Use CLEAR_TOP | SINGLE_TOP to reuse an existing instance if it's
            // already in the back stack, and finish the current activity so the
            // stack never grows. This is critical for theme toggling: if multiple
            // activities are alive when setDefaultNightMode() fires, Android
            // recreates ALL of them simultaneously, causing visible glitching.
            Intent intent = new Intent(this, target);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });
    }

    private void applyBottomInsets(View bottomNavigation) {
        int start = bottomNavigation.getPaddingStart();
        int top = bottomNavigation.getPaddingTop();
        int end = bottomNavigation.getPaddingEnd();
        int bottom = bottomNavigation.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            view.setPaddingRelative(start, top, end, bottom + systemBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(bottomNavigation);
    }

    private Class<?> targetActivityFor(@IdRes int itemId) {
        if (itemId == R.id.nav_inventory) {
            return InventoryActivity.class;
        }
        if (itemId == R.id.nav_stats) {
            return MainActivity.class;
        }
        if (itemId == R.id.nav_add) {
            return AddProductActivity.class;
        }
        if (itemId == R.id.nav_recipes) {
            return RecipesActivity.class;
        }
        if (itemId == R.id.nav_settings) {
            return SettingsActivity.class;
        }
        return null;
    }

    public String saveImageToInternalStorage(Uri sourceUri) {
        try {
            File dir = new File(getFilesDir(), "images");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, UUID.randomUUID().toString() + ".jpg");
            ContentResolver resolver = getContentResolver();
            Bitmap bitmap;
            try (InputStream in = resolver.openInputStream(sourceUri)) {
                bitmap = BitmapFactory.decodeStream(in);
            }
            if (bitmap == null) return null;
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }
}
