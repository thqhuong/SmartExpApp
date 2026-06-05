package com.example.smartexpapp.data.local;

import com.example.smartexpapp.R;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.model.ProductStatus;

public final class ProductMapper {
    private ProductMapper() {
    }

    public static Product toModel(ProductEntity entity) {
        String storageName = LocalDataContract.storageNameForId(entity.storageLocationId);
        return new Product(
                entity.id,
                entity.name,
                entity.category,
                entity.quantity,
                entity.unit,
                storageName,
                entity.storageLocationId,
                entity.expiryDateMillis,
                entity.barcode,
                valueOrDefault(entity.status, ProductStatus.ACTIVE),
                iconForStorage(entity.storageLocationId),
                entity.imageUri,
                entity.createdAt,
                entity.updatedAt,
                entity.cloudId,
                entity.ownerUserId,
                valueOrDefault(entity.syncStatus, Product.SYNC_STATUS_LOCAL),
                entity.lastSyncedAt
        );
    }

    public static ProductEntity toEntity(Product product) {
        ProductEntity entity = new ProductEntity();
        entity.id = product.getId();
        entity.name = product.getName();
        entity.category = product.getCategory();
        entity.quantity = product.getQuantity();
        entity.unit = product.getUnit();
        entity.storageLocationId = product.getStorageLocationId() == null
                ? LocalDataContract.storageIdForName(product.getStorage())
                : product.getStorageLocationId();
        entity.expiryDateMillis = product.getExpiryDateMillis();
        entity.barcode = product.getBarcode();
        entity.imageUri = product.getImageUrl();
        entity.status = valueOrDefault(product.getStatus(), ProductStatus.ACTIVE);
        entity.createdAt = product.getCreatedAt();
        entity.updatedAt = product.getUpdatedAt();
        entity.cloudId = product.getCloudId();
        entity.ownerUserId = product.getOwnerUserId();
        entity.syncStatus = valueOrDefault(product.getSyncStatus(), Product.SYNC_STATUS_LOCAL);
        entity.lastSyncedAt = product.getLastSyncedAt();
        return entity;
    }

    public static int iconForStorage(String storageLocationId) {
        if (LocalDataContract.STORAGE_FREEZE_ID.equals(storageLocationId)) {
            return R.drawable.ic_storage_freeze;
        }
        if (LocalDataContract.STORAGE_REFRIGERATOR_ID.equals(storageLocationId)) {
            return R.drawable.ic_storage_fridge;
        }
        return R.drawable.ic_storage_room;
    }

    private static String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value;
    }
}
