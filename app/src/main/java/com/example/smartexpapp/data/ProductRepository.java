package com.example.smartexpapp.data;

import android.os.Handler;
import android.os.Looper;
import android.content.Context;

import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.ExpiryScanEntity;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ProductRepository {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static final class DashboardSnapshot {
        private final List<Product> activeProducts;
        private final int totalTracked;
        private final int urgentCount;
        private final int expiredCount;
        private final int wastePreventedCount;

        private DashboardSnapshot(List<Product> activeProducts, int urgentCount, int expiredCount, int wastePreventedCount) {
            this.activeProducts = new ArrayList<>(activeProducts);
            this.totalTracked = activeProducts.size();
            this.urgentCount = urgentCount;
            this.expiredCount = expiredCount;
            this.wastePreventedCount = wastePreventedCount;
        }

        public List<Product> getActiveProducts() {
            return new ArrayList<>(activeProducts);
        }

        public int getTotalTracked() {
            return totalTracked;
        }

        public int getUrgentCount() {
            return urgentCount;
        }

        public int getExpiredCount() {
            return expiredCount;
        }

        public int getWastePreventedCount() {
            return wastePreventedCount;
        }
    }

    public interface Callback<T> {
        void onResult(T value);
    }

    public interface ErrorCallback {
        void onError(Exception error);
    }

    private ProductRepository() {
    }

    public static List<Product> getProducts(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        ensureLocalDefaults(database);
        return getProducts(database);
    }

    public static void getProductsAsync(Context context, Callback<List<Product>> callback, ErrorCallback errorCallback) {
        execute(() -> getProducts(context), callback, errorCallback);
    }

    public static List<Product> getProducts(AppDatabase database) {
        return mapProducts(database.productDao().getActiveProductsSortedByExpiry());
    }

    public static DashboardSnapshot getDashboardSnapshot(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        ensureLocalDefaults(database);
        return getDashboardSnapshot(database);
    }

    public static void getDashboardSnapshotAsync(Context context, Callback<DashboardSnapshot> callback, ErrorCallback errorCallback) {
        execute(() -> getDashboardSnapshot(context), callback, errorCallback);
    }

    public static DashboardSnapshot getDashboardSnapshot(AppDatabase database) {
        List<Product> products = getProducts(database);
        int urgentCount = 0;
        int expiredCount = 0;
        for (Product product : products) {
            if (product.isExpiringSoon()) {
                urgentCount++;
            }
            if (product.isExpired()) {
                expiredCount++;
            }
        }
        return new DashboardSnapshot(products, urgentCount, expiredCount, getWastePreventedCount(database));
    }

    public static Product getProductById(Context context, String id) {
        AppDatabase database = AppDatabase.getInstance(context);
        ensureLocalDefaults(database);
        return getProductById(database, id);
    }

    public static void getProductByIdAsync(Context context, String id, Callback<Product> callback, ErrorCallback errorCallback) {
        execute(() -> getProductById(context, id), callback, errorCallback);
    }

    public static Product getProductById(AppDatabase database, String id) {
        ProductEntity entity = database.productDao().getById(id);
        return entity == null ? null : ProductMapper.toModel(entity);
    }

    public static void addProduct(Context context, Product product) {
        addProduct(AppDatabase.getInstance(context), product);
    }

    public static void addProductAsync(Context context, Product product, Callback<Void> callback, ErrorCallback errorCallback) {
        execute(() -> {
            addProduct(context, product);
            return null;
        }, callback, errorCallback);
    }

    public static void addProduct(AppDatabase database, Product product) {
        ensureStorageLocations(database);
        database.productDao().insert(ProductMapper.toEntity(product));
    }

    public static boolean updateProduct(Context context, Product product) {
        AppDatabase database = AppDatabase.getInstance(context);
        Product existing = getProductById(database, product.getId());
        boolean updated = updateProduct(database, product);
        if (updated && existing != null) {
            LocalImageRepository.deleteReplacedProductImage(
                    context.getApplicationContext(),
                    existing.getImageUrl(),
                    product.getImageUrl()
            );
        }
        return updated;
    }

    public static void updateProductAsync(Context context, Product product, Callback<Boolean> callback, ErrorCallback errorCallback) {
        execute(() -> updateProduct(context, product), callback, errorCallback);
    }

    public static void insertExpiryScanAsync(Context context, ExpiryScanEntity scan, Callback<Void> callback, ErrorCallback errorCallback) {
        execute(() -> {
            AppDatabase.getInstance(context).expiryScanDao().insert(scan);
            return null;
        }, callback, errorCallback);
    }

    public static boolean updateProduct(AppDatabase database, Product product) {
        ensureStorageLocations(database);
        return database.productDao().update(ProductMapper.toEntity(product)) > 0;
    }

    public static boolean deleteProduct(Context context, String id) {
        AppDatabase database = AppDatabase.getInstance(context);
        Product product = getProductById(database, id);
        boolean deleted = deleteProduct(database, id);
        if (deleted && product != null) {
            LocalImageRepository.deleteProductImage(context.getApplicationContext(), product.getImageUrl());
        }
        return deleted;
    }

    public static void deleteProductAsync(Context context, String id, Callback<Boolean> callback, ErrorCallback errorCallback) {
        execute(() -> deleteProduct(context, id), callback, errorCallback);
    }

    public static boolean deleteProduct(AppDatabase database, String id) {
        return database.productDao().deleteById(id) > 0;
    }

    public static List<Product> getExpiringBetween(Context context, long startMillis, long endMillis) {
        AppDatabase database = AppDatabase.getInstance(context);
        ensureLocalDefaults(database);
        return getExpiringBetween(database, startMillis, endMillis);
    }

    public static void getExpiringBetweenAsync(Context context, long startMillis, long endMillis, Callback<List<Product>> callback, ErrorCallback errorCallback) {
        execute(() -> getExpiringBetween(context, startMillis, endMillis), callback, errorCallback);
    }

    public static List<Product> getExpiringBetween(AppDatabase database, long startMillis, long endMillis) {
        return mapProducts(database.productDao().getExpiringBetween(startMillis, endMillis));
    }

    public static List<Product> getExpiredProducts(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        ensureLocalDefaults(database);
        return getExpiredProducts(database);
    }

    public static List<Product> getExpiredProducts(AppDatabase database) {
        return mapProducts(database.productDao().getExpiredBefore(startOfToday()));
    }

    public static List<Product> search(Context context, String query) {
        AppDatabase database = AppDatabase.getInstance(context);
        ensureLocalDefaults(database);
        return search(database, query);
    }

    public static void searchAsync(Context context, String query, Callback<List<Product>> callback, ErrorCallback errorCallback) {
        execute(() -> search(context, query), callback, errorCallback);
    }

    public static List<Product> search(AppDatabase database, String query) {
        if (query == null || query.trim().isEmpty()) {
            return getProducts(database);
        }
        return mapProducts(database.productDao().searchActive(query.trim()));
    }

    public static List<Product> filter(Context context, String status, String storageLocationId) {
        AppDatabase database = AppDatabase.getInstance(context);
        ensureLocalDefaults(database);
        return filter(database, status, storageLocationId);
    }

    public static List<Product> filter(AppDatabase database, String status, String storageLocationId) {
        return mapProducts(database.productDao().filter(status, storageLocationId));
    }

    public static boolean markConsumed(Context context, String id) {
        return markConsumed(AppDatabase.getInstance(context), id, null);
    }

    public static boolean markConsumed(Context context, String id, String note) {
        return markConsumed(AppDatabase.getInstance(context), id, note);
    }

    public static void markConsumedAsync(Context context, String id, String note, Callback<Boolean> callback, ErrorCallback errorCallback) {
        execute(() -> markConsumed(context, id, note), callback, errorCallback);
    }

    public static boolean markConsumed(AppDatabase database, String id, String note) {
        return markStatus(database, id, ProductStatus.CONSUMED, note);
    }

    public static boolean markWasted(Context context, String id) {
        return markWasted(AppDatabase.getInstance(context), id, null);
    }

    public static boolean markWasted(Context context, String id, String note) {
        return markWasted(AppDatabase.getInstance(context), id, note);
    }

    public static void markWastedAsync(Context context, String id, String note, Callback<Boolean> callback, ErrorCallback errorCallback) {
        execute(() -> markWasted(context, id, note), callback, errorCallback);
    }

    public static boolean markWasted(AppDatabase database, String id, String note) {
        return markStatus(database, id, ProductStatus.WASTED, note);
    }

    public static boolean markDonated(Context context, String id) {
        return markDonated(AppDatabase.getInstance(context), id, null);
    }

    public static boolean markDonated(Context context, String id, String note) {
        return markDonated(AppDatabase.getInstance(context), id, note);
    }

    public static void markDonatedAsync(Context context, String id, String note, Callback<Boolean> callback, ErrorCallback errorCallback) {
        execute(() -> markDonated(context, id, note), callback, errorCallback);
    }

    public static boolean markDonated(AppDatabase database, String id, String note) {
        return markStatus(database, id, ProductStatus.DONATED, note);
    }

    public static boolean markExpired(Context context, String id) {
        return markExpired(AppDatabase.getInstance(context), id, null);
    }

    public static boolean markExpired(Context context, String id, String note) {
        return markExpired(AppDatabase.getInstance(context), id, note);
    }

    public static boolean markExpired(AppDatabase database, String id, String note) {
        return markStatus(database, id, ProductStatus.EXPIRED, note);
    }

    public static int getWastePreventedCount(Context context) {
        return getWastePreventedCount(AppDatabase.getInstance(context));
    }

    public static void getWastePreventedCountAsync(Context context, Callback<Integer> callback, ErrorCallback errorCallback) {
        execute(() -> getWastePreventedCount(context), callback, errorCallback);
    }

    public static int getWastePreventedCount(AppDatabase database) {
        return database.inventoryActionDao().countByActionTypes(new String[] {
                ProductStatus.CONSUMED,
                ProductStatus.DONATED
        });
    }

    public static void ensureLocalDefaults(AppDatabase database) {
        database.runInTransaction(() -> {
            ensureStorageLocations(database);
            ensureDefaultSettings(database);
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

    private interface Work<T> {
        T run() throws Exception;
    }

    private static <T> void execute(Work<T> work, Callback<T> callback, ErrorCallback errorCallback) {
        EXECUTOR.execute(() -> {
            try {
                T result = work.run();
                if (callback != null) {
                    MAIN.post(() -> callback.onResult(result));
                }
            } catch (Exception error) {
                if (errorCallback != null) {
                    MAIN.post(() -> errorCallback.onError(error));
                }
            }
        });
    }
}
