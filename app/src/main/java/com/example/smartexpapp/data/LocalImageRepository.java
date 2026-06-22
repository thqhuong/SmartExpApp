package com.example.smartexpapp.data;

import android.content.Context;

import java.io.File;
import java.io.IOException;

public final class LocalImageRepository {
    private LocalImageRepository() {
    }

    public static int deleteProductImage(Context context, String imagePath) {
        File file = resolveLocalImage(context, imagePath);
        if (file == null || !file.exists() || file.isDirectory()) {
            return 0;
        }
        return file.delete() ? 1 : 0;
    }

    public static int deleteReplacedProductImage(Context context, String previousImagePath, String replacementImagePath) {
        File previous = resolveLocalImage(context, previousImagePath);
        if (previous == null || !previous.exists() || previous.isDirectory()) {
            return 0;
        }
        File replacement = resolveLocalImage(context, replacementImagePath);
        if (replacement != null && previous.equals(replacement)) {
            return 0;
        }
        return previous.delete() ? 1 : 0;
    }

    public static int deleteAllProductImages(Context context) {
        File imageDir = imageDirectory(context);
        File[] files = imageDir.listFiles();
        if (files == null || files.length == 0) {
            return 0;
        }
        int deleted = 0;
        for (File file : files) {
            if (file.isFile() && file.delete()) {
                deleted++;
            }
        }
        return deleted;
    }

    static File imageDirectory(Context context) {
        return new File(context.getFilesDir(), "images");
    }

    static File resolveLocalImage(Context context, String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return null;
        }
        try {
            File imageDir = imageDirectory(context).getCanonicalFile();
            File file = new File(imagePath.startsWith("file://") ? imagePath.substring(7) : imagePath).getCanonicalFile();
            String imageDirPath = imageDir.getPath() + File.separator;
            if (!file.getPath().startsWith(imageDirPath)) {
                return null;
            }
            return file;
        } catch (IOException ignored) {
            return null;
        }
    }
}
