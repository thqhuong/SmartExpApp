package com.example.smartexpapp.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ExpiryScanDao {
    @Query("SELECT * FROM expiry_scans WHERE product_id = :productId ORDER BY scanned_at DESC")
    List<ExpiryScanEntity> getForProduct(String productId);

    @Query("SELECT * FROM expiry_scans ORDER BY scanned_at DESC")
    List<ExpiryScanEntity> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ExpiryScanEntity scan);
}
