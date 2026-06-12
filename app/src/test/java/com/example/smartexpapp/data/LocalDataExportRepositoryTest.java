package com.example.smartexpapp.data;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.model.Product;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class LocalDataExportRepositoryTest {
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
    public void buildExportJsonIncludesLocalMvpData() throws Exception {
        ProductRepository.addProduct(database, product("milk-id", "Milk"));
        ProductRepository.markConsumed(database, "milk-id", "Used before expiry");
        SettingsRepository.setDisplayName(database, "Kitchen Team");
        SettingsRepository.setDietaryPreferences(database, "vegetarian");

        JSONObject export = LocalDataExportRepository.buildExportJson(database);

        assertEquals("smartexp-local-export-v1", export.getString("schema"));
        assertEquals("Milk", export.getJSONArray("products").getJSONObject(0).getString("name"));
        assertEquals("CONSUMED", export.getJSONArray("products").getJSONObject(0).getString("status"));
        assertEquals("Kitchen Team", export.getJSONObject("settings").getString("displayName"));
        assertEquals("vegetarian", export.getJSONObject("settings").getString("dietaryPreferences"));
        assertEquals(1, export.getJSONArray("inventoryActions").length());
        assertTrue(export.has("expiryScans"));
        assertTrue(export.has("recipeCache"));
        assertTrue(export.has("agentMessages"));
    }

    private Product product(String id, String name) {
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
                null,
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                null,
                null,
                Product.SYNC_STATUS_LOCAL,
                null
        );
    }
}
