package com.example.smartexpapp.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class AppDatabaseMigrationTest {
    private static final String DB_NAME = "migration-test.db";
    private Context context;
    private File databaseFile;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        databaseFile = context.getDatabasePath(DB_NAME);
        if (databaseFile.exists()) {
            databaseFile.delete();
        }
    }

    @After
    public void tearDown() {
        if (databaseFile.exists()) {
            databaseFile.delete();
        }
    }

    @Test
    public void migrationFrom2To8AddsDisplayNameDarkModeLanguageCategoriesNotifyTimeAvatarAndPreservesSettings() {
        createVersion2Database();

        AppDatabase database = Room.databaseBuilder(context, AppDatabase.class, DB_NAME)
                .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8)
                .allowMainThreadQueries()
                .build();

        UserSettingsEntity settings = database.userSettingsDao().getById("default");

        assertNotNull(settings);
        assertEquals(5, settings.reminderDaysBefore);
        assertEquals(540, settings.reminderNotifyTimeMinutes);
        assertEquals(false, settings.notificationEnabled);
        assertEquals("Local User", settings.displayName);
        assertEquals(false, settings.darkMode);
        assertEquals("en", settings.languageTag);
        assertNull(settings.profileAvatarPath);
        assertEquals(0, database.categoryDao().count());
        database.close();
    }

    private void createVersion2Database() {
        databaseFile.getParentFile().mkdirs();
        SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(databaseFile, null);
        database.execSQL("PRAGMA foreign_keys=OFF");
        database.execSQL("CREATE TABLE IF NOT EXISTS `storage_locations` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `sort_order` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        database.execSQL("CREATE TABLE IF NOT EXISTS `products` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `quantity` TEXT NOT NULL, `unit` TEXT NOT NULL, `storage_location_id` TEXT NOT NULL, `expiry_date_millis` INTEGER NOT NULL, `barcode` TEXT, `image_uri` TEXT, `status` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `cloud_id` TEXT, `owner_user_id` TEXT, `sync_status` TEXT NOT NULL, `last_synced_at` INTEGER, PRIMARY KEY(`id`), FOREIGN KEY(`storage_location_id`) REFERENCES `storage_locations`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )");
        database.execSQL("CREATE TABLE IF NOT EXISTS `inventory_actions` (`id` TEXT NOT NULL, `product_id` TEXT NOT NULL, `action_type` TEXT NOT NULL, `quantity_changed` INTEGER NOT NULL, `action_at` INTEGER NOT NULL, `note` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        database.execSQL("CREATE TABLE IF NOT EXISTS `expiry_scans` (`id` TEXT NOT NULL, `product_id` TEXT, `raw_text` TEXT NOT NULL, `detected_date_millis` INTEGER, `confidence` REAL NOT NULL, `scanned_at` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`product_id`) REFERENCES `products`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
        database.execSQL("CREATE TABLE IF NOT EXISTS `recipe_cache` (`id` TEXT NOT NULL, `provider` TEXT NOT NULL, `title` TEXT NOT NULL, `image_url` TEXT, `source_url` TEXT, `used_ingredients` TEXT, `missing_ingredients` TEXT, `cached_at` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        database.execSQL("CREATE TABLE IF NOT EXISTS `agent_messages` (`id` TEXT NOT NULL, `role` TEXT NOT NULL, `message` TEXT NOT NULL, `related_product_ids` TEXT, `source_prompt` TEXT, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        database.execSQL("CREATE TABLE IF NOT EXISTS `user_settings` (`id` TEXT NOT NULL, `reminder_days_before` INTEGER NOT NULL, `dietary_preferences` TEXT, `default_storage_location_id` TEXT NOT NULL, `notification_enabled` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`default_storage_location_id`) REFERENCES `storage_locations`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_products_storage_location_id` ON `products` (`storage_location_id`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_products_expiry_date_millis` ON `products` (`expiry_date_millis`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_products_barcode` ON `products` (`barcode`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_inventory_actions_product_id` ON `inventory_actions` (`product_id`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_expiry_scans_product_id` ON `expiry_scans` (`product_id`)");
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_user_settings_default_storage_location_id` ON `user_settings` (`default_storage_location_id`)");
        long now = System.currentTimeMillis();
        database.execSQL("INSERT INTO storage_locations(id, name, type, sort_order, created_at, updated_at) VALUES('room_temp', 'Room Temp', 'ROOM', 0, " + now + ", " + now + ")");
        database.execSQL("INSERT INTO user_settings(id, reminder_days_before, dietary_preferences, default_storage_location_id, notification_enabled, created_at, updated_at) VALUES('default', 5, NULL, 'room_temp', 0, " + now + ", " + now + ")");
        database.setVersion(2);
        database.close();
    }
}
