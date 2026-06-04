package com.example.smartexpapp.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "inventory_actions",
        foreignKeys = @ForeignKey(
                entity = ProductEntity.class,
                parentColumns = "id",
                childColumns = "product_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("product_id")
)
public class InventoryActionEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "product_id")
    @NonNull
    public String productId = "";

    @ColumnInfo(name = "action_type")
    @NonNull
    public String actionType = "";

    @ColumnInfo(name = "quantity_changed")
    public int quantityChanged;

    @ColumnInfo(name = "action_at")
    public long actionAt;

    public String note;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public InventoryActionEntity() {
    }
}
