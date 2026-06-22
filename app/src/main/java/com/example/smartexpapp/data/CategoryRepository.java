package com.example.smartexpapp.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.example.smartexpapp.R;
import com.example.smartexpapp.data.firestore.ProductSyncRepository;
import com.example.smartexpapp.data.firestore.UserDataSyncRepository;
import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.CategoryEntity;
import com.example.smartexpapp.data.local.ProductEntity;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.util.CategoryColorHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CategoryRepository {
    private static final String PREFS = "category_prefs";
    private static final String KEY_CUSTOMIZED_DEFAULTS = "customized_default_categories";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private static final String[] BUILT_IN = {"Dairy", "General", "Meat", "Pantry", "Produce", "Vegetables"};
    private static final int[] BUILT_IN_LABELS = {
            R.string.cat_dairy,
            R.string.cat_general,
            R.string.cat_meat,
            R.string.cat_pantry,
            R.string.cat_produce,
            R.string.cat_vegetables
    };

    public interface Callback<T> {
        void onResult(T value);
    }

    public interface ErrorCallback {
        void onError(Exception error);
    }

    private CategoryRepository() {
    }

    public static List<String> getDisplayCategories(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        return getDisplayCategories(context, database);
    }

    public static List<String> getDisplayCategories(Context context, AppDatabase database) {
        ensureSeeded(context, database);
        List<String> names = new ArrayList<>();
        for (CategoryEntity category : database.categoryDao().getActiveCategories()) {
            names.add(displayName(context, category.name));
        }
        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static void getDisplayCategoriesAsync(Context context, Callback<List<String>> callback, ErrorCallback errorCallback) {
        execute(() -> getDisplayCategories(context), callback, errorCallback);
    }

    public static void addCategoryAsync(Context context, String name, Callback<Boolean> callback, ErrorCallback errorCallback) {
        execute(() -> addCategory(context, AppDatabase.getInstance(context), name), callback, errorCallback);
    }

    public static void renameCategoryAsync(Context context, String oldName, String newName, Callback<Boolean> callback, ErrorCallback errorCallback) {
        execute(() -> renameCategory(context, AppDatabase.getInstance(context), oldName, newName), callback, errorCallback);
    }

    public static void deleteCategoryAsync(Context context, String name, Callback<Boolean> callback, ErrorCallback errorCallback) {
        execute(() -> deleteCategory(context, AppDatabase.getInstance(context), name), callback, errorCallback);
    }

    public static boolean addCategory(Context context, String name) {
        AppDatabase database = AppDatabase.getInstance(context);
        return addCategory(context, database, name);
    }

    public static boolean addCategory(Context context, AppDatabase database, String name) {
        ensureSeeded(context, database);
        String canonical = canonicalName(context, name);
        if (!hasText(canonical)) {
            return false;
        }
        CategoryEntity existing = database.categoryDao().getByName(canonical);
        long now = System.currentTimeMillis();
        if (existing != null) {
            if (existing.active && existing.deletedAt == null) {
                return false;
            }
            existing.active = true;
            existing.deletedAt = null;
            existing.updatedAt = now;
            existing.syncStatus = syncStatusForWrite(context);
            database.categoryDao().update(existing);
            syncCategories(context, database);
            return true;
        }
        CategoryEntity category = newCategory(canonical, false, database.categoryDao().count() + 1, now);
        category.syncStatus = syncStatusForWrite(context);
        category.ownerUserId = AuthStateRepository.getSignedInOwnerUserId(context);
        database.categoryDao().insert(category);
        syncCategories(context, database);
        return true;
    }

    public static boolean renameCategory(Context context, String oldName, String newName) {
        AppDatabase database = AppDatabase.getInstance(context);
        return renameCategory(context, database, oldName, newName);
    }

    public static boolean renameCategory(Context context, AppDatabase database, String oldName, String newName) {
        ensureSeeded(context, database);
        String oldCanonical = canonicalName(context, oldName);
        String newCanonical = canonicalName(context, newName);
        if (!hasText(oldCanonical) || !hasText(newCanonical) || oldCanonical.equals(newCanonical)) {
            return false;
        }
        long now = System.currentTimeMillis();
        String productSyncStatus = syncStatusForWrite(context);
        String ownerUserId = AuthStateRepository.getSignedInOwnerUserId(context);
        database.runInTransaction(() -> {
            CategoryEntity oldCategory = database.categoryDao().getByName(oldCanonical);
            if (oldCategory != null) {
                if (oldCategory.builtIn) {
                    oldCategory.active = false;
                    oldCategory.updatedAt = now;
                    oldCategory.deletedAt = now;
                    oldCategory.syncStatus = productSyncStatus;
                    oldCategory.ownerUserId = ownerUserId;
                    database.categoryDao().update(oldCategory);
                } else {
                    oldCategory.name = newCanonical;
                    oldCategory.updatedAt = now;
                    oldCategory.syncStatus = productSyncStatus;
                    oldCategory.ownerUserId = ownerUserId;
                    database.categoryDao().update(oldCategory);
                }
            }
            if (oldCategory == null || oldCategory.builtIn) {
                CategoryEntity replacement = database.categoryDao().getByName(newCanonical);
                if (replacement == null) {
                    replacement = newCategory(newCanonical, false, database.categoryDao().count() + 1, now);
                    replacement.ownerUserId = ownerUserId;
                    replacement.syncStatus = productSyncStatus;
                } else {
                    replacement.active = true;
                    replacement.deletedAt = null;
                    replacement.updatedAt = now;
                    replacement.syncStatus = productSyncStatus;
                    replacement.ownerUserId = ownerUserId;
                }
                database.categoryDao().insert(replacement);
            }
            database.productDao().renameCategory(oldCanonical, newCanonical, now, productSyncStatus);
        });
        syncCategories(context, database);
        ProductSyncRepository.syncProductsAsync(context, database);
        return true;
    }

    public static boolean deleteCategory(Context context, String name) {
        AppDatabase database = AppDatabase.getInstance(context);
        return deleteCategory(context, database, name);
    }

    public static boolean deleteCategory(Context context, AppDatabase database, String name) {
        ensureSeeded(context, database);
        String canonical = canonicalName(context, name);
        if (!hasText(canonical) || isCategoryUsed(database, canonical)) {
            return false;
        }
        CategoryEntity category = database.categoryDao().getByName(canonical);
        if (category == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        category.active = false;
        category.deletedAt = now;
        category.updatedAt = now;
        category.syncStatus = syncStatusForWrite(context);
        category.ownerUserId = AuthStateRepository.getSignedInOwnerUserId(context);
        database.categoryDao().update(category);
        syncCategories(context, database);
        return true;
    }

    public static boolean isCategoryActive(Context context, String name) {
        AppDatabase database = AppDatabase.getInstance(context);
        return isCategoryActive(context, database, name);
    }

    public static boolean isCategoryActive(Context context, AppDatabase database, String name) {
        ensureSeeded(context, database);
        CategoryEntity category = database.categoryDao().getByName(canonicalName(context, name));
        return category != null && category.active && category.deletedAt == null;
    }

    public static void ensureSeeded(Context context, AppDatabase database) {
        if (database.categoryDao().count() == 0) {
            long now = System.currentTimeMillis();
            List<CategoryEntity> defaults = new ArrayList<>();
            for (int i = 0; i < BUILT_IN.length; i++) {
                defaults.add(newCategory(BUILT_IN[i], true, i, now));
            }
            database.categoryDao().insertAll(defaults);
        }
        importExistingCategoryState(context, database);
    }

    public static String canonicalName(Context context, String category) {
        return CategoryColorHelper.toCanonical(context, category == null ? null : category.trim());
    }

    public static String displayName(Context context, String canonicalName) {
        if (canonicalName == null) {
            return null;
        }
        for (int i = 0; i < BUILT_IN.length; i++) {
            if (BUILT_IN[i].equals(canonicalName)) {
                return context.getString(BUILT_IN_LABELS[i]);
            }
        }
        return canonicalName;
    }

    public static boolean isBuiltInCanonical(String category) {
        if (category == null) {
            return false;
        }
        for (String builtIn : BUILT_IN) {
            if (builtIn.equals(category)) {
                return true;
            }
        }
        return false;
    }

    private static void importExistingCategoryState(Context context, AppDatabase database) {
        long now = System.currentTimeMillis();
        // Clean up duplicates: find any category whose canonical name matches a built-in category
        // but was inserted as a custom category (e.g. name is "Thịt", "Sữa", etc.)
        for (CategoryEntity cat : database.categoryDao().getActiveCategories()) {
            if (!cat.builtIn) {
                String canonical = canonicalName(context, cat.name);
                if (isBuiltInCanonical(canonical)) {
                    cat.active = false;
                    cat.deletedAt = now;
                    cat.updatedAt = now;
                    database.categoryDao().update(cat);
                    database.productDao().renameCategory(cat.name, canonical, now, syncStatusForWrite(context));
                }
            }
        }
        Set<String> inactiveBuiltIns = new HashSet<>(legacyCustomizedBuiltIns(context));
        for (String builtIn : inactiveBuiltIns) {
            CategoryEntity category = database.categoryDao().getByName(canonicalName(context, builtIn));
            if (category != null && category.active) {
                category.active = false;
                category.deletedAt = now;
                category.updatedAt = now;
                database.categoryDao().update(category);
            }
        }
        for (ProductEntity product : database.productDao().getAllProductsSortedByExpiry()) {
            String canonical = canonicalName(context, product.category);
            if (!hasText(canonical) || isBuiltInCanonical(canonical) || database.categoryDao().getByName(canonical) != null) {
                continue;
            }
            CategoryEntity category = newCategory(canonical, false, database.categoryDao().count() + 1, now);
            category.ownerUserId = product.ownerUserId;
            database.categoryDao().insert(category);
        }
    }

    private static Set<String> legacyCustomizedBuiltIns(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getStringSet(KEY_CUSTOMIZED_DEFAULTS, new HashSet<>());
    }

    private static boolean isCategoryUsed(AppDatabase database, String canonicalName) {
        for (ProductEntity product : database.productDao().getAllProductsSortedByExpiry()) {
            if (canonicalName.equals(product.category)) {
                return true;
            }
        }
        return false;
    }

    private static CategoryEntity newCategory(String name, boolean builtIn, int sortOrder, long now) {
        CategoryEntity category = new CategoryEntity();
        category.id = (builtIn ? "builtin_" : "custom_") + name.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "_") + "_" + UUID.randomUUID().toString().substring(0, 8);
        category.name = name;
        category.sortOrder = sortOrder;
        category.builtIn = builtIn;
        category.active = true;
        category.createdAt = now;
        category.updatedAt = now;
        return category;
    }

    private static String syncStatusForWrite(Context context) {
        return ProductSyncRepository.isSyncAvailable(context)
                ? Product.SYNC_STATUS_PENDING_UPLOAD
                : Product.SYNC_STATUS_LOCAL;
    }

    private static void syncCategories(Context context, AppDatabase database) {
        UserDataSyncRepository.syncCategoriesAsync(context, database);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private interface Work<T> {
        T run() throws Exception;
    }

    private static <T> void execute(Work<T> work, Callback<T> callback, ErrorCallback errorCallback) {
        EXECUTOR.execute(() -> {
            try {
                T result = work.run();
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onResult(result));
                }
            } catch (Exception error) {
                if (errorCallback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> errorCallback.onError(error));
                }
            }
        });
    }
}
