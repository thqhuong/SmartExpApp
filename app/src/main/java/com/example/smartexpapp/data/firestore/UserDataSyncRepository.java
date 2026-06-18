package com.example.smartexpapp.data.firestore;

import android.content.Context;

import com.example.smartexpapp.data.AuthStateRepository;
import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.InventoryActionEntity;
import com.example.smartexpapp.data.local.UserSettingsEntity;
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

    private static void syncSettingsAsync(SyncTarget target, AppDatabase database) {
        UserSettingsEntity local = database.userSettingsDao().getById(FirestoreContract.DEFAULT_SETTINGS_ID);
        if (local != null) {
            target.firestore.document(FirestoreContract.settingsPath(target.userId) + "/" + FirestoreContract.DEFAULT_SETTINGS_ID)
                    .set(settingsDocument(local, target.userId), SetOptions.merge());
        }

        target.firestore.document(FirestoreContract.settingsPath(target.userId) + "/" + FirestoreContract.DEFAULT_SETTINGS_ID)
                .get()
                .addOnSuccessListener(document -> EXECUTOR.execute(() -> {
                    if (!document.exists()) {
                        return;
                    }
                    UserSettingsEntity current = database.userSettingsDao().getById(FirestoreContract.DEFAULT_SETTINGS_ID);
                    long remoteUpdatedAt = longValue(document.get("updatedAt"), 0L);
                    if (current == null || remoteUpdatedAt > current.updatedAt) {
                        database.userSettingsDao().insert(settingsEntity(document.getData(), remoteUpdatedAt));
                    }
                    SyncStatusRepository.markSynced(target.context);
                }))
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
                        try {
                            database.inventoryActionDao().insert(action);
                        } catch (RuntimeException ignored) {
                            // Product rows sync separately; skip action rows until their product exists locally.
                        }
                    }
                    SyncStatusRepository.markSynced(target.context);
                }))
                .addOnFailureListener(error -> SyncStatusRepository.markError(target.context));
    }

    private static Map<String, Object> settingsDocument(UserSettingsEntity settings, String ownerUserId) {
        Map<String, Object> data = new HashMap<>();
        data.put("ownerUserId", ownerUserId);
        data.put("displayName", settings.displayName);
        data.put("reminderDaysBefore", settings.reminderDaysBefore);
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
