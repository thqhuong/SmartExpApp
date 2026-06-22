package com.example.smartexpapp.data.firestore;

import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.data.local.ProductEntity;
import com.example.smartexpapp.model.ProductStatus;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class FirestoreProductMapperTest {
    @Test
    public void toDocumentUsesOwnerScopedProductFields() {
        ProductEntity entity = new ProductEntity();
        entity.id = "local-product-1";
        entity.name = "Milk";
        entity.category = "Dairy";
        entity.quantity = "2";
        entity.unit = "cartons";
        entity.storageLocationId = LocalDataContract.STORAGE_REFRIGERATOR_ID;
        entity.expiryDateMillis = 123456789L;
        entity.barcode = "12345";
        entity.imageUri = "content://image";
        entity.status = ProductStatus.ACTIVE;
        entity.createdAt = 100L;
        entity.updatedAt = 200L;

        Map<String, Object> document = FirestoreProductMapper.toDocument(entity, "firebase-user-123");

        assertEquals("local-product-1", document.get(FirestoreContract.ProductFields.LOCAL_ID));
        assertEquals("firebase-user-123", document.get(FirestoreContract.ProductFields.OWNER_USER_ID));
        assertEquals("Milk", document.get(FirestoreContract.ProductFields.NAME));
        assertEquals(LocalDataContract.STORAGE_REFRIGERATOR_ID, document.get(FirestoreContract.ProductFields.STORAGE_LOCATION_ID));
        assertEquals(123456789L, document.get(FirestoreContract.ProductFields.EXPIRY_DATE_MILLIS));
        assertEquals(ProductStatus.ACTIVE, document.get(FirestoreContract.ProductFields.STATUS));
        assertEquals(100L, document.get(FirestoreContract.ProductFields.CREATED_AT));
        assertEquals(200L, document.get(FirestoreContract.ProductFields.UPDATED_AT));
    }

    @Test
    public void longValueAcceptsFirestoreNumericFieldValues() {
        assertEquals(123L, FirestoreProductMapper.longValue(Long.valueOf(123L), 0L));
        assertEquals(456L, FirestoreProductMapper.longValue(Integer.valueOf(456), 0L));
        assertEquals(789L, FirestoreProductMapper.longValue("789", 0L));
        assertEquals(99L, FirestoreProductMapper.longValue("not-a-number", 99L));
        assertEquals(99L, FirestoreProductMapper.longValue(null, 99L));
    }
}
