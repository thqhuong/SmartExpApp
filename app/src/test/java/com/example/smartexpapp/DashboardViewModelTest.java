package com.example.smartexpapp;

import android.content.Context;
import android.os.Looper;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.example.smartexpapp.data.ProductRepository;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class DashboardViewModelTest {
    private AppDatabase database;
    private ProductRepository repository;
    private DashboardViewModel viewModel;

    private final ExecutorService directExecutor = new AbstractExecutorService() {
        private boolean shutdown = false;

        @Override
        public void shutdown() { shutdown = true; }

        @Override
        public List<Runnable> shutdownNow() { return Collections.emptyList(); }

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
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repository = new ProductRepository(context, database, directExecutor);
        viewModel = new DashboardViewModel(repository);
    }

    @After
    public void tearDown() {
        database.close();
    }

    // ==================== groupProducts tests ====================

    @Test
    public void groupProducts_emptyList_returnsEmptyMap() {
        Map<String, List<Product>> groups = DashboardViewModel.groupProducts(Collections.emptyList());
        assertNotNull(groups);
        assertTrue(groups.isEmpty());
    }

    @Test
    public void groupProducts_null_returnsEmptyMap() {
        Map<String, List<Product>> groups = DashboardViewModel.groupProducts(null);
        assertNotNull(groups);
        assertTrue(groups.isEmpty());
    }

    @Test
    public void groupProducts_expiredProduct_goesToExpiredGroup() {
        Product expired = product("exp-1", "Old Milk", "Dairy",
                LocalDataContract.STORAGE_REFRIGERATOR_NAME, -1);
        Map<String, List<Product>> groups = DashboardViewModel.groupProducts(Arrays.asList(expired));
        assertEquals(1, groups.get("Expired").size());
        assertTrue(groups.get("Urgent").isEmpty());
        assertTrue(groups.get("Soon").isEmpty());
        assertTrue(groups.get("Safe").isEmpty());
    }

    @Test
    public void groupProducts_urgentProduct_goesToUrgentGroup() {
        Product urgent = product("urg-1", "Yogurt", "Dairy",
                LocalDataContract.STORAGE_REFRIGERATOR_NAME, 0);
        Map<String, List<Product>> groups = DashboardViewModel.groupProducts(Arrays.asList(urgent));

        assertEquals("Expected 1 product in Urgent group for day 0", 1, groups.get("Urgent").size());
        assertEquals("Yogurt", groups.get("Urgent").get(0).getName());

        // Also test day 1
        Product urgent1 = product("urg-2", "Cream", "Dairy",
                LocalDataContract.STORAGE_REFRIGERATOR_NAME, 1);
        groups = DashboardViewModel.groupProducts(Arrays.asList(urgent1));
        assertEquals("Expected 1 product in Urgent group for day 1", 1, groups.get("Urgent").size());
    }

    @Test
    public void groupProducts_soonProduct_goesToSoonGroup() {
        Product soon = product("soon-1", "Bread", "Bakery",
                LocalDataContract.STORAGE_ROOM_TEMP_NAME, 3);
        Map<String, List<Product>> groups = DashboardViewModel.groupProducts(Arrays.asList(soon));
        assertEquals(1, groups.get("Soon").size());
        assertEquals("Bread", groups.get("Soon").get(0).getName());
    }

    @Test
    public void groupProducts_safeProduct_goesToSafeGroup() {
        Product safe = product("safe-1", "Rice", "Grains",
                LocalDataContract.STORAGE_ROOM_TEMP_NAME, 30);
        Map<String, List<Product>> groups = DashboardViewModel.groupProducts(Arrays.asList(safe));
        assertEquals(1, groups.get("Safe").size());
        assertEquals("Rice", groups.get("Safe").get(0).getName());
    }

    @Test
    public void groupProducts_multipleProducts_correctlyDistributed() {
        List<Product> products = Arrays.asList(
                product("e1", "Milk", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, -2),
                product("u1", "Yogurt", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 1),
                product("s1", "Cheese", "Dairy", LocalDataContract.STORAGE_REFRIGERATOR_NAME, 5),
                product("f1", "Butter", "Dairy", LocalDataContract.STORAGE_FREEZE_NAME, 90)
        );
        Map<String, List<Product>> groups = DashboardViewModel.groupProducts(products);
        assertEquals(1, groups.get("Expired").size());
        assertEquals(1, groups.get("Urgent").size());
        assertEquals(1, groups.get("Soon").size());
        assertEquals(1, groups.get("Safe").size());
    }

    // ==================== buildStorageSummaries tests ====================

    @Test
    public void buildStorageSummaries_emptyList_returnsEmptyList() {
        List<DashboardState.StorageSummaryEntry> summaries =
                DashboardViewModel.buildStorageSummaries(Collections.emptyList());
        assertNotNull(summaries);
        assertTrue(summaries.isEmpty());
    }

    @Test
    public void buildStorageSummaries_null_returnsEmptyList() {
        List<DashboardState.StorageSummaryEntry> summaries =
                DashboardViewModel.buildStorageSummaries(null);
        assertNotNull(summaries);
        assertTrue(summaries.isEmpty());
    }

    @Test
    public void buildStorageSummaries_countsCorrectly() {
        List<Product> products = Arrays.asList(
                product("1", "Milk", "Dairy", "Refrigerator", 10),
                product("2", "Cheese", "Dairy", "Refrigerator", 20),
                product("3", "Apple", "Fruits", "Room Temp", 15),
                product("4", "Banana", "Fruits", "Room Temp", 5),
                product("5", "Meat", "Meat", "Freeze", 30)
        );
        List<DashboardState.StorageSummaryEntry> summaries =
                DashboardViewModel.buildStorageSummaries(products);

        assertEquals(3, summaries.size());
        assertEquals("Refrigerator", summaries.get(0).getStorageValue());
        assertEquals(2, summaries.get(0).getCount());
        assertEquals("Room Temp", summaries.get(1).getStorageValue());
        assertEquals(2, summaries.get(1).getCount());
        assertEquals("Freeze", summaries.get(2).getStorageValue());
        assertEquals(1, summaries.get(2).getCount());
    }

    @Test
    public void buildStorageSummaries_progress_isPercentOfTotal() {
        List<Product> products = Arrays.asList(
                product("1", "Milk", "Dairy", "Refrigerator", 10),
                product("2", "Cheese", "Dairy", "Refrigerator", 20),
                product("3", "Apple", "Fruits", "Room Temp", 15),
                product("4", "Meat", "Meat", "Freeze", 30)
        );
        List<DashboardState.StorageSummaryEntry> summaries =
                DashboardViewModel.buildStorageSummaries(products);

        assertEquals(4, summaries.get(0).getCount() + summaries.get(1).getCount() + summaries.get(2).getCount());
        assertEquals(50, summaries.get(0).getProgressPercent());  // 2/4
        assertEquals(25, summaries.get(1).getProgressPercent());  // 1/4
        assertEquals(25, summaries.get(2).getProgressPercent());  // 1/4
    }

    @Test
    public void buildStorageSummaries_unknownStorage_notCounted() {
        List<Product> products = Collections.singletonList(
                product("1", "Item", "General", "Pantry", 5)
        );
        List<DashboardState.StorageSummaryEntry> summaries =
                DashboardViewModel.buildStorageSummaries(products);

        int totalCount = 0;
        for (DashboardState.StorageSummaryEntry s : summaries) {
            totalCount += s.getCount();
        }
        assertEquals("Unknown storage should not be counted", 0, totalCount);
    }

    @Test
    public void buildStorageSummaries_maintainsOrder() {
        List<Product> products = Arrays.asList(
                product("1", "FreezeItem", "Meat", "Freeze", 10),
                product("2", "FridgeItem", "Dairy", "Refrigerator", 20)
        );
        List<DashboardState.StorageSummaryEntry> summaries =
                DashboardViewModel.buildStorageSummaries(products);

        assertEquals("Refrigerator", summaries.get(0).getStorageValue());
        assertEquals("Room Temp", summaries.get(1).getStorageValue());
        assertEquals("Freeze", summaries.get(2).getStorageValue());
    }

    // ==================== ViewModel integration tests ====================

    @Test
    public void loadDashboard_initialState_isLoading() {
        DashboardState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertTrue("Initial state should be loading", state.isLoading());
    }

    @Test
    public void loadDashboard_withProducts_emitsStateWithGroups() {
        repository.addProduct(product("milk", "Milk", "Dairy",
                LocalDataContract.STORAGE_REFRIGERATOR_NAME, -1));
        repository.addProduct(product("yogurt", "Yogurt", "Dairy",
                LocalDataContract.STORAGE_REFRIGERATOR_NAME, 1));
        repository.addProduct(product("bread", "Bread", "Bakery",
                LocalDataContract.STORAGE_ROOM_TEMP_NAME, 5));
        repository.addProduct(product("meat", "Meat", "Meat",
                LocalDataContract.STORAGE_FREEZE_NAME, 30));

        viewModel.loadDashboard();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        DashboardState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse("Should not be loading after data loaded", state.isLoading());
        assertFalse("Should not be error", state.isError());
        assertEquals(4, state.getTotalTracked());

        Map<String, List<Product>> groups = state.getProductGroups();
        assertEquals(1, groups.get("Expired").size());
        assertEquals(1, groups.get("Urgent").size());
        assertEquals(1, groups.get("Soon").size());
        assertEquals(1, groups.get("Safe").size());

        List<DashboardState.StorageSummaryEntry> summaries = state.getStorageSummaries();
        assertFalse(summaries.isEmpty());
    }

    @Test
    public void loadDashboard_empty_emitsStateWithZeroMetrics() {
        viewModel.loadDashboard();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        DashboardState state = viewModel.getUiState().getValue();
        assertNotNull(state);
        assertFalse(state.isLoading());
        assertFalse(state.isError());
        assertEquals(0, state.getTotalTracked());
        assertEquals(0, state.getUrgentCount());
        assertEquals(0, state.getWastePreventedCount());
    }

    @Test
    public void loadDashboard_multipleCalls_worksCorrectly() {
        repository.addProduct(product("milk", "Milk", "Dairy",
                LocalDataContract.STORAGE_REFRIGERATOR_NAME, -1));
        repository.addProduct(product("yogurt", "Yogurt", "Dairy",
                LocalDataContract.STORAGE_REFRIGERATOR_NAME, 1));

        viewModel.loadDashboard();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        DashboardState first = viewModel.getUiState().getValue();
        assertNotNull(first);
        assertEquals(2, first.getTotalTracked());

        repository.addProduct(product("bread", "Bread", "Bakery",
                LocalDataContract.STORAGE_ROOM_TEMP_NAME, 5));

        viewModel.loadDashboard();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        DashboardState second = viewModel.getUiState().getValue();
        assertNotNull(second);
        assertEquals(3, second.getTotalTracked());
    }

    // ==================== helpers ====================

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
