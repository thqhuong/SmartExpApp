package com.example.smartexpapp.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.notifications.ReminderScheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LocalDataResetRepository {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onResult(T value);
    }

    public interface ErrorCallback {
        void onError(Exception error);
    }

    public static final class ResetSummary {
        private final int productsDeleted;
        private final int inventoryActionsDeleted;
        private final int expiryScansDeleted;
        private final int recipeCacheDeleted;
        private final int agentMessagesDeleted;
        private final int settingsDeleted;
        private final int imageFilesDeleted;

        private ResetSummary(
                int productsDeleted,
                int inventoryActionsDeleted,
                int expiryScansDeleted,
                int recipeCacheDeleted,
                int agentMessagesDeleted,
                int settingsDeleted,
                int imageFilesDeleted
        ) {
            this.productsDeleted = productsDeleted;
            this.inventoryActionsDeleted = inventoryActionsDeleted;
            this.expiryScansDeleted = expiryScansDeleted;
            this.recipeCacheDeleted = recipeCacheDeleted;
            this.agentMessagesDeleted = agentMessagesDeleted;
            this.settingsDeleted = settingsDeleted;
            this.imageFilesDeleted = imageFilesDeleted;
        }

        public int getProductsDeleted() {
            return productsDeleted;
        }

        public int getInventoryActionsDeleted() {
            return inventoryActionsDeleted;
        }

        public int getExpiryScansDeleted() {
            return expiryScansDeleted;
        }

        public int getRecipeCacheDeleted() {
            return recipeCacheDeleted;
        }

        public int getAgentMessagesDeleted() {
            return agentMessagesDeleted;
        }

        public int getSettingsDeleted() {
            return settingsDeleted;
        }

        public int getImageFilesDeleted() {
            return imageFilesDeleted;
        }
    }

    private LocalDataResetRepository() {
    }

    public static void resetAsync(Context context, Callback<ResetSummary> callback, ErrorCallback errorCallback) {
        Context appContext = context.getApplicationContext();
        execute(() -> {
            ResetSummary summary = reset(AppDatabase.getInstance(appContext), appContext);
            ReminderScheduler.cancel(appContext);
            return summary;
        }, callback, errorCallback);
    }

    public static ResetSummary reset(AppDatabase database) {
        return reset(database, null);
    }

    public static ResetSummary reset(AppDatabase database, Context context) {
        final int[] productsDeleted = {0};
        final int[] inventoryActionsDeleted = {0};
        final int[] expiryScansDeleted = {0};
        final int[] recipeCacheDeleted = {0};
        final int[] agentMessagesDeleted = {0};
        final int[] settingsDeleted = {0};
        int imageFilesDeleted = context == null ? 0 : LocalImageRepository.deleteAllProductImages(context);

        database.runInTransaction(() -> {
            expiryScansDeleted[0] = database.expiryScanDao().deleteAll();
            inventoryActionsDeleted[0] = database.inventoryActionDao().deleteAll();
            recipeCacheDeleted[0] = database.recipeCacheDao().deleteAll();
            agentMessagesDeleted[0] = database.agentMessageDao().deleteAll();
            productsDeleted[0] = database.productDao().deleteAll();
            settingsDeleted[0] = database.userSettingsDao().deleteAll();
            ProductRepository.ensureLocalDefaults(database);
        });

        return new ResetSummary(
                productsDeleted[0],
                inventoryActionsDeleted[0],
                expiryScansDeleted[0],
                recipeCacheDeleted[0],
                agentMessagesDeleted[0],
                settingsDeleted[0],
                imageFilesDeleted
        );
    }

    private interface Work<T> {
        T run() throws Exception;
    }

    private static <T> void execute(Work<T> work, Callback<T> callback, ErrorCallback errorCallback) {
        EXECUTOR.execute(() -> {
            try {
                T result = work.run();
                if (callback != null) {
                    MAIN.post(() -> callback.onResult(result));
                }
            } catch (Exception error) {
                if (errorCallback != null) {
                    MAIN.post(() -> errorCallback.onError(error));
                }
            }
        });
    }
}
