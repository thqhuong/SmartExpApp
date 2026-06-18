package com.example.smartexpapp.data;

import android.os.Handler;
import android.os.Looper;
import android.content.Context;

import com.example.smartexpapp.SmartExpApplication;
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
    private final Context context;
    private final AppDatabase database;
    private final ExecutorService executor;
    private final Handler mainHandler;

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

    public ProductRepository(Context context, AppDatabase database) {
        this(context, database, Executors.newSingleThreadExecutor());
    }

    public ProductRepository(Context context, AppDatabase database, ExecutorService executor) {
        this.context = context.getApplicationContext();
        this.database = database;
        this.executor = executor;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    // --- Instance-based APIs ---

    public List<Product> getProducts() {
        ensureLocalDefaults(database);
        return mapProducts(database.productDao().getActiveProductsSortedByExpiry());
    }

    public void getProductsAsync(Callback<List<Product>> callback, ErrorCallback errorCallback) {
        execute(this::getProducts, callback, errorCallback);
    }

    public DashboardSnapshot getDashboardSnapshot() {
        ensureLocalDefaults(database);
        List<Product> products = getProducts();
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

    public void getDashboardSnapshotAsync(Callback<DashboardSnapshot> callback, ErrorCallback errorCallback) {
        execute(this::getDashboardSnapshot, callback, errorCallback);
    }

    public Product getProductById(String id) {
        ensureLocalDefaults(database);
        ProductEntity entity = database.productDao().getById(id);
        return entity == null ? null : ProductMapper.toModel(entity);
    }

    public void getProductByIdAsync(String id, Callback<Product> callback, ErrorCallback errorCallback) {
        execute(() -> getProductById(id), callback, errorCallback);
    }

    public void addProduct(Product product) {
        ensureStorageLocations(database);
        database.productDao().insert(ProductMapper.toEntity(product));
    }

    public void addProductAsync(Product product, Callback<Void> callback, ErrorCallback errorCallback) {
        execute(() -> {
            addProduct(product);
            return null;
        }, callback, errorCallback);
    }

    public boolean updateProduct(Product product) {
        Product existing = getProductById(product.getId());
        ensureStorageLocations(database);
        boolean updated = database.productDao().update(ProductMapper.toEntity(product)) > 0;
        if (updated && existing != null) {
            LocalImageRepository.deleteReplacedProductImage(
                    context,
                    existing.getImageUrl(),
                    product.getImageUrl()
            );
        }
        return updated;
    }

    public void updateProductAsync(Product product, Callback<Boolean> callback, ErrorCallback errorCallback) {
        execute(() -> updateProduct(product), callback, errorCallback);
    }

    public void insertExpiryScanAsync(ExpiryScanEntity scan, Callback<Void> callback, ErrorCallback errorCallback) {
        execute(() -> {
            database.expiryScanDao().insert(scan);
            return null;
        }, callback, errorCallback);
    }

    public boolean deleteProduct(String id) {
        Product product = getProductById(id);
        boolean deleted = database.productDao().deleteById(id) > 0;
        if (deleted && product != null) {
            LocalImageRepository.deleteProductImage(context, product.getImageUrl());
        }
        return deleted;
    }

    public void deleteProductAsync(String id, Callback<Boolean> callback, ErrorCallback errorCallback) {
        execute(() -> deleteProduct(id), callback, errorCallback);
    }

    public List<Product> getExpiringBetween(long startMillis, long endMillis) {
        ensureLocalDefaults(database);
        return mapProducts(database.productDao().getExpiringBetween(startMillis, endMillis));
    }

    public void getExpiringBetweenAsync(long startMillis, long endMillis, Callback<List<Product>> callback, ErrorCallback errorCallback) {
        execute(() -> getExpiringBetween(startMillis, endMillis), callback, errorCallback);
    }

    public List<Product> getExpiredProducts() {
        ensureLocalDefaults(database);
        return mapProducts(database.productDao().getExpiredBefore(startOfToday()));
    }

    public List<Product> search(String query) {
        ensureLocalDefaults(database);
        if (query == null || query.trim().isEmpty()) {
            return getProducts();
        }
        return mapProducts(database.productDao().searchActive(query.trim()));
    }

    public void searchAsync(String query, Callback<List<Product>> callback, ErrorCallback errorCallback) {
        execute(() -> search(query), callback, errorCallback);
    }

    public List<Product> filter(String status, String storageLocationId) {
        ensureLocalDefaults(database);
        return mapProducts(database.productDao().filter(status, storageLocationId));
    }

    public void getAllProductsAsync(Callback<List<Product>> callback, ErrorCallback errorCallback) {
        execute(() -> filter(null, null), callback, errorCallback);
    }

    public boolean markConsumed(String id, String note) {
        return markStatus(id, ProductStatus.CONSUMED, note);
    }

    public void markConsumedAsync(String id, String note, Callback<Boolean> callback, ErrorCallback errorCallback) {
        execute(() -> markConsumed(id, note), callback, errorCallback);
    }

    public boolean markWasted(String id, String note) {
        return markStatus(id, ProductStatus.WASTED, note);
    }

    public void markWastedAsync(String id, String note, Callback<Boolean> callback, ErrorCallback errorCallback) {
        execute(() -> markWasted(id, note), callback, errorCallback);
    }

    public boolean markDonated(String id, String note) {
        return markStatus(id, ProductStatus.DONATED, note);
    }

    public void markDonatedAsync(String id, String note, Callback<Boolean> callback, ErrorCallback errorCallback) {
        execute(() -> markDonated(id, note), callback, errorCallback);
    }

    public boolean markExpired(String id, String note) {
        return markStatus(id, ProductStatus.EXPIRED, note);
    }

    public boolean markDeleted(String id, String note) {
        return markStatus(id, ProductStatus.DELETED, note);
    }

    public void markDeletedAsync(String id, String note, Callback<Boolean> callback, ErrorCallback errorCallback) {
        execute(() -> markDeleted(id, note), callback, errorCallback);
    }

    public boolean markActive(String id, String note) {
        return markStatus(id, ProductStatus.ACTIVE, note);
    }

    public void markActiveAsync(String id, String note, Callback<Boolean> callback, ErrorCallback errorCallback) {
        execute(() -> markActive(id, note), callback, errorCallback);
    }

    public int getWastePreventedCount() {
        return getWastePreventedCount(database);
    }

    public void getWastePreventedCountAsync(Callback<Integer> callback, ErrorCallback errorCallback) {
        execute(this::getWastePreventedCount, callback, errorCallback);
    }

    private boolean markStatus(String id, String status, String note) {
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

    // --- Legacy Static compatibility delegators (Context-taking) ---

    private static ProductRepository getContainerRepository(Context context) {
        return ((SmartExpApplication) context.getApplicationContext()).appContainer.getProductRepository();
    }

    @Deprecated
    public static List<Product> getProducts(Context context) {
        return getContainerRepository(context).getProducts();
    }

    @Deprecated
    public static void getProductsAsync(Context context, Callback<List<Product>> callback, ErrorCallback errorCallback) {
        getContainerRepository(context).getProductsAsync(callback, errorCallback);
    }

    @Deprecated
    public static DashboardSnapshot getDashboardSnapshot(Context context) {
        return getContainerRepository(context).getDashboardSnapshot();
    }

    @Deprecated
    public static void getDashboardSnapshotAsync(Context context, Callback<DashboardSnapshot> callback, ErrorCallback errorCallback) {
        getContainerRepository(context).getDashboardSnapshotAsync(callback, errorCallback);
    }

    @Deprecated
    public static Product getProductById(Context context, String id) {
        return getContainerRepository(context).getProductById(id);
    }

    @Deprecated
    public static void getProductByIdAsync(Context context, String id, Callback<Product> callback, ErrorCallback errorCallback) {
        getContainerRepository(context).getProductByIdAsync(id, callback, errorCallback);
    }

    @Deprecated
    public static void addProduct(Context context, Product product) {
        getContainerRepository(context).addProduct(product);
    }

    @Deprecated
    public static void addProductAsync(Context context, Product product, Callback<Void> callback, ErrorCallback errorCallback) {
        getContainerRepository(context).addProductAsync(product, callback, errorCallback);
    }

    @Deprecated
    public static boolean updateProduct(Context context, Product product) {
        return getContainerRepository(context).updateProduct(product);
    }

    @Deprecated
    public static void updateProductAsync(Context context, Product product, Callback<Boolean> callback, ErrorCallback errorCallback) {
        getContainerRepository(context).updateProductAsync(product, callback, errorCallback);
    }

    @Deprecated
    public static void insertExpiryScanAsync(Context context, ExpiryScanEntity scan, Callback<Void> callback, ErrorCallback errorCallback) {
        getContainerRepository(context).insertExpiryScanAsync(scan, callback, errorCallback);
    }

    @Deprecated
    public static boolean deleteProduct(Context context, String id) {
        return getContainerRepository(context).deleteProduct(id);
    }

    @Deprecated
    public static void deleteProductAsync(Context context, String id, Callback<Boolean> callback, ErrorCallback errorCallback) {
        getContainerRepository(context).deleteProductAsync(id, callback, errorCallback);
    }

    @Deprecated
    public static List<Product> getExpiringBetween(Context context, long startMillis, long endMillis) {
        return getContainerRepository(context).getExpiringBetween(startMillis, endMillis);
    }

    @Deprecated
    public static void getExpiringBetweenAsync(Context context, long startMillis, long endMillis, Callback<List<Product>> callback, ErrorCallback errorCallback) {
        getContainerRepository(context).getExpiringBetweenAsync(startMillis, endMillis, callback, errorCallback);
    }

    @Deprecated
    public static List<Product> getExpiredProducts(Context context) {
        return getContainerRepository(context).getExpiredProducts();
    }

    @Deprecated
    public static List<Product> search(Context context, String query) {
        return getContainerRepository(context).search(query);
    }

    @Deprecated
    public static void searchAsync(Context context, String query, Callback<List<Product>> callback, ErrorCallback errorCallback) {
        getContainerRepository(context).searchAsync(query, callback, errorCallback);
    }

    @Deprecated
    public static List<Product> filter(Context context, String status, String storageLocationId) {
        return getContainerRepository(context).filter(status, storageLocationId);
    }

    @Deprecated
    public static boolean markConsumed(Context context, String id) {
        return getContainerRepository(context).markConsumed(id, null);
    }

    @Deprecated
    public static boolean markConsumed(Context context, String id, String note) {
        return getContainerRepository(context).markConsumed(id, note);
    }

    @Deprecated
    public static void markConsumedAsync(Context context, String id, String note, Callback<Boolean> callback, ErrorCallback errorCallback) {
        getContainerRepository(context).markConsumedAsync(id, note, callback, errorCallback);
    }

    @Deprecated
    public static boolean markWasted(Context context, String id) {
        return getContainerRepository(context).markWasted(id, null);
    }

    @Deprecated
    public static boolean markWasted(Context context, String id, String note) {
        return getContainerRepository(context).markWasted(id, note);
    }

    @Deprecated
    public static void markWastedAsync(Context context, String id, String note, Callback<Boolean> callback, ErrorCallback errorCallback) {
        getContainerRepository(context).markWastedAsync(id, note, callback, errorCallback);
    }

    @Deprecated
    public static boolean markDonated(Context context, String id) {
        return getContainerRepository(context).markDonated(id, null);
    }

    @Deprecated
    public static boolean markDonated(Context context, String id, String note) {
        return getContainerRepository(context).markDonated(id, note);
    }

    @Deprecated
    public static void markDonatedAsync(Context context, String id, String note, Callback<Boolean> callback, ErrorCallback errorCallback) {
        getContainerRepository(context).markDonatedAsync(id, note, callback, errorCallback);
    }

    @Deprecated
    public static boolean markExpired(Context context, String id) {
        return getContainerRepository(context).markExpired(id, null);
    }

    @Deprecated
    public static boolean markExpired(Context context, String id, String note) {
        return getContainerRepository(context).markExpired(id, note);
    }

    @Deprecated
    public static int getWastePreventedCount(Context context) {
        return getContainerRepository(context).getWastePreventedCount();
    }

    @Deprecated
    public static void getWastePreventedCountAsync(Context context, Callback<Integer> callback, ErrorCallback errorCallback) {
        getContainerRepository(context).getWastePreventedCountAsync(callback, errorCallback);
    }

    // --- Legacy Database-Taking Static Methods (for testing and internal compatibility) ---

    @Deprecated
    public static List<Product> getProducts(AppDatabase database) {
        return mapProducts(database.productDao().getActiveProductsSortedByExpiry());
    }

    @Deprecated
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

    @Deprecated
    public static Product getProductById(AppDatabase database, String id) {
        ProductEntity entity = database.productDao().getById(id);
        return entity == null ? null : ProductMapper.toModel(entity);
    }

    @Deprecated
    public static void addProduct(AppDatabase database, Product product) {
        ensureStorageLocations(database);
        database.productDao().insert(ProductMapper.toEntity(product));
    }

    @Deprecated
    public static boolean updateProduct(AppDatabase database, Product product) {
        ensureStorageLocations(database);
        return database.productDao().update(ProductMapper.toEntity(product)) > 0;
    }

    @Deprecated
    public static boolean deleteProduct(AppDatabase database, String id) {
        return database.productDao().deleteById(id) > 0;
    }

    @Deprecated
    public static List<Product> getExpiringBetween(AppDatabase database, long startMillis, long endMillis) {
        return mapProducts(database.productDao().getExpiringBetween(startMillis, endMillis));
    }

    @Deprecated
    public static List<Product> getExpiredProducts(AppDatabase database) {
        return mapProducts(database.productDao().getExpiredBefore(startOfToday()));
    }

    @Deprecated
    public static List<Product> search(AppDatabase database, String query) {
        if (query == null || query.trim().isEmpty()) {
            return getProducts(database);
        }
        return mapProducts(database.productDao().searchActive(query.trim()));
    }

    @Deprecated
    public static List<Product> filter(AppDatabase database, String status, String storageLocationId) {
        return mapProducts(database.productDao().filter(status, storageLocationId));
    }

    @Deprecated
    public static boolean markConsumed(AppDatabase database, String id, String note) {
        return markStatusStatic(database, id, ProductStatus.CONSUMED, note);
    }

    @Deprecated
    public static boolean markWasted(AppDatabase database, String id, String note) {
        return markStatusStatic(database, id, ProductStatus.WASTED, note);
    }

    @Deprecated
    public static boolean markDonated(AppDatabase database, String id, String note) {
        return markStatusStatic(database, id, ProductStatus.DONATED, note);
    }

    @Deprecated
    public static boolean markExpired(AppDatabase database, String id, String note) {
        return markStatusStatic(database, id, ProductStatus.EXPIRED, note);
    }

    @Deprecated
    public static int getWastePreventedCount(AppDatabase database) {
        return database.inventoryActionDao().countByActionTypes(new String[] {
                ProductStatus.CONSUMED,
                ProductStatus.DONATED
        });
    }

    @Deprecated
    public static void ensureLocalDefaults(AppDatabase database) {
        database.runInTransaction(() -> {
            ensureStorageLocations(database);
            ensureDefaultSettings(database);
        });
    }

    private static boolean markStatusStatic(AppDatabase database, String id, String status, String note) {
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

    // --- Private Static Helpers ---

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

    private interface Work<T> {
        T run() throws Exception;
    }

    private <T> void execute(Work<T> work, Callback<T> callback, ErrorCallback errorCallback) {
        executor.execute(() -> {
            try {
                T result = work.run();
                if (callback != null) {
                    mainHandler.post(() -> callback.onResult(result));
                }
            } catch (Exception error) {
                if (errorCallback != null) {
                    mainHandler.post(() -> errorCallback.onError(error));
                }
            }
        });
    }
}
