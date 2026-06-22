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

    @Query("SELECT inventory_actions.* FROM inventory_actions INNER JOIN products ON products.id = inventory_actions.product_id WHERE products.owner_user_id = :ownerUserId ORDER BY inventory_actions.action_at DESC")
    List<InventoryActionEntity> getAllForOwner(String ownerUserId);

    @Query("SELECT COUNT(*) FROM inventory_actions WHERE action_type IN (:actionTypes)")
    int countByActionTypes(String[] actionTypes);

    @Query("SELECT COUNT(*) FROM inventory_actions WHERE action_type IN (:actionTypes) AND action_at >= :sinceMillis")
    int countByActionTypesSince(String[] actionTypes, long sinceMillis);

    @Query("SELECT COUNT(*) FROM inventory_actions INNER JOIN products ON products.id = inventory_actions.product_id WHERE products.owner_user_id = :ownerUserId AND inventory_actions.action_type IN (:actionTypes)")
    int countByActionTypesForOwner(String ownerUserId, String[] actionTypes);

    @Query("SELECT COUNT(*) FROM inventory_actions INNER JOIN products ON products.id = inventory_actions.product_id WHERE products.owner_user_id = :ownerUserId AND inventory_actions.action_type IN (:actionTypes) AND inventory_actions.action_at >= :sinceMillis")
    int countByActionTypesForOwnerSince(String ownerUserId, String[] actionTypes, long sinceMillis);

    @Query("SELECT COUNT(*) FROM inventory_actions INNER JOIN products ON products.id = inventory_actions.product_id WHERE products.owner_user_id IS NULL AND inventory_actions.action_type IN (:actionTypes)")
    int countLocalByActionTypes(String[] actionTypes);

    @Query("SELECT COUNT(*) FROM inventory_actions INNER JOIN products ON products.id = inventory_actions.product_id WHERE products.owner_user_id IS NULL AND inventory_actions.action_type IN (:actionTypes) AND inventory_actions.action_at >= :sinceMillis")
    int countLocalByActionTypesSince(String[] actionTypes, long sinceMillis);

    @Query("DELETE FROM inventory_actions")
    int deleteAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(InventoryActionEntity action);
}
