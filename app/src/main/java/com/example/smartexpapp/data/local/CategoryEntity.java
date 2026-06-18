package com.example.smartexpapp.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.smartexpapp.model.Product;

@Entity(
        tableName = "categories",
        indices = {
                @Index(value = {"owner_user_id", "name"}, unique = true),
                @Index("sort_order")
        }
)
public class CategoryEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @NonNull
    public String name = "";

    @ColumnInfo(name = "sort_order")
    public int sortOrder;

    @ColumnInfo(name = "is_built_in")
    public boolean builtIn;

    public boolean active = true;

    @ColumnInfo(name = "owner_user_id")
    public String ownerUserId;

    @ColumnInfo(name = "cloud_id")
    public String cloudId;

    @ColumnInfo(name = "sync_status")
    @NonNull
    public String syncStatus = Product.SYNC_STATUS_LOCAL;

    @ColumnInfo(name = "last_synced_at")
    public Long lastSyncedAt;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @ColumnInfo(name = "deleted_at")
    public Long deletedAt;

    public CategoryEntity() {
    }
}
