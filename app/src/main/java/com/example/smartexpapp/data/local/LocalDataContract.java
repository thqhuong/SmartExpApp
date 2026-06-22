package com.example.smartexpapp.data.local;

public final class LocalDataContract {
    public static final String STORAGE_ROOM_TEMP_ID = "room_temp";
    public static final String STORAGE_REFRIGERATOR_ID = "refrigerator";
    public static final String STORAGE_FREEZE_ID = "freeze";

    public static final String STORAGE_ROOM_TEMP_NAME = "Room Temp";
    public static final String STORAGE_REFRIGERATOR_NAME = "Refrigerator";
    public static final String STORAGE_FREEZE_NAME = "Freezer";

    private LocalDataContract() {
    }

    public static String storageIdForName(String storageName) {
        if (storageName == null) {
            return STORAGE_ROOM_TEMP_ID;
        }
        String normalized = storageName.trim().toLowerCase();
        if ("refrigerator".equals(normalized)
                || "fridge".equals(normalized)
                || "cool".equals(normalized)) {
            return STORAGE_REFRIGERATOR_ID;
        }
        if ("freezer".equals(normalized)
                || "freeze".equals(normalized)
                || "frozen".equals(normalized)) {
            return STORAGE_FREEZE_ID;
        }
        return STORAGE_ROOM_TEMP_ID;
    }

    public static String storageNameForId(String storageLocationId) {
        if (STORAGE_REFRIGERATOR_ID.equals(storageLocationId)) {
            return STORAGE_REFRIGERATOR_NAME;
        }
        if (STORAGE_FREEZE_ID.equals(storageLocationId)) {
            return STORAGE_FREEZE_NAME;
        }
        return STORAGE_ROOM_TEMP_NAME;
    }
}
