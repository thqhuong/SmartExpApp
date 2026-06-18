package com.example.smartexpapp.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {
                ProductEntity.class,
                StorageLocationEntity.class,
                InventoryActionEntity.class,
                ExpiryScanEntity.class,
                RecipeCacheEntity.class,
                AgentMessageEntity.class,
                UserSettingsEntity.class,
                CategoryEntity.class
        },
        version = 6,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "smartexp_local.db";
    private static volatile AppDatabase instance;
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Schema is unchanged; this keeps existing installs on a monotonic Room version.
        }
    };
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE user_settings ADD COLUMN display_name TEXT DEFAULT 'Local User'");
        }
    };
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE user_settings ADD COLUMN dark_mode INTEGER NOT NULL DEFAULT 0");
        }
    };
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE user_settings ADD COLUMN language_tag TEXT NOT NULL DEFAULT 'en'");
        }
    };
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `sort_order` INTEGER NOT NULL, `is_built_in` INTEGER NOT NULL, `active` INTEGER NOT NULL, `owner_user_id` TEXT, `cloud_id` TEXT, `sync_status` TEXT NOT NULL, `last_synced_at` INTEGER, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `deleted_at` INTEGER, PRIMARY KEY(`id`))");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_categories_owner_user_id_name` ON `categories` (`owner_user_id`, `name`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_sort_order` ON `categories` (`sort_order`)");
        }
    };

    public abstract ProductDao productDao();

    public abstract StorageLocationDao storageLocationDao();

    public abstract InventoryActionDao inventoryActionDao();

    public abstract ExpiryScanDao expiryScanDao();

    public abstract RecipeCacheDao recipeCacheDao();

    public abstract AgentMessageDao agentMessageDao();

    public abstract UserSettingsDao userSettingsDao();

    public abstract CategoryDao categoryDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                            .fallbackToDestructiveMigrationOnDowngrade(true)
                            .build();
                }
            }
        }
        return instance;
    }

}
