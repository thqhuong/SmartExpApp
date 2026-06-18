package com.example.smartexpapp.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ProductDao {
    @Query("SELECT COUNT(*) FROM products")
    int count();

    @Query("SELECT * FROM products WHERE status = 'ACTIVE' ORDER BY expiry_date_millis ASC, name COLLATE NOCASE ASC")
    List<ProductEntity> getActiveProductsSortedByExpiry();

    @Query("SELECT * FROM products WHERE owner_user_id = :ownerUserId AND status = 'ACTIVE' ORDER BY expiry_date_millis ASC, name COLLATE NOCASE ASC")
    List<ProductEntity> getActiveProductsForOwner(String ownerUserId);

    @Query("SELECT * FROM products WHERE owner_user_id IS NULL AND status = 'ACTIVE' ORDER BY expiry_date_millis ASC, name COLLATE NOCASE ASC")
    List<ProductEntity> getActiveLocalProducts();

    @Query("SELECT * FROM products ORDER BY expiry_date_millis ASC, name COLLATE NOCASE ASC")
    List<ProductEntity> getAllProductsSortedByExpiry();

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    ProductEntity getById(String id);

    @Query("SELECT * FROM products WHERE id = :id AND owner_user_id = :ownerUserId LIMIT 1")
    ProductEntity getByIdForOwner(String id, String ownerUserId);

    @Query("SELECT * FROM products WHERE id = :id AND owner_user_id IS NULL LIMIT 1")
    ProductEntity getLocalById(String id);

    @Query("SELECT * FROM products WHERE owner_user_id = :ownerUserId AND sync_status = :syncStatus ORDER BY updated_at ASC")
    List<ProductEntity> getByOwnerAndSyncStatus(String ownerUserId, String syncStatus);

    @Query("SELECT * FROM products WHERE status = 'ACTIVE' AND expiry_date_millis BETWEEN :startMillis AND :endMillis ORDER BY expiry_date_millis ASC")
    List<ProductEntity> getExpiringBetween(long startMillis, long endMillis);

    @Query("SELECT * FROM products WHERE owner_user_id = :ownerUserId AND status = 'ACTIVE' AND expiry_date_millis BETWEEN :startMillis AND :endMillis ORDER BY expiry_date_millis ASC")
    List<ProductEntity> getExpiringBetweenForOwner(String ownerUserId, long startMillis, long endMillis);

    @Query("SELECT * FROM products WHERE owner_user_id IS NULL AND status = 'ACTIVE' AND expiry_date_millis BETWEEN :startMillis AND :endMillis ORDER BY expiry_date_millis ASC")
    List<ProductEntity> getLocalExpiringBetween(long startMillis, long endMillis);

    @Query("SELECT * FROM products WHERE status = 'ACTIVE' AND expiry_date_millis < :beforeMillis ORDER BY expiry_date_millis ASC")
    List<ProductEntity> getExpiredBefore(long beforeMillis);

    @Query("SELECT * FROM products WHERE owner_user_id = :ownerUserId AND status = 'ACTIVE' AND expiry_date_millis < :beforeMillis ORDER BY expiry_date_millis ASC")
    List<ProductEntity> getExpiredBeforeForOwner(String ownerUserId, long beforeMillis);

    @Query("SELECT * FROM products WHERE owner_user_id IS NULL AND status = 'ACTIVE' AND expiry_date_millis < :beforeMillis ORDER BY expiry_date_millis ASC")
    List<ProductEntity> getLocalExpiredBefore(long beforeMillis);

    @Query("SELECT * FROM products WHERE status = 'ACTIVE' AND (LOWER(name) LIKE '%' || LOWER(:query) || '%' OR LOWER(category) LIKE '%' || LOWER(:query) || '%') ORDER BY expiry_date_millis ASC, name COLLATE NOCASE ASC")
    List<ProductEntity> searchActive(String query);

    @Query("SELECT * FROM products WHERE owner_user_id = :ownerUserId AND status = 'ACTIVE' AND (LOWER(name) LIKE '%' || LOWER(:query) || '%' OR LOWER(category) LIKE '%' || LOWER(:query) || '%') ORDER BY expiry_date_millis ASC, name COLLATE NOCASE ASC")
    List<ProductEntity> searchActiveForOwner(String ownerUserId, String query);

    @Query("SELECT * FROM products WHERE owner_user_id IS NULL AND status = 'ACTIVE' AND (LOWER(name) LIKE '%' || LOWER(:query) || '%' OR LOWER(category) LIKE '%' || LOWER(:query) || '%') ORDER BY expiry_date_millis ASC, name COLLATE NOCASE ASC")
    List<ProductEntity> searchActiveLocal(String query);

    @Query("SELECT * FROM products WHERE status = 'ACTIVE' AND storage_location_id = :storageLocationId ORDER BY expiry_date_millis ASC, name COLLATE NOCASE ASC")
    List<ProductEntity> getActiveByStorageLocation(String storageLocationId);

    @Query("SELECT * FROM products WHERE (:status IS NULL OR status = :status) AND (:storageLocationId IS NULL OR storage_location_id = :storageLocationId) ORDER BY expiry_date_millis ASC, name COLLATE NOCASE ASC")
    List<ProductEntity> filter(String status, String storageLocationId);

    @Query("SELECT * FROM products WHERE owner_user_id = :ownerUserId AND (:status IS NULL OR status = :status) AND (:storageLocationId IS NULL OR storage_location_id = :storageLocationId) ORDER BY expiry_date_millis ASC, name COLLATE NOCASE ASC")
    List<ProductEntity> filterForOwner(String ownerUserId, String status, String storageLocationId);

    @Query("SELECT * FROM products WHERE owner_user_id IS NULL AND (:status IS NULL OR status = :status) AND (:storageLocationId IS NULL OR storage_location_id = :storageLocationId) ORDER BY expiry_date_millis ASC, name COLLATE NOCASE ASC")
    List<ProductEntity> filterLocal(String status, String storageLocationId);

    @Query("SELECT COUNT(*) FROM products WHERE owner_user_id IS NULL")
    int countOwnerlessProducts();

    @Query("UPDATE products SET owner_user_id = :ownerUserId, sync_status = :syncStatus, updated_at = :updatedAt WHERE owner_user_id IS NULL")
    int assignOwnerToOwnerlessProducts(String ownerUserId, String syncStatus, long updatedAt);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProductEntity product);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProductEntity> products);

    @Update
    int update(ProductEntity product);

    @Query("DELETE FROM products WHERE id = :id")
    int deleteById(String id);

    @Query("DELETE FROM products")
    int deleteAll();

    @Query("UPDATE products SET status = :status, updated_at = :updatedAt, sync_status = :syncStatus WHERE id = :id")
    int updateStatus(String id, String status, long updatedAt, String syncStatus);

    @Query("UPDATE products SET cloud_id = :cloudId, sync_status = :syncStatus, last_synced_at = :lastSyncedAt WHERE id = :id")
    int updateSyncMetadata(String id, String cloudId, String syncStatus, Long lastSyncedAt);
}
