package com.example.smartexpapp.data;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.FileProvider;

import com.example.smartexpapp.data.local.AgentMessageEntity;
import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.ExpiryScanEntity;
import com.example.smartexpapp.data.local.InventoryActionEntity;
import com.example.smartexpapp.data.local.ProductEntity;
import com.example.smartexpapp.data.local.RecipeCacheEntity;
import com.example.smartexpapp.data.local.StorageLocationEntity;
import com.example.smartexpapp.data.local.UserSettingsEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class LocalDataExportRepository {
    private static final String EXPORT_FILE_NAME = "smartexp-local-export.json";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onResult(T value);
    }

    public interface ErrorCallback {
        void onError(Exception error);
    }

    private LocalDataExportRepository() {
    }

    public static void exportAsync(Context context, Callback<Uri> callback, ErrorCallback errorCallback) {
        Context appContext = context.getApplicationContext();
        execute(() -> export(appContext), callback, errorCallback);
    }

    public static Uri export(Context context) throws Exception {
        AppDatabase database = AppDatabase.getInstance(context);
        ProductRepository.ensureLocalDefaults(database);
        JSONObject json = buildExportJson(database);
        File exportDir = new File(context.getCacheDir(), "exports");
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw new IllegalStateException("Could not create export directory");
        }
        File exportFile = new File(exportDir, EXPORT_FILE_NAME);
        try (FileOutputStream output = new FileOutputStream(exportFile, false)) {
            output.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", exportFile);
    }

    public static JSONObject buildExportJson(AppDatabase database) throws Exception {
        JSONObject root = new JSONObject();
        root.put("schema", "smartexp-local-export-v1");
        root.put("exportedAt", isoNow());
        root.put("products", products(database.productDao().getAllProductsSortedByExpiry()));
        root.put("storageLocations", storageLocations(database.storageLocationDao().getAll()));
        root.put("settings", settings(database.userSettingsDao().getById("default")));
        root.put("inventoryActions", inventoryActions(database.inventoryActionDao().getAll()));
        root.put("expiryScans", expiryScans(database.expiryScanDao().getAll()));
        root.put("recipeCache", recipeCache(database.recipeCacheDao().getAll()));
        root.put("agentMessages", agentMessages(database.agentMessageDao().getConversation()));
        return root;
    }

    private static JSONArray products(List<ProductEntity> products) throws Exception {
        JSONArray array = new JSONArray();
        for (ProductEntity product : products) {
            array.put(new JSONObject()
                    .put("id", product.id)
                    .put("name", product.name)
                    .put("category", product.category)
                    .put("quantity", product.quantity)
                    .put("unit", product.unit)
                    .put("storageLocationId", product.storageLocationId)
                    .put("expiryDateMillis", product.expiryDateMillis)
                    .put("barcode", product.barcode)
                    .put("imageUri", product.imageUri)
                    .put("status", product.status)
                    .put("createdAt", product.createdAt)
                    .put("updatedAt", product.updatedAt)
                    .put("cloudId", jsonNullable(product.cloudId))
                    .put("ownerUserId", jsonNullable(product.ownerUserId))
                    .put("syncStatus", product.syncStatus)
                    .put("lastSyncedAt", product.lastSyncedAt));
        }
        return array;
    }

    private static Object jsonNullable(Object value) {
        return value == null ? JSONObject.NULL : value;
    }

    private static JSONArray storageLocations(List<StorageLocationEntity> locations) throws Exception {
        JSONArray array = new JSONArray();
        for (StorageLocationEntity location : locations) {
            array.put(new JSONObject()
                    .put("id", location.id)
                    .put("name", location.name)
                    .put("type", location.type)
                    .put("sortOrder", location.sortOrder)
                    .put("createdAt", location.createdAt)
                    .put("updatedAt", location.updatedAt));
        }
        return array;
    }

    private static Object settings(UserSettingsEntity settings) throws Exception {
        if (settings == null) {
            return JSONObject.NULL;
        }
        return new JSONObject()
                .put("id", settings.id)
                .put("reminderDaysBefore", settings.reminderDaysBefore)
                .put("reminderNotifyTimeMinutes", settings.reminderNotifyTimeMinutes)
                .put("dietaryPreferences", settings.dietaryPreferences)
                .put("displayName", settings.displayName)
                .put("profileAvatarPath", settings.profileAvatarPath)
                .put("darkMode", settings.darkMode)
                .put("languageTag", settings.languageTag)
                .put("defaultStorageLocationId", settings.defaultStorageLocationId)
                .put("notificationEnabled", settings.notificationEnabled)
                .put("createdAt", settings.createdAt)
                .put("updatedAt", settings.updatedAt);
    }

    private static JSONArray inventoryActions(List<InventoryActionEntity> actions) throws Exception {
        JSONArray array = new JSONArray();
        for (InventoryActionEntity action : actions) {
            array.put(new JSONObject()
                    .put("id", action.id)
                    .put("productId", action.productId)
                    .put("actionType", action.actionType)
                    .put("quantityChanged", action.quantityChanged)
                    .put("actionAt", action.actionAt)
                    .put("note", action.note)
                    .put("createdAt", action.createdAt)
                    .put("updatedAt", action.updatedAt));
        }
        return array;
    }

    private static JSONArray expiryScans(List<ExpiryScanEntity> scans) throws Exception {
        JSONArray array = new JSONArray();
        for (ExpiryScanEntity scan : scans) {
            array.put(new JSONObject()
                    .put("id", scan.id)
                    .put("productId", scan.productId)
                    .put("rawText", scan.rawText)
                    .put("detectedDateMillis", scan.detectedDateMillis)
                    .put("confidence", scan.confidence)
                    .put("scannedAt", scan.scannedAt)
                    .put("createdAt", scan.createdAt)
                    .put("updatedAt", scan.updatedAt));
        }
        return array;
    }

    private static JSONArray recipeCache(List<RecipeCacheEntity> recipes) throws Exception {
        JSONArray array = new JSONArray();
        for (RecipeCacheEntity recipe : recipes) {
            array.put(new JSONObject()
                    .put("id", recipe.id)
                    .put("provider", recipe.provider)
                    .put("title", recipe.title)
                    .put("imageUrl", recipe.imageUrl)
                    .put("sourceUrl", recipe.sourceUrl)
                    .put("usedIngredients", recipe.usedIngredients)
                    .put("missingIngredients", recipe.missingIngredients)
                    .put("cachedAt", recipe.cachedAt)
                    .put("createdAt", recipe.createdAt)
                    .put("updatedAt", recipe.updatedAt));
        }
        return array;
    }

    private static JSONArray agentMessages(List<AgentMessageEntity> messages) throws Exception {
        JSONArray array = new JSONArray();
        for (AgentMessageEntity message : messages) {
            array.put(new JSONObject()
                    .put("id", message.id)
                    .put("role", message.role)
                    .put("message", message.message)
                    .put("relatedProductIds", message.relatedProductIds)
                    .put("sourcePrompt", message.sourcePrompt)
                    .put("createdAt", message.createdAt)
                    .put("updatedAt", message.updatedAt));
        }
        return array;
    }

    private static String isoNow() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format.format(new Date());
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
