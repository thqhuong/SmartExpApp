package com.example.smartexpapp.data.local;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Smoke coverage for the storage naming contract (issue #41). Display names and
 * legacy/alias values must normalize to the stable persisted ids, and ids must map
 * back to the canonical display names.
 */
public class StorageContractTest {
    @Test
    public void displayNamesNormalizeToCanonicalIds() {
        assertEquals(LocalDataContract.STORAGE_ROOM_TEMP_ID,
                LocalDataContract.storageIdForName("Room Temp"));
        assertEquals(LocalDataContract.STORAGE_REFRIGERATOR_ID,
                LocalDataContract.storageIdForName("Refrigerator"));
        assertEquals(LocalDataContract.STORAGE_FREEZE_ID,
                LocalDataContract.storageIdForName("Freezer"));
    }

    @Test
    public void legacyAndAliasNamesNormalizeToCanonicalIds() {
        assertEquals(LocalDataContract.STORAGE_REFRIGERATOR_ID,
                LocalDataContract.storageIdForName("fridge"));
        assertEquals(LocalDataContract.STORAGE_REFRIGERATOR_ID,
                LocalDataContract.storageIdForName("Cool"));
        assertEquals(LocalDataContract.STORAGE_FREEZE_ID,
                LocalDataContract.storageIdForName("freeze"));
        assertEquals(LocalDataContract.STORAGE_FREEZE_ID,
                LocalDataContract.storageIdForName("Frozen"));
    }

    @Test
    public void blankOrUnknownNamesFallBackToRoomTemp() {
        assertEquals(LocalDataContract.STORAGE_ROOM_TEMP_ID,
                LocalDataContract.storageIdForName(null));
        assertEquals(LocalDataContract.STORAGE_ROOM_TEMP_ID,
                LocalDataContract.storageIdForName("   "));
        assertEquals(LocalDataContract.STORAGE_ROOM_TEMP_ID,
                LocalDataContract.storageIdForName("pantry"));
    }

    @Test
    public void idsMapBackToDisplayNamesAndRoundTrip() {
        assertEquals(LocalDataContract.STORAGE_ROOM_TEMP_NAME,
                LocalDataContract.storageNameForId(LocalDataContract.STORAGE_ROOM_TEMP_ID));
        assertEquals(LocalDataContract.STORAGE_REFRIGERATOR_NAME,
                LocalDataContract.storageNameForId(LocalDataContract.STORAGE_REFRIGERATOR_ID));
        assertEquals(LocalDataContract.STORAGE_FREEZE_NAME,
                LocalDataContract.storageNameForId(LocalDataContract.STORAGE_FREEZE_ID));

        // name -> id -> name stays canonical for the freezer alias collapse
        assertEquals(LocalDataContract.STORAGE_FREEZE_ID,
                LocalDataContract.storageIdForName(
                        LocalDataContract.storageNameForId(LocalDataContract.STORAGE_FREEZE_ID)));
    }
}
