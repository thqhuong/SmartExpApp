package com.example.smartexpapp.data;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.data.firestore.ProductSyncRepository;
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
    private ProductRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        AuthStateRepository.setTestAuthStateOverride(AuthStateRepository.AuthState.guest(true));
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repository = new ProductRepository(context, database);
    }

    @After
    public void tearDown() {
        AuthStateRepository.clearTestAuthStateOverride();
        ProductSyncRepository.setTestSyncAvailableOverride(null);
        database.close();
    }

    @Test
    public void addReadUpdateAndDeleteProduct() {
        Product milk = product("milk-id", "Milk", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 5);
        repository.addProduct(milk);

        List<Product> products = repository.getProducts();
        assertEquals(1, products.size());
        assertEquals("Milk", products.get(0).getName());
        assertEquals(LocalDataContract.STORAGE_REFRIGERATOR_ID, products.get(0).getStorageLocationId());

        Product updated = copyWithName(milk, "Organic Milk");
        assertTrue(repository.updateProduct(updated));
        assertEquals("Organic Milk", repository.getProductById(milk.getId()).getName());

        assertTrue(repository.deleteProduct(milk.getId()));
        assertTrue(repository.getProducts().isEmpty());
        assertNull(repository.getProductById(milk.getId()));
    }

    @Test
    public void addProductAssociatesSignedInOwnerUserId() {
        AuthStateRepository.setTestAuthStateOverride(
                AuthStateRepository.AuthState.signedIn("firebase-user-123", "Kitchen Team", "team@example.com")
        );
        Product milk = product("milk-id", "Milk", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 5);

        repository.addProduct(milk);

        Product saved = repository.getProductById(milk.getId());
        assertNotNull(saved);
        assertEquals("firebase-user-123", saved.getOwnerUserId());
    }

    @Test
    public void signedInProductStaysLocalWhenFirestoreUsesDummyConfig() {
        AuthStateRepository.setTestAuthStateOverride(
                AuthStateRepository.AuthState.signedIn("firebase-user-123", "Kitchen Team", "team@example.com")
        );
        Product milk = product("milk-id", "Milk", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 5);

        repository.addProduct(milk);

        Product saved = repository.getProductById(milk.getId());
        assertNotNull(saved);
        assertEquals(Product.SYNC_STATUS_LOCAL, saved.getSyncStatus());
        assertNull(saved.getCloudId());
        assertNull(saved.getLastSyncedAt());
    }

    @Test
    public void signedInProductIsPendingUploadWhenSyncIsAvailable() {
        AuthStateRepository.setTestAuthStateOverride(
                AuthStateRepository.AuthState.signedIn("firebase-user-123", "Kitchen Team", "team@example.com")
        );
        ProductSyncRepository.setTestSyncAvailableOverride(true);
        Product milk = product("milk-id", "Milk", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 5);

        repository.addProduct(milk);

        Product saved = repository.getProductById(milk.getId());
        assertNotNull(saved);
        assertEquals("firebase-user-123", saved.getOwnerUserId());
        assertEquals(Product.SYNC_STATUS_PENDING_UPLOAD, saved.getSyncStatus());
    }

    @Test
    public void signedInDeleteMarksPendingDeleteWhenSyncIsAvailable() {
        AuthStateRepository.setTestAuthStateOverride(
                AuthStateRepository.AuthState.signedIn("firebase-user-123", "Kitchen Team", "team@example.com")
        );
        ProductSyncRepository.setTestSyncAvailableOverride(true);
        Product milk = product("milk-id", "Milk", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 5);
        repository.addProduct(milk);

        assertTrue(repository.deleteProduct(milk.getId()));

        Product deleted = repository.getProductById(milk.getId());
        assertNotNull(deleted);
        assertEquals(ProductStatus.DELETED, deleted.getStatus());
        assertEquals(Product.SYNC_STATUS_PENDING_DELETE, deleted.getSyncStatus());
        assertTrue(repository.getProducts().isEmpty());
    }

    @Test
    public void addProductKeepsGuestLocalOwnerUserIdNull() {
        AuthStateRepository.setTestAuthStateOverride(AuthStateRepository.AuthState.guest(true));
        Product milk = product("milk-id", "Milk", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 5);

        repository.addProduct(milk);

        Product saved = repository.getProductById(milk.getId());
        assertNotNull(saved);
        assertNull(saved.getOwnerUserId());
    }

    @Test
    public void signedInAddOverridesIncomingOwnerUserId() {
        AuthStateRepository.setTestAuthStateOverride(
                AuthStateRepository.AuthState.signedIn("firebase-user-123", "Kitchen Team", "team@example.com")
        );
        Product owned = product("milk-id", "Milk", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 5)
                .withOwnerUserId("original-owner", System.currentTimeMillis());
        repository.addProduct(owned);

        assertEquals("firebase-user-123", repository.getProductById(owned.getId()).getOwnerUserId());
    }

    @Test
    public void getProductsReturnsActiveProductsSortedByExpiryDate() {
        Product far = product("far-id", "Pasta", "Pantry", LocalDataContract.STORAGE_ROOM_TEMP_NAME, 30);
        Product soon = product("soon-id", "Yogurt", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 2);
        repository.addProduct(far);
        repository.addProduct(soon);

        List<Product> products = repository.getProducts();

        assertEquals(2, products.size());
        assertEquals("Yogurt", products.get(0).getName());
        assertEquals("Pasta", products.get(1).getName());
    }

    @Test
    public void searchFilterAndExpiryQueriesUseRoomData() {
        Product yogurt = product("yogurt-id", "Greek Yogurt", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 2);
        Product peas = product("peas-id", "Frozen Peas", "Vegetables", LocalDataContract.STORAGE_FREEZE_NAME, 90);
        Product expired = product("bread-id", "Bread", "Pantry", LocalDataContract.STORAGE_ROOM_TEMP_NAME, -1);
        repository.addProduct(yogurt);
        repository.addProduct(peas);
        repository.addProduct(expired);

        assertEquals(1, repository.search("yogurt").size());
        assertEquals(1, repository.filter(ProductStatus.ACTIVE, LocalDataContract.STORAGE_FREEZE_ID).size());
        assertEquals("Bread", repository.getExpiredProducts().get(0).getName());

        long start = startOfToday();
        long end = expiryMillisForOffset(3);
        List<Product> expiringSoon = repository.getExpiringBetween(start, end);
        assertEquals(1, expiringSoon.size());
        assertEquals("Greek Yogurt", expiringSoon.get(0).getName());
    }

    @Test
    public void reminderExpiryQueryReturnsOnlyActiveProductsInsideWindow() {
        Product activeSoon = product("active-soon-id", "Milk", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 1);
        Product activeLate = product("active-late-id", "Pasta", "Pantry", LocalDataContract.STORAGE_ROOM_TEMP_NAME, 9);
        Product consumedSoon = product("consumed-soon-id", "Spinach", "Vegetables", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 1);
        repository.addProduct(activeSoon);
        repository.addProduct(activeLate);
        repository.addProduct(consumedSoon);
        repository.markConsumed(consumedSoon.getId(), "Used");

        List<Product> expiring = repository.getExpiringBetween(startOfToday(), expiryMillisForOffset(3));

        assertEquals(1, expiring.size());
        assertEquals("Milk", expiring.get(0).getName());
    }

    @Test
    public void markConsumedUpdatesStatusAndWritesInventoryAction() {
        Product yogurt = product("yogurt-id", "Greek Yogurt", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 2);
        repository.addProduct(yogurt);

        assertTrue(repository.markConsumed(yogurt.getId(), "Used in breakfast"));

        Product updated = repository.getProductById(yogurt.getId());
        assertNotNull(updated);
        assertEquals(ProductStatus.CONSUMED, updated.getStatus());
        assertTrue(repository.getProducts().isEmpty());
        assertEquals(1, database.inventoryActionDao().getForProduct(yogurt.getId()).size());
        assertEquals(ProductStatus.CONSUMED, database.inventoryActionDao().getForProduct(yogurt.getId()).get(0).actionType);
    }

    @Test
    public void inventoryActionsCanBeQueriedByProductOwner() {
        AuthStateRepository.setTestAuthStateOverride(
                AuthStateRepository.AuthState.signedIn("firebase-user-123", "Kitchen Team", "team@example.com")
        );
        Product owned = product("owned-id", "Yogurt", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 2);
        repository.addProduct(owned);
        repository.markConsumed(owned.getId(), "Used");

        AuthStateRepository.setTestAuthStateOverride(AuthStateRepository.AuthState.guest(true));
        Product localOnly = product("local-id", "Bread", "Pantry", LocalDataContract.STORAGE_ROOM_TEMP_NAME, 2);
        repository.addProduct(localOnly);
        repository.markWasted(localOnly.getId(), "Spoiled");

        assertEquals(1, database.inventoryActionDao().getAllForOwner("firebase-user-123").size());
    }

    @Test
    public void wastePreventedCountIncludesConsumedAndDonatedOnly() {
        Product consumed = product("consumed-id", "Yogurt", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 2);
        Product donated = product("donated-id", "Beans", "Pantry", LocalDataContract.STORAGE_ROOM_TEMP_NAME, 10);
        Product wasted = product("wasted-id", "Bread", "Pantry", LocalDataContract.STORAGE_ROOM_TEMP_NAME, 1);
        repository.addProduct(consumed);
        repository.addProduct(donated);
        repository.addProduct(wasted);

        repository.markConsumed(consumed.getId(), "Used");
        repository.markDonated(donated.getId(), "Shared");
        repository.markWasted(wasted.getId(), "Spoiled");

        assertEquals(2, repository.getWastePreventedCount());
    }

    @Test
    public void statsSnapshotDerivesMetricsFromRoomData() {
        Product urgent = product("urgent-id", "Milk", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 1);
        Product expired = product("expired-id", "Bread", "Pantry", LocalDataContract.STORAGE_ROOM_TEMP_NAME, -1);
        Product safe = product("safe-id", "Pasta", "Pantry", LocalDataContract.STORAGE_ROOM_TEMP_NAME, 20);
        Product consumed = product("consumed-id", "Spinach", "Vegetables", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 2);
        repository.addProduct(urgent);
        repository.addProduct(expired);
        repository.addProduct(safe);
        repository.addProduct(consumed);
        repository.markConsumed(consumed.getId(), "Used before expiry");

        ProductRepository.StatsSnapshot snapshot = repository.getStatsSnapshot(0L);

        assertEquals(3, snapshot.getActiveCount());
        assertEquals(1, snapshot.getUrgentCount());
        assertEquals(1, snapshot.getExpiredCount());
        assertEquals(1, snapshot.getPreventedWasteCount());
        assertEquals(3, snapshot.getActiveProducts().size());
    }

    @Test
    public void signedInUsersOnlySeeTheirOwnProducts() {
        ProductRepository.ensureLocalDefaults(database);
        AuthStateRepository.setTestAuthStateOverride(AuthStateRepository.AuthState.guest(true));
        database.productDao().insert(com.example.smartexpapp.data.local.ProductMapper.toEntity(
                product("google-id", "Crab", "Seafood", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 4)
                        .withOwnerUserId("google-uid", System.currentTimeMillis())
        ));
        database.productDao().insert(com.example.smartexpapp.data.local.ProductMapper.toEntity(
                product("email-id", "Rice", "Pantry", LocalDataContract.STORAGE_ROOM_TEMP_NAME, 30)
                        .withOwnerUserId("email-uid", System.currentTimeMillis())
        ));

        AuthStateRepository.setTestAuthStateOverride(
                AuthStateRepository.AuthState.signedIn("email-uid", "Chef", "chef@smartexp.com")
        );
        List<Product> emailProducts = repository.getProducts();
        assertEquals(1, emailProducts.size());
        assertEquals("Rice", emailProducts.get(0).getName());
        assertNull(repository.getProductById("google-id"));

        AuthStateRepository.setTestAuthStateOverride(
                AuthStateRepository.AuthState.signedIn("google-uid", "Google User", "google@example.com")
        );
        List<Product> googleProducts = repository.getProducts();
        assertEquals(1, googleProducts.size());
        assertEquals("Crab", googleProducts.get(0).getName());
        assertNull(repository.getProductById("email-id"));
    }

    @Test
    public void guestSeesOnlyOwnerlessProducts() {
        ProductRepository.ensureLocalDefaults(database);
        database.productDao().insert(com.example.smartexpapp.data.local.ProductMapper.toEntity(
                product("local-id", "Bread", "Pantry", LocalDataContract.STORAGE_ROOM_TEMP_NAME, 2)
        ));
        database.productDao().insert(com.example.smartexpapp.data.local.ProductMapper.toEntity(
                product("owned-id", "Crab", "Seafood", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 4)
                        .withOwnerUserId("google-uid", System.currentTimeMillis())
        ));

        List<Product> products = repository.getProducts();

        assertEquals(1, products.size());
        assertEquals("Bread", products.get(0).getName());
        assertNull(repository.getProductById("owned-id"));
    }

    @Test
    public void signedInEditDeleteAndStatusDoNotMutateOtherOwnerProduct() {
        ProductRepository.ensureLocalDefaults(database);
        Product otherOwner = product("other-id", "Crab", "Seafood", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 4)
                .withOwnerUserId("google-uid", System.currentTimeMillis());
        database.productDao().insert(com.example.smartexpapp.data.local.ProductMapper.toEntity(otherOwner));
        AuthStateRepository.setTestAuthStateOverride(
                AuthStateRepository.AuthState.signedIn("email-uid", "Chef", "chef@smartexp.com")
        );

        assertFalse(repository.updateProduct(copyWithName(otherOwner, "King Crab")));
        assertFalse(repository.markConsumed(otherOwner.getId(), "Used"));
        assertFalse(repository.deleteProduct(otherOwner.getId()));

        AuthStateRepository.setTestAuthStateOverride(
                AuthStateRepository.AuthState.signedIn("google-uid", "Google User", "google@example.com")
        );
        Product stillVisibleToOwner = repository.getProductById(otherOwner.getId());
        assertNotNull(stillVisibleToOwner);
        assertEquals("Crab", stillVisibleToOwner.getName());
        assertEquals(ProductStatus.ACTIVE, stillVisibleToOwner.getStatus());
    }

    @Test
    public void importOwnerlessProductsAssignsCurrentUidAndMarksPendingUpload() {
        ProductRepository.ensureLocalDefaults(database);
        Product local = product("local-id", "Bread", "Pantry", LocalDataContract.STORAGE_ROOM_TEMP_NAME, 2);
        database.productDao().insert(com.example.smartexpapp.data.local.ProductMapper.toEntity(local));
        long updatedAt = System.currentTimeMillis();

        int rows = database.productDao().assignOwnerToOwnerlessProducts(
                "email-uid",
                Product.SYNC_STATUS_PENDING_UPLOAD,
                updatedAt
        );

        assertEquals(1, rows);
        AuthStateRepository.setTestAuthStateOverride(
                AuthStateRepository.AuthState.signedIn("email-uid", "Chef", "chef@smartexp.com")
        );
        Product imported = repository.getProductById(local.getId());
        assertNotNull(imported);
        assertEquals("email-uid", imported.getOwnerUserId());
        assertEquals(Product.SYNC_STATUS_PENDING_UPLOAD, imported.getSyncStatus());
    }

    @Test
    public void dashboardSearchFilterAndExpiryQueriesAreScoped() {
        ProductRepository.ensureLocalDefaults(database);
        database.productDao().insert(com.example.smartexpapp.data.local.ProductMapper.toEntity(
                product("email-yogurt", "Greek Yogurt", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 2)
                        .withOwnerUserId("email-uid", System.currentTimeMillis())
        ));
        database.productDao().insert(com.example.smartexpapp.data.local.ProductMapper.toEntity(
                product("email-bread", "Bread", "Pantry", LocalDataContract.STORAGE_ROOM_TEMP_NAME, -1)
                        .withOwnerUserId("email-uid", System.currentTimeMillis())
        ));
        database.productDao().insert(com.example.smartexpapp.data.local.ProductMapper.toEntity(
                product("google-yogurt", "Greek Yogurt", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 2)
                        .withOwnerUserId("google-uid", System.currentTimeMillis())
        ));
        AuthStateRepository.setTestAuthStateOverride(
                AuthStateRepository.AuthState.signedIn("email-uid", "Chef", "chef@smartexp.com")
        );

        assertEquals(1, repository.search("yogurt").size());
        assertEquals(1, repository.filter(ProductStatus.ACTIVE, LocalDataContract.STORAGE_REFRIGERATOR_ID).size());
        assertEquals(1, repository.getExpiredProducts().size());
        assertEquals(1, repository.getExpiringBetween(startOfToday(), expiryMillisForOffset(3)).size());
        assertEquals(2, repository.getDashboardSnapshot().getTotalTracked());
    }

    @Test
    public void ensureLocalDefaultsDoesNotSeedProducts() {
        assertEquals(0, database.productDao().count());

        ProductRepository.ensureLocalDefaults(database);

        assertEquals(0, database.productDao().count());
        assertEquals(3, database.storageLocationDao().count());
        assertNotNull(database.userSettingsDao().getById("default"));
    }

    @Test
    public void ensureLocalDefaultsPreservesExistingProducts() {
        Product custom = product("custom-id", "Custom Item", "General", LocalDataContract.STORAGE_ROOM_TEMP_NAME, 7);
        repository.addProduct(custom);

        ProductRepository.ensureLocalDefaults(database);

        List<Product> products = repository.getProducts();
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
