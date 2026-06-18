package com.example.smartexpapp.data.firestore;

import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.data.local.ProductEntity;
import com.example.smartexpapp.model.Product;
import com.example.smartexpapp.model.ProductStatus;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public final class FirestoreProductMapper {
    private FirestoreProductMapper() {
    }

    public static Map<String, Object> toDocument(ProductEntity entity, String ownerUserId) {
        Map<String, Object> data = new HashMap<>();
        data.put(FirestoreContract.ProductFields.LOCAL_ID, entity.id);
        data.put(FirestoreContract.ProductFields.OWNER_USER_ID, ownerUserId);
        data.put(FirestoreContract.ProductFields.NAME, entity.name);
        data.put(FirestoreContract.ProductFields.CATEGORY, entity.category);
        data.put(FirestoreContract.ProductFields.QUANTITY, entity.quantity);
        data.put(FirestoreContract.ProductFields.UNIT, entity.unit);
        data.put(FirestoreContract.ProductFields.STORAGE_LOCATION_ID, entity.storageLocationId);
        data.put(FirestoreContract.ProductFields.EXPIRY_DATE_MILLIS, entity.expiryDateMillis);
        data.put(FirestoreContract.ProductFields.BARCODE, entity.barcode);
        data.put(FirestoreContract.ProductFields.IMAGE_URI, entity.imageUri);
        data.put(FirestoreContract.ProductFields.STATUS, entity.status);
        data.put(FirestoreContract.ProductFields.CREATED_AT, entity.createdAt);
        data.put(FirestoreContract.ProductFields.UPDATED_AT, entity.updatedAt);
        return data;
    }

    public static ProductEntity toEntity(DocumentSnapshot document, long syncedAt) {
        ProductEntity entity = new ProductEntity();
        entity.id = stringValue(document, FirestoreContract.ProductFields.LOCAL_ID, document.getId());
        entity.name = stringValue(document, FirestoreContract.ProductFields.NAME, "Unnamed Product");
        entity.category = stringValue(document, FirestoreContract.ProductFields.CATEGORY, "General");
        entity.quantity = stringValue(document, FirestoreContract.ProductFields.QUANTITY, "1");
        entity.unit = stringValue(document, FirestoreContract.ProductFields.UNIT, "pcs");
        entity.storageLocationId = stringValue(document, FirestoreContract.ProductFields.STORAGE_LOCATION_ID, LocalDataContract.STORAGE_ROOM_TEMP_ID);
        entity.expiryDateMillis = longValue(document, FirestoreContract.ProductFields.EXPIRY_DATE_MILLIS, System.currentTimeMillis());
        entity.barcode = stringValue(document, FirestoreContract.ProductFields.BARCODE, null);
        entity.imageUri = stringValue(document, FirestoreContract.ProductFields.IMAGE_URI, null);
        entity.status = stringValue(document, FirestoreContract.ProductFields.STATUS, ProductStatus.ACTIVE);
        entity.createdAt = longValue(document, FirestoreContract.ProductFields.CREATED_AT, syncedAt);
        entity.updatedAt = longValue(document, FirestoreContract.ProductFields.UPDATED_AT, syncedAt);
        entity.cloudId = document.getId();
        entity.ownerUserId = stringValue(document, FirestoreContract.ProductFields.OWNER_USER_ID, null);
        entity.syncStatus = Product.SYNC_STATUS_SYNCED;
        entity.lastSyncedAt = syncedAt;
        return entity;
    }

    public static Product pendingUpload(Product product, long updatedAt) {
        return product.withSyncMetadata(
                product.getCloudId(),
                Product.SYNC_STATUS_PENDING_UPLOAD,
                product.getLastSyncedAt(),
                updatedAt
        );
    }

    public static long deletedAt(DocumentSnapshot document) {
        return longValue(document, FirestoreContract.ProductFields.DELETED_AT, 0L);
    }

    private static String stringValue(DocumentSnapshot document, String field, String fallback) {
        String value = document.getString(field);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private static long longValue(DocumentSnapshot document, String field, long fallback) {
        return longValue(document.get(field), fallback);
    }

    static long longValue(Object value, long fallback) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
