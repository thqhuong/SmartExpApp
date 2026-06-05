package com.example.smartexpapp.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                ProductEntity.class,
                StorageLocationEntity.class,
                InventoryActionEntity.class,
                ExpiryScanEntity.class,
                RecipeCacheEntity.class,
                AgentMessageEntity.class,
                UserSettingsEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DATABASE_NAME = "smartexp_local.db";
    private static volatile AppDatabase instance;

    public abstract ProductDao productDao();

    public abstract StorageLocationDao storageLocationDao();

    public abstract InventoryActionDao inventoryActionDao();

    public abstract ExpiryScanDao expiryScanDao();

    public abstract RecipeCacheDao recipeCacheDao();

    public abstract AgentMessageDao agentMessageDao();

    public abstract UserSettingsDao userSettingsDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return instance;
    }

}
