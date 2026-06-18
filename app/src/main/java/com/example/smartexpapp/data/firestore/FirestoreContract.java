package com.example.smartexpapp.data.firestore;

public final class FirestoreContract {
    public static final String USERS = "users";
    public static final String PRODUCTS = "products";
    public static final String INVENTORY_ACTIONS = "inventoryActions";
    public static final String SETTINGS = "settings";
    public static final String CATEGORIES = "categories";
    public static final String STORAGE_LOCATIONS = "storageLocations";
    public static final String DEFAULT_SETTINGS_ID = "default";

    public static final class ProductFields {
        public static final String LOCAL_ID = "localId";
        public static final String OWNER_USER_ID = "ownerUserId";
        public static final String NAME = "name";
        public static final String CATEGORY = "category";
        public static final String QUANTITY = "quantity";
        public static final String UNIT = "unit";
        public static final String STORAGE_LOCATION_ID = "storageLocationId";
        public static final String EXPIRY_DATE_MILLIS = "expiryDateMillis";
        public static final String BARCODE = "barcode";
        public static final String IMAGE_URI = "imageUri";
        public static final String STATUS = "status";
        public static final String CREATED_AT = "createdAt";
        public static final String UPDATED_AT = "updatedAt";
        public static final String DELETED_AT = "deletedAt";

        private ProductFields() {
        }
    }

    public static String userPath(String userId) {
        return USERS + "/" + userId;
    }

    public static String productsPath(String userId) {
        return userPath(userId) + "/" + PRODUCTS;
    }

    public static String productPath(String userId, String cloudId) {
        return productsPath(userId) + "/" + cloudId;
    }

    public static String settingsPath(String userId) {
        return userPath(userId) + "/" + SETTINGS;
    }

    public static String inventoryActionsPath(String userId) {
        return userPath(userId) + "/" + INVENTORY_ACTIONS;
    }

    private FirestoreContract() {
    }
}
