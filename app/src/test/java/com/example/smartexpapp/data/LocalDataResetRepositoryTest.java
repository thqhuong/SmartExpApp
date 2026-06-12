package com.example.smartexpapp.data;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.example.smartexpapp.data.local.AgentMessageEntity;
import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.ExpiryScanEntity;
import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.data.local.RecipeCacheEntity;
import com.example.smartexpapp.model.Product;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class LocalDataResetRepositoryTest {
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
    public void resetDeletesLocalUserDataImagesAndRecreatesDefaults() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File image = createImageFile(context, "milk.jpg");
        ProductRepository.addProduct(database, product("milk-id", "Milk", image.getAbsolutePath()));
        ProductRepository.markConsumed(database, "milk-id", "Used");
        database.expiryScanDao().insert(scan("scan-id", "milk-id"));
        database.recipeCacheDao().insert(recipe("recipe-id"));
        database.agentMessageDao().insert(message("message-id"));
        SettingsRepository.setDisplayName(database, "Kitchen Team");

        LocalDataResetRepository.ResetSummary summary = LocalDataResetRepository.reset(database, context);

        assertEquals(1, summary.getProductsDeleted());
        assertEquals(1, summary.getInventoryActionsDeleted());
        assertEquals(1, summary.getExpiryScansDeleted());
        assertEquals(1, summary.getRecipeCacheDeleted());
        assertEquals(1, summary.getAgentMessagesDeleted());
        assertEquals(1, summary.getSettingsDeleted());
        assertEquals(1, summary.getImageFilesDeleted());
        assertFalse(image.exists());
        assertEquals(0, database.productDao().count());
        assertEquals(0, database.inventoryActionDao().getAll().size());
        assertEquals(0, database.expiryScanDao().getAll().size());
        assertEquals(0, database.recipeCacheDao().getAll().size());
        assertEquals(0, database.agentMessageDao().getConversation().size());
        assertNotNull(database.userSettingsDao().getById("default"));
        assertEquals("Local User", SettingsRepository.getSettings(database).getDisplayName());
        assertEquals(3, database.storageLocationDao().count());
    }

    private Product product(String id, String name) {
        return product(id, name, null);
    }

    private Product product(String id, String name, String imagePath) {
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.add(Calendar.DAY_OF_YEAR, 2);
        return new Product(
                id,
                name,
                "Dairy",
                "1",
                "pcs",
                LocalDataContract.STORAGE_REFRIGERATOR_NAME,
                LocalDataContract.STORAGE_REFRIGERATOR_ID,
                calendar.getTimeInMillis(),
                null,
                "ACTIVE",
                0,
                imagePath,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                null,
                null,
                Product.SYNC_STATUS_LOCAL,
                null
        );
    }

    private File createImageFile(Context context, String name) throws Exception {
        File imageDir = LocalImageRepository.imageDirectory(context);
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }
        File image = new File(imageDir, name);
        try (FileOutputStream output = new FileOutputStream(image)) {
            output.write(new byte[] {1, 2, 3});
        }
        return image;
    }

    private ExpiryScanEntity scan(String id, String productId) {
        ExpiryScanEntity scan = new ExpiryScanEntity();
        scan.id = id;
        scan.productId = productId;
        scan.rawText = "Best before tomorrow";
        scan.confidence = 0.8f;
        scan.scannedAt = System.currentTimeMillis();
        scan.createdAt = scan.scannedAt;
        scan.updatedAt = scan.scannedAt;
        return scan;
    }

    private RecipeCacheEntity recipe(String id) {
        RecipeCacheEntity recipe = new RecipeCacheEntity();
        recipe.id = id;
        recipe.provider = "local";
        recipe.title = "Use First";
        recipe.cachedAt = System.currentTimeMillis();
        recipe.createdAt = recipe.cachedAt;
        recipe.updatedAt = recipe.cachedAt;
        return recipe;
    }

    private AgentMessageEntity message(String id) {
        AgentMessageEntity message = new AgentMessageEntity();
        message.id = id;
        message.role = "agent";
        message.message = "Use milk first";
        message.createdAt = System.currentTimeMillis();
        message.updatedAt = message.createdAt;
        return message;
    }
}
