package com.example.smartexpapp.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CategoryDao {
    @Query("SELECT COUNT(*) FROM categories")
    int count();

    @Query("SELECT * FROM categories WHERE active = 1 AND deleted_at IS NULL ORDER BY sort_order ASC, name COLLATE NOCASE ASC")
    List<CategoryEntity> getActiveCategories();

    @Query("SELECT * FROM categories WHERE owner_user_id = :ownerUserId AND sync_status = :syncStatus ORDER BY updated_at ASC")
    List<CategoryEntity> getByOwnerAndSyncStatus(String ownerUserId, String syncStatus);

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    CategoryEntity getById(String id);

    @Query("SELECT * FROM categories WHERE name = :name AND owner_user_id IS NULL LIMIT 1")
    CategoryEntity getLocalByName(String name);

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    CategoryEntity getByName(String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CategoryEntity category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CategoryEntity> categories);

    @Update
    int update(CategoryEntity category);

    @Query("DELETE FROM categories")
    int deleteAll();

    @Query("UPDATE categories SET cloud_id = :cloudId, sync_status = :syncStatus, last_synced_at = :lastSyncedAt WHERE id = :id")
    int updateSyncMetadata(String id, String cloudId, String syncStatus, Long lastSyncedAt);
}
