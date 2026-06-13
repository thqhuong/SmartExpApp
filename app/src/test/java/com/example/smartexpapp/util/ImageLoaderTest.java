package com.example.smartexpapp.util;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ImageLoaderTest {
    @Test
    public void remoteCacheKeyIsStableAndQuerySensitive() {
        String first = ImageLoader.cacheKeyForRemoteUrl("https://example.com/recipe-image?title=Soup&ingredients=Tomato");
        String second = ImageLoader.cacheKeyForRemoteUrl("https://example.com/recipe-image?title=Soup&ingredients=Tomato");
        String different = ImageLoader.cacheKeyForRemoteUrl("https://example.com/recipe-image?title=Bowl&ingredients=Rice");

        assertEquals(first, second);
        assertNotEquals(first, different);
        assertEquals(64, first.length());
        assertTrue(first.matches("[0-9a-f]+"));
    }

    @Test
    public void remoteCacheFileStaysInsideAppCacheDirectory() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File cacheDirectory = ImageLoader.remoteImageCacheDirectory(context).getCanonicalFile();
        File cacheFile = ImageLoader.cacheFileForRemoteUrl(
                context,
                "https://smart-exp-recipe-images.example.workers.dev/recipe-image?title=Soup&ingredients=Tomato%2CBasil"
        ).getCanonicalFile();

        assertTrue(cacheFile.getPath().startsWith(cacheDirectory.getPath() + File.separator));
        assertTrue(cacheFile.getName().endsWith(".img"));
    }

    @Test
    public void localFilePathsAreNotTreatedAsRemoteUrls() {
        assertFalse(ImageLoader.isRemoteUrl("/data/user/0/app/files/images/product.jpg"));
        assertFalse(ImageLoader.isRemoteUrl("file:///data/user/0/app/files/images/product.jpg"));
        assertTrue(ImageLoader.isRemoteUrl("https://example.com/recipe.png"));
    }
}
