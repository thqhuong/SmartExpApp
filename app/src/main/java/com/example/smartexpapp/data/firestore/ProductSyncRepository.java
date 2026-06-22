package com.example.smartexpapp.data.firestore;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.smartexpapp.data.AuthStateRepository;
import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.ProductEntity;
import com.example.smartexpapp.model.Product;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ProductSyncRepository {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static ListenerRegistration productListener;
    private static String productListenerUserId;
    private static volatile Boolean testSyncAvailableOverride;

    public interface Callback<T> {
        void onResult(T value);
    }

    public interface ErrorCallback {
        void onError(Exception error);
    }

    private ProductSyncRepository() {
    }

    public static void syncProductsAsync(Context context, AppDatabase database) {
        initialSyncAsync(context, database, ignored -> { }, error -> { });
    }

    public static void initialSyncAsync(
            Context context,
            AppDatabase database,
            Callback<Void> callback,
            ErrorCallback errorCallback
    ) {
        SyncTarget target = targetFor(context);
        if (target == null) {
            SyncStatusRepository.markLocalOnly(context);
            postSuccess(callback);
            return;
        }
        try {
            SyncStatusRepository.markSyncing(context);
            target.products().get()
                    .addOnSuccessListener(snapshot -> EXECUTOR.execute(() -> {
                        try {
                            applyRemoteProducts(context, database, snapshot.getDocuments(), target.userId);
                            uploadPendingChanges(target, database, () -> MAIN_HANDLER.post(() -> {
                                        startProductListener(target, database);
                                        SyncStatusRepository.markSynced(context);
                                        postSuccess(callback);
                                    }),
                                    error -> {
                                        SyncStatusRepository.markError(context);
                                        postError(errorCallback, error);
                                    });
                        } catch (RuntimeException error) {
                            SyncStatusRepository.markError(context);
                            postError(errorCallback, error);
                        }
                    }))
                    .addOnFailureListener(error -> {
                        SyncStatusRepository.markError(context);
                        postError(errorCallback, error);
                    });
        } catch (RuntimeException error) {
            SyncStatusRepository.markError(context);
            postError(errorCallback, error);
        }
    }

    public static void stopProductListener() {
        if (productListener != null) {
            productListener.remove();
            productListener = null;
            productListenerUserId = null;
        }
    }

    public static boolean isSyncAvailable(Context context) {
        if (testSyncAvailableOverride != null) {
            return testSyncAvailableOverride;
        }
        return targetFor(context) != null;
    }

    public static void setTestSyncAvailableOverride(Boolean available) {
        testSyncAvailableOverride = available;
    }

    public static void uploadProductAsync(Context context, AppDatabase database, String localProductId) {
        SyncTarget target = targetFor(context);
        if (target == null) {
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                ProductEntity entity = database.productDao().getById(localProductId);
                if (entity == null || !target.userId.equals(entity.ownerUserId)) {
                    return;
                }
                SyncStatusRepository.markSyncing(context);
                uploadProduct(target, database, entity, null, null);
            } catch (RuntimeException error) {
                SyncStatusRepository.markError(context);
            }
        });
    }

    public static void deleteProductAsync(Context context, AppDatabase database, String localProductId) {
        SyncTarget target = targetFor(context);
        if (target == null) {
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                ProductEntity entity = database.productDao().getById(localProductId);
                if (entity == null || !target.userId.equals(entity.ownerUserId)) {
                    return;
                }
                SyncStatusRepository.markSyncing(context);
                uploadDelete(target, database, entity, null, null);
            } catch (RuntimeException error) {
                SyncStatusRepository.markError(context);
            }
        });
    }

    private static void startProductListener(SyncTarget target, AppDatabase database) {
        if (target.userId.equals(productListenerUserId) && productListener != null) {
            return;
        }
        stopProductListener();
        productListenerUserId = target.userId;
        productListener = target.products().addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null) {
                SyncStatusRepository.markError(target.context);
                return;
            }
            EXECUTOR.execute(() -> {
                try {
                    applyRemoteProducts(target.context, database, snapshot.getDocuments(), target.userId);
                } catch (RuntimeException applyError) {
                    SyncStatusRepository.markError(target.context);
                }
            });
        });
    }

    private static void applyRemoteProducts(Context context, AppDatabase database, List<com.google.firebase.firestore.DocumentSnapshot> documents, String ownerUserId) {
        long syncedAt = System.currentTimeMillis();
        int batchSize = 250;
        for (int i = 0; i < documents.size(); i += batchSize) {
            AuthStateRepository.AuthState authState = AuthStateRepository.getAuthState(context);
            if (!ownerUserId.equals(authState.getUserId())) {
                break;
            }
            int end = Math.min(documents.size(), i + batchSize);
            List<com.google.firebase.firestore.DocumentSnapshot> batch = documents.subList(i, end);
            database.runInTransaction(() -> {
                for (com.google.firebase.firestore.DocumentSnapshot document : batch) {
                    ProductEntity remote = FirestoreProductMapper.toEntity(document, syncedAt);
                    remote.ownerUserId = ownerUserId;
                    ProductEntity local = database.productDao().getById(remote.id);
                    long deletedAt = FirestoreProductMapper.deletedAt(document);
                    if (deletedAt > 0L) {
                        database.productDao().deleteById(remote.id);
                        continue;
                    }
                    if (local == null) {
                        database.productDao().insert(remote);
                        continue;
                    }
                    if (Product.SYNC_STATUS_PENDING_DELETE.equals(local.syncStatus)) {
                        continue;
                    }
                    if (Product.SYNC_STATUS_PENDING_UPLOAD.equals(local.syncStatus) && local.updatedAt > remote.updatedAt) {
                        if (local.lastSyncedAt != null && remote.updatedAt > local.lastSyncedAt) {
                            database.productDao().updateSyncMetadata(
                                    local.id,
                                    hasText(local.cloudId) ? local.cloudId : document.getId(),
                                    Product.SYNC_STATUS_CONFLICT,
                                    local.lastSyncedAt
                            );
                            continue;
                        }
                        continue;
                    }
                    if (remote.updatedAt == local.updatedAt) {
                        database.productDao().updateSyncMetadata(
                                local.id,
                                hasText(local.cloudId) ? local.cloudId : document.getId(),
                                Product.SYNC_STATUS_SYNCED,
                                syncedAt
                        );
                        continue;
                    }
                    if (remote.updatedAt >= local.updatedAt || Product.SYNC_STATUS_SYNCED.equals(local.syncStatus)) {
                        database.productDao().insert(remote);
                    }
                }
            });
        }
    }

    private static void uploadPendingChanges(SyncTarget target, AppDatabase database, Runnable successCallback, ErrorCallback errorCallback) {
        List<ProductEntity> pendingUploads = database.productDao().getByOwnerAndSyncStatus(target.userId, Product.SYNC_STATUS_PENDING_UPLOAD);
        List<ProductEntity> pendingDeletes = database.productDao().getByOwnerAndSyncStatus(target.userId, Product.SYNC_STATUS_PENDING_DELETE);
        int total = pendingUploads.size() + pendingDeletes.size();
        if (total == 0) {
            successCallback.run();
            return;
        }
        AtomicInteger remaining = new AtomicInteger(total);
        AtomicBoolean failed = new AtomicBoolean(false);
        Runnable oneDone = () -> {
            if (remaining.decrementAndGet() == 0 && !failed.get()) {
                successCallback.run();
            }
        };
        ErrorCallback oneError = error -> {
            if (failed.compareAndSet(false, true) && errorCallback != null) {
                errorCallback.onError(error);
            }
        };
        for (ProductEntity entity : pendingUploads) {
            uploadProduct(target, database, entity, oneDone, oneError);
        }
        for (ProductEntity entity : pendingDeletes) {
            uploadDelete(target, database, entity, oneDone, oneError);
        }
    }

    private static void uploadProduct(SyncTarget target, AppDatabase database, ProductEntity entity, Runnable successCallback, ErrorCallback errorCallback) {
        if (target.firestore == null) {
            if (successCallback != null) {
                successCallback.run();
            }
            return;
        }
        String cloudId = hasText(entity.cloudId) ? entity.cloudId : entity.id;
        target.products().document(cloudId)
                .set(FirestoreProductMapper.toDocument(entity, target.userId), SetOptions.merge())
                .addOnSuccessListener(unused -> EXECUTOR.execute(() -> {
                    database.productDao().updateSyncMetadata(
                            entity.id,
                            cloudId,
                            Product.SYNC_STATUS_SYNCED,
                            System.currentTimeMillis()
                    );
                    SyncStatusRepository.markSynced(target.context);
                    if (successCallback != null) {
                        successCallback.run();
                    }
                }))
                .addOnFailureListener(error -> {
                    SyncStatusRepository.markError(target.context);
                    if (errorCallback != null) {
                        errorCallback.onError(error);
                    }
                });
    }

    private static void uploadDelete(SyncTarget target, AppDatabase database, ProductEntity entity, Runnable successCallback, ErrorCallback errorCallback) {
        String cloudId = hasText(entity.cloudId) ? entity.cloudId : entity.id;
        long now = System.currentTimeMillis();
        Map<String, Object> data = new HashMap<>();
        data.put(FirestoreContract.ProductFields.OWNER_USER_ID, target.userId);
        data.put(FirestoreContract.ProductFields.LOCAL_ID, entity.id);
        data.put(FirestoreContract.ProductFields.DELETED_AT, now);
        data.put(FirestoreContract.ProductFields.UPDATED_AT, Math.max(entity.updatedAt, now));
        target.products().document(cloudId).set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> EXECUTOR.execute(() -> {
                    database.productDao().deleteById(entity.id);
                    SyncStatusRepository.markSynced(target.context);
                    if (successCallback != null) {
                        successCallback.run();
                    }
                }))
                .addOnFailureListener(error -> {
                    SyncStatusRepository.markError(target.context);
                    if (errorCallback != null) {
                        errorCallback.onError(error);
                    }
                });
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

    private static void postSuccess(Callback<Void> callback) {
        if (callback != null) {
            MAIN_HANDLER.post(() -> callback.onResult(null));
        }
    }

    private static void postError(ErrorCallback errorCallback, Exception error) {
        if (errorCallback != null) {
            MAIN_HANDLER.post(() -> errorCallback.onError(error));
        }
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

        private CollectionReference products() {
            return firestore.collection(FirestoreContract.productsPath(userId));
        }
    }
}
