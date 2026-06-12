package com.example.smartexpapp.data;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class LocalImageRepositoryTest {
    @Test
    public void deleteProductImageDeletesOnlyFilesInsideImageDirectory() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File imageDir = LocalImageRepository.imageDirectory(context);
        assertTrue(imageDir.exists() || imageDir.mkdirs());
        File image = new File(imageDir, "product.jpg");
        try (FileOutputStream output = new FileOutputStream(image)) {
            output.write(new byte[] {1, 2, 3});
        }
        File outside = File.createTempFile("outside", ".jpg", context.getCacheDir());

        assertEquals(1, LocalImageRepository.deleteProductImage(context, image.getAbsolutePath()));
        assertFalse(image.exists());
        assertEquals(0, LocalImageRepository.deleteProductImage(context, outside.getAbsolutePath()));
        assertTrue(outside.exists());
    }

    @Test
    public void deleteReplacedProductImageDeletesPreviousOnlyWhenPathChanged() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File imageDir = LocalImageRepository.imageDirectory(context);
        assertTrue(imageDir.exists() || imageDir.mkdirs());
        File previous = new File(imageDir, "previous.jpg");
        File replacement = new File(imageDir, "replacement.jpg");
        writeImage(previous);
        writeImage(replacement);

        assertEquals(0, LocalImageRepository.deleteReplacedProductImage(
                context,
                previous.getAbsolutePath(),
                "file://" + previous.getAbsolutePath()
        ));
        assertTrue(previous.exists());

        assertEquals(1, LocalImageRepository.deleteReplacedProductImage(
                context,
                previous.getAbsolutePath(),
                replacement.getAbsolutePath()
        ));
        assertFalse(previous.exists());
        assertTrue(replacement.exists());
    }

    @Test
    public void resolveLocalImageRejectsRemoteAndOutsidePaths() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File outside = File.createTempFile("outside", ".jpg", context.getCacheDir());

        assertNull(LocalImageRepository.resolveLocalImage(context, "https://example.com/product.jpg"));
        assertNull(LocalImageRepository.resolveLocalImage(context, outside.getAbsolutePath()));
    }

    private void writeImage(File image) throws Exception {
        try (FileOutputStream output = new FileOutputStream(image)) {
            output.write(new byte[] {1, 2, 3});
        }
    }
}
