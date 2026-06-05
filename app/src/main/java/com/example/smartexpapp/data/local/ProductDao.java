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

    @Query("SELECT * FROM products ORDER BY expiry_date_millis ASC, name COLLATE NOCASE ASC")
    List<ProductEntity> getAllProductsSortedByExpiry();

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    ProductEntity getById(String id);

    @Query("SELECT * FROM products WHERE status = 'ACTIVE' AND expiry_date_millis BETWEEN :startMillis AND :endMillis ORDER BY expiry_date_millis ASC")
    List<ProductEntity> getExpiringBetween(long startMillis, long endMillis);

    @Query("SELECT * FROM products WHERE status = 'ACTIVE' AND expiry_date_millis < :beforeMillis ORDER BY expiry_date_millis ASC")
    List<ProductEntity> getExpiredBefore(long beforeMillis);

    @Query("SELECT * FROM products WHERE status = 'ACTIVE' AND (LOWER(name) LIKE '%' || LOWER(:query) || '%' OR LOWER(category) LIKE '%' || LOWER(:query) || '%') ORDER BY expiry_date_millis ASC, name COLLATE NOCASE ASC")
    List<ProductEntity> searchActive(String query);

    @Query("SELECT * FROM products WHERE status = 'ACTIVE' AND storage_location_id = :storageLocationId ORDER BY expiry_date_millis ASC, name COLLATE NOCASE ASC")
    List<ProductEntity> getActiveByStorageLocation(String storageLocationId);

    @Query("SELECT * FROM products WHERE (:status IS NULL OR status = :status) AND (:storageLocationId IS NULL OR storage_location_id = :storageLocationId) ORDER BY expiry_date_millis ASC, name COLLATE NOCASE ASC")
    List<ProductEntity> filter(String status, String storageLocationId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProductEntity product);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProductEntity> products);

    @Update
    int update(ProductEntity product);

    @Query("DELETE FROM products WHERE id = :id")
    int deleteById(String id);

    @Query("UPDATE products SET status = :status, updated_at = :updatedAt, sync_status = :syncStatus WHERE id = :id")
    int updateStatus(String id, String status, long updatedAt, String syncStatus);
}
