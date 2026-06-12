package com.example.smartexpapp.data;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class OcrCaptureRepositoryTest {
    @Test
    public void createCaptureFileUsesOcrCacheDirectory() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();

        File file = OcrCaptureRepository.createCaptureFile(context);

        File ocrDir = new File(context.getCacheDir(), "ocr");
        assertTrue(ocrDir.exists());
        assertTrue(file.getName().startsWith("ocr-"));
        assertTrue(file.getName().endsWith(".jpg"));
        assertTrue(file.getParentFile().equals(ocrDir));
    }
}
