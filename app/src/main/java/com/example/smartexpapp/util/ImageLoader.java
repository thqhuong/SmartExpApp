package com.example.smartexpapp.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ImageLoader {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> MEMORY_CACHE = new LruCache<String, Bitmap>(memoryCacheSize()) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value == null ? 0 : value.getByteCount() / 1024;
        }
    };

    private ImageLoader() {
    }

    public static void load(ImageView imageView, String path) {
        if (path == null || path.trim().isEmpty()) {
            return;
        }

        imageView.setTag(path);
        Bitmap cached = MEMORY_CACHE.get(path);
        if (cached != null) {
            imageView.clearColorFilter();
            imageView.setImageTintList(null);
            imageView.setImageBitmap(cached);
            return;
        }

        Context context = imageView.getContext().getApplicationContext();
        EXECUTOR.execute(() -> {
            Bitmap bitmap = decode(context, path);
            if (bitmap == null) {
                return;
            }
            MEMORY_CACHE.put(path, bitmap);
            MAIN.post(() -> {
                if (path.equals(imageView.getTag())) {
                    imageView.clearColorFilter();
                    imageView.setImageTintList(null);
                    imageView.setImageBitmap(bitmap);
                }
            });
        });
    }

    public static void prefetch(Context context, List<String> paths) {
        if (context == null || paths == null || paths.isEmpty()) {
            return;
        }
        Context appContext = context.getApplicationContext();
        for (String path : paths) {
            prefetch(appContext, path);
        }
    }

    public static void prefetch(Context context, String path) {
        if (context == null || path == null || path.trim().isEmpty() || MEMORY_CACHE.get(path) != null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            Bitmap bitmap = decode(appContext, path);
            if (bitmap != null) {
                MEMORY_CACHE.put(path, bitmap);
            }
        });
    }

    private static Bitmap decode(Context context, String path) {
        if (path.startsWith("/") || path.startsWith("file://")) {
            String filePath = path.startsWith("file://") ? path.substring(7) : path;
            return BitmapFactory.decodeFile(filePath);
        }
        if (isRemoteUrl(path)) {
            return decodeRemote(context, path);
        }
        return null;
    }

    private static Bitmap decodeRemote(Context context, String path) {
        File cachedFile = cacheFileForRemoteUrl(context, path);
        if (cachedFile.exists()) {
            Bitmap cached = BitmapFactory.decodeFile(cachedFile.getAbsolutePath());
            if (cached != null) {
                return cached;
            }
            cachedFile.delete();
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setInstanceFollowRedirects(true);
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                return null;
            }
            byte[] bytes;
            try (InputStream stream = connection.getInputStream()) {
                bytes = readAllBytes(stream);
            }
            if (bytes.length == 0) {
                return null;
            }
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap == null) {
                return null;
            }
            writeCacheFile(cachedFile, bytes);
            return bitmap;
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static byte[] readAllBytes(InputStream stream) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static void writeCacheFile(File cachedFile, byte[] bytes) {
        File directory = cachedFile.getParentFile();
        if (directory == null || (!directory.exists() && !directory.mkdirs())) {
            return;
        }
        File tempFile = new File(directory, cachedFile.getName() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(tempFile)) {
            output.write(bytes);
            if (!tempFile.renameTo(cachedFile)) {
                if (cachedFile.exists()) {
                    cachedFile.delete();
                }
                tempFile.renameTo(cachedFile);
            }
        } catch (Exception ignored) {
            tempFile.delete();
        }
    }

    static boolean isRemoteUrl(String path) {
        return path != null && (path.startsWith("http://") || path.startsWith("https://"));
    }

    static File cacheFileForRemoteUrl(Context context, String url) {
        return new File(remoteImageCacheDirectory(context), cacheKeyForRemoteUrl(url) + ".img");
    }

    static File remoteImageCacheDirectory(Context context) {
        return new File(context.getCacheDir(), "remote_image_cache");
    }

    static String cacheKeyForRemoteUrl(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((url == null ? "" : url).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(Character.forDigit((b >> 4) & 0xF, 16));
                builder.append(Character.forDigit(b & 0xF, 16));
            }
            return builder.toString();
        } catch (Exception ignored) {
            return String.valueOf((url == null ? "" : url).hashCode());
        }
    }

    private static int memoryCacheSize() {
        long maxMemoryKb = Runtime.getRuntime().maxMemory() / 1024;
        long cacheSizeKb = maxMemoryKb / 8;
        return (int) Math.max(4 * 1024, Math.min(cacheSizeKb, 32 * 1024));
    }
}
