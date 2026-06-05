package com.example.smartexpapp.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ImageLoader {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ImageLoader() {
    }

    public static void load(ImageView imageView, String path) {
        if (path == null || path.trim().isEmpty()) {
            return;
        }

        imageView.setTag(path);
        EXECUTOR.execute(() -> {
            Bitmap bitmap = decode(path);
            if (bitmap == null) {
                return;
            }
            MAIN.post(() -> {
                if (path.equals(imageView.getTag())) {
                    imageView.clearColorFilter();
                    imageView.setImageTintList(null);
                    imageView.setImageBitmap(bitmap);
                }
            });
        });
    }

    private static Bitmap decode(String path) {
        if (path.startsWith("/") || path.startsWith("file://")) {
            String filePath = path.startsWith("file://") ? path.substring(7) : path;
            return BitmapFactory.decodeFile(filePath);
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setInstanceFollowRedirects(true);
            try (InputStream stream = connection.getInputStream()) {
                return BitmapFactory.decodeStream(stream);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
