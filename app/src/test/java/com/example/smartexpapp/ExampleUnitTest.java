package com.example.smartexpapp;

import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.util.ProductQuantityValidator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class ExampleUnitTest {
    @Test
    public void buildConfigUsesExpectedApplicationId() {
        assertEquals("com.example.smartexpapp", BuildConfig.APPLICATION_ID);
        assertFalse(BuildConfig.VERSION_NAME.trim().isEmpty());
    }

    @Test
    public void finalSubmissionCoreContractsStayStable() {
        assertEquals(
                LocalDataContract.STORAGE_FREEZE_ID,
                LocalDataContract.storageIdForName("freezer")
        );
        assertEquals(
                LocalDataContract.STORAGE_REFRIGERATOR_ID,
                LocalDataContract.storageIdForName("cool")
        );
        assertEquals(
                LocalDataContract.STORAGE_FREEZE_NAME,
                LocalDataContract.storageNameForId(LocalDataContract.STORAGE_FREEZE_ID)
        );
        assertEquals("1.5", ProductQuantityValidator.normalize("1.50"));
        assertNull(ProductQuantityValidator.normalize("0"));
    }
}
