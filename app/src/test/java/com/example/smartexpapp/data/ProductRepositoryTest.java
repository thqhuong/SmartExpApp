package com.example.smartexpapp.data;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.model.ProductStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ProductRepositoryTest {
    private AppDatabase database;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void addReadUpdateAndDeleteProduct() {
        Product milk = product("milk-id", "Milk", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 5);
        ProductRepository.addProduct(database, milk);

        List<Product> products = ProductRepository.getProducts(database);
        assertEquals(1, products.size());
        assertEquals("Milk", products.get(0).getName());
        assertEquals(LocalDataContract.STORAGE_REFRIGERATOR_ID, products.get(0).getStorageLocationId());

        Product updated = copyWithName(milk, "Organic Milk");
        assertTrue(ProductRepository.updateProduct(database, updated));
        assertEquals("Organic Milk", ProductRepository.getProductById(database, milk.getId()).getName());

        assertTrue(ProductRepository.deleteProduct(database, milk.getId()));
        assertTrue(ProductRepository.getProducts(database).isEmpty());
        assertNull(ProductRepository.getProductById(database, milk.getId()));
    }

    @Test
    public void getProductsReturnsActiveProductsSortedByExpiryDate() {
        Product far = product("far-id", "Pasta", "Pantry", LocalDataContract.STORAGE_ROOM_TEMP_NAME, 30);
        Product soon = product("soon-id", "Yogurt", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 2);
        ProductRepository.addProduct(database, far);
        ProductRepository.addProduct(database, soon);

        List<Product> products = ProductRepository.getProducts(database);

        assertEquals(2, products.size());
        assertEquals("Yogurt", products.get(0).getName());
        assertEquals("Pasta", products.get(1).getName());
    }

    @Test
    public void searchFilterAndExpiryQueriesUseRoomData() {
        Product yogurt = product("yogurt-id", "Greek Yogurt", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 2);
        Product peas = product("peas-id", "Frozen Peas", "Vegetables", LocalDataContract.STORAGE_FREEZE_NAME, 90);
        Product expired = product("bread-id", "Bread", "Pantry", LocalDataContract.STORAGE_ROOM_TEMP_NAME, -1);
        ProductRepository.addProduct(database, yogurt);
        ProductRepository.addProduct(database, peas);
        ProductRepository.addProduct(database, expired);

        assertEquals(1, ProductRepository.search(database, "yogurt").size());
        assertEquals(1, ProductRepository.filter(database, ProductStatus.ACTIVE, LocalDataContract.STORAGE_FREEZE_ID).size());
        assertEquals("Bread", ProductRepository.getExpiredProducts(database).get(0).getName());

        long start = startOfToday();
        long end = expiryMillisForOffset(3);
        List<Product> expiringSoon = ProductRepository.getExpiringBetween(database, start, end);
        assertEquals(1, expiringSoon.size());
        assertEquals("Greek Yogurt", expiringSoon.get(0).getName());
    }

    @Test
    public void markConsumedUpdatesStatusAndWritesInventoryAction() {
        Product yogurt = product("yogurt-id", "Greek Yogurt", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 2);
        ProductRepository.addProduct(database, yogurt);

        assertTrue(ProductRepository.markConsumed(database, yogurt.getId(), "Used in breakfast"));

        Product updated = ProductRepository.getProductById(database, yogurt.getId());
        assertNotNull(updated);
        assertEquals(ProductStatus.CONSUMED, updated.getStatus());
        assertTrue(ProductRepository.getProducts(database).isEmpty());
        assertEquals(1, database.inventoryActionDao().getForProduct(yogurt.getId()).size());
        assertEquals(ProductStatus.CONSUMED, database.inventoryActionDao().getForProduct(yogurt.getId()).get(0).actionType);
    }

    @Test
    public void seedDemoProductsOnlyWhenDatabaseIsEmpty() {
        assertEquals(0, database.productDao().count());

        ProductRepository.seedDemoProductsIfEmpty(database);
        int seededCount = database.productDao().count();

        assertEquals(SampleData.products().size(), seededCount);
        assertEquals(3, database.storageLocationDao().count());
        assertNotNull(database.userSettingsDao().getById("default"));

        ProductRepository.seedDemoProductsIfEmpty(database);
        assertEquals(seededCount, database.productDao().count());
    }

    @Test
    public void seedDemoProductsDoesNotRunWhenProductTableHasData() {
        Product custom = product("custom-id", "Custom Item", "General", LocalDataContract.STORAGE_ROOM_TEMP_NAME, 7);
        ProductRepository.addProduct(database, custom);

        ProductRepository.seedDemoProductsIfEmpty(database);

        List<Product> products = ProductRepository.getProducts(database);
        assertEquals(1, products.size());
        assertEquals("Custom Item", products.get(0).getName());
    }

    private Product product(String id, String name, String category, String storage, int daysUntilExpiry) {
        long now = System.currentTimeMillis();
        return new Product(
                id,
                name,
                category,
                "1",
                "pcs",
                storage,
                null,
                expiryMillisForOffset(daysUntilExpiry),
                null,
                ProductStatus.ACTIVE,
                0,
                null,
                now,
                now,
                null,
                null,
                Product.SYNC_STATUS_LOCAL,
                null
        );
    }

    private Product copyWithName(Product product, String name) {
        return new Product(
                product.getId(),
                name,
                product.getCategory(),
                product.getQuantity(),
                product.getUnit(),
                product.getStorage(),
                product.getStorageLocationId(),
                product.getExpiryDateMillis(),
                product.getBarcode(),
                product.getStatus(),
                product.getIconRes(),
                product.getImageUrl(),
                product.getCreatedAt(),
                System.currentTimeMillis(),
                product.getCloudId(),
                product.getOwnerUserId(),
                product.getSyncStatus(),
                product.getLastSyncedAt()
        );
    }

    private long expiryMillisForOffset(int days) {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long startOfToday() {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
