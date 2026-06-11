package com.example.smartexpapp;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.widget.ImageButton;
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
    protected void setupChrome(@IdRes int selectedNavItemId) {
        ImageButton menuButton = findViewById(R.id.topActionMenu);
        ImageButton notificationButton = findViewById(R.id.topActionNotifications);
        View bottomNavigation = findViewById(R.id.bottomNavigation);

        if (menuButton != null) {
            menuButton.setOnClickListener(v -> Toast.makeText(this, "Menu placeholder", Toast.LENGTH_SHORT).show());
        }
        if (notificationButton != null) {
            notificationButton.setOnClickListener(v -> Toast.makeText(this, "Sample alerts only for now.", Toast.LENGTH_SHORT).show());
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
            startActivity(new Intent(this, target));
            overridePendingTransition(0, 0);
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
