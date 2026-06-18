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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ProductSyncRepository {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static ListenerRegistration productListener;
    private static String productListenerUserId;

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
                            applyRemoteProducts(database, snapshot.getDocuments(), target.userId);
                            uploadPendingProducts(target, database);
                            MAIN_HANDLER.post(() -> {
                                startProductListener(target, database);
                                SyncStatusRepository.markSyncEnabled(context);
                                postSuccess(callback);
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
        return targetFor(context) != null;
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
                uploadProduct(target, database, entity);
            } catch (RuntimeException error) {
                SyncStatusRepository.markError(context);
            }
        });
    }

    public static void deleteProductAsync(Context context, Product product) {
        SyncTarget target = targetFor(context);
        if (target == null || product == null || !target.userId.equals(product.getOwnerUserId())) {
            return;
        }
        SyncStatusRepository.markSyncing(context);
        String cloudId = hasText(product.getCloudId()) ? product.getCloudId() : product.getId();
        long now = System.currentTimeMillis();
        Map<String, Object> data = new HashMap<>();
        data.put(FirestoreContract.ProductFields.OWNER_USER_ID, target.userId);
        data.put(FirestoreContract.ProductFields.LOCAL_ID, product.getId());
        data.put(FirestoreContract.ProductFields.DELETED_AT, now);
        data.put(FirestoreContract.ProductFields.UPDATED_AT, now);
        target.products().document(cloudId).set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> SyncStatusRepository.markSynced(context))
                .addOnFailureListener(error -> SyncStatusRepository.markError(context));
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
                    applyRemoteProducts(database, snapshot.getDocuments(), target.userId);
                } catch (RuntimeException applyError) {
                    SyncStatusRepository.markError(target.context);
                }
            });
        });
    }

    private static void applyRemoteProducts(AppDatabase database, List<com.google.firebase.firestore.DocumentSnapshot> documents, String ownerUserId) {
        long syncedAt = System.currentTimeMillis();
        database.runInTransaction(() -> {
            for (com.google.firebase.firestore.DocumentSnapshot document : documents) {
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
                if (Product.SYNC_STATUS_PENDING_UPLOAD.equals(local.syncStatus) && local.updatedAt > remote.updatedAt) {
                    database.productDao().updateSyncMetadata(
                            local.id,
                            hasText(local.cloudId) ? local.cloudId : document.getId(),
                            Product.SYNC_STATUS_CONFLICT,
                            local.lastSyncedAt
                    );
                    continue;
                }
                if (remote.updatedAt >= local.updatedAt || Product.SYNC_STATUS_SYNCED.equals(local.syncStatus)) {
                    database.productDao().insert(remote);
                }
            }
        });
    }

    private static void uploadPendingProducts(SyncTarget target, AppDatabase database) {
        List<ProductEntity> pending = database.productDao().getByOwnerAndSyncStatus(target.userId, Product.SYNC_STATUS_PENDING_UPLOAD);
        for (ProductEntity entity : pending) {
            uploadProduct(target, database, entity);
        }
    }

    private static void uploadProduct(SyncTarget target, AppDatabase database, ProductEntity entity) {
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
                }))
                .addOnFailureListener(error -> SyncStatusRepository.markError(target.context));
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
