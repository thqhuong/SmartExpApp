package com.example.smartexpapp.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.model.ProductStatus;

@Entity(
        tableName = "products",
        foreignKeys = @ForeignKey(
                entity = StorageLocationEntity.class,
                parentColumns = "id",
                childColumns = "storage_location_id"
        ),
        indices = {
                @Index("storage_location_id"),
                @Index("expiry_date_millis"),
                @Index("barcode")
        }
)
public class ProductEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @NonNull
    public String name = "";

    @NonNull
    public String category = "General";

    @NonNull
    public String quantity = "1";

    @NonNull
    public String unit = "pcs";

    @ColumnInfo(name = "storage_location_id")
    @NonNull
    public String storageLocationId = LocalDataContract.STORAGE_ROOM_TEMP_ID;

    @ColumnInfo(name = "expiry_date_millis")
    public long expiryDateMillis;

    public String barcode;

    @ColumnInfo(name = "image_uri")
    public String imageUri;

    @NonNull
    public String status = ProductStatus.ACTIVE;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @ColumnInfo(name = "cloud_id")
    public String cloudId;

    @ColumnInfo(name = "owner_user_id")
    public String ownerUserId;

    @ColumnInfo(name = "sync_status")
    @NonNull
    public String syncStatus = Product.SYNC_STATUS_LOCAL;

    @ColumnInfo(name = "last_synced_at")
    public Long lastSyncedAt;

    public ProductEntity() {
    }
}
