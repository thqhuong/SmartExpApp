package com.example.smartexpapp;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartexpapp.data.OcrCaptureRepository;
import com.example.smartexpapp.data.SettingsRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class BaseActivity extends AppCompatActivity {
    private Consumer<Uri> productPhotoCallback;
    private Uri pendingPhotoCaptureUri;
    private View statusBarScrim;

    private final ActivityResultLauncher<String> productPhotoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                Consumer<Uri> callback = productPhotoCallback;
                productPhotoCallback = null;
                if (callback != null) {
                    callback.accept(uri);
                }
            });

    private final ActivityResultLauncher<Uri> productPhotoCamera =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                Uri captured = pendingPhotoCaptureUri;
                pendingPhotoCaptureUri = null;
                Consumer<Uri> callback = productPhotoCallback;
                productPhotoCallback = null;
                if (Boolean.TRUE.equals(success) && captured != null && callback != null) {
                    callback.accept(captured);
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

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applyTopStatusBarInset();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        applyTopStatusBarInset();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        super.setContentView(view, params);
        applyTopStatusBarInset();
    }

    /**
     * Constrains every screen's content so it never draws over the status bar.
     * Android 15+ (targetSdk 36) forces edge-to-edge, so content otherwise spills
     * under the status bar.
     *
     * <p>Two things happen here:
     * <ul>
     *   <li>The status-bar inset is added as top padding on the screen's root view,
     *       so the actual content (top bar, forms, lists) is pushed below the status
     *       bar. The root's own background still fills the whole window (padding
     *       doesn't clip it), so the decorative gradient stays full-bleed.</li>
     *   <li>An opaque scrim is drawn on top of everything, exactly covering the
     *       status-bar region. Some screens disable child clipping on their scroll
     *       containers, which lets scrolled content bleed up into the status-bar
     *       area; the scrim guarantees that content is always covered there.</li>
     * </ul>
     *
     * On pre-edge-to-edge devices the status-bar inset is 0, so nothing changes.
     */
    private void applyTopStatusBarInset() {
        View content = findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) {
            return;
        }
        ViewGroup contentGroup = (ViewGroup) content;
        if (contentGroup.getChildCount() == 0) {
            return;
        }
        View root = contentGroup.getChildAt(0);
        final int start = root.getPaddingStart();
        final int initialTop = root.getPaddingTop();
        final int end = root.getPaddingEnd();
        final int bottom = root.getPaddingBottom();

        final View scrim = ensureStatusBarScrim(contentGroup);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            view.setPaddingRelative(start, initialTop + top, end, bottom);
            ViewGroup.LayoutParams lp = scrim.getLayoutParams();
            if (lp != null && lp.height != top) {
                lp.height = top;
                scrim.setLayoutParams(lp);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private View ensureStatusBarScrim(ViewGroup contentGroup) {
        if (statusBarScrim != null && statusBarScrim.getParent() == contentGroup) {
            statusBarScrim.bringToFront();
            return statusBarScrim;
        }
        View scrim = new View(this);
        scrim.setBackgroundColor(getColor(R.color.smart_background));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, 0, Gravity.TOP);
        contentGroup.addView(scrim, lp);
        statusBarScrim = scrim;
        return scrim;
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

        if (searchButton != null) {
            searchButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, InventoryActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });
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

    /**
     * Lets the user add a product photo either by taking a new picture with the
     * camera or by choosing one from the gallery. The chosen image {@link Uri} is
     * delivered to {@code callback}.
     */
    public void pickProductPhoto(Consumer<Uri> callback) {
        productPhotoCallback = callback;
        String[] options = {
                getString(R.string.ocr_source_camera),
                getString(R.string.ocr_source_gallery)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.add_product_photo_desc)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        launchProductPhotoCamera();
                    } else {
                        productPhotoPicker.launch("image/*");
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> productPhotoCallback = null)
                .setOnCancelListener(dialog -> productPhotoCallback = null)
                .show();
    }

    private void launchProductPhotoCamera() {
        try {
            pendingPhotoCaptureUri = OcrCaptureRepository.createCaptureUri(this);
            productPhotoCamera.launch(pendingPhotoCaptureUri);
        } catch (Exception error) {
            pendingPhotoCaptureUri = null;
            productPhotoCallback = null;
            Toast.makeText(this, R.string.ocr_camera_error, Toast.LENGTH_SHORT).show();
        }
    }

    /** Opens the gallery picker directly, bypassing the camera/gallery chooser. */
    public void pickImage(Consumer<Uri> callback) {
        productPhotoCallback = callback;
        productPhotoPicker.launch("image/*");
    }
}
