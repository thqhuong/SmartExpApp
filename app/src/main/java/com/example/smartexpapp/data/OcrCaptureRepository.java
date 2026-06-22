package com.example.smartexpapp.data;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

public final class OcrCaptureRepository {
    private OcrCaptureRepository() {
    }

    public static Uri createCaptureUri(Context context) throws IOException {
        File image = createCaptureFile(context);
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", image);
    }

    static File createCaptureFile(Context context) throws IOException {
        File ocrDir = new File(context.getCacheDir(), "ocr");
        if (!ocrDir.exists() && !ocrDir.mkdirs()) {
            throw new IOException("Could not create OCR cache directory");
        }
        return File.createTempFile("ocr-", ".jpg", ocrDir);
    }
}
