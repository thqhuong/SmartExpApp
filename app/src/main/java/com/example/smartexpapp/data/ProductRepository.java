package com.example.smartexpapp.data;

import android.content.Context;

import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.InventoryActionEntity;
import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.data.local.ProductEntity;
import com.example.smartexpapp.data.local.ProductMapper;
import com.example.smartexpapp.data.local.StorageLocationEntity;
import com.example.smartexpapp.data.local.UserSettingsEntity;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.model.ProductStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ProductRepository {
    private ProductRepository() {
    }

    public static List<Product> getProducts(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        seedDemoProductsIfEmpty(database);
        return getProducts(database);
    }

    public static List<Product> getProducts(AppDatabase database) {
        return mapProducts(database.productDao().getActiveProductsSortedByExpiry());
    }

    public static Product getProductById(Context context, String id) {
        AppDatabase database = AppDatabase.getInstance(context);
        seedDemoProductsIfEmpty(database);
        return getProductById(database, id);
    }

    public static Product getProductById(AppDatabase database, String id) {
        ProductEntity entity = database.productDao().getById(id);
        return entity == null ? null : ProductMapper.toModel(entity);
    }

    public static void addProduct(Context context, Product product) {
        addProduct(AppDatabase.getInstance(context), product);
    }

    public static void addProduct(AppDatabase database, Product product) {
        ensureStorageLocations(database);
        database.productDao().insert(ProductMapper.toEntity(product));
    }

    public static boolean updateProduct(Context context, Product product) {
        return updateProduct(AppDatabase.getInstance(context), product);
    }

    public static boolean updateProduct(AppDatabase database, Product product) {
        ensureStorageLocations(database);
        return database.productDao().update(ProductMapper.toEntity(product)) > 0;
    }

    public static boolean deleteProduct(Context context, String id) {
        return deleteProduct(AppDatabase.getInstance(context), id);
    }

    public static boolean deleteProduct(AppDatabase database, String id) {
        return database.productDao().deleteById(id) > 0;
    }

    public static List<Product> getExpiringBetween(Context context, long startMillis, long endMillis) {
        AppDatabase database = AppDatabase.getInstance(context);
        seedDemoProductsIfEmpty(database);
        return getExpiringBetween(database, startMillis, endMillis);
    }

    public static List<Product> getExpiringBetween(AppDatabase database, long startMillis, long endMillis) {
        return mapProducts(database.productDao().getExpiringBetween(startMillis, endMillis));
    }

    public static List<Product> getExpiredProducts(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        seedDemoProductsIfEmpty(database);
        return getExpiredProducts(database);
    }

    public static List<Product> getExpiredProducts(AppDatabase database) {
        return mapProducts(database.productDao().getExpiredBefore(startOfToday()));
    }

    public static List<Product> search(Context context, String query) {
        AppDatabase database = AppDatabase.getInstance(context);
        seedDemoProductsIfEmpty(database);
        return search(database, query);
    }

    public static List<Product> search(AppDatabase database, String query) {
        if (query == null || query.trim().isEmpty()) {
            return getProducts(database);
        }
        return mapProducts(database.productDao().searchActive(query.trim()));
    }

    public static List<Product> filter(Context context, String status, String storageLocationId) {
        AppDatabase database = AppDatabase.getInstance(context);
        seedDemoProductsIfEmpty(database);
        return filter(database, status, storageLocationId);
    }

    public static List<Product> filter(AppDatabase database, String status, String storageLocationId) {
        return mapProducts(database.productDao().filter(status, storageLocationId));
    }

    public static boolean markConsumed(Context context, String id) {
        return markConsumed(AppDatabase.getInstance(context), id, null);
    }

    public static boolean markConsumed(AppDatabase database, String id, String note) {
        return markStatus(database, id, ProductStatus.CONSUMED, note);
    }

    public static boolean markWasted(Context context, String id) {
        return markWasted(AppDatabase.getInstance(context), id, null);
    }

    public static boolean markWasted(AppDatabase database, String id, String note) {
        return markStatus(database, id, ProductStatus.WASTED, note);
    }

    public static boolean markDonated(Context context, String id) {
        return markDonated(AppDatabase.getInstance(context), id, null);
    }

    public static boolean markDonated(AppDatabase database, String id, String note) {
        return markStatus(database, id, ProductStatus.DONATED, note);
    }

    public static boolean markExpired(Context context, String id) {
        return markExpired(AppDatabase.getInstance(context), id, null);
    }

    public static boolean markExpired(AppDatabase database, String id, String note) {
        return markStatus(database, id, ProductStatus.EXPIRED, note);
    }

    public static void seedDemoProductsIfEmpty(AppDatabase database) {
        database.runInTransaction(() -> {
            ensureStorageLocations(database);
            ensureDefaultSettings(database);
            if (database.productDao().count() == 0) {
                List<ProductEntity> products = new ArrayList<>();
                for (Product product : SampleData.products()) {
                    products.add(ProductMapper.toEntity(product));
                }
                database.productDao().insertAll(products);
            }
        });
    }

    private static boolean markStatus(AppDatabase database, String id, String status, String note) {
        ensureStorageLocations(database);
        final boolean[] updated = {false};
        database.runInTransaction(() -> {
            long now = System.currentTimeMillis();
            int rows = database.productDao().updateStatus(id, status, now, Product.SYNC_STATUS_LOCAL);
            updated[0] = rows > 0;
            if (updated[0]) {
                database.inventoryActionDao().insert(actionFor(id, status, note, now));
            }
        });
        return updated[0];
    }

    private static InventoryActionEntity actionFor(String productId, String actionType, String note, long now) {
        InventoryActionEntity action = new InventoryActionEntity();
        action.id = UUID.randomUUID().toString();
        action.productId = productId;
        action.actionType = actionType;
        action.quantityChanged = 0;
        action.actionAt = now;
        action.note = note;
        action.createdAt = now;
        action.updatedAt = now;
        return action;
    }

    private static void ensureStorageLocations(AppDatabase database) {
        if (database.storageLocationDao().count() > 0) {
            return;
        }

        long now = System.currentTimeMillis();
        database.storageLocationDao().insertAll(Arrays.asList(
                new StorageLocationEntity(LocalDataContract.STORAGE_ROOM_TEMP_ID, LocalDataContract.STORAGE_ROOM_TEMP_NAME, "ROOM", 0, now, now),
                new StorageLocationEntity(LocalDataContract.STORAGE_REFRIGERATOR_ID, LocalDataContract.STORAGE_REFRIGERATOR_NAME, "COLD", 1, now, now),
                new StorageLocationEntity(LocalDataContract.STORAGE_FREEZE_ID, LocalDataContract.STORAGE_FREEZE_NAME, "FROZEN", 2, now, now)
        ));
    }

    private static void ensureDefaultSettings(AppDatabase database) {
        if (database.userSettingsDao().getById("default") != null) {
            return;
        }
        long now = System.currentTimeMillis();
        UserSettingsEntity settings = new UserSettingsEntity();
        settings.id = "default";
        settings.defaultStorageLocationId = LocalDataContract.STORAGE_ROOM_TEMP_ID;
        settings.reminderDaysBefore = 3;
        settings.notificationEnabled = true;
        settings.createdAt = now;
        settings.updatedAt = now;
        database.userSettingsDao().insert(settings);
    }

    private static List<Product> mapProducts(List<ProductEntity> entities) {
        List<Product> products = new ArrayList<>();
        for (ProductEntity entity : entities) {
            products.add(ProductMapper.toModel(entity));
        }
        return products;
    }

    private static long startOfToday() {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
