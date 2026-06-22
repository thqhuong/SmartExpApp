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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartexpapp.data.SettingsRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class BaseActivity extends AppCompatActivity {
    private Consumer<Uri> productPhotoCallback;
    private final ActivityResultLauncher<String> productPhotoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                Consumer<Uri> callback = productPhotoCallback;
                productPhotoCallback = null;
                if (callback != null) {
                    callback.accept(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved theme preference BEFORE super.onCreate so the correct
        // configuration is used from the start. Only call setDefaultNightMode
        // when the current mode doesn't match the preference (e.g. cold start
        // or after a process death). This avoids triggering redundant activity
        // recreations.
        String languageTag = SettingsRepository.getCachedLanguageTag(this);
        String currentLanguageTag = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (!languageTag.equals(currentLanguageTag)) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag));
        }

        boolean isNightMode = SettingsRepository.getCachedDarkMode(this);
        int targetMode = isNightMode
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;
        int currentMode = AppCompatDelegate.getDefaultNightMode();
        if (currentMode != targetMode) {
            // This will recreate the activity; calling super first then returning
            // ensures the framework lifecycle is satisfied.
            AppCompatDelegate.setDefaultNightMode(targetMode);
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
        View bottomNavigation = findViewById(R.id.bottomNavigation);

        if (menuButton != null) {
            menuButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
        }

        View themeTogglePill = findViewById(R.id.themeTogglePill);
        bindThemeToggleVisuals(
                SettingsRepository.getCachedDarkMode(this),
                sunBg,
                moonBg,
                sunIcon,
                moonIcon
        );
        View.OnClickListener toggleThemeListener = v -> {
            boolean nextNightMode = !SettingsRepository.getCachedDarkMode(this);
            if (btnToggleLightMode != null) btnToggleLightMode.setEnabled(false);
            if (btnToggleDarkMode != null) btnToggleDarkMode.setEnabled(false);
            if (themeTogglePill != null) themeTogglePill.setEnabled(false);
            bindThemeToggleVisuals(nextNightMode, sunBg, moonBg, sunIcon, moonIcon);

            applyDarkMode(nextNightMode, error -> {
                if (btnToggleLightMode != null) btnToggleLightMode.setEnabled(true);
                if (btnToggleDarkMode != null) btnToggleDarkMode.setEnabled(true);
                if (themeTogglePill != null) themeTogglePill.setEnabled(true);
                bindThemeToggleVisuals(
                        SettingsRepository.getCachedDarkMode(this),
                        sunBg,
                        moonBg,
                        sunIcon,
                        moonIcon
                );
                Toast.makeText(this, R.string.theme_save_error, Toast.LENGTH_SHORT).show();
            });
        };

        if (btnToggleLightMode != null) {
            btnToggleLightMode.setOnClickListener(toggleThemeListener);
        }

        if (btnToggleDarkMode != null) {
            btnToggleDarkMode.setOnClickListener(toggleThemeListener);
        }

        if (themeTogglePill != null) {
            themeTogglePill.setOnClickListener(toggleThemeListener);
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
            topTitle.setText(R.string.app_name_chrome);
        }
    }

    protected void applyDarkMode(boolean darkMode, SettingsRepository.ErrorCallback errorCallback) {
        SettingsRepository.setDarkModeAsync(this, darkMode, settings -> {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            AppCompatDelegate.setDefaultNightMode(
                    settings.isDarkMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        }, errorCallback);
    }

    private void bindThemeToggleVisuals(
            boolean isNightMode,
            View sunBg,
            View moonBg,
            ImageView sunIcon,
            ImageView moonIcon
    ) {
        if (sunBg == null || moonBg == null || sunIcon == null || moonIcon == null) {
            return;
        }
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

    protected void useBackButton() {
        ImageButton menuButton = findViewById(R.id.topActionMenu);
        if (menuButton != null) {
            menuButton.setVisibility(View.VISIBLE);
            menuButton.setImageResource(R.drawable.ic_arrow_back);
            menuButton.setContentDescription(getString(R.string.back));
            menuButton.setImageTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.smart_primary)));
            menuButton.setOnClickListener(v -> finish());
        }
        TextView topTitle = findViewById(R.id.topTitle);
        if (topTitle != null) {
            topTitle.setTextColor(getColor(R.color.smart_primary));
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
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }

    private void applyBottomInsets(View bottomNavigation) {
        int start = bottomNavigation.getPaddingStart();
        int top = bottomNavigation.getPaddingTop();
        int end = bottomNavigation.getPaddingEnd();
        int bottom = bottomNavigation.getPaddingBottom();
        final int originalHeight = bottomNavigation.getLayoutParams() != null
                ? bottomNavigation.getLayoutParams().height
                : 0;

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            android.view.ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp != null && originalHeight > 0) {
                int targetHeight = originalHeight + systemBars.bottom;
                if (lp.height != targetHeight) {
                    lp.height = targetHeight;
                    view.setLayoutParams(lp);
                }
            }
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
            return StatsActivity.class;
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

    public void pickProductPhoto(Consumer<Uri> callback) {
        pickImage(callback);
    }

    public void pickImage(Consumer<Uri> callback) {
        productPhotoCallback = callback;
        productPhotoPicker.launch("image/*");
    }
}
