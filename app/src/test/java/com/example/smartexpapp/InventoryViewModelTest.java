package com.example.smartexpapp;

import android.content.Context;
import android.os.Looper;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.example.smartexpapp.data.ProductRepository;
import com.example.smartexpapp.data.AuthStateRepository;
import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.model.ProductStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class InventoryViewModelTest {
    private AppDatabase database;
    private ProductRepository repository;
    private InventoryViewModel viewModel;

    private final ExecutorService directExecutor = new AbstractExecutorService() {
        private boolean shutdown = false;

        @Override
        public void shutdown() { shutdown = true; }

        @Override
        public List<Runnable> shutdownNow() { return java.util.Collections.emptyList(); }

        @Override
        public boolean isShutdown() { return shutdown; }

        @Override
        public boolean isTerminated() { return shutdown; }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        AuthStateRepository.setTestAuthStateOverride(AuthStateRepository.AuthState.guest(true));
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repository = new ProductRepository(context, database, directExecutor);
        viewModel = new InventoryViewModel(repository);
    }

    @After
    public void tearDown() {
        AuthStateRepository.clearTestAuthStateOverride();
        database.close();
    }

    @Test
    public void searchFilteringAndSorting() {
        Product milk = product("milk-id", "Milk", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 3); // expiring soon
        Product apple = product("apple-id", "Apple", "Fruits", LocalDataContract.STORAGE_ROOM_TEMP_NAME, 10); // safe
        Product meat = product("meat-id", "Meat", "Meat", LocalDataContract.STORAGE_FREEZE_NAME, -2); // expired

        repository.addProduct(milk);
        repository.addProduct(apple);
        repository.addProduct(meat);

        // Test loadProducts
        viewModel.loadProducts();

        // Wait for async task
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        List<Product> products = viewModel.getProducts().getValue();
        assertNotNull(products);
        assertEquals(3, products.size());

        // Test Filter StillGood
        viewModel.setExpiryFilter("StillGood");
        assertEquals(2, viewModel.getProducts().getValue().size());

        // Test Filter Expired
        viewModel.setExpiryFilter("Expired");
        assertEquals(1, viewModel.getProducts().getValue().size());
        assertEquals("Meat", viewModel.getProducts().getValue().get(0).getName());

        // Test Filter ExpiringSoon
        viewModel.setExpiryFilter("ExpiringSoon");
        assertEquals(1, viewModel.getProducts().getValue().size());
        assertEquals("Milk", viewModel.getProducts().getValue().get(0).getName());

        // Reset filters
        viewModel.setExpiryFilter("All");

        // Test Storage Filter
        viewModel.setStorageFilter("Freeze");
        assertEquals(1, viewModel.getProducts().getValue().size());
        assertEquals("Meat", viewModel.getProducts().getValue().get(0).getName());

        viewModel.setStorageFilter("All");

        // Test Sort Name
        viewModel.setSortOrder("name");
        assertEquals("Apple", viewModel.getProducts().getValue().get(0).getName());
        assertEquals("Meat", viewModel.getProducts().getValue().get(1).getName());
        assertEquals("Milk", viewModel.getProducts().getValue().get(2).getName());

        // Test Sort Newest
        viewModel.setSortOrder("newest");
        // Meat was added last, should be first
        assertEquals("Meat", viewModel.getProducts().getValue().get(0).getName());
    }

    @Test
    public void searchDebounce() {
        Product milk = product("milk-id", "Milk", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 3);
        Product apple = product("apple-id", "Apple", "Fruits", LocalDataContract.STORAGE_ROOM_TEMP_NAME, 10);
        repository.addProduct(milk);
        repository.addProduct(apple);

        viewModel.loadProducts();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        viewModel.setSearchQuery("App");

        // Immediately check, should not be updated yet (debounced)
        assertEquals(2, viewModel.getProducts().getValue().size());

        // Idle looper for debounce time (300ms)
        Shadows.shadowOf(Looper.getMainLooper()).runToEndOfTasks();

        // Now it should be updated
        assertEquals(1, viewModel.getProducts().getValue().size());
        assertEquals("Apple", viewModel.getProducts().getValue().get(0).getName());
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

    private long expiryMillisForOffset(int days) {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
