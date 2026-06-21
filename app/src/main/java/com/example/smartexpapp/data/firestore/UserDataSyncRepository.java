package com.example.smartexpapp.data.firestore;

import android.content.Context;

import com.example.smartexpapp.data.AuthStateRepository;
import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.CategoryEntity;
import com.example.smartexpapp.data.local.InventoryActionEntity;
import com.example.smartexpapp.data.local.UserSettingsEntity;
import com.example.smartexpapp.model.Product;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UserDataSyncRepository {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private UserDataSyncRepository() {
    }

    public static void syncUserDataAsync(Context context, AppDatabase database) {
        SyncTarget target = targetFor(context);
        if (target == null) {
            SyncStatusRepository.markLocalOnly(context);
            return;
        }
        SyncStatusRepository.markSyncing(context);
        EXECUTOR.execute(() -> {
            syncSettingsAsync(target, database);
            syncCategoriesAsync(target, database);
            syncInventoryActionsAsync(target, database);
        });
        SyncStatusRepository.markSyncEnabled(context);
    }

    public static void syncSettingsAsync(Context context, AppDatabase database) {
        SyncTarget target = targetFor(context);
        if (target == null) {
            return;
        }
        EXECUTOR.execute(() -> syncSettingsAsync(target, database));
    }

    public static void syncInventoryActionsAsync(Context context, AppDatabase database) {
        SyncTarget target = targetFor(context);
        if (target == null) {
            return;
        }
        EXECUTOR.execute(() -> syncInventoryActionsAsync(target, database));
    }

    public static void syncCategoriesAsync(Context context, AppDatabase database) {
        SyncTarget target = targetFor(context);
        if (target == null) {
            return;
        }
        EXECUTOR.execute(() -> syncCategoriesAsync(target, database));
    }

    private static void syncSettingsAsync(SyncTarget target, AppDatabase database) {
        target.firestore.document(FirestoreContract.settingsPath(target.userId) + "/" + FirestoreContract.DEFAULT_SETTINGS_ID)
                .get()
                .addOnSuccessListener(document -> EXECUTOR.execute(() -> {
                    UserSettingsEntity local = database.userSettingsDao().getById(FirestoreContract.DEFAULT_SETTINGS_ID);
                    if (!document.exists()) {
                        if (local != null) {
                            uploadSettings(target, local);
                        }
                        return;
                    }
                    long remoteUpdatedAt = longValue(document.get("updatedAt"), 0L);
                    if (local == null || remoteUpdatedAt > local.updatedAt) {
                        database.userSettingsDao().insert(settingsEntity(document.getData(), remoteUpdatedAt));
                        SyncStatusRepository.markSynced(target.context);
                        return;
                    }
                    if (local.updatedAt > remoteUpdatedAt) {
                        uploadSettings(target, local);
                    } else {
                        SyncStatusRepository.markSynced(target.context);
                    }
                }))
                .addOnFailureListener(error -> SyncStatusRepository.markError(target.context));
    }

    private static void uploadSettings(SyncTarget target, UserSettingsEntity local) {
        target.firestore.document(FirestoreContract.settingsPath(target.userId) + "/" + FirestoreContract.DEFAULT_SETTINGS_ID)
                .set(settingsDocument(local, target.userId), SetOptions.merge())
                .addOnSuccessListener(unused -> SyncStatusRepository.markSynced(target.context))
                .addOnFailureListener(error -> SyncStatusRepository.markError(target.context));
    }

    private static void syncInventoryActionsAsync(SyncTarget target, AppDatabase database) {
        List<InventoryActionEntity> localActions = database.inventoryActionDao().getAllForOwner(target.userId);
        for (InventoryActionEntity action : localActions) {
            target.firestore.collection(FirestoreContract.inventoryActionsPath(target.userId))
                    .document(action.id)
                    .set(actionDocument(action, target.userId), SetOptions.merge());
        }

        target.firestore.collection(FirestoreContract.inventoryActionsPath(target.userId))
                .get()
                .addOnSuccessListener(snapshot -> EXECUTOR.execute(() -> {
                    for (com.google.firebase.firestore.DocumentSnapshot document : snapshot.getDocuments()) {
                        InventoryActionEntity action = actionEntity(document.getData(), document.getId());
                        if (database.productDao().getById(action.productId) == null) {
                            SyncStatusRepository.markError(target.context);
                            continue;
                        }
                        database.inventoryActionDao().insert(action);
                    }
                    SyncStatusRepository.markSynced(target.context);
                }))
                .addOnFailureListener(error -> SyncStatusRepository.markError(target.context));
    }

    private static void syncCategoriesAsync(SyncTarget target, AppDatabase database) {
        target.firestore.collection(FirestoreContract.categoriesPath(target.userId))
                .get()
                .addOnSuccessListener(snapshot -> EXECUTOR.execute(() -> {
                    long syncedAt = System.currentTimeMillis();
                    for (com.google.firebase.firestore.DocumentSnapshot document : snapshot.getDocuments()) {
                        CategoryEntity remote = categoryEntity(document.getData(), document.getId(), target.userId, syncedAt);
                        CategoryEntity local = database.categoryDao().getById(remote.id);
                        if (remote.deletedAt != null && remote.deletedAt > 0L) {
                            if (local != null) {
                                local.active = false;
                                local.deletedAt = remote.deletedAt;
                                local.updatedAt = remote.updatedAt;
                                local.syncStatus = Product.SYNC_STATUS_SYNCED;
                                local.lastSyncedAt = syncedAt;
                                database.categoryDao().update(local);
                            } else {
                                database.categoryDao().insert(remote);
                            }
                            continue;
                        }
                        if (local == null || remote.updatedAt >= local.updatedAt || Product.SYNC_STATUS_SYNCED.equals(local.syncStatus)) {
                            database.categoryDao().insert(remote);
                        }
                    }
                    uploadPendingCategories(target, database);
                    SyncStatusRepository.markSynced(target.context);
                }))
                .addOnFailureListener(error -> SyncStatusRepository.markError(target.context));
    }

    private static void uploadPendingCategories(SyncTarget target, AppDatabase database) {
        List<CategoryEntity> pending = database.categoryDao().getByOwnerAndSyncStatus(target.userId, Product.SYNC_STATUS_PENDING_UPLOAD);
        for (CategoryEntity category : pending) {
            String cloudId = hasText(category.cloudId) ? category.cloudId : category.id;
            target.firestore.collection(FirestoreContract.categoriesPath(target.userId))
                    .document(cloudId)
                    .set(categoryDocument(category, target.userId), SetOptions.merge())
                    .addOnSuccessListener(unused -> EXECUTOR.execute(() -> {
                        database.categoryDao().updateSyncMetadata(category.id, cloudId, Product.SYNC_STATUS_SYNCED, System.currentTimeMillis());
                        SyncStatusRepository.markSynced(target.context);
                    }))
                    .addOnFailureListener(error -> SyncStatusRepository.markError(target.context));
        }
    }

    private static Map<String, Object> settingsDocument(UserSettingsEntity settings, String ownerUserId) {
        Map<String, Object> data = new HashMap<>();
        data.put("ownerUserId", ownerUserId);
        data.put("displayName", settings.displayName);
        data.put("reminderDaysBefore", settings.reminderDaysBefore);
        data.put("reminderNotifyTimeMinutes", settings.reminderNotifyTimeMinutes);
        data.put("dietaryPreferences", settings.dietaryPreferences);
        data.put("darkMode", settings.darkMode);
        data.put("languageTag", settings.languageTag);
        data.put("defaultStorageLocationId", settings.defaultStorageLocationId);
        data.put("notificationEnabled", settings.notificationEnabled);
        data.put("createdAt", settings.createdAt);
        data.put("updatedAt", settings.updatedAt);
        return data;
    }

    private static UserSettingsEntity settingsEntity(Map<String, Object> data, long remoteUpdatedAt) {
        UserSettingsEntity settings = new UserSettingsEntity();
        settings.id = FirestoreContract.DEFAULT_SETTINGS_ID;
        settings.displayName = stringValue(data, "displayName", "Local User");
        settings.reminderDaysBefore = (int) longValue(data.get("reminderDaysBefore"), 3L);
        settings.reminderNotifyTimeMinutes = (int) longValue(data.get("reminderNotifyTimeMinutes"), 540L);
        settings.dietaryPreferences = stringValue(data, "dietaryPreferences", null);
        settings.darkMode = booleanValue(data.get("darkMode"), false);
        settings.languageTag = stringValue(data, "languageTag", "en");
        settings.defaultStorageLocationId = stringValue(data, "defaultStorageLocationId", settings.defaultStorageLocationId);
        settings.notificationEnabled = booleanValue(data.get("notificationEnabled"), true);
        settings.createdAt = longValue(data.get("createdAt"), remoteUpdatedAt);
        settings.updatedAt = remoteUpdatedAt;
        return settings;
    }

    private static Map<String, Object> actionDocument(InventoryActionEntity action, String ownerUserId) {
        Map<String, Object> data = new HashMap<>();
        data.put("ownerUserId", ownerUserId);
        data.put("localId", action.id);
        data.put("productLocalId", action.productId);
        data.put("actionType", action.actionType);
        data.put("quantityChanged", action.quantityChanged);
        data.put("actionAt", action.actionAt);
        data.put("note", action.note);
        data.put("createdAt", action.createdAt);
        data.put("updatedAt", action.updatedAt);
        return data;
    }

    private static InventoryActionEntity actionEntity(Map<String, Object> data, String documentId) {
        InventoryActionEntity action = new InventoryActionEntity();
        action.id = stringValue(data, "localId", documentId);
        action.productId = stringValue(data, "productLocalId", "");
        action.actionType = stringValue(data, "actionType", "");
        action.quantityChanged = (int) longValue(data.get("quantityChanged"), 0L);
        action.actionAt = longValue(data.get("actionAt"), System.currentTimeMillis());
        action.note = stringValue(data, "note", null);
        action.createdAt = longValue(data.get("createdAt"), action.actionAt);
        action.updatedAt = longValue(data.get("updatedAt"), action.createdAt);
        return action;
    }

    private static Map<String, Object> categoryDocument(CategoryEntity category, String ownerUserId) {
        Map<String, Object> data = new HashMap<>();
        data.put(FirestoreContract.CategoryFields.OWNER_USER_ID, ownerUserId);
        data.put(FirestoreContract.CategoryFields.LOCAL_ID, category.id);
        data.put(FirestoreContract.CategoryFields.NAME, category.name);
        data.put(FirestoreContract.CategoryFields.SORT_ORDER, category.sortOrder);
        data.put(FirestoreContract.CategoryFields.IS_BUILT_IN, category.builtIn);
        data.put(FirestoreContract.CategoryFields.ACTIVE, category.active);
        data.put(FirestoreContract.CategoryFields.CREATED_AT, category.createdAt);
        data.put(FirestoreContract.CategoryFields.UPDATED_AT, category.updatedAt);
        if (category.deletedAt != null) {
            data.put(FirestoreContract.CategoryFields.DELETED_AT, category.deletedAt);
        }
        return data;
    }

    private static CategoryEntity categoryEntity(Map<String, Object> data, String documentId, String ownerUserId, long syncedAt) {
        CategoryEntity category = new CategoryEntity();
        category.id = stringValue(data, FirestoreContract.CategoryFields.LOCAL_ID, documentId);
        category.cloudId = documentId;
        category.ownerUserId = ownerUserId;
        category.name = stringValue(data, FirestoreContract.CategoryFields.NAME, "General");
        category.sortOrder = (int) longValue(data.get(FirestoreContract.CategoryFields.SORT_ORDER), 0L);
        category.builtIn = booleanValue(data.get(FirestoreContract.CategoryFields.IS_BUILT_IN), false);
        category.active = booleanValue(data.get(FirestoreContract.CategoryFields.ACTIVE), true);
        category.createdAt = longValue(data.get(FirestoreContract.CategoryFields.CREATED_AT), syncedAt);
        category.updatedAt = longValue(data.get(FirestoreContract.CategoryFields.UPDATED_AT), category.createdAt);
        long deletedAt = longValue(data.get(FirestoreContract.CategoryFields.DELETED_AT), 0L);
        category.deletedAt = deletedAt > 0L ? deletedAt : null;
        category.syncStatus = Product.SYNC_STATUS_SYNCED;
        category.lastSyncedAt = syncedAt;
        return category;
    }

    private static SyncTarget targetFor(Context context) {
        AuthStateRepository.AuthState authState = AuthStateRepository.getAuthState(context);
        if (!authState.isSignedIn() || !hasText(authState.getUserId())) {
            return null;
        }
        FirebaseFirestore firestore = FirestoreProvider.getInstance(context);
        return firestore == null ? null : new SyncTarget(context.getApplicationContext(), firestore, authState.getUserId());
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String stringValue(Map<String, Object> data, String key, String fallback) {
        if (data == null) {
            return fallback;
        }
        Object value = data.get(key);
        return value instanceof String && !((String) value).trim().isEmpty() ? (String) value : fallback;
    }

    private static long longValue(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private static final class SyncTarget {
        private final Context context;
        private final FirebaseFirestore firestore;
        private final String userId;

        private SyncTarget(Context context, FirebaseFirestore firestore, String userId) {
            this.context = context;
            this.firestore = firestore;
            this.userId = userId;
        }
    }
}
