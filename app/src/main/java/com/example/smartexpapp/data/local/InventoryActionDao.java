package com.example.smartexpapp.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface InventoryActionDao {
    @Query("SELECT * FROM inventory_actions WHERE product_id = :productId ORDER BY action_at DESC")
    List<InventoryActionEntity> getForProduct(String productId);

    @Query("SELECT * FROM inventory_actions ORDER BY action_at DESC")
    List<InventoryActionEntity> getAll();

    @Query("SELECT COUNT(*) FROM inventory_actions WHERE action_type IN (:actionTypes)")
    int countByActionTypes(String[] actionTypes);

    @Query("SELECT COUNT(*) FROM inventory_actions WHERE action_type IN (:actionTypes) AND action_at >= :sinceMillis")
    int countByActionTypesSince(String[] actionTypes, long sinceMillis);

    @Query("DELETE FROM inventory_actions")
    int deleteAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(InventoryActionEntity action);
}
