package com.example.smartexpapp.data;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.example.smartexpapp.data.firestore.ProductSyncRepository;
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

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class CategoryRepositoryTest {
    private Context context;
    private AppDatabase database;
    private ProductRepository productRepository;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("category_prefs", Context.MODE_PRIVATE).edit().clear().commit();
        AuthStateRepository.setTestAuthStateOverride(AuthStateRepository.AuthState.guest(true));
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        productRepository = new ProductRepository(context, database);
    }

    @After
    public void tearDown() {
        AuthStateRepository.clearTestAuthStateOverride();
        ProductSyncRepository.setTestSyncAvailableOverride(null);
        database.close();
    }

    @Test
    public void categoriesSeedBuiltIns() {
        List<String> categories = CategoryRepository.getDisplayCategories(context, database);

        assertEquals(6, categories.size());
        assertTrue(CategoryRepository.isCategoryActive(context, database, "Dairy"));
        assertTrue(CategoryRepository.isCategoryActive(context, database, "General"));
    }

    @Test
    public void addAndDeleteUnusedCustomCategory() {
        assertTrue(CategoryRepository.addCategory(context, database, "Snacks"));
        assertNotNull(database.categoryDao().getByName("Snacks"));
        assertFalse(CategoryRepository.addCategory(context, database, "Snacks"));

        assertTrue(CategoryRepository.deleteCategory(context, database, "Snacks"));
        assertFalse(CategoryRepository.isCategoryActive(context, database, "Snacks"));
    }

    @Test
    public void renameCategoryUpdatesProducts() {
        productRepository.addProduct(product("milk-id", "Milk", "Dairy"));

        assertTrue(CategoryRepository.renameCategory(context, database, "Dairy", "Milk Stuff"));

        Product updated = productRepository.getProductById("milk-id");
        assertNotNull(updated);
        assertEquals("Milk Stuff", updated.getCategory());
        assertFalse(CategoryRepository.isCategoryActive(context, database, "Dairy"));
        assertTrue(CategoryRepository.isCategoryActive(context, database, "Milk Stuff"));
    }

    @Test
    public void signedInCategoryWriteIsPendingUploadWhenSyncAvailable() {
        AuthStateRepository.setTestAuthStateOverride(
                AuthStateRepository.AuthState.signedIn("firebase-user-123", "Kitchen Team", "team@example.com")
        );
        ProductSyncRepository.setTestSyncAvailableOverride(true);

        assertTrue(CategoryRepository.addCategory(context, database, "Snacks"));

        assertEquals(
                Product.SYNC_STATUS_PENDING_UPLOAD,
                database.categoryDao().getByName("Snacks").syncStatus
        );
        assertEquals("firebase-user-123", database.categoryDao().getByName("Snacks").ownerUserId);
    }

    private Product product(String id, String name, String category) {
        long now = System.currentTimeMillis();
        return new Product(
                id,
                name,
                category,
                "1",
                "pcs",
                LocalDataContract.STORAGE_REFRIGERATOR_NAME,
                null,
                now + 86400000L,
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
}
